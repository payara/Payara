/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]
package org.glassfish.admin.rest.resources.custom;

import com.sun.enterprise.server.logging.logviewer.backend.LogFilter;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.Charset;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.admin.rest.adapter.LocatorBridge;
import org.glassfish.admin.rest.logviewer.CharSpool;
import org.glassfish.admin.rest.logviewer.LineEndNormalizingWriter;
import org.glassfish.admin.rest.logviewer.WriterOutputStream;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;

/**
 * Represents a large text data.
 * <p/>
 * <p/>
 * This class defines methods for handling progressive text update.
 * <p/>
 * <h2>Usage</h2>
 * <p/>
 *
 * @author Kohsuke Kawaguchi
 * @author Susan Rai
 */
//@Path("notification-log-view/")
public class NotificationLogViewer extends LogViewerResource {

    private static final Logger logger = Logger.getLogger("NotificationLogViewer");
    List<String> fileBody = new ArrayList<>();

    private Source source;

    @GET
    @Produces("text/plain;charset=UTF-8")
    @Override
    public Response get(@QueryParam("start")
            @DefaultValue("0") long start,
            @QueryParam("instanceName") @DefaultValue("server") String instanceName,
            @Context HttpHeaders hh) throws IOException {
        boolean gzipOK = true;
        MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
        String acceptEncoding = headerParams.getFirst("Accept-Encoding");
        if (acceptEncoding == null || !acceptEncoding.contains("gzip")) {
            gzipOK = false;
        }

        // getting logFilter object from habitat
        LogFilter logFilter = habitat.getRemoteLocator().getService(LogFilter.class);

        // getting log file location on DAS for server/local instance/remote instance
        String logLocation = logFilter.getLogFileForGivenTarget(instanceName);

        readServerLogFile(logLocation);
        String logFolderLocation = logLocation.replace("server.log", "");
        String notificationFolderLocation = logFolderLocation + File.separator + "notificationLogs";
        createNotificationLogFolder(notificationFolderLocation);
        String notificationLogLocation = notificationFolderLocation + File.separator + "notification.log";
        writeToNotificationLogFile(notificationLogLocation);
        initLargeText(new File(notificationLogLocation), false);

        if (!source.exists()) {
            // file doesn't exist yet
            UriBuilder uriBuilder = ui.getAbsolutePathBuilder();
            uriBuilder.queryParam("start", 0);
            uriBuilder.queryParam("instanceName", instanceName);

            return Response.ok(
                    new StreamingOutput() {

                @Override
                public void write(OutputStream out) throws IOException, WebApplicationException {
                }
            }).
                    header("X-Text-Append-Next", uriBuilder.build()).build();
        }

        if (source.length() < start) {
            start = 0;  // text rolled over
        }
        final CharSpool spool = new CharSpool();
        long size = writeLogTo(start, spool);

        //       response.addHeader("X-Text-Size", String.valueOf(r));
        // if (!completed) {
        //           response.addHeader("X-More-Data", "true");
        // }
        if (size < 10000) {
            gzipOK = false;
        }
        final boolean gz = gzipOK;
        ResponseBuilder rp = Response.ok(
                new StreamingOutput() {

            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                Writer w = getWriter(out, gz);
                spool.writeTo(new LineEndNormalizingWriter(w));
                w.flush();
                w.close();
            }
        });
        UriBuilder uriBuilder = ui.getAbsolutePathBuilder();
        uriBuilder.queryParam("start", size);
        uriBuilder.queryParam("instanceName", instanceName);
        URI next = uriBuilder.build();
        rp.header("X-Text-Append-Next", next);
        if (gzipOK) {
            rp = rp.header("Content-Encoding", "gzip");
        }
        return rp.build();
        //   return rp.header("X-Text-Size", String.valueOf(r)).header("X-More-Data", "true").build();
    }

    private Writer getWriter(OutputStream out, boolean gzipOK) throws IOException {
        if (gzipOK == false) {
            return new OutputStreamWriter(out);
        } else {
            return new OutputStreamWriter(new GZIPOutputStream(out), "UTF-8");
        }
    }

    public void createNotificationLogFolder(String FolderName) {
        File notificationFolder = new File(FolderName);
        if (!notificationFolder.exists()) {
            if (notificationFolder.mkdir()) {
                logger.log(Level.INFO, "Notification Folder was created on " + FolderName);
            } else {
                logger.log(Level.SEVERE, "Failed to create Notification Folder");
            }
        }
    }

    public void writeToNotificationLogFile(String fileName) {
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(fileName))) {
            for (String text : fileBody) {
                if (text.contains("WARNING") || text.contains("SEVERE") || text.contains("FINE")
                        || text.contains("CONFIG") || text.contains("INFO")) {
                    pw.println("\n" + text);
                } else {
                    pw.println(text);
                }
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

    }

    public void readServerLogFile(String file) {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                if (sCurrentLine.contains("LogNotifier") || sCurrentLine.contains("RequestEvent")
                        || sCurrentLine.contains("ServletRequestEvent") || sCurrentLine.contains("conversationId")
                        || sCurrentLine.contains("elapsedTime") || sCurrentLine.contains("user-agent")
                        || sCurrentLine.contains("requestTracing") || sCurrentLine.contains("alarmType")) {
                    fileBody.add(sCurrentLine);
                }
            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

    }

}
