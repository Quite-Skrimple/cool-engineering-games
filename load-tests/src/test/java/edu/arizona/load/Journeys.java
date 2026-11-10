package edu.arizona.load;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import io.gatling.javaapi.core.ScenarioBuilder;

/** What simulated users do. Every request goes through the gateway with a real bearer token. */
final class Journeys {

    private Journeys() {
    }

    static String prop(String name, String fallback) {
        return System.getProperty(name, fallback);
    }

    static int intProp(String name, int fallback) {
        return Integer.parseInt(prop(name, String.valueOf(fallback)));
    }

    /**
     * A logged-in player browsing and joining in. Posting a comment or reacting makes engagement-service
     * call game-catalog-service with the player's own token, so one journey exercises the gateway,
     * service discovery, token propagation between services and both databases.
     */
    static ScenarioBuilder player(Seeder seed) {
        Iterator<Map<String, Object>> feeder = Stream.generate(() -> {
            Seeder.Player player = seed.randomPlayer();
            return Map.<String, Object>of("token", player.token(), "gameId", seed.randomGame());
        }).iterator();

        return scenario("Player browsing and engaging")
                .feed(feeder)
                .exec(http("List approved games").get("/api/games")
                        .header("Authorization", "Bearer #{token}").check(status().is(200)))
                .pause(Duration.ofMillis(100), Duration.ofMillis(400))
                .exec(http("View a game").get("/api/games/#{gameId}")
                        .header("Authorization", "Bearer #{token}").check(status().is(200)))
                .exec(http("Read comments").get("/api/comments?gameId=#{gameId}")
                        .header("Authorization", "Bearer #{token}").check(status().is(200)))
                .pause(Duration.ofMillis(100), Duration.ofMillis(400))
                .exec(http("Post a comment (engagement calls catalog)").post("/api/comments")
                        .header("Authorization", "Bearer #{token}")
                        .body(StringBody("{\"gameId\":#{gameId},\"body\":\"load test comment\"}"))
                        .check(status().is(201)))
                .exec(http("React to a game (engagement calls catalog)").post("/api/reactions")
                        .header("Authorization", "Bearer #{token}")
                        .body(StringBody("{\"gameId\":#{gameId},\"type\":\"LIKE\"}"))
                        .check(status().in(201, 409)))
                .exec(http("Read my library").get("/api/library")
                        .header("Authorization", "Bearer #{token}").check(status().is(200)));
    }

    /**
     * The security rules, checked while the system is busy. A rejection counts as a success here because
     * the check expects it: no token must give 401, a tampered token 401, a player asking for the user
     * list 403, and the administrator the same list with 200.
     */
    static ScenarioBuilder security(Seeder seed) {
        Iterator<Map<String, Object>> feeder = Stream.generate(() -> Map.<String, Object>of(
                "playerToken", seed.randomPlayer().token(), "adminToken", seed.adminToken())).iterator();

        return scenario("Security rules under load")
                .feed(feeder)
                .exec(http("Unauthenticated request (expect 401)").get("/api/games").check(status().is(401)))
                .exec(http("Tampered token (expect 401)").get("/api/games")
                        .header("Authorization", "Bearer #{playerToken}x").check(status().is(401)))
                .exec(http("Forbidden request: player lists users (expect 403)").get("/api/users")
                        .header("Authorization", "Bearer #{playerToken}").check(status().is(403)))
                .exec(http("Player cannot submit a game (expect 403)").post("/api/games")
                        .header("Authorization", "Bearer #{playerToken}")
                        .body(StringBody("{\"title\":\"x\",\"genre\":\"x\",\"description\":\"x\",\"playableUrl\":\"https://example.com\"}"))
                        .check(status().is(403)))
                .exec(http("Administrator lists users (expect 200)").get("/api/users")
                        .header("Authorization", "Bearer #{adminToken}").check(status().is(200)));
    }

    /** A brand-new visitor: sign up, log in, load their profile. The password hashing makes this the heavy path. */
    static ScenarioBuilder newcomer(Seeder seed) {
        return scenario("Newcomer signing up")
                .exec(session -> session.set("name", "nc" + UUID.randomUUID().toString().replace("-", "").substring(0, 20)))
                .exec(http("Register").post("/api/auth/register")
                        .body(StringBody("{\"username\":\"#{name}\",\"email\":\"#{name}@arizona.edu\",\"password\":\"password123\"}"))
                        .check(status().is(201), jmesPath("id").saveAs("userId")))
                .exec(session -> {
                    seed.extraUserIds.add(Long.parseLong(session.getString("userId")));
                    return session;
                })
                .exec(http("Log in").post("/api/auth/login")
                        .body(StringBody("{\"username\":\"#{name}\",\"password\":\"password123\"}"))
                        .check(status().is(200), jmesPath("accessToken").saveAs("accessToken")))
                .exec(http("Load my profile").get("/api/users/me")
                        .header("Authorization", "Bearer #{accessToken}").check(status().is(200)));
    }
}
