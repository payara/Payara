/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.tenantmanager.impl;

import java.beans.PropertyChangeEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;

@Service
public class TenantTransactionListener implements ConfigListener {
    @Inject
    protected Logger logger;

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        // assume there is a change and all changes are for one document
        PropertyChangeEvent event = events[0];
        ConfigBeanProxy source = (ConfigBeanProxy) event.getSource();
        ConfigBean configBean = (ConfigBean) Dom.unwrap(source);
        TenantDocument doc = (TenantDocument) configBean.document;
        try {
            save(doc);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    // TODO: refactor GlassFishDocument and DomainXmlPersistance to let CTM reuse it
    private void save(TenantDocument doc) throws IOException {
        URI filePath; 
        try {
            // alternatively tmConfig.getFileStore().toString() + doc.getRoot().getName? 
            filePath = doc.getResource().toURI();
        } catch (URISyntaxException e1) {
            // can not happen
            throw new IOException(e1);
        } 
        File destination = new File(filePath);

        // get a temporary file
        File f = File.createTempFile("domain", ".xml", destination.getParentFile());
        if (!f.exists()) {
            throw new IOException(localStrings.getLocalString("NoTmpFile",
                    "Cannot create temporary file when saving domain.xml"));
        }

        // write to the temporary file
        XMLStreamWriter writer = null;
        OutputStream fos = new FileOutputStream(f);
        try {
            writer = xmlFactory.createXMLStreamWriter(new BufferedOutputStream(fos));
            IndentingXMLStreamWriter indentingXMLStreamWriter = new IndentingXMLStreamWriter(writer);
            doc.writeTo(indentingXMLStreamWriter);
            indentingXMLStreamWriter.close();
        }
        catch (XMLStreamException e) {
            String msg = localStrings.getLocalString("TmpFileNotSaved",
                            "Configuration could not be saved to temporary file");
            logger.log(Level.SEVERE, msg, e);
            throw new IOException(e.getMessage(), e);
            // return after calling finally clause, because since temp file couldn't be saved,
            // renaming should not be attempted
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (XMLStreamException e) {
                    logger.log(Level.SEVERE, localStrings.getLocalString("CloseFailed", 
                            "Cannot close configuration writer stream"), e);
                    throw new IOException(e.getMessage(), e);
                }
            }
            fos.close();
        }        

        // backup the current file
        File backup = new File(destination.getParentFile(), "domain.xml.bak");
        if (destination.exists() && backup.exists() && !backup.delete()) {
            String msg = localStrings.getLocalString("BackupDeleteFailed",
                    "Could not delete previous backup file at {0}" , backup.getAbsolutePath());
            logger.severe(msg);
            throw new IOException(msg);
        }
        if (destination.exists() && !FileUtils.renameFile(destination, backup)) {
            String msg = localStrings.getLocalString("TmpRenameFailed",
                    "Could not rename {0} to {1}",  destination.getAbsolutePath() , backup.getAbsolutePath());
            logger.severe(msg);
            throw new IOException(msg);
        }
        // save the temp file to domain.xml
        if (!FileUtils.renameFile(f, destination)) {
            String msg = localStrings.getLocalString("TmpRenameFailed",
                    "Could not rename {0} to {1}",  f.getAbsolutePath() , destination.getAbsolutePath());
            // try to rename backup to domain.xml (so that at least something is there)
            if (!FileUtils.renameFile(backup, destination)) {
                msg += "\n" + localStrings.getLocalString("RenameFailed",
                        "Could not rename backup to {0}", destination.getAbsolutePath());
            }
            logger.severe(msg);
            throw new IOException(msg);
        }
    }

    final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(TenantTransactionListener.class);    

    final XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
}