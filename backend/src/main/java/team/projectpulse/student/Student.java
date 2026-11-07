package team.projectpulse.student;

import team.projectpulse.section.Section;
import team.projectpulse.team.Team;
import team.projectpulse.user.PeerEvaluationUser;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Student extends PeerEvaluationUser {

    @ManyToOne(optional = false)
    private Section section;
    @ManyToOne
    private Team team;
    /**
     * The calendar date on which she was deactivated, or null while she is active (BR-student-lifecycle). The week
     * it falls in is her deactivation week, the first week in which she is no longer an active teammate.
     */
    private LocalDate deactivatedOn;


    public Student() {
    }

    public Student(String username, String firstName, String lastName, String email, String password, boolean enabled, String roles) {
        super(username, firstName, lastName, email, password, enabled, roles);
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public LocalDate getDeactivatedOn() {
        return deactivatedOn;
    }

    public void setDeactivatedOn(LocalDate deactivatedOn) {
        this.deactivatedOn = deactivatedOn;
    }

}
