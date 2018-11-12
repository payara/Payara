/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
//Portions Copyright [2016-2018] [Payara Foundation]

package com.sun.enterprise.admin.cli.embeddable;

import com.sun.enterprise.universal.GFBase64Encoder;
import org.glassfish.admin.payload.PayloadFilesManager;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.Payload;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFishException;
import org.jvnet.hk2.annotations.ContractsProvided;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.api.InternalSystemAdministrator;

/**
 * This is an implementation of {@link Deployer}.
 * Unlike the other EmbeddedDeployer, this deployer uses admin command execution
 * framework to execute the underlying command, as a result we don't by-pass things like command replication code.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */

@Service()
@PerLookup
@ContractsProvided({DeployerImpl.class, Deployer.class}) // bcos Deployer interface can't depend on HK2, we need ContractProvided here.
public class DeployerImpl implements Deployer {

    private static final Logger logger = Logger.getLogger(DeployerImpl.class.getPackage().getName());

    /*
     * This class currently copies generic URIs to a file before processing. Once deployment backend
     * supports URI, we should be able to use URIs directly.
     */

    @Inject
    ServiceLocator habitat;
    
    @Inject
    private InternalSystemAdministrator kernelIdentity;

    @Override
    public String deploy(URI archive, String... params) throws GlassFishException {
        File file;
        try {
            file = convertToFile(archive);
        } catch (IOException e) {
            throw new GlassFishException("Unable to make a file out of " + archive, e);
        }
        return deploy(file, params);
    }

    @Override
    public String deploy(File file, String... params) throws GlassFishException {
        String[] newParams = new String[params.length + 1];
        System.arraycopy(params, 0, newParams, 0, params.length);
        newParams[params.length] = file.getAbsolutePath();
        CommandExecutorImpl executer = habitat.getService(CommandExecutorImpl.class);
        try {
            String command = "deploy";
            ActionReport actionReport = executer.createActionReport();
            ParameterMap commandParams = executer.getParameters(command, newParams);
            org.glassfish.api.admin.CommandRunner.CommandInvocation inv =
                    executer.getCommandRunner().getCommandInvocation(command, actionReport, kernelIdentity.getSubject());
            inv.parameters(commandParams);
            // set outputbound payload if --retrieve option is specified.
            Payload.Outbound outboundPayload = null;
            String retrieveOpt = commandParams.getOne("retrieve");
            File retrieve = retrieveOpt != null ? new File(retrieveOpt) : null;
            if (retrieve != null && retrieve.exists()) {
                outboundPayload = PayloadImpl.Outbound.newInstance();
                inv.outbound(outboundPayload);
            }
            inv.execute();
            // extract the outbound payload.
            if (outboundPayload != null) {
                extractPayload(outboundPayload, actionReport, retrieve);
            }
            
            return actionReport.getResultType(String.class);
        } catch (CommandException e) {
            throw new GlassFishException(e);
        }
    }

    @Override
    public String deploy(InputStream is, String... params) throws GlassFishException {
        try {
            return deploy(createFile(is), params);
        } catch (IOException e) {
            throw new GlassFishException(e);
        }
    }

    @Override
    public void undeploy(String appName, String... params) throws GlassFishException {
        String[] newParams = new String[params.length + 1];
        System.arraycopy(params, 0, newParams, 0, params.length);
        newParams[params.length] = appName;
        CommandExecutorImpl executer = habitat.getService(CommandExecutorImpl.class);
        try {
            ActionReport actionReport = executer.executeCommand("undeploy", newParams);
            if (actionReport.hasSuccesses()) {
                logger.log(Level.INFO, "{0} was successfully undeployed", appName);
            }
        } catch (CommandException e) {
            throw new GlassFishException(e);
        }
    }

    @Override
    public Collection<String> getDeployedApplications() throws GlassFishException {
        try {
            CommandExecutorImpl executer = habitat.getService(CommandExecutorImpl.class);
            ActionReport report = executer.executeCommand("list-components");
            Properties props = report.getTopMessagePart().getProps();
            return new ArrayList<String>(props.stringPropertyNames());
        } catch (Exception e) {
            throw new GlassFishException(e);
        }
    }

    private File convertToFile(URI archive) throws IOException {
        File file;
        if ("file".equalsIgnoreCase(archive.getScheme())) {
            file = new File(archive);
        } else {
            URL urlArchive = archive.toURL();
            String auth = urlArchive.getUserInfo();
            HttpURLConnection httpConnection = (HttpURLConnection) urlArchive.openConnection();
                    if (auth != null) {
                        String encodedAuth = new GFBase64Encoder().encode(auth.getBytes());
                        httpConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                    }
            
            file = createFile(httpConnection.getInputStream());
        }
        return file;
    }

    private File createFile(InputStream in) throws IOException {
        File file;
        file = File.createTempFile("app", "tmp");
        file.deleteOnExit();
        try (OutputStream out = new FileOutputStream(file)) {
            copyStream(in, out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return file;
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
    }

    /**
     * Extract the payload (client side stub jar files) to the directory specified via
     * --retrieve option.
     *
     * @param outboundPayload Payload to be extracted
     * @param actionReport    ActionReport of the deploy command.
     * @param retrieveDir     Directory where the payload should be extracted to.
     */
    private void extractPayload(Payload.Outbound outboundPayload,
                                ActionReport actionReport, File retrieveDir) {
        File payloadZip = null;
        FileOutputStream payloadOutputStream = null;
        FileInputStream payloadInputStream = null;
        try {
            /*
            * Add the report to the payload to mimic what the normal
            * non-embedded server does.
            */
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            actionReport.writeReport(baos);
            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            final Properties reportProps = new Properties();
            reportProps.setProperty("data-request-type", "report");
            outboundPayload.addPart(0, actionReport.getContentType(), "report", reportProps, bais);

            /*
            * Now process the payload as an *inbound* payload as the non-embedded
            * admin client does, by writing the *outbound* payload to a temporary file
            * then reading from that file.
            */
            payloadZip = File.createTempFile("appclient", ".zip");
            payloadOutputStream = new FileOutputStream(payloadZip);
            outboundPayload.writeTo(payloadOutputStream);
            payloadOutputStream.flush();
            payloadOutputStream.close();

            /*
            * Use the temp file's contents as the inbound payload to
            * correctly process the downloaded files.
            */
            final PayloadFilesManager pfm = new PayloadFilesManager.Perm(
                    retrieveDir, null /* no action report to record extraction results */, logger);
            payloadInputStream = new FileInputStream(payloadZip);
            final PayloadImpl.Inbound inboundPayload = PayloadImpl.Inbound.newInstance(
                    "application/zip", payloadInputStream);
            pfm.processParts(inboundPayload); // explodes the payloadZip.
        } catch (Exception ex) {
            // Log error and ignore exception.
            logger.log(Level.WARNING, ex.getMessage(), ex);
        } finally {
            if (payloadOutputStream != null) {
                try {
                    payloadOutputStream.close();
                } catch (IOException ioex) {
                    logger.log(Level.WARNING, ioex.getMessage());
                }
            }
            if (payloadInputStream != null) {
                try {
                    payloadInputStream.close();
                } catch (IOException ioex) {
                    logger.log(Level.WARNING, ioex.getMessage());
                }
            }
            if (payloadZip != null && !payloadZip.delete()) {
                    logger.log(Level.WARNING, "Cannot delete payload: {0}", payloadZip.toString());
            }
        }
    }

}
