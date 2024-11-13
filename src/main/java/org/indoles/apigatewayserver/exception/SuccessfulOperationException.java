package org.indoles.apigatewayserver.exception;

public class SuccessfulOperationException extends CustomException {

    public SuccessfulOperationException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
