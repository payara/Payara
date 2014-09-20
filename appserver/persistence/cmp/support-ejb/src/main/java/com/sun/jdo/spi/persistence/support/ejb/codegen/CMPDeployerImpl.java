/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdo.spi.persistence.support.ejb.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javax.inject.Inject;
import javax.inject.Named;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.enterprise.config.serverbeans.JavaConfig;
import com.sun.enterprise.deployment.Application;
import com.sun.jdo.spi.persistence.support.ejb.ejbc.CMPProcessor;
import com.sun.jdo.spi.persistence.support.ejb.ejbc.JDOCodeGenerator;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.EJBHelper;
import com.sun.jdo.spi.persistence.utility.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;
import org.glassfish.ejb.spi.CMPDeployer;
import org.glassfish.persistence.common.I18NHelper;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 * Generates concrete impls for CMP beans in an archive. 
 *
 * @author Nazrul Islam
 * @since  JDK 1.4
 */
@Service
public class CMPDeployerImpl implements CMPDeployer {

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME) @Optional
    private JavaConfig javaConfig;

    /**
     * Generates the concrete impls for all CMPs in the application.
     *
     * @throws DeploymentException if this exception was thrown while generating concrete impls
     */
    public void deploy(DeploymentContext ctx) throws DeploymentException {
        
        // deployment descriptor object representation for the archive
        Application application = null;

        // deployment descriptor object representation for each module
        EjbBundleDescriptorImpl bundle = null;

        // ejb name
        String beanName = null;

        // GeneratorException message if any
        StringBuffer generatorExceptionMsg = null; 

        try {
            CMPGenerator gen = new JDOCodeGenerator();

            // stubs dir for the current deployment (generated/ejb)
            File stubsDir = ctx.getScratchDir("ejb"); //NOI18N

            application = ctx.getModuleMetaData(Application.class);

            if (_logger.isLoggable(Logger.FINE)) {
                _logger.fine( "cmpc.processing_cmp",  //NOI18N
                        application.getRegistrationName());
            }

            List<File> cmpFiles = new ArrayList<File>();
            final ClassLoader jcl = application.getClassLoader();

            bundle = ctx.getModuleMetaData(EjbBundleDescriptorImpl.class);
                
            // This gives the dir where application is exploded
            String archiveUri = ctx.getSource().getURI().getSchemeSpecificPart();

            if (_logger.isLoggable(Logger.FINE)) {
                _logger.fine("[CMPC] Module Dir name is " //NOI18N
                        + archiveUri);
            }

            // xml dir for the current deployment (generated/xml)
            String generatedXmlsPath = ctx.getScratchDir("xml").getCanonicalPath();

            if (_logger.isLoggable(Logger.FINE)) {
                _logger.fine("[CMPC] Generated XML Dir name is " //NOI18N
                        + generatedXmlsPath);
            }

            try {
                long start = System.currentTimeMillis();
                gen.init(bundle, ctx, archiveUri, generatedXmlsPath);
                
                Iterator ejbs=bundle.getEjbs().iterator();
                while ( ejbs.hasNext() ) {

                    EjbDescriptor desc = (EjbDescriptor) ejbs.next();
                    beanName = desc.getName();

                    if (_logger.isLoggable(Logger.FINE)) {
                        _logger.fine("[CMPC] Ejb Class Name: " //NOI18N
                                           + desc.getEjbClassName());
                    }
    
                    if ( desc instanceof IASEjbCMPEntityDescriptor) {
    
                        // generate concrete CMP class implementation
                        IASEjbCMPEntityDescriptor entd = 
                                (IASEjbCMPEntityDescriptor)desc;
    
                        if (_logger.isLoggable(Logger.FINE)) {
                            _logger.fine(
                                    "[CMPC] Home Object Impl name  is " //NOI18N
                                    + entd.getLocalHomeImplClassName());
                        }
    
                        // The classloader needs to be set else we fail down the road.
                        ClassLoader ocl = entd.getClassLoader();
                        entd.setClassLoader(jcl);
                    
                        try {
                            gen.generate(entd, stubsDir, stubsDir);
                        } catch (GeneratorException e) {
                            String msg = e.getMessage();
                            _logger.warning(msg);
                            generatorExceptionMsg = addGeneratorExceptionMessage(
                                    msg, generatorExceptionMsg);
                        }  finally {
                            entd.setClassLoader(ocl);
                        }

                    /* WARNING: IASRI 4683195
                     * JDO Code failed when there was a relationship involved
                     * because it depends upon the orginal ejbclasname and hence
                     * this code is shifted to just before the Remote Impl is
                     * generated.Remote/Home Impl generation depends upon this
                     * value
                     */
    
                    }

                } // end while ejbs.hasNext()
                beanName = null;

                cmpFiles.addAll(gen.cleanup());

                long end = System.currentTimeMillis();
                _logger.fine("CMP Generation: " + (end - start) + " msec");

            } catch (GeneratorException e) {
                String msg = e.getMessage();
                _logger.warning(msg);
                generatorExceptionMsg = addGeneratorExceptionMessage(msg, 
                        generatorExceptionMsg);
            } 

            bundle = null; // Used in exception processing

            // Compile the generated classes
            if (generatorExceptionMsg == null) {

                long start = System.currentTimeMillis();
                compileClasses(ctx, cmpFiles, stubsDir);
                long end = System.currentTimeMillis();

                _logger.fine("Java Compilation: " + (end - start) + " msec");

                 // Do Java2DB if needed
                start = System.currentTimeMillis();

                CMPProcessor processor = new CMPProcessor(ctx);
                processor.process();

                end = System.currentTimeMillis();
                _logger.fine("Java2DB processing: " + (end - start) + " msec");
                _logger.fine( "cmpc.done_processing_cmp", 
                        application.getRegistrationName());
            }

        } catch (GeneratorException e) {
            _logger.warning(e.getMessage());
            throw new DeploymentException(e);

        } catch (Throwable e) {
            String eType = e.getClass().getName();
            String appName = application.getRegistrationName();
            String exMsg = e.getMessage();

            String msg = null;
            if (bundle == null) {
                // Application or compilation error
                msg = I18NHelper.getMessage(messages,
                    "cmpc.cmp_app_error", eType, appName, exMsg);
            } else {
                String bundleName = bundle.getModuleDescriptor().getArchiveUri();
                if (beanName == null) {
                    // Module processing error
                    msg = I18NHelper.getMessage(messages,
                        "cmpc.cmp_module_error",
                        new Object[] {eType, appName, bundleName, exMsg});
                } else {
                    // CMP bean generation error
                    msg = I18NHelper.getMessage(messages,
                        "cmpc.cmp_bean_error",
                        new Object[] {eType, beanName, appName, bundleName, exMsg});
                }
            }

            _logger.log(Logger.SEVERE, msg, e);

            throw new DeploymentException(msg);
        }

        if (generatorExceptionMsg != null) {
            // We already logged each separate part.
            throw new DeploymentException(generatorExceptionMsg.toString());
        }
    }

    /**
     * Integration point for cleanup on undeploy or failed deploy.
     */
    public void clean(DeploymentContext ctx) {
        CMPProcessor processor = new CMPProcessor(ctx);
        processor.clean();
    }
        
    /**
     * Integration point for application unload
     */
    public void unload(ClassLoader cl) {
        try {
            EJBHelper.notifyApplicationUnloaded(cl);
        } catch (Exception e) {
            _logger.log(Logger.WARNING, "cmpc.cmp_cleanup_problems", e);
        }
    }
        
    /**
     * Compile .java files.
     *
     * @param    ctx          DeploymentContext associated with the call
     * @param    files        actual source files
     * @param    destDir      destination directory for .class files
     *
     * @exception  GeneratorException  if an error while code compilation
     */
    private void compileClasses(DeploymentContext ctx, List<File> files,
            File destDir) throws GeneratorException {

        if (files.isEmpty() ) {
            return;
        }

        // class path for javac
        String classPath = ctx.getTransientAppMetaData(CMPDeployer.MODULE_CLASSPATH, String.class); 
        List<String> options    = new ArrayList<String>();
        if (javaConfig!=null) {
            options.addAll(javaConfig.getJavacOptionsAsList());
        }

        StringBuffer msgBuffer = new StringBuffer();
        boolean compilationResult = false;
        try {
            // add the rest of the javac options
            options.add("-d");
            options.add(destDir.toString());
            options.add("-classpath");
            options.add(System.getProperty("java.class.path") //TODO do we need to add java.class.path for compilation?
                         + File.pathSeparator + classPath);

            if (_logger.isLoggable(Logger.FINE)) {
                for(File file : files) {
                    _logger.fine(I18NHelper.getMessage(messages,
                                    "cmpc.compile", file.getPath()));
                }

                StringBuffer sbuf = new StringBuffer();
                for ( String s : options) {
                    sbuf.append("\n\t").append(s);
                }
                _logger.fine("[CMPC] JAVAC OPTIONS: " + sbuf.toString());
            }

            // Using Java 6 compiler API to compile the generated .java files
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = 
                   new DiagnosticCollector<JavaFileObject>();
            StandardJavaFileManager manager = 
                    compiler.getStandardFileManager(diagnostics, null, null);
            Iterable compilationUnits = manager.getJavaFileObjectsFromFiles(files);

            long start = System.currentTimeMillis();
            long end = start;

            compilationResult = compiler.getTask(
                    null, manager, diagnostics, options, null, compilationUnits).call();

            end = System.currentTimeMillis();
            _logger.fine("JAVA compile time (" + files.size()
                    + " files) = " + (end - start));

            // Save compilation erros in msgBuffer to be used in case of failure
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                //Ignore NOTE about generated non safe code
                if (diagnostic.getKind().equals(Diagnostic.Kind.NOTE)) {
                    if (_logger.isLoggable(Logger.FINE)) {
                        msgBuffer.append("\n").append(diagnostic.getMessage(null));
                    }
                    continue;
                }
                msgBuffer.append("\n").append(diagnostic.getMessage(null));
            }

            manager.close();

        } catch(Exception jce) {
            _logger.fine("cmpc.cmp_complilation_exception", jce);
            String msg = I18NHelper.getMessage(messages,
                    "cmpc.cmp_complilation_exception",
                    new Object[] {jce.getMessage()} );
            GeneratorException ge = new GeneratorException(msg);
            ge.initCause(jce);
            throw ge;
        }

        if (!compilationResult) {
            // Log but throw an exception with a shorter message
            _logger.warning(I18NHelper.getMessage(messages, 
                    "cmpc.cmp_complilation_problems", msgBuffer.toString()));
            throw new GeneratorException(I18NHelper.getMessage(
                    messages, "cmpc.cmp_complilation_failed"));
        }

    }

    /** Adds GeneratorException message to the buffer.
     *
     * @param    msg     the message text to add to the buffer.
     * @param    buf    the buffer to use.
     * @return    the new or updated buffer.
     */
    private StringBuffer addGeneratorExceptionMessage(String msg, StringBuffer buf) {
        StringBuffer rc = buf;
        if (rc == null) 
            rc = new StringBuffer(msg);
        else 
            rc.append('\n').append(msg);

        return rc;
    }

    // ---- VARIABLE(S) - PRIVATE --------------------------------------
    private static final Logger _logger  = LogHelperCmpCompiler.getLogger();
    private static final ResourceBundle messages = I18NHelper.loadBundle(CMPDeployerImpl.class);

}
