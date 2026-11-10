package edu.arizona.load;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates the data a simulation needs through the gateway (real users with real tokens, approved games)
 * and removes it again afterwards, so a run leaves the system as it found it.
 */
final class Seeder {

    record Player(long id, String token) {
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String baseUrl;
    private final String runId = Long.toString(System.currentTimeMillis(), 36);

    private String adminUsername;
    private String adminPassword;
    private String adminToken;
    final List<Player> players = new CopyOnWriteArrayList<>();
    final List<Long> gameIds = new CopyOnWriteArrayList<>();
    /** Users created while the simulation runs (the "newcomers" journey), deleted at the end. */
    final Queue<Long> extraUserIds = new ConcurrentLinkedQueue<>();

    Seeder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    void seed(String adminUsername, String adminPassword, int playerCount, int gameCount) throws Exception {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        loginAsAdmin();

        for (int i = 0; i < gameCount; i++) {
            String game = call("POST", "/api/games",
                    "{\"title\":\"Load test game %s-%d\",\"genre\":\"Puzzle\",\"playableUrl\":\"https://example.com/load\"}"
                            .formatted(runId, i), adminToken, 201);
            long id = Long.parseLong(field(game, "id"));
            call("PUT", "/api/games/" + id + "/approval", "{\"status\":\"APPROVED\"}", adminToken, 200);
            gameIds.add(id);
        }

        // password hashing is deliberately slow, so create the players in parallel
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                String name = "load" + runId + "x" + n;
                String user = call("POST", "/api/auth/register",
                        "{\"username\":\"%s\",\"email\":\"%s@arizona.edu\",\"password\":\"password123\"}".formatted(name, name),
                        null, 201);
                String login = call("POST", "/api/auth/login",
                        "{\"username\":\"%s\",\"password\":\"password123\"}".formatted(name), null, 200);
                players.add(new Player(Long.parseLong(field(user, "id")), field(login, "accessToken")));
                return null;
            }));
        }
        for (var future : futures) {
            future.get();
        }
        pool.shutdown();
        System.out.printf("Seeded %d players and %d approved games through %s%n", players.size(), gameIds.size(), baseUrl);
    }

    Player randomPlayer() {
        return players.get(ThreadLocalRandom.current().nextInt(players.size()));
    }

    long randomGame() {
        return gameIds.get(ThreadLocalRandom.current().nextInt(gameIds.size()));
    }

    /** The administrator's token, for journeys that check what an administrator may do. */
    String adminToken() {
        return adminToken;
    }

    void cleanup() {
        if (adminToken == null) {
            return;
        }
        int removed = 0;
        try {
            // a long run can outlive the token the seeding used, so log in again
            loginAsAdmin();
            for (Player player : players) {
                call("DELETE", "/api/users/" + player.id(), null, adminToken, 204);
                removed++;
            }
            for (Long id : extraUserIds) {
                call("DELETE", "/api/users/" + id, null, adminToken, 204);
                removed++;
            }
            for (Long id : gameIds) {
                call("DELETE", "/api/games/" + id, null, adminToken, 204);
            }
            System.out.printf("Removed %d users and %d games created for the run%n", removed, gameIds.size());
        } catch (Exception e) {
            System.out.println("Cleanup incomplete: " + e.getMessage());
        }
    }

    private void loginAsAdmin() {
        adminToken = field(call("POST", "/api/auth/login",
                "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(adminUsername, adminPassword), null, 200),
                "accessToken");
    }

    private String call(String method, String path, String json, String token, int expectedStatus) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            request.method(method, json == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json));

            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != expectedStatus) {
                throw new IllegalStateException(method + " " + path + " answered " + response.statusCode()
                        + " (expected " + expectedStatus + "): " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(method + " " + path + " failed: " + e.getMessage(), e);
        }
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"?([^\",}]+)\"?").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("No '" + name + "' in " + json);
        }
        return m.group(1);
    }
}
