/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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