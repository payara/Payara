/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.cdi.jsr107.implementation;

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
            if (componentId == null){
                componentId = "";
            }
            if (componentId.equals(invocationComponentId)) {
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
