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
    
    public boolean isIgnoreExtra() {
        return this.ignoreExtra;
    }

    public ArrayValue ignoreExtra(boolean val) {
        this.ignoreExtra = val;
        return this;
    }

    public ArrayValue ignoreExtra() {
        return ignoreExtra(true);
    }

    public boolean isOrdered() {
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

    public int size() {
        return getValues().size();
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
