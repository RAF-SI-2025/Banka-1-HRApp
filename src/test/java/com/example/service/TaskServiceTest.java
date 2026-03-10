package com.example.service;

import com.example.config.AppConfig;
import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.exception.HRAppException;
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
import java.util.Optional;

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

    @BeforeEach
    void setUp() {
        service = new TaskService(taskRepo, gradeRepo, txManager);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Creates a persisted Task stub with the given id and status. */
    private Task taskStub(long id, TaskStatus status) {
        Task t = new Task("Task " + id);
        t.setId(id);
        t.setStatus(status);
        t.setComment("");
        return t;
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

    @Test
    void addTask_withCompletedStatus_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.addTask(1L, CreateUpdateTaskDTO.of("Task", "comment", TaskStatus.COMPLETED)));
    }

    @Test
    void addTask_withFailedStatus_savesGradeMinAndReturnsDTO() throws SQLException {
        doAnswer(inv -> { ((Task) inv.getArgument(0)).setId(10L); return null; })
                .when(taskRepo).save(any(), anyLong());

        TaskDTO result = service.addTask(1L, CreateUpdateTaskDTO.of("Failed task", "", TaskStatus.FAILED));

        assertEquals(TaskStatus.FAILED,        result.getStatus());
        assertEquals(AppConfig.getGradeMin(),  result.getGrade());
        verify(gradeRepo).saveForTask(AppConfig.getGradeMin(), 1L, 10L);
        verify(txManager).commit();
    }

    @Test
    void addTask_onSQLException_throwsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(taskRepo).save(any(), anyLong());
        assertThrows(HRAppException.class,
                () -> service.addTask(1L, CreateUpdateTaskDTO.of("Task", "", TaskStatus.PENDING)));
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Test
    void updateTask_toCompleted_savesGradeAndReturnsDTO() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.PENDING)));
        CreateUpdateTaskDTO dto = CreateUpdateTaskDTO.of("Done task", "", TaskStatus.COMPLETED);

        TaskDTO result = service.updateTask(1L, 5L, dto, 8);

        assertEquals("Done task",         result.getTaskName());
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        assertEquals(8,                    result.getGrade());
        verify(gradeRepo).saveForTask(8, 1L, 5L);
        verify(txManager).commit();
        verify(txManager, never()).rollback();
    }

    @Test
    void updateTask_alreadyCompleted_updatesGrade() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.COMPLETED)));
        CreateUpdateTaskDTO dto = CreateUpdateTaskDTO.of("Done task", "", TaskStatus.COMPLETED);

        service.updateTask(1L, 5L, dto, 9);

        verify(gradeRepo).updateByTaskId(5L, 9);
        verify(gradeRepo, never()).saveForTask(anyInt(), anyLong(), anyLong());
    }

    @Test
    void updateTask_fromCompletedToPending_deletesGrade() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.COMPLETED)));
        CreateUpdateTaskDTO dto = CreateUpdateTaskDTO.of("Back to pending", "", TaskStatus.PENDING);

        service.updateTask(1L, 5L, dto, null);

        verify(gradeRepo).deleteByTaskId(5L);
        verify(gradeRepo, never()).saveForTask(anyInt(), anyLong(), anyLong());
        verify(txManager).commit();
    }

    @Test
    void updateTask_pendingToPending_noGradeInteraction() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.PENDING)));
        CreateUpdateTaskDTO dto = CreateUpdateTaskDTO.of("Still pending", "", TaskStatus.PENDING);

        service.updateTask(1L, 5L, dto, null);

        verify(gradeRepo, never()).saveForTask(anyInt(), anyLong(), anyLong());
        verify(gradeRepo, never()).updateByTaskId(anyLong(), anyInt());
        verify(gradeRepo, never()).deleteByTaskId(anyLong());
    }

    @Test
    void updateTask_toCompleted_withNullGrade_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.updateTask(1L, 5L,
                        CreateUpdateTaskDTO.of("Task", "", TaskStatus.COMPLETED), null));
    }

    @Test
    void updateTask_toCompleted_withOutOfRangeGrade_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.updateTask(1L, 5L,
                        CreateUpdateTaskDTO.of("Task", "", TaskStatus.COMPLETED), 99));
    }

    @Test
    void updateTask_toFailed_automaticallyAssignsGradeMin() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.PENDING)));
        CreateUpdateTaskDTO dto = CreateUpdateTaskDTO.of("Failed task", "", TaskStatus.FAILED);

        TaskDTO result = service.updateTask(1L, 5L, dto, null);

        assertEquals(TaskStatus.FAILED,        result.getStatus());
        assertEquals(AppConfig.getGradeMin(),  result.getGrade());
        verify(gradeRepo).saveForTask(AppConfig.getGradeMin(), 1L, 5L);
        verify(txManager).commit();
    }

    @Test
    void updateTask_alreadyFailed_updatesGradeToMin() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.FAILED)));

        service.updateTask(1L, 5L, CreateUpdateTaskDTO.of("Still failed", "", TaskStatus.FAILED), null);

        verify(gradeRepo).updateByTaskId(5L, AppConfig.getGradeMin());
        verify(gradeRepo, never()).saveForTask(anyInt(), anyLong(), anyLong());
    }

    @Test
    void updateTask_fromFailedToPending_deletesGrade() throws SQLException {
        when(taskRepo.findById(5L)).thenReturn(Optional.of(taskStub(5L, TaskStatus.FAILED)));

        service.updateTask(1L, 5L, CreateUpdateTaskDTO.of("Back", "", TaskStatus.PENDING), null);

        verify(gradeRepo).deleteByTaskId(5L);
        verify(gradeRepo, never()).saveForTask(anyInt(), anyLong(), anyLong());
        verify(txManager).commit();
    }

    @Test
    void updateTask_onSQLException_rollsBackAndThrowsHRAppException() throws SQLException {
        when(taskRepo.findById(anyLong())).thenThrow(new SQLException("db error"));

        assertThrows(HRAppException.class,
                () -> service.updateTask(1L, 5L,
                        CreateUpdateTaskDTO.of("Task", "", TaskStatus.PENDING), null));

        verify(txManager).rollback();
        verify(txManager, never()).commit();
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    void deleteTask_softDeletesTaskAndRemovesGrade() throws SQLException {
        service.deleteTask(7L);

        verify(gradeRepo).deleteByTaskId(7L);
        verify(taskRepo).softDelete(7L);
        verify(txManager).commit();
        verify(txManager, never()).rollback();
    }

    @Test
    void deleteTask_onSQLException_rollsBackAndThrowsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(taskRepo).softDelete(anyLong());

        assertThrows(HRAppException.class, () -> service.deleteTask(7L));

        verify(txManager).rollback();
        verify(txManager, never()).commit();
    }

    // ── getTasksForMember ─────────────────────────────────────────────────────

    @Test
    void getTasksForMember_whenNoTasks_returnsEmptyList() throws SQLException {
        when(taskRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());
        assertTrue(service.getTasksForMember(1L).isEmpty());
    }

    @Test
    void getTasksForMember_pendingTask_hasNullGrade() throws SQLException {
        Task task = taskStub(3L, TaskStatus.PENDING);
        when(taskRepo.findByMemberId(1L)).thenReturn(List.of(task));

        List<TaskDTO> result = service.getTasksForMember(1L);

        assertEquals(1, result.size());
        assertNull(result.get(0).getGrade());
        verify(gradeRepo, never()).findByTaskId(anyLong());
    }

    @Test
    void getTasksForMember_completedTask_loadsGrade() throws SQLException {
        Task task = taskStub(3L, TaskStatus.COMPLETED);
        when(taskRepo.findByMemberId(1L)).thenReturn(List.of(task));
        when(gradeRepo.findByTaskId(3L)).thenReturn(8);

        List<TaskDTO> result = service.getTasksForMember(1L);

        assertEquals(8, result.get(0).getGrade());
        verify(gradeRepo).findByTaskId(3L);
    }

    @Test
    void getTasksForMember_failedTask_loadsGradeMin() throws SQLException {
        Task task = taskStub(3L, TaskStatus.FAILED);
        when(taskRepo.findByMemberId(1L)).thenReturn(List.of(task));
        when(gradeRepo.findByTaskId(3L)).thenReturn(AppConfig.getGradeMin());

        List<TaskDTO> result = service.getTasksForMember(1L);

        assertEquals(AppConfig.getGradeMin(), result.get(0).getGrade());
        verify(gradeRepo).findByTaskId(3L);
    }

    @Test
    void getTasksForMember_onSQLException_throwsHRAppException() throws SQLException {
        when(taskRepo.findByMemberId(anyLong())).thenThrow(new SQLException("db error"));
        assertThrows(HRAppException.class, () -> service.getTasksForMember(1L));
    }
}
