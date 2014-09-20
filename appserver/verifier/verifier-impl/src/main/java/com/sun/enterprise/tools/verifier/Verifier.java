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

package com.sun.enterprise.tools.verifier;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.apiscan.stdapis.APIRepository;
import com.sun.enterprise.tools.verifier.app.ApplicationVerifier;
import com.sun.enterprise.tools.verifier.appclient.AppClientVerifier;
import com.sun.enterprise.tools.verifier.connector.ConnectorVerifier;
import com.sun.enterprise.tools.verifier.ejb.EjbVerifier;
import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.VerifierConstants;
import com.sun.enterprise.tools.verifier.web.WebVerifier;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This class is the main class to invoke the verification process. It
 * is directly called by the scripts in AVK and verifier in appserver.
 * The deployment backend invokes verifier in a separate process.
 * Deploytool GUI invokes verifier by calling the verify() and
 * generateReports() APIs.
 */
@Service
@PerLookup
public class Verifier implements org.glassfish.internal.deployment.Verifier
{
    @Inject
    private DescriptorFactory descriptorFactory;

    @Inject
    private ClassLoaderHierarchy clh;

    @Inject
    private ServerEnvironment env;

    private Application application = null;

    private static boolean debug = false;
    private static Logger logger = LogDomains.getLogger(
            LogDomains.AVK_VERIFIER_LOGGER);
    private LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();

    private final String TMPDIR = System.getProperty("java.io.tmpdir");
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(
            "yyyyMMddhhmmss"); // NOI18N
    private String explodeDirName = TMPDIR + File.separator + "exploded" + // NOI18N
            dateFormatter.format(new Date());

    /**
     * contains arguments data. It is used throughout the verification framework
     */
    private VerifierFrameworkContext verifierFrameworkContext = null;

    public Verifier()
    {
        // This needs to be done by deployment backlend
//        verificationContext.setUseTimeStamp(true);
//        verificationContext.setOutputDirName(System.getProperty("com.sun.aas.instanceRoot") + // NOI18N
//                                                            File.separator +
//                                                            "logs" + // NOI18N
//                                                            File.separator +
//                                                            "verifier-results"); // NOI18N
    }

    public void verify(DeploymentContext context) {
        com.sun.enterprise.tools.verifier.VerifierFrameworkContext verifierFrameworkContext = new com.sun.enterprise.tools.verifier.VerifierFrameworkContext();
        verifierFrameworkContext.setArchive(context.getSource());
        verifierFrameworkContext.setApplication(context.getModuleMetaData(com.sun.enterprise.deployment.Application.class));
        verifierFrameworkContext.setJarFileName(context.getSourceDir().getAbsolutePath());
        verifierFrameworkContext.setJspOutDir(context.getScratchDir("jsp"));
        verifierFrameworkContext.setIsBackend(true);
        verifierFrameworkContext.setOutputDirName(env.getDomainRoot().getAbsolutePath()+"/logs/verifier-results");
        com.sun.enterprise.tools.verifier.ResultManager rm = verifierFrameworkContext.getResultManager();

        try {
            init(verifierFrameworkContext);
            verify();
        } catch (Exception e) {
            LogRecord logRecord = new LogRecord(Level.SEVERE,
                                "Could not verify successfully.");
            logRecord.setThrown(e);
            verifierFrameworkContext.getResultManager().log(logRecord);
        }

        try {
            generateReports();
        } catch (IOException ioe) {
            context.getLogger().log(
                Level.WARNING, "Can not generate verifier report: {0}", ioe.getMessage());
        }
        int failedCount = rm.getFailedCount() + rm.getErrorCount();
        if (failedCount != 0) {
            ((ExtendedDeploymentContext)context).clean();
            throw new DeploymentException(smh.getLocalString("deploy.failverifier","Some verifier tests failed. Aborting deployment"));
        }        
    }

    public void deployed(DeploymentContext context) {
        
    }

    public void init(VerifierFrameworkContext verifierFrameworkContext) throws IOException
    {
        this.verifierFrameworkContext = verifierFrameworkContext;
        InputStream is = getClass().getClassLoader().getResourceAsStream(
                VerifierConstants.CFG_RESOURCE_PREFIX + "standard-apis.xml"); // NOI18N
        try
        {
            APIRepository.Initialize(is);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (is != null) {
                is.close();
            }
        }
        if (!verifierFrameworkContext.isBackend())
        {
            try
            {
                initStandalone();
            }
            catch (IOException ioe)
            {
                cleanup();
                throw ioe;
            }
            catch (RuntimeException re)
            {
                cleanup();
                throw re;
            }
        }
    }

    /**
     * This method does the verification by running all the verifier tests
     *
     * @return ResultManager that contains all the test results.
     * @throws IOException
     */
    public void verify() throws IOException
    {
        assert (verifierFrameworkContext != null);
        try
        {
            verifyArchive();
        }
        finally
        {
            cleanup();
        }
    }

//    /**
//     * @param jarFile This method is called from gui MainPanel to run verifier
//     *                on selected archive
//     * @return ResultManager Object containing all test results
//     * @throws IOException
//     */
//    public ResultManager verify(String jarFile) throws IOException {
//        verificationContext.setJarFileName(jarFile);
//        return verify();
//    }

//    /**
//     * Call from deployment backend. This call is in the appserver process.
//     * Verifier will run in appserver mode for this invocation.
//     * If parameter application is null then this api is equivalent to
//     * invoking a standalone verifier.
//     * Parameter abstractArchive must not be null.
//     *
//     * @return status of the invocation. A non zero value will denote a failure.
//     * @throws IOException
//     */
//    public int verify(Application application,
//                      Archive abstractArchive,
//                      List<String> classPath,
//                      File jspOutDir)
//            throws IOException {
//        boolean originalBoundsChecking = Descriptor.isBoundsChecking();
//        Descriptor.setBoundsChecking(false);
//        ResultManager rmanager=null;
//        verificationContext.setJspOutDir(jspOutDir);
//        verificationContext.setIsBackend(true);
//        VerificationHandler verificationHandler = null;
//        try {
//            if(application == null) { //can be a standalone connector deployment
//                verificationContext.setJarFileName(new File(abstractArchive.getURI()).getAbsolutePath());
//                verificationHandler = new VerificationHandler(verificationContext);
//            } else
//                verificationHandler = new VerificationHandler(verificationContext,
//                                                              application,
//                                                              abstractArchive,
//                                                              classPath);
//            rmanager = verificationHandler.verifyArchive();
//        } catch(Exception e) {
//            LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();
//            LogRecord logRecord =
//                    new LogRecord(Level.SEVERE,
//                                 smh.getLocalString(getClass().getName() +
//                                                   ".verifyFailed", // NOI18N
//                                                   "Could not verify successfully.")); // NOI18N
//            logRecord.setThrown(e);
//            verificationContext.getResultManager().log(logRecord);
//        } finally { // restore the original values
//            Descriptor.setBoundsChecking(originalBoundsChecking);
//            if(verificationHandler!=null)
//                verificationHandler.cleanup();
//        }
//        generateReports();
//        return rmanager.getErrorCount() + rmanager.getFailedCount();
//    }

    /**
     * It generates the reports using the ResultManager
     *
     * @throws IOException
     */
    public void generateReports() throws IOException
    {
        new ReportHandler(verifierFrameworkContext).generateAllReports();
    }

    /**
     * checks if verifier is running in debug mode
     *
     * @return debug status
     */
    public static boolean isDebug()
    {
        return debug;
    }

    /**
     * debug messages are logged here.
     *
     * @param t
     */
    public static void debug(Throwable t)
    {
        logger.log(Level.FINEST, "Exception occurred", t);
    }

    private void verifyArchive()
    {
        if (!getApplication().isVirtual())
        { // don't run app tests for standalone module
            runVerifier(new ApplicationVerifier(verifierFrameworkContext));
        }

        for (Iterator itr = getApplication().getBundleDescriptors(EjbBundleDescriptor.class).iterator();
             itr.hasNext();)
        {
            EjbBundleDescriptor ejbd = (EjbBundleDescriptor) itr.next();
            runVerifier(new EjbVerifier(verifierFrameworkContext, ejbd));
        }

        for (Iterator itr = getApplication().getBundleDescriptors(WebBundleDescriptor.class).iterator();
             itr.hasNext();)
        {
            WebBundleDescriptor webd = (WebBundleDescriptor) itr.next();
            runVerifier(new WebVerifier(verifierFrameworkContext, webd));
        }

        for (Iterator itr = getApplication().getBundleDescriptors(ApplicationClientDescriptor.class).iterator();
             itr.hasNext();)
        {
            ApplicationClientDescriptor appClientDescriptor =
                    (ApplicationClientDescriptor) itr.next();
            runVerifier(new AppClientVerifier(verifierFrameworkContext, appClientDescriptor));
        }

        for (Iterator itr = getApplication().getBundleDescriptors(ConnectorDescriptor.class).iterator();
             itr.hasNext();)
        {
            ConnectorDescriptor cond = (ConnectorDescriptor) itr.next();
            runVerifier(new ConnectorVerifier(verifierFrameworkContext, cond));
        }
    }

    /**
     * initialization done for standalone verifier invocation.
     *
     * @throws IOException
     */
    private void initStandalone() throws IOException
    {
        logger.log(Level.FINE, getClass().getName() + ".debug.startingLoadJar");
        // We will set PublicAPI class loader as our parent, so we don't
        // need any server classpath.
//        if (!verificationContext.isPortabilityMode())
//        {
//            String as_config_dir =
//                    System.getProperty("com.sun.aas.installRoot") + File.separator + "config";
//            classPath = PELaunch.getServerClassPath(as_config_dir,
//                                                    verificationContext.getDomainDir());
//        }
//
        // initialize /tmp/* directories
        initVerifierTmpDirs();
        String jarFile = verifierFrameworkContext.getJarFileName();
        //We must call InstalledLibrariesResolver.initializeInstalledLibRegistry() before explodeArchive,
        //because inside this call, the list of installed optional packages in the system gets initialised.
        //That list is then used inside resolveDependencies() code.
        //It looks to be a bug as ideally this kind of dependency should be taken care of inside
        //InstalledLibrariesResolver class itself.
        //But any way, we don't have a choice but to make this work around in our code.
        InstalledLibrariesResolver.initializeInstalledLibRegistry(env.getLibPath().getAbsolutePath());
        DescriptorFactory.ResultHolder result =
            descriptorFactory.createApplicationDescriptor(
                new File(jarFile),
                new File(explodeDirName),
                clh.getCommonClassLoader());
        verifierFrameworkContext.setApplication(result.application);
        verifierFrameworkContext.setArchive(result.archive);
    }

    private void runVerifier(BaseVerifier baseVerifier)
    {
        try
        {
            baseVerifier.verify();
        }
        catch (Exception e)
        {
            log("Problem in running tests for :" +
                    baseVerifier.getDescriptor().getName(),
                    e);
        }
    }

    private boolean initVerifierTmpDirs() throws IOException
    {
        // Make sure we can create the directory appservResultDir
        File test = new File(explodeDirName);
        if (!test.isDirectory() && !test.getAbsoluteFile().mkdirs())
        {
            logger.log(Level.SEVERE, getClass().getName() +
                    ".explodedircreateerror", test.getAbsolutePath()); // NOI18N
            throw new IOException(smh.getLocalString(getClass().getName()
                    + ".explodedircreateerror", test.getAbsolutePath())); // NOI18N
        }
        return true;
    }

    public void cleanup()
    {
//        if(!isBackend && application!=null)
//            ((JarClassLoader)application.getClassLoader()).done();
        if (!verifierFrameworkContext.isBackend() &&
                !((new File(verifierFrameworkContext.getJarFileName())).isDirectory()))
        {
            FileUtils.whack(new File(explodeDirName));
        }
    }

    /**
     * This method is used to log exception messges in the error vector of
     * ResultManager object.
     *
     * @param message
     * @param e
     */
    private void log(String message, Exception e)
    {
        if (message == null)
        {
            message = "";
        }
        LogRecord logRecord = new LogRecord(Level.SEVERE, message);
        logRecord.setThrown(e);
        verifierFrameworkContext.getResultManager().log(logRecord);
    }


    public Application getApplication() {
        return verifierFrameworkContext.getApplication();
    }

}
