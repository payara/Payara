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

import java.io.*;

import java.util.Map;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.jdo.api.persistence.enhancer.classfile.ClassFile;
import com.sun.jdo.api.persistence.enhancer.classfile.ClassField;
import com.sun.jdo.api.persistence.enhancer.classfile.ClassMethod;
import com.sun.jdo.api.persistence.enhancer.classfile.ConstClass;

import com.sun.jdo.api.persistence.enhancer.util.Support;
import com.sun.jdo.api.persistence.enhancer.util.UserException;
import com.sun.jdo.api.persistence.enhancer.util.ClassFileSource;

//@olsen: added import
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;


//@olsen: cosmetics
//@olsen: moved: this class -> package impl
//@olsen: subst: [iI]Persistent -> [pP]ersistenceCapable
//@olsen: subst: jdo/ -> com/sun/forte4j/persistence/internal/
//@olsen: subst: /* ... */ -> // ...
//@olsen: subst: filterEnv -> env
//@olsen: subst: FilterEnv -> Environment
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: Hashtable -> Map, HashMap
//@olsen: subst: Enumeration,... -> Iterator, hasNext(), next()
//@olsen: subst: absolut jdo types and names -> constants from JDOMetaData
//@olsen: subst: persistType() -> persistenceType
//@olsen: subst: ClassControl. ->
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, affirm()
//@olsen: removed: support for [No]AnnotateField
//@olsen: removed: old, disabled ODI code


/**
 * ClassControl acts as the primary handle for a class within the
 * filter tool.  ClassControl instances are typically accessed through
 * various methods on Environment.
 */
public final class ClassControl
    extends Support {

    /* Valid persistence types */

    /* The user didn't tell us to make this persistence-capable or
     * persistence-aware and it doesn't appear to be annotated */
    //@olsen: changed value 3 -> -1
    static public final int TransientOnly = -1;

    /* The user made no explicit statement about the persistence capability. */
    static public final int PersistUnknown = 0;

    /* The user said that this class needs to be annotated for fetch/dirty
     * but the class doesn't need to be stored persistently */
    //@olsen: changed value 2 -> 1
    static public final int PersistAware = 1;

    /* The user said this class should be persistence capable or it was
     * promoted to persistence-capable status as a result of being a base
     * class of an explicitly persistence capable class */
    //@olsen: changed value 1 -> 2
    static public final int PersistCapable = 2;

    /* A filtered version of this class exists and is up to date w.r.t.
     * the unfiltered versionr - filtering will NOT be required.
     */
    //@olsen: changed value 3 -> -1
    static public final int UpdateNotNeeded = -1;

    /* It is not yet known whether the current filtered version of this
     * class exists or if it is up-to-date w.r.t. the unfiltered version.
     */
    static public final int UpdateUnknown = 0;

    /* No filtered version of this class exists - filtering will be required.
     */
    static public final int UpdateNew = 1;

    /* A filtered version of this class exists but the unfiltered version
     * is newer - filtering will be required.
     */
    static public final int UpdateNewer = 2;

    /* Central repository for the options and classes */
    //@olsen: added association
    //@olsen: made final
    private final Environment env;

    /* The class to be annotated */
    //@olsen: made final
    private final ClassFileSource theClassSource;

    /* The classfile to be annotated */
    private ClassFile theClass;

    /* What type of class is this w.r.t. persistence */
    private int persistenceType = PersistUnknown;

    /* What type of class is this w.r.t. persistence as defined on cmd line */
//@olsen: disabled feature
/*
    private int initialPersistenceType = PersistUnknown;
*/

    /* Was this class promoted to persistent capable? */
//@olsen: disabled feature
/*
    private boolean implicitlyPersistent = false;
*/

    /* Was this class explicitly listed on the command line? */
//@olsen: disabled feature
/*
    private boolean explicitlyMentioned = false;
*/

    /* What is the update state of the class */
    private int updateType = UpdateUnknown;

    /* What annotation related actions are to be performed for the class */
    //@olsen: made final
    private final ClassAction classAction;

    /* If true, this class is believed to have been modified in some way */
    private boolean classUpdated;

    /* If true, this class was renamed via repackaging */
//@olsen: disabled feature
/*
    private boolean classRenamed;
*/

    // public accessors

    /**
     * Return true if the classfile has been updated
     */
    public boolean updated() {
        return classUpdated;
    }

    /**
     * Return true if the classfile has been renamed
     */
//@olsen: disabled feature
/*
    public boolean renamed() {
        return classRenamed;
    }
*/

    /**
     * Record a modification of the class
     */
    public void noteUpdate() {
        classUpdated = true;
    }

    /**
     * Get the ClassFile data structure for the class
     */
    public ClassFile classFile() {
        return theClass;
    }

    /**
     * Return the persistence type for this class
     */
    public int persistType() {
        return persistenceType;
    }

    /**
     * Return the initial persistence type for this class
     */
//@olsen: disabled feature
/*
    public int initialPersistType() {
        return initialPersistenceType;
    }
*/

    /**
     * Return true if the persistence type for this class is either
     * PersistAware or PersistCapable.
     */
//@olsen: not used
//@olsen: disabled feature
/*
    public boolean persistAware() {
        if (persistenceType == PersistUnknown)
            checkPersistAware();
        return (persistenceType == PersistCapable ||
                persistenceType == PersistAware);
    }
*/

    /**
     * Return true if the persistence type for this class is PersistCapable.
     */
//@olsen: disabled feature
/*
    public boolean persistCapable() {
        if (persistenceType == PersistUnknown)
            checkPersistAware();
        return persistenceType == PersistCapable;
    }
*/

    /**
     * Return true if filtering of the class is required.
     * This checks only whether the filtered version of the class is
     * up to date w.r.t. its input class.
     */
    public boolean filterRequired() {
        if (updateType == UpdateUnknown)
            checkUpdateType();
//@olsen: optimized code
        return (updateType > UpdateUnknown || env.forceOverwrite());
/*
        return (updateType == UpdateNew
                || updateType == UpdateNewer
                || env.forceOverwrite());
*/
    }

    /**
     * Mark the class as requiring update.
     */
    public void requireUpdate() {
        updateType = UpdateNew;
    }

    /**
     * Return true if the class is one which should be a candidate for
     * annotation.
     */
    public boolean annotateable() {
//@olsen: disabled feature: isImplicitlyPersistent()
/*
        return (initialPersistenceType == PersistCapable ||
                initialPersistenceType == PersistAware ||
                isImplicitlyPersistent());
*/
        //@olsen: changed to check each class not explicitly known as transient
        return (persistenceType >= PersistUnknown);
    }

    /**
     * Set the peristence type for this class
     */
//@olsen: disabled feature: -> setPersistenceType()
/*
    public void setPersistType(int ptype) {
        persistenceType = ptype;
    }
*/

    /**
     * Set the initial peristence type for this class
     */
//@olsen: disabled feature
/*
    public void setInitialPersistType(int ptype) {
        initialPersistenceType = ptype;
        persistenceType = ptype;
    }
*/

    /**
     * Returns true if this class was promoted to persistent implicitly
     */
//@olsen: disabled feature
/*
    public boolean isImplicitlyPersistent() {
        return implicitlyPersistent;
    }
*/

    /**
     * Record that this class was promoted to persistence capable
     */
//@olsen: disabled feature
/*
    public void setImplicitlyPersistent(boolean wasPromoted) {
        implicitlyPersistent = wasPromoted;
    }
*/

    /**
     * Returns true if this class was explicitly named on the command line
     */
//@olsen: disabled feature
/*
    public boolean isExplicitlyNamed() {
        return explicitlyMentioned;
    }
*/

    /**
     * Record whether this class was explicitly listed as a class on the
     * command line.
     */
//@olsen: disabled feature
/*
    public void setExplicitlyNamed(boolean wasNamed) {
        explicitlyMentioned = wasNamed;
    }
*/

    /**
     * Get the ClassAction for this class
     */
    public ClassAction action() {
        return classAction;
    }

    /**
     * Constructor
     */
    //@olsen: added parameter 'env' for association
    public ClassControl(ClassFileSource theSource,
                        Environment env) {
        theClassSource = theSource;
        buildTheClass(true /* allowJDK12ClassFiles */);
        theClassSource.setExpectedClassName(className());
        classAction = new ClassAction(this, env);
        this.env = env;

        if (false) {
            System.out.println("ClassControl(): new class = " + className());//NOI18N
        }
    }

    /**
     * Constructor
     */
    //@olsen: added parameter 'env' for association
    public ClassControl(ClassFileSource theSource,
                        ClassFile theFile,
                        Environment env) {
        theClassSource = theSource;
        theClass = theFile;
        theClassSource.setExpectedClassName(className());
        classAction = new ClassAction(this, env);
        this.env = env;

        if (false) {
            System.out.println("ClassControl(): new class = " + className());//NOI18N
        }
    }


    /**
     * Sets the persistence type of a class by JDO meta-data.
     */
    //@olsen: added method
    private void setPersistenceType() {
        final JDOMetaData meta = env.getJDOMetaData();

        //@olsen: skip class if its persistence type is already known
        if (persistenceType != PersistUnknown) {
            return;
        }

        //@olsen: check whether class is an interface
        if (classFile().isInterface()) {
            persistenceType = TransientOnly;
            return;
        }

        //@olsen: check whether class is known to be transient
        final String className = className();
        if (meta.isTransientClass(className)) {
            persistenceType = TransientOnly;
            return;
        }

        //@olsen: check whether class is persistence-capable
        if (meta.isPersistenceCapableClass(className)) {
            persistenceType = PersistCapable;

            //@olsen: for Dogwood, check limitation on PC-inheritance
            affirm(meta.isPersistenceCapableRootClass(className),
                   ("Sorry, not supported yet: the persistent-capable class "//NOI18N
                    + userClassName()
                    + "cannot extend a persistent-capable super-class."));//NOI18N
        }
    }

    /**
     * Note the class characteristics
     */
    public void scan1() {
        //@olsen: added support for timing statistics
        try{
            if (env.doTimingStatistics()) {
                Support.timer.push("ClassControl.scan1()");//NOI18N
            }
            //@olsen: added: set the persistent type of class
            setPersistenceType();

//@olsen: disabled feature
/*
            if (annotateable() || implementsPersistenceCapable()) {
*/

            if (annotateable()) {
                //@olsen: dropped argument: boolean filterRequired
                //classAction.scan1(filterRequired());
                if (filterRequired()) {
                    classAction.scan1();
                } else {
                    //@olsen: added output
                    env.message("skipping " + userClassName() + //NOI18N
                                " because it is already up to date.");//NOI18N
                }
            }
        } finally {
            if (env.doTimingStatistics()) {
                Support.timer.pop();
            }
        }
    }

    /**
     * Check the class to see what actions need to be performed
     */
//@olsen: disabled feature
/*
    public void scan2() {
        if (annotateable()) {
            env.message("computing annotations for " + userClassName());
            classAction.scan2(filterRequired());
        }
    }
*/

    /**
     * Reparent class if needed
     */
    //@olsen: subst: augmentInterfaces -> augment
    public void augment() {
        //@olsen: added support for timing statistics
        try{
            if (env.doTimingStatistics()) {
                Support.timer.push("ClassControl.augment()");//NOI18N
            }
            if (annotateable()) {
                //@olsen: dropped argument: boolean filterRequired
                //classAction.augment(filterRequired());
                if (filterRequired()) {
                    classAction.augment();
                } else {
                    //@olsen: added output
                    env.message("skipping " + userClassName() + //NOI18N
                                " because it is already up to date.");//NOI18N
                }
            }
        } finally {
            if (env.doTimingStatistics()) {
                Support.timer.pop();
            }
        }
    }

    /**
     * Retarget the class if needed
     */
//@olsen: disabled feature
/*
    public void retarget(Map classTranslations) {
        // Trust our caller on this one
        // The check for filterRequired is pushed down into ClassAction
        // for this method to allow any special considerations to be made
        // there.
        if (classTranslations.get(className()) != null)
            classRenamed = true;

        env.message("retargetting class references for " + userClassName());
        classAction.retarget(classTranslations,
                             filterRequired());

        // update our class source to reflect our new name if it has changed
        // We need to do this even if filtering is not required
        theClassSource.setExpectedClassName(className());
    }
*/

    /**
     * perform necessary annotation actions on the class
     */
    public void annotate() {
        //@olsen: added support for timing statistics
        try{
            if (env.doTimingStatistics()) {
                Support.timer.push("ClassControl.annotate()");//NOI18N
            }
            if (annotateable()) {
                if (filterRequired()) {
                    classAction.annotate();
                } else {
                    env.message("skipping " + userClassName() + //NOI18N
                                " because it is already up to date.");//NOI18N
                }
            }
        } finally {
            if (env.doTimingStatistics()) {
                Support.timer.pop();
            }
        }
    }

    // package accessors

    /**
     * Return the class name in VM form
     */
    public String className() {
        ConstClass cname = theClass.className();
        return (cname == null) ? null : cname.asString();
    }

    /**
     * Return the class name in user ('.' delimited) form
     */
    public String userClassName() {
        return userClassFromVMClass(className());
    }

    /**
     * Return the class name in user ('.' delimited) form
     */
    //^olsen: move to -> classfile.Descriptor ?
    static public String userClassFromVMClass(String vmName) {
        return vmName.replace('/', '.');
    }

    /**
     * Return the class name in VM ('/' delimited) form
     */
    //^olsen: move to -> classfile.Descriptor ?
    static public String vmClassFromUserClass(String userName) {
        return userName.replace('.', '/');
    }

    /**
     * Return the vm package name for this class
     */
    public String pkg() {
        return packageOf(className());
    }

    /**
     * Return the vm package name for the vm class name
     */
    static public String packageOf(String vmName) {
        int last = vmName.lastIndexOf('/');
        if (last < 0)
            return "";//NOI18N
        return vmName.substring(0, last);
    }

    /**
     * Return the unpackaged  name for this class
     */
//@olsen: disabled feature
/*
    public String unpackagedName() {
        return unpackagedNameOf(className());
    }
*/

    /**
     * Return the unpackaged name for the vm class name
     */
//@olsen: disabled feature
/*
    static public String unpackagedNameOf(String vmName) {
        int last = vmName.lastIndexOf('/');
        if (last < 0)
            return vmName;
        return vmName.substring(last+1);
    }
*/

    /**
     * Return the name of the class source
     */
    public String sourceName() {
        return theClassSource.containingFilePath();
    }

    /**
     * Return the source of the class
     */
    public ClassFileSource source() {
        return theClassSource;
    }

    /**
     * Check whether the class already implements PersistenceCapable.
     * Drag in new classes if needed.
     */
//@olsen: disabled feature
/*
    public boolean implementsPersistenceCapable() {
        return implementsInterface(JDOMetaData.JDOPersistenceCapablePath,
                                   null);
    }
*/

//@olsen: disabled feature
/*
    abstract class Checker {
        abstract boolean checkClass(ClassControl aClass);
    }
*/

    /**
     * Check whether the class will implement PersistenceCapableHooks
     * in the absence of the postprocessor doing it.
     * Drag in new classes if needed.  Must not be called until scan2 phase
     */
//@olsen: disabled feature
/*
    public boolean hasPersistenceCapableHooksProvided() {
        return implementsInterface(JDOMetaData.JDOInstanceCallbacksPath,
                                   new Checker() {
                                           boolean checkClass(ClassControl aClass) {
                                               return (aClass != ClassControl.this &&
                                                       aClass.action().getImplementsPersistenceHooks());
                                           }
                                       });
    }
*/

    /**
     * Check whether the class implements the specified class or
     * is the specified class.
     * If ccCheck is non-null, it is called on each ClassControl visited
     * during the traversal to the base class as an additional predicate
     * which, if it returns true, causes termination of the walk with
     * a return value of true.
     *
     * TBD: modify this method and its callers to move all checking to
     * the Checker predicate.
     */
//@olsen: disabled feature
/*
    boolean implementsInterface(String implementClassName,
                                Checker ccChecker) {
        String currClassName = className();
        while (currClassName != null) {
            ClassControl cc = env.findClass(currClassName);
            if (cc == null)
                return false;

            ClassFile cf = cc.classFile();

            Iterator interfaces = cf.interfaces().iterator();
            while (interfaces.hasNext()) {
                ConstClass i = (ConstClass)interfaces.next();
                String interfaceName = i.asString();
                ClassControl icc = env.findClass(interfaceName);
                if (icc == null) {
                    env.error("Class " + interfaceName
                              + " could not be found.");
                    return false;
                }
                if (interfaceName.equals(implementClassName) ||
                    icc.implementsInterface(implementClassName, ccChecker))
                    return true;
            }

            if (ccChecker != null && ccChecker.checkClass(cc))
                return true;

            ConstClass superClass = cf.superName();
            if (superClass == null)
                // java/lang/Object has no super class
                return false;

            currClassName = superClass.asString();
        }

        return false;
    }
*/

    /**
     * Check whether the class derives from the specified class or
     * is the specified class.
     */
//@olsen: disabled feature
/*
    //@olsen: made public
    public boolean inherits(String inheritClassName) {
        String currClassName = className();
        while (currClassName != null) {
            if (currClassName.equals(inheritClassName))
                return true;

            ClassControl cc = env.findClass(currClassName);
            if (cc == null)
                return false;

            ClassFile cf = cc.classFile();
            ConstClass superClass = cf.superName();
            if (superClass == null)
                // java/lang/Object has no super class
                return false;

            currClassName = superClass.asString();
        }

        return false;
    }
*/

    /**
     * Find the class which implements the given method and signature.
     * Search up the class hierarchy from "this" until either java.lang.Object
     * is found or a class which implements the method is found.  Return
     * null if the method has no implementation.
     */
//@olsen: disabled feature
/*
    ClassControl findMethodClass(String methodName,
                                 String methodSig) {
        String currClassName = className();

        while (currClassName != null) {
            ClassControl cc = env.findClass(currClassName);
            if (cc == null)
                break;

            ClassFile cf = cc.classFile();
            if (cf.findMethod(methodName, methodSig) != null)
                return cc;
            ConstClass superClass = cf.superName();
            if (superClass == null)
                // java/lang/Object has no super class
                return null;

            currClassName = superClass.asString();
        }
        env.error("Class " + userClassFromVMClass(currClassName) +
                  " could not be found while trying to locate method " +
                  methodName);

        return null;
    }
*/

//@olsen: disabled feature -> get this information from JDOMetaData
/*
    ClassControl findBasestPersistCapable() {
        String currClassName = className();

        ClassControl ret = null;
        while (currClassName != null) {
            ClassControl cc = env.findClass(currClassName);
            if (cc == null)
                break;

            if (cc.persistCapable()) {
                ret = cc;
            }
            ConstClass superClass = cc.classFile().superName();
            if (superClass == null)
                // java/lang/Object has no super class
                return ret;

            currClassName = superClass.asString();
        }
        env.error("Class " + userClassFromVMClass(currClassName) +
                  " could not be found while trying to locate Base class.");

        return null;
    }
*/

    /**
     * Write the file.  If destination directory is non-null, write the
     * file relative to that directory, else write it relative to its
     * original location.
     */
    //@olsen: made public
    public void write(File destFile)
        throws IOException, FileNotFoundException {
        DataOutputStream dos =
            theClassSource.getOutputStream(destFile);
        theClass.write(dos);
        dos.close();
    }

    // private methods

    /**
     * Check to see if this class appears to be annotated.  If so,
     * update PersistUnknown to PersistAware.
     */
//@olsen: disabled feature
/*
    private void checkPersistAware() {
        if (persistenceType == PersistUnknown) {
            if (action().hasAnnotatedAttribute()) {
                // The class is annotated - it is either
                // persistence-aware or persistence-capable.
                if (implementsPersistenceCapable())
                    persistenceType = PersistCapable;
                else
                    persistenceType = PersistAware;
            } else {
                // Either unannotated or manually annotated.
                // We used to check for the presence of a ClassInfo class
                // but the fact that we are relying now on dynamic classinfo
                // means that we encounter increasing numbers of false
                // warnings so for now, we'll just assume that the
                // PersistenceCapable is sufficient and defer error detection
                // until runtime.
                if (implementsPersistenceCapable()
                    || classFile().isInterface())
                    persistenceType = PersistCapable;

                // This code still needs to be implemented but not being
                // implemented simply means that more array fetches might
                // occur than are really necessary.
                //else if (this class appears to be annotated)
                //    persistenceType = PersistAware;
                else
                    persistenceType = TransientOnly;
            }
        }
    }
*/

    /**
     * Check to see if output class for this class is up-to-date with
     * respect to its input class.  Set updateType to reflect what
     * type of update of the output class is needed, if any.
     */
    private void checkUpdateType() {
        if (updateType == UpdateUnknown) {
            String lookupName;

            //@olsen: added inplace of disable featured
            lookupName = className();
//@olsen: disabled feature
/*
            // check to see if it exists in the updated form
            String pkg = pkg();
            String xlat = (String) env.translations().get(pkg);
            if (xlat != null) {
                if (xlat.length() == 0)
                    lookupName = unpackagedName();
                else
                    lookupName = xlat + "/" + unpackagedName();
            } else {
                lookupName = className();
            }
*/

            ClassFileSource annotatedSource;
            if (env.updateInPlace() && !theClassSource.isZipped())
                annotatedSource = theClassSource;
            else
                annotatedSource = env.lookupDestClass(lookupName);

            if (annotatedSource == null) {
                // No annotated class exists
                updateType = UpdateNew;
            } else {
                try {
                    long annModDate = annotatedSource.modificationDate();
                    long srcModDate = source().modificationDate();
                    if (annModDate < srcModDate) {
                        // An annotated class exists, but it is older than
                        // the input
                        updateType = UpdateNewer;
                    } else {
                        if (annotatedSource == theClassSource
                            && !action().hasAnnotatedAttribute()) {
                            // An unannotated class file to be updated
                            // in-place
                            if (persistenceType == PersistCapable
                                || persistenceType == PersistAware)
                                updateType = UpdateNewer;
                            else
                                updateType = UpdateNotNeeded;
                        } else {
                            //@olsen: an annotated class exists, which is
                            // newer than the input
                            updateType = UpdateNotNeeded;
                        }
                    }
                } catch (FileNotFoundException e) {
                    // shouldn't occur, but if it does, handle it
                    updateType = UpdateNew;
                }
            }
        }
    }

    /**
     * Constructs the ClassFile for the class
     */
    private void buildTheClass(boolean allowJDK12ClassFiles) {
        if (false) {
            System.out.println("Reading class "//NOI18N
                               + theClassSource.expectedClassName());
        }

        //@olsen: cosmetics
        //try {
        //    try {
        //    } catch (FileNotFoundException e) {
        //    }
        //} catch (IOException e) {
        //}
        try {
            DataInputStream dis = theClassSource.classFileContents();
            theClass = new ClassFile(dis);
            dis.close();
        } catch (FileNotFoundException e) {
            // File should already have been tested for existence
            //@olsen: support for I18N
            throw new UserException(
                getI18N("enhancer.file_not_found",//NOI18N
                        sourceName()),
                e);
        } catch (IOException e) {
            //@olsen: support for I18N
            throw new UserException(
                getI18N("enhancer.io_error_while_reading_file",//NOI18N
                        sourceName()),
                e);
        }
    }
}
