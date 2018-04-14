/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2018 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.cdi.jsr107.implementation;

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.JavaEEContextUtil;

/**
 * Packages up an object into a Serializable value
 * @author steve
 * @param <T> value type
 */
public class PayaraValueHolder<T> implements Externalizable {
   
    private static final long serialVersionUID = -4600378937394648370L;
    
    private byte data[];
    
    public PayaraValueHolder() {
        
    }
    
    public PayaraValueHolder(T value) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(Globals.getDefaultHabitat().getService(JavaEEContextUtil.class).getInstanceComponentId());
            oos.writeObject(value);
            data = baos.toByteArray();
        }
    }
    
    @SuppressWarnings("unchecked")
    public T getValue() throws IOException, ClassNotFoundException {
        String componentId = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data); PayaraTCCLObjectInputStream ois = new PayaraTCCLObjectInputStream(bais)) {
            componentId = (String)ois.readObject();
            Object result = ois.readObject();
            return (T)result;
        }
        catch (ClassNotFoundException ex) {
            String invocationComponentId = Globals.getDefaultHabitat().getService(JavaEEContextUtil.class).getInstanceComponentId();
            if (!Strings.nullToEmpty(componentId).equals(invocationComponentId)) {
                throw new ClassNotFoundException(String.format("Wrong application: expected %s bug got %s", componentId, invocationComponentId),
                        new IllegalStateException("Wrong Application"));
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(data.length);
        out.write(data,0,data.length);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int length = in.readInt();
        data = new byte[length];
        in.readFully(data, 0, length);
    }
}
