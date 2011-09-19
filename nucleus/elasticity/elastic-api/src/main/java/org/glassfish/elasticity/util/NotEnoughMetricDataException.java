package org.glassfish.elasticity.util;

/**
 * @author Mahesh Kannan
 */
public class NotEnoughMetricDataException
    extends RuntimeException {

    public NotEnoughMetricDataException() {
    }

    public NotEnoughMetricDataException(String s) {
        super(s);
    }

    public NotEnoughMetricDataException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public NotEnoughMetricDataException(Throwable throwable) {
        super(throwable);
    }
}
