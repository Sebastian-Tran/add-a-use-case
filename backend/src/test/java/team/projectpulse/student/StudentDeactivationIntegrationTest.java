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
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.StatusCode;

import java.time.LocalDate;

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
    @DisplayName("T1: an instructor of her course section deactivates her; she stays on her team")
    void testInstructorBillDeactivatesJohn() throws Exception {
        // The dev clock is fixed at 2023-08-20, a Sunday in 2023-W33
        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(JOHN_ID))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.teamName").value("Team1"))
                .andExpect(jsonPath("$.data.deactivatedWeek").value("2023-W33"));

        Student john = this.studentRepository.findById(JOHN_ID).orElseThrow();
        assertThat(john.isEnabled()).isFalse();
        assertThat(john.getDeactivatedOn()).isEqualTo(LocalDate.of(2023, 8, 20));
        assertThat(john.getTeam().getTeamName()).isEqualTo("Team1");
    }

    @Test
    @DisplayName("T3: the course admin of her course may deactivate her without being assigned to her course section")
    void testCourseAdminBingyangNotAssignedToHerSectionDeactivatesJohn() throws Exception {
        // The seed assigns Bingyang to John's course section; take him off it so only his course admin role can admit him
        Student john = this.studentRepository.findById(JOHN_ID).orElseThrow();
        Section section = john.getSection();
        Instructor bingyang = section.getInstructors().stream().filter(i -> i.getEmail().equals("b.wei@abc.edu")).findFirst().orElseThrow();
        section.removeInstructor(bingyang);
        this.studentRepository.flush();

        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.adminBingyangToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("T6: deactivating her again is refused and keeps her recorded date (6a)")
    void testDeactivateAlreadyDeactivatedStudent() throws Exception {
        Student john = this.studentRepository.findById(JOHN_ID).orElseThrow();
        john.setEnabled(false);
        john.setDeactivatedOn(LocalDate.of(2023, 8, 2));
        this.studentRepository.saveAndFlush(john);

        this.mockMvc.perform(put(this.baseUrl + "/students/" + JOHN_ID + "/deactivation").header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("This student is already deactivated."));

        assertThat(this.studentRepository.findById(JOHN_ID).orElseThrow().getDeactivatedOn()).isEqualTo(LocalDate.of(2023, 8, 2));
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
