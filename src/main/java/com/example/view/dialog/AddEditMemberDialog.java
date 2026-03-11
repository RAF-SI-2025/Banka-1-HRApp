package com.example.view.dialog;

import com.example.dto.CreateUpdateMemberDTO;
import com.example.dto.TeamMemberDTO;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * Modal dialog for adding a new team member or editing an existing one.
 * Returns a validated {@link CreateUpdateMemberDTO} on confirmation,
 * or {@code null} if the user cancels.
 */
public class AddEditMemberDialog extends Dialog<CreateUpdateMemberDTO> {

    /**
     * Constructs the dialog.
     *
     * @param existing the member to pre-populate fields with, or {@code null} for a new member
     */
    public AddEditMemberDialog(TeamMemberDTO existing) {
        setTitle(existing == null ? "Add Team Member" : "Edit Team Member");
        setHeaderText(existing == null ? "Enter details for the new team member."
                                       : "Update details for " + existing.getName() + " " + existing.getSurname() + ".");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField nameField    = new TextField();
        TextField surnameField = new TextField();
        nameField.setPromptText("First name");
        surnameField.setPromptText("Last name");

        if (existing != null) {
            nameField.setText(existing.getName());
            surnameField.setText(existing.getSurname());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Name:"),    0, 0);
        grid.add(nameField,             1, 0);
        grid.add(new Label("Surname:"), 0, 1);
        grid.add(surnameField,          1, 1);
        getDialogPane().setContent(grid);

        // Disable Save while either field is blank
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        Runnable validateFields = () -> saveButton.setDisable(
                nameField.getText().trim().isEmpty() || surnameField.getText().trim().isEmpty()
        );
        nameField.textProperty().addListener((obs, o, n) -> validateFields.run());
        surnameField.textProperty().addListener((obs, o, n) -> validateFields.run());

        setResultConverter(btn -> {
            if (btn == saveButtonType) {
                return CreateUpdateMemberDTO.of(nameField.getText(), surnameField.getText());
            }
            return null;
        });
    }
}
