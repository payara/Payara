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

import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.Stack;
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
//@olsen: subst: JDOFlags -> jdoFlags
//@olsen: subst: JDO[gs]etFlags -> jdo[GS]etFlags
//@olsen: subst: (object)reference -> stateManager
//@olsen: subst: [Nn]eedsJDORefMethods -> [Nn]eedsJDOStateManagerMethods
//@olsen: subst: JDORef -> jdoStateManager
//@olsen: subst: JDO[gs]etRef -> jdo[GS]etStateManager
//@olsen: subst: [iI]Persistent -> [pP]ersistenceCapable
//@olsen: subst: PersistentAux -> StateManager
//@olsen: subst: jdo/ -> com/sun/forte4j/persistence/internal/
//@olsen: subst: /* ... */ -> // ...
//@olsen: subst: FilterEnv -> Environment
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: Hashtable -> Map, HashMap
//@olsen: subst: absolut jdo types and names -> constants from JDOMetaData
//@olsen: subst: .classControl(). -> .
//@olsen: subst: noteList -> note
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, affirm()
//@olsen: removed: proprietary support for HashCode
//@olsen: removed: support for [No]AnnotateField
//@olsen: removed: old, disabled ODI code


/*
 * The current code annotation strategy
 * 1) getfield instructions operating on persistent types cause a fetch
 * 2) putfield instructions operating on persistent types cause a dirty
 * 3) fetches which can be identified to be a fetch of "this" are moved
 *    to the start of the method.
 * 3) dirties which can be identified to be a dirty of "this" are moved
 *    to the start of the method only if the code path to the dirty is
 *    unconditional.
 * 4) Array loads cause array fetches
 * 5) Array stores cause array dirtys
 * 6) Each array fetch/dirty call which occurs in a loop construct
 *    is allocated a dedicated local variable which is used to cache
 *    the last array dirtied or fetched by the instruction in order to
 *    minimize the overhead in array manipulation within a loop.
 * 7) In calls to methods in non-persistence-aware classes which are
 *    declared to take array parameters, the array parameter is fetched.
 *    If the called code stores to the array, the user must manually
 *    annotate.
 * 8) Certain method invocations trigger fetches or dirties of arguments.
 *    These special cases are listed in InvokeAnnotation.java.
 *
 * Possible Alternative Code Annotation strategy
 * 1) non-static non-private methods always fetch/dirty "this"
 * 2) non-static private methods never fetch but may dirty "this"
 * 3) invocations of private methods from static methods fetch the target
 * 4) invocations of private methods from non-static methods fetch the
 *    target if it can't be identified as "this"
 * 5) putfields always cause dirtying of the target but if the
 *    target is known to be this, promote to a dirty of this.
 * 6) getfields only cause fetching of the target if the
 *    target is not known to be this
 * 7) Array loads cause array fetches
 * 8) Array stores cause array dirtys
 */


/**
 * MethodAnnotater controls the code annotation for a single method
 * within a class.
 */
//^olsen: move code -> MethodAction
class MethodAnnotater
    extends Support
    implements AnnotationConstants {

    //@olsen: made final
    private final ClassAction ca;

    //@olsen: made final
    private final ClassMethod method;

    //@olsen: made final
    private final ConstantPool pool;

    /* Central repository for the options and classes */
    //@olsen: added association
    //@olsen: made final
    private final Environment env;

    /* What types of annotation will be done on the method? */
    private int annotate;

    /* List of single element register values (Integer) for temporaries */
    private Vector tmpRegisters;

    /* List of double element register values (Integer) for temporaries */
    private Vector tmpDoubleRegisters;

    /* List of single word register values which cache fetches/stores
     * Each of these registers must be initialized to null at the start of
     * the method */
//@olsen: disabled feature
/*
    private Vector caches;
*/

    /* The maximum amount of stack needed by any specific annotation sequence,
     * less the amount of stack which the annotated instruction is known to
     * need, if any */
    private short annotationStack = 0;

    /* If true, the method will contain an unconditional fetch(this) */
//@olsen: disabled feature
/*
    private boolean fetchThis = false;
*/

    /* If true, the method will contain an unconditional dirty(this) */
//@olsen: disabled feature
/*
    private boolean dirtyThis = false;
*/

    /* Table mapping Insn to InsnNote - allows annotation computations to
     * be attached to instructions non-intrusively */
    private Map insnNotes = new HashMap(11);

    /* The largest loop contained within the method, or null.  */
//@olsen: disabled feature
/*
    private Loop largestLoop;
*/

    // package accessors

    /**
     * Is any annotation needed for this method?  The result of this
     * method isn't valid until after checkMethod has been run.
     */
    boolean needsAnnotation() {
        return annotate != 0;
    }

    /**
     * Constructor
     */
    //@olsen: added parameter 'env' for association
    MethodAnnotater(ClassAction ca,
                    ClassMethod method,
                    Environment env) {
        this.ca = ca;
        this.method = method;
        this.env = env;
        this.pool = ca.classFile().pool();
    }

// ---------------------------------------------------------------------------

    /**
     * Examine the method to determine what sort of annotations are needed
     */
    void checkMethod() {
        //@olsen: added printing output
        env.message(
            "checking method " + ca.userClassName()//NOI18N
            + "." + method.name().asString()//NOI18N
            + Descriptor.userMethodArgs(method.signature().asString()));

        //@olsen: cosmetics
        annotate = 0;
        final CodeAttribute codeAttr = method.codeAttribute();
        if (codeAttr == null) {
            return;
        }

//^olsen: make robust
/*
        if (isAnnotated(codeAttr)) {
            env.message("Method " + ca.userClassName() +
                        "." + method.name().asString() +
                        Descriptor.userMethodArgs(method.signature().asString()) +
                        " is already annotated.");
            return;
        }
*/

        // look for certain special cases to avoid
        //@olsen: cosmetics
        if (avoidAnnotation()) {
            return;
        }
        checkCode(codeAttr);

//@olsen: disabled feature
/*
        if (!avoidAnnotation()) {
            largestLoop = Loop.checkLoops(codeAttr.theCode());
            checkCode(codeAttr);
        } else if (methodIsPersistentFinalize()) {
            annotate = MakeThisTransient;
        }
*/
    }

    /**
     * Check to see if the code attribute contains any calls to
     * Implementaiton.fetch, or Implementation.dirty
     */
//^olsen: make robust
/*
    private boolean isAnnotated(CodeAttribute codeAttr) {
        for (Insn insn = codeAttr.theCode();
             insn != null;
             insn = insn.next()) {

            // All non-interface method invocations are InsnConstOp

            if (insn instanceof InsnConstOp) {
                InsnConstOp coInsn = (InsnConstOp) insn;
                ConstBasic operand = coInsn.value();
                if (operand instanceof ConstMethodRef) {

                    // A method invocation of some sort

                    ConstMethodRef methRef = (ConstMethodRef) operand;
                    if (methRef.className().asString().equals("com/sun/forte4j/persistence/internal/Implementation")) {

                        // A method invocation against class Persistent

                        ConstNameAndType nt = methRef.nameAndType();
                        String ntName = nt.name().asString();
                        if (ntName.equals("fetch") || ntName.equals("dirty"))
                            // A fetch or a dirty call
                            return true;
                    }
                }
            }
        }
        return false;
    }
*/

    /**
     * Check to see if this method is an initializer.
     */
//@olsen: disabled feature
/*
    private boolean methodIsInitializer() {
        String methName = method.name().asString();
        //^olsen: check for serialization
        return (methName.equals("<init>") ||
                (methName.equals("readObject") &&
                 method.signature().asString().equals(
                     "(Ljava/io/ObjectInputStream;)V")));
    }
*/


    /**
     * Check to see if this method is a finalize method.
     */
//@olsen: disabled feature
/*
    private boolean methodIsFinalize() {
        return (method.name().asString().equals("finalize") &&
                method.signature().asString().equals("()V") &&
                !method.isStatic());
    }
*/

    /**
     * Check to see if this method is an persistent finalize.
     */
//@olsen: disabled feature
/*
    private boolean methodIsPersistentFinalize() {
        return (methodIsFinalize() &&
                ca.persistCapable());
    }
*/

    /**
     * Check to see if this is a special case that should not be
     * annotated.
     */
    private boolean avoidAnnotation() {
        //@olsen: cosmetics

//@olsen: allow for annotating initializers+finalizers
/*
        final String methodName = method.name().asString();
        final String methodSig = method.signature().asString();

        if (methodName.equals("<clinit>") || methodIsFinalize())
            // Never annotate class initializers or finalizers
            return true;
*/

//^olsen: enable feature, rather use HashMap for lookup
//@olsen: disabled feature
/*
        if (ca.persistCapable()) {
            if ((methodName.equals("initializeContents") &&
                 methodSig.equals("(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V")) ||
                (methodName.equals("flushContents") &&
                 methodSig.equals("(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V")) ||
                (methodName.equals("clearContents") &&
                 methodSig.equals("()V")) ||
                (methodName.equals("postInitializeContents") &&
                 methodSig.equals("()V")) ||
                (methodName.equals("preFlushContents") &&
                 methodSig.equals("()V")) ||
                (methodName.equals("preClearContents") &&
                 methodSig.equals("()V")) ||
                (methodName.equals("jdoGetStateManager") &&
                 methodSig.equals("()Lcom/sun/forte4j/persistence/internal/StateManager;")) ||
                (methodName.equals("jdoSetStateManager") &&
                 methodSig.equals("(Lcom/sun/forte4j/persistence/internal/StateManager;)V")) ||
                (methodName.equals("jdoGetFlags") &&
                 methodSig.equals("()B")) ||
                (methodName.equals("jdoSetFlags") &&
                 methodSig.equals("(B)V")))
                // This is one of the special persistence actions.
                // Don't annotate it
                return true;
        }
*/

        return false;
    }

// ---------------------------------------------------------------------------

    /**
     * Check the code attribute for possible annotations
     */
    //^olsen: move code to inner class ?!
    void checkCode(CodeAttribute codeAttr) {
        //@olsen: cosmetics
        Insn firstInsn = codeAttr.theCode();

        // mark branch targets so we can distinguish them from
        // targets which exist for the benefit of line numbers,
        // local variables, etc.
        for (Insn markInsn = firstInsn;
             markInsn != null;
             markInsn = markInsn.next()) {
            markInsn.markTargets();
        }

        int allFlags = 0;
//@olsen: disabled feature
/*
        boolean branchesSeen = false;
*/

        for (Insn insn = firstInsn; insn != null; insn = insn.next() ) {
            InsnNote note = null;

            switch(insn.opcode()) {
//@olsen: disabled feature
/*
            case opc_invokestatic:
            case opc_invokespecial:
            case opc_invokevirtual:
            case opc_invokeinterface:
                note = noteInvokeAnnotation(insn);
                break;
*/
            case opc_getfield:
                note = noteGetFieldAnnotation(insn);
                break;
            case opc_putfield:
                note = notePutFieldAnnotation(insn);
                break;
//@olsen: disabled feature
/*
            case opc_aaload:
            case opc_baload:
            case opc_caload:
            case opc_saload:
            case opc_iaload:
            case opc_laload:
            case opc_faload:
            case opc_daload:
                note = noteArrayLoadAnnotation(insn);
                break;
            case opc_aastore:
            case opc_bastore:
            case opc_castore:
            case opc_sastore:
            case opc_iastore:
            case opc_lastore:
            case opc_fastore:
            case opc_dastore:
                note = noteArrayStoreAnnotation(insn);
                break;
*/
            default:
                break;
            }

            if (note != null) {
                addNoteList(note);

                //@olsen: ensured to use single note only (as instantiated)
                affirm((note.next() == null),
                       "Number of annotation notes for instruction > 1.");//NOI18N
                allFlags |= note.insnFlags;

//@olsen: ensured to use single note only (as instantiated)
/*
                for (InsnNote aNote = note;
                     aNote != null;
                     aNote = aNote.next()) {
//@olsen: disabled feature
///
                    if (branchesSeen == false)
                        aNote.insnFlags |= Unconditional;
///

//@olsen: disabled feature
///
                    if (largestLoop != null && largestLoop.contains(insn))
                        aNote.insnFlags |= InLoop;
///

//@olsen: disabled feature
///
                    // annotating based on thisOptimization will be done later
                    if (aNote.dirtyThis() && aNote.unconditional())
                        dirtyThis = true;
                    else if (aNote.dirtyThis() || aNote.fetchThis())
                        fetchThis = true;
///

                    allFlags |= aNote.insnFlags;
                }
*/
            }

//@olsen: disabled feature
/*
            if (insn.branches())
                branchesSeen = true;
*/
        }

//@olsen: disabled feature
/*
        if (methodIsInitializer()) {
            // An inititalizer -  either force the fetchThis, dirtyThis flags
            // on or off.
            if (env.doInitializerOptimization()) {
                // turn on the fetchThis, dirtyThis flags to inhibit fetches
                // and stores of this if enabled.  We won't really insert the
                // fetch/dirty, since it isn't needed.
                fetchThis = true;
                dirtyThis = true;
            } else {
                // Make sure that the fetchThis, dirtyThis flags are turned off
                fetchThis = false;
                dirtyThis = false;
            }
        }
*/

        //^olsen: prepare for inheritance on PC classes
        //@olsen: check for annotating of clone()
        final String methodName = method.name().asString();
        final String methodSig = method.signature().asString();
        //^olsen: annotate persistence-capable root classes only
        //        until the JDO spec precisely defines how to treat
        //        user-defined clone methods in transient classes
        final boolean implementsPersistence = ca.getImplementsPersistence();
        if (false) {
            System.out.println("    check for annotating clone()");//NOI18N
            System.out.println("    methodName = " + methodName);//NOI18N
            System.out.println("    methodSig = " + methodSig);//NOI18N
            System.out.println("    implementsPersistence = "//NOI18N
                               + implementsPersistence);
        }
        if (methodName.equals("clone")//NOI18N
            && methodSig.equals("()Ljava/lang/Object;")//NOI18N
            && implementsPersistence) {
            //^olsen: rather scan for 'invokespecial clone()Ljava/lang/Object;'
            //        in instruction loop above
            allFlags |= SuperClone;
        }

//@olsen: disabled feature
/*
        if (methodName.equals("clone") &&
            methodSig.equals("()Ljava/lang/Object;") &&
            ca.persistCapable()) {
            allFlags |= FetchThis;
            fetchThis = true;
        }
*/
        annotate = allFlags;
    }


    /**
     * Make note of annotations if needed for a method invocation instruction.
     */
//@olsen: disabled feature
/*
    private InsnNote noteInvokeAnnotation(Insn insn) {
        int flags = 0;

        ConstBasicMemberRef methRef = (ConstBasicMemberRef)
            ((InsnConstOp)insn).value();

        InsnArgNote note = null;

        for (InvokeAnnotation invAnn
                 = InvokeAnnotation.checkInvoke(methRef, env);
             invAnn != null;
             invAnn = invAnn.next()) {
            int thisFlags = 0;
            Insn dep = findArgDepositer(insn, invAnn.whichArg());
            if (dep != null && dep.opcode() == opc_aload_0 && !method.isStatic()) {
                if ((invAnn.annotateHow() & DirtyAny) != 0)
                    thisFlags = DirtyThis;
                else
                    thisFlags = FetchThis;
            } else
                thisFlags = invAnn.annotateHow();

            InsnArgNote newNote =
                new InsnArgNote(insn, thisFlags, invAnn.whichArg(),
                                Descriptor.extractArgSig(
                                    methRef.nameAndType().signature().asString()));

            // sort in order of decreasing stack depth
            if (note == null || note.arg() < newNote.arg()) {
                newNote.nextNote = note;
                note = newNote;
            } else {
                InsnArgNote aNote = note;
                while (aNote.nextNote != null && aNote.nextNote.arg() > newNote.arg())
                    aNote = aNote.nextNote;
                newNote.nextNote = aNote.nextNote;
                aNote.nextNote = newNote;
            }
        }

        return note;
    }
*/

    /**
     * make note of annotations if needed for the getField instruction.
     */
    //^olsen: merge code with notePutFieldAnnotation() ?!
    private InsnNote noteGetFieldAnnotation(Insn insn) {
        //@olsen: cosmetics
        final InsnConstOp getFieldInsn = (InsnConstOp)insn;
        final ConstFieldRef fieldRef = (ConstFieldRef)getFieldInsn.value();
        final String fieldOf = fieldRef.className().asString();

        //@olsen: changed to use JDOMetaData
        final String fieldName
            = fieldRef.nameAndType().name().asString();
        final JDOMetaData meta = env.getJDOMetaData();
        if (!meta.isPersistentField(fieldOf, fieldName))
            return null;

//@olsen: disabled feature
/*
        final ClassControl cc = env.findClass(fieldOf);

        if (cc == null || !cc.persistCapable())
            return null;
*/

        //@olsen: added checks
        final boolean dfgField
            = meta.isDefaultFetchGroupField(fieldOf, fieldName);
        final boolean pkField
            = meta.isPrimaryKeyField(fieldOf, fieldName);
        final int fieldIndex
            = meta.getFieldNo(fieldOf, fieldName);
        final String targetPCRootClass
            = meta.getPersistenceCapableRootClass(fieldOf);

        int flags = 0;
        //@olsen: added variables
        final String fieldSig = fieldRef.nameAndType().signature().asString();
        // there's no field value on the stack yet
        final int stackArgSize = 0;

        //@olsen: added println() for debugging
        if (false) {
            System.out.println("    get field "//NOI18N
                               + fieldOf + "." + fieldName//NOI18N
                               + "[" + fieldIndex + "]"//NOI18N
                               + "<" + fieldSig + ">"//NOI18N
                               + " : p"//NOI18N
                               + (dfgField ? ",dfg" : ",!dfg")//NOI18N
                               + (pkField ? ",pk" : ",!pk")//NOI18N
                               + ";");//NOI18N
        }

        Insn dep = findArgDepositer(insn, stackArgSize);
        if (dep != null
            && dep.opcode() == opc_aload_0
            && !method.isStatic())
            // This represents a fetch of "this"
            flags |= FetchThis;
        else
            flags |= FetchPersistent;

        //@olsen: added test
        if (dfgField)
            flags |= DFGField;

        //@olsen: added test
        if (pkField)
            flags |= PKField;

        //@olsen: changed to use JDOMetaData
        return new InsnNote(insn, flags,
                            stackArgSize,
                            fieldSig, fieldOf, fieldName, fieldIndex,
                            targetPCRootClass);
//@olsen: disabled feature
/*
        return new InsnNote(insn, flags, 0, "", cc.action());
*/
    }

    /**
     * Generate annotations if needed for the putField instruction.
     */
    //^olsen: merge code with noteGetFieldAnnotation() ?!
    private InsnNote notePutFieldAnnotation(Insn insn) {
        //@olsen: cosmetics
        final InsnConstOp putFieldInsn = (InsnConstOp)insn;
        final ConstFieldRef fieldRef = (ConstFieldRef)putFieldInsn.value();
        final String fieldOf = fieldRef.className().asString();

        //@olsen: changed to use JDOMetaData
        final String fieldName
            = fieldRef.nameAndType().name().asString();
        final JDOMetaData meta = env.getJDOMetaData();
        if (!meta.isPersistentField(fieldOf, fieldName))
            return null;

//@olsen: disabled feature
/*
        final ClassControl cc = env.findClass(fieldOf);

        if (cc == null || !cc.persistCapable())
            return null;
*/

        //@olsen: added checks
        final boolean dfgField
            = meta.isDefaultFetchGroupField(fieldOf, fieldName);
        final boolean pkField
            = meta.isPrimaryKeyField(fieldOf, fieldName);
        final int fieldIndex
            = meta.getFieldNo(fieldOf, fieldName);
        final String targetPCRootClass
            = meta.getPersistenceCapableRootClass(fieldOf);

        int flags = 0;
        //@olsen: added variables
        final String fieldSig = fieldRef.nameAndType().signature().asString();
        // size of field value on the stack
        final int stackArgSize
            = (fieldSig.equals("J") || fieldSig.equals("D")) ? 2 : 1;//NOI18N

        //@olsen: added println() for debugging
        if (false) {
            System.out.println("    put field "//NOI18N
                               + fieldOf + "." + fieldName//NOI18N
                               + "[" + fieldIndex + "]"//NOI18N
                               + "<" + fieldSig + ">"//NOI18N
                               + " : p"//NOI18N
                               + (dfgField ? ",dfg" : ",!dfg")//NOI18N
                               + (pkField ? ",pk" : ",!pk")//NOI18N
                               + ";");//NOI18N
        }

        Insn dep = findArgDepositer(insn, stackArgSize);
        if (dep != null
            && dep.opcode() == opc_aload_0
            && !method.isStatic())
            // This represents a dirtyfication of "this"
            flags |= DirtyThis;
        else
            flags |= DirtyPersistent;

        //@olsen: added test
        if (dfgField)
            flags |= DFGField;

        //@olsen: added test
        if (pkField)
            flags |= PKField;

        //@olsen: changed to use JDOMetaData
        return new InsnNote(insn, flags,
                            stackArgSize,
                            fieldSig, fieldOf, fieldName, fieldIndex,
                            targetPCRootClass);
//@olsen: disabled feature
/*
        return new InsnNote(insn, flags, stackArgSize, fieldSig, cc.action());
*/
    }

    /**
     * Generate annotations if needed for the arrayLoad instruction.
     */
//@olsen: disabled feature
/*
    private InsnNote noteArrayLoadAnnotation(Insn insn) {
        int arrayFetchType = 0;
        switch(insn.opcode()) {
        case opc_aaload:
            arrayFetchType =  ArrayTypeObject;
            break;
        case opc_caload:
            arrayFetchType =  ArrayTypeChar;
            break;
        case opc_saload:
            arrayFetchType =  ArrayTypeShort;
            break;
        case opc_iaload:
            arrayFetchType =  ArrayTypeInt;
            break;
        case opc_laload:
            arrayFetchType =  ArrayTypeLong;
            break;
        case opc_faload:
            arrayFetchType =  ArrayTypeFloat;
            break;
        case opc_daload:
            arrayFetchType =  ArrayTypeDouble;
            break;
        case opc_baload:
            // Unfortunately, both byte arrays and boolean arrays are accessed
            // using the same instruction so don't attempt to infer the array
            // element type for these.
            break;
        }
        return new InsnNote(insn, FetchArray | arrayFetchType, 1, "I", null);
    }
*/

//@olsen: disabled feature
/*
    private InsnNote noteArrayStoreAnnotation(Insn insn) {
        int valueType = Insn.loadStoreDataType(insn.opcode());
        int valueSize = Descriptor.elementSize(valueType);
        String stackSig = "I" + Descriptor.elementSig(valueType);

        // Compute the array store type for completeness.  The generated
        // annotation currently doesn't use this information because there
        // are no array element type-specific overloads of dirty() but
        // perhaps we'll add them at some point.
        int arrayStoreType = 0;
        switch(insn.opcode()) {
        case opc_aastore:
            arrayStoreType =  ArrayTypeObject;
            break;
        case opc_castore:
            arrayStoreType =  ArrayTypeChar;
            break;
        case opc_sastore:
            arrayStoreType =  ArrayTypeShort;
            break;
        case opc_iastore:
            arrayStoreType =  ArrayTypeInt;
            break;
        case opc_lastore:
            arrayStoreType =  ArrayTypeLong;
            break;
        case opc_fastore:
            arrayStoreType =  ArrayTypeFloat;
            break;
        case opc_dastore:
            arrayStoreType =  ArrayTypeDouble;
            break;
        case opc_bastore:
            // Unfortunately, both byte arrays and boolean arrays are accessed
            // using the same instruction so don't attempt to infer the array
            // element type for these.
            break;
        }

        return new InsnNote(insn, DirtyArray | arrayStoreType,
                            valueSize+1, stackSig, null);
    }
*/

// ---------------------------------------------------------------------------

    /**
     * Annotate the class method.  For now, brute force rules.
     */
    void annotateMethod() {
        //@olsen: cosmetics
        final CodeAttribute codeAttr = method.codeAttribute();
        if (codeAttr == null || !needsAnnotation())
            return;

//@olsen: disabled feature
/*
        if ((annotate & MakeThisTransient) != 0) {
            makeThisTransient(codeAttr);
            if (annotate == MakeThisTransient)
                return;
        }
*/

        //@olsen: added printing output
        env.message(
            "annotating method " + ca.userClassName()//NOI18N
            + "." + method.name().asString()//NOI18N
            + Descriptor.userMethodArgs(method.signature().asString()));

//@olsen: disabled feature
/*
        clearThisAnnotation();
        removeRedundantThisAnnotation();
*/

        Insn firstInsn = codeAttr.theCode();

        // First instruction is a target
        Insn insn = firstInsn.next();

        while (insn != null) {
            switch(insn.opcode()) {
//@olsen: disabled feature
/*
            case opc_invokestatic:
            case opc_invokespecial:
            case opc_invokevirtual:
            case opc_invokeinterface:
*/
            case opc_getfield:
            case opc_putfield:
//@olsen: disabled feature
/*
            case opc_aaload:
            case opc_baload:
            case opc_caload:
            case opc_saload:
            case opc_iaload:
            case opc_laload:
            case opc_faload:
            case opc_daload:
            case opc_aastore:
            case opc_bastore:
            case opc_castore:
            case opc_sastore:
            case opc_iastore:
            case opc_lastore:
            case opc_fastore:
            case opc_dastore:
*/
                insn = insnAnnotation(insn);
                break;
            default:
                break;
            }

            insn = insn.next();
        }

        //@olsen: do special annotation if detected super.clone()
        if ((annotate & SuperClone) != 0) {
            final String superName = ca.classFile().superName().asString();
            annotateClone(codeAttr, superName);
        }

//@olsen: disabled feature
/*
        if (methodIsInitializer()) {
        } else {
            // Pre- fetch/dirty this if needed
            if (fetchThis || dirtyThis) {
                // Optimize a fetch(this) or dirty(this) to the start of
                // the method.
                // For fetch calls this is:
                // if (jdoFlags < 0)
                // Implementation.fetch(this);
                //
                // For dirty calls this is:
                // if ((jdoFlags&PersistenceCapable.writeBarrierSet) != 0)
                // Implementation.dirty(this);

                Insn newInsn = Insn.create(opc_aload_0);
                Insn annotationStart = newInsn;
                InsnTarget afterCondition = null;

//@olsen: disabled feature
///
                if (ca.getFlagsMemberValid() &&
                    ca.getFlagsMember() != null) {
///

                    //@olsen: changed to use JDOMetaData
                    final String className = ca.className();
                    final String pcRootClass
                        = env.getJDOMetaData().getPersistenceCapableRootClass(className);
                    newInsn = newInsn.append(
                        Insn.create(opc_getfield,
                                    pool.addFieldRef(
                                        pcRootClass,
                                        JDOMetaData.JDOFlagsFieldName,
                                        JDOMetaData.JDOFlagsFieldSig)));
//@olsen: disabled feature
///
                    ClassControl flagsCC
                        = ca.getFlagsMemberClassControl();
                    newInsn = newInsn.append(
                        Insn.create(opc_getfield,
                                    pool.addFieldRef(
                                        flagsCC.className(),
                                        ca.getFlagsMember(),
                                        "B")));
///

                    afterCondition = new InsnTarget();
                    if (dirtyThis) {
                        newInsn = newInsn.append(Insn.create(opc_iconst_2));
                        newInsn = newInsn.append(Insn.create(opc_iand));
                        newInsn = newInsn.append(Insn.create(opc_ifeq, afterCondition));
                        newInsn = newInsn.append(Insn.create(opc_aload_0));
                    } else {
                        newInsn = newInsn.append(Insn.create(opc_ifge, afterCondition));
                        newInsn = newInsn.append(Insn.create(opc_aload_0));
                    }
//@olsen: disabled feature
///
                }
///

                newInsn = newInsn.append(
                    Insn.create(
                        opc_invokestatic,
                        pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation",
                                          (dirtyThis ? "dirty" : "fetch"),
                                          "(" + JDOMetaData.JDOPersistenceCapableSig + ")V")));

                if (afterCondition != null)
                    newInsn = newInsn.append(afterCondition);
                firstInsn.insert(annotationStart);
                noteStack(2);
            }

//@olsen: disabled feature
///
            if (methodName.equals("clone") &&
                methodSig.equals("()Ljava/lang/Object;") &&
                !ca.getNeedsClone()) {
                annotateClone(codeAttr, superName);
            }
///
//@olsen: disabled feature
        }
*/

//@olsen: disabled feature
/*
        //^olsen: caches -> int[] ?
        if (caches != null && caches.size() > 0) {
            // Generate fetch/dirty cache initializers
            Insn initInsn = null;
            //^olsen: optimize traversal ?
            for (int i = 0; i < caches.size(); i++) {
                int slot = ((Integer) caches.elementAt(i)).intValue();
                Insn nullInsn = Insn.create(opc_aconst_null);
                if (initInsn == null)
                    initInsn = nullInsn;
                else
                    initInsn.append(nullInsn);
                initInsn.append(InsnUtils.aStore(slot, pool));
            }

            // These initializations must not occur in an
            // exception handler or the code may fail to verify.  If an
            // exception handler starts at offset 0, our initializations
            // will fall into the exception handler block.  For
            // simplicity, just add a new target as the initial
            // instruction - it doesn't cost anything in the
            // generated code.
            InsnTarget newFirstInsn = new InsnTarget();
            initInsn.append(firstInsn);
            newFirstInsn.append(initInsn);
            firstInsn = newFirstInsn;
            codeAttr.setTheCode(firstInsn);
        }
*/

        if (annotationStack > 0)
            codeAttr.setStackUsed(codeAttr.stackUsed() + annotationStack);
    }

// ---------------------------------------------------------------------------

    /**
     * If dirtyThis or fetchThis is set, remove flags indicating the need to
     * fetch or dirth "this" on individual instructions.
     */
//@olsen: disabled feature
/*
    private void clearThisAnnotation() {
        // If the user has disabled "this" optimization, simply turn the
        // dirtyThis and fetchThis flags off unless this is an initializer
        // method, in which case we defer reseting the flags until the end
        // of this method.
        if (!env.doThisOptimization() && !methodIsInitializer()) {
            dirtyThis = false;
            fetchThis = false;
        }

        if (!dirtyThis && !fetchThis)
            return;

        final CodeAttribute codeAttr = method.codeAttribute();
        if (codeAttr != null) {
            for (Insn insn = codeAttr.theCode();
                 insn != null;
                 insn = insn.next()) {
                for (InsnNote note = getNoteList(insn);
                     note != null;
                     note = note.next()) {

                    if (dirtyThis && note.dirtyThis())
                        note.dontDirtyThis();
                    if ((dirtyThis || fetchThis) && note.fetchThis())
                        note.dontFetchThis();
                }
            }
        }

        if (methodIsInitializer()) {
            dirtyThis = false;
            fetchThis = false;
        }
    }
*/

    /**
     * Optimize out obviously redundant fetch(this) and dirty(this)
     * annotations.  These are repeated fetches and dirties which occur
     * in straight-line code with no intervening branch targets or
     * method calls.
     */
//@olsen: disabled feature
/*
    private void removeRedundantThisAnnotation() {
        // This optimization doesn't apply to static methods or initializers.
        // Static methods are ignored because they don't have a "this" and
        // initializers may be excluded if we expect that there are no
        // fetch/dirty of "this".
        if (method.isStatic() ||
            (methodIsInitializer() && env.doInitializerOptimization()))
            return;

        CodeAttribute codeAttr = method.codeAttribute();
        if (codeAttr != null && needsAnnotation()) {
            Insn firstInsn = codeAttr.theCode();

            // First instruction is a target
            Insn insn = firstInsn.next();

            boolean thisFetched = false;
            boolean thisDirtied = false;

            while (insn != null) {

                for (InsnNote note = getNoteList(insn);
                     note != null;
                     note = note.next()) {

                    if (note.fetchThis()) {
                        if (thisFetched)
                            note.dontFetchThis();
                        else
                            thisFetched = true;
                    }
                    if (note.dirtyThis()) {
                        if (thisDirtied)
                            note.dontDirtyThis();
                        else {
                            thisDirtied = true;
                            thisFetched = true;
                        }
                    }
                }

                boolean invalidate = false;
                switch(insn.opcode()) {
                case opc_jsr:
                case opc_invokestatic:
                case opc_invokespecial:
                case opc_invokevirtual:
                case opc_invokeinterface:
                    invalidate = true;
                    break;

                case opc_monitorenter:
                    // If the code is explicitly synchronizing then the user
                    // might have some reason to expect instructions to
                    // interleave against another thread execution in a
                    // particular order, so invalidate any assumption about
                    // the fetch/dirty flags
                    invalidate = true;
                    break;

                case Insn.opc_target:
                    // targets which result from line-number info, etc. do not
                    // invalidate the optimization
                    if (((InsnTarget)insn).isBranchTarget())
                        invalidate = true;
                    break;

                default:
                    break;
                }

                if (invalidate) {
                    thisFetched = false;
                    thisDirtied = false;
                }

                insn = insn.next();
            }
        }
    }
*/

// ---------------------------------------------------------------------------

    //^olsen: extend for full support of inheritance on PC classes
    //@olsen: reimplemented this method
    private void annotateClone(CodeAttribute codeAttr,
                               String superName) {
        if (false) {
            final String methodName = method.name().asString();
            final String methodSig = method.signature().asString();
            System.out.println("annotateClone()");//NOI18N
            System.out.println("    methodName = " + methodName);//NOI18N
            System.out.println("    methodSig = " + methodSig);//NOI18N
            System.out.println("    superName = " + superName);//NOI18N
        }

        Insn insn;
        for (insn = codeAttr.theCode();
             insn != null;
             insn = insn.next()) {

            // Found the clone method.  See if it is the flavor of clone()
            // which does a super.clone() call, and if it is, add
            // field initializations for the jdoStateManager and jdoFlags
            // fields.
            if (insn.opcode() != opc_invokespecial)
                continue;

            final InsnConstOp invoke = (InsnConstOp)insn;
            final ConstMethodRef methodRef = (ConstMethodRef)invoke.value();
            final ConstNameAndType methodNT = methodRef.nameAndType();
            final String methodName = methodNT.name().asString();
            final String methodSig = methodNT.signature().asString();

            if (!(methodName.equals("clone")//NOI18N
                  && methodSig.equals("()Ljava/lang/Object;")))//NOI18N
                continue;

            if (false) {
                final ConstClass methodClass = methodRef.className();
                final String methodClassName = methodClass.asString();
                System.out.println("        found invocation of: "//NOI18N
                                   + methodClassName
                                   + "." + methodName + methodSig);//NOI18N
            }

            // check whether next instruction already is a downcast to a
            // class implementing PersistenceCapable
            final String thisClass = ca.className();
            final Insn checkCastInsn = insn.next();
            final boolean needCheckcast;
            if (checkCastInsn.opcode() != opc_checkcast) {
                needCheckcast = true;
            } else {
                ConstClass target =
                    (ConstClass) ((InsnConstOp) checkCastInsn).value();
                if (target.asString().equals(thisClass)) {
                    insn = checkCastInsn;
                    needCheckcast = false;
                } else {
                    needCheckcast = true;
                }
            }

            // clear jdo fields of clone
            {
                // duplicate downcastet reference
                final Insn newInsn = Insn.create(opc_dup);
                if (needCheckcast) {
                    newInsn.append(Insn.create(opc_checkcast,
                                               pool.addClass(thisClass)));
                }
                newInsn.append(Insn.create(opc_dup));

                // clear jdo fields
                newInsn.append(Insn.create(opc_aconst_null));
                newInsn.append(Insn.create(
                    opc_putfield,
                    pool.addFieldRef(thisClass,
                                     JDOMetaData.JDOStateManagerFieldName,
                                     JDOMetaData.JDOStateManagerFieldSig)));
                newInsn.append(Insn.create(opc_iconst_0));
                newInsn.append(Insn.create(
                    opc_putfield,
                    pool.addFieldRef(thisClass,
                                     JDOMetaData.JDOFlagsFieldName,
                                     JDOMetaData.JDOFlagsFieldSig)));

                // insert code
                insn.insert(newInsn);
                noteStack(3);
            }
        }
    }

//@olsen: disabled feature
/*
    private void annotateClone(CodeAttribute codeAttr,
                               String superName) {
        Insn insn;
        for (insn = codeAttr.theCode();
             insn != null;
             insn = insn.next()) {

            // Found the clone method.  See if it is the flavor of clone()
            // which does a super.clone() call, and if it is, add
            // field initializations for the jdoStateManager and jdoFlags
            // fields.
            if (insn.opcode() == opc_invokespecial) {
                InsnConstOp invoke = (InsnConstOp) insn;
                ConstMethodRef methRef = (ConstMethodRef) invoke.value();
                String methName = methRef.nameAndType().name().asString();
                if (methName.equals("clone")) {
                    String thisClass = ca.className();

//@olsen: disabled feature
///
                    //      The following change to the method ref is a
                    //      workaround for the Sun JIT.  If there is a
                    //      derived class whose clone() method calls
                    //      super.clone() where the super class's clone()
                    //      was constructed by osjcfp,
                    //      the compiler will generate code which
                    //      calls java.lang.Object.clone() instead of the
                    //      base class's clone (since that's all it can
                    //      see at compile time).  It also sets
                    //      (correctly) the ACC_SUPER bit.  Unfortunately,
                    //      the JDK JIT will not call the inserted clone
                    //      method, but instead calls the
                    //      java.lang.Object.clone() method.  The hackery
                    //      below modifies the invokespecial super.clone()
                    //      call to instead use the osjcfp supplied clone().
                    //      It should be removed if/when the JIT
                    //      is fixed. -cwl 6/27/97

                    ClassControl cc =
                        env.findClass(superName).findMethodClass(
                            "clone", "()Ljava/lang/Object;");
                    if (cc != null &&
                        !cc.className().equals(methRef.className().asString())) {
                        env.message("Changing " + thisClass + ".clone() to call " +
                                    cc.className() + ".clone() instead of " +
                                    methRef.className().asString() + ".clone()");
                        ConstMethodRef newMethRef =
                            pool.addMethodRef(cc.className(),
                                              "clone",
                                              "()Ljava/lang/Object;");
                        invoke.setValue(newMethRef);
                    }
///

//@olsen: disabled feature
///
                    boolean needCheckcast = false;
                    Insn checkCastInsn = insn.next();
                    if (checkCastInsn.opcode() != opc_checkcast)
                        needCheckcast = true;
                    else {
                        ConstClass target =
                            (ConstClass) ((InsnConstOp) checkCastInsn).value();
                        ClassControl targetCC = env.findClass(target.asString());
                        if (targetCC != null && !targetCC.inherits(thisClass))
                            needCheckcast = true;
                        else
                            insn = checkCastInsn;
                    }

                    boolean checkStack = false;
                    if (ca.getNeedsJDOStateManagerMethods()) {
                        Insn newInsn = Insn.create(opc_dup);
                        if (needCheckcast)
                            newInsn.append(Insn.create(opc_checkcast,
                                                       pool.addClass(thisClass)));
                        newInsn.append(Insn.create(opc_aconst_null));
                        newInsn.append(Insn.create(
                            opc_putfield,
                            pool.addFieldRef(thisClass,
                                             ca.getRefMember(),
                                             JDOStateManagerSig)));
                        insn.insert(newInsn);
                        checkStack = true;
                    }

                    if (ca.getNeedsJDOFlagsMethods()) {
                        Insn newInsn = Insn.create(opc_dup);
                        if (needCheckcast)
                            newInsn.append(Insn.create(opc_checkcast,
                                                       pool.addClass(thisClass)));
                        newInsn.append(Insn.create(opc_iconst_0));
                        newInsn.append(Insn.create(opc_putfield,
                                                   pool.addFieldRef(thisClass,
                                                                    ca.getFlagsMember(),
                                                                    "B")));

                        insn.insert(newInsn);
                        checkStack = true;
                    }

                    if (checkStack)
                        noteStack(2);
///
                }
            }
        }
    }
*/

// ---------------------------------------------------------------------------

    /**
     * For a non-static method of a class which implements PersistenceCapable,
     * convert the object to a transient object by setting the stateManager
     * to null and the object flags to 0.  It is assumed that
     * the ObjectTable will have already been cleaned up by the
     * garbage collector.
     */
//@olsen: disabled feature
/*
    private void makeThisTransient(CodeAttribute codeAttr) {

        Insn insn = codeAttr.theCode();
        while (insn.opcode() == Insn.opc_target)
            insn = insn.next();

        // Set the statemanager to null
        Insn annotation = Insn.create(opc_aload_0);
        annotation.append(Insn.create(opc_aconst_null));
        ConstInterfaceMethodRef methRef =
            pool.addInterfaceMethodRef(JDOPersistenceCapablePath,
                                       "jdoSetStateManager",
                                       "(Lcom/sun/forte4j/persistence/internal/StateManager;)V");
        annotation.append(new InsnInterfaceInvoke(methRef, 2));

        // Set the object flags to null
        annotation.append(Insn.create(opc_aload_0));
        annotation.append(Insn.create(opc_iconst_0));
        methRef = pool.addInterfaceMethodRef(
            JDOPersistenceCapablePath,
            "jdoSetFlags",
            "(B)V");
        annotation.append(new InsnInterfaceInvoke(methRef, 2));

        insn.prev().insert(annotation);

        if (codeAttr.stackUsed() < 2)
            codeAttr.setStackUsed(2);
    }
*/

// ---------------------------------------------------------------------------

    /**
     * Generate annotations if needed for the instruction.
     */
    private Insn insnAnnotation(final Insn insn) {
        // The note list should be sorted in order of decreasing arg depth

        int initialSingleRegs = 0;

//@olsen: ensured to use single note only (as instantiated)
/*
        for (InsnNote note = getNoteList(insn);
             note != null;
             note = note.next()) { ... }
*/
        InsnNote note = getNoteList(insn);
        if (note == null)
            return insn;

        //@olsen: ensured to use single note only (as instantiated)
        affirm(insn == note.insn);
        affirm((note.next() == null),
               "Number of annotation notes for instruction > 1.");//NOI18N

        //@olsen: not needed to ensure: note.dirtyThis() && !method.isStatic()
        final boolean fetch = (note.fetchPersistent() || note.fetchThis());
        final boolean dirty = (note.dirtyPersistent() || note.dirtyThis());
        //@olsen: added consistency check
        affirm((fetch ^ dirty),
               "Inconsistent fetch/dirty flags.");//NOI18N

        //@olsen: added checks
        final boolean dfgField = note.dfgFieldAccess();
        final boolean pkField = note.pkFieldAccess();

        //@olsen: added println() for debugging
        if (false) {
            final String targetClassName = note.targetClassName;
            final String targetFieldName = note.targetFieldName;
            //final String targetPCRootClass = note.targetPCRootClass;

            System.out.println("    build annotation: "//NOI18N
                               + targetClassName
                               + "." + targetFieldName + " : "//NOI18N
                               + (pkField ? "pk," : "!pk,")//NOI18N
                               + (dfgField ? "dfg," : "!dfg,")//NOI18N
                               + (fetch ? "fetch " : "dirty ")//NOI18N
                               + (note.fetchPersistent()
                                  ? "persistent" : "this")//NOI18N
                               + ";");//NOI18N
        }

        //@olsen: improved control flow
        //@olsen: 4385427: do not enhance PK read access at all
        if (pkField && fetch) {
            return insn;
        }

        //@olsen: enhance for mediated access
        //@olsen: enhance PK write as mediated access
        //@olsen: added: mediated getfield/putfield insn annotation
        if (pkField || !dfgField) {
            //insn.prev().insert(Insn.create(opc_nop));
            //@olsen: 4429769: drop putfield instruction on mediated write
            //        access; isolate the get/putfield instruction to allow
            //        to now be inserted by buildAccessAnnotation() itself
            final Insn prev = insn.prev();
            insn.remove();

            //@olsen: changed not to return null
            final AnnotationFragment frag1 = buildAccessAnnotation(note);
            affirm(frag1, "Illegal annotation of PK or non-dfg field.");//NOI18N

            //@olsen: 4429769, replace current instruction with fragment
            //insn.prev().insert(frag1.annotation);
            //noteStack(frag1.stackRequired - note.arg());
            //return insn;
            final Insn last = prev.insert(frag1.annotation);
            noteStack(frag1.stackRequired - note.arg());
            return last;
        }

        // do basic annotation
        //@olsen: enhance for non-mediated access
        final AnnotationFragment frag0 = buildBasicAnnotation(note);
        //@olsen: changed not to return null
        affirm(frag0, "Illegal annotation of dfg field.");//NOI18N
        //if (frag0 != null) {
        {
            // Attempt to find an instruction where the argument is known
            // to be on the top of stack
            StackState state
                = new StackState(note.arg(), note.sig(), insn.prev());
            minimizeStack(state);

            if (false) {
                System.out.println("        state.argDepth =  "//NOI18N
                                   + state.argDepth);
                System.out.print("        state.insn = ");//NOI18N
                state.insn.printInsn(System.out);
                System.out.print("        insn = ");//NOI18N
                insn.printInsn(System.out);
            }

            // generate the necessary instructions
            Insn annotation = null;
            if (state.argDepth == 0) {
                // The value is on top of the stack - the dup in the basic
                // annotation fragment will suffice
                annotation = frag0.annotation;
                noteStack(frag0.stackRequired);
            } else if (state.argDepth == 1) {
                // The value on top of the stack is one deep.  Because the
                // operand of interest is also a single word value we can
                // simply execute a swap operation to get access to the
                // operand on top of the stack
                annotation = Insn.create(opc_swap);
                annotation.append(frag0.annotation);
                annotation.append(Insn.create(opc_swap));

                // reduce the code fragment's stack requirements by
                // the amount that minimizeStack reduced the stack depth,
                // since that is the context in which the code fragment
                // will run.
                noteStack(frag0.stackRequired - (note.arg()-1));
            }  else {
                // The value is hidden by 2 or more stack operands.  Move
                // the obscuring values into temporaries to get access to
                // the value - put them back when done
                Stack stackTypes = state.stackTypes;
                int depth = state.argDepth;
                int elem = stackTypes.size()-1;

                int singleRegs = initialSingleRegs;
                int doubleRegs = 0;
                int regnums[] = new int[depth];
                int regtotal = 0;

                // Now, move values into temp registers
                while (depth > 0) {
                    int elemType =
                        ((Integer)stackTypes.elementAt(elem--)).intValue();
                    int elemSize = Descriptor.elementSize(elemType);
                    depth -= elemSize;
                    int reg = ((elemSize == 1)
                               ? tmpReg(singleRegs++)
                               : tmpReg2(doubleRegs++));
                    regnums[regtotal++] = reg;

                    Insn store = InsnUtils.store(elemType, reg, pool);
                    if (annotation == null)
                        annotation = store;
                    else
                        annotation.append(store);
                }
                affirm((depth >= 0),
                       "Stack underflow while computing save registers");//NOI18N

                annotation.append(frag0.annotation);

                while (regtotal > 0)
                    annotation.append(InsnUtils.load(
                        ((Integer)stackTypes.elementAt(++elem)).intValue(),
                        regnums[--regtotal], pool));

                noteStack(frag0.stackRequired - note.arg());
            }

            state.insn.insert(annotation);
        }

        return insn;
    }

    //@olsen: added method for direct annotation of put/getfield
    //@olsen: must not return null
    private AnnotationFragment buildAccessAnnotation(final InsnNote note) {
        final int requiredStack;
        final Insn annotation;

        final String targetClassName = note.targetClassName;
        final String targetFieldName = note.targetFieldName;
        final String targetPCRootClass = note.targetPCRootClass;

        //@olsen: not needed to ensure: note.dirtyThis() && !method.isStatic()
        final boolean fetch = (note.fetchPersistent() || note.fetchThis());
        final boolean dirty = (note.dirtyPersistent() || note.dirtyThis());
        //@olsen: added consistency check
        affirm((fetch ^ dirty),
               "Inconsistent fetch/dirty flags.");//NOI18N

        //@olsen: added println() for debugging
        if (false) {
            final boolean dfgField = note.dfgFieldAccess();
            final boolean pkField = note.pkFieldAccess();

            System.out.println("    build access annotation: "//NOI18N
                               + targetClassName
                               + "." + targetFieldName + " : "//NOI18N
                               + (pkField ? "pk," : "!pk,")//NOI18N
                               + (dfgField ? "dfg," : "!dfg,")//NOI18N
                               + (fetch ? "fetch " : "dirty ")//NOI18N
                               + (note.fetchPersistent()
                                  ? "persistent" : "this")//NOI18N
                               + ";");//NOI18N
        }

        final int argSize = note.arg();
        final String fieldSig = note.sig();
        final int fieldType = Descriptor.elementType(fieldSig);
        final int fieldIndex = note.targetFieldIndex;
        if (false) {
            System.out.println("        argSize = " + argSize);//NOI18N
            System.out.println("        fieldSig = " + fieldSig);//NOI18N
            System.out.println("        fieldType = " + fieldType);//NOI18N
            System.out.println("        fieldIndex = " + fieldIndex);//NOI18N
        }

        if (fetch) {
            // get jdoStateManager
            Insn insn = annotation = Insn.create(opc_dup);
            insn = insn.append(
                Insn.create(opc_getfield,
                            pool.addFieldRef(
                                targetPCRootClass,
                                JDOMetaData.JDOStateManagerFieldName,
                                JDOMetaData.JDOStateManagerFieldSig)));

            // test jdoStateManager
            // load/dirty field if nonnull
            InsnTarget fetchDirty = new InsnTarget();
            InsnTarget afterFetchDirty = new InsnTarget();
            insn = insn.append(Insn.create(opc_dup));
            insn = insn.append(
                Insn.create(opc_ifnonnull, fetchDirty));

            // pop jdoStateManager and skip loading/dirtying
            insn = insn.append(Insn.create(opc_pop));
            insn = insn.append(
                Insn.create(opc_goto, afterFetchDirty));

            // invoke StateManager's fetch method
            insn = insn.append(fetchDirty);

            // push field's unique index onto stack (1st arg)
            insn = insn.append(InsnUtils.integerConstant(fieldIndex, pool));

            // call stateManager's void prepareGetField(int fieldID) method
            requiredStack = 2;
            insn = insn.append(
                new InsnInterfaceInvoke(
                    pool.addInterfaceMethodRef(
                        JDOMetaData.JDOStateManagerPath,
                        "prepareGetField",//NOI18N
                        "(I)V"),//NOI18N
                    requiredStack));

            insn = insn.append(afterFetchDirty);
            insn = insn.append(note.insn);
        } else {
            //affirm(dirty);
            int singleRegs = 0;
            int doubleRegs = 0;

            // move current value into temp registers
            affirm(argSize > 0);
            final int reg = ((argSize == 1)
                             ? tmpReg(singleRegs++)
                             : tmpReg2(doubleRegs++));
            Insn insn = annotation = InsnUtils.store(fieldType, reg, pool);

            // get jdoStateManager
            insn = insn.append(Insn.create(opc_dup));
            insn = insn.append(
                Insn.create(opc_getfield,
                            pool.addFieldRef(
                                targetPCRootClass,
                                JDOMetaData.JDOStateManagerFieldName,
                                JDOMetaData.JDOStateManagerFieldSig)));

            // test jdoStateManager
            // load/dirty field if nonnull
            InsnTarget fetchDirty = new InsnTarget();
            InsnTarget afterFetchDirty = new InsnTarget();
            insn = insn.append(Insn.create(opc_dup));
            insn = insn.append(
                Insn.create(opc_ifnonnull, fetchDirty));

            // pop jdoStateManager and skip loading/dirtying
            insn = insn.append(Insn.create(opc_pop));
            // restore value from registers
            affirm(argSize > 0);
            insn = insn.append(InsnUtils.load(fieldType, reg, pool));
            //@olsen: 4429769, insert the original putfield instruction here
            insn = insn.append(note.insn);
            insn = insn.append(
                Insn.create(opc_goto, afterFetchDirty));

            // invoke StateManager's load method
            insn = insn.append(fetchDirty);

            // push field's unique index onto stack (1st arg)
            insn = insn.append(InsnUtils.integerConstant(fieldIndex, pool));

            // restore value from registers (2nd arg)
            affirm(argSize > 0);
            insn = insn.append(InsnUtils.load(fieldType, reg, pool));

            // call stateManager's set<Type>Field(index, value) method
            switch(fieldType) {
            case T_BOOLEAN:
                //boolean setBooleanField(int fieldNumber, boolean value);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setBooleanField",//NOI18N
                            "(IB)B"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            case T_CHAR:
                //char setCharField(int fieldNumber, char 3);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setCharField",//NOI18N
                            "(IC)C"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            case T_BYTE:
                //byte setByteField(int fieldNumber, byte value);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setByteField",//NOI18N
                            "(IZ)Z"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            case T_SHORT:
                //short setShortField(int fieldNumber, short value);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setShortField",//NOI18N
                            "(IS)S"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            case T_INT:
                //int setIntField(int fieldNumber, int value);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setIntField",//NOI18N
                            "(II)I"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            case T_LONG:
                //long setLongField(int fieldNumber, long value);
                requiredStack = 4;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setLongField",//NOI18N
                            "(IJ)J"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                insn = insn.append(Insn.create(opc_pop));
                break;
            case T_FLOAT:
                //float setFloatField(int fieldNumber, float value);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setFloatField",//NOI18N
                            "(IF)F"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            case T_DOUBLE:
                //double setDoubleField(int fieldNumber, double value);
                requiredStack = 4;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setDoubleField",//NOI18N
                            "(ID)D"),//NOI18N
                        requiredStack));
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                insn = insn.append(Insn.create(opc_pop));
                break;
            case TC_OBJECT:
            case TC_INTERFACE:
                //Object setObjectField(int fieldNumber, Object value);
                requiredStack = 3;
                insn = insn.append(
                    new InsnInterfaceInvoke(
                        pool.addInterfaceMethodRef(
                            JDOMetaData.JDOStateManagerPath,
                            "setObjectField",//NOI18N
                            "(ILjava/lang/Object;)Ljava/lang/Object;"),//NOI18N
                        requiredStack));

                //@olsen: 4429769, no need to downcast anymore
/*
                // add a down-cast to the field's type
                affirm((fieldSig.charAt(0) == 'L'
                        && fieldSig.charAt(fieldSig.length() - 1) == ';'),
                       "Inconsistent field signature");//NOI18N
                final String fieldTypeClassName
                    = fieldSig.substring(1, fieldSig.length() - 1);
                final ConstClass fieldTypeConstClass
                    = pool.addClass(fieldTypeClassName);
                insn = insn.append(
                    Insn.create(opc_checkcast, fieldTypeConstClass));
*/
                //@olsen: 4429769, disregard object and setField's return value
                insn = insn.append(Insn.create(opc_pop2));
                break;
            default:
                throw new InternalError("Unexpected field type");//NOI18N
            }

            insn = insn.append(afterFetchDirty);
        }

        //@olsen: added println() for debugging
        if (false) {
            System.out.println("        built annotation, "//NOI18N
                               + "required stack = "//NOI18N
                               + requiredStack);
        }

        return new AnnotationFragment(annotation, requiredStack);
    }

    /**
     * Assuming that an object reference is on the top of stack,
     * generate an instruction sequence to perform the annotation
     * indicated by the note.
     */
    //@olsen: must not return null
    private AnnotationFragment buildBasicAnnotation(InsnNote note) {
        int requiredStack = 2;
        Insn basicAnnotation = null;

        //@olsen: changed to use JDOMetaData
        final String targetClassName = note.targetClassName;
        final String targetFieldName = note.targetFieldName;
        final String targetPCRootClass = note.targetPCRootClass;

        //@olsen: not needed to ensure: note.dirtyThis() && !method.isStatic()
        final boolean fetch = (note.fetchPersistent() || note.fetchThis());
        final boolean dirty = (note.dirtyPersistent() || note.dirtyThis());
        //@olsen: added consistency check
        affirm((fetch ^ dirty),
               "Inconsistent fetch/dirty flags.");//NOI18N

        //@olsen: added println() for debugging
        if (false) {
            final boolean dfgField = note.dfgFieldAccess();
            final boolean pkField = note.pkFieldAccess();

            System.out.println("    build basic annotation: "//NOI18N
                               + targetClassName
                               + "." + targetFieldName + " : "//NOI18N
                               + (pkField ? "pk," : "!pk,")//NOI18N
                               + (dfgField ? "dfg," : "!dfg,")//NOI18N
                               + (fetch ? "fetch " : "dirty ")//NOI18N
                               + (note.fetchPersistent()
                                  ? "persistent" : "this")//NOI18N
                               + ";");//NOI18N
        }

        //@olsen: changed code for annotation
        {
            Insn insn = null;

            //requiredStack = 2;

            // get jdoFlags
            basicAnnotation = insn = Insn.create(opc_dup);
            insn = insn.append(
                Insn.create(opc_getfield,
                            pool.addFieldRef(
                                targetPCRootClass,
                                JDOMetaData.JDOFlagsFieldName,
                                JDOMetaData.JDOFlagsFieldSig)));

            // test jdoFlags
            // skip loading for read if <= 0 / for update if == 0
            InsnTarget afterFetchDirty = new InsnTarget();
            insn = insn.append(
                Insn.create((fetch ? opc_ifle : opc_ifeq),
                            afterFetchDirty));

            // get jdoStateManager
            insn = insn.append(Insn.create(opc_dup));
            insn = insn.append(
                Insn.create(opc_getfield,
                            pool.addFieldRef(
                                targetPCRootClass,
                                JDOMetaData.JDOStateManagerFieldName,
                                JDOMetaData.JDOStateManagerFieldSig)));

            // invoke StateManager's load method
            insn = insn.append(
                new InsnInterfaceInvoke(
                    pool.addInterfaceMethodRef(
                        JDOMetaData.JDOStateManagerPath,
                        (fetch ? "loadForRead" : "loadForUpdate"),//NOI18N
                        "()V"),//NOI18N
                    1));

            insn = insn.append(afterFetchDirty);
        }

        //@olsen: added println() for debugging
        if (false) {
            System.out.println("        built annotation, "//NOI18N
                               + "required stack = "//NOI18N
                               + requiredStack);
        }

        return new AnnotationFragment(basicAnnotation, requiredStack);
    }

    /**
     * Assuming that an object reference is on the top of stack,
     * generate an instruction sequence to perform the annotation
     * indicated by the note.
     */
//@olsen: disabled feature
/*
    private AnnotationFragment buildBasicAnnotation(InsnNote note) {
        Insn annotation = null;  // used?

        int requiredStack = 2;
        Insn basicAnnotation = null;
//@olsen: disabled feature
///
        ConstMethodRef methRef = null;
        InsnTarget afterFetchDirty = null;
        Insn flagsCheckAnnotation = null;
///

//@olsen: disabled feature
///
        ClassAction targetCA = note.targetClassAction();
///

//@olsen: disabled feature
///
        // we may need to save the argument in a register for later use
        Insn regStore = null;
        if (note.getArgReg() >= 0) {
            regStore = Insn.create(opc_dup);
            regStore.append(InsnUtils.aStore(note.getArgReg(), pool));
        }
///

///
        if (methRef != null) {
            if (flagsCheckAnnotation == null) {
                basicAnnotation = Insn.create(opc_dup);
                basicAnnotation.append(Insn.create(opc_invokestatic, methRef));
            } else {
                basicAnnotation = flagsCheckAnnotation;
                basicAnnotation.append(Insn.create(opc_dup));
                basicAnnotation.append(Insn.create(opc_invokestatic, methRef));
                basicAnnotation.append(afterFetchDirty);
            }
        }
///

//@olsen: disabled feature
///
        if (note.fetchPersistent() ||
            (note.fetchThis() && thisIsPersistent())) {
            methRef
                = pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "fetch",
                                    "(" + JDOMetaData.JDOPersistenceCapableSig + ")V");
            if (targetCA != null) {
                targetCA.ensureFlagsMemberValid();

                if (targetCA.getFlagsMember() != null) {
                    ClassControl flagsCC = targetCA.getFlagsMemberClassControl();

                    flagsCheckAnnotation = Insn.create(opc_dup);
                    //@olsen: changed to use JDOMetaData
                    flagsCheckAnnotation.append(
                        Insn.create(opc_getfield,
                                    pool.addFieldRef(
                                        targetPCRootClass,
                                        JDOMetaData.JDOFlagsFieldName,
                                        JDOMetaData.JDOFlagsFieldSig)));
                    flagsCheckAnnotation.append(
                        Insn.create(opc_getfield,
                                    pool.addFieldRef(flagsCC.className(),
                                                     targetCA.getFlagsMember(),
                                                     "B")));
                    afterFetchDirty = new InsnTarget();
                    //@olsen: skip loading for read if <= 0
                    flagsCheckAnnotation.append(
                        Insn.create(opc_ifle,
                                    afterFetchDirty));
                }
            }
        } else if (note.dirtyPersistent() ||
                   (note.dirtyThis() && thisIsPersistent())) {
            methRef
                = pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "dirty",
                                    "(" + JDOMetaData.JDOPersistenceCapableSig + ")V");
            if (targetCA != null) {
                targetCA.ensureFlagsMemberValid();

                if (targetCA.getFlagsMember() != null) {
                    ClassControl flagsCC = targetCA.getFlagsMemberClassControl();
                    flagsCheckAnnotation = Insn.create(opc_dup);
                    //@olsen: changed to use JDOMetaData
                    flagsCheckAnnotation.append(
                        Insn.create(opc_getfield,
                                    pool.addFieldRef(
                                        targetPCRootClass,
                                        JDOMetaData.JDOFlagsFieldName,
                                        JDOMetaData.JDOFlagsFieldSig)));
                    flagsCheckAnnotation.append(
                        Insn.create(opc_getfield,
                                    pool.addFieldRef(flagsCC.className(),
                                                     targetCA.getFlagsMember(),
                                                     "B")));
                    afterFetchDirty = new InsnTarget();
                    flagsCheckAnnotation.append(Insn.create(opc_iconst_2));
                    flagsCheckAnnotation.append(Insn.create(opc_iand));
                    flagsCheckAnnotation.append(
                        Insn.create(opc_ifeq,
                                    afterFetchDirty));
                    // One more word is needed here for the constant
                    requiredStack = 2;
                }
            }
        } else if (note.fetchArray()) {
            String fetchSig =
                arrayFetchSignature(env.doArrayElementFetch()
                                    ? note.arrayElementType() : 0);
            methRef = pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "fetch", fetchSig);
        } else if (note.fetchObject()) {
            methRef = pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "fetch",
                                        "(Ljava/lang/Object;)V");
        } else if (note.dirtyArray()) {
            methRef = pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "dirty",
                                        "(Ljava/lang/Object;)V");
        } else if (note.dirtyObject()) {
            methRef = pool.addMethodRef("com/sun/forte4j/persistence/internal/Implementation", "dirty",
                                        "(Ljava/lang/Object;)V");
        }

        if (methRef != null) {
            if (flagsCheckAnnotation == null) {
                basicAnnotation = Insn.create(opc_dup);
                basicAnnotation.append(Insn.create(opc_invokestatic, methRef));
            } else {
                basicAnnotation = flagsCheckAnnotation;
                basicAnnotation.append(Insn.create(opc_dup));
                basicAnnotation.append(Insn.create(opc_invokestatic, methRef));
                basicAnnotation.append(afterFetchDirty);
            }

            boolean cacheResult = false;

            if (env.doArrayOptimization() &&
                (note.fetchArray() || note.dirtyArray()) &&
                note.inLoop())
                cacheResult = true;

            if (cacheResult || note.checkNull()) {
                // Since this method appears to create a loop of some sort,
                // add a cache to remember what array has been fetched
                int cacheSlot = 0;
                InsnTarget skipTo = new InsnTarget();

                // This generally requires at least two words of stack
                if (cacheResult)
                    cacheSlot = newCacheSlot();

                Insn skipAnnotation = Insn.create(opc_dup);
                if (note.checkNull()) {
                    // skip cache-check/fetch if null
                    skipAnnotation.append(Insn.create(opc_ifnull, skipTo));

                    if (cacheResult)
                        // we used the dup'd result so dup again
                        skipAnnotation.append(Insn.create(opc_dup));
                }

                if (cacheResult) {
                    skipAnnotation.append(InsnUtils.aLoad(cacheSlot, pool));
                    skipAnnotation.append(Insn.create(opc_if_acmpeq, skipTo));
                }

                skipAnnotation.append(basicAnnotation);

                if (cacheResult) {
                    skipAnnotation.append(Insn.create(opc_dup));
                    skipAnnotation.append(InsnUtils.aStore(cacheSlot, pool));
                }

                if (requiredStack < 2)
                    requiredStack = 2;

                skipAnnotation.append(skipTo);
                basicAnnotation = skipAnnotation;
            }
        }
///

//@olsen: disabled feature
///
        // Put the unconditional store-arg-to-register annotation at the
        // front if non-null
        if (regStore != null) {
            regStore.append(basicAnnotation);
            basicAnnotation = regStore;
        }
///

        if (basicAnnotation != null)
            return new AnnotationFragment(basicAnnotation, requiredStack);

        return null;
    }
*/

    /**
     * Compute the method signature for a array fetch() method call based
     * on the type of array element (as defined in AnnotationConstants
     */
//@olsen: disabled feature
/*
    String arrayFetchSignature(int arrayElementType) {
        switch (arrayElementType) {
        case ArrayTypeBoolean:
            return "([Z)V";
        case ArrayTypeByte:
            return "([B)V";
        case ArrayTypeChar:
            return "([C)V";
        case ArrayTypeShort:
            return "([S)V";
        case ArrayTypeInt:
            return "([I)V";
        case ArrayTypeLong:
            return "([J)V";
        case ArrayTypeFloat:
            return "([F)V";
        case ArrayTypeDouble:
            return "([D)V";
        case ArrayTypeObject:
            return "([Ljava/lang/Object;)V";
        }
        // No special matching type - just use the default signature.
        return "(Ljava/lang/Object;)V";
    }
*/

    /**
     * Allocate a two word temporary register
     * @param idx the index of the temporary register to return.  If the
     *  specified temporary hasn't been allocated, allocated it now.
     */
    private int tmpReg2(int idx) {
        if (tmpDoubleRegisters == null)
            tmpDoubleRegisters = new Vector(3);

        // allocated as many 2 register pairs as necessary in order to
        // make idx be a valid index
        while (tmpDoubleRegisters.size() <= idx) {
            final CodeAttribute codeAttr = method.codeAttribute();
            final int reg = codeAttr.localsUsed();
            tmpDoubleRegisters.addElement(new Integer(reg));
            codeAttr.setLocalsUsed(reg+2);
        }

        return ((Integer)tmpDoubleRegisters.elementAt(idx)).intValue();
    }

    /**
     * Allocate a one word temporary register
     * @param idx the index of the temporary register to return.  If the
     *  specified temporary hasn't been allocated, allocated it now.
     */
    private int tmpReg(int idx) {
        if (tmpRegisters == null)
            tmpRegisters = new Vector(3);

        // allocate as many registers as necessary in order to
        // make idx be a valid index
        while (tmpRegisters.size() <= idx) {
            final CodeAttribute codeAttr = method.codeAttribute();
            final int reg = codeAttr.localsUsed();
            tmpRegisters.addElement(new Integer(reg));
            codeAttr.setLocalsUsed(reg+1);
        }
        return ((Integer)tmpRegisters.elementAt(idx)).intValue();
    }

    /**
     * Allocate an object fetch/store cache slot
     */
//@olsen: disabled feature
/*
    private int newCacheSlot() {
        CodeAttribute codeAttr = method.codeAttribute();
        int slot = codeAttr.localsUsed();
        codeAttr.setLocalsUsed(slot+1);
        if (caches == null)
            caches = new Vector(3);
        caches.addElement(new Integer(slot));
        return slot;
    }
*/

    /**
     * Note the following amount of stack used by a single annotation.
     */
    private void noteStack(int stk) {
        if (stk > annotationStack)
            annotationStack = (short)stk;
    }

    /**
     * Is this a non-static method of a persistence-capable class?
     */
//@olsen: disabled feature
/*
    private boolean thisIsPersistent() {
        return (ca.persistCapable() &&
                !method.isStatic());
    }
*/

// ---------------------------------------------------------------------------

    /**
     * Attempt to locate the instruction which deposits to the top of stack
     * the 1 word stack argument to currInsn which is argDepth deep on the
     * stack (top of stack == 0).
     * If unable to determine this with confidence, return null.
     *
     * Note that this method will not look back past a target.
     * Also, the operations performed by the dup2, dup_x1, dup_x2, dup2_x1,
     * dup2_x2 instructions are currently not understood by this method so
     * we don't attempt to chain back through these instructions.
     */
    private Insn findArgDepositer(Insn currInsn, int argDepth) {
        Insn depositer = null;
        for (Insn i = currInsn.prev(); argDepth >= 0; i = i.prev()) {
            // At control flow branch/merge points, abort the search for the
            // target operand.
            if (i.branches() ||
                ((i instanceof InsnTarget) && ((InsnTarget)i).isBranchTarget()))
                break;

            int nArgs = i.nStackArgs();
            int nResults = i.nStackResults();

            if (argDepth - nResults < 0) {
                // This instruction does deposit the value
                // For now, don't return depositers other than opc_dup which
                // deposit more than one value.  These are the
                // long/doubleinstructions (which can't be depositing a one
                // word value) and the dupX variants
                if (nResults > 1 && i.opcode() != opc_dup)
                    break;
                depositer = i;

                // consider special cases which may cause us to look further
                switch (i.opcode()) {
                case opc_dup:
                    if (argDepth == 0)
                        // keep going to find the real depositer at a greater depth
                        argDepth++;
                    break;
                case opc_checkcast:
                    // keep going to find the real depositer
                    break;
                default:
                    return i;
                }
            }

            argDepth += (nArgs - nResults);
        }

        return depositer;
    }

    /**
     * Assume that after the execution of state.insn there is a word on
     * the stack which is state.argDepth words deep.
     * Scan backwards through the instruction sequence, attempting to
     * locate an instruction after which the argument is at a minimal
     * depth w.r.t. the top of stack.  Update the state to indicate
     * progress.
     * Note that this method will not look back past a target.
     */
    private void minimizeStack(StackState state) {
        Insn i = state.insn;
        int argDepth = state.argDepth;

        Stack argTypesStack = new Stack();
        Stack resultTypesStack = new Stack();
        Stack stackTypes = new Stack();
        copyStack(state.stackTypes, stackTypes);

        for (; argDepth > 0; i = i.prev()) {
            // At control flow branch/merge points, abort the search for the
            // target operand.  The caller will have to make do with the best
            // stack state computed thus far.
            if (i.branches() ||
                ((i instanceof InsnTarget)
                 && ((InsnTarget)i).isBranchTarget()))
                break;

            int nArgs = i.nStackArgs();
            int nResults = i.nStackResults();
            String argTypes = i.argTypes();
            String resultTypes = i.resultTypes();

            argDepth -= nResults;
            // If the target argument was placed there by an instruction which
            // deposited multiple results (one of the dup type instructions)
            // then we don't have the smarts to figure out where it came from
            // so just quit looking
            if (argDepth < 0)
                break;
            argDepth += nArgs;

            if (i.opcode() == opc_swap) {
                Object x = stackTypes.pop();
                Object y = stackTypes.pop();
                stackTypes.push(x);
                stackTypes.push(y);
            } else {
                // Make sure the arg types and result types stacks are empty
                while (!argTypesStack.empty()) argTypesStack.pop();
                while (!resultTypesStack.empty()) resultTypesStack.pop();

                Descriptor.computeStackTypes(argTypes, argTypesStack);
                Descriptor.computeStackTypes(resultTypes, resultTypesStack);

                int expectWords = 0;
                while (!resultTypesStack.empty())
                    expectWords += Descriptor.elementSize(
                        ((Integer) resultTypesStack.pop()).intValue());

                while (expectWords > 0)
                    expectWords -= Descriptor.elementSize(
                        ((Integer) stackTypes.pop()).intValue());

                if (expectWords < 0) {
                    // perhaps we ought to signal an exception, but returning
                    // will keep things going just fine.
                    return;
                }

                transferStackArgs(argTypesStack, stackTypes);
            }

            if (argDepth >= 0 && argDepth < state.argDepth &&
                knownTypes(stackTypes, argDepth)) {
                state.argDepth = argDepth;
                state.insn = i.prev();
                copyStack(stackTypes, state.stackTypes);
            }
        }
    }

    /* Take all stack elements in fromStack and push them onto toStack
     * such that they are in the same relative stack positions */
    private final void transferStackArgs(Stack fromStack, Stack toStack) {
        if (!fromStack.empty()) {
            Object o = fromStack.pop();
            transferStackArgs(fromStack, toStack);
            toStack.push(o);
        }
    }

    /* Make toStack look just like fromStack */
    private final void copyStack(Stack fromStack, Stack toStack) {
        while (!toStack.empty())
            toStack.pop();

        // take advantage of Stack's inheritance from Vector
        for (int i=0; i<fromStack.size(); i++)
            toStack.addElement(fromStack.elementAt(i));
    }

    /* Check that the top nWords worth of types on stack are well defined */
    private final boolean knownTypes(Stack stack, int nWords) {
        // take advantage of Stack's inheritance from Vector
        for (int i=stack.size()-1; i>= 0 && nWords > 0; i--) {
            int words = 0;
            switch (((Integer)stack.elementAt(i)).intValue()) {
            case T_UNKNOWN:
            case T_WORD:
            case T_TWOWORD:
                return false;

            case T_BOOLEAN:
            case T_CHAR:
            case T_FLOAT:
            case T_BYTE:
            case T_SHORT:
            case T_INT:
            case TC_OBJECT:
            case TC_INTERFACE:
            case TC_STRING:
                words = 1;
                break;

            case T_DOUBLE:
            case T_LONG:
                words = 2;
                break;

            default:
                break;
            }
            nWords -= words;
        }
        return true;
    }

// ---------------------------------------------------------------------------

    /**
     * Add a list of notes to the note list.
     */
    //@olsen: made final
    private final void addNoteList(InsnNote note) {
        insnNotes.put(note.insn, note);
    }

    /**
     * Find the note list for the specified instruction.
     */
    //@olsen: made final
    private final InsnNote getNoteList(Insn insn) {
        return (InsnNote)insnNotes.get(insn);
    }
}

// ---------------------------------------------------------------------------

/**
 * Class loop is a simple class to represent a possible looping construct
 * within a method.
 */
//@olsen: disabled feature
/*
class Loop {
    int loopStart; // instruction offset - inclusive
    int loopEnd;   // instruction offset - inclusive

    Loop(int lStart, int lEnd) {
        loopStart = lStart;
        loopEnd = lEnd;
    }
*/

    /**
     * Scan the instructions looking for backward branches which suggests a
     * loop structure.  If any are found, return a Loop object which
     * represents the largest possible loop.
     */
//@olsen: disabled feature
/*
    static Loop checkLoops(Insn code) {
        // Use 999999 to represent an impossibly large instruction offset
        // The current VM design limits methods to 64k of instructions
        int loopStart = 999999;
        int loopEnd = 0;

        for (Insn i = code; i != null; i = i.next()) {
            if (i instanceof InsnTargetOp) {
                InsnTarget targ = ((InsnTargetOp) i).target();
                if (targ.offset() < i.offset()) {
                    // a backward branch
                    if (targ.offset() < loopStart)
                        loopStart = targ.offset();
                    if (i.offset() > loopEnd)
                        loopEnd = i.offset();
                }
            }
        }

        if (loopStart < loopEnd)
            return new Loop(loopStart, loopEnd);
        return null;
    }
*/

    /**
     * Is the instruction contained within the loop?
     * Note that the instruction must have a valid offset for this
     * to answer correctly.
     */
//@olsen: disabled feature
/*
    boolean contains(Insn i) {
        return i.offset() >= loopStart && i.offset() <= loopEnd;
    }
}
*/

// ---------------------------------------------------------------------------

/**
 * A structure to record what annotation requirements are implied
 * by a particular VM instruction.
 */
class InsnNote
    extends Support
    implements AnnotationConstants {
    //@olsen: made final
    final Insn insn;
    int insnFlags;
    final int argWord;
//@olsen: disabled feature
/*
    int argReg = -1;
*/
    final String stackSig;

    //@olsen: added fields
    final String targetClassName;
    final String targetFieldName;
    final int targetFieldIndex;
    final String targetPCRootClass;
//@olsen: disabled feature
/*
    final ClassAction targetClassAction;
*/

    /**
     * Return the next instruction note in this instruction sequence.
     * Most instructions need only a single note, but subtypes may need
     * to be chained, and should re-implement next().
     */
    InsnNote next() {
        return null;
    }

    /**
     * Return a descriptor for the current stack state.  This descriptor
     * is a sequence of VM element type descriptors in decreasing stack
     * depth order.  That is, the element on the top of stack is the last
     * element in the descriptor signature.  This descriptor is only
     * guaranteed to represent the objects between the instruction stack
     * operand (which is offset arg()) deep on the stack and the top of
     * stack at the time insn is to be executed.  It may however contain
     * additional descriptor elements.
     */
     final String sig() {
        return stackSig;
    }

//@olsen: disabled feature
/*
    final ClassAction targetClassAction() {
        return targetClassAction;
    }
*/

    /**
     * Return the offset from the top of the stack of the argument
     */
    final int arg() {
        return argWord;
    }

    final boolean fetchThis() {
        return (insnFlags & FetchThis) != 0;
    }

//@olsen: disabled feature
/*
    final void dontFetchThis() {
        insnFlags &= ~FetchThis;
    }
*/

    final boolean dirtyThis() {
        return (insnFlags & DirtyThis) != 0;
    }

//@olsen: disabled feature
/*
    final void dontDirtyThis() {
        insnFlags &= ~DirtyThis;
    }
*/

//@olsen: disabled feature
/*
    final boolean unconditional() {
        return (insnFlags & Unconditional) != 0;
    }
*/

//@olsen: disabled feature / not used anymore
/*
    final boolean fetchObject() {
        return (insnFlags & FetchObject) != 0;
    }

    final boolean dirtyObject() {
        return (insnFlags & DirtyObject) != 0;
    }
*/

    final boolean fetchPersistent() {
        return (insnFlags & FetchPersistent) != 0;
    }

    final boolean dirtyPersistent() {
        return (insnFlags & DirtyPersistent) != 0;
    }

    //@olsen: added method
    final boolean dfgFieldAccess() {
        return (insnFlags & DFGField) != 0;
    }

    //@olsen: added method
    final boolean pkFieldAccess() {
        return (insnFlags & PKField) != 0;
    }

//@olsen: disabled feature
/*
    final boolean fetchArray() {
        return (insnFlags & FetchArray) != 0;
    }

    final boolean dirtyArray() {
        return (insnFlags & DirtyArray) != 0;
    }

    final int arrayElementType() {
        return insnFlags & ArrayTypeMask;
    }

    final boolean inLoop() {
        return (insnFlags & InLoop) != 0;
    }

    final boolean checkNull() {
        return (insnFlags & CheckNull) != 0;
    }
*/

    /* If getArgReg returns < 0, the argReg is not set */
//@olsen: disabled feature
/*
    final int getArgReg() {
        return argReg;
    }

    final void setArgReg(int reg) {
        argReg = reg;
    }
*/

    /**
     * Construct an instruction note.
     * @param argWord must be the depth of the word on the stack in
     *    word units
     * @param stackSig a stack descriptor for the stack - see
     *  the doc for sig().
     */
    InsnNote(Insn i, int flags, int argWord,
             String stackSig,
             String targetClassName,
             String targetFieldName,
             int targetFieldIndex,
             String targetPCRootClass) {
//@olsen: disabled feature
/*
       InsnNote(Insn i, int flags, int argWord,
             String stackSig, ClassAction targetClassAction) {
*/
        insn = i;
        insnFlags = flags;
        this.stackSig = stackSig;
        this.argWord = argWord;
//@olsen: disabled feature
/*
        this.targetClassAction = targetClassAction;
*/
        this.targetClassName = targetClassName;
        this.targetFieldName = targetFieldName;
        this.targetFieldIndex = targetFieldIndex;
        this.targetPCRootClass = targetPCRootClass;

        //@olsen: added consistency check
        affirm(!(insn == null
                 || argWord < 0
                 || targetClassName == null
                 || targetFieldName == null
                 || targetFieldIndex < 0
                 || targetPCRootClass == null),
               "Inconsistent instruction annotation note.");//NOI18N
    }
}

/**
 * A specialized form of instruction note for method invocation arguments.
 * The only thing that this adds is a link element to allow multiple notes
 * to apply to an instruction.
 */
//@olsen: disabled feature
/*
class InsnArgNote extends InsnNote {
    InsnArgNote nextNote;

    InsnNote next() {
        return nextNote;
    }

    InsnArgNote(Insn i, int flags, int argWord, String stackSig) {
        super(i, flags, argWord, stackSig, null, null);
    }
}
*/

// ---------------------------------------------------------------------------

/**
 * StackState is really just a simple association of instruction
 * and the depth of some stack operand on the operand stack.
 */
class StackState implements VMConstants {
    /* number of words deep that the target word is */
    int argDepth;

    /* Stack of types */
    Stack stackTypes;

    /* the instruction after which, the word is argDepth deep */
    Insn insn;

    StackState(int depth, String stackSig, Insn i) {
        stackTypes = new Stack();
        Descriptor.computeStackTypes(stackSig, stackTypes);
        argDepth = depth;
        insn = i;
    }
}

/**
 * AnnotationFragment is really just a simple association of instruction
 * and the number of words of stack used during the execution of the
 * fragment.
 */
class AnnotationFragment {
    Insn annotation;
    int  stackRequired;

    AnnotationFragment(Insn i, int stack) {
        annotation = i;
        stackRequired = stack;
    }
}

// ---------------------------------------------------------------------------

