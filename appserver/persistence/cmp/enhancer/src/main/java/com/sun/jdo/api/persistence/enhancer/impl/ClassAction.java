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

//import java.io.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import com.sun.jdo.api.persistence.enhancer.classfile.*;

import com.sun.jdo.api.persistence.enhancer.util.Support;

//@olsen: added import
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;


//@olsen: cosmetics
//@olsen: moved: this class -> package impl
//@olsen: subst: (object)state -> flags
//@olsen: subst: JDOFlags -> jdoFlags
//@olsen: subst: makeJDO[gs]etFlags -> makeJDO[GS]etFlags
//@olsen: subst: JDO[gs]etFlags -> jdo[GS]etFlags
//@olsen: subst: [Nn]eedsJDORefMethods -> [Nn]eedsJDOStateManagerMethods
//@olsen: subst: JDORef -> jdoStateManager
//@olsen: subst: makeJDO[gs]etRef -> makeJDO[GS]etStateManager
//@olsen: subst: JDO[gs]etRef -> jdo[GS]etStateManager
//@olsen: subst: [iI]Persistent -> [pP]ersistenceCapable
//@olsen: subst: PersistentAux -> StateManager
//@olsen: subst: jdo/ -> com/sun/forte4j/persistence/internal/
//@olsen: subst: jdo. -> com.sun.forte4j.persistence.internal.
//@olsen: subst: /* ... */ -> // ...
//@olsen: subst: filterEnv -> env
//@olsen: subst: FilterEnv -> Environment
//@olsen: made MethodBuilder's methods non-static
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: Hashtable -> Map, HashMap
//@olsen: subst: Vector -> Collection, List, ArrayList
//@olsen: subst: Enumeration,... -> Iterator, hasNext(), next()
//@olsen: subst: absolut jdo types and names -> constants from JDOMetaData
//@olsen: subst: refMember, flagsMember -> JDOStateManagerName, JDOFlagsName
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, affirm()
//@olsen: removed: proprietary support for HashCode
//@olsen: removed: proprietary support for TypeSummary
//@olsen: removed: proprietary support for ClassInfo
//@olsen: removed: proprietary support for {set,get}{Ref,Flags}{Fields,Methods}
//@olsen: simplified detection/generation of jdo[GS]etStateManager() methods
//@olsen: simplified detection/generation of jdo[GS]etFlags() methods
//@olsen: removed: old, disabled ODI code


/**
 * ClassAction handles the persistence annotation actions for a class.
 * The details specific to individual methods and fields are delegated
 * to instances of FieldAction and MethodAction.
 */
final class ClassAction
    extends Support
    implements VMConstants {

    /* bit mask constants indicating what needs to be generated */
//@olsen: disabled feature
/*
    private static final int GENInitContents  = 0x02;
    private static final int GENFlushContents = 0x04;
    private static final int GENClearContents = 0x08;

    private static final int GENAllContents   =
    GENInitContents | GENFlushContents | GENClearContents;
*/

    /* Constants for the class level annotation attribute */
    private static final String AnnotatedAttribute = "com.sun.jdo.api.persistence.enhancer.annotated";//NOI18N
    private static final short  AnnotatedVersion = 1;

    //@olsen: added method name constants
    static private final String jdoGetStateManagerName
    = "jdoGetStateManager";//NOI18N
    static private final String jdoSetStateManagerName
    = "jdoSetStateManager";//NOI18N
    static private final String jdoGetFlagsName
    = "jdoGetFlags";//NOI18N
    static private final String jdoSetFlagsName
    = "jdoSetFlags";//NOI18N
    static private final String jdoMakeDirtyName
    = "jdoMakeDirty";//NOI18N
    static private final String jdoIsDirtyName
    = "jdoIsDirty";//NOI18N
    static private final String jdoIsTransactionalName
    = "jdoIsTransactional";//NOI18N
    static private final String jdoIsPersistentName
    = "jdoIsPersistent";//NOI18N
    static private final String jdoIsNewName
    = "jdoIsNew";//NOI18N
    static private final String jdoIsDeletedName
    = "jdoIsDeleted";//NOI18N
    static private final String jdoGetPersistenceManagerName
    = "jdoGetPersistenceManager";//NOI18N
    static private final String jdoGetObjectIdName
    = "jdoGetObjectId";//NOI18N
    static private final String jdoConstructorName
    = "<init>";//NOI18N
    static private final String jdoNewInstanceName
    = "jdoNewInstance";//NOI18N
    static private final String jdoClearName
    = "jdoClear";//NOI18N
    static private final String jdoCopyName
    = "jdoCopy";//NOI18N
    static private final String jdoGetFieldName
    = "jdoGetField";//NOI18N
    static private final String jdoSetFieldName
    = "jdoSetField";//NOI18N
    static private final String jdoCloneName
    = "clone";//NOI18N

    /* The class to be annotated */
    //@olsen: made final
    private final ClassControl control;

    /* Central repository for the options and classes */
    //@olsen: added association
    //@olsen: made final
    private final Environment env;

    /* The method builder helper object. */
    //@olsen: made MethodBuilder's methods non-static
    //@olsen: made final
    private final MethodBuilder methodBuilder;

    /* Hash table mapping ClassMethod to MethodAction  */
    //@olsen: made final
    //@olsen: subst: Hashtable -> HashMap
    private final Map methodActionTable = new HashMap(11);

    /* Vector of FieldAction  */
    //@olsen: made final
    //@olsen: subst: Vector -> ArrayList
    private final List fieldActionTable = new ArrayList();

    /* What should we generate for this class?
     * This is a combination of the GENXXXX constants defined above. */
//@olsen: disabled feature
/*
    private int generate;
*/

    /* True if this has already been annotated */
    private boolean previouslyAnnotated = false;

    /* If true, this class will directly implement PersistenceCapable
       interface. */
    private boolean implementsPersistence = false;

    /* If true, this class will directly implement PersistenceCapableHooks
       interface if no base class does so. */
//@olsen: disabled feature
/*
    private boolean implementsPersistenceHooks = false;
    private boolean implementsPersistenceHooksKnown = false;
*/

    /* True if a user-defined jdo member has been seen in this class. */
    //@olsen: added fields
    private boolean sawImplementsPersistenceCapable = false;
    private boolean sawImplementsCloneable = false;
    private boolean sawFieldJDOStateManager = false;
    private boolean sawFieldJDOFlags = false;
    private boolean sawMethodJDOGetStateManager = false;
    private boolean sawMethodJDOSetStateManager = false;
    private boolean sawMethodJDOGetFlags = false;
    private boolean sawMethodJDOSetFlags = false;
    private boolean sawMethodJDOMakeDirty = false;
    private boolean sawMethodJDOIsDirty = false;
    private boolean sawMethodJDOIsTransactional = false;
    private boolean sawMethodJDOIsPersistent = false;
    private boolean sawMethodJDOIsNew = false;
    private boolean sawMethodJDOIsDeleted = false;
    private boolean sawMethodJDOGetPersistenceManager = false;
    private boolean sawMethodJDOGetObjectId = false;
    private boolean sawMethodJDOConstructor = false;
    private boolean sawMethodJDONewInstance = false;
    private boolean sawMethodJDOGetField = false;
    private boolean sawMethodJDOSetField = false;
    private boolean sawMethodJDOClear = false;
    private boolean sawMethodJDOCopy = false;
    private boolean sawMethodJDOClone = false;

    /* True if the preDestroyPersistent() method needs to be generated
       for PersistenceCapable. */
//@olsen: disabled feature
/*
    private boolean needsPreDestroyPersistent = true;
*/

    /* True if the postInitializeContents() method needs to be generated
       for PersistenceCapable. */
//@olsen: disabled feature
/*
    private boolean needsPostInitializeContents = true;
*/

    /* True if the preFlushContents() method needs to be generated
       for PersistenceCapable. */
//@olsen: disabled feature
/*
    private boolean needsPreFlushContents = true;
*/

    /* True if the preClearContents() method needs to be generated
       for PersistenceCapable. */
//@olsen: disabled feature
/*
    private boolean needsPreClearContents = true;
*/


    // public accessors

    /**
     * Constructor
     */
    //@olsen: added parameter 'env' for association
    public ClassAction(ClassControl control,
                       Environment env) {
        this.control = control;
        this.env = env;
        this.methodBuilder = new MethodBuilder(env);
    }

    /**
     * Perform the pass1 scan of the class.
     * Certain scan operations must be completed for all classes before
     * the remaining operations can be completed.
     */
    //@olsen: dropped argument: boolean filterRequired
    public void scan1() {
        //@olsen: moved verbose output from ClassControl to ClassAction
        env.message("scanning class " + control.userClassName());//NOI18N

        //@olsen: added constraints; ensured by ClassControl
        affirm(!classFile().isInterface());
        affirm(control.persistType() > ClassControl.TransientOnly);

        scanAttributes();

        //@olsen: 4357074 skip previously enhanced files
        if (previouslyAnnotated) {
            return;
        }

        //@olsen: initialize 'implementsPersistence' flag from JDOMetaData
        final String name = className();
        implementsPersistence
            = env.getJDOMetaData().isPersistenceCapableRootClass(name);

        //@olsen: checks on persistence-capable classes
        final boolean isPersistent
            = (control.persistType() == ClassControl.PersistCapable);
        if (isPersistent) {
            // check whether this class directly implements PersistenceCapable
            // or clonable.
            scanForImplementsInterfaces();

            // check only fields of persistence-capable classes
            scanFields();
        }

        //@olsen: removed check, ensured before already
        //if (!previouslyAnnotated && !classFile().isInterface())
        scanMethods();
    }

    /**
     * Add an PersistenceCapable implementation and PersistenceCapableHooks
     * implementation if needed.
     */
    //@olsen: subst: augmentInterfaces -> augment
    //@olsen: dropped argument: boolean filterRequired
    public void augment() {
        if (previouslyAnnotated)
            return;

        if (implementsPersistence) {
            env.message("augmenting class " + control.userClassName());//NOI18N

            if (!sawImplementsPersistenceCapable) {
                augmentClassInterface(JDOMetaData.JDOPersistenceCapablePath);
            }

            if (!sawImplementsCloneable) {
                augmentClassInterface(JDOMetaData.javaLangCloneablePath);
            }

            //@olsen: made fields to have public access
            {
                insertPersistenceCapableFields(
                    JDOMetaData.JDOStateManagerFieldName,
                    JDOMetaData.JDOStateManagerFieldSig,
                    JDOMetaData.JDOStateManagerFieldType,
                    ACCTransient | ACCPublic);

                insertPersistenceCapableFields(
                    JDOMetaData.JDOFlagsFieldName,
                    JDOMetaData.JDOFlagsFieldSig,
                    JDOMetaData.JDOFlagsFieldType,
                    ACCTransient | ACCPublic);
            }

            insertPersistenceCapableMethods();
        }

//@olsen: disabled feature
/*
        if (getImplementsPersistenceHooks() &&
            !control.hasPersistenceCapableHooksProvided()) {
            env.message("modifying class " + control.userClassName() +
                        " to implement " +
                        ClassControl.userClassFromVMClass(
                            JDOMetaData.JDOInstanceCallbacksName));

            augmentClassInterface(JDOMetaData.JDOInstanceCallbacksName);
            insertPersistenceCapableHooksMethods();
        }
*/
    }

    /**
     * Modify the class references within this class according to the
     * mappings in classTranslations.
     */
//@olsen: disabled feature
/*
    public void retarget(Map classTranslations,
                         boolean filterRequired) {

        //@olsen: added final modifiers
        final ConstantPool pool = classFile().pool();

        // First, translate the constant pool
        final int nEntries = pool.nEntries();
        for (int i=0; i<nEntries; i++) {
            ConstBasic basic = pool.constantAt(i);
            if (basic != null) {
                if (basic instanceof ConstClass) {
                    ConstClass classRef = (ConstClass) basic;
                    String classRefName = classRef.asString();
                    String translation =
                        Descriptor.translateClass(classRefName,
                                                  classTranslations);
                    if (translation != classRefName)
                        classRef.changeClass(pool.addUtf8(translation));
                } else if (basic instanceof ConstNameAndType) {
                    ConstNameAndType ntRef = (ConstNameAndType) basic;
                    String sig = ntRef.signature().asString();
                    String modSig = Descriptor.remapTypes(sig,
                                                          classTranslations);
                    if (!modSig.equals(sig))
                        ntRef.changeSignature(pool.addUtf8(modSig));
                }
            }
        }

        // Next, translate method signatures
        for (Enumeration cme = classFile().methods().elements();
             cme.hasMoreElements(); ) {
            ClassMethod method = (ClassMethod) cme.nextElement();
            String sig = method.signature().asString();
            String newSig = Descriptor.remapTypes(sig, classTranslations);
            if (!newSig.equals(sig))
                method.changeSignature(pool.addUtf8(newSig));
        }

        // Next, translate field signatures
        for (Enumeration cfe = classFile().fields().elements();
             cfe.hasMoreElements(); ) {
            ClassField field = (ClassField) cfe.nextElement();
            String sig = field.signature().asString();
            String newSig = Descriptor.remapTypes(sig, classTranslations);
            if (!newSig.equals(sig))
                field.changeSignature(pool.addUtf8(newSig));
        }

//@olsen: disabled feature
//
        InvokeAnnotation.retarget(env, classTranslations);
//

        for (Iterator me = methodActionTable.values().iterator();
             me.hasNext();){
            MethodAction ma = (MethodAction)me.next();
            ma.retarget(classTranslations);
        }

        for (Iterator fe = fieldActionTable.iterator(); fe.hasNext();){
            FieldAction fa = (FieldAction)fe.next();
            fa.retarget(classTranslations);
        }

        if (!filterRequired)
            return;

        // this should be correct most of the time, but may cause some classes
        // to be updated unnecessarily.
        control.noteUpdate();
    }
*/

    /**
     * Perform the annotation operations for this class
     */
    public void annotate() {
        if (previouslyAnnotated)
            return;

        //@olsen: moved verbose output from ClassControl to ClassAction
        env.message("annotating class " + control.userClassName());//NOI18N

        boolean updates = false;

        for (Iterator e = methodActions(); e.hasNext(); ) {
            MethodAction methodAction = (MethodAction)e.next();
            if (methodAction.needsAnnotation()) {
                methodAction.annotate();
                updates = true;
            }
        }

//@olsen: disabled feature
/*
        if ((generate & GENInitContents) != 0) {
            classFile().addMethod(methodBuilder.makeInitializeContents(this));
            updates = true;
        }

        if ((generate & GENFlushContents) != 0) {
            classFile().addMethod(methodBuilder.makeFlushContents(this));
            updates = true;
        }

        if ((generate & GENClearContents) != 0) {
            classFile().addMethod(methodBuilder.makeClearContents(this));
            updates = true;
        }
*/

        // Even if we haven't updated anything, we want to add an annotated
        // attribute if we are doing in-place updates so that we don't
        // rewrite the file every time.
        if (updates || env.updateInPlace()) {
            control.noteUpdate();

            //^olsen: to test

            // Leave a hint that we've been here before
            final byte[] data = new byte[2];
            data[0] = (byte)(AnnotatedVersion >>> 8);
            data[1] = (byte)(AnnotatedVersion & 0xff);
            final ClassAttribute annotatedAttr
                = new GenericAttribute(
                    classFile().pool().addUtf8(AnnotatedAttribute), data);
            classFile().attributes().addElement(annotatedAttr);
        }
    }

    // package accessors

    /**
     *  Get the control object
     */
    ClassControl classControl() {
        return control;
    }

    /**
     * Get the class file which we are operating on
     */
    ClassFile classFile() {
        return control.classFile();
    }

    /**
     * Return an Enumeration of the FieldActions for the class
     */
    Iterator fieldActions() {
        return fieldActionTable.iterator();
    }

    /**
     * Return an Enumeration of the MethodActions for the class
     */
    Iterator methodActions() {
        return methodActionTable.values().iterator();
    }

    /**
     * Return the class name in VM form
     */
    public String className() {
        return control.className();
    }

    /**
     * Return the class name in user ('.' delimited) form
     */
    public String userClassName() {
        return control.userClassName();
    }

    /**
     * Return true if this class will implement PersistenceCapable.
     */
    public boolean getImplementsPersistence() {
        return implementsPersistence;
    }

    /**
     * Return true if this class will implement PersistenceCapableHooks.
     */
//@olsen: disabled feature
/*
    public boolean getImplementsPersistenceHooks() {
        if (!implementsPersistenceHooksKnown) {
            if (!needsPreDestroyPersistent ||
                !needsPreClearContents ||
                !needsPreFlushContents ||
                !needsPostInitializeContents)
                implementsPersistenceHooks = true;
            implementsPersistenceHooksKnown = true;
        }
        return implementsPersistenceHooks;
    }
*/

    /**
     * Return true if this method needs an implementation of clone().
     */
    public boolean hasCloneMethod() {
        return sawMethodJDOClone;
    }

    /**
     * Checks the class attributes for a filter.annotated attribute.
     * This works even if scan1 hasn't run yet.
     */
    public boolean hasAnnotatedAttribute() {
        if (previouslyAnnotated)
            return true;

        Enumeration e = classFile().attributes().elements();
        while (e.hasMoreElements()) {
            ClassAttribute attr = (ClassAttribute) e.nextElement();
            if (attr.attrName().asString().equals(AnnotatedAttribute))
                return true;
        }

        return false;
    }

    /**
     * Check whether this class has a persistent field of the
     * specified name.  This might be called when the FieldActions have
     * not yet been built.
     */
//@olsen: disabled feature
/*
    boolean fieldIsPersistent(String fieldName) {
        ClassField field = classFile().findField(fieldName);
        if (field != null) {
            String className = classFile().className().asString();
            String fieldName = field.name().asString();
            //@olsen: disabled feature
            //return FieldAction.fieldIsPersistent(classFile(), field);
            return env.getJDOMetaData().isPersistentField(className, fieldName);
        }
        return false;
    }
*/

    // private methods

    /**
     * Scans the attributes of a ClassFile
     */
    private void scanAttributes() {
        Enumeration e = classFile().attributes().elements();
        while (e.hasMoreElements()) {
            ClassAttribute attr = (ClassAttribute) e.nextElement();
            if (attr.attrName().asString().equals(AnnotatedAttribute)) {
                previouslyAnnotated = true;

//@olsen: disabled feature
/*
                if (!control.isImplicitlyPersistent() && !env.updateInPlace()) {
*/
                {
                    // At some point we may want to consider stripping old
                    // annotations and re-annotating, but not yet
                    env.message("ignoring previously enhanced class "//NOI18N
                                + control.userClassName());
                }
                break;
            }
        }
    }

    /**
     * Scans the class to check whether it implemens interfaces
     * PersistenceCapable and Clonable.
     * Sets instance variables <code>sawImplementsPersistenceCapable</code> if
     * the class implements <code>JDOMetaData.JDOPersistenceCapablePath</code>.
     * Sets the instance variable <code>sawImplementsCloneable</code>
     * if the class implements <code>JDOMetaData.javaLangCloneablePath</code>.
     * Please note that only the current class is scanned for implemented
     * interfaces by this method. Even if the super class implements
     * one of the above interfaces, the corresponding instance variable will
     * not be set.
     */
    //@olsen: added method
    private void scanForImplementsInterfaces() {
        for (Iterator ifc = classFile().interfaces().iterator();
             ifc.hasNext();) {
            final ConstClass i = (ConstClass)ifc.next();
            String interfaceNamePath = i.asString();
            if (interfaceNamePath.equals(JDOMetaData.JDOPersistenceCapablePath)) {
                sawImplementsPersistenceCapable = true;

                //@olsen: warn if user-defined 'implements PC' clause
                env.warning(
                    getI18N("enhancer.class_implements_jdo_pc",//NOI18N
                            new Object[]{
                                userClassName(),
                                JDOMetaData.JDOPersistenceCapableType
                            }));
            }
            if(JDOMetaData.javaLangCloneablePath.equals(interfaceNamePath) ) {
                sawImplementsCloneable = true;
            }
        }

//@olsen: disabled feature
//@olsen: don't check whether this class implements PC indirectly
/*
        if (control.implementsPersistenceCapable())
            env.warning(
                getI18N("enhancer.class_implements_pc",
                        userClassName(),
                        meta.JDOPersistenceCapableType));
*/
    }

    /**
     * Scans the fields of a ClassFile
     * If this is not a persistence capable class, do nothing.
     */
    private void scanFields() {
        Enumeration e = classFile().fields().elements();
        while (e.hasMoreElements()) {
            final ClassField f = (ClassField)e.nextElement();
            final String fieldName = f.name().asString();
            final String fieldSig = f.signature().asString();

            //@olsen: added check
            scanForJDOFields(fieldName, fieldSig);

            FieldAction action = new FieldAction(this, f, env);
            action.check();
            fieldActionTable.add(action);
        }
    }

    /**
     * Scan for JDO fields.
     */
    //@olsen: added method
    private void scanForJDOFields(String fieldName,
                                  String fieldSig) {
        if (fieldName.equals(JDOMetaData.JDOStateManagerFieldName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_field",//NOI18N
                        userClassName(),
                        JDOMetaData.JDOStateManagerFieldName));
            sawFieldJDOStateManager = true;
            return;
        }
        if (fieldName.equals(JDOMetaData.JDOFlagsFieldName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_field",//NOI18N
                        userClassName(),
                        JDOMetaData.JDOFlagsFieldName));
            sawFieldJDOFlags = true;
            return;
        }
        //@olsen: check whether member starts with the reserved jdo prefix
        if (fieldName.startsWith("jdo")) {//NOI18N
            //@olsen: issue a warning only
            env.warning(
                getI18N("enhancer.class_has_jdo_like_member",//NOI18N
                        userClassName(), fieldName));
            return;
        }
    }

    /**
     * Scans the methods of a ClassFile.
     */
    private void scanMethods() {
        final boolean isPersistent
            = (control.persistType() == ClassControl.PersistCapable);

        Enumeration e = classFile().methods().elements();
        while (e.hasMoreElements()) {
            final ClassMethod m = (ClassMethod)e.nextElement();
            final String methodName = m.name().asString();
            final String methodSig = m.signature().asString();

            if (isPersistent) {
                scanForJDOMethods(methodName, methodSig);
            }

            final MethodAction action = new MethodAction(this, m, env);
            action.check();
            methodActionTable.put(m, action);
        }
    }


    /**
     * Scan for JDO methods.
     */
    //@olsen: moved code from scanMethods()
    private void scanForJDOMethods(String methodName,
                                   String methodSig) {
        if (methodName.equals(jdoGetStateManagerName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOGetStateManager = true;
            return;
        }
        if (methodName.equals(jdoSetStateManagerName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOSetStateManager = true;
            return;
        }
        if (methodName.equals(jdoGetFlagsName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOGetFlags = true;
            return;
        }
        if (methodName.equals(jdoSetFlagsName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOSetFlags = true;
            return;
        }
        if (methodName.equals(jdoMakeDirtyName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOMakeDirty = true;
            return;
        }
        if (methodName.equals(jdoIsDirtyName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOIsDirty = true;
            return;
        }
        if (methodName.equals(jdoIsTransactionalName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOIsTransactional = true;
            return;
        }
        if (methodName.equals(jdoIsPersistentName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOIsPersistent = true;
            return;
        }
        if (methodName.equals(jdoIsNewName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOIsNew = true;
            return;
        }
        if (methodName.equals(jdoIsDeletedName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOIsDeleted = true;
            return;
        }
        if (methodName.equals(jdoGetPersistenceManagerName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOGetPersistenceManager = true;
            return;
        }
        if (methodName.equals(jdoGetObjectIdName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOGetObjectId = true;
            return;
        }
        //^olsen: get signature from method builder
        // for jdo constructor, check by name and signature
        if (methodName.equals(jdoConstructorName)
            && methodSig.equals("(" + JDOMetaData.JDOStateManagerSig + ")V")) {//NOI18N
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOConstructor = true;
            return;
        }
        if (methodName.equals(jdoNewInstanceName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDONewInstance = true;
            return;
        }
        if (methodName.equals(jdoClearName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOClear = true;
            return;
        }
        if (methodName.equals(jdoCopyName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOCopy = true;
            return;
        }
        if (methodName.equals(jdoGetFieldName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOGetField = true;
            return;
        }
        if (methodName.equals(jdoSetFieldName)) {
            env.error(
                getI18N("enhancer.class_defines_jdo_method",//NOI18N
                        userClassName(), methodName));
            sawMethodJDOSetField = true;
            return;
        }
        //^olsen: get signature from method builder
        // for method clone(), check by name and signature
        if (methodName.equals(jdoCloneName)
            && methodSig.equals("()Ljava/lang/Object;")) {//NOI18N
            // it's OK to have a user-defined clone()
            sawMethodJDOClone = true;
            return;
        }
        //@olsen: check whether member starts with the reserved jdo prefix
        if (methodName.startsWith("jdo")) {//NOI18N
            //@olsen: issue a warning only
            env.warning(
                getI18N("enhancer.class_has_jdo_like_member",//NOI18N
                        userClassName(), methodName));
            return;
        }

//@olsen: disabled feature
/*
        boolean sawInitializeContents = false;
        boolean sawFlushContents = false;
        boolean sawClearContents = false;
*/
//@olsen: disabled feature
/*
        else if (methodName.equals("initializeContents") &&
                 methodSig.equals("(com/sun/forte4j/persistence/internal/ObjectContents;)V"))
            sawInitializeContents = true;
        else if (methodName.equals("flushContents") &&
                 methodSig.equals("(Lcom/sun/forte4j/persistence/internal/ObjectContents;)V"))
            sawFlushContents = true;
        else if (methodName.equals("clearContents") &&
                 methodSig.equals("()V"))
            sawClearContents = true;
*/
//@olsen: disabled feature
/*
        else if (methodName.equals("preDestroyPersistent") &&
                 methodSig.equals("()V"))
            needsPreDestroyPersistent = false;
        else if (methodName.equals("postInitializeContents") &&
                 methodSig.equals("()V"))
            needsPostInitializeContents = false;
        else if (methodName.equals("preFlushContents") &&
                 methodSig.equals("()V"))
            needsPreFlushContents = false;
        else if (methodName.equals("preClearContents") &&
                 methodSig.equals("()V"))
            needsPreClearContents = false;
*/
//@olsen: disabled feature
/*
            if (!sawInitializeContents)
                generate |= GENInitContents;
            if (!sawFlushContents)
                generate |= GENFlushContents;
            if (!sawClearContents)
                generate |= GENClearContents;
*/
    }

    /**
     * Add a field as the index'th element of the field vector in the class.
     */
    private void insertPersistenceCapableFields(String fieldName,
                                                String fieldSig,
                                                String printableFieldSig,
                                                int accessFlags) {
        affirm(implementsPersistence);

        control.noteUpdate();

        // create it
        env.message("adding "//NOI18N
                    + control.userClassName() +
                    "." + fieldName + " " + printableFieldSig);//NOI18N

        final ClassFile cfile = classFile();
        final ConstantPool pool = cfile.pool();

        //@olsen: fix 4467428, add synthetic attribute for generated fields
        final AttributeVector fieldAttrs = new AttributeVector();
        fieldAttrs.addElement(
            new SyntheticAttribute(
                pool.addUtf8(SyntheticAttribute.expectedAttrName)));

        final ClassField theField
            = new ClassField(accessFlags,
                             pool.addUtf8(fieldName),
                             pool.addUtf8(fieldSig),
                             fieldAttrs);

        cfile.addField(theField);
    }

    /**
     * Add all the methods required for com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable interface.
     */
    private void insertPersistenceCapableMethods() {
        affirm(implementsPersistence);

        control.noteUpdate();

        //@olsen: simplified generation of jdo[GS]etStateManager methods
        affirm(!sawMethodJDOGetStateManager);
        classFile().addMethod(
            methodBuilder.makeJDOGetStateManager(
                this,
                jdoGetStateManagerName));

        affirm(!sawMethodJDOSetStateManager);
        classFile().addMethod(
            methodBuilder.makeJDOSetStateManager(
                this,
                jdoSetStateManagerName));

        //@olsen: simplified generation of jdo[GS]etFlags methods
        affirm(!sawMethodJDOGetFlags);
        classFile().addMethod(
            methodBuilder.makeJDOGetFlags(
                this,
                jdoGetFlagsName));
        affirm(!sawMethodJDOSetFlags);
        classFile().addMethod(
            methodBuilder.makeJDOSetFlags(
                this,
                jdoSetFlagsName));

        //@olsen: add generation of jdoMakeDirty() method
        affirm(!sawMethodJDOMakeDirty);
        classFile().addMethod(
            methodBuilder.makeJDOMakeDirtyMethod(
                this,
                jdoMakeDirtyName));

        //@olsen: add generation of JDO interrogative methods
        affirm(!sawMethodJDOIsDirty);
        classFile().addMethod(
            methodBuilder.makeJDOInterrogativeMethod(
                this,
                jdoIsDirtyName));
        affirm(!sawMethodJDOIsTransactional);
        classFile().addMethod(
            methodBuilder.makeJDOInterrogativeMethod(
                this,
                jdoIsTransactionalName));
        affirm(!sawMethodJDOIsPersistent);
        classFile().addMethod(
            methodBuilder.makeJDOInterrogativeMethod(
                this,
                jdoIsPersistentName));
        affirm(!sawMethodJDOIsNew);
        classFile().addMethod(
            methodBuilder.makeJDOInterrogativeMethod(
                this,
                jdoIsNewName));
        affirm(!sawMethodJDOIsDeleted);
        classFile().addMethod(
            methodBuilder.makeJDOInterrogativeMethod(
                this,
                jdoIsDeletedName));

        //@olsen: add generation of jdoGetPersistenceManager method
        affirm(!sawMethodJDOGetPersistenceManager);
        classFile().addMethod(
            methodBuilder.makeJDOGetPersistenceManagerMethod(
                this,
                jdoGetPersistenceManagerName));

        //@olsen: add generation of jdoGetObjectId method
        affirm(!sawMethodJDOGetObjectId);
        classFile().addMethod(
            methodBuilder.makeJDOGetObjectIdMethod(
                this,
                jdoGetObjectIdName));

        //@olsen: add generation of the JDO constructor
        affirm(!sawMethodJDOConstructor);
        classFile().addMethod(
            methodBuilder.makeJDOConstructor(
                this,
                jdoConstructorName));

        //@olsen: add generation of the jdoNewInstance method
        affirm(!sawMethodJDONewInstance);
        classFile().addMethod(
            methodBuilder.makeJDONewInstanceMethod(
                this,
                jdoNewInstanceName));

        //@olsen: add generation of the jdoGetField method
        affirm(!sawMethodJDOGetField);
        classFile().addMethod(
            methodBuilder.makeJDOGetFieldMethod(
                this,
                jdoGetFieldName));

        //@olsen: add generation of the jdoSetField method
        affirm(!sawMethodJDOSetField);
        classFile().addMethod(
            methodBuilder.makeJDOSetFieldMethod(
                this,
                jdoSetFieldName));

        //@olsen: add generation of the jdoClear method
        affirm(!sawMethodJDOClear);
        classFile().addMethod(
            methodBuilder.makeJDOClearMethod(
                this,
                jdoClearName));

        //@lars: removed jdoCopy-method creation
        //@olsen: add generation of the jdoCopy method
        /*
        affirm(!sawMethodJDOCopy);
        classFile().addMethod(
            methodBuilder.makeJDOCopyMethod(
                this,
                jdoCopyName));
        */

        //@olsen: generate method clone() if not present
        if (!sawMethodJDOClone) {
            classFile().addMethod(
                methodBuilder.makeJDOClone(
                    this,
                    jdoCloneName));
        }
    }

    /**
     * Add all the methods required for com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapableHooks interface.
     */
//@olsen: disabled feature
/*
    private void insertPersistenceCapableHooksMethods() {
        if (needsPreDestroyPersistent)
            classFile().addMethod
                (methodBuilder.makeNullMethod(this, "preDestroyPersistent"));

        if (needsPostInitializeContents)
            classFile().addMethod
                (methodBuilder.makeNullMethod(this, "postInitializeContents"));

        if (needsPreFlushContents)
            classFile().addMethod
                (methodBuilder.makeNullMethod(this, "preFlushContents"));

        if (needsPreClearContents)
            classFile().addMethod
                (methodBuilder.makeNullMethod(this, "preClearContents"));

        if (needsPreDestroyPersistent || needsPostInitializeContents
            || needsPreFlushContents || needsPreClearContents)
            control.noteUpdate();
    }
*/

    /**
     * Add the specified interface to list.
     */
    private void augmentClassInterface(String interfaceName) {
        control.noteUpdate();
        ClassFile cfile = classFile();
        ConstClass iface = cfile.pool().addClass(interfaceName);
        //@olsen: moved output to here
        env.message("adding implements "//NOI18N
                    + ClassControl.userClassFromVMClass(interfaceName));
        cfile.addInterface(iface);
    }
}
