package fish.payara.cdi.auth.roles;

/**
 * This CallerAccessException is thrown to report that caller access to a
 * bean method was denied.
 */
public class CallerAccessException extends RuntimeException {

    private static final long serialVersionUID = 4923220329322198628L;


    public CallerAccessException() {
    }

    public CallerAccessException(String message) {
        super(message);
    }

    public CallerAccessException(String message, Exception e) {
        super(message, e);
    }

}