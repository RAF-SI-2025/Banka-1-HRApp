package com.example;

import com.example.repository.DatabaseManager;
import com.example.repository.GradeRepository;
import com.example.repository.JdbcTransactionManager;
import com.example.repository.SkillRepository;
import com.example.repository.TaskRepository;
import com.example.repository.TeamMemberRepository;
import com.example.service.TaskService;
import com.example.service.TeamMemberService;
import com.example.repository.TransactionManager;
import com.example.view.MainStage;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Application entry point.
 * <p>
 * Responsible for wiring the dependency graph (poor-man's DI container):
 * repositories are constructed first, then services, then the view.
 * No class below this level uses {@code new} on a service or repository —
 * all dependencies are injected through constructors.
 */
public class Main extends Application {

    /** Shared database manager held here so we can close it on exit. */
    private DatabaseManager dbManager;

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Wires the dependency graph and shows the main window.
     *
     * @param stage the primary stage provided by JavaFX (unused — MainStage creates its own)
     */
    @Override
    public void start(Stage stage) {
        // ── Infrastructure ───────────────────────────────────────────────────
        dbManager = DatabaseManager.getInstance();
        TransactionManager txManager = new JdbcTransactionManager(dbManager);

        // ── Repositories ─────────────────────────────────────────────────────
        TeamMemberRepository memberRepo = new TeamMemberRepository(dbManager);
        TaskRepository       taskRepo   = new TaskRepository(dbManager);
        SkillRepository      skillRepo  = new SkillRepository(dbManager);
        GradeRepository      gradeRepo  = new GradeRepository(dbManager);

        // ── Services ─────────────────────────────────────────────────────────
        TeamMemberService memberService = new TeamMemberService(memberRepo, taskRepo, skillRepo, gradeRepo, txManager);
        TaskService       taskService   = new TaskService(taskRepo, gradeRepo, txManager);

        // ── View ─────────────────────────────────────────────────────────────
        MainStage mainStage = new MainStage(memberService, taskService);
        mainStage.show();
    }

    /**
     * Called by JavaFX when the application is about to exit.
     * Closes the database connection cleanly.
     */
    @Override
    public void stop() {
        if (dbManager != null) {
            dbManager.close();
        }
    }
}