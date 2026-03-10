package com.example.integration;


import com.example.config.AppConfig;
import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.dto.TeamMemberDTO;
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
        assertNull(task.getGrade());
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

    @Test
    void addTask_withCompletedStatus_throwsValidationException() {
        long memberId = createMember("A", "B");

        assertThrows(ValidationException.class,
                () -> taskService.addTask(memberId, CreateUpdateTaskDTO.of("Task", "", TaskStatus.COMPLETED)));
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
        // mark one as completed via updateTask
        TaskDTO t = taskService.addTask(memberId, CreateUpdateTaskDTO.of("Task C", "", TaskStatus.PENDING));
        taskService.updateTask(memberId, t.getId(), CreateUpdateTaskDTO.of("Task C", "", TaskStatus.COMPLETED), 7);

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
    void updateTask_toCompleted_savesGrade() {
        long memberId = createMember("Update", "Task");
        TaskDTO original = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Original", "Old comment", TaskStatus.PENDING));

        TaskDTO updated = taskService.updateTask(memberId, original.getId(),
                CreateUpdateTaskDTO.of("Updated name", "New comment", TaskStatus.COMPLETED), 8);

        assertEquals("Updated name",       updated.getTaskName());
        assertEquals("New comment",        updated.getComment());
        assertEquals(TaskStatus.COMPLETED, updated.getStatus());
        assertEquals(8,                    updated.getGrade());

        TaskDTO reloaded = taskService.getTasksForMember(memberId).get(0);
        assertEquals(TaskStatus.COMPLETED, reloaded.getStatus());
        assertEquals(8,                    reloaded.getGrade());
    }

    @Test
    void updateTask_fromCompletedToPending_removesGrade() {
        long memberId = createMember("Reopen", "Task");
        TaskDTO task = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Task", "", TaskStatus.PENDING));
        taskService.updateTask(memberId, task.getId(),
                CreateUpdateTaskDTO.of("Task", "", TaskStatus.COMPLETED), 9);

        taskService.updateTask(memberId, task.getId(),
                CreateUpdateTaskDTO.of("Task", "", TaskStatus.PENDING), null);

        TaskDTO reloaded = taskService.getTasksForMember(memberId).get(0);
        assertEquals(TaskStatus.PENDING, reloaded.getStatus());
        assertNull(reloaded.getGrade());
    }

    @Test
    void updateTask_failedStatus_automaticallyGetsGradeMin() {
        long memberId = createMember("Fail", "Task");
        TaskDTO task = taskService.addTask(memberId,
                CreateUpdateTaskDTO.of("Task", "", TaskStatus.PENDING));

        TaskDTO updated = taskService.updateTask(memberId, task.getId(),
                CreateUpdateTaskDTO.of("Task", "", TaskStatus.FAILED), null);

        assertEquals(TaskStatus.FAILED,               updated.getStatus());
        assertEquals(AppConfig.getGradeMin(),          updated.getGrade());
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
        assertEquals("Inline task",      all.get(0).getTasks().get(0).getTaskName());
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

