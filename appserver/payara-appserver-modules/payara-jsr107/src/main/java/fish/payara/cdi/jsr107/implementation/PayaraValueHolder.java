/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2017 Payara Foundation. All rights reserved.

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * Packages up an object into a Serializable value
 * @author steve
 */
public class PayaraValueHolder<T> implements Externalizable {
   
    private static final long serialVersionUID = -4600378937394648370L;
    
    private byte data[];
    
    public PayaraValueHolder() {
        
    }
    
    public PayaraValueHolder(T value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos  = new ObjectOutputStream(baos);
        oos.writeObject(value);
        data = baos.toByteArray();
        oos.close();
        baos.close();
    }
    
    @SuppressWarnings("unchecked")
    public T getValue() throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        PayaraTCCLObjectInputStream ois = new PayaraTCCLObjectInputStream(bais);
        Object result = ois.readObject();
        ois.close();
        bais.close();
        return (T)result;
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
