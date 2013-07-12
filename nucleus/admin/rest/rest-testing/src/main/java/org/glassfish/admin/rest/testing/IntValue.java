package org.glassfish.admin.rest.testing;

public class IntValue extends ScalarValue {
    private int value;

    public IntValue() {
    }
    
    public int getValue() {
        return this.value;
    }

    public IntValue value(int val) {
        this.value = val;
        regexp(null);
        return this;
    }

    @Override
    public Object getJsonValue() throws Exception {
        assertJsonable();
        return getValue();
    }

    @Override
    public void print(IndentingStringBuffer sb) {
        sb.println("intValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
