package edu.arizona.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import edu.arizona.identity.model.entity.PasswordResetToken;
import edu.arizona.identity.repository.PasswordResetTokenRepository;
import edu.arizona.identity.service.MailService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "app.bootstrap-admin.username=admin",
        "app.bootstrap-admin.email=admin@arizona.edu",
        "app.bootstrap-admin.password=Admin-test-12345"
})
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @MockitoBean
    private MailService mailService;

    // ---------- helpers ----------

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long register(String username, String email) throws Exception {
        String body = postJson("/api/auth/register", """
                {"username":"%s","email":"%s","password":"%s"}""".formatted(username, email, PASSWORD))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String login(String username, String password) throws Exception {
        String body = postJson("/api/auth/login", """
                {"username":"%s","password":"%s"}""".formatted(username, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String adminToken() throws Exception {
        return login("admin", "Admin-test-12345");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- registration ----------

    @Test
    void registrationCreatesAPlayerAndNeverReturnsThePassword() throws Exception {
        String username = unique("reg");

        postJson("/api/auth/register", """
                {"username":"%s","email":"%s@arizona.edu","password":"%s"}""".formatted(username, username, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrationCannotChooseARole() throws Exception {
        String username = unique("sneaky");

        postJson("/api/auth/register", """
                {"username":"%s","email":"%s@arizona.edu","password":"%s","role":"ADMIN"}"""
                .formatted(username, username, PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void registrationRejectsDuplicatesAndInvalidInput() throws Exception {
        String username = unique("dup");
        register(username, username + "@arizona.edu");

        postJson("/api/auth/register", """
                {"username":"%s","email":"other-%s@arizona.edu","password":"%s"}""".formatted(username, username, PASSWORD))
                .andExpect(status().isConflict());
        postJson("/api/auth/register", """
                {"username":"%s2","email":"%s@ARIZONA.edu","password":"%s"}""".formatted(username, username, PASSWORD))
                .andExpect(status().isConflict());
        postJson("/api/auth/register", """
                {"username":"x","email":"not-an-email","password":"short"}""")
                .andExpect(status().isBadRequest());
    }

    // ---------- authentication ----------

    @Test
    void loginIssuesATokenThatIdentifiesTheUserAndTheirRole() throws Exception {
        String username = unique("login");
        long id = register(username, username + "@arizona.edu");

        String body = postJson("/api/auth/login", """
                {"username":"%s","password":"%s"}""".formatted(username, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.username").value(username))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");
        var jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(id));
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("PLAYER");

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void loginAlsoAcceptsTheEmailAddress() throws Exception {
        String username = unique("mail");
        register(username, username + "@arizona.edu");

        postJson("/api/auth/login", """
                {"username":"%s@arizona.edu","password":"%s"}""".formatted(username, PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownUserLookIdentical() throws Exception {
        String username = unique("bad");
        register(username, username + "@arizona.edu");

        String wrongPassword = postJson("/api/auth/login", """
                {"username":"%s","password":"wrong-password"}""".formatted(username))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownUser = postJson("/api/auth/login", """
                {"username":"nobody-%s","password":"wrong-password"}""".formatted(username))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void aSuspendedAccountCannotLogInUntilReinstated() throws Exception {
        String username = unique("susp");
        long id = register(username, username + "@arizona.edu");
        String admin = adminToken();

        mockMvc.perform(put("/api/users/" + id + "/status").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk());
        postJson("/api/auth/login", """
                {"username":"%s","password":"%s"}""".formatted(username, PASSWORD))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/" + id + "/status").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        login(username, PASSWORD);
    }

    @Test
    void protectedEndpointsRejectMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- authorization ----------

    @Test
    void aPlayerCannotListUsersButAnAdminCan() throws Exception {
        String username = unique("list");
        register(username, username + "@arizona.edu");
        String player = login(username, PASSWORD);

        mockMvc.perform(get("/api/users").header("Authorization", bearer(player)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/users").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void aUserCanReadAndEditOnlyTheirOwnAccount() throws Exception {
        String a = unique("usera");
        String b = unique("userb");
        long idA = register(a, a + "@arizona.edu");
        long idB = register(b, b + "@arizona.edu");
        String tokenA = login(a, PASSWORD);

        mockMvc.perform(get("/api/users/" + idA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/" + idB).header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/" + idB).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"hijacked\",\"email\":\"hijacked@arizona.edu\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/" + idB).header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/" + idA).header("Authorization", bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"email\":\"%s@arizona.edu\"}".formatted(a, b)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/users/" + idB).header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void aPlayerCannotPromoteThemselvesButAnAdminCanVerifyACreator() throws Exception {
        String username = unique("dev");
        long id = register(username, username + "@arizona.edu");
        String player = login(username, PASSWORD);

        mockMvc.perform(put("/api/users/" + id + "/role").header("Authorization", bearer(player))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/" + id + "/role").header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEVELOPER"));

        // roles live in the token, so the promotion shows up in the next token issued
        var jwt = jwtDecoder.decode(login(username, PASSWORD));
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("DEVELOPER");
    }

    @Test
    void theLastAdministratorCannotBeDeletedOrDemoted() throws Exception {
        String admin = adminToken();
        long adminId = ((Number) JsonPath.read(
                mockMvc.perform(get("/api/users/me").header("Authorization", bearer(admin)))
                        .andReturn().getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(delete("/api/users/" + adminId).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/users/" + adminId + "/role").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"PLAYER\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- password reminder ----------

    private String requestResetLink(String email) throws Exception {
        postJson("/api/auth/forgot-password", "{\"email\":\"%s\"}".formatted(email))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordReset(eq(email), anyString(), link.capture(), anyLong());
        return link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
    }

    @Test
    void forgotPasswordGivesTheSameAnswerForUnknownAddressesAndSendsNothing() throws Exception {
        String known = unique("known");
        register(known, known + "@arizona.edu");

        String knownBody = postJson("/api/auth/forgot-password", "{\"email\":\"%s@arizona.edu\"}".formatted(known))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknownBody = postJson("/api/auth/forgot-password", "{\"email\":\"ghost-%s@arizona.edu\"}".formatted(known))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(knownBody).isEqualTo(unknownBody);
        verify(mailService, never()).sendPasswordReset(eq("ghost-" + known + "@arizona.edu"),
                anyString(), anyString(), anyLong());
    }

    @Test
    void aResetLinkSetsANewPasswordExactlyOnce() throws Exception {
        String username = unique("reset");
        String email = username + "@arizona.edu";
        register(username, email);

        String token = requestResetLink(email);

        postJson("/api/auth/reset-password", """
                {"token":"%s","newPassword":"brand-new-pass-1"}""".formatted(token))
                .andExpect(status().isOk());

        login(username, "brand-new-pass-1");
        postJson("/api/auth/login", """
                {"username":"%s","password":"%s"}""".formatted(username, PASSWORD))
                .andExpect(status().isUnauthorized());

        postJson("/api/auth/reset-password", """
                {"token":"%s","newPassword":"another-pass-2"}""".formatted(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anExpiredOrInventedTokenIsRejected() throws Exception {
        String username = unique("expired");
        String email = username + "@arizona.edu";
        long id = register(username, email);
        String token = requestResetLink(email);

        List<PasswordResetToken> tokens = tokenRepository.findAll().stream()
                .filter(t -> t.getUserId() == id).toList();
        assertThat(tokens).hasSize(1);
        tokens.get(0).setExpiresAt(Instant.now().minusSeconds(60));
        tokenRepository.save(tokens.get(0));

        postJson("/api/auth/reset-password", """
                {"token":"%s","newPassword":"brand-new-pass-1"}""".formatted(token))
                .andExpect(status().isBadRequest());
        postJson("/api/auth/reset-password", """
                {"token":"made-up-token","newPassword":"brand-new-pass-1"}""")
                .andExpect(status().isBadRequest());
        login(username, PASSWORD);
    }

    @Test
    void theResetTokenIsStoredHashedNotInTheClear() throws Exception {
        String username = unique("hash");
        String email = username + "@arizona.edu";
        long id = register(username, email);
        String token = requestResetLink(email);

        PasswordResetToken stored = tokenRepository.findAll().stream()
                .filter(t -> t.getUserId() == id).findFirst().orElseThrow();
        assertThat(stored.getTokenHash()).isNotEqualTo(token).hasSize(64);
    }

    @Test
    void aSecondResetRequestInvalidatesTheFirstLink() throws Exception {
        String username = unique("twice");
        String email = username + "@arizona.edu";
        register(username, email);

        String first = requestResetLink(email);
        postJson("/api/auth/forgot-password", "{\"email\":\"%s\"}".formatted(email)).andExpect(status().isAccepted());

        postJson("/api/auth/reset-password", """
                {"token":"%s","newPassword":"brand-new-pass-1"}""".formatted(first))
                .andExpect(status().isBadRequest());
    }
}
