package com.vickezi.globals.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomValidatorTest {
    private final CustomValidator customValidator = new CustomValidator();

    @Test
    void validateInput_validString() {
        String input = "validInput";
        assertDoesNotThrow(() -> customValidator.validateInput(input));
    }

    @Test
    void validateInput_invalidString() {
        String input = "<script>alert('XSS')</script>";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> customValidator.validateInput(input));
        assertEquals("Input was sanitized to empty, add a valid input and try again", exception.getMessage());
    }

    @Test
    void validateInput_validInteger() {
        Integer input = 10;
        assertDoesNotThrow(() -> customValidator.validateInput(input));
    }

    @Test
    void validateInput_invalidInteger() {
        Integer input = -1;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> customValidator.validateInput(input));
        assertEquals("Input -1 was rejected as invalid", exception.getMessage());
    }

    @Test
    void validateInput_nullInput() {
        Object input = null;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> customValidator.validateInput(input));
        assertEquals("Null input null was rejected as invalid", exception.getMessage());
    }

    @Test
    void validateInput_emptyString() {
        String input = "";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> customValidator.validateInput(input));
        assertEquals("Input  was rejected as invalid", exception.getMessage());
    }
}