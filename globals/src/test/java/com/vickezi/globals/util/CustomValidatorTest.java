package com.vickezi.globals.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomValidatorTest {

    @Test
    void validateInput_validString() {
        String input = "validInput";
        assertDoesNotThrow(() -> CustomValidator.validateInput(input));
    }

    @Test
    void validateInput_invalidString() {
        String input = "<script>alert('XSS')</script>";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> CustomValidator.validateInput(input));
        assertEquals("Input can not be empty, failing", exception.getMessage());
    }

    @Test
    void validateInput_validInteger() {
        Integer input = 10;
        assertDoesNotThrow(() -> CustomValidator.validateInput(input));
    }

    @Test
    void validateInput_invalidInteger() {
        Integer input = -1;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> CustomValidator.validateInput(input));
        assertEquals("Input -1 was rejected as invalid", exception.getMessage());
    }

    @Test
    void validateInput_nullInput() {
        Object input = null;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> CustomValidator.validateInput(input));
        assertEquals("Null input was rejected as invalid", exception.getMessage());
    }

    @Test
    void validateInput_emptyString() {
        String input = "";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> CustomValidator.validateInput(input));
        assertEquals("Input cannot be empty or null", exception.getMessage());
    }
}