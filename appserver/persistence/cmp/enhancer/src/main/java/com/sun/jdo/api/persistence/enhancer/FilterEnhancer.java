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

/*
 * FilterEnhancer.java
 */

package com.sun.jdo.api.persistence.enhancer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.util.Properties;

import com.sun.jdo.api.persistence.model.Model;

import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaData;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataModelImpl;
import com.sun.jdo.api.persistence.enhancer.meta.JDOMetaDataPropertyImpl;

import com.sun.jdo.api.persistence.enhancer.impl.EnhancerControl;
import com.sun.jdo.api.persistence.enhancer.impl.Environment;
import com.sun.jdo.api.persistence.enhancer.impl.ClassControl;

import com.sun.jdo.api.persistence.enhancer.classfile.ClassFile;

//@olsen: added: support for I18N
import com.sun.jdo.api.persistence.enhancer.util.Support;
import com.sun.jdo.api.persistence.enhancer.util.UserException;
import com.sun.jdo.api.persistence.enhancer.util.ClassFileSource;


//@lars: the output stream is always written with the class - even if it hasn't been enhanced
//@lars: added an error-PrintWriter to all constructors
//@lars: changes to reflect the new ByteCodeEnhancer interface


/**
 * Implements a JDO enhancer as a byte-code filtering tool.
 */
//@olsen: added class
public class FilterEnhancer
    extends Support
    implements ByteCodeEnhancer
{
    static public final String DO_SIMPLE_TIMING
    = "ByteCodeEnhancer.doSimpleTiming";//NOI18N
    static public final String VERBOSE_LEVEL
    = "ByteCodeEnhancer.verboseLevel";//NOI18N
    static public final String VERBOSE_LEVEL_QUIET
    = "quiet";//NOI18N
    static public final String VERBOSE_LEVEL_WARN
    = "warn";//NOI18N
    static public final String VERBOSE_LEVEL_VERBOSE
    = "verbose";//NOI18N
    static public final String VERBOSE_LEVEL_DEBUG
    = "debug";//NOI18N

    /* Central repository for the options selected by
     * the user and the current state of the Filter execution */
    private Environment env = new Environment();

    private EnhancerControl econtrol = new EnhancerControl(env);

//    private StringWriter errString = new StringWriter();
//    private PrintWriter err = new PrintWriter(errString, true);

    /**
     * Initializes an instance of a JDO enhancer.
     * @param metaData the JDO meta-data object
     * @param settings enhancement properties
     * @param out standard ouput stream for the enhancer
     */
    protected void init(JDOMetaData metaData,
                        Properties  settings,
                        PrintWriter out,
                        PrintWriter err)
        throws EnhancerUserException, EnhancerFatalError
    {
        if (metaData == null) {
            //@olsen: support for I18N
            throw new EnhancerFatalError(
                getI18N("enhancer.internal_error",//NOI18N
                        "Illegal argument: metaData == null"));//NOI18N
        }

        env.setJDOMetaData(metaData);

        // set verbose level
        if  (err != null)
        {
            env.setErrorWriter(err);
        }
        if  (out != null)
        {
            env.setOutputWriter(out);
        }
        final String verboseLevel
            = (settings == null ? null : settings.getProperty(VERBOSE_LEVEL));
        if (VERBOSE_LEVEL_QUIET.equals(verboseLevel)) {
            env.setVerbose(false);
            env.setQuiet(true);
        } else if (VERBOSE_LEVEL_WARN.equals(verboseLevel)) {
            env.setVerbose(false);
            env.setQuiet(false);
        } else if (VERBOSE_LEVEL_VERBOSE.equals(verboseLevel)) {
            env.setVerbose(true);
            env.setQuiet(false);
        } else if (VERBOSE_LEVEL_DEBUG.equals(verboseLevel)) {
            env.setVerbose(true);
            env.setQuiet(false);
        } else {
            env.setVerbose(false);
            env.setQuiet(false);
        }

        //@olsen: force settings
        env.setNoOptimization(true);
        env.messageNL("FilterEnhancer: forced settings: -noopt");//NOI18N
    }

    /**
     * Creates an instance of a JDO enhancer.
     * @param metaData the JDO meta-data object
     * @param settings enhancement properties
     * @param out standard ouput stream for the enhancer
     */
    public FilterEnhancer(JDOMetaData metaData,
                          Properties  settings,
                          PrintWriter out,
                          PrintWriter err)
        throws EnhancerUserException, EnhancerFatalError
    {
        init(metaData, settings, out, err);
    }

    /**
     * Creates an instance of a JDO enhancer.
     * @param metaData the JDO meta-data properties
     * @param settings enhancement properties
     * @param out standard ouput stream for the enhancer
     */
    public FilterEnhancer(Properties  metaData,
                          Properties  settings,
                          PrintWriter out,
                          PrintWriter err)
        throws EnhancerUserException, EnhancerFatalError
    {
        if (metaData == null) {
            //@olsen: support for I18N
            throw new EnhancerFatalError(
                getI18N("enhancer.internal_error",//NOI18N
                        "Illegal argument: metaData == null"));//NOI18N
        }

        final JDOMetaData meta
            = new JDOMetaDataPropertyImpl(metaData, out);
        init(meta, settings, out, err);
    }

    /**
     * Creates an instance of a JDO enhancer.
     * @param metaData the JDO model
     * @param settings enhancement properties
     * @param out standard ouput stream for the enhancer
     */
    public FilterEnhancer(Model       metaData,
                          Properties  settings,
                          PrintWriter out,
                          PrintWriter err)
        throws EnhancerUserException, EnhancerFatalError
    {
        if (metaData == null) {
            //@olsen: support for I18N
            throw new EnhancerFatalError(
                getI18N("enhancer.internal_error",//NOI18N
                        "Illegal argument: metaData == null"));//NOI18N
        }

        final JDOMetaData meta
            = new JDOMetaDataModelImpl(metaData,
                                       env.getOutputWriter());
        init(meta, settings, out, err);
    }


    /**
     * Enhances a given class according to the JDO meta-data.
     */
    public boolean enhanceClassFile(InputStream         inByteCode,
                                    OutputStreamWrapper outByteCode)
        throws EnhancerUserException, EnhancerFatalError
    {
        env.messageNL("FilterEnhancer: enhancing classfile ...");//NOI18N

        // reset environment to clear class map etc.
        env.reset();

        // enhance class file; check Exceptions
        final boolean changed;
        try {
            changed = enhanceClassFile1(inByteCode, outByteCode);
        } catch (UserException ex) {
            // note: catch UserException before RuntimeException

            // reset environment to clear class map etc.
            env.reset();
            //@olsen: support for I18N
            throw new EnhancerUserException(
                getI18N("enhancer.error",//NOI18N
                        ex.getMessage()),
                ex);
        } catch (RuntimeException ex) {
            // note: catch UserException before RuntimeException

            // reset environment to clear class map etc.
            env.reset();
            //@olsen: support for I18N
            ex.printStackTrace ();
            throw new EnhancerFatalError(
                getI18N("enhancer.internal_error",//NOI18N
                        ex.getMessage()),
                ex);
        }

        env.messageNL(changed
                      ? "FilterEnhancer: classfile enhanced successfully."//NOI18N
                      : "FilterEnhancer: classfile not changed.");//NOI18N
        return changed;
    }

    /**
     * Enhances a given class according to the JDO meta-data.
     */
    private boolean enhanceClassFile1(InputStream         inByteCode,
                                      OutputStreamWrapper outByteCode)
    {
        // check arguments
        affirm(inByteCode, "Illegal argument: inByteCode == null.");//NOI18N
        affirm(outByteCode, "Illegal argument: outByteCode == null.");//NOI18N

        // parse class
        final ClassFileSource cfs;
        final ClassFile cf;
        final ClassControl cc;
        try {
            // create class file source
            cfs = new ClassFileSource(null, inByteCode);

            // create class file
            final DataInputStream dis = cfs.classFileContents();
            cf = new ClassFile(dis);
//@lars: do not close the input stream
//            dis.close();

            // create class control
            cc = new ClassControl(cfs, cf, env);
            env.addClass(cc);

            // get real class name
            final String className = cc.className();
            cfs.setExpectedClassName(className);
        } catch (IOException ex) {
            //@olsen: support for I18N
            throw new UserException(
                getI18N("enhancer.io_error_while_reading_stream"),//NOI18N
                ex);
        } catch (ClassFormatError ex) {
            //@olsen: support for I18N
            throw new UserException(
                getI18N("enhancer.class_format_error"),//NOI18N
                ex);
        }

        // enhance class
        econtrol.modifyClasses();
        if (env.errorCount() > 0) {
            // retrieve error messages
            env.getErrorWriter ().flush ();
            /*
            final String str = errString.getBuffer().toString();

            // reset env's error writer
            errString = new StringWriter();
            err = new PrintWriter(errString, true);
            env.setErrorWriter(err);
            */

            //@olsen: support for I18N
            throw new UserException(env.getLastErrorMessage ());
        }

        // write class
        boolean changed = (cc.updated() && cc.filterRequired());
        try {
            if (changed)
            {
                env.message("writing enhanced class " + cc.userClassName()//NOI18N
                            + " to output stream");//NOI18N
            }
            else
            {
                env.message("no changes on class " + cc.userClassName());
            }
            outByteCode.setClassName (cc.userClassName ());
            final DataOutputStream dos = new DataOutputStream(outByteCode.getStream ());
            cf.write(dos);
            dos.flush();
        } catch (IOException ex) {
            //@olsen: support for I18N
            throw new UserException(
                getI18N("enhancer.io_error_while_writing_stream"),//NOI18N
                ex);
        }
        return changed;
    }


    /**********************************************************************
     *
     *********************************************************************/

    public boolean enhanceClassFile (InputStream  in,
                                     OutputStream out)
                   throws EnhancerUserException,
                          EnhancerFatalError
    {

        return enhanceClassFile (in, new OutputStreamWrapper (out));

    }  //FilterEnhancer.enhanceClassFile()


}  //FilterEnhancer
