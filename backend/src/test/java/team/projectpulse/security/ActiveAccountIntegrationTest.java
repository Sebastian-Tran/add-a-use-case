package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.student.Student;
import team.projectpulse.student.StudentRepository;
import team.projectpulse.system.StatusCode;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account: a student deactivated while logged in loses access on her next request, without waiting for
 * her token to expire (UC-STU-deactivate-student, Associated Information; stu.md T8).
 */
@DisplayName("Integration tests for refusing deactivated accounts")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StudentRepository studentRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String johnToken;

    Integer johnId;


    @BeforeEach
    void setUp() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456"))).andReturn();
        JSONObject data = new JSONObject(mvcResult.getResponse().getContentAsString()).getJSONObject("data");
        this.johnToken = "Bearer " + data.getString("token");
        this.johnId = data.getJSONObject("userInfo").getInt("id");
    }

    @Test
    void testTokenOfActiveStudentIsAccepted() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.johnId).header(HttpHeaders.AUTHORIZATION, this.johnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));
    }

    @Test
    void testTokenIssuedBeforeDeactivationIsRefused() throws Exception {
        deactivateJohn();

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.johnId).header(HttpHeaders.AUTHORIZATION, this.johnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testFreshLoginOfDeactivatedStudentIsRefused() throws Exception {
        deactivateJohn();

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    private void deactivateJohn() {
        Student john = this.studentRepository.findById(this.johnId).orElseThrow();
        john.setEnabled(false);
        this.studentRepository.saveAndFlush(john);
    }

}
