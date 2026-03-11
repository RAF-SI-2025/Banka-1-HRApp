package com.example.model;

import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for model and DTO classes to cover uncovered methods.
 */
class ModelAndDtoTest {

    // ── Task model ────────────────────────────────────────────────────────────

    @Test
    void task_constructorWithTeamMember_setsFields() {
        TeamMember member = new TeamMember("Ana", "Jovic");
        Task task = new Task("Do work", member);

        assertEquals("Do work", task.getTaskName());
        assertEquals(member, task.getTeamMember());
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void task_constructorWithCommentAndTeamMember_setsFields() {
        TeamMember member = new TeamMember("Ana", "Jovic");
        Task task = new Task("Do work", "Some comment", member);

        assertEquals("Do work",      task.getTaskName());
        assertEquals("Some comment", task.getComment());
        assertEquals(member,         task.getTeamMember());
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void task_setTaskName_updatesValue() {
        Task task = new Task("Original");
        task.setTaskName("Updated");
        assertEquals("Updated", task.getTaskName());
    }

    @Test
    void task_setTeamMember_updatesValue() {
        Task task = new Task("Work");
        TeamMember member = new TeamMember("Nikola", "Tesla");
        task.setTeamMember(member);
        assertEquals(member, task.getTeamMember());
    }

    // ── TeamMemberDTO count helpers ───────────────────────────────────────────

    @Test
    void teamMemberDTO_getPendingCount_correctCount() {
        List<TaskDTO> tasks = List.of(
                new TaskDTO(1L, "T1", TaskStatus.PENDING,   "", 0),
                new TaskDTO(2L, "T2", TaskStatus.PENDING,   "", 0),
                new TaskDTO(3L, "T3", TaskStatus.COMPLETED, "", 0)
        );
        TeamMemberDTO dto = new TeamMemberDTO(1L, "A", "B", 0.0, tasks, Collections.emptyList());

        assertEquals(2, dto.getPendingCount());
    }

    @Test
    void teamMemberDTO_getCompletedCount_correctCount() {
        List<TaskDTO> tasks = List.of(
                new TaskDTO(1L, "T1", TaskStatus.COMPLETED, "", 0),
                new TaskDTO(2L, "T2", TaskStatus.PENDING,   "", 0),
                new TaskDTO(3L, "T3", TaskStatus.COMPLETED, "", 0)
        );
        TeamMemberDTO dto = new TeamMemberDTO(1L, "A", "B", 0.0, tasks, Collections.emptyList());

        assertEquals(2, dto.getCompletedCount());
    }

    @Test
    void teamMemberDTO_getFailedCount_correctCount() {
        List<TaskDTO> tasks = List.of(
                new TaskDTO(1L, "T1", TaskStatus.FAILED,  "", 0),
                new TaskDTO(2L, "T2", TaskStatus.PENDING, "", 0),
                new TaskDTO(3L, "T3", TaskStatus.FAILED,  "", 0)
        );
        TeamMemberDTO dto = new TeamMemberDTO(1L, "A", "B", 0.0, tasks, Collections.emptyList());

        assertEquals(2, dto.getFailedCount());
    }

    @Test
    void teamMemberDTO_countHelpers_withNoTasks_returnZero() {
        TeamMemberDTO dto = new TeamMemberDTO(1L, "A", "B", 0.0,
                Collections.emptyList(), Collections.emptyList());

        assertEquals(0, dto.getPendingCount());
        assertEquals(0, dto.getCompletedCount());
        assertEquals(0, dto.getFailedCount());
    }

    // ── CreateUpdateMemberDTO max-length validation ───────────────────────────

    @Test
    void createUpdateMemberDTO_nameTooLong_throwsValidationException() {
        String longName = "A".repeat(101);
        assertThrows(ValidationException.class,
                () -> CreateUpdateMemberDTO.of(longName, "Surname"));
    }

    @Test
    void createUpdateMemberDTO_surnameTooLong_throwsValidationException() {
        String longSurname = "A".repeat(101);
        assertThrows(ValidationException.class,
                () -> CreateUpdateMemberDTO.of("Name", longSurname));
    }

    // ── CreateUpdateTaskDTO max-length validation ─────────────────────────────

    @Test
    void createUpdateTaskDTO_taskNameTooLong_throwsValidationException() {
        String longName = "A".repeat(201);
        assertThrows(ValidationException.class,
                () -> CreateUpdateTaskDTO.of(longName, "", TaskStatus.PENDING));
    }

    @Test
    void createUpdateTaskDTO_commentTooLong_throwsValidationException() {
        String longComment = "A".repeat(501);
        assertThrows(ValidationException.class,
                () -> CreateUpdateTaskDTO.of("Task", longComment, TaskStatus.PENDING));
    }
}

