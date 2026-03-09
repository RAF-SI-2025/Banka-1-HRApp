package com.example.service;

import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.ValidationException;
import com.example.model.TeamMember;
import com.example.repository.GradeRepository;
import com.example.repository.SkillRepository;
import com.example.repository.TaskRepository;
import com.example.repository.TeamMemberRepository;
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

    private TeamMemberService service;

    /** Sets up the service with mocked repositories before each test. */
    @BeforeEach
    void setUp() {
        service = new TeamMemberService(memberRepo, taskRepo, skillRepo, gradeRepo);
    }

    // ── createMember ──────────────────────────────────────────────────────────

    @Test
    void createMember_withValidData_persistsMemberAndReturnsDTO() throws SQLException {
        doAnswer(inv -> { ((TeamMember) inv.getArgument(0)).setId(1L); return null; })
                .when(memberRepo).save(any());
        when(taskRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());
        when(gradeRepo.findByMemberId(anyLong())).thenReturn(Collections.emptyList());

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
        when(memberRepo.findAll()).thenReturn(Collections.emptyList());

        List<TeamMemberDTO> result = service.getAllMembers();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllMembers_returnsAllNonDeletedMembers() throws SQLException {
        TeamMember m = new TeamMember("Marko", "Petrovic");
        m.setId(5L);
        when(memberRepo.findAll()).thenReturn(List.of(m));
        when(taskRepo.findByMemberId(5L)).thenReturn(Collections.emptyList());
        when(skillRepo.findByMemberId(5L)).thenReturn(Collections.emptyList());
        when(gradeRepo.findByMemberId(5L)).thenReturn(Collections.emptyList());

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
        when(gradeRepo.findByMemberId(2L)).thenReturn(Collections.emptyList());

        TeamMemberDTO result = service.updateMember(2L, CreateUpdateMemberDTO.of("New", "Name"));

        assertEquals("New", result.getName());
        verify(memberRepo).update(existing);
    }

    // ── deleteMember ──────────────────────────────────────────────────────────

    @Test
    void deleteMember_softDeletesMemberAndTasks() throws SQLException {
        service.deleteMember(3L);

        verify(taskRepo).softDeleteByMemberId(3L);
        verify(memberRepo).softDelete(3L);
    }

    // ── addGrade ──────────────────────────────────────────────────────────────

    @Test
    void addGrade_withValidGrade_savesGrade() throws SQLException {
        service.addGrade(1L, 8);

        verify(gradeRepo).save(8, 1L);
    }

    @Test
    void addGrade_withGradeTooHigh_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.addGrade(1L, 11));
    }

    @Test
    void addGrade_withGradeTooLow_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.addGrade(1L, 0));
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
}
