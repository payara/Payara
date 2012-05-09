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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.IndentingXMLStreamWriter;

import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * Override <code>make()</code> to create ConfigBean instead of Dom for
 * <code>ConfigSupport.apply()</code> and keep URL of origin resource to
 * persist changes later.
 * 
 * @author Andriy Zhdanov
 * 
 */
public class TenantDocument extends DomDocument<TenantConfigBean> {

    public TenantDocument(final Habitat habitat, URL resource, long lastModified) {
        super(habitat);

        this.resource = resource;
        this.lastModified = lastModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TenantConfigBean make(final Habitat habitat, XMLStreamReader xmlStreamReader,
            TenantConfigBean dom, ConfigModel configModel) {
        return new TenantConfigBean(habitat,this, dom, configModel, xmlStreamReader);
    }

    /**
     * Get origin resource URL.
     * 
     * @return URL of origin resource
     */
    public URL getResource() {
        return resource;
    }

    /**
     * Get tenant file last modified time.
     * 
     * @return file last modified time.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Reentrant per JVM process lock against document file.
     * 
     * @return Lock.
     */
    public Lock getLock() {
        return lock;
    }

    public boolean isLocked() {
        return lock.isLocked();
    }

    private void save(FileOutputStream fos) throws IOException {
        try {
            XMLStreamWriter writer = xmlFactory.createXMLStreamWriter(new BufferedOutputStream(fos));
            IndentingXMLStreamWriter indentingXMLStreamWriter = new IndentingXMLStreamWriter(writer);
            this.writeTo(indentingXMLStreamWriter);
            indentingXMLStreamWriter.close();
        }
        catch (XMLStreamException e) {
            String msg = localStrings.getLocalString("FileNotSaved",
                            "TenantDocument could not be saved");
            logger.log(Level.SEVERE, msg, e);
            throw new IOException(e.getMessage(), e);
            // return after calling finally clause, because since temp file couldn't be saved,
            // renaming should not be attempted
        }        
    }

    // Reentrant (per JVM process) lock against document xml file. 
    private final DocumentLock lock = new DocumentLock();
    
    private class DocumentLock implements Lock {
        private FileOutputStream fileStream;
        private FileChannel fileChannel;
        private FileLock fileLock;
        private AtomicInteger locksByThisProcess = new AtomicInteger(0);

        @Override
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {

            if ( locksByThisProcess.incrementAndGet() == 1) {
                // Physically lock the file only if we are the first one to enter
                long nanosTimeout = TimeUnit.NANOSECONDS.convert(time, unit);
                long increment = nanosTimeout/20;
                long lastTime = System.nanoTime();
                for (; ;) {
                    if (tryLock()) {
                        return true;
                    }
                    if (nanosTimeout < 0) {
                        return false;
                    }
                    LockSupport.parkNanos(increment);
                    long now = System.nanoTime();
                    nanosTimeout -= now - lastTime;
                    lastTime = now;
                    if (Thread.interrupted())
                        break;
                }
                throw new InterruptedException();
          }

            // return true if we locked already
            return locksByThisProcess.get() > 0;
        }

        @Override
        public void unlock() {
            // TODO: synchronize block?
            if (locksByThisProcess.get() > 0 && locksByThisProcess.decrementAndGet() == 0) {
                // Physically unlock the file when the last one gets out.
                if (fileLock != null && fileLock.isValid()) {
                    synchronized(this) {
                        if (fileLock != null) {
                            // save then always unlock
                            try {
                                TenantDocument.this.save(fileStream);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } finally {
                                try {
                                    fileLock.release();
                                    fileChannel.close();
                                    fileStream.close();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } finally {
                                    fileLock = null;
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean tryLock() {
            if (fileLock == null) {
                synchronized(this) {
                    if (fileLock == null) {
                        try {
                            // acquire exclusive lock on config file. 
                            File f = new File(TenantDocument.this.resource.toURI());
                            fileStream = new FileOutputStream(f); // save for unlock
                            fileChannel = fileStream.getChannel();
                            fileLock = fileChannel.lock();
                            return true;
                        } catch (URISyntaxException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (OverlappingFileLockException e){
                            // ok, locked by other, return false
                        }
                        return false;
                    } else {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public boolean isLocked() {
            return fileLock != null && fileLock.isValid();
        }

    }

    private URL resource;
    private long lastModified;

    private final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(TenantDocument.class);    

    @Inject
    private Logger logger;

    private final XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
}
