package org.glassfish.admin.rest.testing;

public class LongValue extends ScalarValue {
    private long value;

    LongValue() {
    }

    long getValue() {
        return this.value;
    }

    public LongValue value(long val) {
        this.value = val;
        regexp(null);
        return this;
    }

    @Override
    Object getJsonValue() throws Exception {
        assertJsonable();
        return new Long(getValue());
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("longValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
