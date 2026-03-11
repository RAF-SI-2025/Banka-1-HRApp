package com.example.view.dialog;

import com.example.config.AppConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Modal dialog for adding a grade to a task.
 * Returns the selected grade value on confirmation, or {@code null} if the user cancels.
 */
public class AddGradeDialog extends Dialog<Integer> {

    /**
     * Constructs the dialog for the given task name.
     *
     * @param taskName the task's name shown in the header
     */
    public AddGradeDialog(String taskName) {
        setTitle("Add Grade");
        setHeaderText("Add a grade for task: " + taskName + ".");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Spinner<Integer> gradeSpinner = new Spinner<>(AppConfig.getGradeMin(), AppConfig.getGradeMax(), AppConfig.getGradeMin());
        gradeSpinner.setEditable(true);
        gradeSpinner.setPrefWidth(80);

        gradeSpinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9]*")) {
                gradeSpinner.getEditor().setText(oldVal);
            } else if (newVal.isEmpty()) {
                gradeSpinner.getEditor().setText(String.valueOf(AppConfig.getGradeMin()));
                gradeSpinner.getValueFactory().setValue(AppConfig.getGradeMin());
            }
        });

        VBox content = new VBox(10,
                new Label("Grade (" + AppConfig.getGradeMin() + " – " + AppConfig.getGradeMax() + "):"),
                gradeSpinner);
        content.setPadding(new Insets(20));
        getDialogPane().setContent(content);

        setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return gradeSpinner.getValue();
            }
            return null;
        });
    }
}
