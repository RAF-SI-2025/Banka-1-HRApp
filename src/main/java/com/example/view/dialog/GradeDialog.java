package com.example.view.dialog;

import com.example.config.AppConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Modal dialog that asks the user for a grade when marking a task as COMPLETED.
 * Pre-populates the spinner with the existing grade when editing an already-completed task.
 * Returns the selected grade value on confirmation, or an empty Optional if cancelled.
 */
public class GradeDialog extends Dialog<Integer> {

    /**
     * @param memberName  display name of the member (shown in header)
     * @param taskName    name of the task being graded (shown in header)
     * @param currentGrade existing grade to pre-fill, or {@code null} for a new grade
     */
    public GradeDialog(String memberName, String taskName, Integer currentGrade) {
        setTitle("Assign Grade");
        setHeaderText("Assign a grade for \"" + taskName + "\" — " + memberName);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        int initial = currentGrade != null ? currentGrade : AppConfig.getGradeMin();
        Spinner<Integer> gradeSpinner = new Spinner<>(
                AppConfig.getGradeMin(), AppConfig.getGradeMax(), initial);
        gradeSpinner.setEditable(true);
        gradeSpinner.setPrefWidth(80);

        VBox content = new VBox(10,
                new Label("Grade (" + AppConfig.getGradeMin() + " – " + AppConfig.getGradeMax() + "):"),
                gradeSpinner);
        content.setPadding(new Insets(20));
        getDialogPane().setContent(content);

        setResultConverter(btn -> btn == saveButtonType ? gradeSpinner.getValue() : null);
    }
}

