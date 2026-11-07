package team.projectpulse.student;

import team.projectpulse.section.Section;
import team.projectpulse.system.WeekKeys;
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

    /**
     * Her deactivation week, the week key of {@link #deactivatedOn}, or null while she is active.
     */
    public String getDeactivationWeek() {
        return this.deactivatedOn == null ? null : WeekKeys.of(this.deactivatedOn);
    }

    /**
     * Whether she is no longer an active teammate in the given week: her deactivation week is that week or an earlier
     * one (BR-student-lifecycle). Every week rule about a deactivated student uses this one comparison, so teammates
     * may still evaluate her for the week before her deactivation, and the report shows her for the weeks before it.
     * Week keys share the {@code YYYY-Www} format, so they compare as strings.
     */
    public boolean isInactiveIn(String week) {
        String deactivationWeek = getDeactivationWeek();
        return deactivationWeek != null && week.compareTo(deactivationWeek) >= 0;
    }

}
