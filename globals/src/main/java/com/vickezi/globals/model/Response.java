package com.vickezi.globals.model;


/**
 * Represents an immutable response object containing a message, a success flag, and a status code.
 * This record is designed to simplify the creation of response objects for APIs, services, or other
 * operations where a standardized response format is required.
 *
 * @param message    A descriptive message about the response (e.g., "Operation successful").
 * @param success    A boolean flag indicating whether the operation was successful (true) or not (false).
 * @param statusCode An integer representing the status code of the response (e.g., 200 for success, 202 for accepted,
 *                  400 for client errors).
 */
public record Response (String message, boolean success, int statusCode){}
