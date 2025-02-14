package com.vickezi.globals.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;


import static com.vickezi.globals.util.Constants.*;
public class CustomValidator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    public void validateInput(Object input){
        Assert.notNull(input, "Null input null was rejected as invalid");
        if(input instanceof Integer){
            if((Integer)input<0){
                logger.error("Detected invalid input: [{}] ",input);
                throw new RuntimeException(String.format("Input %s was rejected as invalid",input));
            }
            return;
        }
        Assert.hasText((String) input, "Input  was rejected as invalid");
        final String sanitizedInput = sanitize((String) input);
        Assert.hasText( sanitizedInput, "Input was sanitized to empty, add a valid input and try again");
        if(!validate(sanitizedInput)){
            logger.error("Detected invalid input: [{}] ",input);
            throw new RuntimeException(String.format("Input %s was rejected as invalid",input));
        }
    }
    /**
     * Validates the input object to check for SQL injection, XSS, and HTML tags.
     *
     * @param input the input object to validate, can be an Integer or String
     * @return true if the input is valid and does not contain malicious content, false otherwise
     */
    private boolean validate(Object input) {
        return switch (input) {
            case Integer i -> (int) input > 0;
            case String s -> !SQL_INJECTION_PATTERN.matcher((CharSequence) input).find() &&
                    !XSS_PATTERN.matcher((CharSequence) input).find() &&
                    !HTML_TAG_PATTERN.matcher((CharSequence) input).find();
            case null, default -> false;
        };
    }
    /**
     * Sanitizes the input string by removing patterns that match SQL injection, XSS, and HTML tags.
     *
     * @param input the input string to sanitize
     * @return the sanitized string with malicious patterns removed, or null if the input is null
     */
    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll(SQL_INJECTION_PATTERN.pattern(), "")
                .replaceAll(XSS_PATTERN.pattern(), "")
                .replaceAll(HTML_TAG_PATTERN.pattern(), "");
    }
}
