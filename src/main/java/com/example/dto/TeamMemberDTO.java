package com.example.dto;

import com.example.model.TaskStatus;

import java.util.List;

/**
 * Read-only data transfer object representing a team member with all related data.
 * Used to pass member data from the service layer to the view layer.
 */
public class TeamMemberDTO {

    private final long id;
    private final String name;
    private final String surname;
    private final double averageGrade;
    private final List<TaskDTO> tasks;
    private final List<String> skills;
    private final List<Integer> grades;

    /**
     * Constructs a TeamMemberDTO with all fields.
     *
     * @param id           the member's database ID
     * @param name         first name
     * @param surname      last name
     * @param averageGrade pre-calculated average grade (0 if no grades)
     * @param tasks        list of task DTOs
     * @param skills       list of skill names
     * @param grades       list of individual grade values
     */
    public TeamMemberDTO(long id, String name, String surname, double averageGrade,
                         List<TaskDTO> tasks, List<String> skills, List<Integer> grades) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.averageGrade = averageGrade;
        this.tasks = List.copyOf(tasks);
        this.skills = List.copyOf(skills);
        this.grades = List.copyOf(grades);
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

    /** @return the pre-calculated average grade, or 0 if no grades exist */
    public double getAverageGrade() { return averageGrade; }

    /** @return an unmodifiable list of task DTOs */
    public List<TaskDTO> getTasks() { return tasks; }

    /** @return an unmodifiable list of skill names */
    public List<String> getSkills() { return skills; }

    /** @return an unmodifiable list of grade values */
    public List<Integer> getGrades() { return grades; }
}
