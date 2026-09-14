package com.lpgchain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a login attempt fails due to invalid credentials.
 *
 * The message is always generic ("Invalid email or password") so that
 * an attacker cannot determine whether the email exists in the database.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
