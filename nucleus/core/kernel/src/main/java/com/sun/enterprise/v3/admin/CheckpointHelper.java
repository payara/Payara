/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.admin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.Payload.Inbound;
import org.glassfish.api.admin.Payload.Outbound;
import org.glassfish.api.admin.Payload.Part;

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.admin.JobManager;

/**
 * This class is starting point for persistent CheckpointHelper, and currently only
 * persists and restores AdminCommandContext with payloads in separate files.
 * 
 * @author Andriy Zhdanov
 * 
 */
public class CheckpointHelper {
    
    private static final CheckpointHelper instance = new CheckpointHelper();
    
    public static void save(JobManager.Checkpoint checkpoint, File contextFile) throws IOException {
        File outboundFile = null;
        File inboundFile = null;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(contextFile));
            oos.writeObject(checkpoint);
            oos.close();
            Outbound outboundPayload = checkpoint.getContext().getOutboundPayload();
            if (outboundPayload != null && outboundPayload.isDirty()) {
                outboundFile = new File(contextFile.getAbsolutePath() + ".outbound");
                instance.saveOutbound(outboundPayload, outboundFile);
            }
            Inbound inboundPayload = checkpoint.getContext().getInboundPayload();
            if (inboundPayload != null) {
                inboundFile = new File(contextFile.getAbsolutePath() + ".inbound");
                instance.saveInbound(inboundPayload, inboundFile);
            }
        } catch (IOException e) {
            contextFile.delete();
            if (outboundFile != null) {
                outboundFile.delete();
            }
            if (inboundFile != null) {
                inboundFile.delete();
            }
            throw e;
        }
    }

    public void saveAdminCommandContext(AdminCommandContext context, File contextFile) throws IOException {
        File outboundFile = null;
        File inboundFile = null;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(contextFile));
            oos.writeObject(context);
            oos.close();
            Outbound outboundPayload = context.getOutboundPayload();
            if (outboundPayload != null && outboundPayload.isDirty()) {
                outboundFile = new File(contextFile.getAbsolutePath() + ".outbound");
                saveOutbound(outboundPayload, outboundFile);
            }
            Inbound inboundPayload = context.getInboundPayload();
            if (inboundPayload!= null) {
                inboundFile = new File(contextFile.getAbsolutePath() + ".inbound");
                saveInbound(inboundPayload, inboundFile);
            }
        } catch (IOException e) {
            contextFile.delete();
            if (outboundFile != null) {
                outboundFile.delete();
            }
            if (inboundFile != null) {
                inboundFile.delete();
            }
            throw e;
        }
    }

    public AdminCommandContext loadAdminCommandContext(File contextFile, Outbound outbound) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(contextFile));
        AdminCommandContext context = (AdminCommandContext) ois.readObject();
        ois.close();
        File outboundFile = new File(contextFile.getAbsolutePath() + ".outbound");
        loadOutbound(outbound, outboundFile);
        context.setOutboundPayload(outbound);
        File inboundFile = new File(contextFile.getAbsolutePath() + ".inbound");
        Inbound inbound = loadInbound(inboundFile);
        context.setInboundPayload(inbound);
        return context;
    }

    void saveOutbound(Payload.Outbound outbound, File outboundFile) throws IOException {
        FileOutputStream os = new FileOutputStream(outboundFile);
        // Outbound saves text/plain with one part as text with no any details, force zip
        writePartsTo(outbound.parts(), os);
        outbound.resetDirty();
    }

    void loadOutbound(Outbound outbound, File outboundFile) throws IOException {
        Inbound outboundSource = loadInbound(outboundFile);
        Iterator<Part> parts = outboundSource.parts();
        File topDir = createTempDir("checkpoint", "");
        topDir.deleteOnExit();
        while (parts.hasNext()) {
            Part part = parts.next();
            File sourceFile = File.createTempFile("source", "", topDir);
            FileUtils.copy(part.getInputStream(), new FileOutputStream(sourceFile), Long.MAX_VALUE);
            outbound.addPart(part.getContentType(), part.getName(), part.getProperties(), new FileInputStream(sourceFile));
        }
        outbound.resetDirty();
    }

    void saveInbound(Payload.Inbound inbound, File inboundFile) throws IOException {
        if (!inboundFile.exists()) { // not saved yet
            FileOutputStream os = new FileOutputStream(inboundFile);
            writePartsTo(inbound.parts(), os);
        }
    }

    Inbound loadInbound(File inboundFile) throws IOException {
        FileInputStream is = new FileInputStream(inboundFile);
        Inbound inboundSource = PayloadImpl.Inbound.newInstance("application/zip", is);
        return inboundSource;
    }

    // ZipPayloadImpl
    
    private void writePartsTo(Iterator<Part> parts, OutputStream os) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(os);
        while (parts.hasNext()) {
            Part part = parts.next();
            prepareEntry(part, zos);
            part.copy(zos);
            zos.closeEntry();
        }
        zos.close();
    }

    private void prepareEntry(final Payload.Part part, final ZipOutputStream zos) throws IOException {
        ZipEntry entry = new ZipEntry(part.getName());
        entry.setExtra(getExtraBytes(part));
        zos.putNextEntry(entry);
    }

    private byte[] getExtraBytes(Part part) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Properties props = part.getProperties();
        Properties fullProps = new Properties();
        if (props != null) {
            fullProps.putAll(props);
        }
        fullProps.setProperty(CONTENT_TYPE_NAME, part.getContentType());
        try {
            fullProps.store(baos, null);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    File createTempDir(final String prefix, final String suffix) throws IOException {
        File temp = File.createTempFile(prefix, suffix);
        if ( ! temp.delete()) {
            throw new IOException("Cannot delete temp file " + temp.getAbsolutePath());
        }
        if ( ! temp.mkdirs()) {
            throw new IOException("Cannot create temp directory" + temp.getAbsolutePath());
        }
        return temp;
    }

    private static final String CONTENT_TYPE_NAME = "Content-Type";
    
}
