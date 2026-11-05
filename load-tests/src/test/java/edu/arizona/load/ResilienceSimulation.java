package edu.arizona.load;

import static edu.arizona.load.Journeys.intProp;
import static edu.arizona.load.Journeys.prop;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.http.HttpDsl.http;

import java.time.Duration;

import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * A constant arrival rate that does not slow down when the system does, so failures show up as
 * errors and latency instead of being hidden by fewer requests. Start it, then stop and restart
 * service containers while it runs and read the report: errors should appear while a service is
 * gone and stop once it is back.
 *
 * mvn -pl load-tests gatling:test -Dgatling.simulationClass=edu.arizona.load.ResilienceSimulation
 *     -DadminPassword=... [-Drate=15 -Dhold=120 -DminSuccess=50]
 */
public class ResilienceSimulation extends Simulation {

    private final Seeder seeder = new Seeder(prop("baseUrl", "http://localhost:8000"));

    public ResilienceSimulation() {
        HttpProtocolBuilder protocol = http.baseUrl(prop("baseUrl", "http://localhost:8000"))
                .acceptHeader("application/json")
                .contentTypeHeader("application/json");

        setUp(Journeys.player(seeder).injectOpen(
                constantUsersPerSec(intProp("rate", 15)).during(Duration.ofSeconds(intProp("hold", 120)))))
                .protocols(protocol)
                .assertions(global().successfulRequests().percent().gt((double) intProp("minSuccess", 50)));
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
