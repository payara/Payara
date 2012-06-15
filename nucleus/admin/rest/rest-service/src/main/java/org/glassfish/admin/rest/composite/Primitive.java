/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.composite;

import static org.objectweb.asm.Opcodes.*;

/**
 * This enum encapsulates the metadata for primitives needed for generating fields, getters and setters
 *
 * @author jdlee
 */
enum Primitive {

    DOUBLE("D", DRETURN, DLOAD),
    FLOAT("F", FRETURN, FLOAD),
    LONG("J", LRETURN, LLOAD),
    SHORT("S", IRETURN, ILOAD),
    INT("I", IRETURN, ILOAD),
//        CHAR   ("C", IRETURN, ILOAD),
    BYTE("B", IRETURN, ILOAD),
    BOOLEAN("Z", IRETURN, ILOAD);
    private final int returnOpcode;
    private final int setOpcode;
    private final String internalType;

    Primitive(String type, int returnOpcode, int setOpcode) {
        this.internalType = type;
        this.returnOpcode = returnOpcode;
        this.setOpcode = setOpcode;
    }

    public int getReturnOpcode() {
        return returnOpcode;
    }

    public int getSetOpCode() {
        return setOpcode;
    }

    public String getInternalType() {
        return internalType;
    }

    static Primitive getPrimitive(String type) {
        if ("S".equals(type) || "short".equals(type)) {
            return SHORT;
        } else if ("J".equals(type) || "long".equals(type)) {
            return LONG;
        } else if ("I".equals(type) || "int".equals(type)) {
            return INT;
        } else if ("F".equals(type) || "float".equals(type)) {
            return FLOAT;
        } else if ("D".equals(type) || "double".equals(type)) {
            return DOUBLE;
//            } else if ("C".equals(type) || "char".equals(type)) {
//                return CHAR;
        } else if ("B".equals(type) || "byte".equals(type)) {
            return BYTE;
        } else if ("Z".equals(type) || "boolean".equals(type)) {
            return BOOLEAN;
        } else {
            throw new RuntimeException("Unknown primitive type: " + type);
        }
    }
}