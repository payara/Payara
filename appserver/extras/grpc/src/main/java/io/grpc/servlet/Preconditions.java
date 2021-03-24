package io.grpc.servlet;

public final class Preconditions {

    private Preconditions() {
    }

    protected static <T> T checkNotNull(T object, String errorMessage) {
        if (object == null) {
            if (errorMessage != null) {
                throw new NullPointerException(errorMessage);
            }
            throw new NullPointerException();
        }
        return object;
    }

    protected static void checkArgument(Object object, String errorMessage) {
        if (object == null) {
            if (errorMessage != null) {
                throw new IllegalArgumentException(errorMessage);
            }
            throw new IllegalArgumentException();
        }
    }

    protected static void checkState(Object object, String errorMessage) {
        if (object == null) {
            if (errorMessage != null) {
                throw new IllegalStateException(errorMessage);
            }
            throw new IllegalStateException();
        }
    }

}
