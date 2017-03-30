/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ha.store.adapter.file;

import org.glassfish.ha.store.api.*;

import java.io.*;

import java.util.Map;
import java.util.logging.*;

/**
 * An implementation of BackingStore that uses file system to
 * persist any Serializable data
 *
 * @author Mahesh Kannan
 */
public class FileBackingStore<K extends Serializable, V extends Serializable>
        extends BackingStore<K, V> {

    protected Logger logger =
            Logger.getLogger(FileBackingStore.class.getName());

    protected File baseDir;

    private boolean shutdown;

    private static Level TRACE_LEVEL = Level.FINE;

    private String debugStr;

    private FileBackingStoreFactory factory;

    private long defaultMaxIdleTimeoutInSeconds = 10L * 60L;

    /**
     * No arg constructor
     */
    public FileBackingStore() {
    }

    @Override
    protected void initialize(BackingStoreConfiguration<K, V> conf)
        throws BackingStoreException {

        if (conf.getLogger() != null) {
            logger = conf.getLogger();
        }
        
        super.initialize(conf);
        debugStr = "[FileBackingStore - " + conf.getStoreName() + "] ";
        
        baseDir = conf.getBaseDirectory();

        try {
            if ((baseDir.mkdirs() == false) && (! baseDir.isDirectory())) {
                throw new BackingStoreException("[FileBackingStore::initialize] Create base directory (" + baseDir.getAbsolutePath() + ") failed");
            }

            logger.log(Level.INFO, "[FileBackingStore::initialize] Successfully Created and initialized store. "
                    + "Working dir: " + conf.getBaseDirectory() + "; Configuration: " + conf);
        } catch (Exception ex) {
            logger.log(Level.WARNING, debugStr + " Exception during initialization", ex);
        }

        try {
            Map<String, Object> vendorMap = conf.getVendorSpecificSettings();
            defaultMaxIdleTimeoutInSeconds = Long.parseLong(
                    (String) vendorMap.get("max.idle.timeout.in.seconds"));
        } catch (Exception ex) {
            //Ignore. Use default
        }
    }

    /*package*/ void setFileBackingStoreFactory(FileBackingStoreFactory factory) {
        this.factory = factory;
    }

    public BackingStoreFactory getBackingStoreFactory() {
        return factory;
    }

    @Override
    public V load(K key, String version) throws BackingStoreException {

        String fileName = key.toString();
        V value = null;


        if (logger.isLoggable(TRACE_LEVEL)) {
            logger.log(TRACE_LEVEL, debugStr + "Entered load(" + key + ", " + version + ")");
        }

        byte[] data = readFromfile(fileName);
        if (data != null) {
            try {
                ByteArrayInputStream bis2 = new ByteArrayInputStream(data);
                ObjectInputStream ois = super.createObjectInputStream(bis2);
                value = (V) ois.readObject();

                if (logger.isLoggable(TRACE_LEVEL)) {
                    logger.log(TRACE_LEVEL, debugStr + "Done load(" + key + ", " + version + ")");
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING,debugStr + "Failed to load(" + key + ", " + version + ")", ex);
            }
        }
        
        return value;
    }

    public void remove(K sessionKey) {
        remove(sessionKey.toString());
    }

    private void remove(String sessionKey) {
        try {
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + "Entered remove(" + sessionKey + ")");
            }
            boolean status = removeFile(new File(baseDir, sessionKey));
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + "Done remove( " + sessionKey + "); status => " + status);
            }
        } catch (Exception ex) {
            logger.log(TRACE_LEVEL, debugStr + "Failed to remove(" + sessionKey + ")");
        }
    }

    @Override
    public void destroy() {
        try {
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + "Entered destroy()");
            }
            String[] fileNames = baseDir.list();
            if (fileNames == null) {
                return;
            }
            for (int i = 0; i < fileNames.length; i++) {
                remove(fileNames[i]);
            }

            if (baseDir.delete() == false) {
                if (baseDir.exists()) {
                    logger.log(Level.WARNING, debugStr + " destroy() failed to remove dir: " + baseDir.getAbsolutePath());
                }
            }
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + "Done destroy()");
            }
        } catch (Throwable th) {
            logger.log(Level.WARNING, debugStr + " destroy() failed ", th);
        } finally {
            FileBackingStoreFactory.removemapping(getBackingStoreConfiguration().getStoreName());
        }
    }

    public int removeExpired() {
        return removeExpired(defaultMaxIdleTimeoutInSeconds * 1000L);
    }

    //TODO: deprecate after next shoal integration   
    public int removeExpired(long idleForMillis) {
        long threshold = System.currentTimeMillis() - idleForMillis;
        int expiredSessions = 0;
        if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + "Entered removeExpired()");
            }
        try {
            String[] fileNames = baseDir.list();
            if (fileNames == null) {
                return 0;
            }
            int size = fileNames.length;
            for (int i = 0; (i < size) && (!shutdown); i++) {
                File file = new File(baseDir, fileNames[i]);
                if (file.exists()) {
                    long lastAccessed = file.lastModified();
                    if (lastAccessed < threshold) {
                        if (!file.delete()) {
                            if (file.exists()) {
                                logger.log(Level.WARNING, debugStr
                                        + " Couldn't remove file: " + fileNames[i]);
                            }
                        } else {
                            expiredSessions++;
                        }
                    }
                }
            }
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + "Done removeExpired()");
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, debugStr + " Exception while getting "
                    + "expired files", ex);
        }

        return expiredSessions;
    }

    public void shutdown() {
        shutdown = true;
        //Nothing else to do here. DO NOT DELETE THE WORKING DIRECTORY
    }


    @Override
    public int size() throws BackingStoreException {
        String[] numFiles = baseDir.list();
        return numFiles == null ? 0 : numFiles.length;
    }

    @Override
    public String save(K sessionKey, V value, boolean isNew)
            throws BackingStoreException {

        String fileName = sessionKey.toString();

        if (logger.isLoggable(TRACE_LEVEL)) {
            logger.log(TRACE_LEVEL, debugStr + "Entered save(" + sessionKey + ")");
        }
        writetoFile(sessionKey, fileName, getSerializedState(sessionKey, value));
        if (logger.isLoggable(TRACE_LEVEL)) {
            logger.log(TRACE_LEVEL, debugStr + "Done save(" + sessionKey + ")");
        }
        return getBackingStoreConfiguration().getInstanceName();
    }

    //TODO: deprecate after next shoal integration
    public void updateTimeStamp(K k, String version, long timeStamp)
            throws BackingStoreException {
        updateTimestamp(k, timeStamp);
    }
    
    public void updateTimestamp(K sessionKey, long time)
            throws BackingStoreException {
        if (logger.isLoggable(TRACE_LEVEL)) {
            logger.log(TRACE_LEVEL, debugStr + "Entered updateTimestamp(" + sessionKey + ", " + time + ")");
        }
        touchFile(sessionKey, sessionKey.toString(), time);
        if (logger.isLoggable(TRACE_LEVEL)) {
            logger.log(TRACE_LEVEL, debugStr + "Done updateTimestamp(" + sessionKey + ", " + time + ")");
        }
    }

    private void touchFile(Object sessionKey, String fileName, long time)
            throws BackingStoreException {
        try {
            File file = new File(baseDir, fileName);

            if (file.setLastModified(time) == false) {
                if (file.exists() == false) {
                    logger.log(Level.WARNING, debugStr
                            + ": Cannot update timsestamp for: " + sessionKey
                            + "; File does not exist");
                } else {
                    throw new BackingStoreException(
                            debugStr + ": Cannot update timsestamp for: " + sessionKey);
                }
            }
        } catch (BackingStoreException sfsbSMEx) {
            throw sfsbSMEx;
        } catch (Exception ex) {
            logger.log(Level.WARNING, debugStr
                    + ": Exception while updating timestamp", ex);
            throw new BackingStoreException(
                    "Cannot update timsestamp for: " + sessionKey
                            + "; Got exception: " + ex);
        }
    }

    private boolean removeFile(final File file) {
        boolean success = false;
        if (System.getSecurityManager() == null) {
            success = file.delete();
        } else {
            success = (Boolean) java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            return Boolean.valueOf(file.delete());
                        }
                    }
            );
        }

        return success;
    }

    private byte[] getSerializedState(K key, V value)
            throws BackingStoreException {

        byte[] data = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(value);
            oos.flush();
            data = bos.toByteArray();
        } catch (IOException ioEx) {
            throw new BackingStoreException("Error during getSerializedState", ioEx);
        } finally {
            try {
		if (oos != null) {
                    oos.close();
                }
            } catch (IOException ioEx) {/* Noop */}
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException ioEx) {/* Noop */}
        }

        return data;
    }

    private byte[] readFromfile(String fileName) {
        byte[] data = null;
        if (logger.isLoggable(TRACE_LEVEL)) {
            logger.log(TRACE_LEVEL, debugStr + " Attempting to load session: "
                    + fileName);
        }

        File file = new File(baseDir, fileName);
        if (file.exists()) {
            int dataSize = (int) file.length();
            data = new byte[dataSize];
            BufferedInputStream bis = null;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                int offset = 0;
                for (int toRead = dataSize; toRead > 0;) {
                    int count = bis.read(data, offset, toRead);
                    offset += count;
                    toRead -= count;
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING,
                        "FileStore.readFromfile failed", ex);
            } finally {
                try {
                    bis.close();
                } catch (Exception ex) {
                    logger.log(Level.FINE, debugStr + " Error while "
                            + "closing buffered input stream", ex);
                }
                try {
                    fis.close();
                } catch (Exception ex) {
                    logger.log(Level.FINE, debugStr + " Error while "
                            + "closing file input stream", ex);
                }
            }
        } else {
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(Level.WARNING, debugStr + "Could not find "
                        + "file for: " + fileName);
            }
        }

        return data;
    }

    private void writetoFile(K sessionKey, String fileName, byte[] data)
            throws BackingStoreException {
        File file = new File(baseDir, fileName);
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {

            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(data, 0, data.length);
            bos.flush();
            if (logger.isLoggable(TRACE_LEVEL)) {
                logger.log(TRACE_LEVEL, debugStr + " Successfully saved "
                        + "session: " + sessionKey);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "writetoFile(" + sessionKey + ") failed", ex);
            try {
                removeFile(file);
            } catch (Exception ex1) {
            }
            String errMsg = "Could not save session: " + sessionKey;
            throw new BackingStoreException(errMsg, ex);
        } finally {
            try {
                if (bos != null) bos.close();
            } catch (Exception ex) {
                logger.log(Level.FINE, "Error while closing buffered output stream", ex);
            }
            try {
                if (fos != null) fos.close();
            } catch (Exception ex) {
                logger.log(Level.FINE, "Error while closing file output stream", ex);
            }
        }
    }
}
