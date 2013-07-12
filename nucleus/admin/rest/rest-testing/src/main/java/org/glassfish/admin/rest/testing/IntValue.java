package org.glassfish.admin.rest.testing;

public class IntValue extends ScalarValue {
    private int value;

    IntValue() {
    }
    
    int getValue() {
        return this.value;
    }

    public IntValue value(int val) {
        this.value = val;
        regexp(null);
        return this;
    }

    @Override
    Object getJsonValue() throws Exception {
        assertJsonable();
        return new Integer(getValue());
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("intValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
