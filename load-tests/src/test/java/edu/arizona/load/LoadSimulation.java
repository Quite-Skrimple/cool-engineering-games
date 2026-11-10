package edu.arizona.load;

import static edu.arizona.load.Journeys.intProp;
import static edu.arizona.load.Journeys.prop;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.http.HttpDsl.http;

import java.time.Duration;

import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * Throughput under a steady number of concurrent users: ramp up, then hold. Each finished user is
 * replaced by a new one, so a faster system completes more journeys. Run it against one instance of
 * each service and again against several to see what scaling out buys.
 *
 * mvn -pl load-tests gatling:test -Dgatling.simulationClass=edu.arizona.load.LoadSimulation
 *     -DadminPassword=... [-Dconcurrent=40 -Dramp=20 -Dhold=60 -DbaseUrl=http://localhost:8000]
 */
public class LoadSimulation extends Simulation {

    private final Seeder seeder = new Seeder(prop("baseUrl", "http://localhost:8000"));

    public LoadSimulation() {
        int concurrent = intProp("concurrent", 20);
        Duration ramp = Duration.ofSeconds(intProp("ramp", 20));
        Duration hold = Duration.ofSeconds(intProp("hold", 60));
        int newcomersPerSecond = intProp("newcomersPerSecond", 2);

        HttpProtocolBuilder protocol = http.baseUrl(prop("baseUrl", "http://localhost:8000"))
                .acceptHeader("application/json")
                .contentTypeHeader("application/json");

        setUp(
                Journeys.player(seeder).injectClosed(
                        rampConcurrentUsers(1).to(concurrent).during(ramp),
                        constantConcurrentUsers(concurrent).during(hold)),
                Journeys.newcomer(seeder).injectOpen(
                        constantUsersPerSec(newcomersPerSecond).during(ramp.plus(hold))),
                Journeys.security(seeder).injectOpen(
                        constantUsersPerSec(intProp("securityChecksPerSecond", 2)).during(ramp.plus(hold))))
                .protocols(protocol)
                .assertions(
                        global().successfulRequests().percent().gt(95.0),
                        global().failedRequests().percent().lt(1.0),
                        global().responseTime().percentile(95.0).lt(intProp("maxP95Millis", 1500)));
    }

    @Override
    public void before() {
        try {
            seeder.seed(prop("adminUsername", "admin"), prop("adminPassword", ""),
                    intProp("players", 40), intProp("games", 10));
        } catch (Exception e) {
            throw new IllegalStateException("Could not seed the system: " + e.getMessage(), e);
        }
    }

    @Override
    public void after() {
        seeder.cleanup();
    }
}
