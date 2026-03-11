package com.example.integration;

import com.example.repository.*;
import com.example.service.TaskService;
import com.example.service.TeamMemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base class for integration tests.
 * <p>
 * Spins up a fresh in-memory SQLite database for every test, runs the schema
 * DDL manually (same tables as the Flyway migrations), builds real repository
 * and service instances, and tears down the connection afterwards.
 * No mocks — every layer from service down to SQL is exercised.
 */
public abstract class IntegrationTestBase {

    protected Connection connection;
    protected DatabaseManager dbManager;

    protected TeamMemberRepository memberRepo;
    protected TaskRepository       taskRepo;
    protected SkillRepository      skillRepo;
    protected GradeRepository      gradeRepo;
    protected TransactionManager   txManager;

    protected TeamMemberService memberService;
    protected TaskService       taskService;

    @BeforeEach
    void setUpDatabase() throws SQLException {
        // Fresh in-memory database — isolated per test
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("""
                    CREATE TABLE team_members (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        name       TEXT    NOT NULL,
                        surname    TEXT    NOT NULL,
                        is_deleted INTEGER NOT NULL DEFAULT 0
                    )""");
            stmt.execute("""
                    CREATE TABLE tasks (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        task_name  TEXT    NOT NULL,
                        comment    TEXT    NOT NULL DEFAULT '',
                        status     TEXT    NOT NULL DEFAULT 'PENDING',
                        member_id  INTEGER NOT NULL,
                        is_deleted INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE
                    )""");
            stmt.execute("""
                    CREATE TABLE skills (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        skill_name TEXT    NOT NULL,
                        member_id  INTEGER NOT NULL,
                        FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE
                    )""");
            stmt.execute("""
                    CREATE TABLE grades (
                        id      INTEGER PRIMARY KEY AUTOINCREMENT,
                        grade   INTEGER NOT NULL,
                        task_id INTEGER NOT NULL UNIQUE,
                        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
                    )""");
        }

        // Wire up the real stack using the package-private test constructor
        dbManager   = new DatabaseManager(connection);
        memberRepo  = new TeamMemberRepository(dbManager);
        taskRepo    = new TaskRepository(dbManager);
        skillRepo   = new SkillRepository(dbManager);
        gradeRepo   = new GradeRepository(dbManager);
        txManager   = new JdbcTransactionManager(dbManager);

        memberService = new TeamMemberService(memberRepo, taskRepo, skillRepo, gradeRepo, txManager);
        taskService   = new TaskService(taskRepo, gradeRepo, txManager);
    }

    @AfterEach
    void tearDownDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}

