package com.example.dto;

import com.example.config.AppConfig;
import com.example.exception.ValidationException;

/**
 * Input DTO for creating or updating a team member.
 * Validation is enforced in the factory method {@link #of(String, String)}.
 */
public class CreateUpdateMemberDTO {

    private final String name;
    private final String surname;

    private CreateUpdateMemberDTO(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    /**
     * Creates a validated {@link CreateUpdateMemberDTO}.
     * Trims whitespace before validation.
     *
     * @param name    the member's first name
     * @param surname the member's last name
     * @return a validated DTO
     * @throws ValidationException if name or surname is blank or exceeds the maximum length
     */
    public static CreateUpdateMemberDTO of(String name, String surname) {
        String trimmedName    = name    != null ? name.trim()    : "";
        String trimmedSurname = surname != null ? surname.trim() : "";

        if (trimmedName.isEmpty()) {
            throw new ValidationException("Name cannot be blank.");
        }
        if (trimmedSurname.isEmpty()) {
            throw new ValidationException("Surname cannot be blank.");
        }
        if (trimmedName.length() > AppConfig.getMaxNameLength()) {
            throw new ValidationException("Name exceeds maximum length of " + AppConfig.getMaxNameLength() + " characters.");
        }
        if (trimmedSurname.length() > AppConfig.getMaxNameLength()) {
            throw new ValidationException("Surname exceeds maximum length of " + AppConfig.getMaxNameLength() + " characters.");
        }

        return new CreateUpdateMemberDTO(trimmedName, trimmedSurname);
    }

    /** @return the validated first name */
    public String getName() { return name; }

    /** @return the validated last name */
    public String getSurname() { return surname; }
}
