package com.example.repository;

import com.example.model.TeamMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository responsible for all CRUD operations on the {@code team_members} table.
 * Uses soft-delete: rows are never physically removed; {@code is_deleted = 1} marks them hidden.
 */
public class TeamMemberRepository {

    private static final Logger log = LoggerFactory.getLogger(TeamMemberRepository.class);

    private final DatabaseManager dbManager;

    /**
     * Constructs the repository with the required database manager (constructor injection).
     *
     * @param dbManager the shared database manager
     */
    public TeamMemberRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a new team member and populates the generated {@code id} on the entity.
     *
     * @param member the member to insert (id will be set after save)
     * @throws SQLException on database error
     */
    public void save(TeamMember member) throws SQLException {
        String sql = "INSERT INTO team_members (name, surname) VALUES (?, ?)";
        try (PreparedStatement ps = dbManager.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getSurname());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    member.setId(keys.getLong(1));
                }
            }
        }
        log.info("Saved team member id={} ({} {})", member.getId(), member.getName(), member.getSurname());
    }

    /**
     * Updates an existing team member's name and surname.
     *
     * @param member the member with updated values (must have a valid id)
     * @throws SQLException on database error
     */
    public void update(TeamMember member) throws SQLException {
        String sql = "UPDATE team_members SET name = ?, surname = ? WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getSurname());
            ps.setLong(3, member.getId());
            ps.executeUpdate();
        }
        log.info("Updated team member id={}", member.getId());
    }

    /**
     * Soft-deletes a team member by setting {@code is_deleted = 1}.
     * The row remains in the database; cascade soft-delete of tasks is handled separately.
     *
     * @param id the member's database ID
     * @throws SQLException on database error
     */
    public void softDelete(long id) throws SQLException {
        String sql = "UPDATE team_members SET is_deleted = 1 WHERE id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        log.info("Soft-deleted team member id={}", id);
    }

    /**
     * Returns all non-deleted team members ordered by surname then name.
     *
     * @return list of team members (without their related tasks/skills/grades)
     * @throws SQLException on database error
     */
    public List<TeamMember> findAll() throws SQLException {
        String sql = "SELECT id, name, surname FROM team_members WHERE is_deleted = 0 ORDER BY surname, name";
        List<TeamMember> members = new ArrayList<>();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TeamMember m = new TeamMember(rs.getString("name"), rs.getString("surname"));
                m.setId(rs.getLong("id"));
                members.add(m);
            }
        }
        return members;
    }

    /**
     * Finds a single non-deleted team member by ID.
     *
     * @param id the member's database ID
     * @return an {@link Optional} containing the member, or empty if not found
     * @throws SQLException on database error
     */
    public Optional<TeamMember> findById(long id) throws SQLException {
        String sql = "SELECT id, name, surname FROM team_members WHERE id = ? AND is_deleted = 0";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TeamMember m = new TeamMember(rs.getString("name"), rs.getString("surname"));
                    m.setId(rs.getLong("id"));
                    return Optional.of(m);
                }
            }
        }
        return Optional.empty();
    }
}
