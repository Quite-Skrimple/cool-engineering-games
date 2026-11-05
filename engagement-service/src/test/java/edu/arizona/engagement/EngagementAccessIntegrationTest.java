package edu.arizona.engagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import edu.arizona.engagement.config.GameCatalogClient;
import edu.arizona.engagement.exception.ServiceUnavailableException;
import edu.arizona.engagement.repository.ReactionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "eureka.client.enabled=false")
class EngagementAccessIntegrationTest {

    private static final String ALICE = TestTokens.bearer(11, "alice", "PLAYER");
    private static final String BOB = TestTokens.bearer(12, "bob", "PLAYER");
    private static final String ADMIN = TestTokens.bearer(900, "admin", "ADMIN");

    /** Every test gets its own game id, because the database is shared between tests. */
    private static final AtomicLong NEXT_GAME = new AtomicLong(1000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReactionRepository reactionRepository;

    @MockitoBean
    private GameCatalogClient catalogClient;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String token, String json) throws Exception {
        var builder = request.header("Authorization", token);
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    private long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long react(long gameId, String token, String type) throws Exception {
        return idOf(send(post("/api/reactions"), token, "{\"gameId\":%d,\"type\":\"%s\"}".formatted(gameId, type))
                .andExpect(status().isCreated()));
    }

    private long comment(long gameId, String token, String body) throws Exception {
        return idOf(send(post("/api/comments"), token, "{\"gameId\":%d,\"body\":\"%s\"}".formatted(gameId, body))
                .andExpect(status().isCreated()));
    }

    // ---------- authentication ----------

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/reactions?gameId=1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/comments?gameId=1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/library")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameId\":1,\"type\":\"LIKE\"}")).andExpect(status().isUnauthorized());
    }

    // ---------- the user comes from the token ----------

    @Test
    void theReactionBelongsToTheCallerEvenIfTheBodyNamesSomeoneElse() throws Exception {
        long game = NEXT_GAME.incrementAndGet();

        send(post("/api/reactions"), ALICE,
                "{\"userId\":999,\"gameId\":%d,\"type\":\"LIKE\"}".formatted(game))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(11))
                .andExpect(jsonPath("$.gameId").value(game));
    }

    @Test
    void theCatalogIsAskedAboutTheGameBeforeAnythingIsStored() throws Exception {
        long game = NEXT_GAME.incrementAndGet();

        react(game, ALICE, "LIKE");

        verify(catalogClient).requireVisibleGame(game);
    }

    @Test
    void aReactionCanOnlyBeMadeOncePerGame() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        react(game, ALICE, "LIKE");

        send(post("/api/reactions"), ALICE, "{\"gameId\":%d,\"type\":\"DISLIKE\"}".formatted(game))
                .andExpect(status().isConflict());
        react(game, BOB, "DISLIKE");
    }

    // ---------- calling the catalog ----------

    @Test
    void aGameTheCallerCannotSeeIsTreatedAsMissingAndNothingIsStored() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        doThrow(new NoSuchElementException("Game not found: " + game)).when(catalogClient).requireVisibleGame(game);

        send(post("/api/reactions"), ALICE, "{\"gameId\":%d,\"type\":\"LIKE\"}".formatted(game))
                .andExpect(status().isNotFound());
        send(post("/api/comments"), ALICE, "{\"gameId\":%d,\"body\":\"hello\"}".formatted(game))
                .andExpect(status().isNotFound());
        send(post("/api/library"), ALICE, "{\"gameId\":%d}".formatted(game))
                .andExpect(status().isNotFound());

        assertThat(reactionRepository.findByGameId(game)).isEmpty();
    }

    @Test
    void whenTheCatalogIsDownWritesFailFastWith503AndReadsStillWork() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        react(game, BOB, "LIKE");
        doThrow(new ServiceUnavailableException("game-catalog-service is unavailable"))
                .when(catalogClient).requireVisibleGame(game);

        send(post("/api/reactions"), ALICE, "{\"gameId\":%d,\"type\":\"LIKE\"}".formatted(game))
                .andExpect(status().isServiceUnavailable());
        send(get("/api/reactions?gameId=" + game), ALICE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void readingDoesNotCallTheCatalog() throws Exception {
        long game = NEXT_GAME.incrementAndGet();

        send(get("/api/reactions?gameId=" + game), ALICE, null).andExpect(status().isOk());
        send(get("/api/comments?gameId=" + game), ALICE, null).andExpect(status().isOk());

        verify(catalogClient, never()).requireVisibleGame(game);
    }

    // ---------- ownership ----------

    @Test
    void onlyTheOwnerCanChangeAReactionAndOwnerOrAdminCanRemoveIt() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        long reaction = react(game, ALICE, "LIKE");

        send(put("/api/reactions/" + reaction), BOB, "{\"type\":\"DISLIKE\"}").andExpect(status().isForbidden());
        send(put("/api/reactions/" + reaction), ADMIN, "{\"type\":\"DISLIKE\"}").andExpect(status().isForbidden());
        send(delete("/api/reactions/" + reaction), BOB, null).andExpect(status().isForbidden());

        send(put("/api/reactions/" + reaction), ALICE, "{\"type\":\"DISLIKE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DISLIKE"));
        send(delete("/api/reactions/" + reaction), ADMIN, null).andExpect(status().isNoContent());
        send(delete("/api/reactions/" + reaction), ALICE, null).andExpect(status().isNotFound());
    }

    @Test
    void onlyTheAuthorCanEditACommentButAnAdminCanModerateIt() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        long comment = comment(game, ALICE, "Great puzzle");

        send(put("/api/comments/" + comment), BOB, "{\"body\":\"edited by bob\"}").andExpect(status().isForbidden());
        send(delete("/api/comments/" + comment), BOB, null).andExpect(status().isForbidden());

        send(put("/api/comments/" + comment), ALICE, "{\"body\":\"Great puzzle, edited\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Great puzzle, edited"));

        send(delete("/api/comments/" + comment), ADMIN, null).andExpect(status().isNoContent());
        send(get("/api/comments?gameId=" + game), ALICE, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void commentsAreListedPerGameWithTheirAuthors() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        comment(game, ALICE, "first");
        comment(game, BOB, "second");

        String body = send(get("/api/comments?gameId=" + game), ADMIN, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Integer> authors = JsonPath.read(body, "$[*].userId");
        assertThat(authors).containsExactlyInAnyOrder(11, 12);
    }

    // ---------- paging ----------

    @Test
    void commentListingsArePagedNewestFirst() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        long first = comment(game, ALICE, "first");
        long second = comment(game, BOB, "second");
        long third = comment(game, ALICE, "third");

        String page0 = send(get("/api/comments?gameId=" + game + "&size=2"), ALICE, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String page1 = send(get("/api/comments?gameId=" + game + "&size=2&page=1"), ALICE, null)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat((List<Integer>) JsonPath.read(page0, "$[*].id")).containsExactly((int) third, (int) second);
        assertThat((List<Integer>) JsonPath.read(page1, "$[*].id")).containsExactly((int) first);
    }

    @Test
    void aListingNeverReturnsMoreThanThePageLimitAndBadValuesAreClamped() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        react(game, ALICE, "LIKE");
        react(game, BOB, "DISLIKE");

        send(get("/api/reactions?gameId=" + game + "&size=1"), ALICE, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        send(get("/api/reactions?gameId=" + game + "&size=100000"), ALICE, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        send(get("/api/reactions?gameId=" + game + "&size=0&page=-5"), ALICE, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- library ----------

    @Test
    void aLibraryIsPrivateToItsOwner() throws Exception {
        long game = NEXT_GAME.incrementAndGet();

        send(post("/api/library"), ALICE, "{\"gameId\":%d}".formatted(game))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(11));
        send(post("/api/library"), ALICE, "{\"gameId\":%d}".formatted(game)).andExpect(status().isConflict());

        String alice = send(get("/api/library"), ALICE, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String bob = send(get("/api/library"), BOB, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(alice, "$[*].gameId")).contains((int) game);
        assertThat((List<Integer>) JsonPath.read(bob, "$[*].gameId")).doesNotContain((int) game);
    }

    @Test
    void removingFromTheLibraryOnlyAffectsTheCaller() throws Exception {
        long game = NEXT_GAME.incrementAndGet();
        send(post("/api/library"), ALICE, "{\"gameId\":%d}".formatted(game)).andExpect(status().isCreated());
        send(post("/api/library"), BOB, "{\"gameId\":%d}".formatted(game)).andExpect(status().isCreated());

        send(delete("/api/library/" + game), ALICE, null).andExpect(status().isNoContent());
        send(delete("/api/library/" + game), ALICE, null).andExpect(status().isNoContent());

        String bob = send(get("/api/library"), BOB, null).andReturn().getResponse().getContentAsString();
        assertThat((List<Integer>) JsonPath.read(bob, "$[*].gameId")).contains((int) game);
    }

    // ---------- validation ----------

    @Test
    void invalidInputIsRejected() throws Exception {
        send(post("/api/reactions"), ALICE, "{\"gameId\":1,\"type\":\"MEH\"}").andExpect(status().isBadRequest());
        send(post("/api/reactions"), ALICE, "{\"type\":\"LIKE\"}").andExpect(status().isBadRequest());
        send(post("/api/comments"), ALICE, "{\"gameId\":1,\"body\":\"\"}").andExpect(status().isBadRequest());
        send(post("/api/library"), ALICE, "{}").andExpect(status().isBadRequest());
    }
}
