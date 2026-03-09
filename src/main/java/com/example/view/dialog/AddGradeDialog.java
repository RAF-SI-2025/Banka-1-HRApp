package com.example.view.dialog;

import com.example.config.AppConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Modal dialog for adding a grade to a team member.
 * Returns the selected grade value on confirmation, or {@code null} if the user cancels.
 */
public class AddGradeDialog extends Dialog<Integer> {

    /**
     * Constructs the dialog for the given member's name.
     *
     * @param memberName the member's display name shown in the header
     */
    public AddGradeDialog(String memberName) {
        setTitle("Add Grade");
        setHeaderText("Add a grade for " + memberName + ".");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Spinner<Integer> gradeSpinner = new Spinner<>(AppConfig.GRADE_MIN, AppConfig.GRADE_MAX, AppConfig.GRADE_MIN);
        gradeSpinner.setEditable(true);
        gradeSpinner.setPrefWidth(80);

        VBox content = new VBox(10,
                new Label("Grade (" + AppConfig.GRADE_MIN + " – " + AppConfig.GRADE_MAX + "):"),
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
