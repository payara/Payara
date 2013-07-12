package org.glassfish.admin.rest.testing;

public class StringValue extends ScalarValue {
    private String value = "";

    StringValue() {
    }

    String getValue() {
        return this.value;
    }

    public StringValue value(String val) {
        this.value = val;
        regexp(null);
        return this;
    }

    @Override
    Object getJsonValue() throws Exception {
        assertJsonable();
        return getValue();
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("stringValue value=" + getValue() + " regexp=" + getRegexp());
    }
}
