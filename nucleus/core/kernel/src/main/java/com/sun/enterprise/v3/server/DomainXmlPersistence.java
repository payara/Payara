/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.glassfish.common.util.admin.ManagedFile;
import org.glassfish.config.support.ConfigurationAccess;
import org.glassfish.config.support.ConfigurationPersistence;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;

/**
 * domain.xml persistence.
 *
 * @author Jerome Dochez
 */
@Service
@Singleton
public class DomainXmlPersistence implements ConfigurationPersistence, ConfigurationAccess {

    @Inject
    ServerEnvironmentImpl env;
    @Inject
    protected Logger logger;
    @Inject
    ConfigModularityUtils modularityUtils;
    
    DomDocument skippedDoc = null;

    final XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();

    final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DomainXmlPersistence.class);    


    private synchronized ManagedFile getPidFile() throws IOException {
        File location=null;
        try {
            // I am locking indefinitely with a 2 seconds timeOut.
            location = new File(env.getConfigDirPath(), "lockfile");
            if (!location.exists()) {
                if (!location.createNewFile()) {
                    if (!location.exists()) {
                        String message = localStrings.getLocalString("cannotCreateLockfile",
                                "Cannot create lock file at {0}, configuration changes will not be persisted",
                                location);
                        logger.log(Level.SEVERE, message);
                        throw new IOException(message);
                    }
                }
            }
            return new ManagedFile(location, 2000, -1);
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    localStrings.getLocalString("InvalidLocation",
                            "Cannot obtain lockfile location {0}, configuration changes will not be persisted",
                            location), e);
            throw e;
        }
    }

    @Override
    public Lock accessRead() throws IOException, TimeoutException {
        return getPidFile().accessRead();
    }

    @Override
    public Lock accessWrite() throws IOException, TimeoutException {
        return getPidFile().accessWrite();
    }

    @Override
    public void save(DomDocument doc) throws IOException {
        if (modularityUtils.isIgnorePersisting() && !modularityUtils.isCommandInvocation()) {
            if (skippedDoc != null) {
                assert(doc == skippedDoc);
            }
            skippedDoc = doc;
            return;
        }
        File destination = getDestination();
        if (destination == null) {
            String msg = localStrings.getLocalString("NoLocation",
                    "domain.xml cannot be persisted, null destination");
            logger.severe(msg);
            throw new IOException(msg);
        }
        Lock writeLock=null;
        try {
            try {
                writeLock = accessWrite();
            } catch (TimeoutException e) {
                String msg = localStrings.getLocalString("Timeout",
                        "Timed out when waiting for write lock on configuration file");
                logger.log(Level.SEVERE, msg);
                throw new IOException(msg, e);

            }

            // get a temporary file
            File f = File.createTempFile("domain", ".xml", destination.getParentFile());
            if (!f.exists()) {
                throw new IOException(localStrings.getLocalString("NoTmpFile",
                        "Cannot create temporary file when saving domain.xml"));
            }
            // write to the temporary file
            XMLStreamWriter writer = null;
            OutputStream fos = getOutputStream(f);
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
            File backup = new File(env.getConfigDirPath(), "domain.xml.bak");
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
        } catch(IOException e) {
            logger.log(Level.SEVERE, localStrings.getLocalString("ioexception",
                    "IOException while saving the configuration, changes not persisted"), e);
            throw e;
        } finally {
            if (writeLock!=null) {
                writeLock.unlock();
            }
        }
        skippedDoc = null;
        saved(destination);
    }

    /**
     * Update the modified time of the persisted domain.xml so that 
     * instances will detect it as changed.
     * This is for triggering instance synchronization to occur.
     */
    public void touch() throws IOException {
        getDestination().setLastModified(System.currentTimeMillis());
    } 

    protected void saved(File destination) {
        logger.fine("Configuration saved at " + destination);
    }

    protected File getDestination() throws IOException {
        return new File(env.getConfigDirPath(), "domain.xml");
    }

    protected OutputStream getOutputStream(File destination) throws IOException {
        return new FileOutputStream(destination);
    }
    
    /* 
     * The purpose of this service is to write out the domain.xml if any writes
     * were skipped during startup of the server. 
     */
    @Service
    @RunLevel(PostStartupRunLevel.VAL)
    static class SkippedWriteWriter implements PostConstruct {
        
        @Inject DomainXmlPersistence domPersist;
        @Inject Logger logger;

        @Override
        public void postConstruct() {
            DomDocument doc = domPersist.skippedDoc;
            if (doc != null) {
                try {
                    domPersist.save(doc);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, localStrings.getLocalString("ioexception",
                        "IOException while saving the configuration, changes not persisted"), e);
                }
            }
        } 
    }
}
