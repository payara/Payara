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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;

import java.io.File;
//@olsen: added import
import java.io.PrintWriter;

import com.sun.jdo.api.persistence.enhancer.classfile.ClassFile;
import com.sun.jdo.api.persistence.enhancer.classfile.ClassField;

import com.sun.jdo.api.persistence.enhancer.util.ClassPath;
import com.sun.jdo.api.persistence.enhancer.util.ClassFileSource;
import com.sun.jdo.api.persistence.enhancer.util.Support;

//@olsen: added import
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;


//@lars added: field for last error message
//@olsen: cosmetics
//@olsen: moved: this class -> package impl
//@olsen: subst: /* ... */ -> // ...
//@olsen: make fields private
//@olsen: subst: FilterEnv -> Environment
//@olsen: subst: collectAllClasses -> collectClasses
//@olsen: subst: Vector -> Collection, ArrayList
//@olsen: subst: Enumeration,... -> Iterator, hasNext(), next()
//@olsen: subst: control -> cc
//@olsen: added: support for I18N
//@olsen: subst: FilterError -> UserException, assert()
//@olsen: removed: proprietary support for HashCode
//@olsen: removed: proprietary support for TypeSummary
//@olsen: removed: proprietary support for ClassInfo
//@olsen: removed: proprietary support for IndexableField
//@olsen: removed: support for IgnoreTransientField, AddedTransientField
//@olsen: removed: support for [No]AnnotateField
//@olsen: removed: old, disabled ODI code

//^olsen: move: ClassControl+ClassPath handling


/**
 * Environment serves as a central collection for the options and
 * working environment of the filter tool.
 */
public final class Environment
    extends Support {

    /* Writer for regular program output and warnings. */
    //@olsen: added field
    private PrintWriter out = new PrintWriter(System.out, true);

    /* Writer for error output. */
    //@olsen: added field
    private PrintWriter err = new PrintWriter(System.err, true);

    /* If true, provide timing statistics */
    //@olsen: added support for timing statistics
    private boolean timingOption = false;

    /* If true, provide verbose output */
    private boolean verboseOption = false;

    /* If true, squash warnings */
    private boolean quietOption = false;

    /* The level of debugging detail to provide - not currently used */
//@olsen: disabled feature
/*
    private int dumpLevel = 0;
*/

    /* If true, perform only a dry run - no output is written to disk */
    private boolean noWriteOption = false;

    /* If true, allow java classes to be modified for persistence */
//@olsen: disabled feature
/*
    private boolean modifyJavaClassesOption = false;
*/

    /* If true, allow final fields to be updated by initializeContents */
//@olsen: disabled feature
/*
    private boolean allowFinalFieldModifications = false;
*/

    /* If true, disable hoisting of dirty(this), fetch(this) to the start
       of a method */
    private boolean disableThisHookHoisting;

    /* If true, disable suppression of constructor annotation */
    private boolean disableInitializerAnnotationSuppression;

    /* If true, disable in-loop array caching */
//@olsen: disabled feature
/*
    private boolean disableArrayHookCaching;
*/

    /* If true, disable element type-specific array fetching */
    /* This knob is temporary */
//@olsen: disabled feature
/*
    private boolean disableArrayElementFetch = false;
*/

    /* If true, forces overwriting all output files. */
    private boolean forceOverwriteOption = false;

    /* If true, perform class file updates in-place rather than to the
       destination directory. */
    private boolean updateInPlaceOption = false;

    /* The number of errors encountered thus far */
    private int errorsEncountered = 0;

    /* The out directory specified */
    private File destinationDirectory = null;

    /* Hash VM class name to ClassControl */
    private Hashtable classMap = new Hashtable(11);

    /* Set of classes that were looked up but not found. */
    //@olsen: subst: Hashtable -> HashMap
    private HashMap missingClasses = new HashMap(11);

    /* Hash VM class name to ClassControl
     * Entries in this table have been renamed and the class control
     * reflects the updated name */
//@olsen: inlined method
/*
    private Hashtable renamedMap = new Hashtable(203);
*/

    /* explicit package name translations
     * Maps string to string */
//@olsen: disabled feature
/*
    private Hashtable translations = new Hashtable(11);
*/

    /* Search path to be used for locating classes */
    //@olsen: added default initialization
    private ClassPath classPathOption = new ClassPath("");//NOI18N

    /* Search path to be used for locating annotated classes in output dir */
    private ClassPath destClassPath;

    /* A set of fully qualified field names (maps name to itself) */
    private Hashtable fieldSuppressions = new Hashtable();

    /* A set of fully qualified class names (maps name to itself) */
    private Hashtable classSuppressions = new Hashtable();

    /* The instance providing the JDO meta data. */
    //@olsen: added field
    private JDOMetaData jdoMetaData;

    /* Last error message */
    private String lastErrorMessage = null;

    // public accessors

    public void setDoTimingStatistics(boolean dontOpt) {
        timingOption = dontOpt;
    }

    public boolean doTimingStatistics() {
        return timingOption;
    }

    //@olsen: subst: param err -> error
    public void error(String error) {
        errorsEncountered++;
        //@olsen: support for I18N
        //@olsen: redirected output
        //System.out.print("Error: ");
        //System.out.println(err);
        err.println(lastErrorMessage = getI18N("enhancer.enumerated_error",
                                               errorsEncountered,
                                               error));
    }

    public void warning(String warn) {
        if (!quietOption) {
            //@olsen: support for I18N
            //@olsen: redirected output
            //System.out.print("Warning: ");
            //System.out.println(warn);
            out.println(getI18N("enhancer.warning", warn));//NOI18N
        }
    }

    public void warning(String warn, String classname) {
        if (!quietOption &&
            !classWarningsSuppressed(classname)) {
            //@olsen: support for I18N
            //@olsen: redirected output
            //System.out.print("Warning: ");
            //System.out.println(warn);
            out.println(getI18N("enhancer.warning", warn));//NOI18N
        }
    }

    public void warning(String warn, String classname, String fieldname) {
        if (!quietOption &&
            !classWarningsSuppressed(classname) &&
            !fieldWarningsSuppressed(classname, fieldname)) {
            //@olsen: support for I18N
            //@olsen: redirected output
            //System.out.print("Warning: ");
            //System.out.println(warn);
            out.print(getI18N("enhancer.warning", warn));//NOI18N
        }
    }

    public void message(String mess) {
        if (verboseOption) {
            //@olsen: redirected output
            //System.out.println(mess);
            out.println("JDO ENHANCER: " + mess);//NOI18N
        }
    }

    public void messageNL(String mess) {
        if (verboseOption) {
            //@olsen: redirected output
            //System.out.println(mess);
            out.println();
            out.println("JDO ENHANCER: " + mess);//NOI18N
        }
    }

    public int errorCount() {
        return errorsEncountered;
    }

    public final String getLastErrorMessage () {
        return this.lastErrorMessage;
    }

//@olsen: disabled feature
/*
    public boolean verbose() {
        return verboseOption;
    }

    public boolean quiet() {
        return quietOption;
    }
*/

    public boolean forceOverwrite() {
        return forceOverwriteOption;
    }

    public boolean updateInPlace() {
        return updateInPlaceOption;
    }

    //@olsen: added method
    public File destinationDirectory() {
        return destinationDirectory;
    }

//@olsen: disabled feature
/*
    //@olsen: added method
    public Hashtable translations() {
        return translations;
    }
*/

    //@olsen: added method
//@olsen: disabled feature
/*
    public ClassPath classPathOption() {
        return classPathOption;
    }
*/

    /**
     * Expected dump levels are 0, 1, 2, 3
     * dump level 0 is always on.
     */
//@olsen: disabled feature
/*
    public boolean dump(int level) {
        return dumpLevel >= level;
    }
*/

    public boolean writeClasses() {
        return (noWriteOption == false && errorsEncountered == 0);
    }

//@olsen: disabled feature
/*
    public boolean doArrayOptimization() {
        return disableArrayHookCaching == false;
    }
*/

    public boolean doThisOptimization() {
        return disableThisHookHoisting == false;
    }

//@olsen: disabled feature
/*
    public boolean doArrayElementFetch() {
        return disableArrayElementFetch == false;
    }
*/

    public boolean doInitializerOptimization() {
        return disableInitializerAnnotationSuppression == false;
    }

//@olsen: disabled feature
/*
    public boolean modifyJavaClasses() {
        return modifyJavaClassesOption;
    }
*/

//@olsen: disabled feature
/*
    public boolean allowFinalModifications() {
        return allowFinalFieldModifications;
    }
*/

    /**
     * Is the class a well known persistent capable class?  These are
     * normally the java primitives.
     */
    //@olsen: subst: isKnownPersistent -> JDOMetaData.isSecondClassObjectType
/*
    public boolean isKnownPersistent(String className) {
        if (className.equals("java/lang/String") ||
            className.equals("java/lang/Integer") ||
            className.equals("java/lang/Number") ||
            className.equals("java/lang/Short") ||
            className.equals("java/lang/Byte") ||
            className.equals("java/lang/Long") ||
            className.equals("java/lang/Float") ||
            className.equals("java/lang/Double") ||
            className.equals("java/lang/Character") ||
            className.equals("java/lang/Boolean"))
            return true;
        return false;
    }
*/

    /* The instance providing the JDO meta data. */
    //@olsen: added method
    public JDOMetaData getJDOMetaData()
    {
        return jdoMetaData;
    }

    /* Set the instance providing the JDO meta data. */
    //@olsen: added method
    public void setJDOMetaData(JDOMetaData jdoMetaData)
    {
        this.jdoMetaData = jdoMetaData;
    }

    /**
     * Add a newly created transient class to the list of classes.
     * Its source should be "near" friend.  That is, if the friend
     * is in a zip file, place this in the same zip file.  Else if it
     * is in an individual class file, place this in a class file in
     * the same directory.
     */
//@olsen: disabled feature
/*
    public ClassControl addClass(ClassFile newClass, ClassFileSource friend) {
        String className = newClass.className().asString();
        ClassFileSource source = friend.friendSource(className);
        ClassControl cc = new ClassControl(source, newClass, this);
        cc.noteUpdate();
        cc.setPersistType(ClassControl.PersistUnknown);
        classMap.put(className, cc);
        return cc;
    }
*/

    /**
     * Add a newly created transient class to the list of classes.
     * Its source is undefined.
     */
//@olsen: disabled feature
/*
    public ClassControl addClass(ClassFile newClass) {
        String className = newClass.className().asString();
        ClassFileSource source = new ClassFileSource(className, (File)null);
        ClassControl cc = new ClassControl(source, newClass, this);
        cc.noteUpdate();
        cc.setPersistType(ClassControl.PersistUnknown);
        classMap.put(className, cc);
        return cc;
    }
*/

    /**
     * Add the class to the class mapping table.  Check that it does
     * not conflict with earlier settings.
     */
    //@olsen: moved: OSCFP.addClass(ClassControl) -> impl.Environment
    public void addClass(ClassControl cc) {
        String className = cc.className();
        ClassControl existCC = getClass(className);

        if (existCC != null) {

            if (!existCC.source().sameAs(cc.source())) {
                //@olsen: support for I18N
                error(getI18N("enhancer.class_already_entered",//NOI18N
                              cc.userClassName(),
                              cc.sourceName(),
                              existCC.sourceName()));
                return;
            }

            // the two files are from the same source - select the higher
            // level of persistence capability and discard the other
            if (cc.persistType() == ClassControl.PersistUnknown ||
                existCC.persistType() == ClassControl.PersistCapable ||
                (existCC.persistType() == ClassControl.PersistAware &&
                 cc.persistType() != ClassControl.PersistCapable))
                return;

        }

        if (existCC == null && cc.sourceName() != null)
            message("adding class " + cc.userClassName() +//NOI18N
                    " from " + cc.sourceName());//NOI18N

        classMap.put(className, cc);
    }

    /**
     * Add the modified name to the class map if the class name has changed.
     */
    //@olsen: added method
//@olsen: disabled feature
/*
    public void renameClass(String oldClassName) {
        ClassControl cc = (ClassControl)classMap.remove(oldClassName);
        String newClassName = cc.className();
        renamedMap.put(oldClassName, cc);
        classMap.put(newClassName, cc);
    }
*/

    /**
     * Look for the specified class in the class map.  If not there,
     * use the class path to find the class.  If still not found,
     * return false.
     */
    public boolean canFindClass(String className) {
        return findClass(className) != null;
    }

    /**
     * Look for the specified class in the class map.  No other class
     * lookup is performed.  Use this only if you are certain that the
     * class will have been found.
     */
    public ClassControl getClass(String className) {
        return (ClassControl)classMap.get(className);
    }

    //@olsen: added method
    public Iterator getClasses() {
        return classMap.values().iterator();
    }

    /**
     * Look for the specified class in the class map.  If not there,
     * use the class path to find the class.  If still not found,
     * return false.
     */
    public ClassControl findClass(String className) {
        ClassControl cc = (ClassControl) classMap.get(className);

        if ((cc == null) && (missingClasses.get(className) == null)) {

            // Not already known - try looking up in class path
            cc = lookupClass(className);
            if (cc != null) {
                message("Reading class " + cc.userClassName() +//NOI18N
                        " from " + cc.sourceName());//NOI18N
                classMap.put(className, cc);
            } else {
                missingClasses.put(className, className);
            }
        }

        return cc;
    }

    /**
     * Look up the specified class in the class search path.  Callers
     * should normally consult the classmap prior to calling this function.
     * The class is not entered into the classmap
     */
    public ClassControl lookupClass(String className) {
        ClassFileSource source = classPathOption.findClass(className);

        while (true) {
            if (source == null)
                return null;

            //@olsen: cosmetics
            try {
                ClassControl cc = new ClassControl(source, this);
                if (cc.className() != null &&
                    cc.className().equals(className))
                    return cc;
            } catch (ClassFormatError e) {
            }

            // Try to find an alternate source for the class
            source = source.nextSource(className);
        }
    }


    /**
     * Look for the specified class in the renamed class map.
     * No classpath searching is done.
     */
//@olsen: disabled feature
/*
    public ClassControl getRenamedClass(String className) {
        return (ClassControl) renamedMap.get(className);
    }
*/

    /**
     * Return a ArrayList of ClassControl objects which have the specified
     * persistence type
     */
    public ArrayList collectClasses(int persistType) {
        ArrayList v = new ArrayList();
        for (Iterator e = classMap.values().iterator(); e.hasNext();) {
            ClassControl cc = (ClassControl)e.next();
            if (cc.persistType() == persistType)
                v.add(cc);
        }
        return v;
    }

    /**
     * Return an ArrayList of the ClassControls in classMap.
     * This is useful in that it provides a stable base for enumeration.
     */
    public ArrayList collectClasses() {
        ArrayList v = new ArrayList();
        for (Iterator e = classMap.values().iterator(); e.hasNext(); )
            v.add(e.next());
        return v;
    }


    /**
     * Look for a class source using the destination directory as a
     * root directory for the lookup which represents the annotated output
     * for the class specified.  Return null if not found.
     */
    public ClassFileSource lookupDestClass(String className) {
        if (destClassPath == null && destinationDirectory != null)
            destClassPath = new ClassPath(destinationDirectory.getPath());
        return (destClassPath == null
                ? null : destClassPath.findClass(className));
    }

    // package local methods


    /**
     * The constructor
     */
    public Environment() {
    }

//@olsen: disabled feature
/*
    public void setClassPath(String path) {
        message("setting class path to " + path);
        classPathOption = new ClassPath(path);
    }
*/

    public void setDestinationDirectory(String dir) {
        final File dest = new File(dir);
        if (destinationDirectory != null) {
            //@olsen: support for I18N
            error(getI18N("destination_directory_already_set",//NOI18N
                          dir,
                          destinationDirectory.getPath()));
            return;
        }
        if (!dest.isDirectory()) {
            error(getI18N("enhancer.destination_directory_not_exist",//NOI18N
                          dir));
            return;
        }
        destinationDirectory = dest;
    }

    /**
     * Update the class path to remove the destination directory if it
     * is found in the class path.
     */
    public void excludeDestinationDirectory() {
        if (destinationDirectory != null)
            classPathOption.remove(destinationDirectory);
    }

    /**
     * Update the class path to move the destination directory to the
     * end of the class path if it is found in the class path.
     */
    public void moveDestinationDirectoryToEnd() {
        if (destinationDirectory != null &&
            classPathOption.remove(destinationDirectory))
            classPathOption.append(destinationDirectory);
    }

    //@olsen: added method
    public void setOutputWriter(PrintWriter out) {
        this.out = out;
    }

    //@olsen: added method
    public PrintWriter getOutputWriter() {
        return out;
    }

    //@olsen: added method
    public void setErrorWriter(PrintWriter err) {
        this.err = err;
    }

    //@olsen: added method
    public PrintWriter getErrorWriter() {
        return err;
    }

    public void setVerbose(boolean beVerbose) {
        verboseOption = beVerbose;
    }

    public boolean isVerbose() {
        return this.verboseOption;
    }

    public void setQuiet(boolean beQuiet) {
        quietOption = beQuiet;
    }

//@olsen: disabled feature
/*
    public void setModifyJavaClasses(boolean allowMods) {
        modifyJavaClassesOption = allowMods;
    }
*/

//@olsen: disabled feature
/*
    public void setAllowFinalModifications(boolean allowMods) {
        allowFinalFieldModifications = allowMods;
    }
*/

    public void setNoWrite(boolean dontWrite) {
        noWriteOption = dontWrite;
    }

    // optimization control

//@olsen: disabled feature
/*
    public void setNoArrayOptimization(boolean dontOpt) {
        disableArrayHookCaching = dontOpt;
        disableArrayElementFetch = dontOpt;
    }
*/

    public void setNoThisOptimization(boolean dontOpt) {
        disableThisHookHoisting = dontOpt;
    }

    public void setNoInitializerOptimization(boolean dontOpt) {
        disableInitializerAnnotationSuppression = dontOpt;
    }

    public void setNoOptimization(boolean dontOpt) {
//@olsen: disabled feature
/*
        disableArrayHookCaching = dontOpt;
*/
        disableThisHookHoisting = dontOpt;
        disableInitializerAnnotationSuppression = dontOpt;
//@olsen: disabled feature
/*
        disableArrayElementFetch = dontOpt;
*/
    }

    public void setForceOverwrite(boolean forceOverwrite) {
        forceOverwriteOption = forceOverwrite;
    }

//@olsen: disabled feature
/*
    public void setUpdateInPlace(boolean inPlace) {
        updateInPlaceOption = inPlace;
    }
*/

//@olsen: disabled feature
/*
    public void setPackageTranslation(String origPackage, String newPackage) {
        // make special allowances for unpackaged classes
        if (origPackage.equals("."))
            origPackage = "";
        if (newPackage.equals("."))
            newPackage = "";

        String validOrigPackage = validVMPackage(origPackage);
        String validNewPackage = validVMPackage(newPackage);

        if (validOrigPackage == null)
            error("The name \"" + origPackage + "\" is not a valid package name.");
        else if (validNewPackage == null)
            error("The name \"" + newPackage + "\" is not a valid package name.");

        translations.put(validOrigPackage, validNewPackage);
    }
*/

    /**
     * Add a suppression entry for a class
     */
    public void suppressClassWarnings(String className) {
        classSuppressions.put(className, className);
    }

    /**
     * Add a suppression entry for a field of a class
     */
    public void suppressFieldWarnings(String fullFieldName) {
        fieldSuppressions.put(fullFieldName, fullFieldName);
    }

    /**
     * Convert a user package name to a VM package name.
     * If the package name isn't valid, return null instead.
     */
    static String validVMPackage(String pkg) {
        StringBuffer buf = new StringBuffer();

        int i=0;
        while (i<pkg.length()) {
            if (i != 0) {
                // each package component must be preceded by a '.'
                if (pkg.charAt(i) != '.')
                    return null;

                // translate '.' to '/'
                buf.append("/");//NOI18N

                // there must be more characters for the next package component
                i++;
                if (i == pkg.length())
                    return null;
            }

            if (!Character.isJavaIdentifierStart(pkg.charAt(i)))
                return null;
            buf.append(pkg.charAt(i++));

            while (i < pkg.length() &&
                   Character.isJavaIdentifierPart(pkg.charAt(i)))
                buf.append(pkg.charAt(i++));
        }

        return buf.toString();
    }

    /**
     * Check whether the named class should have warnings suppressed.
     */
    private boolean classWarningsSuppressed(String classname) {
        return classSuppressions.get(classname) != null;
    }

    /**
     * Check whether the named field in the named class should have
     * warnings suppressed.
     */
    private boolean fieldWarningsSuppressed(String classname,
                                            String fieldName) {
        return fieldSuppressions.get(classname + "." + fieldName) != null;//NOI18N
    }

    /**
     * Reset the environment.
     */
    //@olsen: added method
    public void reset() {
/*
        jdoMetaData = null;

        verboseOption = false;
        quietOption = false;
        dumpLevel = 0;

        disableThisHookHoisting = false;
        disableInitializerAnnotationSuppression = false;
        disableArrayHookCaching = false;
        disableArrayElementFetch = false;

        noWriteOption = false;
        forceOverwriteOption = false;
        updateInPlaceOption = false;
        classPathOption = null;
        destinationDirectory = null;
        destClassPath = null;

        renamedMap.clear();
        translations.clear();
*/
        errorsEncountered = 0;

        classMap.clear();
        missingClasses.clear();

        fieldSuppressions.clear();
        classSuppressions.clear();
    }
}
