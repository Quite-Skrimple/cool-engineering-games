# Phase 1 Video Script — Cool Engineering Games

Target length: **6–8 minutes**. Read the lines in *italics* as narration (adjust the wording to sound natural in your own voice — it doesn't need to be verbatim). Each **[ACTION]** is a screen-recording step. Record in one take per scene if possible; it's easier to stitch 5 short clips than to redo one long one.

Save the final file as `LastnameFirstname_Phase1_Presentation.mp4`.

---

## Before you hit record — setup checklist

Do this once, before recording anything, so you're not waiting on builds on camera:

1. Open a terminal at the repo root and run `mvn package` once, so the Docker build step (Scene 5) is fast.
2. Open the repo in your IDE with these files ready in tabs: [`identity-service/.../entity/User.java`](../identity-service/src/main/java/edu/arizona/identity/model/entity/User.java), [`game-catalog-service/.../entity/Game.java`](../game-catalog-service/src/main/java/edu/arizona/catalog/model/entity/Game.java), [`engagement-service/.../entity/Reaction.java`](../engagement-service/src/main/java/edu/arizona/engagement/model/entity/Reaction.java).
3. Open [`docs/architecture.svg`](architecture.svg) in a browser tab, large enough to read.
4. Open Postman, import `postman/cool-engineering-games.postman_collection.json` and `postman/cool-engineering-games.postman_environment.json`, and select the **Cool Engineering Games - Local** environment.
5. Make sure nothing is already bound to ports 8081–8083 or 8888 (close any services from a previous run).
6. Have a second terminal ready at `docker/` for Scene 5.

---

## Scene 1 — Theme (30–45 sec)

**[ACTION]** Face-cam or a title slide with the project name.

*"Hey, we're presenting Phase 1 of Cool Engineering Games — think CoolMathGames, but for short browser games built around engineering concepts: circuits, logic, structures, that kind of thing. Players can browse games, react to them, save them to a library, and comment. Creators submit games that go through an approval flow before they're public. For Phase 1, we built the backend as four Spring Boot microservices, each with its own database, fully Dockerized, with a config server driving dev and prod profiles. Let's walk through it."*

---

## Scene 2 — Canonical model & bounded contexts (90–120 sec)

**[ACTION]** Switch to the IDE. Show the services folder list first (`identity-service`, `game-catalog-service`, `engagement-service`, `configserver`), then open `User.java`.

*"Each microservice is its own bounded context with its own schema — there are no shared tables and no cross-service foreign keys anywhere in this system."*

**[ACTION]** Point to the `User` entity fields (`id`, `username`, `email`, `passwordHash`, `role`, timestamps).

*"`identity-service` owns the `User` entity — accounts, roles, that's it. It's the source of truth for who a player or developer is."*

**[ACTION]** Switch tab to `Game.java`, point at `creatorId`.

*"`game-catalog-service` owns `Game` — title, genre, a playable URL, and an approval status. Notice `creatorId` here — it's just a plain `Long`, not a JPA relationship. We never join across service boundaries; if we need to know who created a game, we store the id and, if needed, ask `identity-service` over REST."*

**[ACTION]** Switch tab to `Reaction.java` (mention Comment/LibraryEntry verbally without needing to open them).

*"`engagement-service` owns three entities that travel together — `Reaction` for likes and dislikes, `LibraryEntry` for a player's saved games, and `Comment`. They share one schema because they're all the same concern — a player engaging with a game — but again, `userId` and `gameId` are just ids, no foreign keys out to the other two services."*

*"That's the canonical model: four bounded contexts, one schema each, connected only by REST calls and plain ids — never by database joins."* (Optionally, pull up the table in [README.md](../README.md) under "Canonical data model & bounded contexts" and scroll it on screen for a few seconds.)

---

## Scene 3 — Deployment sketch (45–60 sec)

**[ACTION]** Switch to the browser tab with `docs/architecture.svg` open full-screen.

*"Here's the deployment we're aiming for. In prod, all four services plus three Postgres databases run in one Docker network — `configserver` hands out profile-specific settings at boot, and each business service only ever talks to its own database."*

**[ACTION]** Point at the bottom half of the diagram (dev profile box).

*"In dev, there's no Docker at all — each service runs standalone on the host with an in-memory H2 database, and pulling config from `configserver` is attempted but optional, so you can run one service in isolation without spinning up everything else."*

---

## Scene 4 — REST prototype via Postman (2–2.5 min)

**[ACTION]** Switch to a terminal. Start the three services in dev mode (three separate terminal tabs, or run them in the background):

```bash
mvn -pl identity-service spring-boot:run
```
```bash
mvn -pl game-catalog-service spring-boot:run
```
```bash
mvn -pl engagement-service spring-boot:run
```

*"Let's bring the services up in dev mode first, so you can see the REST API working end to end."*

**[ACTION]** Wait for all three to log `Started ...Application`. Switch to Postman.

*"This is the Postman collection — 38 requests, 65 assertions, covering every endpoint on every service, including the error paths like 409 conflicts and 404s, with a cleanup folder at the end so the whole thing is safely re-runnable."*

**[ACTION]** Open the collection's folder tree so "1. configserver", "2. identity-service", "3. game-catalog-service", "4. engagement-service", "5. Cleanup" are visible.

*"It walks the full story: create a user, create a game under that user, react to it, add it to a library, comment on it — then clean everything up at the end."*

**[ACTION]** Click the collection's **⋮ → Run collection**, confirm the **Cool Engineering Games - Local** environment is selected, click **Run Cool Engineering Games**.

*"Let's run the whole collection top to bottom."*

**[ACTION]** Let it finish (a few seconds). Show the results summary (all green / passed assertions).

*"And there it is — every request passed, every assertion green, including the negative tests. That's the full CRUD surface of all three business services exercised in one run."*

**[ACTION — optional, nice to have]** Click into one request (e.g. "Create user") and show the request body and the response JSON, and one error-path request (e.g. "Create user - duplicate username (409)") to show it returning the expected 409.

*"Quick close-up: a plain POST with a JSON body comes back 201 with the created resource — and notice the password is never echoed back. And here's the duplicate-username case correctly returning a 409 with a clear error message."*

**[ACTION]** Stop the three `mvn spring-boot:run` processes (Ctrl+C in each terminal) before moving to Scene 5.

---

## Scene 5 — Docker deployment & two profiles (90–120 sec)

**[ACTION]** Switch to the terminal at `docker/`.

*"Now let's switch to the prod profile, fully Dockerized. We already packaged the jars, so we just bring the stack up."*

**[ACTION]** Run:

```bash
docker compose up --build
```

*"This builds and starts `configserver`, a dedicated Postgres container per service, and all three business services with `SPRING_PROFILES_ACTIVE=prod`. Each service waits for its database and for `configserver` to report healthy before it starts."*

**[ACTION]** While it's starting, switch to a browser or `curl`/Postman and hit the config server directly to show the two profiles side by side:

```bash
curl http://localhost:8888/identity-service/dev
```
```bash
curl http://localhost:8888/identity-service/prod
```

*"Here's the same config server answering for both profiles — dev resolves to an H2 in-memory URL, prod resolves to Postgres. That's the one config service driving both environments from the same codebase."*

**[ACTION]** Once all containers report healthy (`docker compose ps`, or watch the logs), switch back to Postman, re-select the environment (same URLs — dev and prod expose the same ports), and run the collection again, or just fire a couple of requests live (e.g. "Create user", "Get all users").

*"And the exact same Postman collection runs against this Docker/Postgres stack without changing a single URL — same ports, same contracts, different profile under the hood."*

**[ACTION]** Switch to a Postgres client, `docker exec` into `identity-db`, or just show `docker compose ps` listing the running Postgres containers.

*"And this time the data isn't in-memory — it's sitting in these Postgres containers, each with its own named volume, so it survives a restart."*

**[ACTION]** Run `docker compose down -v` at the end if you want a clean state for the next take; otherwise leave it running.

---

## Scene 6 — Wrap-up (15–20 sec)

**[ACTION]** Back to face-cam or title slide.

*"That's Phase 1 — four bounded-context microservices, a canonical data model with no cross-service joins, a config server driving dev and prod, full CRUD REST coverage tested in Postman, and a Dockerized deployment. Thanks for watching — links to the GitHub repo, the Postman workspace, and the README are in the submission."*

**[ACTION]** Stop recording.

---

## After recording

- Trim dead air between scenes.
- Export as `.mp4`.
- Rename to `LastnameFirstname_Phase1_Presentation.mp4` using whichever partner is submitting.
