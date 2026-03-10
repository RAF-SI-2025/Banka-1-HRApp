package com.example.service;

import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.exception.ValidationException;
import com.example.model.Task;
import com.example.model.TaskStatus;
import com.example.repository.GradeRepository;
import com.example.repository.TaskRepository;
import com.example.repository.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskService}.
 * All repository interactions are mocked — no real database is used.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository     taskRepo;
    @Mock private GradeRepository    gradeRepo;
    @Mock private TransactionManager txManager;

    private TaskService service;

    /** Sets up the service with mocked repositories before each test. */
    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepo, gradeRepo, txManager);
    }

    // ── addTask ───────────────────────────────────────────────────────────────

    @Test
    void addTask_withValidData_persistsAndReturnsDTO() throws SQLException {
        doAnswer(inv -> { ((Task) inv.getArgument(0)).setId(10L); return null; })
                .when(taskRepo).save(any(), anyLong());

        TaskDTO result = service.addTask(1L, CreateUpdateTaskDTO.of("Implement login", "Do OAuth", TaskStatus.PENDING));

        assertEquals("Implement login", result.getTaskName());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertEquals("Do OAuth",        result.getComment());
        assertEquals(10L,               result.getId());
        verify(taskRepo).save(any(Task.class), eq(1L));
    }

    @Test
    void addTask_withBlankName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.addTask(1L, CreateUpdateTaskDTO.of("", "comment", TaskStatus.PENDING)));
    }

    @Test
    void addTask_withNullStatus_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.addTask(1L, CreateUpdateTaskDTO.of("Task", "comment", null)));
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Test
    void updateTask_updatesAllFieldsAndReturnsDTO() throws SQLException {
        when(gradeRepo.findByTaskId(5L)).thenReturn(0);
        CreateUpdateTaskDTO dto = CreateUpdateTaskDTO.of("Updated name", "New comment", TaskStatus.COMPLETED);

        TaskDTO result = service.updateTask(5L, dto);

        assertEquals("Updated name",        result.getTaskName());
        assertEquals(TaskStatus.COMPLETED,  result.getStatus());
        assertEquals("New comment",         result.getComment());
        verify(taskRepo).update(any(Task.class));
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    void deleteTask_callsSoftDelete() throws SQLException {
        service.deleteTask(7L);

        verify(taskRepo).softDelete(7L);
    }

    // ── getTasksForMember ─────────────────────────────────────────────────────

    @Test
    void getTasksForMember_whenNoTasks_returnsEmptyList() throws SQLException {
        when(taskRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());

        List<TaskDTO> result = service.getTasksForMember(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTasksForMember_mapsToDTOs() throws SQLException {
        Task task = new Task("Fix bug");
        task.setId(3L);
        task.setStatus(TaskStatus.COMPLETED);
        task.setComment("Fixed in PR #42");
        when(taskRepo.findByMemberId(1L)).thenReturn(List.of(task));
        when(gradeRepo.findByTaskId(3L)).thenReturn(7);

        List<TaskDTO> result = service.getTasksForMember(1L);

        assertEquals(1,                    result.size());
        assertEquals("Fix bug",            result.get(0).getTaskName());
        assertEquals(TaskStatus.COMPLETED, result.get(0).getStatus());
        assertEquals("Fixed in PR #42",   result.get(0).getComment());
        assertEquals(7,                    result.get(0).getGrade());
    }

    // ── gradeTask ─────────────────────────────────────────────────────────────

    @Test
    void gradeTask_withValidGrade_callsSaveOrUpdate() throws SQLException {
        Task completedTask = new Task("Some task");
        completedTask.setId(5L);
        completedTask.setStatus(TaskStatus.COMPLETED);
        when(taskRepo.findById(5L)).thenReturn(java.util.Optional.of(completedTask));

        service.gradeTask(5L, 8);

        verify(gradeRepo).saveOrUpdate(8, 5L);
    }

    @Test
    void gradeTask_withMinGrade_callsSaveOrUpdate() throws SQLException {
        Task completedTask = new Task("Some task");
        completedTask.setId(1L);
        completedTask.setStatus(TaskStatus.COMPLETED);
        when(taskRepo.findById(1L)).thenReturn(java.util.Optional.of(completedTask));

        service.gradeTask(1L, 1);

        verify(gradeRepo).saveOrUpdate(1, 1L);
    }

    @Test
    void gradeTask_withMaxGrade_callsSaveOrUpdate() throws SQLException {
        Task completedTask = new Task("Some task");
        completedTask.setId(1L);
        completedTask.setStatus(TaskStatus.COMPLETED);
        when(taskRepo.findById(1L)).thenReturn(java.util.Optional.of(completedTask));

        service.gradeTask(1L, 10);

        verify(gradeRepo).saveOrUpdate(10, 1L);
    }

    @Test
    void gradeTask_withGradeTooHigh_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.gradeTask(1L, 11));
    }

    @Test
    void gradeTask_withGradeTooLow_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.gradeTask(1L, 0));
    }

    @Test
    void gradeTask_withTooHighGrade_doesNotCallRepository() throws SQLException {
        assertThrows(ValidationException.class, () -> service.gradeTask(1L, 11));

        verify(gradeRepo, never()).saveOrUpdate(anyInt(), anyLong());
    }
}
