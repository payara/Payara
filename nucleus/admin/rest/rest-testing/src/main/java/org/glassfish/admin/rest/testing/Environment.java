package org.glassfish.admin.rest.testing;

public interface Environment {
    String getProtocol();
    String getHost();
    String getPort();
    String getUserName();
    String getPassword();
    void debug(String message);
    void debug(String message, Throwable t);
}
