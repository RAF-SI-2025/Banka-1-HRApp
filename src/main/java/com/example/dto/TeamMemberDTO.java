package com.example.dto;

import com.example.model.TaskStatus;

import java.util.List;

/**
 * Read-only data transfer object representing a team member with all related data.
 * Used to pass member data from the service layer to the view layer.
 * <p>
 * Average grade is calculated as the average of grades across completed tasks only.
 */
public class TeamMemberDTO {

    private final long id;
    private final String name;
    private final String surname;
    private final double averageGrade;
    private final List<TaskDTO> tasks;
    private final List<String> skills;

    /**
     * Constructs a TeamMemberDTO with all fields.
     *
     * @param id           the member's database ID
     * @param name         first name
     * @param surname      last name
     * @param averageGrade average grade across completed tasks (0 if none graded)
     * @param tasks        list of task DTOs (each carrying its own grade)
     * @param skills       list of skill names
     */
    public TeamMemberDTO(long id, String name, String surname, double averageGrade,
                         List<TaskDTO> tasks, List<String> skills) {
        this.id           = id;
        this.name         = name;
        this.surname      = surname;
        this.averageGrade = averageGrade;
        this.tasks        = List.copyOf(tasks);
        this.skills       = List.copyOf(skills);
    }

    /** @return the number of tasks currently in PENDING status */
    public long getPendingCount() {
        return tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
    }

    /** @return the number of tasks with COMPLETED status */
    public long getCompletedCount() {
        return tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
    }

    /** @return the number of tasks with FAILED status */
    public long getFailedCount() {
        return tasks.stream().filter(t -> t.getStatus() == TaskStatus.FAILED).count();
    }

    /** @return the member's database ID */
    public long getId() { return id; }

    /** @return the member's first name */
    public String getName() { return name; }

    /** @return the member's last name */
    public String getSurname() { return surname; }

    /** @return average grade across completed graded tasks, or 0 if none */
    public double getAverageGrade() { return averageGrade; }

    /** @return an unmodifiable list of task DTOs */
    public List<TaskDTO> getTasks() { return tasks; }

    /** @return an unmodifiable list of skill names */
    public List<String> getSkills() { return skills; }
}
