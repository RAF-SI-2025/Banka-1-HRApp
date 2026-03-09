package com.example.exception;

/**
 * Thrown when a team member lookup by ID returns no result.
 */
public class MemberNotFoundException extends HRAppException {

    /**
     * Constructs the exception for the given member ID.
     *
     * @param id the ID that could not be found
     */
    public MemberNotFoundException(long id) {
        super("Team member not found with id: " + id);
    }
}
