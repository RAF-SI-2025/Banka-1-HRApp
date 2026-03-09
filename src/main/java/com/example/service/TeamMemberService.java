package com.example.service;

import com.example.config.AppConfig;
import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.TaskDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.HRAppException;
import com.example.exception.MemberNotFoundException;
import com.example.exception.ValidationException;
import com.example.model.Task;
import com.example.model.TeamMember;
import com.example.repository.GradeRepository;
import com.example.repository.SkillRepository;
import com.example.repository.TaskRepository;
import com.example.repository.TeamMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for team member operations.
 * <p>
 * Orchestrates calls to the repository layer and converts domain entities to DTOs.
 * This class must not be accessed directly from the database layer — all DB calls
 * go through the injected repositories.
 */
public class TeamMemberService {

    private static final Logger log = LoggerFactory.getLogger(TeamMemberService.class);

    private final TeamMemberRepository memberRepo;
    private final TaskRepository taskRepo;
    private final SkillRepository skillRepo;
    private final GradeRepository gradeRepo;

    /**
     * Constructs the service with all required repositories (constructor injection).
     *
     * @param memberRepo repository for team member data
     * @param taskRepo   repository for task data
     * @param skillRepo  repository for skill data
     * @param gradeRepo  repository for grade data
     */
    public TeamMemberService(TeamMemberRepository memberRepo,
                             TaskRepository taskRepo,
                             SkillRepository skillRepo,
                             GradeRepository gradeRepo) {
        this.memberRepo = memberRepo;
        this.taskRepo   = taskRepo;
        this.skillRepo  = skillRepo;
        this.gradeRepo  = gradeRepo;
    }

    /**
     * Returns all non-deleted team members with their full related data (tasks, skills, grades).
     *
     * @return list of team member DTOs
     * @throws HRAppException on database error
     */
    public List<TeamMemberDTO> getAllMembers() {
        try {
            List<TeamMember> members = memberRepo.findAll();
            return members.stream()
                    .map(this::buildDTO)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("Failed to load team members", e);
            throw new HRAppException("Failed to load team members.", e);
        }
    }

    /**
     * Creates a new team member from the given DTO and returns the persisted member DTO.
     *
     * @param dto validated input DTO
     * @return the created team member as a DTO (with generated id)
     * @throws HRAppException on database error
     */
    public TeamMemberDTO createMember(CreateUpdateMemberDTO dto) {
        try {
            TeamMember member = new TeamMember(dto.getName(), dto.getSurname());
            memberRepo.save(member);
            log.info("Created team member: {} {}", dto.getName(), dto.getSurname());
            return buildDTO(member);
        } catch (SQLException e) {
            log.error("Failed to create team member", e);
            throw new HRAppException("Failed to create team member.", e);
        }
    }

    /**
     * Updates the name/surname of an existing team member and returns the refreshed DTO.
     *
     * @param id  the member's database ID
     * @param dto validated input DTO with new values
     * @return the updated team member as a DTO
     * @throws MemberNotFoundException if no member exists with the given id
     * @throws HRAppException          on database error
     */
    public TeamMemberDTO updateMember(long id, CreateUpdateMemberDTO dto) {
        try {
            TeamMember member = memberRepo.findById(id)
                    .orElseThrow(() -> new MemberNotFoundException(id));
            member.setName(dto.getName());
            member.setSurname(dto.getSurname());
            memberRepo.update(member);
            log.info("Updated team member id={}", id);
            return buildDTO(member);
        } catch (SQLException e) {
            log.error("Failed to update team member id={}", id, e);
            throw new HRAppException("Failed to update team member.", e);
        }
    }

    /**
     * Soft-deletes a team member and all their tasks.
     *
     * @param id the member's database ID
     * @throws HRAppException on database error
     */
    public void deleteMember(long id) {
        try {
            taskRepo.softDeleteByMemberId(id);
            memberRepo.softDelete(id);
            log.info("Soft-deleted team member id={} and their tasks", id);
        } catch (SQLException e) {
            log.error("Failed to delete team member id={}", id, e);
            throw new HRAppException("Failed to delete team member.", e);
        }
    }

    /**
     * Adds a skill to a team member. Duplicate skills (case-insensitive) are silently ignored.
     *
     * @param memberId the member's database ID
     * @param skill    the skill name (will be upper-cased and trimmed)
     * @throws ValidationException if the skill is blank or too long
     * @throws HRAppException      on database error
     */
    public void addSkill(long memberId, String skill) {
        String normalised = skill != null ? skill.trim().toUpperCase() : "";
        if (normalised.isEmpty()) {
            throw new ValidationException("Skill name cannot be blank.");
        }
        if (normalised.length() > AppConfig.MAX_SKILL_LENGTH) {
            throw new ValidationException("Skill name exceeds maximum length of " + AppConfig.MAX_SKILL_LENGTH + " characters.");
        }
        try {
            skillRepo.save(normalised, memberId);
        } catch (SQLException e) {
            log.error("Failed to add skill for member id={}", memberId, e);
            throw new HRAppException("Failed to add skill.", e);
        }
    }

    /**
     * Removes a skill from a team member.
     *
     * @param memberId the member's database ID
     * @param skill    the exact skill name to remove
     * @throws HRAppException on database error
     */
    public void removeSkill(long memberId, String skill) {
        try {
            skillRepo.delete(skill, memberId);
        } catch (SQLException e) {
            log.error("Failed to remove skill for member id={}", memberId, e);
            throw new HRAppException("Failed to remove skill.", e);
        }
    }

    /**
     * Adds a grade to a team member's grade history.
     *
     * @param memberId the member's database ID
     * @param grade    the grade value (must be within {@link AppConfig#GRADE_MIN}–{@link AppConfig#GRADE_MAX})
     * @throws ValidationException if the grade is out of range
     * @throws HRAppException      on database error
     */
    public void addGrade(long memberId, int grade) {
        if (grade < AppConfig.GRADE_MIN || grade > AppConfig.GRADE_MAX) {
            throw new ValidationException(
                    "Grade must be between " + AppConfig.GRADE_MIN + " and " + AppConfig.GRADE_MAX + ".");
        }
        try {
            gradeRepo.save(grade, memberId);
        } catch (SQLException e) {
            log.error("Failed to add grade for member id={}", memberId, e);
            throw new HRAppException("Failed to add grade.", e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads all related data for a member entity and converts it to a {@link TeamMemberDTO}.
     *
     * @param member the member entity (id must be set)
     * @return a fully populated DTO
     */
    private TeamMemberDTO buildDTO(TeamMember member) {
        try {
            List<Task> tasks     = taskRepo.findByMemberId(member.getId());
            List<String> skills  = skillRepo.findByMemberId(member.getId());
            List<Integer> grades = gradeRepo.findByMemberId(member.getId());

            double avg = grades.isEmpty()
                    ? 0
                    : grades.stream().mapToInt(Integer::intValue).average().orElse(0);

            List<TaskDTO> taskDTOs = tasks.stream()
                    .map(t -> new TaskDTO(t.getId(), t.getTaskName(), t.getStatus(), t.getComment()))
                    .collect(Collectors.toList());

            return new TeamMemberDTO(member.getId(), member.getName(), member.getSurname(),
                    avg, taskDTOs, skills, grades);
        } catch (SQLException e) {
            log.error("Failed to build DTO for member id={}", member.getId(), e);
            throw new HRAppException("Failed to load member details.", e);
        }
    }
}
