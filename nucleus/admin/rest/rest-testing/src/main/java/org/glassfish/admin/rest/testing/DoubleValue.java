package org.glassfish.admin.rest.testing;

public class DoubleValue extends ScalarValue {

    private double value;

    DoubleValue() {
    }

    double getValue() {
        return this.value;
    }

    public DoubleValue value(double val) {
        this.value = val;
        regexp(null);
        return this;
    }

    @Override
    Object getJsonValue() throws Exception {
        assertJsonable();
        return new Double(getValue());
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("doubleValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
