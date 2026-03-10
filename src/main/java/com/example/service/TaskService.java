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
 * Service layer for task and grade operations.
 * <p>
 * Keeps task business logic separate from team member concerns.
 * Grades are managed here because they belong to individual tasks.
 */
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepo;
    private final GradeRepository gradeRepo;
    private final TransactionManager transactionManager;

    /**
     * Constructs the service with the required repositories and transaction manager
     * (constructor injection).
     *
     * @param taskRepo           repository for task data
     * @param gradeRepo          repository for grade data
     * @param transactionManager transaction manager for multi-step operations
     */
    public TaskService(TaskRepository taskRepo, GradeRepository gradeRepo,
                       TransactionManager transactionManager) {
        this.taskRepo            = taskRepo;
        this.gradeRepo           = gradeRepo;
        this.transactionManager  = transactionManager;
    }

    /**
     * Returns all non-deleted tasks for the given member, each with its grades.
     *
     * @param memberId the owning member's database ID
     * @return list of task DTOs
     * @throws HRAppException on database error
     */
    public List<TaskDTO> getTasksForMember(long memberId) {
        try {
            List<Task> tasks = taskRepo.findByMemberId(memberId);
            return tasks.stream()
                    .map(t -> {
                        try {
                            int grade = gradeRepo.findByTaskId(t.getId());
                            return new TaskDTO(t.getId(), t.getTaskName(), t.getStatus(), t.getComment(), grade);
                        } catch (SQLException e) {
                            throw new HRAppException("Failed to load grade for task id=" + t.getId(), e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("Failed to load tasks for member id={}", memberId, e);
            throw new HRAppException("Failed to load tasks.", e);
        }
    }

    /**
     * Creates a new task for the given member and returns the persisted task DTO.
     *
     * @param memberId the owning member's database ID
     * @param dto      validated input DTO
     * @return the created task as a DTO (with generated id)
     * @throws HRAppException on database error
     */
    public TaskDTO addTask(long memberId, CreateUpdateTaskDTO dto) {
        try {
            Task task = new Task(dto.getTaskName());
            task.setComment(dto.getComment());
            task.setStatus(dto.getStatus());
            if (dto.getStatus() == TaskStatus.FAILED) {
                transactionManager.beginTransaction();
                try {
                    taskRepo.save(task, memberId);
                    gradeRepo.saveOrUpdate(AppConfig.getGradeMin(), task.getId());
                    transactionManager.commit();
                } catch (SQLException e) {
                    transactionManager.rollback();
                    throw e;
                }
            } else {
                taskRepo.save(task, memberId);
            }
            int grade = dto.getStatus() == TaskStatus.FAILED ? AppConfig.getGradeMin() : 0;
            log.info("Added task '{}' to member id={}", dto.getTaskName(), memberId);
            return new TaskDTO(task.getId(), task.getTaskName(), task.getStatus(), task.getComment(), grade);
        } catch (SQLException e) {
            log.error("Failed to add task for member id={}", memberId, e);
            throw new HRAppException("Failed to add task.", e);
        }
    }

    /**
     * Updates a task's name, comment, and status, and returns the refreshed DTO.
     *
     * @param taskId the task's database ID
     * @param dto    validated input DTO with new values
     * @return the updated task as a DTO
     * @throws HRAppException on database error
     */
    public TaskDTO updateTask(long taskId, CreateUpdateTaskDTO dto) {
        try {
            Task task = new Task(dto.getTaskName());
            task.setId(taskId);
            task.setComment(dto.getComment());
            task.setStatus(dto.getStatus());
            if (dto.getStatus() == TaskStatus.FAILED) {
                transactionManager.beginTransaction();
                try {
                    taskRepo.update(task);
                    gradeRepo.saveOrUpdate(AppConfig.getGradeMin(), taskId);
                    transactionManager.commit();
                } catch (SQLException e) {
                    transactionManager.rollback();
                    throw e;
                }
            } else if (dto.getStatus() == TaskStatus.PENDING) {
                transactionManager.beginTransaction();
                try {
                    taskRepo.update(task);
                    gradeRepo.deleteByTaskId(taskId);
                    transactionManager.commit();
                } catch (SQLException e) {
                    transactionManager.rollback();
                    throw e;
                }
            } else {
                taskRepo.update(task);
            }
            log.info("Updated task id={}", taskId);
            int grade = gradeRepo.findByTaskId(taskId);
            return new TaskDTO(task.getId(), task.getTaskName(), task.getStatus(), task.getComment(), grade);
        } catch (SQLException e) {
            log.error("Failed to update task id={}", taskId, e);
            throw new HRAppException("Failed to update task.", e);
        }
    }

    /**
     * Soft-deletes a task.
     *
     * @param taskId the task's database ID
     * @throws HRAppException on database error
     */
    public void deleteTask(long taskId) {
        try {
            taskRepo.softDelete(taskId);
            log.info("Deleted task id={}", taskId);
        } catch (SQLException e) {
            log.error("Failed to delete task id={}", taskId, e);
            throw new HRAppException("Failed to delete task.", e);
        }
    }

    /**
     * Sets (or replaces) the grade for the given task.
     *
     * @param taskId the task's database ID
     * @param grade  the grade value (must be within configured min/max range)
     * @throws ValidationException if the grade is out of range
     * @throws HRAppException      on database error
     */
    public void gradeTask(long taskId, int grade) {
        if (grade < AppConfig.getGradeMin() || grade > AppConfig.getGradeMax()) {
            throw new ValidationException(
                    "Grade must be between " + AppConfig.getGradeMin() + " and " + AppConfig.getGradeMax() + ".");
        }
        try {
            Task task = taskRepo.findById(taskId)
                    .orElseThrow(() -> new HRAppException("Task not found: id=" + taskId));
            if (task.getStatus() == TaskStatus.PENDING) {
                throw new ValidationException("Cannot grade a pending task.");
            }
            if (task.getStatus() == TaskStatus.FAILED) {
                throw new ValidationException("Cannot change the grade of a failed task.");
            }
            gradeRepo.saveOrUpdate(grade, taskId);
            log.info("Graded task id={} with grade={}", taskId, grade);
        } catch (SQLException e) {
            log.error("Failed to grade task id={}", taskId, e);
            throw new HRAppException("Failed to save grade.", e);
        }
    }
}
