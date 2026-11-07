package team.projectpulse.student;

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
import team.projectpulse.system.StatusCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UC-STU-deactivate-student: {@code PUT /students/{studentId}/deactivation}. The tests are named after the rows of
 * the test list in {@code docs/design/stu.md}.
 *
 * <p>Seed data: John (id 4) is in course section 2023-2024 of course COSC 40993, whose course admin is Bingyang Wei
 * and whose instructors are Bingyang Wei and Bill Gates. Tim Cook is course admin of another course.
 */
@DisplayName("Integration tests for deactivating a student")
class StudentDeactivationIntegrationTest extends AbstractIntegrationTest {

    static final int JOHN_ID = 4;

    static final int NO_SUCH_ID = 999999;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StudentRepository studentRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String adminBingyangToken;

    String instructorBillToken;

    String adminTimToken;

    String studentJohnToken;


    @BeforeEach
    void setUp() throws Exception {
        this.adminBingyangToken = login("b.wei@abc.edu");
        this.instructorBillToken = login("b.gates@abc.edu");
        this.adminTimToken = login("t.cook@abc.edu");
        this.studentJohnToken = login("j.smith@abc.edu");
    }

    private String login(String email) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456"))).andReturn();
        return "Bearer " + new JSONObject(mvcResult.getResponse().getContentAsString()).getJSONObject("data").getString("token");
    }

    @Test
    @DisplayName("T7: an instructor who is neither her instructor nor her course admin is refused")
    void testAdminTimOfAnotherCourseCannotDeactivateJohn() throws Exception {
        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertThat(this.studentRepository.findById(JOHN_ID).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("T7: the student herself is refused")
    void testStudentJohnCannotDeactivateHimself() throws Exception {
        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertThat(this.studentRepository.findById(JOHN_ID).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("T7: an unknown id is refused as 403, revealing nothing about whether she exists")
    void testDeactivateUnknownStudent() throws Exception {
        this.mockMvc.perform(put(this.baseUrl + "/students/" + NO_SUCH_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.adminBingyangToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
    }

}
