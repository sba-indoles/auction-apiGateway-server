package org.indoles.apigatewayserver.exception;

public record ErrorResponse(
        String message,
        String errorCode
) {

    public static ErrorResponse of(final String message, final String errorCode) {
        return new ErrorResponse(message, errorCode);
    }
}
