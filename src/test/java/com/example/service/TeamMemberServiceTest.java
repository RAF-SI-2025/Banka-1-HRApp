package com.example.service;

import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.HRAppException;
import com.example.exception.MemberNotFoundException;
import com.example.exception.ValidationException;
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

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Stubs the repos needed for buildDTO when the member has no tasks/skills/grades. */
    private void stubEmptyMemberDetails(long memberId) throws SQLException {
        when(taskRepo.findByMemberId(memberId)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(memberId)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByMemberId(memberId)).thenReturn(Collections.emptyList());
    }

    // ── createMember ──────────────────────────────────────────────────────────

    @Test
    void createMember_withValidData_persistsMemberAndReturnsDTO() throws SQLException {
        doAnswer(inv -> { ((TeamMember) inv.getArgument(0)).setId(1L); return null; })
                .when(memberRepo).save(any());
        stubEmptyMemberDetails(1L);

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

    @Test
    void createMember_onSQLException_throwsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(memberRepo).save(any());

        assertThrows(HRAppException.class,
                () -> service.createMember(CreateUpdateMemberDTO.of("Ana", "Jovic")));
    }

    // ── getAllMembers ─────────────────────────────────────────────────────────

    @Test
    void getAllMembers_whenNoMembers_returnsEmptyList() throws SQLException {
        when(memberRepo.findAllWithDetails()).thenReturn(Collections.emptyList());
        assertTrue(service.getAllMembers().isEmpty());
    }

    @Test
    void getAllMembers_returnsAllNonDeletedMembers() throws SQLException {
        TeamMemberDTO dto = new TeamMemberDTO(5L, "Marko", "Petrovic", 0.0,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(memberRepo.findAllWithDetails()).thenReturn(List.of(dto));

        List<TeamMemberDTO> result = service.getAllMembers();

        assertEquals(1, result.size());
        assertEquals("Marko", result.get(0).getName());
    }

    @Test
    void getAllMembers_onSQLException_throwsHRAppException() throws SQLException {
        when(memberRepo.findAllWithDetails()).thenThrow(new SQLException("db error"));
        assertThrows(HRAppException.class, () -> service.getAllMembers());
    }

    // ── getMemberById ─────────────────────────────────────────────────────────

    @Test
    void getMemberById_whenMemberExists_returnsDTO() throws SQLException {
        TeamMember member = new TeamMember("Luka", "Modric");
        member.setId(7L);
        when(memberRepo.findById(7L)).thenReturn(Optional.of(member));
        stubEmptyMemberDetails(7L);

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

    @Test
    void getMemberById_withNoGrades_returnsAverageZero() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(10L);
        when(memberRepo.findById(10L)).thenReturn(Optional.of(member));
        stubEmptyMemberDetails(10L);

        assertEquals(0.0, service.getMemberById(10L).getAverageGrade());
    }

    @Test
    void getMemberById_withSingleGrade_returnsCorrectAverage() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(11L);
        when(memberRepo.findById(11L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(11L)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(11L)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByMemberId(11L)).thenReturn(List.of(7));

        assertEquals(7.0, service.getMemberById(11L).getAverageGrade());
    }

    @Test
    void getMemberById_withMultipleGrades_returnsCorrectAverage() throws SQLException {
        TeamMember member = new TeamMember("Test", "User");
        member.setId(12L);
        when(memberRepo.findById(12L)).thenReturn(Optional.of(member));
        when(taskRepo.findByMemberId(12L)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(12L)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByMemberId(12L)).thenReturn(List.of(4, 6, 10));

        assertEquals(20.0 / 3.0, service.getMemberById(12L).getAverageGrade(), 0.0001);
    }

    // ── updateMember ──────────────────────────────────────────────────────────

    @Test
    void updateMember_withValidData_updatesAndReturnsDTO() throws SQLException {
        TeamMember existing = new TeamMember("Old", "Name");
        existing.setId(2L);
        when(memberRepo.findById(2L)).thenReturn(Optional.of(existing));
        stubEmptyMemberDetails(2L);

        TeamMemberDTO result = service.updateMember(2L, CreateUpdateMemberDTO.of("New", "Name"));

        assertEquals("New", result.getName());
        verify(memberRepo).update(existing);
    }

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
    void addSkill_withNullName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.addSkill(1L, null));
    }

    @Test
    void addSkill_withTooLongName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.addSkill(1L, "A".repeat(300)));
    }

    @Test
    void addSkill_normalisesToUppercase() throws SQLException {
        service.addSkill(1L, "java");
        verify(skillRepo).save("JAVA", 1L);
    }

    @Test
    void addSkill_onSQLException_throwsHRAppException() throws SQLException {
        doThrow(new SQLException("db error")).when(skillRepo).save(any(), anyLong());
        assertThrows(HRAppException.class, () -> service.addSkill(1L, "java"));
    }

    // ── removeSkill ───────────────────────────────────────────────────────────

    @Test
    void removeSkill_delegatesToRepository() throws SQLException {
        service.removeSkill(1L, "JAVA");
        verify(skillRepo).delete("JAVA", 1L);
    }
}
