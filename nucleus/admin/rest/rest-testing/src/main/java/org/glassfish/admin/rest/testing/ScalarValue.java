package org.glassfish.admin.rest.testing;

public abstract class ScalarValue extends Value {
    private String regexp = "";

    String getRegexp() {
        return this.regexp;
    }

    public ScalarValue regexp(String val) {
        this.regexp = val;
        return this;
    }

    protected void assertJsonable() {
        if (getRegexp() != null) {
            throw new IllegalStateException("Regular expressions cannot be converted to json");
        }
    }
}
