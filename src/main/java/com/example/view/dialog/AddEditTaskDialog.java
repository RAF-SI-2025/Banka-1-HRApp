package com.example.view.dialog;

import com.example.dto.CreateUpdateTaskDTO;
import com.example.dto.TaskDTO;
import com.example.model.TaskStatus;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Modal dialog for adding a new task or editing an existing one.
 * Returns a validated {@link CreateUpdateTaskDTO} on confirmation,
 * or {@code null} if the user cancels.
 */
public class AddEditTaskDialog extends Dialog<CreateUpdateTaskDTO> {

    /**
     * Constructs the dialog.
     *
     * @param existing the task to pre-populate fields with, or {@code null} for a new task
     */
    public AddEditTaskDialog(TaskDTO existing) {
        setTitle(existing == null ? "Add Task" : "Edit Task");
        setHeaderText(existing == null ? "Enter details for the new task." : "Update task details.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField taskNameField = new TextField();
        taskNameField.setPromptText("Task name");

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Comment (optional)");
        commentArea.setPrefRowCount(3);
        commentArea.setWrapText(true);

        ComboBox<TaskStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(TaskStatus.values());
        statusCombo.setValue(TaskStatus.PENDING);

        if (existing != null) {
            taskNameField.setText(existing.getTaskName());
            commentArea.setText(existing.getComment());
            statusCombo.setValue(existing.getStatus());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Task Name:"), 0, 0);
        grid.add(taskNameField,           1, 0);
        grid.add(new Label("Status:"),    0, 1);
        grid.add(statusCombo,             1, 1);
        grid.add(new Label("Comment:"),   0, 2);
        grid.add(commentArea,             1, 2);
        getDialogPane().setContent(grid);
        getDialogPane().setPrefWidth(450);

        // Disable Save while the task name is blank
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(taskNameField.getText().trim().isEmpty());
        taskNameField.textProperty().addListener((obs, o, n) ->
                saveButton.setDisable(n.trim().isEmpty()));

        setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return CreateUpdateTaskDTO.of(
                        taskNameField.getText(),
                        commentArea.getText(),
                        statusCombo.getValue()
                );
            }
            return null;
        });
    }
}
