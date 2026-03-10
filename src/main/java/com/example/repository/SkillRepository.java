package com.example.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository responsible for all CRUD operations on the {@code skills} table.
 * Skills are stored as plain strings and use hard-delete (they carry no historical value).
 */
public class SkillRepository {

    private static final Logger log = LoggerFactory.getLogger(SkillRepository.class);

    private final DatabaseManager dbManager;

    /**
     * Constructs the repository with the required database manager (constructor injection).
     *
     * @param dbManager the shared database manager
     */
    public SkillRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Inserts a skill for the given member, ignoring duplicates (case-insensitive).
     *
     * @param skill    the skill name (already normalised/uppercased by the caller)
     * @param memberId the owning member's database ID
     * @throws SQLException on database error
     */
    public void save(String skill, long memberId) throws SQLException {
        if (existsForMember(skill, memberId)) {
            log.warn("Skill '{}' already exists for member id={} — skipping insert.", skill, memberId);
            return;
        }
        String sql = "INSERT INTO skills (skill_name, member_id) VALUES (?, ?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, skill);
            ps.setLong(2, memberId);
            ps.executeUpdate();
        }
        log.info("Saved skill '{}' for member id={}", skill, memberId);
    }

    /**
     * Hard-deletes a skill for the given member.
     *
     * @param skill    the exact skill name to remove
     * @param memberId the owning member's database ID
     * @throws SQLException on database error
     */
    public void delete(String skill, long memberId) throws SQLException {
        String sql = "DELETE FROM skills WHERE skill_name = ? AND member_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, skill);
            ps.setLong(2, memberId);
            ps.executeUpdate();
        }
        log.info("Deleted skill '{}' for member id={}", skill, memberId);
    }

    /**
     * Returns all skills for the given member, ordered alphabetically.
     *
     * @param memberId the owning member's database ID
     * @return list of skill names
     * @throws SQLException on database error
     */
    public List<String> findByMemberId(long memberId) throws SQLException {
        String sql = "SELECT skill_name FROM skills WHERE member_id = ? ORDER BY skill_name";
        List<String> skills = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    skills.add(rs.getString("skill_name"));
                }
            }
        }
        return skills;
    }

    /**
     * Checks whether a skill with the given name already exists for the given member.
     *
     * @param skill    the skill name to check
     * @param memberId the owning member's database ID
     * @return {@code true} if the skill already exists
     * @throws SQLException on database error
     */
    private boolean existsForMember(String skill, long memberId) throws SQLException {
        String sql = "SELECT 1 FROM skills WHERE skill_name = ? AND member_id = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, skill);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
