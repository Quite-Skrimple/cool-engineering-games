package edu.arizona.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "eureka.client.enabled=false")
class GameAccessIntegrationTest {

    private static final String GAME_JSON = """
            {"title":"Circuit Breaker","description":"Logic gates","genre":"Puzzle","playableUrl":"https://example.com/cb"}""";

    private static final String PLAYER = TestTokens.bearer(201, "player", "PLAYER");
    private static final String DEV_A = TestTokens.bearer(101, "devA", "DEVELOPER");
    private static final String DEV_B = TestTokens.bearer(102, "devB", "DEVELOPER");
    private static final String ADMIN = TestTokens.bearer(900, "admin", "ADMIN");

    @Autowired
    private MockMvc mockMvc;

    private long createGame(String token) throws Exception {
        String body = mockMvc.perform(post("/api/games").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void approve(long id) throws Exception {
        mockMvc.perform(put("/api/games/" + id + "/approval").header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk());
    }

    private List<Integer> listedIds(String token, String query) throws Exception {
        String body = mockMvc.perform(get("/api/games" + query).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].id");
    }

    @Test
    void everyEndpointNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/games")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aPlayerCannotSubmitAGameButADeveloperCan() throws Exception {
        mockMvc.perform(post("/api/games").header("Authorization", PLAYER)
                        .contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/games").header("Authorization", DEV_A)
                        .contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
    }

    @Test
    void theCreatorAndStatusComeFromTheTokenNotTheRequestBody() throws Exception {
        mockMvc.perform(post("/api/games").header("Authorization", DEV_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Spoofed","genre":"Puzzle","playableUrl":"https://example.com/x",
                                 "creatorId":999,"approvalStatus":"APPROVED"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creatorId").value(101))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
    }

    @Test
    void aGameWaitingForReviewIsHiddenFromEveryoneButItsCreatorAndAdmins() throws Exception {
        long id = createGame(DEV_A);

        assertThat(listedIds(PLAYER, "")).doesNotContain((int) id);
        assertThat(listedIds(DEV_B, "")).doesNotContain((int) id);
        mockMvc.perform(get("/api/games/" + id).header("Authorization", PLAYER)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/games/" + id).header("Authorization", DEV_B)).andExpect(status().isNotFound());

        assertThat(listedIds(DEV_A, "?mine=true")).contains((int) id);
        mockMvc.perform(get("/api/games/" + id).header("Authorization", DEV_A)).andExpect(status().isOk());
        assertThat(listedIds(ADMIN, "?status=PENDING")).contains((int) id);
        mockMvc.perform(get("/api/games/" + id).header("Authorization", ADMIN)).andExpect(status().isOk());
    }

    @Test
    void onlyAnAdminCanApproveAndApprovalPublishesTheGame() throws Exception {
        long id = createGame(DEV_A);

        mockMvc.perform(put("/api/games/" + id + "/approval").header("Authorization", DEV_A)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());

        approve(id);

        assertThat(listedIds(PLAYER, "")).contains((int) id);
        mockMvc.perform(get("/api/games/" + id).header("Authorization", PLAYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }

    @Test
    void aPlayerCannotListOtherStatusesEvenByAskingForThem() throws Exception {
        long id = createGame(DEV_A);

        assertThat(listedIds(PLAYER, "?status=PENDING")).isEmpty();
        assertThat(listedIds(PLAYER, "?status=PENDING")).doesNotContain((int) id);
    }

    @Test
    void onlyTheCreatorOrAnAdminCanEditAGame() throws Exception {
        long id = createGame(DEV_A);
        approve(id);

        mockMvc.perform(put("/api/games/" + id).header("Authorization", DEV_B)
                        .contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/games/" + id).header("Authorization", PLAYER)
                        .contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void editingAnApprovedGameSendsItBackForReviewUnlessAnAdminDidIt() throws Exception {
        long id = createGame(DEV_A);
        approve(id);

        mockMvc.perform(put("/api/games/" + id).header("Authorization", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Renamed by admin","genre":"Puzzle","playableUrl":"https://example.com/cb"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        mockMvc.perform(put("/api/games/" + id).header("Authorization", DEV_A)
                        .contentType(MediaType.APPLICATION_JSON).content(GAME_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
        assertThat(listedIds(PLAYER, "")).doesNotContain((int) id);
    }

    @Test
    void onlyTheCreatorOrAnAdminCanDeleteAGame() throws Exception {
        long mine = createGame(DEV_A);
        approve(mine);
        long hidden = createGame(DEV_A);

        mockMvc.perform(delete("/api/games/" + mine).header("Authorization", DEV_B)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/games/" + hidden).header("Authorization", DEV_B)).andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/games/" + mine).header("Authorization", DEV_A)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/games/" + hidden).header("Authorization", ADMIN)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/games/" + mine).header("Authorization", ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void invalidGamesAndUnknownIdsAreRejected() throws Exception {
        mockMvc.perform(post("/api/games").header("Authorization", DEV_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"genre\":\"Puzzle\",\"playableUrl\":\"https://example.com/x\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/games/999999").header("Authorization", ADMIN)).andExpect(status().isNotFound());
    }
}
