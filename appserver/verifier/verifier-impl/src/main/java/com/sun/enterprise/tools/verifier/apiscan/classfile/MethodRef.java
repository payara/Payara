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

package com.sun.enterprise.tools.verifier.apiscan.classfile;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class MethodRef {
    private String owningClassNameInternal; // in internal form, e.g. com/acme/Address

    private String owningClassName; // in external form, i.e. java.lang.Object

    private String name; // main

    private String descriptor; // ([Ljava.lang.String;)I

    public static final String CLINIT_NAME = "<clinit>"; // NOI18N

    public static final String CLINIT_DESC = "()V"; // NOI18N

    public MethodRef(String owningClassNameInternal, String name, String descriptor) {
        this.owningClassNameInternal = owningClassNameInternal;
        this.owningClassName = Util.convertToExternalClassName(owningClassNameInternal);
        this.name = name;
        this.descriptor = descriptor;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getOwningClassNameInternal() {
        return owningClassNameInternal;
    }

    public String getOwningClassName(){
        return owningClassName;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodRef)) return false;
        final MethodRef methodRef = (MethodRef) o;
        if (descriptor != null ?
                !descriptor.equals(methodRef.descriptor) :
                methodRef.descriptor != null)
            return false;
        if (name != null ?
                !name.equals(methodRef.name) : methodRef.name != null)
            return false;
        if (owningClassNameInternal != null ?
                !owningClassNameInternal.equals(methodRef.owningClassNameInternal) :
                methodRef.owningClassNameInternal != null)
            return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = (owningClassNameInternal != null ? owningClassNameInternal.hashCode() : 0);
        result = 29 * result + (name != null ? name.hashCode() : 0);
        result = 29 * result +
                (descriptor != null ? descriptor.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return owningClassNameInternal + "." + name + descriptor; // NOI18N
    }
}
