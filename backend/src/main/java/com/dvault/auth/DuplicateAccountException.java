package com.dvault.auth;

public class DuplicateAccountException extends RuntimeException {
    public DuplicateAccountException() { super("This email is already registered"); }
}
