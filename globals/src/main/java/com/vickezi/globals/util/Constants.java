package com.vickezi.globals.util;

import java.util.regex.Pattern;
/**
 * Utility class containing constants used throughout the application.
 */
public class Constants {
    /**
     * Pattern to detect SQL injection attempts.
     */
    public static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(SELECT|UPDATE|DELETE|INSERT|DROP|ALTER|CREATE|EXEC|UNION|--|#|\\bOR\\b|\\bAND\\b|;)", Pattern.CASE_INSENSITIVE);
    /**
     * Pattern to detect Cross-Site Scripting (XSS) attempts.
     */
    public static final Pattern XSS_PATTERN = Pattern.compile(
            "<script>(.*?)</script>|<\\s*?script|javascript:|alert\\(", Pattern.CASE_INSENSITIVE);
    /**
     * Pattern to detect HTML tags.
     */
    // Pattern to detect HTML tags
    public static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
    /**
     * Topic name for user registered events.
     */
    public static final String USER_EMAIL_REGISTERED_EVENT_TOPIC = "user_email_registered_event";
    /**
     * Topic name for user registration confirmation notices.
     */
    public static final String USER_REGISTRATION_CONFIRMATION_NOTICE_TOPIC = "user_registration_confirmation";
    /**
     *  Basic email pattern, can be more strict based on requirements
     */
    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    public static String OPERATION_SUCCESSFUL ="Operation successful";
    public static String OPERATION_FAILED ="Operation failed";


}
