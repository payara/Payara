/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.admin.rest.testing;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

import static org.glassfish.admin.rest.testing.Common.*;

public class ObjectValue extends Value {
    private boolean ignoreExtra;

    ObjectValue() {
    }

    public boolean isIgnoreExtra() {
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

    public Set<String> getPropertyNames() {
        return getProperties().keySet();
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
