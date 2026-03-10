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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for task operations.
 * <p>
 * COMPLETED tasks require a grade (provided by the caller).
 * FAILED tasks automatically receive grade {@link AppConfig#getGradeMin()}.
 * Moving a task back to PENDING removes its grade.
 */
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository     taskRepo;
    private final GradeRepository    gradeRepo;
    private final TransactionManager transactionManager;

    public TaskService(TaskRepository taskRepo,
                       GradeRepository gradeRepo,
                       TransactionManager transactionManager) {
        this.taskRepo           = taskRepo;
        this.gradeRepo          = gradeRepo;
        this.transactionManager = transactionManager;
    }

    /** Returns all non-deleted tasks for the given member, each with its grade if present. */
    public List<TaskDTO> getTasksForMember(long memberId) {
        try {
            return taskRepo.findByMemberId(memberId).stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("Failed to load tasks for member id={}", memberId, e);
            throw new HRAppException("Failed to load tasks.", e);
        }
    }

    /**
     * Creates a new task. COMPLETED is not allowed at creation.
     * FAILED tasks automatically receive grade min.
     */
    public TaskDTO addTask(long memberId, CreateUpdateTaskDTO dto) {
        if (dto.getStatus() == TaskStatus.COMPLETED) {
            throw new ValidationException("A new task cannot be created as COMPLETED. " +
                    "Create it as PENDING and then mark it complete to assign a grade.");
        }
        try {
            Task task = new Task(dto.getTaskName());
            task.setComment(dto.getComment());
            task.setStatus(dto.getStatus());

            if (dto.getStatus() == TaskStatus.FAILED) {
                transactionManager.beginTransaction();
                try {
                    taskRepo.save(task, memberId);
                    gradeRepo.saveForTask(AppConfig.getGradeMin(), memberId, task.getId());
                    transactionManager.commit();
                } catch (SQLException e) {
                    log.error("Failed to save FAILED task for member id={}, rolling back", memberId, e);
                    transactionManager.rollback();
                    throw e;
                }
                log.info("Added FAILED task '{}' with grade {} to member id={}",
                        dto.getTaskName(), AppConfig.getGradeMin(), memberId);
                return new TaskDTO(task.getId(), task.getTaskName(), task.getStatus(),
                        task.getComment(), AppConfig.getGradeMin());
            }

            taskRepo.save(task, memberId);
            log.info("Added task '{}' to member id={}", dto.getTaskName(), memberId);
            return new TaskDTO(task.getId(), task.getTaskName(), task.getStatus(), task.getComment());
        } catch (SQLException e) {
            log.error("Failed to add task for member id={}", memberId, e);
            throw new HRAppException("Failed to add task.", e);
        }
    }

    /**
     * Updates a task and manages its grade:
     * COMPLETED → grade required; FAILED → grade auto-set to min; PENDING → grade removed.
     */
    public TaskDTO updateTask(long memberId, long taskId, CreateUpdateTaskDTO dto, Integer grade) {
        if (dto.getStatus() == TaskStatus.COMPLETED) {
            validateGrade(grade);
        }
        if (dto.getStatus() == TaskStatus.FAILED) {
            grade = AppConfig.getGradeMin();
        }
        try {
            transactionManager.beginTransaction();
            try {
                Task existing = taskRepo.findById(taskId).orElseThrow(
                        () -> new HRAppException("Task not found: " + taskId));
                TaskStatus previousStatus = existing.getStatus();

                Task updated = new Task(dto.getTaskName());
                updated.setId(taskId);
                updated.setComment(dto.getComment());
                updated.setStatus(dto.getStatus());
                taskRepo.update(updated);

                if (dto.getStatus() == TaskStatus.COMPLETED || dto.getStatus() == TaskStatus.FAILED) {
                    boolean hadGrade = previousStatus == TaskStatus.COMPLETED
                            || previousStatus == TaskStatus.FAILED;
                    if (hadGrade) {
                        gradeRepo.updateByTaskId(taskId, grade);
                    } else {
                        gradeRepo.saveForTask(grade, memberId, taskId);
                    }
                } else if (previousStatus == TaskStatus.COMPLETED
                        || previousStatus == TaskStatus.FAILED) {
                    gradeRepo.deleteByTaskId(taskId);
                }

                transactionManager.commit();
                log.info("Updated task id={} status={}", taskId, dto.getStatus());

                Integer savedGrade = (dto.getStatus() == TaskStatus.COMPLETED
                        || dto.getStatus() == TaskStatus.FAILED) ? grade : null;
                return new TaskDTO(taskId, dto.getTaskName(), dto.getStatus(),
                        dto.getComment(), savedGrade);
            } catch (HRAppException | SQLException e) {
                log.error("Failed to update task id={}, rolling back", taskId, e);
                transactionManager.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Failed to update task id={}", taskId, e);
            throw new HRAppException("Failed to update task.", e);
        }
    }

    /** Soft-deletes a task and removes its grade atomically. */
    public void deleteTask(long taskId) {
        try {
            transactionManager.beginTransaction();
            try {
                gradeRepo.deleteByTaskId(taskId);
                taskRepo.softDelete(taskId);
                transactionManager.commit();
                log.info("Deleted task id={}", taskId);
            } catch (SQLException e) {
                log.error("Failed to delete task id={}, rolling back", taskId, e);
                transactionManager.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Failed to delete task id={}", taskId, e);
            throw new HRAppException("Failed to delete task.", e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validateGrade(Integer grade) {
        if (grade == null) {
            throw new ValidationException("A grade is required when marking a task as COMPLETED.");
        }
        if (grade < AppConfig.getGradeMin() || grade > AppConfig.getGradeMax()) {
            throw new ValidationException(
                    "Grade must be between " + AppConfig.getGradeMin()
                            + " and " + AppConfig.getGradeMax() + ".");
        }
    }

    private TaskDTO toDTO(Task t) {
        try {
            Integer grade = (t.getStatus() == TaskStatus.COMPLETED
                    || t.getStatus() == TaskStatus.FAILED)
                    ? gradeRepo.findByTaskId(t.getId())
                    : null;
            return new TaskDTO(t.getId(), t.getTaskName(), t.getStatus(), t.getComment(), grade);
        } catch (SQLException e) {
            log.error("Failed to load grade for task id={}", t.getId(), e);
            throw new HRAppException("Failed to load task grade.", e);
        }
    }
}
