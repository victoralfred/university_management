package com.vickezi.globals.util;

import java.util.regex.Pattern;

public class Constants {
    public static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(SELECT|UPDATE|DELETE|INSERT|DROP|ALTER|CREATE|EXEC|UNION|--|#|\\bOR\\b|\\bAND\\b|;)", Pattern.CASE_INSENSITIVE);    // Pattern to detect XSS attempts
    public static final Pattern XSS_PATTERN = Pattern.compile(
            "<script>(.*?)</script>|<\\s*?script|javascript:|alert\\(", Pattern.CASE_INSENSITIVE);
    // Pattern to detect HTML tags
    public static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>", Pattern.CASE_INSENSITIVE);
}
