package org.glassfish.admin.rest.testing;

public class DoubleValue extends ScalarValue {

    private double value;

    public DoubleValue() {
    }

    public double getValue() {
        return this.value;
    }

    public DoubleValue value(double val) {
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
        sb.println("doubleValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
