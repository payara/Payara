package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String exceptionType;
    public final String message;
    public final ErrorResponse cause;

    private ErrorResponse(String exceptionType, String message, ErrorResponse cause) {
        this.exceptionType = exceptionType;
        this.message = message;
        this.cause = cause;
    }

    public ErrorResponse(Throwable ex) {
        this(ex.getClass().getName(), ex.getMessage(), ex.getCause() == null ? null : new ErrorResponse(ex.getCause()));
    }

}
