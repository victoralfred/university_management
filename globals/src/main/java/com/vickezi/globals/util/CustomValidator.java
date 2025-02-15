package com.vickezi.globals.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import static com.vickezi.globals.util.Constants.*;

public class CustomValidator {
    private static final Logger logger = LoggerFactory.getLogger(CustomValidator.class);

    /**
     * Validate the input for different types (Integer and String) and handle any validation errors.
     * @param input the input object to validate
     * @return the validated and sanitized input
     */
    public static <T>Object validateInput(T input) {
        Assert.notNull(input, "Null input was rejected as invalid");

        if (input instanceof Integer) {
            return validateInteger((Integer) input);
        }

        if (input instanceof String) {
            // If I get value not matching email format
            return validateString((String) input);
        }

        throw new InvalidInputException("Cannot determine type of input: " + input);
    }

    /**
     * Validates an Integer input.
     * @param input the Integer to validate
     * @return the valid Integer
     */
    private static Integer validateInteger(Integer input) {
        if (input < 0) {
            logError(input.toString());
            throw new InvalidInputException(String.format("Input %s was rejected as invalid", input));
        }
        return input;
    }
    /**
     * Validates and sanitizes a String input.
     * @param input the String to validate and sanitize
     * @return the sanitized String
     */
    public static String validateStringLiterals(String input) {
        Assert.hasText(input, "Input cannot be empty or null");
        String sanitizedInput = sanitize(input);

        if (!validate(sanitizedInput)) {
            logError(sanitizedInput);
            throw new InvalidInputException(String.format("Input %s was rejected as invalid", sanitizedInput));
        }
        return sanitizedInput;
    }
    /**
     * Validates and sanitizes a String input.
     * @param input the String to validate and sanitize
     * @return the sanitized String
     */
    private static String validateString(String input) {
        String sanitizedInput = validateStringLiterals(input);
        // Additional validation for email format if the input is an email
        if (!isValidEmailFormat(sanitizedInput)) {
            logError(sanitizedInput);
            throw new RuntimeException("Invalid email format: " + sanitizedInput);
        }
        return sanitizedInput;
    }

    /**
     * Validates input to check for SQL injection, XSS, and HTML tags.
     * @param input the input object to validate
     * @return true if the input is valid and does not contain malicious content
     */
    private static boolean validate(Object input) {
        if (input instanceof Integer) {
            return (Integer) input > 0;
        }
        if (input instanceof String str) {
            Assert.hasText(str, "Input can not be empty, failing");
            return !containsSQLInjection(str) && !containsXSS(str) && !containsHTMLTags(str);
        }

        return false;
    }

    /**
     * Checks for SQL injection patterns in a String.
     * @param input the String to check
     * @return true if the input contains SQL injection patterns
     */
    private static boolean containsSQLInjection(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Checks for XSS patterns in a String.
     * @param input the String to check
     * @return true if the input contains XSS patterns
     */
    private static boolean containsXSS(String input) {
        return XSS_PATTERN.matcher(input).find();
    }

    /**
     * Checks for HTML tags in a String.
     * @param input the String to check
     * @return true if the input contains HTML tags
     */
    private static boolean containsHTMLTags(String input) {
        return HTML_TAG_PATTERN.matcher(input).find();
    }

    /**
     * Sanitizes a String by removing patterns that match SQL injection, XSS, and HTML tags.
     * @param input the String to sanitize
     * @return the sanitized String
     */
    private static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll(SQL_INJECTION_PATTERN.pattern(), "")
                .replaceAll(XSS_PATTERN.pattern(), "")
                .replaceAll(HTML_TAG_PATTERN.pattern(), "");
    }

    /**
     * Logs invalid input to the logger.
     * @param message the message to log
     */
    private static void logError(String message) {
        logger.error("Detected invalid input: [{}]", message);
    }

    /**
     * Custom exception for invalid inputs.
     */
    public static class InvalidInputException extends RuntimeException {
        public InvalidInputException(String message) {
            super(message);
        }
    }
    /**
     * Checks if the input string is a valid email format.
     *
     * @param email the input email to check
     * @return true if the email format is valid, false otherwise
     */
    public static boolean isValidEmailFormat(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
