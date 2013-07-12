package org.glassfish.admin.rest.testing;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;

import static org.glassfish.admin.rest.testing.Common.*;

public class ArrayValue extends Value {
    private boolean ignoreExtra;
    private boolean ordered;
    private List<Value> values = new ArrayList<Value>();

    ArrayValue() {
    }
    
    boolean isIgnoreExtra() {
        return this.ignoreExtra;
    }

    public ArrayValue ignoreExtra(boolean val) {
        this.ignoreExtra = val;
        return this;
    }

    public ArrayValue ignoreExtra() {
        return ignoreExtra(true);
    }

    boolean isOrdered() {
        return this.ordered;
    }

    public ArrayValue ordered(boolean val) {
        this.ordered = val;
        return this;
    }

    public ArrayValue ordered() {
        return ordered(true);
    }
    
    List<Value> getValues() {
        return this.values;
    }

    public ArrayValue add(Value value) {
        if (value == null) {
            value = nilVal();
        }
        getValues().add(value);
        return this;
    }

    public ArrayValue add(String propertyValue) {
        Value val = (propertyValue != null) ? stringVal(propertyValue) : null;
        return add(val);
    }

    public ArrayValue add(long propertyValue) {
        return add(longVal(propertyValue));
    }

    public ArrayValue add(int propertyValue) {
        return add(intVal(propertyValue));
    }

    public ArrayValue add(double propertyValue) {
        return add(doubleVal(propertyValue));
    }

    public ArrayValue add(boolean propertyValue) {
        return add(booleanVal(propertyValue));
    }

    public ArrayValue add() {
        return add(nilVal());
    }

    public ArrayValue remove(int index) {
        if (0 <= index && index < getValues().size()) {
            getValues().remove(index);
        }
        // TBD - should we complain if the index is out of range?
        return this;
    }

    public Value get(int index) {
        return (index < getValues().size()) ? getValues().get(index) : null;
    }

    public ObjectValue getObjectVal(int index) {
        return (ObjectValue) get(index);
    }

    public StringValue getStringVal(int index) {
        return (StringValue) get(index);
    }

    public LongValue getLongVal(int index) {
        return (LongValue) get(index);
    }

    public IntValue getIntVal(int index) {
        return (IntValue) get(index);
    }

    public DoubleValue getDoubleVal(int index) {
        return (DoubleValue) get(index);
    }

    public BooleanValue getBooleanVal(int index) {
        return (BooleanValue) get(index);
    }

    public NilValue getNilVal(int index) {
        return (NilValue) get(index);
    }
    // TBD - could support swapping out a value, but we're not likely to need that since arrays are usually homogeneous

    @Override
    Object getJsonValue() throws Exception {
        if (isIgnoreExtra()) {
            throw new IllegalStateException("Cannot be converted to json because ignoreExtra is true");
        }
        JSONArray a = new JSONArray();
        for (Value v : getValues()) {
            a.put(v.getJsonValue());
        }
        return a;
    }

    public JSONArray toJSONArray() throws Exception {
        return (JSONArray) getJsonValue();
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("arrayValue ignoreExtra=" + isIgnoreExtra() + " ordered=" + isOrdered());
        sb.indent();
        try {
            for (Value value : getValues()) {
                value.print(sb);
            }
        } finally {
            sb.undent();
        }
    }
}
