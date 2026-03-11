package com.example.integration;


import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.HRAppException;
import com.example.exception.ValidationException;
import com.example.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link com.example.service.TaskService}.
 * Uses a real in-memory SQLite database — no mocks.
 */
class TaskServiceIntegrationTest extends IntegrationTestBase {

    /** Creates a member and returns their id — helper to reduce boilerplate. */
    private long createMember(String name, String surname) {
        return memberService.createMember(CreateUpdateMemberDTO.of(name, surname)).getId();
    }

    // ── addTask ───────────────────────────────────────────────────────────────

    @Test
    void addTask_persistsAndReturnsDTO() {
        long memberId = createMember("Ana", "Jovic");

        TaskDTO task = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Write tests", "Cover all branches", TaskStatus.PENDING));

        assertTrue(task.getId() > 0);
        assertEquals("Write tests",        task.getTaskName());
        assertEquals("Cover all branches", task.getComment());
        assertEquals(TaskStatus.PENDING,   task.getStatus());
    }

    @Test
    void addTask_withBlankName_throwsValidationException() {
        long memberId = createMember("A", "B");

        assertThrows(ValidationException.class,
                () -> taskService.addTask(memberId, CreateUpdateTaskDTO.of("", "c", TaskStatus.PENDING)));
    }

    @Test
    void addTask_withNullStatus_throwsValidationException() {
        long memberId = createMember("A", "B");

        assertThrows(ValidationException.class,
                () -> taskService.addTask(memberId, CreateUpdateTaskDTO.of("Task", "c", null)));
    }

    // ── getTasksForMember ─────────────────────────────────────────────────────

    @Test
    void getTasksForMember_whenNoTasks_returnsEmptyList() {
        long memberId = createMember("Empty", "Member");

        assertTrue(taskService.getTasksForMember(memberId).isEmpty());
    }

    @Test
    void getTasksForMember_returnsAllActiveTasks() {
        long memberId = createMember("Multi", "Tasks");
        taskService.addTask(memberId, CreateUpdateTaskDTO.of("Task A", "", TaskStatus.PENDING));
        taskService.addTask(memberId, CreateUpdateTaskDTO.of("Task B", "", TaskStatus.FAILED));
        taskService.addTask(memberId, CreateUpdateTaskDTO.of("Task C", "", TaskStatus.COMPLETED));

        List<TaskDTO> tasks = taskService.getTasksForMember(memberId);

        assertEquals(3, tasks.size());
    }

    @Test
    void getTasksForMember_doesNotReturnDeletedTasks() {
        long memberId = createMember("Delete", "Tasks");
        TaskDTO task = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Gone task", "", TaskStatus.PENDING));

        taskService.deleteTask(task.getId());

        assertTrue(taskService.getTasksForMember(memberId).isEmpty());
    }

    @Test
    void getTasksForMember_doesNotReturnTasksOfOtherMembers() {
        long member1 = createMember("Member", "One");
        long member2 = createMember("Member", "Two");
        taskService.addTask(member1, CreateUpdateTaskDTO.of("Member1 task", "", TaskStatus.PENDING));
        taskService.addTask(member2, CreateUpdateTaskDTO.of("Member2 task", "", TaskStatus.PENDING));

        List<TaskDTO> tasks = taskService.getTasksForMember(member1);

        assertEquals(1, tasks.size());
        assertEquals("Member1 task", tasks.get(0).getTaskName());
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Test
    void updateTask_changesAllFields() {
        long memberId = createMember("Update", "Task");
        TaskDTO original = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Original", "Old comment", TaskStatus.PENDING));

        TaskDTO updated = taskService.updateTask(original.getId(),
                CreateUpdateTaskDTO.of("Updated name", "New comment", TaskStatus.COMPLETED));

        assertEquals("Updated name",       updated.getTaskName());
        assertEquals("New comment",        updated.getComment());
        assertEquals(TaskStatus.COMPLETED, updated.getStatus());

        // Verify the change is persisted
        List<TaskDTO> tasks = taskService.getTasksForMember(memberId);
        assertEquals("Updated name", tasks.get(0).getTaskName());
        assertEquals(TaskStatus.COMPLETED, tasks.get(0).getStatus());
    }

    @Test
    void updateTask_allStatuses_persist() {
        long memberId = createMember("Status", "Test");
        TaskDTO task = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Status task", "", TaskStatus.PENDING));

        for (TaskStatus status : TaskStatus.values()) {
            TaskDTO updated = taskService.updateTask(task.getId(),
                    CreateUpdateTaskDTO.of("Status task", "", status));
            assertEquals(status, updated.getStatus());
        }
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    void deleteTask_softDeletesTask() {
        long memberId = createMember("Soft", "Delete");
        TaskDTO task = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("To delete", "", TaskStatus.PENDING));

        taskService.deleteTask(task.getId());

        assertTrue(taskService.getTasksForMember(memberId).isEmpty());
    }

    @Test
    void deleteTask_onlyDeletesSpecifiedTask() {
        long memberId = createMember("Partial", "Delete");
        TaskDTO keep   = taskService.addTask(memberId, CreateUpdateTaskDTO.of("Keep me",   "", TaskStatus.PENDING));
        TaskDTO remove = taskService.addTask(memberId, CreateUpdateTaskDTO.of("Remove me", "", TaskStatus.PENDING));

        taskService.deleteTask(remove.getId());

        List<TaskDTO> remaining = taskService.getTasksForMember(memberId);
        assertEquals(1, remaining.size());
        assertEquals(keep.getId(), remaining.get(0).getId());
    }

    // ── getAllMembers shows tasks inline ──────────────────────────────────────

    @Test
    void findAllWithDetails_returnsTasksForMember() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Inline", "Tasks"));
        taskService.addTask(member.getId(),
                CreateUpdateTaskDTO.of("Inline task", "desc", TaskStatus.PENDING));

        List<TeamMemberDTO> all = memberService.getAllMembers();

        assertEquals(1, all.size());
        assertEquals(1, all.get(0).getTasks().size());
        assertEquals("Inline task", all.get(0).getTasks().get(0).getTaskName());
        assertEquals(TaskStatus.PENDING, all.get(0).getTasks().get(0).getStatus());
    }

    // ── Transaction: deleteMember cascades task soft-delete ──────────────────

    @Test
    void deleteMember_withinTransaction_softDeletesBothMemberAndTasks() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Trans", "Action"));
        taskService.addTask(member.getId(), CreateUpdateTaskDTO.of("Task 1", "", TaskStatus.PENDING));
        taskService.addTask(member.getId(), CreateUpdateTaskDTO.of("Task 2", "", TaskStatus.PENDING));

        memberService.deleteMember(member.getId());

        assertTrue(memberService.getAllMembers().isEmpty());
        assertTrue(taskService.getTasksForMember(member.getId()).isEmpty());
    }
}

