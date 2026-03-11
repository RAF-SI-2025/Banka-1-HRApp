package com.example.service;

import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.HRAppException;
import com.example.exception.MemberNotFoundException;
import com.example.exception.ValidationException;
import com.example.model.Task;
import com.example.model.TaskStatus;
import com.example.model.TeamMember;
import com.example.repository.GradeRepository;
import com.example.repository.SkillRepository;
import com.example.repository.TaskRepository;
import com.example.repository.TeamMemberRepository;
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
 * Unit tests for {@link TeamMemberService}.
 * All repository interactions are mocked — no real database is used.
 */
@ExtendWith(MockitoExtension.class)
class TeamMemberServiceTest {

    @Mock private TeamMemberRepository memberRepo;
    @Mock private TaskRepository       taskRepo;
    @Mock private SkillRepository      skillRepo;
    @Mock private GradeRepository      gradeRepo;
    @Mock private TransactionManager   txManager;

    private TeamMemberService service;

    /** Sets up the service with mocked repositories before each test. */
    @BeforeEach
    void setUp() {
        service = new TeamMemberService(memberRepo, taskRepo, skillRepo, gradeRepo, txManager);
    }

    // ── createMember ──────────────────────────────────────────────────────────

    @Test
    void createMember_withValidData_persistsMemberAndReturnsDTO() throws SQLException {
        doAnswer(inv -> { ((TeamMember) inv.getArgument(0)).setId(1L); return null; })
                .when(memberRepo).save(any());
        when(taskRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());

        TeamMemberDTO result = service.createMember(CreateUpdateMemberDTO.of("Ana", "Jovic"));

        assertEquals("Ana",   result.getName());
        assertEquals("Jovic", result.getSurname());
        assertEquals(1L,      result.getId());
        verify(memberRepo).save(any(TeamMember.class));
    }

    @Test
    void createMember_withBlankName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.createMember(CreateUpdateMemberDTO.of("", "Jovic")));
    }

    @Test
    void createMember_withBlankSurname_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> service.createMember(CreateUpdateMemberDTO.of("Ana", "  ")));
    }

    // ── getAllMembers ─────────────────────────────────────────────────────────

    @Test
    void getAllMembers_whenNoMembers_returnsEmptyList() throws SQLException {
        when(memberRepo.findAllWithDetails()).thenReturn(Collections.emptyList());

        List<TeamMemberDTO> result = service.getAllMembers();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllMembers_returnsAllNonDeletedMembers() throws SQLException {
        TeamMemberDTO dto = new TeamMemberDTO(5L, "Marko", "Petrovic", 0.0,
                Collections.emptyList(), Collections.emptyList());
        when(memberRepo.findAllWithDetails()).thenReturn(List.of(dto));

        List<TeamMemberDTO> result = service.getAllMembers();

        assertEquals(1, result.size());
        assertEquals("Marko", result.get(0).getName());
    }

    // ── updateMember ──────────────────────────────────────────────────────────

    @Test
    void updateMember_withValidData_updatesAndReturnsDTO() throws SQLException {
        TeamMember existing = new TeamMember("Old", "Name");
        existing.setId(2L);
        when(memberRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(taskRepo.findByMemberId(2L)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(2L)).thenReturn(Collections.emptyList());

        TeamMemberDTO result = service.updateMember(2L, CreateUpdateMemberDTO.of("New", "Name"));

        assertEquals("New", result.getName());
        verify(memberRepo).update(existing);
    }

    // ── deleteMember ──────────────────────────────────────────────────────────

    @Test
    void deleteMember_softDeletesMemberAndTasksWithinTransaction() throws SQLException {
        service.deleteMember(3L);

        verify(txManager).beginTransaction();
        verify(taskRepo).softDeleteByMemberId(3L);
        verify(memberRepo).softDelete(3L);
        verify(txManager).commit();
        verify(txManager, never()).rollback();
    }

    @Test
    void deleteMember_onSQLException_rollsBackAndThrowsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(memberRepo).softDelete(anyLong());

        assertThrows(HRAppException.class, () -> service.deleteMember(1L));

        verify(txManager).beginTransaction();
        verify(txManager).rollback();
        verify(txManager, never()).commit();
    }

    // ── addSkill ──────────────────────────────────────────────────────────────

    @Test
    void addSkill_withBlankName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.addSkill(1L, "  "));
    }

    @Test
    void addSkill_normalisesToUppercase() throws SQLException {
        service.addSkill(1L, "java");

        verify(skillRepo).save("JAVA", 1L);
    }

    // ── removeSkill ───────────────────────────────────────────────────────────

    @Test
    void removeSkill_delegatesToRepository() throws SQLException {
        service.removeSkill(1L, "JAVA");

        verify(skillRepo).delete("JAVA", 1L);
    }

    // ── getMemberById ─────────────────────────────────────────────────────────

    @Test
    void getMemberById_whenMemberExists_returnsDTO() throws SQLException {
        TeamMember member = new TeamMember("Luka", "Modric");
        member.setId(7L);
        when(memberRepo.findById(7L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(7L)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(7L)).thenReturn(Collections.emptyList());

        TeamMemberDTO result = service.getMemberById(7L);

        assertEquals(7L,       result.getId());
        assertEquals("Luka",   result.getName());
        assertEquals("Modric", result.getSurname());
    }

    @Test
    void getMemberById_whenMemberNotFound_throwsMemberNotFoundException() throws SQLException {
        when(memberRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class, () -> service.getMemberById(99L));
    }

    @Test
    void getMemberById_onSQLException_throwsHRAppException() throws SQLException {
        when(memberRepo.findById(anyLong())).thenThrow(new SQLException("db error"));

        assertThrows(HRAppException.class, () -> service.getMemberById(1L));
    }

    // ── getAllMembers – SQLException ───────────────────────────────────────────

    @Test
    void getAllMembers_onSQLException_throwsHRAppException() throws SQLException {
        when(memberRepo.findAllWithDetails()).thenThrow(new SQLException("db error"));

        assertThrows(HRAppException.class, () -> service.getAllMembers());
    }

    // ── createMember – SQLException ───────────────────────────────────────────

    @Test
    void createMember_onSQLException_throwsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(memberRepo).save(any());

        assertThrows(HRAppException.class,
                () -> service.createMember(CreateUpdateMemberDTO.of("Ana", "Jovic")));
    }

    // ── updateMember – error paths ────────────────────────────────────────────

    @Test
    void updateMember_whenMemberNotFound_throwsMemberNotFoundException() throws SQLException {
        when(memberRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MemberNotFoundException.class,
                () -> service.updateMember(99L, CreateUpdateMemberDTO.of("New", "Name")));
    }

    @Test
    void updateMember_onSQLException_throwsHRAppException() throws SQLException {
        when(memberRepo.findById(anyLong())).thenThrow(new SQLException("db error"));

        assertThrows(HRAppException.class,
                () -> service.updateMember(1L, CreateUpdateMemberDTO.of("New", "Name")));
    }

    // ── addSkill – null & too-long & SQLException ─────────────────────────────

    @Test
    void addSkill_withNullName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.addSkill(1L, null));
    }

    @Test
    void addSkill_withTooLongName_throwsValidationException() {
        String tooLong = "A".repeat(300);
        assertThrows(ValidationException.class, () -> service.addSkill(1L, tooLong));
    }

    @Test
    void addSkill_onSQLException_throwsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(skillRepo).save(any(), anyLong());

        assertThrows(HRAppException.class, () -> service.addSkill(1L, "java"));
    }

    // ── grade average calculation (via getMemberById) ─────────────────────────

    @Test
    void getMemberById_withNoTasks_returnsAverageZero() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(10L);
        when(memberRepo.findById(10L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(10L)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(10L)).thenReturn(Collections.emptyList());

        TeamMemberDTO result = service.getMemberById(10L);

        assertEquals(0.0, result.getAverageGrade());
    }

    @Test
    void getMemberById_withCompletedGradedTask_returnsCorrectAverage() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(11L);
        Task completedTask = new Task("Done");
        completedTask.setId(101L);
        completedTask.setStatus(TaskStatus.COMPLETED);

        when(memberRepo.findById(11L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(11L)).thenReturn(List.of(completedTask));
        when(skillRepo.findByMemberId(11L)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByTaskId(101L)).thenReturn(7);

        TeamMemberDTO result = service.getMemberById(11L);

        assertEquals(7.0, result.getAverageGrade());
    }

    @Test
    void getMemberById_withMultipleCompletedGradedTasks_returnsCorrectAverage() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(12L);
        Task t1 = new Task("Task 1"); t1.setId(201L); t1.setStatus(TaskStatus.COMPLETED);
        Task t2 = new Task("Task 2"); t2.setId(202L); t2.setStatus(TaskStatus.COMPLETED);
        Task t3 = new Task("Task 3"); t3.setId(203L); t3.setStatus(TaskStatus.COMPLETED);

        when(memberRepo.findById(12L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(12L)).thenReturn(List.of(t1, t2, t3));
        when(skillRepo.findByMemberId(12L)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByTaskId(201L)).thenReturn(4);
        when(gradeRepo.findByTaskId(202L)).thenReturn(6);
        when(gradeRepo.findByTaskId(203L)).thenReturn(10);

        TeamMemberDTO result = service.getMemberById(12L);

        assertEquals(20.0 / 3.0, result.getAverageGrade(), 0.0001);
    }

    @Test
    void getMemberById_pendingTaskGradeDoesNotCountTowardsAverage() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(13L);
        Task completed = new Task("Done");  completed.setId(301L); completed.setStatus(TaskStatus.COMPLETED);
        Task pending   = new Task("Pending"); pending.setId(302L); pending.setStatus(TaskStatus.PENDING);

        when(memberRepo.findById(13L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(13L)).thenReturn(List.of(completed, pending));
        when(skillRepo.findByMemberId(13L)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByTaskId(301L)).thenReturn(8);
        when(gradeRepo.findByTaskId(302L)).thenReturn(2);

        TeamMemberDTO result = service.getMemberById(13L);

        assertEquals(8.0, result.getAverageGrade(), 0.001);
    }
}
