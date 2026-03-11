package com.example.view;

import com.example.config.AppConfig;
import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.dto.TeamMemberDTO;
import com.example.exception.GlobalExceptionHandler;
import com.example.service.TaskService;
import com.example.service.TeamMemberService;
import com.example.view.dialog.AddEditMemberDialog;
import com.example.view.dialog.AddEditTaskDialog;
import com.example.view.dialog.AddGradeDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

/**
 * Main application window.
 * <p>
 * Layout: SplitPane with a member list on the left and a detail panel
 * (tabs for Tasks, Skills) on the right. All business operations
 * are delegated to the injected service layer — no logic lives in this class.
 */
public class MainStage extends Stage {

    private final TeamMemberService memberService;
    private final TaskService taskService;

    // ── Left panel ───────────────────────────────────────────────────────────
    private final ObservableList<TeamMemberDTO> memberList = FXCollections.observableArrayList();
    private TableView<TeamMemberDTO> memberTable;

    // ── Right panel — tasks ──────────────────────────────────────────────────
    private final ObservableList<TaskDTO> taskList = FXCollections.observableArrayList();
    private TableView<TaskDTO> taskTable;

    // ── Right panel — skills ─────────────────────────────────────────────────
    private final ObservableList<String> skillList = FXCollections.observableArrayList();
    private ListView<String> skillListView;

    // ── Header labels ────────────────────────────────────────────────────────
    private Label memberHeaderLabel;
    private Label avgGradeHeaderLabel;

    /**
     * Constructs the main stage with all required services (constructor injection).
     *
     * @param memberService service for team member operations
     * @param taskService   service for task operations
     */
    public MainStage(TeamMemberService memberService, TaskService taskService) {
        this.memberService = memberService;
        this.taskService   = taskService;
        initialise();
    }

    // ── Initialisation ───────────────────────────────────────────────────────

    /**
     * Builds the scene graph and loads initial data.
     */
    private void initialise() {
        setTitle(AppConfig.getAppTitle());
        setWidth(AppConfig.getAppWidth());
        setHeight(AppConfig.getAppHeight());

        BorderPane root = new BorderPane();
        root.setTop(buildToolBar());
        root.setCenter(buildMainContent());

        setScene(new Scene(root));
        setOnCloseRequest(e -> {
            // DatabaseManager.close() is called from Main via shutdown hook
        });

        loadMembers();
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    /**
     * Builds the top toolbar with member management buttons.
     *
     * @return the configured {@link ToolBar}
     */
    private ToolBar buildToolBar() {
        Button addBtn    = new Button("+ Add Member");
        Button editBtn   = new Button("Edit Member");
        Button deleteBtn = new Button("Delete Member");

        addBtn.setOnAction(e    -> handleAddMember());
        editBtn.setOnAction(e   -> handleEditMember());
        deleteBtn.setOnAction(e -> handleDeleteMember());

        return new ToolBar(addBtn, editBtn, deleteBtn);
    }

    // ── Main content ─────────────────────────────────────────────────────────

    /**
     * Builds the central SplitPane containing the member list and the detail panel.
     *
     * @return the configured {@link SplitPane}
     */
    private SplitPane buildMainContent() {
        SplitPane splitPane = new SplitPane(buildMemberListPanel(), buildDetailPanel());
        splitPane.setDividerPositions(AppConfig.getDividerPosition());
        return splitPane;
    }

    // ── Left panel: member list ───────────────────────────────────────────────

    /**
     * Builds the left panel containing a search field and the team members {@link TableView}.
     *
     * @return the configured panel
     */
    private VBox buildMemberListPanel() {
        Label title = new Label("Team Members");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        //Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name…");

        FilteredList<TeamMemberDTO> filteredMembers = new FilteredList<>(memberList, m -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredMembers.setPredicate(m ->
                filter.isEmpty() ||
                m.getName().toLowerCase().contains(filter) ||
                m.getSurname().toLowerCase().contains(filter)
            );
        });

        memberTable = new TableView<>(filteredMembers);
        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        memberTable.setPlaceholder(new Label("No team members yet."));

        TableColumn<TeamMemberDTO, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<TeamMemberDTO, String> surnameCol = new TableColumn<>("Surname");
        surnameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSurname()));

        TableColumn<TeamMemberDTO, String> avgCol = new TableColumn<>("Avg Grade");
        avgCol.setCellValueFactory(d -> {
            double avg = d.getValue().getAverageGrade();
            return new SimpleStringProperty(avg == 0 ? "N/A" : String.format("%.1f", avg));
        });

        TableColumn<TeamMemberDTO, String> tasksCol = new TableColumn<>("P / C / F");
        tasksCol.setCellValueFactory(d -> {
            TeamMemberDTO m = d.getValue();
            return new SimpleStringProperty(
                    m.getPendingCount() + " / " + m.getCompletedCount() + " / " + m.getFailedCount());
        });

        memberTable.getColumns().addAll(nameCol, surnameCol, avgCol, tasksCol);
        memberTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> onMemberSelected(selected));

        VBox panel = new VBox(8, title, searchField, memberTable);
        panel.setPadding(new Insets(10));
        VBox.setVgrow(memberTable, Priority.ALWAYS);
        return panel;
    }

    // ── Right panel: detail view ──────────────────────────────────────────────

    /**
     * Builds the right detail panel with a header and a tabbed content area.
     *
     * @return the configured panel
     */
    private VBox buildDetailPanel() {
        memberHeaderLabel  = new Label("Select a team member");
        memberHeaderLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        avgGradeHeaderLabel = new Label("");

        HBox header = new HBox(20, memberHeaderLabel, avgGradeHeaderLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(buildTasksTab(), buildSkillsTab());

        VBox panel = new VBox(10, header, tabs);
        panel.setPadding(new Insets(10));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return panel;
    }

    /**
     * Builds the Tasks tab with a task table and action buttons.
     *
     * @return the configured {@link Tab}
     */
    private Tab buildTasksTab() {
        taskTable = new TableView<>(taskList);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        taskTable.setPlaceholder(new Label("No tasks yet."));

        TableColumn<TaskDTO, String> nameCol = new TableColumn<>("Task Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTaskName()));

        TableColumn<TaskDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));

        TableColumn<TaskDTO, String> commentCol = new TableColumn<>("Comment");
        commentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComment()));

        TableColumn<TaskDTO, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> {
            int grade = d.getValue().getGrade();
            return new SimpleStringProperty(grade == 0 ? "—" : String.valueOf(grade));
        });

        taskTable.getColumns().addAll(nameCol, statusCol, commentCol, gradeCol);

        Button addBtn    = new Button("Add Task");
        Button editBtn   = new Button("Edit Task");
        Button deleteBtn = new Button("Delete Task");
        Button gradeBtn  = new Button("Grade Task");

        gradeBtn.setDisable(true);
        taskTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    boolean notGradeable = selected == null
                            || selected.getStatus() == com.example.model.TaskStatus.PENDING
                            || selected.getStatus() == com.example.model.TaskStatus.FAILED;
                    gradeBtn.setDisable(notGradeable);
                });

        addBtn.setOnAction(e    -> handleAddTask());
        editBtn.setOnAction(e   -> handleEditTask());
        deleteBtn.setOnAction(e -> handleDeleteTask());
        gradeBtn.setOnAction(e  -> handleGradeTask());

        HBox buttons = new HBox(8, addBtn, editBtn, deleteBtn, gradeBtn);
        buttons.setPadding(new Insets(5, 0, 0, 0));

        VBox content = new VBox(8, taskTable, buttons);
        content.setPadding(new Insets(10));
        VBox.setVgrow(taskTable, Priority.ALWAYS);

        return new Tab("Tasks", content);
    }

    /**
     * Builds the Skills tab with a skill list and action buttons.
     *
     * @return the configured {@link Tab}
     */
    private Tab buildSkillsTab() {
        skillListView = new ListView<>(skillList);

        Button addBtn    = new Button("Add Skill");
        Button removeBtn = new Button("Remove Skill");

        addBtn.setOnAction(e    -> handleAddSkill());
        removeBtn.setOnAction(e -> handleRemoveSkill());

        HBox buttons = new HBox(8, addBtn, removeBtn);
        buttons.setPadding(new Insets(5, 0, 0, 0));

        VBox content = new VBox(8, skillListView, buttons);
        content.setPadding(new Insets(10));
        VBox.setVgrow(skillListView, Priority.ALWAYS);

        return new Tab("Skills", content);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * Reloads the full member list from the service and restores any prior selection.
     */
    private void loadMembers() {
        long selectedId = getSelectedMemberId();
        runAsync(memberService::getAllMembers, members -> {
            memberList.setAll(members);
            restoreSelection(selectedId);
        });
    }

    /**
     * Refreshes only the currently selected member's data from the database.
     */
    private void refreshSelectedMember() {
        TeamMemberDTO selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        long selectedId = selected.getId();
        int index = memberList.indexOf(selected);
        TaskDTO selectedTask = taskTable.getSelectionModel().getSelectedItem();
        long selectedTaskId = selectedTask != null ? selectedTask.getId() : -1;

        runAsync(() -> memberService.getMemberById(selectedId), updated -> {
            if (index >= 0) {
                memberList.set(index, updated);
                memberTable.getSelectionModel().select(index);
                onMemberSelected(updated);
                // Restore task selection
                if (selectedTaskId >= 0) {
                    for (int i = 0; i < taskList.size(); i++) {
                        if (taskList.get(i).getId() == selectedTaskId) {
                            taskTable.getSelectionModel().select(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Returns the ID of the currently selected member, or -1 if nothing is selected.
     *
     * @return selected member ID or -1
     */
    private long getSelectedMemberId() {
        TeamMemberDTO selected = memberTable.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getId() : -1;
    }

    /**
     * Re-selects the member with the given ID after a list reload.
     *
     * @param id the ID to re-select, or -1 to skip
     */
    private void restoreSelection(long id) {
        if (id < 0) return;
        memberList.stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .ifPresent(m -> {
                    memberTable.getSelectionModel().select(m);
                    onMemberSelected(m);
                });
    }

    /**
     * Updates the right panel when a member is selected in the table.
     *
     * @param member the newly selected member DTO, or {@code null} if deselected
     */
    private void onMemberSelected(TeamMemberDTO member) {
        if (member == null) {
            memberHeaderLabel.setText("Select a team member");
            avgGradeHeaderLabel.setText("");
            taskList.clear();
            skillList.clear();
            return;
        }
        memberHeaderLabel.setText(member.getName() + " " + member.getSurname());
        refreshAvgLabel(member.getAverageGrade());
        taskList.setAll(member.getTasks());
        skillList.setAll(member.getSkills());
    }

    /**
     * Updates the average grade label in the header.
     *
     * @param avg the average grade value (0 means no grades)
     */
    private void refreshAvgLabel(double avg) {
        String text = avg == 0 ? "N/A" : String.format("%.1f", avg);
        avgGradeHeaderLabel.setText("Avg Grade: " + text);
    }

    // ── Member handlers ───────────────────────────────────────────────────────

    /** Opens the add-member dialog and persists the result. */
    private void handleAddMember() {
        new AddEditMemberDialog(null).showAndWait().ifPresent(dto -> {
            runAsync(() -> memberService.createMember(dto), newMember -> {
                memberList.add(newMember);
                memberTable.getSelectionModel().select(newMember);
                onMemberSelected(newMember);
            });
        });
    }

    /** Opens the edit-member dialog for the selected member and persists the result. */
    private void handleEditMember() {
        TeamMemberDTO selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a team member to edit."); return; }

        new AddEditMemberDialog(selected).showAndWait().ifPresent(dto -> {
            int index = memberList.indexOf(selected);
            runAsync(() -> memberService.updateMember(selected.getId(), dto), updated -> {
                if (index >= 0) {
                    memberList.set(index, updated);
                    memberTable.getSelectionModel().select(index);
                    onMemberSelected(updated);
                }
            });
        });
    }

    /** Confirms and soft-deletes the selected member. */
    private void handleDeleteMember() {
        TeamMemberDTO selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a team member to delete."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + " " + selected.getSurname() + " and all their tasks?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                runAsync(() -> { memberService.deleteMember(selected.getId()); return null; }, ignored -> {
                    memberList.remove(selected);
                    if (!memberList.isEmpty()) {
                        memberTable.getSelectionModel().selectFirst();
                    } else {
                        clearDetailPanel();
                    }
                });
            }
        });
    }

    // ── Task handlers ─────────────────────────────────────────────────────────

    /** Opens the add-task dialog and persists the result for the selected member. */
    private void handleAddTask() {
        TeamMemberDTO member = memberTable.getSelectionModel().getSelectedItem();
        if (member == null) { showWarning("Please select a team member first."); return; }

        new AddEditTaskDialog(null).showAndWait().ifPresent(dto -> {
            runAsync(() -> { taskService.addTask(member.getId(), dto); return null; }, ignored -> refreshSelectedMember());
        });
    }

    /** Opens the edit-task dialog for the selected task and persists the result. */
    private void handleEditTask() {
        TaskDTO selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a task to edit."); return; }

        new AddEditTaskDialog(selected).showAndWait().ifPresent(dto -> {
            runAsync(() -> { taskService.updateTask(selected.getId(), dto); return null; }, ignored -> refreshSelectedMember());
        });
    }

    /** Confirms and soft-deletes the selected task. */
    private void handleDeleteTask() {
        TaskDTO selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a task to delete."); return; }

        runAsync(() -> { taskService.deleteTask(selected.getId()); return null; }, ignored -> refreshSelectedMember());
    }

    // ── Skill handlers ────────────────────────────────────────────────────────

    /** Prompts for a skill name and adds it to the selected member. */
    private void handleAddSkill() {
        TeamMemberDTO member = memberTable.getSelectionModel().getSelectedItem();
        if (member == null) { showWarning("Please select a team member first."); return; }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Skill");
        dialog.setHeaderText("Add a skill for " + member.getName() + " " + member.getSurname() + ".");
        dialog.setContentText("Skill name:");

        dialog.showAndWait().ifPresent(skill -> {
            if (!skill.trim().isEmpty()) {
                runAsync(() -> { memberService.addSkill(member.getId(), skill); return null; }, ignored -> refreshSelectedMember());
            }
        });
    }

    /** Removes the selected skill from the selected member. */
    private void handleRemoveSkill() {
        TeamMemberDTO member        = memberTable.getSelectionModel().getSelectedItem();
        String        selectedSkill = skillListView.getSelectionModel().getSelectedItem();
        if (selectedSkill == null) { showWarning("Please select a skill to remove."); return; }

        runAsync(() -> { memberService.removeSkill(member.getId(), selectedSkill); return null; }, ignored -> refreshSelectedMember());
    }

    // ── Grade handlers ────────────────────────────────────────────────────────

    /** Opens the grade dialog for the selected task and persists the result. */
    private void handleGradeTask() {
        TaskDTO selectedTask = taskTable.getSelectionModel().getSelectedItem();
        if (selectedTask == null) { showWarning("Please select a task to grade."); return; }

        new AddGradeDialog(selectedTask.getTaskName())
                .showAndWait().ifPresent(grade -> {
                    runAsync(() -> { taskService.gradeTask(selectedTask.getId(), grade); return null; }, ignored -> refreshSelectedMember());
                });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Clears all detail-panel data (called after a member is deleted).
     */
    private void clearDetailPanel() {
        memberHeaderLabel.setText("Select a team member");
        avgGradeHeaderLabel.setText("");
        taskList.clear();
        skillList.clear();
    }

    /**
     * Runs {@code dbCall} on a background thread, then invokes {@code onSuccess}
     * on the JavaFX application thread with the result.
     * Errors are forwarded to {@link GlobalExceptionHandler}.
     *
     * @param dbCall    the database operation to execute off the UI thread
     * @param onSuccess callback executed on the FX thread when the operation succeeds
     */
    private <T> void runAsync(Callable<T> dbCall, Consumer<T> onSuccess) {
        Task<T> bgTask = new Task<>() {
            @Override
            protected T call() throws Exception {
                return dbCall.call();
            }
        };
        bgTask.setOnSucceeded(e -> onSuccess.accept(bgTask.getValue()));
        bgTask.setOnFailed(e -> GlobalExceptionHandler.handle(bgTask.getException()));
        new Thread(bgTask, "db-worker").start();
    }

    /**
     * Shows a simple warning dialog with the given message.
     *
     * @param message the message to display
     */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Action Required");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
