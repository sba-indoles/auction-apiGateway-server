package org.indoles.apigatewayserver.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class BadRequestException extends BusinessException {

    public BadRequestException(final String message, final ErrorCode errorCode) {
        super(message, BAD_REQUEST.value(), errorCode);
    }
}
