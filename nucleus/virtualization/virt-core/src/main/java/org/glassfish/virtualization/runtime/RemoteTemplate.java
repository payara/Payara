/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.virtualization.runtime;

import org.glassfish.virtualization.spi.FileOperations;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.MachineOperations;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.util.RuntimeContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Defines a remote template, installed on a remote machine.
 *
 * @author Jerome Dochez
 */
public class RemoteTemplate extends VMTemplate {
    private final Machine machine;
    private final ReentrantLock cacheLock = new ReentrantLock();
    private final AtomicBoolean inUpdate = new AtomicBoolean(false);

    /**
     * Creates a new template instance for a remotely installed template
     *
     * @param location the machine where the template is installed.
     * @param config   the template characteristics
     */
    public RemoteTemplate(Machine location, TemplateInstance config) {
        super(config);
        this.machine = location;
    }

    /**
     * returns the machine on which this remote template is installed.
     *
     * @return the remote machine
     */
    Machine getMachine() {
        return machine;
    }

    @Override
    public void copyTo(final Machine destination, final String destDir) throws IOException {

        destination.execute(new MachineOperations<Void>() {
            @Override
            public Void run(FileOperations fileOperations) throws IOException {
                // try our cached copy first.
                try {
                    String electedCache = nextCached(fileOperations);
                    if (electedCache != null) {
                        String cachedFileName = electedCache + File.separator + fileName();
                        String targetFileName = destDir + File.separator + fileName();
                        fileOperations.mv(cachedFileName, targetFileName);
                        fileOperations.delete(electedCache);
                        if (fileOperations.exists(targetFileName))
                            return null;
                    }
                } catch (IOException e) {
                    RuntimeContext.logger.log(Level.WARNING, "Exception while getting a cached copy ", e);
                }

                // if an exception occurs or there was no cached copy available, use synchronous copy.
                fileOperations.localCopy(remotePath(), destDir);
                return null;
            }
        });
    }

    private String remotePath() {
        String fileName = null;
        try {
            fileName = fileName();
        } catch (FileNotFoundException e) {
            return null;
        }
        return machine.getConfig().getTemplatesLocation() + "/" + getDefinition().getName() + "/" + fileName;
    }

    @Override
    public long getSize() throws IOException {
        return machine.execute(new MachineOperations<Long>() {
            @Override
            public Long run(FileOperations fileOperations) throws IOException {
                return fileOperations.length(remotePath());
            }
        });
    }

    void cleanCache(final int cacheSize) throws IOException {
        final String cacheLocation = machine.getConfig().getTemplateCacheLocation() + File.separator + getDefinition().getName();
        machine.execute(new MachineOperations<Object>() {
            @Override
            public Object run(FileOperations fileOperations) throws IOException {
                try {
                    for (String fileName : fileOperations.ls(cacheLocation)) {
                        try {
                            Long fileNameAsNumber = Long.parseLong(fileName);
                            if (fileNameAsNumber > cacheSize) {
                                // we do need this file any more, it's either an attempted copy that never finished
                                // or it's a leftover cached template from a cache reduction.
                                fileOperations.delete(cacheLocation + File.separator + fileName);
                            }
                        } catch (NumberFormatException e) {
                            RuntimeContext.logger.log(Level.WARNING, "Weird file name format in cache " + fileName);
                        }

                    }
                } catch (IOException e) {
                    RuntimeContext.logger.log(Level.SEVERE, "Exception while cleaning cache on " + machine, e);
                }
                return null;
            }
        });
    }

    void refreshCache(final int cacheSize) {
        boolean alreadyInUpdate = inUpdate.compareAndSet(false, true);
        try {
            if (!alreadyInUpdate) {
                RuntimeContext.logger.info("Cache on machine " + machine + " already in update");
                return;
            }
            RuntimeContext.logger.fine("Cache on machine " + machine + " starting to update for " + templateInstance.getConfig().getName());
            final String cacheLocation = machine.getConfig().getTemplateCacheLocation() + File.separator + getDefinition().getName();
            try {
                machine.execute(new MachineOperations<Void>() {
                    @Override
                    public Void run(FileOperations fileOps) throws IOException {
                        // first we need to ensure that the cache is clean of any temporary file
                        int inProgress=0;
                        try {
                            for (String fileName : fileOps.ls(cacheLocation)) {
                                try {
                                    Long fileNameAsNumber = Long.parseLong(fileName);
                                    if (fileNameAsNumber > cacheSize) {
                                        // This is a copy in process most likely
                                        inProgress++;
                                    }
                                } catch (NumberFormatException e) {
                                    RuntimeContext.logger.log(Level.WARNING, "Weird file name format in cache " + fileName);
                                }

                            }
                        } catch (IOException e) {
                            RuntimeContext.logger.log(Level.SEVERE, "Exception while cleaning cache on " + machine, e);
                        }


                        // now we ensure the cache is populated correctly.
                        for (int i = 0; i < cacheSize; i++) {
                            String templateCacheLocation = cacheLocation + File.separator + i;
                            try {
                                if (fileOps.exists(templateCacheLocation)) {
                                    // we need to ensure that it is up to date.
                                    String templateFileName = templateCacheLocation + File.separator + fileName();
                                    if (fileOps.exists(templateFileName) && !isUpToDate(fileOps, remotePath(), templateFileName)) {
                                        // it's old, get rid of it.
                                        try {
                                            cacheLock.lock();
                                            fileOps.delete(templateCacheLocation);
                                        } finally {
                                            cacheLock.unlock();
                                        }
                                    }
                                }
                                // at this point, either it was not there, or was too old and got whacked
                                // and therefore we need to copy, but if it is there, the copy is up to date
                                // so we can return
                                if (fileOps.exists(templateCacheLocation)) continue;

                                // We decrease our inProgress number to check whether or not we must initiate a new cached copy
                                if (inProgress>1) {
                                    inProgress--;
                                    continue;
                                }

                                // copy the file to a temporary location to avoid keeping the lock too long
                                String tmpCacheLocation = cacheLocation + File.separator + String.valueOf(System.currentTimeMillis());
                                fileOps.mkdir(tmpCacheLocation);
                                fileOps.localCopy(remotePath(), tmpCacheLocation);

                                // switch the temporary location to the final location.
                                String finalCacheLocation = cacheLocation + File.separator + i;
                                try {
                                    cacheLock.lock();
                                    // check that some other thread did not use this slot.
                                    if (!fileOps.exists(finalCacheLocation)) {
                                        fileOps.mv(tmpCacheLocation, finalCacheLocation);
                                    }
                                } finally {
                                    cacheLock.unlock();
                                }
                            } catch (IOException e) {
                                RuntimeContext.logger.log(Level.SEVERE,
                                        "Cannot cache " + i + " th template " + templateInstance.getConfig().getName()
                                                + " on " + machine, e);
                            }
                        }
                        return null;
                    }
                });
            } catch (IOException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Exception while setting up cache", e);
            }
        } finally {
            inUpdate.set(false);
        }
        RuntimeContext.logger.fine("Cache on machine " + machine + " done with update for " + templateInstance.getConfig().getName());
    }

    private boolean isUpToDate(FileOperations fileOperations, String reference, String copy) throws IOException {

        Date referenceModTime = fileOperations.mod(reference);
        Date copyModTime = fileOperations.mod(copy);
        return copyModTime.after(referenceModTime);
    }

    private String nextCached(FileOperations fileOperations) throws IOException {

        int i = 0;
        int cacheSize = new Integer(templateInstance.getConfig().getVirtualization().getTemplateCacheSize());
        String cacheLocation = machine.getConfig().getTemplateCacheLocation() + File.separator + getDefinition().getName();
        while (i < cacheSize) {
            String aCacheLocation = cacheLocation + File.separator + i;
            if (fileOperations.exists(aCacheLocation)) {
                String electedCache = cacheLocation + File.separator + String.valueOf(System.currentTimeMillis());
                try {
                    cacheLock.lock();
                    if (fileOperations.exists(aCacheLocation)) {
                        fileOperations.mv(aCacheLocation, electedCache);
                    } else {
                        electedCache = null;
                    }
                } finally {
                    cacheLock.unlock();
                }
                if (electedCache != null) return electedCache;
            }
            i++;
        }
        return null;
    }

    protected String fileName() throws FileNotFoundException {
        File file=null;
        try {
            file = templateInstance.getFileByExtension(".img");
        } catch (FileNotFoundException e) {
            try {
                file = templateInstance.getFileByExtension(".vdi");
            } catch (FileNotFoundException e1) {
                return null;
            }
        }
        return file.getName();
    }
}