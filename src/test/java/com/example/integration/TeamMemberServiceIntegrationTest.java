package com.example.integration;


import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.HRAppException;
import com.example.exception.MemberNotFoundException;
import com.example.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link com.example.service.TeamMemberService}.
 * Uses a real in-memory SQLite database — no mocks.
 */
class TeamMemberServiceIntegrationTest extends IntegrationTestBase {

    // ── createMember ─────────────────────────────────────────────────────────

    @Test
    void createMember_persistsAndReturnsDTO() {
        TeamMemberDTO dto = memberService.createMember(CreateUpdateMemberDTO.of("Ana", "Jovic"));

        assertTrue(dto.getId() > 0);
        assertEquals("Ana",   dto.getName());
        assertEquals("Jovic", dto.getSurname());
        assertEquals(0.0,     dto.getAverageGrade());
        assertTrue(dto.getTasks().isEmpty());
        assertTrue(dto.getSkills().isEmpty());
    }

    @Test
    void createMember_withBlankName_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> memberService.createMember(CreateUpdateMemberDTO.of("", "Jovic")));
    }

    @Test
    void createMember_withBlankSurname_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> memberService.createMember(CreateUpdateMemberDTO.of("Ana", "  ")));
    }

    // ── getAllMembers ─────────────────────────────────────────────────────────

    @Test
    void getAllMembers_whenEmpty_returnsEmptyList() {
        assertTrue(memberService.getAllMembers().isEmpty());
    }

    @Test
    void getAllMembers_returnsSavedMembers() {
        memberService.createMember(CreateUpdateMemberDTO.of("Marko", "Petrovic"));
        memberService.createMember(CreateUpdateMemberDTO.of("Jelena", "Nikolic"));

        List<TeamMemberDTO> all = memberService.getAllMembers();

        assertEquals(2, all.size());
    }

    @Test
    void getAllMembers_orderedBySurnameThenName() {
        memberService.createMember(CreateUpdateMemberDTO.of("Zoran",  "Zoric"));
        memberService.createMember(CreateUpdateMemberDTO.of("Ana",    "Antic"));
        memberService.createMember(CreateUpdateMemberDTO.of("Milica", "Antic"));

        List<TeamMemberDTO> all = memberService.getAllMembers();

        assertEquals("Antic", all.get(0).getSurname());
        assertEquals("Ana",   all.get(0).getName());
        assertEquals("Antic", all.get(1).getSurname());
        assertEquals("Milica",all.get(1).getName());
        assertEquals("Zoric", all.get(2).getSurname());
    }

    // ── getMemberById ─────────────────────────────────────────────────────────

    @Test
    void getMemberById_returnsCorrectMember() {
        TeamMemberDTO created = memberService.createMember(CreateUpdateMemberDTO.of("Nikola", "Tesla"));

        TeamMemberDTO found = memberService.getMemberById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("Nikola", found.getName());
        assertEquals("Tesla",  found.getSurname());
    }

    @Test
    void getMemberById_withNonExistentId_throwsMemberNotFoundException() {
        assertThrows(MemberNotFoundException.class, () -> memberService.getMemberById(9999L));
    }

    // ── updateMember ─────────────────────────────────────────────────────────

    @Test
    void updateMember_changesNameAndSurname() {
        TeamMemberDTO created = memberService.createMember(CreateUpdateMemberDTO.of("Old", "Name"));

        TeamMemberDTO updated = memberService.updateMember(created.getId(),
                CreateUpdateMemberDTO.of("New", "Name"));

        assertEquals("New",  updated.getName());
        assertEquals("Name", updated.getSurname());
        // Verify the change is persisted
        TeamMemberDTO reloaded = memberService.getMemberById(created.getId());
        assertEquals("New", reloaded.getName());
    }

    @Test
    void updateMember_withNonExistentId_throwsMemberNotFoundException() {
        assertThrows(MemberNotFoundException.class,
                () -> memberService.updateMember(9999L, CreateUpdateMemberDTO.of("X", "Y")));
    }

    // ── deleteMember ─────────────────────────────────────────────────────────

    @Test
    void deleteMember_softDeletesAndHidesFromFindAll() {
        TeamMemberDTO created = memberService.createMember(CreateUpdateMemberDTO.of("Delete", "Me"));

        memberService.deleteMember(created.getId());

        assertTrue(memberService.getAllMembers().isEmpty());
    }

    @Test
    void deleteMember_alsoSoftDeletesTheirTasks() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Task", "Owner"));
        taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("My task", "", com.example.model.TaskStatus.PENDING));

        memberService.deleteMember(member.getId());

        // Tasks should be gone along with the member
        assertTrue(taskService.getTasksForMember(member.getId()).isEmpty());
    }

    // ── addSkill / removeSkill ────────────────────────────────────────────────

    @Test
    void addSkill_normalisesAndPersists() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Skill", "User"));

        memberService.addSkill(member.getId(), "java");

        TeamMemberDTO reloaded = memberService.getMemberById(member.getId());
        assertTrue(reloaded.getSkills().contains("JAVA"));
    }

    @Test
    void addSkill_duplicateIsIgnored() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Dup", "Skill"));

        memberService.addSkill(member.getId(), "JAVA");
        memberService.addSkill(member.getId(), "java"); // duplicate

        TeamMemberDTO reloaded = memberService.getMemberById(member.getId());
        assertEquals(1, reloaded.getSkills().size());
    }

    @Test
    void addSkill_withBlankName_throwsValidationException() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("A", "B"));
        assertThrows(ValidationException.class, () -> memberService.addSkill(member.getId(), "  "));
    }

    @Test
    void removeSkill_removesPersistedSkill() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Remove", "Skill"));
        memberService.addSkill(member.getId(), "PYTHON");

        memberService.removeSkill(member.getId(), "PYTHON");

        TeamMemberDTO reloaded = memberService.getMemberById(member.getId());
        assertFalse(reloaded.getSkills().contains("PYTHON"));
    }

    // ── gradeTask ─────────────────────────────────────────────────────────────

    @Test
    void gradeTask_persists_andAverageIsCorrectForCompletedTasks() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Grade", "User"));

        com.example.dto.TaskDTO t1 = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Task 1", "", com.example.model.TaskStatus.PENDING));
        taskService.updateTask(t1.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Task 1", "", com.example.model.TaskStatus.COMPLETED));
        taskService.gradeTask(t1.getId(), 8);

        com.example.dto.TaskDTO t2 = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Task 2", "", com.example.model.TaskStatus.PENDING));
        taskService.updateTask(t2.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Task 2", "", com.example.model.TaskStatus.COMPLETED));
        taskService.gradeTask(t2.getId(), 6);

        TeamMemberDTO reloaded = memberService.getMemberById(member.getId());
        assertEquals(7.0, reloaded.getAverageGrade(), 0.001);
    }

    @Test
    void gradeTask_tooHigh_throwsValidationException() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("A", "B"));
        com.example.dto.TaskDTO task = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("T", "", com.example.model.TaskStatus.PENDING));
        assertThrows(ValidationException.class, () -> taskService.gradeTask(task.getId(), 11));
    }

    @Test
    void gradeTask_tooLow_throwsValidationException() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("A", "B"));
        com.example.dto.TaskDTO task = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("T", "", com.example.model.TaskStatus.PENDING));
        assertThrows(ValidationException.class, () -> taskService.gradeTask(task.getId(), 0));
    }

    @Test
    void gradeTask_onlyCompletedTasksCountTowardsAverage() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Grade", "Average"));

        // COMPLETED task — should count
        com.example.dto.TaskDTO completed = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Done", "", com.example.model.TaskStatus.PENDING));
        taskService.updateTask(completed.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Done", "", com.example.model.TaskStatus.COMPLETED));
        taskService.gradeTask(completed.getId(), 8);

        // PENDING task — cannot be graded, should NOT count towards average
        taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Pending", "", com.example.model.TaskStatus.PENDING));

        TeamMemberDTO reloaded = memberService.getMemberById(member.getId());
        assertEquals(8.0, reloaded.getAverageGrade(), 0.001);
    }

    @Test
    void gradeTask_canBeUpdatedWithSecondCall() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Grade", "Update"));
        com.example.dto.TaskDTO task = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Task", "", com.example.model.TaskStatus.PENDING));
        taskService.updateTask(task.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Task", "", com.example.model.TaskStatus.COMPLETED));

        taskService.gradeTask(task.getId(), 5);
        taskService.gradeTask(task.getId(), 9); // overwrite

        List<com.example.dto.TaskDTO> tasks = taskService.getTasksForMember(member.getId());
        assertEquals(9, tasks.get(0).getGrade());
    }

    // ── getAllMembers with full details (findAllWithDetails path) ─────────────

    @Test
    void getAllMembers_includesTasksAndSkills() {
        TeamMemberDTO member = memberService.createMember(CreateUpdateMemberDTO.of("Full", "Details"));
        com.example.dto.TaskDTO task = taskService.addTask(member.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Implement feature", "PR #1", com.example.model.TaskStatus.PENDING));
        taskService.updateTask(task.getId(),
                com.example.dto.CreateUpdateTaskDTO.of("Implement feature", "PR #1", com.example.model.TaskStatus.COMPLETED));
        taskService.gradeTask(task.getId(), 7);
        memberService.addSkill(member.getId(), "JAVA");

        List<TeamMemberDTO> all = memberService.getAllMembers();

        assertEquals(1, all.size());
        TeamMemberDTO dto = all.get(0);
        assertEquals(1, dto.getTasks().size());
        assertEquals("Implement feature", dto.getTasks().get(0).getTaskName());
        assertEquals(com.example.model.TaskStatus.COMPLETED, dto.getTasks().get(0).getStatus());
        assertEquals(7, dto.getTasks().get(0).getGrade());
        assertTrue(dto.getSkills().contains("JAVA"));
        assertEquals(7.0, dto.getAverageGrade(), 0.001);
    }
}

