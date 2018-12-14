/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * MethodPermissionDescriptor.java
 *
 * Created on December 6, 2001, 2:32 PM
 */

package com.sun.enterprise.deployment;

import java.util.*;

/**
 * This class defines a method permission information in the assembly
 * descriptor
 *
 * @author  Jerome Dochez
 * @version
 */
public class MethodPermissionDescriptor extends DescribableDescriptor {

    List<MethodDescriptor> methods = new ArrayList<>();
    List<MethodPermission> mps = new ArrayList<>() ;

    /** Creates new MethodPermissionDescriptor */
    public MethodPermissionDescriptor() {
    }

    public void addMethod(MethodDescriptor aMethod) {
        methods.add(aMethod);
    }

    public void addMethods(Collection<MethodDescriptor> methods) {
        this.methods.addAll(methods);
    }

    public void addMethodPermission(MethodPermission mp) {
        mps.add(mp);
    }

    public MethodDescriptor[] getMethods() {
        return methods.toArray(new MethodDescriptor[0]);
    }

    public MethodPermission[] getMethodPermissions() {
        return mps.toArray(new MethodPermission[0]);
    }

    public void print(StringBuilder buffer) {
        buffer.append("Method Permission ").append(getDescription() == null ? "" : getDescription());
        buffer.append("\nFor the following Permissions ");
        for (Object mp1 : mps) {
            MethodPermission mp = (MethodPermission) mp1;
            mp.print(buffer);
            buffer.append("\n");
        }
        buffer.append("\nFor the following ").append(methods.size()).append(" methods\n");
        for (Iterator methodsIterator = methods.iterator();methodsIterator.hasNext();) {
            MethodDescriptor md = (MethodDescriptor) methodsIterator.next();
            md.print(buffer);
            buffer.append("\n");
        }
    }
}
