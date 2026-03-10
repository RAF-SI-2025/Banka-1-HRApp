package com.example.view;

import com.example.config.AppConfig;
import com.example.dto.TaskDTO;
import com.example.dto.TeamMemberDTO;
import com.example.model.TaskStatus;
import com.example.service.TaskService;
import com.example.service.TeamMemberService;
import com.example.util.AsyncRunner;
import com.example.view.dialog.AddEditMemberDialog;
import com.example.view.dialog.AddEditTaskDialog;
import com.example.view.dialog.GradeDialog;
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

import java.util.Optional;


/**
 * Main application window.
 * Layout: SplitPane — member list on the left, detail panel (Tasks + Skills tabs) on the right.
 * Grades are assigned automatically when a task is marked COMPLETED.
 */
public class MainStage extends Stage {

    private final TeamMemberService memberService;
    private final TaskService       taskService;

    // ── Left panel ────────────────────────────────────────────────────────────
    private final ObservableList<TeamMemberDTO> memberList = FXCollections.observableArrayList();
    private TableView<TeamMemberDTO> memberTable;

    // ── Right panel — tasks ───────────────────────────────────────────────────
    private final ObservableList<TaskDTO> taskList = FXCollections.observableArrayList();
    private TableView<TaskDTO> taskTable;

    // ── Right panel — skills ──────────────────────────────────────────────────
    private final ObservableList<String> skillList = FXCollections.observableArrayList();
    private ListView<String> skillListView;

    // ── Header labels ─────────────────────────────────────────────────────────
    private Label memberHeaderLabel;
    private Label avgGradeHeaderLabel;

    public MainStage(TeamMemberService memberService, TaskService taskService) {
        this.memberService = memberService;
        this.taskService   = taskService;
        initialise();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initialise() {
        setTitle(AppConfig.getAppTitle());
        setWidth(AppConfig.getAppWidth());
        setHeight(AppConfig.getAppHeight());

        BorderPane root = new BorderPane();
        root.setTop(buildToolBar());
        root.setCenter(buildMainContent());
        setScene(new Scene(root));
        loadMembers();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private ToolBar buildToolBar() {
        Button addBtn    = new Button("+ Add Member");
        Button editBtn   = new Button("Edit Member");
        Button deleteBtn = new Button("Delete Member");
        addBtn.setOnAction(e    -> handleAddMember());
        editBtn.setOnAction(e   -> handleEditMember());
        deleteBtn.setOnAction(e -> handleDeleteMember());
        return new ToolBar(addBtn, editBtn, deleteBtn);
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private SplitPane buildMainContent() {
        SplitPane split = new SplitPane(buildMemberListPanel(), buildDetailPanel());
        split.setDividerPositions(AppConfig.getDividerPosition());
        return split;
    }

    private VBox buildMemberListPanel() {
        Label title = new Label("Team Members");
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name…");

        FilteredList<TeamMemberDTO> filtered = new FilteredList<>(memberList, m -> true);
        searchField.textProperty().addListener((obs, o, n) -> {
            String f = n == null ? "" : n.trim().toLowerCase();
            filtered.setPredicate(m -> f.isEmpty()
                    || m.getName().toLowerCase().contains(f)
                    || m.getSurname().toLowerCase().contains(f));
        });

        memberTable = new TableView<>(filtered);
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
                .addListener((obs, old, sel) -> onMemberSelected(sel));

        VBox panel = new VBox(8, title, searchField, memberTable);
        panel.setPadding(new Insets(10));
        VBox.setVgrow(memberTable, Priority.ALWAYS);
        return panel;
    }

    private VBox buildDetailPanel() {
        memberHeaderLabel   = new Label("Select a team member");
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

    private Tab buildTasksTab() {
        taskTable = new TableView<>(taskList);
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        taskTable.setPlaceholder(new Label("No tasks yet."));

        TableColumn<TaskDTO, String> nameCol = new TableColumn<>("Task Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTaskName()));

        TableColumn<TaskDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));

        TableColumn<TaskDTO, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(d -> {
            Integer g = d.getValue().getGrade();
            return new SimpleStringProperty(g != null ? String.valueOf(g) : "—");
        });

        TableColumn<TaskDTO, String> commentCol = new TableColumn<>("Comment");
        commentCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getComment()));

        taskTable.getColumns().addAll(nameCol, statusCol, gradeCol, commentCol);

        Button addBtn    = new Button("Add Task");
        Button editBtn   = new Button("Edit Task");
        Button deleteBtn = new Button("Delete Task");
        addBtn.setOnAction(e    -> handleAddTask());
        editBtn.setOnAction(e   -> handleEditTask());
        deleteBtn.setOnAction(e -> handleDeleteTask());

        HBox buttons = new HBox(8, addBtn, editBtn, deleteBtn);
        buttons.setPadding(new Insets(5, 0, 0, 0));

        VBox content = new VBox(8, taskTable, buttons);
        content.setPadding(new Insets(10));
        VBox.setVgrow(taskTable, Priority.ALWAYS);
        return new Tab("Tasks", content);
    }

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

    private void loadMembers() {
        long selectedId = getSelectedMemberId();
        AsyncRunner.run(
                memberService::getAllMembers,
                members -> {
                    memberList.setAll(members);
                    restoreSelection(selectedId);
                });
    }

    private void refreshSelectedMember() {
        TeamMemberDTO selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        long selectedId = selected.getId();
        AsyncRunner.run(
                () -> memberService.getMemberById(selectedId),
                updated -> {
                    int index = memberList.indexOf(selected);
                    if (index >= 0) {
                        memberList.set(index, updated);
                        memberTable.getSelectionModel().select(index);
                        onMemberSelected(updated);
                    }
                });
    }

    private long getSelectedMemberId() {
        TeamMemberDTO sel = memberTable.getSelectionModel().getSelectedItem();
        return sel != null ? sel.getId() : -1;
    }

    private void restoreSelection(long id) {
        if (id < 0) return;
        memberList.stream().filter(m -> m.getId() == id).findFirst()
                .ifPresent(m -> {
                    memberTable.getSelectionModel().select(m);
                    onMemberSelected(m);
                });
    }

    private void onMemberSelected(TeamMemberDTO member) {
        if (member == null) {
            memberHeaderLabel.setText("Select a team member");
            avgGradeHeaderLabel.setText("");
            taskList.clear();
            skillList.clear();
            return;
        }
        memberHeaderLabel.setText(member.getName() + " " + member.getSurname());
        String avgText = member.getAverageGrade() == 0 ? "N/A"
                : String.format("%.1f", member.getAverageGrade());
        avgGradeHeaderLabel.setText("Avg Grade: " + avgText);
        taskList.setAll(member.getTasks());
        skillList.setAll(member.getSkills());
    }

    // ── Member handlers ───────────────────────────────────────────────────────

    private void handleAddMember() {
        new AddEditMemberDialog(null).showAndWait().ifPresent(dto ->
                AsyncRunner.run(
                        () -> memberService.createMember(dto),
                        newMember -> {
                            memberList.add(newMember);
                            memberTable.getSelectionModel().select(newMember);
                            onMemberSelected(newMember);
                        }));
    }

    private void handleEditMember() {
        TeamMemberDTO sel = memberTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Please select a team member to edit."); return; }
        new AddEditMemberDialog(sel).showAndWait().ifPresent(dto ->
                AsyncRunner.run(
                        () -> memberService.updateMember(sel.getId(), dto),
                        updated -> {
                            int idx = memberList.indexOf(sel);
                            if (idx >= 0) {
                                memberList.set(idx, updated);
                                memberTable.getSelectionModel().select(idx);
                                onMemberSelected(updated);
                            }
                        }));
    }

    private void handleDeleteMember() {
        TeamMemberDTO sel = memberTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Please select a team member to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + sel.getName() + " " + sel.getSurname() + " and all their tasks?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                AsyncRunner.run(
                        () -> memberService.deleteMember(sel.getId()),
                        () -> {
                            memberList.remove(sel);
                            if (!memberList.isEmpty()) memberTable.getSelectionModel().selectFirst();
                            else clearDetailPanel();
                        });
            }
        });
    }

    // ── Task handlers ─────────────────────────────────────────────────────────

    private void handleAddTask() {
        TeamMemberDTO member = memberTable.getSelectionModel().getSelectedItem();
        if (member == null) { showWarning("Please select a team member first."); return; }
        new AddEditTaskDialog(null).showAndWait().ifPresent(dto ->
                AsyncRunner.run(
                        () -> taskService.addTask(member.getId(), dto),
                        result -> refreshSelectedMember()));
    }

    /**
     * Opens the edit-task dialog. If the user selects COMPLETED, a grade dialog is shown
     * immediately after. The grade is then passed to the service together with the task update.
     */
    private void handleEditTask() {
        TaskDTO selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showWarning("Please select a task to edit."); return; }

        TeamMemberDTO member = memberTable.getSelectionModel().getSelectedItem();

        new AddEditTaskDialog(selected).showAndWait().ifPresent(dto -> {
            Integer grade = null;

            if (dto.getStatus() == TaskStatus.COMPLETED) {
                // Ask for a grade — pre-fill with existing grade if already completed
                Integer existing = selected.getGrade();
                Optional<Integer> gradeOpt = new GradeDialog(
                        member.getName() + " " + member.getSurname(),
                        selected.getTaskName(),
                        existing).showAndWait();

                if (gradeOpt.isEmpty()) return; // user cancelled grade dialog — abort whole edit
                grade = gradeOpt.get();
            }

            final Integer finalGrade = grade;
            AsyncRunner.run(
                    () -> taskService.updateTask(member.getId(), selected.getId(), dto, finalGrade),
                    result -> refreshSelectedMember());
        });
    }

    private void handleDeleteTask() {
        TaskDTO sel = taskTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Please select a task to delete."); return; }
        AsyncRunner.run(() -> taskService.deleteTask(sel.getId()), this::refreshSelectedMember);
    }

    // ── Skill handlers ────────────────────────────────────────────────────────

    private void handleAddSkill() {
        TeamMemberDTO member = memberTable.getSelectionModel().getSelectedItem();
        if (member == null) { showWarning("Please select a team member first."); return; }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Skill");
        dialog.setHeaderText("Add a skill for " + member.getName() + " " + member.getSurname() + ".");
        dialog.setContentText("Skill name:");
        dialog.showAndWait().ifPresent(skill -> {
            if (!skill.trim().isEmpty()) {
                AsyncRunner.run(
                        () -> memberService.addSkill(member.getId(), skill),
                        this::refreshSelectedMember);
            }
        });
    }

    private void handleRemoveSkill() {
        TeamMemberDTO member = memberTable.getSelectionModel().getSelectedItem();
        String skill = skillListView.getSelectionModel().getSelectedItem();
        if (skill == null) { showWarning("Please select a skill to remove."); return; }
        AsyncRunner.run(
                () -> memberService.removeSkill(member.getId(), skill),
                this::refreshSelectedMember);
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void clearDetailPanel() {
        memberHeaderLabel.setText("Select a team member");
        avgGradeHeaderLabel.setText("");
        taskList.clear();
        skillList.clear();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Action Required");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
