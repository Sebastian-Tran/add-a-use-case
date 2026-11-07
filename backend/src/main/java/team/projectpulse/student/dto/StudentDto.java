package team.projectpulse.student.dto;

import jakarta.validation.constraints.NotEmpty;

public record StudentDto(Integer id,
                         @NotEmpty(message = "username is required.")
                         String username,
                         @NotEmpty(message = "first rubricName is required.")
                         String firstName,
                         @NotEmpty(message = "last rubricName is required.")
                         String lastName,
                         @NotEmpty(message = "email is required.")
                         String email,
                         boolean enabled,
                         String roles,
                         Integer sectionId,
                         String sectionName,
                         Integer teamId,
                         String teamName,
                         // Her deactivation week as a week key, or null while she is active. Derived on the server so
                         // the SPA does no date arithmetic; it is never read back from a request.
                         String deactivatedWeek) {
}
