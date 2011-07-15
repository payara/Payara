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

package com.sun.jdo.api.persistence.enhancer.impl;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;

import com.sun.jdo.api.persistence.enhancer.classfile.*;

import com.sun.jdo.api.persistence.enhancer.util.Support;
import com.sun.jdo.api.persistence.enhancer.util.InternalError;
import com.sun.jdo.api.persistence.enhancer.util.ClassFileSource;

//@olsen: added import
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;


//@olsen: cosmetics
//@olsen: moved: this class -> package impl
//@olsen: subst: (object)state -> flags
//@olsen: subst: JDOflags -> jdoFlags
//@olsen: subst: makeJDO[gs]etFlags -> makeJDO[GS]etFlags
//@olsen: subst: JDO[gs]etFlags -> jdo[GS]etFlags
//@olsen: subst: [Nn]eedsJDORefMethods -> [Nn]eedsJDOStateManagerMethods
//@olsen: subst: JDOref -> jdoStateManager
//@olsen: subst: makeJDO[gs]etRef -> makeJDO[GS]etStateManager
//@olsen: subst: JDO[gs]etRef -> jdo[GS]etStateManager
//@olsen: subst: [iI]Persistent -> [pP]ersistenceCapable
//@olsen: subst: PersistentAux -> StateManager
//@olsen: subst: jdo/ -> com/sun/forte4j/persistence/internal/
//@olsen: subst: jdo. -> com.sun.forte4j.persistence.internal.
//@olsen: subst: /* ... */ -> // ...
//@olsen: added: final modifiers on local variables
//@olsen: subst: FilterEnv -> Environment
//@olsen: made MethodBuilder's methods non-static
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: Enumeration,... -> Iterator, hasNext(), next()
//@olsen: subst: myMethodName -> methodName
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, affirm()
//@olsen: removed: proprietary support for ClassInfo
//@olsen: removed: proprietary support for FieldNote
//@olsen: removed: old, disabled ODI code


/**
 * MethodBuilder is a collection of methods which
 * create the persistence methods of a persistent class
 */
class MethodBuilder
    extends Support
    implements VMConstants {

    //@olsen: fix for bug 4467428:
    // Debugging under jdk 1.3.1 shows the problem that any breakpoints
    // in PC classes are ignored if the added jdo methods do NOT have a
    // non-empty line number table attribute, no matter whether the
    // 'Synthetic' attribute is given or not.  However, this doesn't
    // seem to comply with the JVM Spec (2nd edition), which states
    // that the synthetic attribute _must_ be specified if no source
    // code information is available for the member:
    //
    //     4.7.6 The Synthetic Attribute
    //     ... A class member that does not appear in the source code must
    //     be marked using a Synthetic attribute. ...
    //
    //     4.7.8 The LineNumberTable Attribute
    //     The LineNumberTable attribute is an optional variable-length
    //     attribute in the attributes table of a Code (see 4.7.3)
    //     attribute. It may be used by debuggers to determine which
    //     part of the Java virtual machine code array corresponds to a
    //     given line number in the original source file. ... Furthermore,
    //     multiple LineNumberTable attributes may together represent a
    //     given line of a source file; that is, LineNumberTable attributes
    //     need not be one-to-one with source lines.
    //
    // Unfortunately, if we do both, adding the synthetic attribute and
    // a (dummy) line number table on generated methods, jdk's 1.3.1 javap
    // fails to disassemble the classfile with an exception:
    //
    //     sun.tools.java.CompilerError: checkOverride() synthetic
    //
    // So, to workaround these problems and to allow for both, debugging
    // and disassembling with the jdk (1.3.1) tools, we pretend that the
    // generated jdo methods have source code equivalents by
    // - not adding the synthetic code attribute
    // - providing a dummy line number table code attribute
    static private final boolean addSyntheticAttr = false;
    static private final boolean addLineNumberTableAttr = true;

    /* Central repository for the options and classes */
    //@olsen: added association
    //@olsen: made final
    private final Environment env;

    /**
     * Constructor.
     */
    //@olsen: added constructor
    //@olsen: added parameter 'env' for association
    public MethodBuilder(Environment env) {
        this.env = env;
    }

    /**
     * Build a null method named methodName for the class.
     *
     * public void methodName() {
     * }
     */
    //@olsen: moved to beginning of this class
    ClassMethod makeNullMethod(final ClassAction ca,
                               final String methodName) {
        //@olsen: added variable
        final String methodSig = "()V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass thisClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod nullMethod
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              0, // maxStack
                              1, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return nullMethod;
    }

    /**
     * Build the initializeContents method for the class
     */
//@olsen: dropped method
/*
    ClassMethod makeInitializeContents(ClassAction ca) {
        final ConstantPool pool = ca.classFile().pool();
        final ConstClass thisClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        ClassMethod initializeContentsMethod =
            new ClassMethod(ACCPublic,
                            pool.addUtf8("initializeContents"),
                            pool.addUtf8("(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V"),
                            methodAttrs);

        // begin of method body
        Insn begin = new InsnTarget();
        Insn insn = begin;

        // Allocated reg 2 as cached ClassInfo*
        insn = insn.append(
            Insn.create(opc_getstatic,
                        pool.addFieldRef(thisClass.asString(),
                                         ca.getClassInfoMember(),
                                         "Lcom/sun/forte4j/persistence/internal/ClassInfo;")));
        insn = insn.append(Insn.create(opc_astore_2));

        for (Iterator e = ca.fieldActions(); e.hasNext();) {
            FieldAction act = (FieldAction)e.next();
            if (act.isPersistent()) {
                int idx = act.index();
                // get this
                insn = insn.append(Insn.create(opc_aload_0));
                // get GenericObject
                insn = insn.append(Insn.create(opc_aload_1));
                // get field index
                insn = insn.append(InsnUtils.integerConstant(idx, pool));
                // get ClassInfo
                insn = insn.append(Insn.create(opc_aload_2));
                // call the get method
                insn = insn.append(Insn.create(opc_invokevirtual,
                                               pool.addMethodRef("com/sun/forte4j/persistence/internal/ObjectContents",
                                                                 act.getMethod(), act.getMethodSig())));

                switch(act.getMethodReturn()) {
                case T_DOUBLE:
                case T_LONG:
                case T_BOOLEAN:
                case T_CHAR:
                case T_FLOAT:
                case T_BYTE:
                case T_SHORT:
                case T_INT:
                case TC_STRING:
                    // The above types are assumed to be accurate, with no
                    // conversions required
                    break;

                case TC_OBJECT:
                case TC_INTERFACE:
                    if (!act.typeDescriptor().equals("Ljava/lang/Object;")) {
                        ConstClass fieldType = pool.addClass(act.typeName());
                        // Add a cast to the appropriate type
                        insn = insn.append(Insn.create(opc_checkcast, fieldType));
                    }
                    break;
                default:
                    throw new InternalError("Unexpected return type");
                }

                // finally, store the result
                insn = insn.append(Insn.create(opc_putfield,
                                               pool.addFieldRef(thisClass.asString(), act.fieldName(),
                                                                act.typeDescriptor())));
            }
        }

        ConstClass superClass = ca.classFile().superName();
        if (!ca.getImplementsPersistence()) {
            // Need to invoke initializeContents on super
            // get this
            insn = insn.append(Insn.create(opc_aload_0));
            // get generic object
            insn = insn.append(Insn.create(opc_aload_1));
            // do the invoke
            insn = insn.append(Insn.create(opc_invokespecial,
                                           pool.addMethodRef(superClass.asString(),
                                                             "initializeContents", "(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V")));
        }

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              4, // maxStack
                              3, // maxLocals
                              begin,
                              new ExceptionTable(),
                              new AttributeVector()));
        return initializeContentsMethod;
    }
*/

    /**
     * Build the flushContents method for the class
     */
//@olsen: dropped method
/*
    ClassMethod makeFlushContents(ClassAction ca) {
        final ConstantPool pool = ca.classFile().pool();
        final ConstClass thisClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        ClassMethod flushContentsMethod =
            new ClassMethod(ACCPublic,
                            pool.addUtf8("flushContents"),
                            pool.addUtf8("(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V"),
                            methodAttrs);

        // begin of method body
        Insn begin = new InsnTarget();
        Insn insn = begin;

        // Allocated reg 2 as cached ClassInfo*
        insn = insn.append(
            Insn.create(opc_getstatic,
                        pool.addFieldRef(thisClass.asString(),
                                         ca.getClassInfoMember(),
                                         "Lcom/sun/forte4j/persistence/internal/ClassInfo;")));
        insn = insn.append(Insn.create(opc_astore_2));

        for (Iterator e = ca.fieldActions(); e.hasNext();) {
            FieldAction act = (FieldAction)e.next();
            if (act.isPersistent()) {
                int idx = act.index();
                // get GenericObject
                insn = insn.append(Insn.create(opc_aload_1));
                // get field index
                insn = insn.append(InsnUtils.integerConstant(idx, pool));
                // get this
                insn = insn.append(Insn.create(opc_aload_0));
                // get the field value
                insn = insn = insn.append(Insn.create(opc_getfield,
                                                      pool.addFieldRef(thisClass.asString(), act.fieldName(),
                                                                       act.typeDescriptor())));
                // get ClassInfo
                insn = insn.append(Insn.create(opc_aload_2));

                // call the set method
                insn = insn.append(Insn.create(opc_invokevirtual,
                                               pool.addMethodRef("com/sun/forte4j/persistence/internal/ObjectContents",
                                                                 act.setMethod(), act.setMethodSig())));
            }
        }

        ConstClass superClass = ca.classFile().superName();
        if (!ca.getImplementsPersistence()) {
            // Need to invoke flushContents on super
            // get this
            insn = insn.append(Insn.create(opc_aload_0));
            // get generic object
            insn = insn.append(Insn.create(opc_aload_1));
            // do the invoke
            insn = insn.append(Insn.create(opc_invokespecial,
                                           pool.addMethodRef(superClass.asString(),
                                                             "flushContents", "(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V")));
        }

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              6, // maxStack (might actually be 5, but ok)
                              3, // maxLocals
                              begin,
                              new ExceptionTable(),
                              new AttributeVector()));
        return flushContentsMethod;
    }
*/

    /**
     * Build the clearContents method for the class
     */
//@olsen: dropped method
/*
    ClassMethod makeClearContents(ClassAction ca) {
        final ConstantPool pool = ca.classFile().pool();
        final ConstClass thisClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        ClassMethod clearContentsMethod =
            new ClassMethod(ACCPublic,
                            pool.addUtf8("clearContents"),
                            pool.addUtf8("()V"),
                            methodAttrs);

        // begin of method body
        Insn begin = new InsnTarget();
        Insn insn = begin;

        for (Iterator e = ca.fieldActions(); e.hasNext();) {
            FieldAction act = (FieldAction)e.next();
            if (act.isPersistent()) {
                int idx = act.index();
                // get this
                insn = insn.append(Insn.create(opc_aload_0));

                // Use the getMethodReturn type to decide how to initialize
                switch(act.getMethodReturn()) {
                case T_DOUBLE:
                    insn = insn.append(Insn.create(opc_dconst_0));
                    break;
                case T_LONG:
                    insn = insn.append(Insn.create(opc_lconst_0));
                    break;
                case T_FLOAT:
                    insn = insn.append(Insn.create(opc_fconst_0));
                    break;
                case T_BOOLEAN:
                case T_CHAR:
                case T_BYTE:
                case T_SHORT:
                case T_INT:
                    insn = insn.append(Insn.create(opc_iconst_0));
                    break;
                case TC_STRING:
                case TC_OBJECT:
                case TC_INTERFACE:
                    insn = insn.append(Insn.create(opc_aconst_null));
                    break;
                default:
                    throw new InternalError("Unexpected return type");
                }

                // finally, store the result
                insn = insn.append(Insn.create(opc_putfield,
                                               pool.addFieldRef(thisClass.asString(), act.fieldName(),
                                                                act.typeDescriptor())));
            }
        }

        ConstClass superClass = ca.classFile().superName();
        if (!ca.getImplementsPersistence()) {
            // Need to invoke clearContents on super
            // get this
            insn = insn.append(Insn.create(opc_aload_0));
            // do the invoke
            insn = insn.append(Insn.create(opc_invokespecial,
                                           pool.addMethodRef(superClass.asString(),
                                                             "clearContents", "()V")));
        }

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              3, // maxStack (maybe only 2 - ok)
                              1, // maxLocals
                              begin,
                              new ExceptionTable(),
                              new AttributeVector()));
        return clearContentsMethod;
    }
*/

    /**
     * Build an empty class initializer method for this class
     */
//@olsen: dropped method
/*
    ClassMethod makeClassInit(ClassAction ca) {
        final ConstantPool pool = ca.classFile().pool();
        final ConstClass thisClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        ClassMethod classInitMethod =
            new ClassMethod(ACCStatic,
                            pool.addUtf8("<clinit>"),
                            pool.addUtf8("()V"),
                            methodAttrs);

        // begin of method body
        Insn begin = new InsnTarget();
        Insn insn = begin;

        // return from the initializer
        // end of method body
        insn = insn.append(Insn.create(opc_return));

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              0,
                              0, // maxLocals
                              begin,
                              new ExceptionTable(),
                              new AttributeVector()));
        return classInitMethod;
    }
*/

    /**
     * Build the jdoGetStateManager method for the class.
     *
     * public StateManager jdoGetStateManager() {
     *     return this.jdoStateManager;
     * }
     */
    //@olsen: cosmetics
    ClassMethod makeJDOGetStateManager(final ClassAction ca,
                                       final String methodName) {
        //@olsen: added variable
        final String methodSig = "()" + JDOMetaData.JDOStateManagerSig;//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoGetStateManagerMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // get jdoStateManager field
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(opc_getfield,
                        pool.addFieldRef(theClass.asString(),
                                         JDOMetaData.JDOStateManagerFieldName,
                                         JDOMetaData.JDOStateManagerFieldSig)));

        // end of method body
        insn = insn.append(Insn.create(opc_areturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              1, // maxStack
                              1, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoGetStateManagerMethod;
    }

    /**
     * Build the jdoSetStateManager method for the class.
     *
     * public void jdoSetStateManager(StateManager sm) {
     *     this.jdoStateManager = sm;
     * }
     */
    //@olsen: cosmetics
    ClassMethod makeJDOSetStateManager(final ClassAction ca,
                                       final String methodName) {
        //@olsen: added variable
        final String methodSig = "(" + JDOMetaData.JDOStateManagerSig + ")V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoSetStateManagerMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // put argument value to jdoStateManager field
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            Insn.create(opc_putfield,
                        pool.addFieldRef(theClass.asString(),
                                         JDOMetaData.JDOStateManagerFieldName,
                                         JDOMetaData.JDOStateManagerFieldSig)));

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              2, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoSetStateManagerMethod;
    }

    /**
     * Build the jdoGetFlags method for the class.
     *
     * public byte jdoGetFlags() {
     *     return this.jdoFlags;
     * }
     */
    //@olsen: cosmetics
    ClassMethod makeJDOGetFlags(final ClassAction ca,
                                final String methodName) {
        //@olsen: added variable
        final String methodSig = "()B";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoGetFlagsMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // get jdoFlags field
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(opc_getfield,
                        pool.addFieldRef(theClass.asString(),
                                         JDOMetaData.JDOFlagsFieldName,
                                         JDOMetaData.JDOFlagsFieldSig)));

        // end of method body
        insn = insn.append(Insn.create(opc_ireturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              1, // maxStack
                              1, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoGetFlagsMethod;
    }

    /**
     * Build the jdoSetFlags method for the class.
     *
     * public void jdoSetFlags(byte flags) {
     *     this.jdoFlags = flags;
     * }
     */
    //@olsen: cosmetics
    ClassMethod makeJDOSetFlags(final ClassAction ca,
                                final String methodName) {
        //@olsen: added variable
        final String methodSig = "(B)V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoSetFlagsMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // put argument value to jdoFlags field
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(Insn.create(opc_iload_1));
        insn = insn.append(
            Insn.create(opc_putfield,
                        pool.addFieldRef(theClass.asString(),
                                         JDOMetaData.JDOFlagsFieldName,
                                         JDOMetaData.JDOFlagsFieldSig)));

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              2, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoSetFlagsMethod;
    }

    /**
     * Build the jdoMakeDirty method for the class.
     *
     * public void makeDirty() {
     *     final StateManager sm = this.jdoStateManager;
     *     if (sm != null)
     *         sm.makeDirty(fieldName);
     * }
     */
    //@olsen: added method for generating the jdoMakeDirty method
    ClassMethod makeJDOMakeDirtyMethod(final ClassAction ca,
                                       final String methodName) {
        //@olsen: added variable
        final String methodSig = "(Ljava/lang/String;)V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoMakeDirtyMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // fetch the jdoStateManager field into local var
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(
                opc_getfield,
                pool.addFieldRef(theClass.asString(),
                                 JDOMetaData.JDOStateManagerFieldName,
                                 JDOMetaData.JDOStateManagerFieldSig)));
        insn = insn.append(Insn.create(opc_astore_2));

        // test the jdoStateManager field and goto end if null
        InsnTarget end = new InsnTarget();
        insn = insn.append(Insn.create(opc_aload_2));
        insn = insn.append(Insn.create(opc_ifnull, end));

        // call the jdoStateManager's makeDirty method with argument
        insn = insn.append(Insn.create(opc_aload_2));
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            new InsnInterfaceInvoke(
                pool.addInterfaceMethodRef(
                    JDOMetaData.JDOStateManagerPath,
                    "makeDirty",//NOI18N
                    "(Ljava/lang/String;)V"),//NOI18N
                2));

        // end of method body
        insn = insn.append(end);
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              2, // maxStack
                              3, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoMakeDirtyMethod;
    }

    /**
     * Build an interrogative method named methodName for the class.
     *
     * public boolean isXXX() {
     *     final StateManager sm = this.jdoStateManager;
     *     if (sm == null)
     *         return false;
     *     return sm.isXXXX();
     * }
     */
    //@olsen: added method for generating an interrogative JDO method
    ClassMethod makeJDOInterrogativeMethod(final ClassAction ca,
                                           final String methodName) {
        //@olsen: added variable
        final String methodSig = "()Z";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod interrogativeMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // fetch the jdoStateManager field into local var
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(
                opc_getfield,
                pool.addFieldRef(theClass.asString(),
                                 JDOMetaData.JDOStateManagerFieldName,
                                 JDOMetaData.JDOStateManagerFieldSig)));
        insn = insn.append(Insn.create(opc_astore_1));

        // test the jdoStateManager field and return false if null
        InsnTarget call = new InsnTarget();
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(Insn.create(opc_ifnonnull, call));
        insn = insn.append(Insn.create(opc_iconst_0));
        insn = insn.append(Insn.create(opc_ireturn));

        // call the jdoStateManager's interrogative method (jdoIs... -> is...)
        final String callName = "i" + methodName.substring(4);//NOI18N
        insn = insn.append(call);
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            new InsnInterfaceInvoke(
                pool.addInterfaceMethodRef(
                    JDOMetaData.JDOStateManagerPath,
                    callName,
                    "()Z"),//NOI18N
                1));

        // end of method body
        insn = insn.append(Insn.create(opc_ireturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              1, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return interrogativeMethod;
    }

    /**
     * Build the jdoGetPersistenceManager method for the class.
     *
     * public PersistenceManager jdoGetPersistenceManager() {
     *     final StateManager sm = this.jdoStateManager;
     *     if (sm == null)
     *         return null;
     *     return sm.getPersistenceManager();
     * }
     */
    //@olsen: added method for generating the jdoGetPersistenceManager method
    ClassMethod makeJDOGetPersistenceManagerMethod(final ClassAction ca,
                                                   final String methodName) {
        //@olsen: added variable
        final String methodSig = "()" + JDOMetaData.JDOPersistenceManagerSig;//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoGetPersistenceManagerMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // fetch the jdoStateManager field into local var
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(
                opc_getfield,
                pool.addFieldRef(theClass.asString(),
                                 JDOMetaData.JDOStateManagerFieldName,
                                 JDOMetaData.JDOStateManagerFieldSig)));
        insn = insn.append(Insn.create(opc_astore_1));

        // test the jdoStateManager field and return null if null
        InsnTarget call = new InsnTarget();
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(Insn.create(opc_ifnonnull, call));
        insn = insn.append(Insn.create(opc_aconst_null));
        insn = insn.append(Insn.create(opc_areturn));

        // call the jdoStateManager's getPersistenceManager method
        insn = insn.append(call);
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            new InsnInterfaceInvoke(
                pool.addInterfaceMethodRef(
                    JDOMetaData.JDOStateManagerPath,
                    "getPersistenceManager",//NOI18N
                    "()" + JDOMetaData.JDOPersistenceManagerSig),//NOI18N
                1));

        // end of method body
        insn = insn.append(Insn.create(opc_areturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              1, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoGetPersistenceManagerMethod;
    }

    /**
     * Build the jdoGetObjectId method for the class.
     *
     * public Object jdoGetObjectId() {
     *     final StateManager sm = this.jdoStateManager;
     *     if (sm == null)
     *         return null;
     *     return sm.getObjectId();
     * }
     */
    //@olsen: added method for generating the jdoGetObjectId method
    ClassMethod makeJDOGetObjectIdMethod(final ClassAction ca,
                                         final String methodName) {
        //@olsen: added variable
        final String methodSig = "()Ljava/lang/Object;";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();

        //@olsen: made method final
        final ClassMethod jdoGetObjectIdMethod
            = new ClassMethod(ACCPublic | ACCFinal,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // fetch the jdoStateManager field into local var
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(
                opc_getfield,
                pool.addFieldRef(theClass.asString(),
                                 JDOMetaData.JDOStateManagerFieldName,
                                 JDOMetaData.JDOStateManagerFieldSig)));
        insn = insn.append(Insn.create(opc_astore_1));

        // test the jdoStateManager field and return null if null
        InsnTarget call = new InsnTarget();
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(Insn.create(opc_ifnonnull, call));
        insn = insn.append(Insn.create(opc_aconst_null));
        insn = insn.append(Insn.create(opc_areturn));

        // call the jdoStateManager's getObjectId method
        insn = insn.append(call);
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            new InsnInterfaceInvoke(
                pool.addInterfaceMethodRef(
                    JDOMetaData.JDOStateManagerPath,
                    "getObjectId",//NOI18N
                    "()Ljava/lang/Object;"),//NOI18N
                1));

        // end of method body
        insn = insn.append(Insn.create(opc_areturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              1, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoGetObjectIdMethod;
    }

    /**
     * Build the jdoGetJDOConstructor method for the class.
     *
     * public <init>(StateManager sm) {
     *     this.jdoFlags = 1;
     *     this.jdoStateManager = sm;
     * }
     */
    //@olsen: added method for generating the JDO constructor
    ClassMethod makeJDOConstructor(final ClassAction ca,
                                   final String methodName) {
        //@olsen: added variable
        final String methodSig = "(" + JDOMetaData.JDOStateManagerSig + ")V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod jdoGetJDOConstructor
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // call the super-class' constructor
        final ConstClass superClass = ca.classFile().superName();
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(opc_invokespecial,
                        pool.addMethodRef(superClass.asString(),
                                          "<init>",//NOI18N
                                          "()V")));//NOI18N

        // put argument value to jdoFlags field
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(Insn.create(opc_iconst_1));
        insn = insn.append(
            Insn.create(opc_putfield,
                        pool.addFieldRef(theClass.asString(),
                                         JDOMetaData.JDOFlagsFieldName,
                                         JDOMetaData.JDOFlagsFieldSig)));

        // put argument value to jdoStateManager field
        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            Insn.create(
                opc_putfield,
                pool.addFieldRef(theClass.asString(),
                                 JDOMetaData.JDOStateManagerFieldName,
                                 JDOMetaData.JDOStateManagerFieldSig)));

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              2, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoGetJDOConstructor;
    }

    /**
     * Build the jdoNewInstance method for the class.
     *
     * public Object jdoNewInstance(StateManager sm) {
     *     return new <init>(sm);
     * }
     */
    //@olsen: added method for generating the getObjectId method
    ClassMethod makeJDONewInstanceMethod(final ClassAction ca,
                                         final String methodName) {
        //@olsen: added variable
        final String methodSig
            = "(" + JDOMetaData.JDOStateManagerSig + ")Ljava/lang/Object;";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod jdoNewInstanceMethod
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // create an instance of the class by JDO constructor
        insn = insn.append(Insn.create(opc_new, theClass));
        insn = insn.append(Insn.create(opc_dup));
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            Insn.create(
                opc_invokespecial,
                pool.addMethodRef(
                    theClass.asString(),
                    "<init>",//NOI18N
                    "(" + JDOMetaData.JDOStateManagerSig + ")V")));//NOI18N

        // end of method body
        insn = insn.append(Insn.create(opc_areturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              3, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoNewInstanceMethod;
    }

    /**
     * Build the jdoClear method for the class.
     *
     * public void jdoClear() {
     *     ...
     * }
     */
    //@olsen: added method for generating the jdoClear method
    ClassMethod makeJDOClearMethod(final ClassAction ca,
                                   final String methodName) {
        //@olsen: added variable
        final String methodSig = "()V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod jdoClearMethod
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        //@olsen: disabled code
        if (false) {
            // reset jdoFlags = LOAD_REQUIRED
            insn = insn.append(Insn.create(opc_aload_0));
            insn = insn.append(Insn.create(opc_iconst_1));
            insn = insn.append(
                Insn.create(opc_putfield,
                            pool.addFieldRef(theClass.asString(),
                                             JDOMetaData.JDOFlagsFieldName,
                                             JDOMetaData.JDOFlagsFieldSig)));
        }

        // iterate over all declared fields of the class
        for (Iterator e = ca.fieldActions(); e.hasNext();) {
            final FieldAction act = (FieldAction)e.next();
            //printFieldAction(act);
            //System.out.println();

            // ignore non-persistent fields
            if (!act.isPersistent())
                continue;

            // ignore primary key fields
            if (act.isPrimaryKey())
                continue;

            //@olsen: disconnect mutable SCOs before clear
            if (act.isMutableSCO()) {
                // fetch field
                insn = insn.append(Insn.create(opc_aload_0));
                insn = insn.append(
                    Insn.create(opc_getfield,
                                pool.addFieldRef(
                                    theClass.asString(),
                                    act.fieldName(),
                                    act.typeDescriptor())));

                // test whether instanceof SCO base type
                // skip disconnecting if == 0
                final ConstClass cc
                    = pool.addClass(JDOMetaData.JDOSecondClassObjectBasePath);
                InsnTarget disconnect = new InsnTarget();
                InsnTarget afterDisconnect = new InsnTarget();
                insn = insn.append(
                    Insn.create(opc_dup));
                insn = insn.append(
                    Insn.create(opc_instanceof,
                                cc));
                insn = insn.append(
                    Insn.create(opc_ifne,
                                disconnect));

                // pop field and skip disconnecting
                insn = insn.append(
                    Insn.create(opc_pop));
                insn = insn.append(
                    Insn.create(opc_goto, afterDisconnect));

                // disconnect SCO field's object
                insn = insn.append(disconnect);

                // cast to SCO base type
                insn = insn.append(
                    Insn.create(opc_checkcast,
                                cc));

                // call method: void unsetOwner();
                final int requiredStack = 1;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOSecondClassObjectBasePath,
                            "unsetOwner",//NOI18N
                            "()V"),//NOI18N
                        requiredStack));

                insn = insn.append(afterDisconnect);
            }

            // get this
            insn = insn.append(Insn.create(opc_aload_0));

            // use the getMethodReturn type to decide how to clear field
            switch(act.getMethodReturn()) {
            case T_DOUBLE:
                insn = insn.append(Insn.create(opc_dconst_0));
                break;
            case T_LONG:
                insn = insn.append(Insn.create(opc_lconst_0));
                break;
            case T_FLOAT:
                insn = insn.append(Insn.create(opc_fconst_0));
                break;
            case T_BOOLEAN:
            case T_CHAR:
            case T_BYTE:
            case T_SHORT:
            case T_INT:
                insn = insn.append(Insn.create(opc_iconst_0));
                break;
            case TC_STRING:
            case TC_OBJECT:
            case TC_INTERFACE:
                insn = insn.append(Insn.create(opc_aconst_null));
                break;
            default:
                throw new InternalError("Unexpected return type");//NOI18N
            }

            // put default value to field
            insn = insn.append(
                Insn.create(opc_putfield,
                            pool.addFieldRef(theClass.asString(),
                                             act.fieldName(),
                                             act.typeDescriptor())));
        }

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              3, // maxStack
                              1, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoClearMethod;
    }

    /**
     * Build the jdoCopy method for the class.
     *
     * public void jdoCopy(Object o, boolean cloneSCOs) {
     *     ...
     * }
     */
//@lars, @olsen: disabled jdoCopy becuase of two problems with the
// current code (this method hasn't been used by the runtime anyway):
// 4388418: Generated jdoCopy() must ignore static fields
// 4388367: Generated jdoCopy() can throw NPE if argument = 'true'
/*
    //@olsen: added method for generating the jdoCopy method
    ClassMethod makeJDOCopyMethod(final ClassAction ca,
                                  final String methodName) {

        //@olsen: added variable
        final String methodSig = "(Ljava/lang/Object;Z)V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod jdoCopyMethod
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // get class object of this and of argument object
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            Insn.create(opc_invokevirtual,
                        pool.addMethodRef(
                            "java/lang/Object",//NOI18N
                            "getClass",//NOI18N
                            "()Ljava/lang/Class;")));//NOI18N
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(
            Insn.create(opc_invokevirtual,
                        pool.addMethodRef(
                            "java/lang/Object",//NOI18N
                            "getClass",//NOI18N
                            "()Ljava/lang/Class;")));//NOI18N

        // test class objects for equality and throw exception if false
        insn = insn.append(
            Insn.create(opc_invokevirtual,
                        pool.addMethodRef(
                            "java/lang/Object",//NOI18N
                            "equals",//NOI18N
                            "(Ljava/lang/Object;)Z")));//NOI18N
        InsnTarget cast = new InsnTarget();
        insn = insn.append(Insn.create(opc_ifne, cast));
        final String exceptionClassName
            = "com/sun/forte4j/persistence/JDOFatalException";//NOI18N
        insn = insn.append(
            Insn.create(opc_new,
                        pool.addClass(exceptionClassName)));
        insn = insn.append(Insn.create(opc_dup));
        insn = insn.append(
            Insn.create(opc_invokespecial,
                        pool.addMethodRef(
                            exceptionClassName,
                            "<init>",//NOI18N
                            "()V")));//NOI18N
        insn = insn.append(Insn.create(opc_athrow));

        // cast argument object to this class' type and store into local var
        insn = insn.append(cast);
        insn = insn.append(Insn.create(opc_aload_1));
        insn = insn.append(Insn.create(opc_checkcast, theClass));
        insn = insn.append(Insn.create(opc_astore_3));

        // iterate over all declared fields of the class
        final ArrayList mscoFields = new ArrayList();
        final JDOMetaData jdoMetaData = env.getJDOMetaData();
        for (Iterator e = ca.fieldActions(); e.hasNext();) {
            final FieldAction act = (FieldAction)e.next();
            //printFieldAction(act);
            //System.out.println();

            // remember fields of MSCO type for later processing
            if (act.isMutableSCO()) {
                mscoFields.add(act);
                continue;
            }

            // get this object
            insn = insn.append(Insn.create(opc_aload_0));

            // fetch arguments object's field
            insn = insn.append(Insn.create(opc_aload_3));
            insn = insn.append(
                Insn.create(opc_getfield,
                            pool.addFieldRef(
                                theClass.asString(),
                                act.fieldName(),
                                act.typeDescriptor())));

            // store into this object's field
            insn = insn.append(
                Insn.create(opc_putfield,
                            pool.addFieldRef(
                                theClass.asString(),
                                act.fieldName(),
                                act.typeDescriptor())));
        }

        if (mscoFields.size() > 0) {
            // test boolean argument value and clone msco fields if true
            insn = insn.append(Insn.create(opc_iload_2));
            InsnTarget shallow = new InsnTarget();
            insn = insn.append(Insn.create(opc_ifeq, shallow));

            // clone fields
            for (Iterator i = mscoFields.iterator(); i.hasNext();) {
                final FieldAction act = (FieldAction)i.next();

                // get this object
                insn = insn.append(Insn.create(opc_aload_0));

                // fetch arguments object's field
                insn = insn.append(Insn.create(opc_aload_3));
                insn = insn.append(
                    Insn.create(opc_getfield,
                                pool.addFieldRef(
                                    theClass.asString(),
                                    act.fieldName(),
                                    act.typeDescriptor())));

                // clone field value and cast to field's type
                insn = insn.append(
                    Insn.create(opc_invokevirtual,
                                pool.addMethodRef(
                                    act.typeName(),
                                    "clone",//NOI18N
                                    "()Ljava/lang/Object;")));//NOI18N
                insn = insn.append(
                    Insn.create(opc_checkcast,
                                pool.addClass(act.typeName())));

                // store into this object's field
                insn = insn.append(
                    Insn.create(opc_putfield,
                                pool.addFieldRef(
                                    theClass.asString(),
                                    act.fieldName(),
                                    act.typeDescriptor())));
            }
            InsnTarget done = new InsnTarget();
            insn = insn.append(Insn.create(opc_goto, done));

            // copy field values
            insn = insn.append(shallow);
            for (Iterator i = mscoFields.iterator(); i.hasNext();) {
                final FieldAction act = (FieldAction)i.next();

                // get this object
                insn = insn.append(Insn.create(opc_aload_0));

                // fetch arguments object's field
                insn = insn.append(Insn.create(opc_aload_3));
                insn = insn.append(
                    Insn.create(opc_getfield,
                                pool.addFieldRef(
                                    theClass.asString(),
                                    act.fieldName(),
                                    act.typeDescriptor())));

                // store into this object's field
                insn = insn.append(
                    Insn.create(opc_putfield,
                                pool.addFieldRef(
                                    theClass.asString(),
                                    act.fieldName(),
                                    act.typeDescriptor())));
            }

            insn = insn.append(done);
        }

        // end of method body
        insn = insn.append(Insn.create(opc_return));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              4, // maxStack
                              4, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoCopyMethod;
    }
*/

    /**
     * Build the jdoGetField method for the class.
     *
     * public Object jdoGetField(int fieldNumber) {
     *     return ...
     * }
     */
    //@olsen: added method for generating the jdoGetField method
    ClassMethod makeJDOGetFieldMethod(final ClassAction ca,
                                      final String methodName) {
        //@olsen: added variable
        final String methodSig = "(I)Ljava/lang/Object;";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod jdoGetFieldMethod
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // get the declared, persistent fields from the JDOMetaData
        final String className = ca.className();
        //@lars: changed getPersistentFields() into getManagedFields()
        final String[] fieldNames
            = env.getJDOMetaData().getManagedFields(className);
        final int nofFields = fieldNames.length;
        //@olsen: added println() for debugging
        if (false) {
            System.out.print("MethodBuilder.makeJDOGetFieldMethod(): "//NOI18N
                             + " declared, persistent fields of class '"//NOI18N
                             + className + "' = {");//NOI18N
            for (int i = 0; i < nofFields; i++)
                System.out.print(" " + fieldNames[i]);//NOI18N
            System.out.println(" }");//NOI18N
        }

        // generate the switch-statement only if more than zero fields
        final InsnTarget defaultOp = new InsnTarget();
        if (nofFields > 0) {
            // get the declared, persistent fields from the class
            final HashMap fieldsByName = new HashMap();
            for (Iterator e = ca.fieldActions(); e.hasNext();) {
                final FieldAction act = (FieldAction)e.next();
                fieldsByName.put(act.fieldName(), act);
            }

            // do the tableswitch on argument
            insn = insn.append(Insn.create(opc_iload_1));
            final int lowOp = 0;
            final InsnTarget[] targetsOp = new InsnTarget[nofFields];
            for (int i = 0; i < nofFields; i++)
                targetsOp[i] = new InsnTarget();
            insn = insn.append(
                new InsnTableSwitch(lowOp, defaultOp, targetsOp));

            // do the case-targets for field accesses
            for (int i = 0; i < nofFields; i++) {
                // target for accessing field [i]
                insn = insn.append(targetsOp[i]);

                final FieldAction act
                    = (FieldAction)fieldsByName.get(fieldNames[i]);
                affirm(act,
                       ("Field '" + fieldNames[i]//NOI18N
                        + "' returned by JDOMetaData is not known by class '"//NOI18N
                        + className + "'."));//NOI18N

                // use the getMethodReturn type to create the wrapper object
                final String wrapperClassName;
                final String wrapperSignature;
                switch(act.getMethodReturn()) {
                case T_DOUBLE:
                    wrapperClassName = "java/lang/Double";//NOI18N
                    wrapperSignature = "(D)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_LONG:
                    wrapperClassName = "java/lang/Long";//NOI18N
                    wrapperSignature = "(J)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_FLOAT:
                    wrapperClassName = "java/lang/Float";//NOI18N
                    wrapperSignature = "(F)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_BOOLEAN:
                    wrapperClassName = "java/lang/Boolean";//NOI18N
                    wrapperSignature = "(Z)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_CHAR:
                    wrapperClassName = "java/lang/Character";//NOI18N
                    wrapperSignature = "(C)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_BYTE:
                    wrapperClassName = "java/lang/Byte";//NOI18N
                    wrapperSignature = "(B)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_SHORT:
                    wrapperClassName = "java/lang/Short";//NOI18N
                    wrapperSignature = "(S)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case T_INT:
                    wrapperClassName = "java/lang/Integer";//NOI18N
                    wrapperSignature = "(I)V";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_new, pool.addClass(wrapperClassName)));
                    insn = insn.append(Insn.create(opc_dup));
                    break;
                case TC_STRING:
                case TC_OBJECT:
                case TC_INTERFACE:
                    wrapperClassName = null;
                    wrapperSignature = null;
                    break;
                default:
                    throw new InternalError("Unexpected return type");//NOI18N
                }

                // fetch this object's field
                insn = insn.append(Insn.create(opc_aload_0));
                insn = insn.append(
                    Insn.create(
                        opc_getfield,
                        pool.addFieldRef(
                            theClass.asString(),
                            act.fieldName(),
                            act.typeDescriptor())));

                // wrap the field value if primitive
                switch(act.getMethodReturn()) {
                case T_DOUBLE:
                case T_LONG:
                case T_FLOAT:
                case T_BOOLEAN:
                case T_CHAR:
                case T_BYTE:
                case T_SHORT:
                case T_INT:
                    insn = insn.append(
                        Insn.create(
                            opc_invokespecial,
                            pool.addMethodRef(
                                wrapperClassName,
                                "<init>",//NOI18N
                                wrapperSignature)));
                    break;
                case TC_STRING:
                case TC_OBJECT:
                case TC_INTERFACE:
                    break;
                default:
                    throw new InternalError("Unexpected return type");//NOI18N
                }

                // return the object (break)
                insn = insn.append(Insn.create(opc_areturn));
            }
        }

        // do the default branch target creating a fatal exception
        insn = insn.append(defaultOp);
        final String exceptionClassName
            = "com/sun/jdo/api/persistence/support/JDOFatalException";//NOI18N
        insn = insn.append(
            Insn.create(
                opc_new,
                pool.addClass(exceptionClassName)));
        insn = insn.append(Insn.create(opc_dup));
        insn = insn.append(
            Insn.create(
                opc_invokespecial,
                pool.addMethodRef(
                    exceptionClassName,
                    "<init>",//NOI18N
                    "()V")));//NOI18N

        // end of method body
        insn = insn.append(Insn.create(opc_athrow));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              4, // maxStack
                              2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoGetFieldMethod;
    }

    /**
     * Build the jdoSetField method for the class.
     *
     * public jdoSetField(int fieldNumber, Object value) {
     *     ...
     * }
     */
    //@olsen: added method for generating the jdoSetField method
    ClassMethod makeJDOSetFieldMethod(final ClassAction ca,
                                      final String methodName) {
        //@olsen: added variable
        final String methodSig = "(ILjava/lang/Object;)V";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ConstantPool pool = ca.classFile().pool();
        final ConstClass theClass = ca.classFile().className();

        final AttributeVector methodAttrs = new AttributeVector();
        final ClassMethod jdoSetFieldMethod
            = new ClassMethod(ACCPublic,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

        // get the declared, persistent fields from the JDOMetaData
        final String className = ca.className();
        //@lars: changed getPersistentFields() into getManagedFields()
        final String[] fieldNames
            = env.getJDOMetaData().getManagedFields(className);
        final int nofFields = fieldNames.length;
        //@olsen: added println() for debugging
        if (false) {
            System.out.print("MethodBuilder.makeJDOSetFieldMethod(): "//NOI18N
                             + " declared, persistent fields of class '"//NOI18N
                             + className + "' = {");//NOI18N
            for (int i = 0; i < nofFields; i++)
                System.out.print(" " + fieldNames[i]);//NOI18N
            System.out.println(" }");//NOI18N
        }

        // generate the switch-statement only if more than zero fields
        final InsnTarget defaultOp = new InsnTarget();
        if (nofFields > 0) {
            // get the declared, persistent fields from the class
            final HashMap fieldsByName = new HashMap();
            for (Iterator e = ca.fieldActions(); e.hasNext();) {
                final FieldAction act = (FieldAction)e.next();
                fieldsByName.put(act.fieldName(), act);
            }

            // do the tableswitch on argument
            insn = insn.append(Insn.create(opc_iload_1));
            final int lowOp = 0;
            final InsnTarget[] targetsOp = new InsnTarget[nofFields];
            for (int i = 0; i < nofFields; i++)
                targetsOp[i] = new InsnTarget();
            insn = insn.append(
                new InsnTableSwitch(lowOp, defaultOp, targetsOp));

            // do the case-targets for field accesses
            for (int i = 0; i < nofFields; i++) {
                // target for accessing field [i]
                insn = insn.append(targetsOp[i]);

                final FieldAction act
                    = (FieldAction)fieldsByName.get(fieldNames[i]);
                affirm(act,
                       ("Field '"//NOI18N
                        + fieldNames[i]
                        + "' returned by JDOMetaData is not known by class '"//NOI18N
                        + className + "'."));//NOI18N

                // get object and value argument
                insn = insn.append(Insn.create(opc_aload_0));
                insn = insn.append(Insn.create(opc_aload_2));

                // use the getMethodReturn type to downcast the Object argument
                final String wrapperClassName;
                final String unwrapperSignature;
                final String unwrapperName;
                switch(act.getMethodReturn()) {
                case T_DOUBLE:
                    wrapperClassName = "java/lang/Double";//NOI18N
                    unwrapperSignature = "()D";//NOI18N
                    unwrapperName = "doubleValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_LONG:
                    wrapperClassName = "java/lang/Long";//NOI18N
                    unwrapperSignature = "()J";//NOI18N
                    unwrapperName = "longValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_FLOAT:
                    wrapperClassName = "java/lang/Float";//NOI18N
                    unwrapperSignature = "()F";//NOI18N
                    unwrapperName = "floatValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_BOOLEAN:
                    wrapperClassName = "java/lang/Boolean";//NOI18N
                    unwrapperSignature = "()Z";//NOI18N
                    unwrapperName = "booleanValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_CHAR:
                    wrapperClassName = "java/lang/Character";//NOI18N
                    unwrapperSignature = "()C";//NOI18N
                    unwrapperName = "charValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_BYTE:
                    wrapperClassName = "java/lang/Byte";//NOI18N
                    unwrapperSignature = "()B";//NOI18N
                    unwrapperName = "byteValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_SHORT:
                    wrapperClassName = "java/lang/Short";//NOI18N
                    unwrapperSignature = "()S";//NOI18N
                    unwrapperName = "shortValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case T_INT:
                    wrapperClassName = "java/lang/Integer";//NOI18N
                    unwrapperSignature = "()I";//NOI18N
                    unwrapperName = "intValue";//NOI18N
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(wrapperClassName)));
                    break;
                case TC_STRING:
                case TC_OBJECT:
                case TC_INTERFACE:
                    wrapperClassName = null;
                    unwrapperSignature = null;
                    unwrapperName = null;
                    insn = insn.append(
                        Insn.create(opc_checkcast,
                                    pool.addClass(act.typeName())));
                    break;
                default:
                    throw new InternalError("Unexpected return type");//NOI18N
                }

                // unwrap the object if primitive wrapper
                switch(act.getMethodReturn()) {
                case T_DOUBLE:
                case T_LONG:
                case T_FLOAT:
                case T_BOOLEAN:
                case T_CHAR:
                case T_BYTE:
                case T_SHORT:
                case T_INT:
                    insn = insn.append(
                        Insn.create(
                            opc_invokevirtual,
                            pool.addMethodRef(
                                wrapperClassName,
                                unwrapperName,
                                unwrapperSignature)));
                    break;
                case TC_STRING:
                case TC_OBJECT:
                case TC_INTERFACE:
                    break;
                default:
                    throw new InternalError("Unexpected return type");//NOI18N
                }

                // store argument value in field
                insn = insn.append(
                    Insn.create(
                        opc_putfield,
                        pool.addFieldRef(
                            theClass.asString(),
                            act.fieldName(),
                            act.typeDescriptor())));

                // return (break)
                insn = insn.append(Insn.create(opc_return));
            }
        }

        // do the default branch target creating a fatal exception
        insn = insn.append(defaultOp);
        final String exceptionClassName
            = "com/sun/jdo/api/persistence/support/JDOFatalException";//NOI18N
        insn = insn.append(
            Insn.create(
                opc_new,
                pool.addClass(exceptionClassName)));
        insn = insn.append(Insn.create(opc_dup));
        insn = insn.append(
            Insn.create(
                opc_invokespecial,
                pool.addMethodRef(
                    exceptionClassName,
                    "<init>",//NOI18N
                    "()V")));//NOI18N

        // end of method body
        insn = insn.append(Insn.create(opc_athrow));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              3, // maxStack
                              3, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        return jdoSetFieldMethod;
    }

    /**
     * Build the clone method for the class.
     */
    //@olsen: subst: makeClone -> makeJDOClone
    ClassMethod makeJDOClone(final ClassAction ca,
                             final String methodName) {
        //@olsen: added variable
        final String methodSig = "()Ljava/lang/Object;";//NOI18N
        env.message("adding "//NOI18N
                    + ca.classControl().userClassName() +
                    "." + methodName//NOI18N
                    + Descriptor.userMethodArgs(methodSig));

        final ClassFile cFile = ca.classFile();
        final ConstantPool pool = cFile.pool();
        final ConstClass theClass = cFile.className();
        final ConstClass superClass = cFile.superName();

        final AttributeVector methodAttrs = new AttributeVector();
        //@olsen: fixed bug: changed ACCProtected to ACCPublic to allow for
        //        an inherited method clone() to be public!
        //@olsen: removed ACCSynchronized flag
        final ClassMethod cloneMethod
            = new ClassMethod(ACCPublic, //|ACCSynchronized,
                              pool.addUtf8(methodName),
                              pool.addUtf8(methodSig),
                              methodAttrs);

        // begin of method body
        //@olsen: fix 4467428, made 'begin' final InsnTarget
        final InsnTarget begin = new InsnTarget();
        Insn insn = begin;

//@olsen: disabled feature
/*
        //   if (jdoFlags < 0)
        //     Implementation.fetch(this);

        InsnTarget cloneStart = new InsnTarget();

        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(opc_getfield,
                        pool.addFieldRef(theClass.asString(),
                                         JDOMetaData.JDOFlagsFieldName,
                                         JDOMetaData.JDOFlagsFieldSig)));
        insn = insn.append(Insn.create(opc_ifge, cloneStart));

        insn = insn.append(Insn.create(opc_aload_0));
        insn = insn.append(
            Insn.create(opc_invokestatic,
                        pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "fetch",
                                          "(" + JDOMetaData.JDOPersistenceCapableSig + ")V")));

        insn = insn.append(cloneStart);
*/

        // THISCLASS newObject = (THISCLASS) super.clone();
        {
            insn = insn.append(Insn.create(opc_aload_0));
            insn = insn.append(
                Insn.create(opc_invokespecial,
                            pool.addMethodRef(superClass.asString(),
//                            pool.addMethodRef("java/lang/Object",
                                              methodName,
                                              methodSig)));

            // add cast to the appropriate type
            insn = insn.append(Insn.create(opc_checkcast, theClass));
        }

//@olsen: disabled feature
/*
        if (ca.getNeedsJDOStateManagerMethods()) {
*/
        // newObject.jdoStateManager = null;
        if (false)
        {
            insn = insn.append(Insn.create(opc_dup));
            insn = insn.append(Insn.create(opc_aconst_null));
            insn = insn.append(
                Insn.create(
                    opc_putfield,
                    pool.addFieldRef(theClass.asString(),
                                     JDOMetaData.JDOStateManagerFieldName,
                                     JDOMetaData.JDOStateManagerFieldSig)));
        }

//@olsen: disabled feature
/*
        if (ca.getNeedsJDOFlagsMethods()) {
*/
        // newObject.jdoFlags = 0;
        if (false)
        {
            insn = insn.append(Insn.create(opc_dup));
            insn = insn.append(Insn.create(opc_iconst_0));
            insn = insn.append(
                Insn.create(opc_putfield,
                            pool.addFieldRef(theClass.asString(),
                                             JDOMetaData.JDOFlagsFieldName,
                                             JDOMetaData.JDOFlagsFieldSig)));
        }

        // return newObject;

        // end of method body
        insn = insn.append(Insn.create(opc_areturn));

        final AttributeVector codeSpecificAttrs = new AttributeVector();

        //@olsen: fix 4467428, added dummy, non-empty line number table
        if (addLineNumberTableAttr) {
            codeSpecificAttrs.addElement(
                new LineNumberTableAttribute(
                    pool.addUtf8(LineNumberTableAttribute.expectedAttrName),
                    new short[]{ 0 }, new InsnTarget[]{ begin }));
        }

        methodAttrs.addElement(
            new CodeAttribute(pool.addUtf8(CodeAttribute.expectedAttrName),
                              //@olsen: updated maxLocals
                              1, //3, //3, // maxStack
                              1, //2, // maxLocals
                              begin,
                              new ExceptionTable(),
                              codeSpecificAttrs));

        //@olsen: fix 4467428, added synthetic attribute for generated method
        if (addSyntheticAttr) {
            methodAttrs.addElement(
                new SyntheticAttribute(
                    pool.addUtf8(SyntheticAttribute.expectedAttrName)));
        }

        methodAttrs.addElement(
            new ExceptionsAttribute(
                pool.addUtf8(ExceptionsAttribute.expectedAttrName),
                pool.addClass("java/lang/CloneNotSupportedException")));//NOI18N

        return cloneMethod;
    }

    // for debugging
    static private void printFieldAction(FieldAction act) {
        System.out.println("fieldName() = " + act.fieldName());//NOI18N
        System.out.println("userFieldName() = " + act.userFieldName());//NOI18N
        System.out.println("typeDescriptor() = " + act.typeDescriptor());//NOI18N
        System.out.println("typeName() = " + act.typeName());//NOI18N
        System.out.println("fieldClassName() = " + act.fieldClassName());//NOI18N
        System.out.println("isPersistent() = " + act.isPersistent());//NOI18N
        System.out.println("isPrimaryKey() = " + act.isPrimaryKey());//NOI18N
        System.out.println("isMutableSCO() = " + act.isMutableSCO());//NOI18N
        System.out.println("isSynthetic() = " + act.isSynthetic());//NOI18N
        //System.out.println("index() = " + act.index());
        System.out.println("nDims() = " + act.nDims());//NOI18N
        System.out.println("createMethod() = " + act.createMethod());//NOI18N
        System.out.println("createMethodSig() = " + act.createMethodSig());//NOI18N
        System.out.println("setMethod() = " + act.setMethod());//NOI18N
        System.out.println("setMethodSig() = " + act.setMethodSig());//NOI18N
        System.out.println("setMethodArg() = "//NOI18N
                           + typeToString(act.setMethodArg()));
        System.out.println("getMethod() = " + act.getMethod());//NOI18N
        System.out.println("getMethodSig() = " + act.getMethodSig());//NOI18N
        System.out.println("getMethodReturn() = "//NOI18N
                           + typeToString(act.getMethodReturn()));
    }

    // for debugging
    static private String typeToString(int val) {
        switch(val) {
        case T_DOUBLE:
            return "T_DOUBLE";//NOI18N
        case T_LONG:
            return "T_LONG";//NOI18N
        case T_BOOLEAN:
            return "T_BOOLEAN";//NOI18N
        case T_CHAR:
            return "T_CHAR";//NOI18N
        case T_FLOAT:
            return "T_FLOAT";//NOI18N
        case T_BYTE:
            return "T_BYTE";//NOI18N
        case T_SHORT:
            return "T_SHORT";//NOI18N
        case T_INT:
            return "T_INT";//NOI18N
        case TC_STRING:
            return "TC_STRING";//NOI18N
        case TC_OBJECT:
            return "TC_OBJECT";//NOI18N
        case TC_INTERFACE:
            return "TC_INTERFACE";//NOI18N
        default:
            throw new InternalError("Unexpected return type");//NOI18N
        }
    }
}
