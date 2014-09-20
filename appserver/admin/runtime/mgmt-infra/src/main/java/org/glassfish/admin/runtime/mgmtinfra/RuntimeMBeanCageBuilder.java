/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.runtime.mgmtinfra;

import org.jvnet.hk2.component.CageBuilder;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.annotations.Service;
import javax.management.*;
import javax.management.modelmbean.*;
import java.lang.management.*;
import java.io.*;

/**
 * Entry class for creating and registering runtime mBeans based on POJOs 
 *
 * @author Sreenivas Munnangi
 */
@Service
public class RuntimeMBeanCageBuilder implements CageBuilder {

    private static final boolean dbg = false;

    public void onEntered(Inhabitant<?> i) {
        if (dbg) {
            System.out.println("RuntimeMBeanCageBuilder: onEntered(" + i.typeName() + 
                "): time to create and register the mBean ...");
        }
        Object o = i.get();
        createMBean(o);
    }

    private void createMBean(Object o) {
        if (dbg) {
            System.out.println("RuntimeMBeanCageBuilder: createMBean: " + 
                "creating mbean for Object ..." + o);
        }
        try {
            String className = o.getClass().getName();
            String serName = className.substring(className.lastIndexOf(".") + 1);
            if (dbg) {
                System.out.println("RuntimeMBeanCageBuilder: createMBean: " + 
                    " className = " + className +
                    " serName = " + serName);
            }
            InputStream fis = null;
            ObjectInputStream inStream = null;
            try {
                fis = o.getClass().getResourceAsStream(serName+".ser");
                inStream = new ObjectInputStream( fis );
            } catch (Exception ex) {
                if (dbg) {
                    System.out.println("RuntimeMBeanCageBuilder: createMBean: " + 
                        "companion class " + o + " is not an mBean ...");
                }
                ex.printStackTrace();
                return;
            }
            ObjectName mbon = new ObjectName(((Runtime)o).getObjectName());
            ModelMBeanInfo mmbinfo = ( ModelMBeanInfo )inStream.readObject();
            RequiredModelMBean modelmbean = new RequiredModelMBean(mmbinfo);
            modelmbean.setManagedResource(o, "objectReference");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(modelmbean,mbon);
        } catch (Exception e) {
            if (dbg) {
                System.out.println("RuntimeMBeanCageBuilder: createMBean: " + 
                    "exception while creating mBean ...");
            }
            e.printStackTrace();
        }
    }

}
