package org.glassfish.admin.rest.testing;

public class BooleanValue extends ScalarValue {
    private boolean value;

    BooleanValue() {
    }

    boolean getValue() {
        return this.value;
    }

    public BooleanValue value(boolean val) {
        this.value = val;
        regexp(null);
        return this;
    }

    @Override
    Object getJsonValue() throws Exception {
        assertJsonable();
        return (getValue()) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("booleanValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
