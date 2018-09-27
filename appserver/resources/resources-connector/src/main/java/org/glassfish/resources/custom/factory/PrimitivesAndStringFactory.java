/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package org.glassfish.resources.custom.factory;


import javax.naming.spi.ObjectFactory;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.Reference;
import javax.naming.RefAddr;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Locale;

public class PrimitivesAndStringFactory implements Serializable, ObjectFactory {

    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference)obj;

        Enumeration<RefAddr> refAddrs = ref.getAll();
        String type = null;
        String value = null;
        boolean hasAnyProperties = false;
        while(refAddrs.hasMoreElements()){
            hasAnyProperties = true;
            RefAddr addr = refAddrs.nextElement();
            String propName = addr.getType();

            type = ref.getClassName();

            if(propName.equalsIgnoreCase("value")){
                value = (String)addr.getContent();
            }
        }
        // if there are no properties at all, back-up obtain the type of the custom resource
        if (!hasAnyProperties && type == null) {
            type = ref.getClassName();
        }

        if(type != null && value != null){
            type = type.toUpperCase(Locale.getDefault());
            if(type.endsWith("INT") || type.endsWith("INTEGER")){
                return Integer.valueOf(value);
            } else if (type.endsWith("LONG")){
                return Long.valueOf(value);
            } else if(type.endsWith("DOUBLE")){
                return Double.valueOf(value);
            } else if(type.endsWith("FLOAT") ){
                return Float.valueOf(value);
            } else if(type.endsWith("CHAR") || type.endsWith("CHARACTER")){
                return value.charAt(0);
            } else if(type.endsWith("SHORT")){
                return Short.valueOf(value);
            } else if(type.endsWith("BYTE")){
                return Byte.valueOf(value);
            } else if(type.endsWith("BOOLEAN")){
                return Boolean.valueOf(value);
            } else if(type.endsWith("STRING")){
                return value;
            }else{
                throw new IllegalArgumentException("unknown type ["+type+"] ");
            }
        }else if (type == null){
            throw new IllegalArgumentException("type cannot be null");
        }else{
            throw new IllegalAccessException("value cannot be null");
        }
    }
}
