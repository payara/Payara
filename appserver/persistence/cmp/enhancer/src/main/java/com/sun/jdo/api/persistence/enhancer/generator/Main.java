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

package com.sun.jdo.api.persistence.enhancer.generator;

import java.lang.reflect.Modifier;

import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import java.io.Serializable;
import java.io.File;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.sun.jdo.api.persistence.enhancer.util.Assertion;

import com.sun.jdo.api.persistence.enhancer.meta.ExtendedJDOMetaData;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataPropertyImpl;

import com.sun.jdo.spi.persistence.utility.generator.JavaFileWriter;
import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriter;
import com.sun.jdo.spi.persistence.utility.generator.JavaClassWriterHelper;
import com.sun.jdo.spi.persistence.utility.generator.io.IOJavaFileWriter;
import com.sun.jdo.spi.persistence.utility.generator.io.IOJavaClassWriter;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import com.sun.jdo.api.persistence.enhancer.LogHelperEnhancer;
import java.util.ResourceBundle;
import org.glassfish.persistence.common.I18NHelper;


/**
 *
 */
public final class Main
    extends Assertion
{
    //
    // CAUTION: THE ENHANCER-GENERATOR DEALS WITH CLASS NAMES IN THE
    // JVM FORMAT, THAT IS, '/' INSTEAD OF '.' IS USED AS SEPARATOR.
    //
    // Class names in the Java language format ('.' as separator) are
    // referred to as "normalized" in the generator code.
    //
    // File names are named as such; they use File.separatorChar as
    // separator.
    //

     /** The logger */
    private static final Logger logger = LogHelperEnhancer.getLogger();
    private static final String dotLine =
        "----------------------------------------------------------------------";
    /**
     *  The stream to write messages to.
     */
    private final PrintWriter out = new PrintWriter(System.out, true);

    /**
     *  The stream to write error messages to.
     */
    private final PrintWriter err = new PrintWriter(System.err, true);

    /**
     *  The command line options.
     */
    private final CmdLineOptions opts = new CmdLineOptions();

    /**
     * Java Writer Class
     */
    private JavaFileWriter fWriter = null;
    private JavaClassWriter writer = null;

    /**
     * The MetaData for generating classes.
     */
    private ExtendedJDOMetaData meta = null;

    private File destinationDir = null;


    /**
     * I18N message handler
     */
    private final static ResourceBundle messages = I18NHelper.loadBundle("com.sun.jdo.api.persistence.enhancer.Bundle"); // NOI18N
    public Main()
    {}

    public Main(ExtendedJDOMetaData meta, File destinationDir)
             throws IOException {
        this.meta = meta;
        this.destinationDir = destinationDir;
        createDestinationDir();
    }

    /**
     *
     */
    public static final void main(String[] argv)
    {
        final Main gen = new Main();
        try {
            gen.opts.processArgs(argv);
            gen.init();
            gen.generate();
        } catch(Exception ex) {
            gen.printError(null, ex);
        }
    }

    /**
     *  A class for holding the command line options.
     */
    private class CmdLineOptions
    {
        // final Collection inputFileNames = new ArrayList();
        String destinationDirectory = null;
        String jdoXMLModelFileName = null;
        String jdoPropertiesFileName = null;
        boolean verbose = false;

        /**
         * Print a usage message to System.err
         */
        public void usage() {
            err.println("Usage: Main <options> <arguments>...");
            err.println("Options:");
            err.println("  -v, --verbose            print verbose output");
            err.println("  -d, --dest <dir>         destination directory for output files");
            err.println("  -p, --properties <file>  use property file for meta data");
            err.println();
            err.println("Arguments:");
            err.println();
            err.println("Returns a non-zero value in case of errors.");
            System.exit(1);
        }

        /**
         * Process command line options
         */
        protected int processArgs(String[] argv)
        {
            for (int i = 0; i < argv.length; i++) {
                final String arg = argv[i];
                if (arg.equals("-v")
                    || arg.equals("--verbose")) {
                    verbose = true;
                    continue;
                }
                if (arg.equals("-d")
                    || arg.equals("--dest")) {
                    if (argv.length - i < 2) {
                        printError("Missing argument to the -d/-dest option", null);
                        usage();
                    }
                    destinationDirectory = argv[++i];
                    continue;
                }
                if (arg.equals("-p") ||
                    arg.equals("--properties")) {
                    if (argv.length - i < 2) {
                        printError("Missing argument to the -p/--properties option", null);
                        usage();
                    }
                    jdoPropertiesFileName = argv[++i];
                    continue;
                }
                if (arg.length() > 0 && arg.charAt(0) == '-') {
                    printError("Unrecognized option:" + arg, null);
                    usage();
                }
                if (arg.length() == 0) {
                    printMessage("Ignoring empty command line argument.");
                    continue;
                }

                //inputFileNames.add(arg);
            }

            // The user must specify a destination directory
            if (jdoPropertiesFileName == null) {
                printError("No destination directory specified", null);
                usage();
            }

            // The user must specify a destination directory
            if (destinationDirectory == null) {
                printError("No destination directory specified", null);
                usage();
            }

            return 0;
        }
    }

    private void init()
        throws FileNotFoundException, IOException
    {
        // load the properties
        affirm(opts.jdoPropertiesFileName != null);
        FileInputStream finput = null;
        try {
            final File f = new File(opts.jdoPropertiesFileName);
            finput = new FileInputStream(f);
            final Properties props = new Properties();
            props.load(finput);
            meta = new JDOMetaDataPropertyImpl(props, out);
        } finally {
            if (finput != null) {
                try {
                    finput.close();
                } catch(Exception ex) {
                    printError(ex.getMessage(), ex);
                }
            }
        }

        affirm(opts.destinationDirectory != null);
        destinationDir = new File(opts.destinationDirectory);
        createDestinationDir();
    }

    private void createDestinationDir()
        throws IOException
    {
        // create the destination directory
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw new IOException(I18NHelper.getMessage(messages,"EXC_DestDirCreateFailure",destinationDir));  //NOI18N
        }
    }

    private void generate()
        throws IOException
    {
        final String[] classes = meta.getKnownClasses();
        for (int i = 0; i < classes.length; i++) {
            final String className = classes[i];
            generate(className);
        }
    }

    // entry point for EJB TP class generation
    // The argument is a fully qualified class name expected in the
    // JVM format, that is, with '/' for '.' as separator.  See comment
    // at the beginning of this class.
    public File generate(final String className)
        throws IOException
    {
        affirm(className != null);
        printMessage("generating '" + className + "'...");

        //@olsen, 4653156: fixed file name
        final String filePath = className.replace('/', File.separatorChar);
        final String classFileName
            = filePath + JavaClassWriterHelper.javaExtension_;
        final File file = new File(destinationDir, classFileName);

        //@olsen: not needed: IOJavaFileWriter takes care of creating file
        // create the destination directory
        //final File dir = file.getAbsoluteFile().getParentFile();
        //if (!dir.exists() && !dir.mkdirs()) {
        //    throw new IOException("unable to create destination directory: "
        //                          + "'" + destinationDir + "'");
        //}

        fWriter = new IOJavaFileWriter(file);
        writer = new IOJavaClassWriter();
        generateClass(className);
        fWriter.addClass(writer);
        printMessage("DONE generating '" + className + "'...");

        //@olsen: moved from finally{} to main block
        // by JavaFileWriter, no stale resources remain allocated ever
        fWriter.save();
        return file;
    }

    private void generateClass(final String className)
        throws IOException
    {
        affirm(className != null);

        final String packageName = ImplHelper.getPackageName(className);
        fWriter.setPackage(packageName, null);

        // write the class header and key class
        final String oidClassName = meta.getKeyClass(className);
        if (oidClassName == null) {
            writeClassHeader(className);
        } else {
            final String oidPackageName
                = ImplHelper.getPackageName(oidClassName);
            affirm(packageName.equals(oidPackageName),
                   "PC class and key class must be in same package.");

            final boolean enclosedOid
                = oidClassName.startsWith(className + "$");
            if (enclosedOid) {
                writeClassHeader(className);
                writeOidClass(className, ImplHelper.getClassName(oidClassName),
                    enclosedOid);
            } else {
                writeOidClass(className, ImplHelper.getClassName(oidClassName),
                    enclosedOid);
                writeClassHeader(className);
            }
        }

        writeClassMembers(className);

        // write the augmentation
        final boolean isPC = meta.isPersistenceCapableClass(className);
        if (isPC) {
            final boolean isPCRoot
                = meta.isPersistenceCapableRootClass(className);
            if (isPCRoot) {
                writePCRootMembers(className);
            }
            writePCMembers(className);
        }
    }

    private void writeClassHeader(final String className)
        throws IOException
    {
        final boolean isPCRoot = meta.isPersistenceCapableRootClass(className);
        final String superclass =
            ImplHelper.normalizeClassName(meta.getSuperClass(className));

        final String[] comments = null;
        final String[] interfaces
            = (isPCRoot
               ? new String[]{ ImplHelper.CLASSNAME_JDO_PERSISTENCE_CAPABLE, "Cloneable" }
               : null);
        writer.setClassDeclaration(meta.getClassModifiers(className),
                ImplHelper.getClassName(className), comments);
        writer.setSuperclass(superclass);
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length; i++) {
                writer.addInterface(interfaces[i]);
            }
        }
    }

    private void writeClassMembers(final String className)
        throws IOException
    {
        final String[] comments = new String[]{
            dotLine,
            "Class Members:",
            dotLine
        };

        // write default constructor
        writer.addConstructor(ImplHelper.getClassName(className),
            Modifier.PUBLIC, null, null, null,
            ImplHelper.getDefaultConstructorImpl(),
            comments);

        final String[] fieldNames = meta.getKnownFields(className);
        final int n = (fieldNames != null ? fieldNames.length : 0);

        // write method clone() for enhancer testing purpose
        final ArrayList list = new ArrayList();
        for (int i = 0; i < n; i++) {
            final String fieldName = (String)fieldNames[i];
            final int access = meta.getFieldModifiers(className, fieldName);
            if ((access & Modifier.STATIC) == 0) {
                list.add(fieldName);
            }
        }

        writer.addMethod(
            "clone",
            Modifier.PUBLIC,
            JavaClassWriterHelper.Object_,
            null,
            null,
            new String[]{ "java.lang.CloneNotSupportedException" },
            ImplHelper.getCloneImpl(className),
            ImplHelper.COMMENT_NOT_ENHANCER_ADDED);

        // write the fields and with their bean getters/setters
        for (int i = 0; i < n; i++) {
            final String fieldName = (String)fieldNames[i];
            writeFieldMember(className, fieldName);
        }
    }

    private void writeFieldMember(final String className,
                                  final String fieldName)
        throws IOException
    {
        final String fieldType = meta.getFieldType(className, fieldName);
        final String normalizedFieldType =
            ImplHelper.normalizeClassName(fieldType);
        final int fieldNumber = meta.getFieldNo(className, fieldName);
        final int flags = meta.getFieldFlags(className, fieldName);

        final int access = meta.getFieldModifiers(className, fieldName);

        // the field
        writer.addField(
            fieldName,
            access,
            normalizedFieldType,
            null, null);

        // do not write bean getters and setters for static fields
        if ((access & Modifier.STATIC) != 0) {
            return;
        }

        // accessor
        {
            affirm(((flags & meta.CHECK_READ) == 0)
                   | (flags & meta.MEDIATE_READ) == 0);
            final String[] impl;
            if ((flags & meta.CHECK_READ) != 0) {
                impl = ImplHelper.getFieldCheckReadImpl(fieldName,
                                                        fieldType,
                                                        fieldNumber);
            } else if ((flags & meta.MEDIATE_READ) != 0) {
                impl = ImplHelper.getFieldMediateReadImpl(fieldName,
                                                          fieldType,
                                                          fieldNumber);
            } else {
                impl = ImplHelper.getFieldDirectReadImpl(fieldName,
                                                         fieldType,
                                                         fieldNumber);
            }
            writer.addMethod(
                createMethodName(JavaClassWriterHelper.get_, fieldName),
                Modifier.PUBLIC,
                normalizedFieldType,
                null, null, null,
                impl,
                ImplHelper.COMMENT_ENHANCER_ADDED);
        }

        // mutator
        {
            affirm(((flags & meta.CHECK_WRITE) == 0)
                   | (flags & meta.MEDIATE_WRITE) == 0);
            final String[] impl;
            if ((flags & meta.CHECK_WRITE) != 0) {
                impl = ImplHelper.getFieldCheckWriteImpl(fieldName,
                                                         fieldType,
                                                         fieldNumber,
                                                         fieldName);
            } else if ((flags & meta.MEDIATE_WRITE) != 0
                    && !meta.isKnownNonManagedField(className, fieldName,
                    null)) {
                impl = ImplHelper.getFieldMediateWriteImpl(fieldName,
                                                           fieldType,
                                                           fieldNumber,
                                                           fieldName);
            } else {
                impl = ImplHelper.getFieldDirectWriteImpl(fieldName,
                                                          fieldType,
                                                          fieldNumber,
                                                          fieldName);
            }
            writer.addMethod(
                createMethodName(JavaClassWriterHelper.set_, fieldName),
                Modifier.PUBLIC,
                JavaClassWriterHelper.void_,
                new String[]{ fieldName },
                new String[]{ normalizedFieldType },
                null,
                impl,
                ImplHelper.COMMENT_ENHANCER_ADDED);
        }

    }

    private void writePCRootMembers(final String className)
        throws IOException
    {
        final String[] comments = new String[]{
            dotLine,
            "Augmentation for Persistence-Capable Root Classes (added by enhancer):",
            dotLine
        };

        // write constructor with parameter StateManager
        writer.addConstructor(
            ImplHelper.getClassName(className),
            Modifier.PUBLIC,
            new String[]{ ImplHelper.FIELDNAME_JDO_STATE_MANAGER },
            new String[]{ ImplHelper.CLASSNAME_JDO_STATE_MANAGER },
            null,
            ImplHelper.getJDOConstructorSMImpl(ImplHelper.FIELDNAME_JDO_STATE_MANAGER),
            comments);

        // jdoStateManager
        writer.addField(
            ImplHelper.FIELDNAME_JDO_STATE_MANAGER,
            Modifier.PUBLIC | Modifier.TRANSIENT,
            ImplHelper.CLASSNAME_JDO_STATE_MANAGER,
            JavaClassWriterHelper.null_,
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // jdoFlags
        writer.addField(
            ImplHelper.FIELDNAME_JDO_FLAGS,
            Modifier.PUBLIC | Modifier.TRANSIENT,
            "byte",
            "0",
            ImplHelper.COMMENT_ENHANCER_ADDED);


        // jdoGetStateManager
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_GET_STATE_MANAGER,
            Modifier.PUBLIC | Modifier.FINAL,
            ImplHelper.CLASSNAME_JDO_STATE_MANAGER, null, null, null,
            new String[] { "return " +  ImplHelper.FIELDNAME_JDO_STATE_MANAGER + JavaClassWriterHelper.delim_ },
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // jdoSetStateManager
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_SET_STATE_MANAGER,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.void_,
            new String[] { ImplHelper.FIELDNAME_JDO_STATE_MANAGER },
            new String[] { ImplHelper.CLASSNAME_JDO_STATE_MANAGER }, null,
            new String[] { "this." +  ImplHelper.FIELDNAME_JDO_STATE_MANAGER + " = " + ImplHelper.FIELDNAME_JDO_STATE_MANAGER + JavaClassWriterHelper.delim_ },
            ImplHelper.COMMENT_ENHANCER_ADDED);

        //jdoGetFlags
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_GET_FLAGS,
            Modifier.PUBLIC | Modifier.FINAL,
            "byte", null, null, null,
            new String[] { "return " +  ImplHelper.FIELDNAME_JDO_FLAGS +
                    JavaClassWriterHelper.delim_ },
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // jdoSetFlags
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_SET_FLAGS,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.void_,
            new String[] { ImplHelper.FIELDNAME_JDO_FLAGS },
            new String[] { "byte" }, null,
            new String[] { "this." +  ImplHelper.FIELDNAME_JDO_FLAGS + " = " + ImplHelper.FIELDNAME_JDO_FLAGS + JavaClassWriterHelper.delim_ },
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // getPersistenceManager
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_GET_PERSISTENCE_MANAGER,
            Modifier.PUBLIC | Modifier.FINAL,
            ImplHelper.CLASSNAME_JDO_PERSISTENCE_MANAGER, null, null, null,
            ImplHelper.getJDOStateManagerObjectDelegationImpl("getPersistenceManager()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // getObjectId
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_GET_OBJECT_ID,
            Modifier.PUBLIC | Modifier.FINAL,
            Object.class.getName(), null, null, null,
            ImplHelper.getJDOStateManagerObjectDelegationImpl("getObjectId()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // is-methods
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_IS_PERSISTENT,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.boolean_, null, null, null,
            ImplHelper.getJDOStateManagerBooleanDelegationImpl("isPersistent()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        writer.addMethod(
            ImplHelper.METHODNAME_JDO_IS_TRANSACTIONAL,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.boolean_, null, null, null,
            ImplHelper.getJDOStateManagerBooleanDelegationImpl("isTransactional()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        writer.addMethod(
            ImplHelper.METHODNAME_JDO_IS_NEW,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.boolean_, null, null, null,
            ImplHelper.getJDOStateManagerBooleanDelegationImpl("isNew()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        writer.addMethod(
            ImplHelper.METHODNAME_JDO_IS_DELETED,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.boolean_, null, null, null,
            ImplHelper.getJDOStateManagerBooleanDelegationImpl("isDeleted()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        writer.addMethod(
            ImplHelper.METHODNAME_JDO_IS_DIRTY,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.boolean_, null, null, null,
            ImplHelper.getJDOStateManagerBooleanDelegationImpl("isDirty()"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // makeDirty
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_MAKE_DIRTY,
            Modifier.PUBLIC | Modifier.FINAL,
            JavaClassWriterHelper.void_,
            new String[]{ "fieldName" },
            new String[]{ String.class.getName() },
            null,
            ImplHelper.getJDOStateManagerVoidDelegationImpl("makeDirty(fieldName)"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

    }

    private void writePCMembers(final String className)
        throws IOException
    {
        final String[] comments = new String[]{
            dotLine,
            "Augmentation for Persistence-Capable Classes (added by enhancer):",
            dotLine
        };

        final String[] managedFieldNames
            = meta.getManagedFields(className);
        final String[] managedFieldTypes
            = meta.getFieldType(className, managedFieldNames);
        final boolean isPCRoot
            = meta.isPersistenceCapableRootClass(className);

        // jdoGetField
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_GET_FIELD,
            Modifier.PUBLIC,
            JavaClassWriterHelper.Object_,
            new String[]{ "fieldNumber" },
            new String[]{ "int" },
            null,
            ImplHelper.getJDOGetFieldImpl("fieldNumber",
                                          managedFieldNames,
                                          managedFieldTypes),
            comments);

        // jdoSetField
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_SET_FIELD,
            Modifier.PUBLIC,
            JavaClassWriterHelper.void_,
            new String[]{ "fieldNumber", "obj" },
            new String[]{ "int", JavaClassWriterHelper.Object_ },
            null,
            ImplHelper.getJDOSetFieldImpl("fieldNumber", "obj",
                                          managedFieldNames,
                                          managedFieldTypes),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // jdoClear
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_CLEAR,
            Modifier.PUBLIC,
            JavaClassWriterHelper.void_, null, null, null,
            ImplHelper.getJDOClearImpl(className, meta, managedFieldNames,
                                       managedFieldTypes),
            ImplHelper.COMMENT_ENHANCER_ADDED);

        // jdoNewInstance
        writer.addMethod(
            ImplHelper.METHODNAME_JDO_NEW_INSTANCE,
            Modifier.PUBLIC,
            JavaClassWriterHelper.Object_,
            new String[]{ "sm" },
            new String[]{ ImplHelper.CLASSNAME_JDO_STATE_MANAGER },
            null,
            ImplHelper.getJDONewInstanceImpl(className, "sm"),
            ImplHelper.COMMENT_ENHANCER_ADDED);

    }

    private void writeOidClass(final String className,
                               final String oidClassName,
                               final boolean enclosedOid)
        throws IOException
    {
        final String[] comments = new String[]{
            dotLine,
            "Key Class:",
            dotLine
        };

        final String superOidClassName
            = ImplHelper.normalizeClassName(meta.getSuperKeyClass(className));

        JavaClassWriter oidWriter = new IOJavaClassWriter();

        oidWriter.setClassDeclaration(
            (enclosedOid ? Modifier.PUBLIC | Modifier.STATIC : 0),
            oidClassName,
            ImplHelper.COMMENT_NOT_ENHANCER_ADDED);
        oidWriter.setSuperclass(superOidClassName);
        oidWriter.addInterface(Serializable.class.getName());

        final boolean isPCRoot
            = meta.isPersistenceCapableRootClass(className);

        final String[] pknames = meta.getKeyFields(className);
        final String[] pktypes = meta.getFieldType(className, pknames);

        // write the PK-fields
        for (int i = 0; i < pknames.length; i++) {
            oidWriter.addField(
                pknames[i],
                Modifier.PUBLIC,
                ImplHelper.normalizeClassName(pktypes[i]),
                null,
                null);
        }

        // write default constructor
        oidWriter.addConstructor(
            oidClassName,
            Modifier.PUBLIC,
            null, null, null,
            ImplHelper.getDefaultConstructorImpl(),
            ImplHelper.COMMENT_NOT_ENHANCER_ADDED);

        // hashCode
        oidWriter.addMethod(
            "hashCode",
            Modifier.PUBLIC,
            "int",
            null,
            null,
            null,
            ImplHelper.getOidHashCodeImpl(pknames,
                                          pktypes,
                                          isPCRoot),
            ImplHelper.COMMENT_NOT_ENHANCER_ADDED);

        // equals
        oidWriter.addMethod(
            "equals", Modifier.PUBLIC, JavaClassWriterHelper.boolean_,
            new String[]{ "pk" },
            new String[]{ Object.class.getName() },
            null,
            ImplHelper.getOidEqualsImpl(oidClassName,
                                        pknames,
                                        pktypes,
                                        "pk",
                                        isPCRoot),
            ImplHelper.COMMENT_NOT_ENHANCER_ADDED);

          if (enclosedOid) {
              writer.addClass(oidWriter);
          } else {
              fWriter.addClass(oidWriter);
          }
    }

    static private String createMethodName(final String prefix,
                                           final String fieldName)
    {
        return (prefix + Character.toUpperCase(fieldName.charAt(0))
                + fieldName.substring(1));
    }

    //XXX use common logger later
    private void printMessage(String msg)
    {
	  logger.finest("TP PCClassGen: " + msg); // NOI18N
    }

    private void printError(String msg, Throwable ex)
    {
        if (msg != null) {
            String errmsg=msg + (ex != null ? ": " + ex.getMessage() : ""); //NOI18N
            logger.log(Logger.SEVERE,"CME.generic_exception",errmsg); //NOI18N
        }
        if (ex != null) {
            logger.log(Logger.SEVERE,"CME.generic_exception_stack",ex); //NOI18N
        }
    }
}
