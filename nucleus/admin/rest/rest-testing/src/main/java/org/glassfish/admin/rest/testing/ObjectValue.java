package org.glassfish.admin.rest.testing;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

import static org.glassfish.admin.rest.testing.Common.*;

public class ObjectValue extends Value {
    private boolean ignoreExtra;

    ObjectValue() {
    }

    boolean isIgnoreExtra() {
        return this.ignoreExtra;
    }

    public ObjectValue ignoreExtra(boolean val) {
        this.ignoreExtra = val;
        return this;
    }

    public ObjectValue ignoreExtra() {
        return ignoreExtra(true);
    }
    private Map<String, Value> properties = new HashMap<String, Value>();

    Map<String, Value> getProperties() {
        return this.properties;
    }

    public ObjectValue put(String propertyName, Value propertyValue) {
        if (propertyValue == null) {
            propertyValue = nilVal();
        }
        getProperties().put(propertyName, propertyValue);
        return this;
    }

    public ObjectValue put(String propertyName, String propertyValue) {
        Value val = (propertyValue != null) ? stringVal(propertyValue) : null;
        return put(propertyName, val);
    }

    public ObjectValue put(String propertyName, long propertyValue) {
        return put(propertyName, longVal(propertyValue));
    }

    public ObjectValue put(String propertyName, int propertyValue) {
        return put(propertyName, intVal(propertyValue));
    }

    public ObjectValue put(String propertyName, double propertyValue) {
        return put(propertyName, doubleVal(propertyValue));
    }

    public ObjectValue put(String propertyName, boolean propertyValue) {
        return put(propertyName, booleanVal(propertyValue));
    }

    public ObjectValue put(String propertyName) {
        return put(propertyName, nilVal());
    }

    public ObjectValue remove(String propertyName) {
        if (has(propertyName)) {
            getProperties().remove(propertyName);
        }
        // TBD - should we complain if the property doesn't exist ?
        return this;
    }

    public boolean has(String propertyName) {
        return getProperties().containsKey(propertyName);
    }

    public Value get(String propertyName) {
        return getProperties().get(propertyName);
    }

    public ObjectValue getObjectVal(String propertyName) {
        return (ObjectValue) get(propertyName);
    }

    public ArrayValue getArrayVal(String propertyName) {
        return (ArrayValue) get(propertyName);
    }

    public StringValue getStringVal(String propertyName) {
        return (StringValue) get(propertyName);
    }

    public LongValue getLongVal(String propertyName) {
        return (LongValue) get(propertyName);
    }

    public IntValue getIntVal(String propertyName) {
        return (IntValue) get(propertyName);
    }

    public DoubleValue getDoubleVal(String propertyName) {
        return (DoubleValue) get(propertyName);
    }

    public BooleanValue getBooleanVal(String propertyName) {
        return (BooleanValue) get(propertyName);
    }

    public NilValue getNilVal(String propertyName) {
        return (NilValue) get(propertyName);
    }

    @Override
    Object getJsonValue() throws Exception {
        if (isIgnoreExtra()) {
            throw new IllegalStateException("Cannot be converted to json because ignoreExtra is true");
        }
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Value> p : getProperties().entrySet()) {
            o.put(p.getKey(), p.getValue().getJsonValue());
        }
        return o;
    }

    public JSONObject toJSONObject() throws Exception {
        return (JSONObject) getJsonValue();
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("objectValue ignoreExtra=" + isIgnoreExtra());
        sb.indent();
        try {
            for (Map.Entry<String, Value> p : getProperties().entrySet()) {
                sb.println("property name=" + p.getKey());
                sb.indent();
                try {
                    p.getValue().print(sb);
                } finally {
                    sb.undent();
                }
            }
        } finally {
            sb.undent();
        }
    }
}
