package org.glassfish.admin.rest.testing;

import org.codehaus.jettison.json.JSONObject;

public class NilValue extends Value {

    NilValue() {
    }

    @Override
    Object getJsonValue() throws Exception {
        return JSONObject.NULL;
    }

    @Override
    void print(IndentingStringBuffer sb) {
        sb.println("nilValue");
    }
}
