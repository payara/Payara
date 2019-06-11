package fish.payara.ejb.http.protocol;

import java.io.Serializable;

public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String exceptionType;
    public final String message;
    public final ErrorResponse cause;

    public ErrorResponse() {
        this(null, null, null); // for JSON
    }

    private ErrorResponse(String exceptionType, String message, ErrorResponse cause) {
        this.exceptionType = exceptionType;
        this.message = message;
        this.cause = cause;
    }

    public ErrorResponse(Throwable ex) {
        this(ex.getClass().getName(), ex.getMessage(), ex.getCause() == null ? null : new ErrorResponse(ex.getCause()));
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        toString(str);
        return str.toString();
    }

    private void toString(StringBuilder str) {
        str.append(exceptionType).append(": ").append(message);
        if (cause != null) {
            str.append("\ncaused by:\n");
            cause.toString(str);
        }
    }
}
