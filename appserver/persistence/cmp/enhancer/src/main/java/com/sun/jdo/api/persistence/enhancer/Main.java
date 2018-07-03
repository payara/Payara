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

package com.sun.jdo.api.persistence.enhancer;

//@olsen:
//import java.io.*;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;

//@olsen:
//import java.util.*;
import java.util.Map;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipException;

import java.net.URL;

import com.sun.jdo.api.persistence.model.Model;

//import com.sun.jdo.api.persistence.enhancer.util.ClassFileSource;
//import com.sun.jdo.api.persistence.enhancer.util.ZipFileRegistry;
//import com.sun.jdo.api.persistence.enhancer.util.ClassPath;
//import com.sun.jdo.api.persistence.enhancer.util.FilePath;

//@olsen: added support for timing statistics
import com.sun.jdo.api.persistence.enhancer.util.Support;

//import com.sun.jdo.api.persistence.enhancer.classfile.ClassFile;

import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataModelImpl;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataPropertyImpl;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataTimer;

//import com.sun.jdo.api.persistence.enhancer.impl.ClassControl;
//import com.sun.jdo.api.persistence.enhancer.impl.Environment;


//import org.openidex.jarpackager.ArchiveEntry; //@yury Added the capability to create ClassFileSources out of ArchiveEntries

//@olsen: disabled feature
/*
import com.sun.jdo.api.persistence.enhancer.impl.FieldMap;
*/

//@lars: moved functionality into ByteCodeEnhancerHelper
//@lars: design improvements
//@olsen: cosmetics
//@olsen: subst: [iI]Persistent -> [pP]ersistenceCapable
//@olsen: subst: /* ... */ -> // ...
//@olsen: moved: class FilterError -> package util
//@olsen: moved: OSCFP.addClass(ClassControl) -> impl.Environment
//@olsen: subst: filterEnv.classMap.elements() -> filterEnv.getClasses()
//@olsen: subst: filterEnv.classMap.get(name) -> filterEnv.getClass(name)
//@olsen: subst: filterEnv.translations -> filterEnv.translations()
//@olsen: subst: filterEnv.translations -> filterEnv.destinationDirectory()
//@olsen: subst: OSCFP -> Main
//@olsen: subst: filterEnv -> env
//@olsen: subst: FilterEnv -> Environment
//@olsen: dropped parameter 'Environment env', use association instead
//@olsen: subst: augment -> closeOver
//@olsen: subst: collectAllClasses -> collectClasses
//@olsen: subst: Vector -> Collection, List, ArrayList
//@olsen: subst: Hashtable -> Map, HashMap
//@olsen: subst: Enumeration,... -> Iterator, hasNext(), next()
//@olsen: moved: Main.closeOverClasses() -> EnhancerControl
//@olsen: moved: Main.closeOverClass(ClassControl) -> EnhancerControl
//@olsen: moved: Main.modifyClasses() -> EnhancerControl
//@olsen: moved: Main.retargetClasses() -> EnhancerControl
//@olsen: moved: Main.locateTranslatedClasses() -> EnhancerControl
//@olsen: moved: Main.checkIndexableFields() -> EnhancerControl
//@olsen: subst: (ClassControl) control -> cc
//@olsen: removed: proprietary support for HashCode
//@olsen: removed: proprietary support for TypeSummary
//@olsen: removed: proprietary support for ClassInfo
//@olsen: removed: proprietary support for IndexableField
//@olsen: removed: support for IgnoreTransientField, AddedTransientField
//@olsen: removed: support for [No]AnnotateField


/**
 * Main is the starting point for the persistent filter tool.
 */
public class Main
{


    /**
     *  The byte code enhancer.
     */
    private ByteCodeEnhancer enhancer = null;


    /**
     *  The stream to write messages to.
     */
    private final PrintWriter outMessages = new PrintWriter (System.out, true);


    /**
     *  The stream to write error messages to.
     */
    private final PrintWriter outErrors = new PrintWriter (System.err, true);


    /**
     *  The command line options.
     */
    private final CmdLineOptions cmdLineOpts = new CmdLineOptions ();


    /**
     * Construct a filter tool instance
     */
    public Main() {
    }


    /**
     * This is where it all starts.
     */
    public static void main(String[] argv) {
        //@olsen: added support for timing statistics
        final Main main = new Main();
        try {
            //@olsen: split: method filter() -> process(), processArgs()
            //System.exit(new Main().process(argv));
            main.process(argv);
        } catch (RuntimeException tt) {
            //} catch (Throwable tt) {
            //@olsen: changed to print to error stream
            System.err.println("Exception occurred while postprocessing:");
            tt.printStackTrace(System.err);
            //System.exit(1);
            throw tt;
        } finally {
            //@olsen: added support for timing statistics
            if (main.cmdLineOpts.doTiming) {
                Support.timer.print();
            }
        }
    }

    /**
     * Process command line options and perform filtering tasks
     */
    //@olsen: split: method filter() -> process(), processArgs()
    public int process(String[] argv) {
        //@olsen: added inplace of disabled feature
        ArrayList cNames = new ArrayList();
        //@olsen: split: method filter() -> process(), processArgs()
        int res = processArgs(argv, cNames);
        if (res != 0) {
            //@olsen: added println()
            printMessage ("aborted with errors.");//NOI18N
            return res;
        }

        //@olsen: added support for timing statistics
        try {
            if (this.cmdLineOpts.doTiming) {
                Support.timer.push("Main.process(String[])");//NOI18N
            }

            // Find all of the classes on which we want to operate
//@olsen: disabled feature
            enhanceInputFiles (cNames);
/*
            computeClasses(pcNames, paNames, ccNames);
*/

            printMessage ("done.");//NOI18N
            return 0;
        } finally {
            if (this.cmdLineOpts.doTiming) {
                Support.timer.pop();
            }
        }
    }

    /**
     * Process command line options
     */
    //@olsen: split: method filter() -> process(), processArgs()
    //@olsen: made private
    protected int processArgs(String[] argv,
                            //@olsen: added inplace of disabled feature
                            Collection cNames) {
        argv = preprocess(argv);

//@olsen: disabled feature
/*
        ArrayList ccNames = new ArrayList();
        ArrayList pcNames = new ArrayList();
        ArrayList paNames = new ArrayList();
        int classMode = ClassControl.PersistCapable;
*/

//@olsen: disabled feature
/*
        String classpath = System.getProperty("java.class.path");
        String sysClasspath = System.getProperty("sun.boot.class.path", "");
*/

        //@olsen: added
        Properties jdoMetaDataProperties = null;

        for (int i=0; i<argv.length; i++) {
            //@olsen: improved control flow, subst: else -> continue

            String arg = argv[i];
//@olsen: disabled feature
/*
            if (arg.equals("-cc") ||
                arg.equals("-copyclass")) {
                classMode = ClassControl.PersistUnknown;
                continue;
            }
            if (arg.equals("-pa") ||
                arg.equals("-persistaware")) {
                classMode = ClassControl.PersistAware;
                continue;
            }
            if (arg.equals("-pc") ||
                arg.equals("-persistcapable")) {
                classMode = ClassControl.PersistCapable;
                continue;
            }
*/
            if (arg.equals("-v") ||//NOI18N
                arg.equals("-verbose")) {//NOI18N
                this.cmdLineOpts.verbose = true;
                this.cmdLineOpts.quiet = false;
                continue;
            }
            if (arg.equals("-q") ||//NOI18N
                arg.equals("-quiet")) {//NOI18N
                this.cmdLineOpts.quiet = true;
                this.cmdLineOpts.verbose = false;
                continue;
            }
            if (arg.equals("-f") ||//NOI18N
                arg.equals("-force")) {//NOI18N
                this.cmdLineOpts.forceWrite = true;
                continue;
            }
//@olsen: disabled feature
/*
            if (arg.equals("-inplace")) {
                env.setUpdateInPlace(true);
                continue;
            }
*/
//@lars: disabled feature
/*
            if (arg.equals("-qf") ||//NOI18N
                arg.equals("-quietfield")) {//NOI18N
                if (argv.length-i < 2) {
                    usage();
                    printError ("Missing argument to the -quietfield option", null);//NOI18N
                } else {
                    String fullFieldName = argv[++i];
                    if (fullFieldName.indexOf('.') == -1) {
                        printError ("Field name specifications must include " +//NOI18N
                                  "a fully qualified class name.  " +//NOI18N
                                  fullFieldName + " does not include one.", null);//NOI18N
                    } else {
                        env.suppressFieldWarnings(fullFieldName);
                    }
                }
                continue;
            }
            if (arg.equals("-qc") ||//NOI18N
                arg.equals("-quietclass")) {//NOI18N
                if (argv.length-i < 2) {
                    usage();
                    env.error("Missing argument to the -quietclass option");//NOI18N
                } else {
                    env.suppressClassWarnings(argv[++i]);
                }
                continue;
            }
*/
            if (arg.equals("-nowrite")) {//NOI18N
                this.cmdLineOpts.noWrite = true;
                continue;
            }
//@olsen: disabled feature
/*
            if (arg.equals("-modifyjava")) {
                env.setModifyJavaClasses(true);
                continue;
            }
*/
//@olsen: disabled feature
/*
            if (arg.equals("-modifyfinals")) {
                env.setAllowFinalModifications(true);
                continue;
            }
*/
//@olsen: disabled feature
/*
            if (arg.equals("-noarrayopt")) {
                env.setNoArrayOptimization(true);
                continue;
            }
*/
//@lars: disabled feature
/*
            if (arg.equals("-nothisopt")) {//NOI18N
                env.setNoThisOptimization(true);
                continue;
            }
            if (arg.equals("-noinitializeropt")) {//NOI18N
                env.setNoInitializerOptimization(true);
                continue;
            }
            if (arg.equals("-noopt")) {//NOI18N
                env.setNoOptimization(true);
                continue;
            }
*/
            if (arg.equals("-d") ||//NOI18N
                arg.equals("-dest")) {//NOI18N
                if (argv.length-i < 2) {
                    printError ("Missing argument to the -dest option", null);//NOI18N
                    usage();
                }
                this.cmdLineOpts.destinationDirectory = argv[++i];
                continue;
            }
//@olsen: disabled feature
/*
            if (arg.equals("-classpath") ||
                arg.equals("-cpath")) {
                if (argv.length-i < 2) {
                    usage();
                    env.error("Missing argument to the -classpath option");
                }
                classpath = argv[++i];
                continue;
            }
            if (arg.equals("-sysclasspath") ||
                arg.equals("-syscpath")) {
                if (argv.length-i < 2) {
                    usage();
                    env.error("Missing argument to the -sysclasspath option");
                }
                sysClasspath = argv[++i];
                continue;
            }
*/
//@olsen: disabled feature
/*
            if (arg.equals("-tp") ||
                arg.equals("-translatepackage")) {
                if (argv.length-i < 3) {
                    usage();
                    env.error("Missing arguments to the -translatepackage option");
                }
                env.setPackageTranslation(argv[i+1], argv[i+2]);
                i += 2;
                continue;
            }
*/
            //@olsen: new command line option for timing statistics
            if (arg.equals("-t") ||//NOI18N
                arg.equals("--doTiming")) {//NOI18N
                this.cmdLineOpts.doTiming = true;
//                env.setDoTimingStatistics(true);
                continue;
            }
            //@olsen: new command line option for JDO meta data properties
            if (arg.equals("-jp") ||//NOI18N
                arg.equals("--jdoProperties")) {//NOI18N
                if (argv.length-i < 2) {
                    printError("Missing argument to the -jp/--jdoProperties option", null);//NOI18N
                    usage();
                }
                try {
                    jdoMetaDataProperties = new Properties();
                    jdoMetaDataProperties.load(new FileInputStream(argv[++i]));
                } catch (IOException ex) {
                    printError("Cannot read JDO meta data properties from file", ex);//NOI18N
                    usage();
                }
                continue;
            }
            if (arg.length() > 0 && arg.charAt(0) == '-') {
                printError("Unrecognized option:" + arg, null);//NOI18N
                usage();
            }
            if (arg.length() == 0) {
                printMessage ("Empty file name encountered on the command line.");//NOI18N
            }

            //@olsen: added inplace of disabled feature
            cNames.add(arg);
//@olsen: disabled feature
/*
            {
                if (arg.equals("java.lang.Object"))
                    env.error("java.lang.Object may not be postprocessed");
                else if (classMode == ClassControl.PersistCapable)
                    pcNames.add(arg);
                else if (classMode == ClassControl.PersistAware)
                    paNames.add(arg);
                else if (classMode == ClassControl.PersistUnknown)
                    ccNames.add(arg);
                else
                    affirm(false, "Invalid class mode");
            }
*/
        }

        //@olsen: forced settings
        //env.setVerbose(true);
        this.cmdLineOpts.quiet = false;
//        env.setNoOptimization(true);
//        env.message("forced settings: -noopt");//NOI18N

/*
        if (env.errorCount() > 0)
            return 1;
*/

//@olsen: disabled feature
/*
        env.setClassPath(classpath +
                         java.io.File.pathSeparator +
                         sysClasspath);
*/

        // The user must specify a destination directory
        if (this.cmdLineOpts.destinationDirectory == null) {
            if (argv.length > 0)
                printError("No -dest output directory was specified", null);//NOI18N
            usage();
        }

        //@olsen: added: initialize JDO meta data
        JDOMetaData jdoMetaData;
        if (jdoMetaDataProperties != null) {
            printMessage("using JDO meta-data from properties");//NOI18N
            jdoMetaData = new JDOMetaDataPropertyImpl(jdoMetaDataProperties, this.outMessages);
        } else {
            printMessage("using JDO meta-data from Model.Enhancer");//NOI18N
            jdoMetaData = new JDOMetaDataModelImpl(Model.ENHANCER, this.outMessages);
        }
        //@olsen: added support for timing statistics
        if (this.cmdLineOpts.doTiming) {
            // wrap with timing meta data object
            jdoMetaData = new JDOMetaDataTimer(jdoMetaData);
        }

        try
        {
            this.enhancer = createEnhancer (jdoMetaData);
        }
        catch (Exception ex)
        {
            printError ("Error creating the enhancer", ex);
        }


//@olsen: disabled feature
/*
        // -translatepackage is incompatible with in-place update.
        if (env.updateInPlace() && env.translations().size() > 0) {
            env.error("The -translatepackage option cannot be used " +
                      "in conjunction with the -inplace option.");
            return 1;
        }
*/

        // make sure we don't lookup classes from the destination directory
        // unless we are doing in-place annotation, and in that case it should
        // be as a last resort.
//@lars: removed
/*
        if (env.updateInPlace())
            env.moveDestinationDirectoryToEnd();
        else
            env.excludeDestinationDirectory();
*/

        //@olsen: split: method filter() -> process(), processArgs()
        return 0;
    }

    // Private methods

    /**
     * Preprocess @files, returning a new argv incorporating the contents
     * of the @files, if any
     */
    private String[] preprocess(String[] args) {
        ArrayList argVec = new ArrayList();
        for (int i=0; i<args.length; i++) {
            if (args[i].length() > 0 && args[i].charAt(0) == '@') {
                String filename = null;
                if (args[i].length() == 1) {
                    if (i+1 < args.length)
                        filename = args[++i];
                } else {
                    filename = args[i].substring(1);
                }

                if (filename == null) {
                    printError("missing file name argument to @.", null);//NOI18N
                } else {
                    appendFileContents(filename, argVec);
                }
            } else {
                argVec.add(args[i]);
            }
        }
        //@olsen: subst: Vector -> ArrayList
        //String[] newArgs = new String[argVec.size()];
        //argVec.copyInto(newArgs);
        final String[] newArgs = (String[])argVec.toArray(new String[0]);
        return newArgs;
    }

    /**
     * Given an input file name, open the file and append each "word"
     * within the file to argVec.  This currently has only a very
     * primitive notion of words (separated by white space).
     */
    private void appendFileContents(String filename, ArrayList argVec) {
        try {
            FileReader inputFile = new FileReader(filename);
            try {
                BufferedReader input = new BufferedReader(inputFile);
                String s = null;
                while ((s = input.readLine()) != null) {
                    StringTokenizer parser = new StringTokenizer(s, " \t", false);//NOI18N
                    while (parser.hasMoreElements()) {
                        String token = parser.nextToken();
                        if (token.length() > 0 && token.charAt(0) == '@')
                            printError("The included file \"" +//NOI18N
                                      filename +
                                      "\" contains a recursive include.  " +//NOI18N
                                      "Recursive includes are not supported.", null);//NOI18N
                        if (token.charAt(0) == '#') break;
                        argVec.add(token);
                    }
                }
            }
            catch (IOException ex) {
                printError("IO exception reading file " + filename + ".", ex);//NOI18N
            }
        }
        catch (FileNotFoundException ex) {
            printError("file " + filename + " not found.", ex);//NOI18N
        }
    }


    /**********************************************************************
     *
     *********************************************************************/

    private final ByteCodeEnhancer createEnhancer (JDOMetaData jdometadata)
                                   throws EnhancerUserException,
                                          EnhancerFatalError
    {

        Properties props = new Properties ();
        if  (this.cmdLineOpts.verbose)
        {
            props.put (FilterEnhancer.VERBOSE_LEVEL, FilterEnhancer.VERBOSE_LEVEL_VERBOSE);
        }

        return new FilterEnhancer (jdometadata, props, this.outMessages, this.outErrors);

    }  //Main.createEnhancer()


    /**********************************************************************
     *  Enhances all files entered in the command line.
     *
     *  @param  filenames  The filenames.
     *********************************************************************/

    private final void enhanceInputFiles (Collection filenames)
    {

        for (Iterator names = filenames.iterator(); names.hasNext ();)
        {
            try
            {
                String name = (String) names.next ();
                int n = name.length ();

                //if we have a class-files
                InputStream in = null;
                if (isClassFileName (name))
                {
                    enhanceClassFile (openFileInputStream (name));
                }
                else
                {
                    //if we have an archive
                    if (isZipFileName (name))
                    {
                        enhanceZipFile (name); //getZipFile (name));
                    }
                    //assume that it is a class name
                    else
                    {
                        enhanceClassFile (openClassInputStream (name));
                    }
                }
            }
            catch (Throwable ex)
            {
                printError (null, ex);
            }
        }

    }  //Main.enhanceInputFiles()


    /**********************************************************************
     *  Enhances a classfile.
     *
     *  @param  in  The input stream of the classfile.
     *********************************************************************/

    private final void enhanceClassFile (InputStream in)
    {

        OutputStream out = null;
        try
        {
            File temp = File.createTempFile ("enhancer", ".class");
            out = new BufferedOutputStream (new FileOutputStream (temp));

            //enhance
            OutputStreamWrapper wrapper = new OutputStreamWrapper (out);
            boolean enhanced = this.enhancer.enhanceClassFile (in, wrapper);
            closeOutputStream (out);
            createOutputFile (enhanced, createClassFileName (wrapper.getClassName ()), temp);
        }
        catch (Throwable ex)
        {
            printError (null, ex);
        }
        finally
        {
            closeInputStream (in);
            closeOutputStream (out);
        }

    }  //Main.enhanceClassFile()


    /**********************************************************************
     *  Enhances a zipfile.
     *
     *  @param  name  The filename of the zipfile.
     *********************************************************************/

    private final void enhanceZipFile (String filename)
    {

        ZipInputStream  in = null;
        ZipOutputStream out = null;
        try
        {
            File temp = File.createTempFile ("enhancer", ".zip");
            in = new ZipInputStream (new BufferedInputStream (new FileInputStream (new File (filename))));
            out = new ZipOutputStream (new BufferedOutputStream (new FileOutputStream (temp)));

            //enhance the zipfile
            boolean enhanced = ByteCodeEnhancerHelper.enhanceZipFile (this.enhancer, in, out);
            closeOutputStream (out);
            out = null;

            //create the output file
            createOutputFile (enhanced, new File (filename).getName (), temp);
        }
        catch (Throwable ex)
        {
            printError (null, ex);
        }
        finally
        {
            closeOutputStream (out);
            closeInputStream (in);
        }

    }  //Main.enhanceZipFile()


    /**********************************************************************
     *  Opens an input stream for the given filename
     *
     *  @param  filename  The name of the file.
     *
     *  @return  The input stream.
     *
     *  @exception  FileNotFoundException  If the file could not be found.
     *********************************************************************/

    private static final InputStream openFileInputStream (String filename)
                                     throws FileNotFoundException
    {

     	return new BufferedInputStream (new FileInputStream (new File (filename)));

    }  //Main.openFileInputStream()


    /**********************************************************************
     *  Opens an input stream for the given classname. The input stream is
     *  created via an URL that is obtained by the current ClassLoader.
     *
     *  @param  classname  The name of the class (dot-notation).
     *
     *  @return  The iput stream.
     *
     *  @exception  IOException             If an I/O error occured.
     *  @exception  ClassNotFoundException  If the class could not be found.
     *********************************************************************/

    private final InputStream openClassInputStream (String classname)
                              throws IOException,
                                     ClassNotFoundException
    {

        URL url = Main.class.getClassLoader ().getSystemResource (createClassFileName (classname));
        if  (url == null)
        {
            throw new ClassNotFoundException (classname);
        }
        return url.openStream ();

    }  //Main.getClassInPath()


    /**********************************************************************
     *  Tests if a filename is a classfile name.
     *
     *  @param  filename  The name of the file.
     *
     *  @return  Do we have a potential classfile?
     *********************************************************************/

    private static final boolean isClassFileName (String filename)
    {

        return filename.endsWith (".class");

    }  //Main.isClassFileName()


    /**********************************************************************
     *  Tests if a filename is a zipfile (only by testing if the extension -
     *  ignoring the case - is <code>".zip"</code> or <code>".jar"</code>).
     *
     *  @param  filename  The name of the file.
     *
     *  @param  Do we have a potential zipfile?
     *********************************************************************/

    private static final boolean isZipFileName (String filename)
    {

        final int n = filename.length ();
        if  (n < 5)
        {
            return false;
        }
        String ext = filename.substring (n - 4);

        return ext.equalsIgnoreCase (".zip")  ||  ext.equalsIgnoreCase (".jar");

    }  //Main.isZipFileName


    /**********************************************************************
     *  Creates a filename from a classname (by replacing <code>'.'</code>
     *  by <code>'/'</code>.
     *
     *  @param  classname  The classname.
     *
     *  @return  The filename.
     *********************************************************************/

    private static final String createClassFileName (String classname)
    {

        return classname.replace ('.', '/') + ".class";

    }  //Main.createClassFileName()


    /**********************************************************************
     *  Creates a file object that represents the output zipfile for a given
     *  zipfile to enhance.
     *
     *  @param  zipfilename  The input zipfile name.
     *
     *  @return  The output zipfile name.
     *********************************************************************/

    private final File createZipOutputFile (String zipfilename)
    {

        return new File (this.cmdLineOpts.destinationDirectory, new File (zipfilename).getName ());

    }  //Main.createZipOutputFile()


    /**********************************************************************
     *  Creates the output file for an enhaced class- or zipfile. If the
     *  enhanced file is written back depends on the command line options.
     *
     *  @param  enhanced  Has the input file been enhanced?
     *  @param  filename  The name of the output file.
     *  @param  temp      The temp file, the output is written to.
     *
     *  @exception  IOException  If the file could not be created.
     *********************************************************************/

    private final void createOutputFile (boolean  enhanced,
                                         String   filename,
                                         File     temp)
                       throws IOException
    {

        //noWrite or (not enhanced and not forceWrite)
        if  (this.cmdLineOpts.noWrite  ||  ( ! enhanced  &&  ! this.cmdLineOpts.forceWrite))
        {
            temp.deleteOnExit ();
            return;
        }

        File file = new File (this.cmdLineOpts.destinationDirectory, filename);
        createPathOfFile (file);
        file.delete ();  //delete old file if exists
        //@olsen: added workaround to JDK bug with file.renameTo()
        boolean renamed = temp.renameTo(file);
        if (!renamed) {
            //@dave: empirical evidence shows that renameTo does not allow for
            // crossing filesystem boundaries.  If it fails, try "by hand".
            try {
                DataInputStream dis =
                    new DataInputStream(new FileInputStream(temp));
                DataOutputStream dos =
                    new DataOutputStream(new FileOutputStream(file));
                int PAGESIZE = 4096; // Suggest a better size?
                byte data[] = new byte[PAGESIZE];
                while (dis.available() > 0) {
                    int numRead = dis.read(data, 0, PAGESIZE);
                    dos.write(data, 0, numRead);
                }
                renamed = true;
                temp.delete ();  // delete temp file
            } catch (IOException ex) {
                // empty
            }
            if (!renamed) {
                throw new IOException("Could not rename temp file '" +
                                      temp.getAbsolutePath() +
                                      "' to '" + file.getAbsolutePath() + "'.");
            }
        }
    }


    /**********************************************************************
     *  Closes an input stream.
     *
     *  @param  in  The input stream.
     *********************************************************************/

    private final void closeInputStream (InputStream in)
    {

        if  (in != null)
        {
            try
            {
                in.close ();
            }
            catch (IOException ex)
            {
                printError (null, ex);
            }
        }

    }  //Main.closeInputStream()


    /**********************************************************************
     *  Closes an output stream.
     *
     *  @param  in  The output stream.
     *********************************************************************/

    private final void closeOutputStream (OutputStream out)
    {

        if  (out != null)
        {
            try
            {
                out.close ();
            }
            catch (IOException ex)
            {
                printError (null, ex);
            }
        }

    } //Main.closeOutputStream()


    /**********************************************************************
     *  Creates only the path of the given file.
     *
     *  @param  file  The file.
     *
     *  @exception  IOException  If an error occured.
     *********************************************************************/

    private static final void createPathOfFile (File file)
                              throws IOException
    {

        //Verzeichnis erzeugen
        File dir = file.getAbsoluteFile ().getParentFile ();
        if  ( ! dir.exists ()  &&  ! dir.mkdirs ())
        {
            throw new IOException ("Error creating directory '" + dir.getAbsolutePath () + "'.");
        }

    }  //Main.createPathOfFile()


    /**********************************************************************
     *  Prints out an error.
     *
     *  @param  msg  The error message (can be <code>null</code>).
     *  @param  ex   An optional exception (can be <code>null</code>).
     *********************************************************************/

    private final void printError (String    msg,
                                   Throwable ex)
    {

        if  (msg != null)
        {
            this.outErrors.println (msg + (ex != null  ?  ": " + ex  :  ""));
        }
        if (ex != null)
        {
            ex.printStackTrace (this.outErrors);
        }

    }  //Main.printError()


    /**********************************************************************
     *  Prints out a message.
     *
     *  @param  msg  The message.
     *********************************************************************/

    private final void printMessage (String msg)
    {

        this.outMessages.println (msg);

    }  //Main.printMessage()


    /**
     * Print a usage message to System.err
     */
    public static void usage() {
        //@olsen: document that main() takes a file name argument
        System.err.println("Usage: main <options> <file name>");
//@olsen: disabled feature
/*
        System.err.println("  { -copyclass | -cc }");
        System.err.println("  { -persistaware | -pa }");
        System.err.println("  { -persistcapable | -pc }");
*/
        System.err.println("  { -verbose | -v }");
        System.err.println("  { -force | -f }");
        System.err.println("  { -quiet | -q }");
//        System.err.println("  { -quietfield | -qf } <qualified field name>");
//        System.err.println("  { -quietclass | -qc } <qualified class name>");
//@olsen: disabled feature
/*
        System.err.println("  -inplace");
*/
//        System.err.println("  -noarrayopt");
//        System.err.println("  -nothisopt");
//        System.err.println("  -noinitializeropt");
//        System.err.println("  -noopt");
//@olsen: disabled feature
/*
        System.err.println("  -modifyjava");
*/
//@olsen: disabled feature
/*
        //@olsen: added missing  cmd line option
        System.err.println("  -modifyfinals");
*/
        System.err.println("  -nowrite");
        System.err.println(" -dest | -d <destination directory>");
//@olsen: disabled feature
/*
        System.err.println("  { -translatepackage | -tp } <orig pkg name> <new pkg name>");
*/
//@olsen: disabled feature
/*
        System.err.println("  { -arraydims | -a } <number of array dimensions>");
*/
//@olsen: disabled feature
/*
        System.err.println("  { -classpath | -cpath } <classpath for lookup>");
        System.err.println("  { -sysclasspath | -syscpath } <system classpath for lookup>");
*/
        //@olsen: new command line option for JDO meta data properties
        System.err.println("  --jdoProperties | -jp");
        System.err.println("  --timing | -t");

        System.err.println("  @<cmd-arg-file>");

        System.exit (1);
    }


    //#####################################################################
    /**
     *  A class for holding the command line options.
     */
    //#####################################################################

    private final class CmdLineOptions
    {


        /**
         *  The destination directory to write the enhanced classes to.
         */
        String destinationDirectory = null;


        /**
         *  Do timing messures?
         */
        boolean doTiming = false;


        /**
         *  Verbose output?
         */
        boolean verbose = false;


        /**
         *  No output at all?
         */
        boolean quiet = false;


        /**
         *  Force a write even if the classfile has not been enhanced?
         */
        boolean forceWrite = false;


        /**
         *  Really write the enhanced or not enhanced classes to disk?
         */
        boolean noWrite = false;


    }  //CmdLineOptions


}  //Main
