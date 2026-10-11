package com.healthcheckassistant.common.error;

public class InvalidPatientDataException extends RuntimeException {
    public InvalidPatientDataException(String message) { super(message); }
}
