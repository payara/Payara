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

//----------------------------------------------------------------------------
//
// Description: Recovery lock file handling
// Author:      Marina Vatkina
// Date:        Sep 2010
//
//----------------------------------------------------------------------------

package com.sun.enterprise.transaction.jts.recovery;

import java.util.*;
import java.io.*;
import java.nio.channels.FileLock;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;

import com.sun.enterprise.transaction.jts.api.TransactionRecoveryFence;
import com.sun.enterprise.transaction.jts.api.DelegatedTransactionRecoveryFence;

import com.sun.jts.CosTransactions.Configuration;
import com.sun.jts.CosTransactions.LogControl;
import com.sun.jts.CosTransactions.RecoveryManager;

/**
 * This class manages lock file required for delegated recovery.
 * @author mvatkina
 *
 * @see
 * Records in the recovery lock file have the following format:
 * PREFIX INSTANCE_NAME TIMESTAMP
 * Where PREFIX can be one of:
 * - "O" means OWNED by this instance, i.e. non-delegated recovery 
 * - "B" means recovered BY the specified instance 
 * - "F" means recovered FOR the specified instance
 * TIMESTAMP is the time of the recovery operation
 * 
*/

public class RecoveryLockFile implements TransactionRecoveryFence, DelegatedTransactionRecoveryFence {

    // Logger to log transaction messages = use class from com.sun.jts sub-package to find the bundle
    static Logger _logger = LogDomains.getLogger(Configuration.class, LogDomains.TRANSACTION_LOGGER);

    private final static String SEPARATOR = " ";
    private final static String OWN = "O";
    private final static String FOR = "F";
    private final static String BY = "B";
    private final static String END_LINE = "\n";
    
    // Single instance
    private static final RecoveryLockFile instance = new RecoveryLockFile();

    private volatile boolean started = false;
    private String instance_name;
    private String log_path;
    private GMSCallBack gmsCallBack;

    private RecoveryLockFile() {
    }

    public static DelegatedTransactionRecoveryFence getDelegatedTransactionRecoveryFence(GMSCallBack gmsCallBack) {
        instance.init(gmsCallBack);

        return instance;
    }

    public void start() {
        if (!started) {
            gmsCallBack.finishDelegatedRecovery(log_path);
            started = true;
        }
    }

    private void init(GMSCallBack gmsCallBack) {
        this.gmsCallBack = gmsCallBack;
        instance_name = Configuration.getPropertyValue(Configuration.INSTANCE_NAME);
        log_path = LogControl.getLogPath();
        // Create (if it doesn't exist) recoveryLockFile to hold info about instance and delegated recovery
        File recoveryLockFile = LogControl.recoveryLockFile(null, log_path);
        try {
            recoveryLockFile.createNewFile();
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "jts.exception_creating_recovery_file", recoveryLockFile);
            _logger.log(Level.WARNING, "", ex);
        }
        RecoveryManager.registerTransactionRecoveryFence(this);
    }

    /**
     * {@inheritDoc}
     */
    public void raiseFence() {
        while (isRecovering()) {
            //wait
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
            }
        }
        registerRecovery();
    }

    /**
     * {@inheritDoc}
     */
    public void lowerFence() {
        _logger.log(Level.INFO, "Lower Fence request for instance " + instance_name);
        doneRecovering();
        _logger.log(Level.INFO, "Fence lowered for instance " + instance_name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFenceRaised(String logDir, String instance, long timestamp) {
        return isRecovering(logDir, instance, timestamp, BY);
    }

    /**
     * {@inheritDoc}
     */
    public void raiseFence(String logPath, String instance) {
        raiseFence(logPath, instance, 0L);
    }

    /**
     * {@inheritDoc}
     */
    public void raiseFence(String logPath, String instance, long timestamp) {
        _logger.log(Level.INFO, "Raise Fence request for instance " + instance);
        while (isRecovering(logPath, instance, timestamp, BY)) {
            //wait
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
            }
        }
        registerRecovery(logPath, instance);
        _logger.log(Level.INFO, "Fence raised for instance " + instance);
    }

    /**
     * {@inheritDoc}
     */
    public void lowerFence(String logPath, String instance) {
        _logger.log(Level.INFO, "Lower Fence request for instance " + instance);
        doneRecovering(logPath, instance);
        _logger.log(Level.INFO, "Fence lowered for instance " + instance);
    }

    /**
     * {@inheritDoc}
     */
    public String getInstanceRecoveredFor(String path, long timestamp) {
        if (!isRecovering(path, null, timestamp, FOR)) {
            return doneRecovering(path, null, FOR);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void transferRecoveryTo(String logDir, String instance) {
        doneRecovering(logDir, null, BY);
        registerRecovery(logDir, instance);
    }

    /**
     * Returns true if running instance is doing its own recovery
     */
    private boolean isRecovering() {
        return isRecovering(log_path, instance_name, 0L, BY);
    }

    /**
     * Returns true if recovery file on the specified path contains information 
     * that the specified instance started recovery after specified timestamp
     * either for itself or by another instance.
     */
    private boolean isRecovering(String logDir, String instance, long timestamp, String prefix) {
        BufferedReader reader = null;
        File recoveryLockFile = LogControl.recoveryLockFile(".", logDir);
        if (!recoveryLockFile.exists()) {
            _logger.log(Level.INFO, "Lock File not found " + recoveryLockFile);
            return false;
        }

        boolean result = false;
        try {
            _logger.log(Level.INFO, "Checking Lock File " + recoveryLockFile);
            RandomAccessFile raf = new RandomAccessFile(recoveryLockFile, "rw");
            FileLock lock = raf.getChannel().lock();
            try {
                reader = new BufferedReader(new FileReader(recoveryLockFile));
                String line = null;
                while( (line = reader.readLine()) != null) {
                    _logger.log(Level.INFO, "Testing line: " + line);
                    String[] parts = line.split(SEPARATOR);
                    if (parts.length != 3) {
                        throw new IllegalStateException();
                    } else if ((parts[0].equals(OWN) && parts[1].equals(instance)) ||
                             (instance == null && parts[0].equals(prefix))) {
                        result = (Long.parseLong(parts[2]) > timestamp);
                        break;
                    } else {
                        // skip all other lines
                        continue;
                    }
                } 
            } finally {
                lock.release();
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
                }
            }
        }

        _logger.log(Level.INFO, "Recovering? " + result);
        return result;
    }

    /**
     * Removes recovery data from the recovery lock file for running instance
     */
    private void doneRecovering() {
        doneRecovering(log_path, instance_name, OWN);
    }

    /**
     * Removes recovery data from the recovery lock files for both, the instance that the
     * recovery is done for (i.e. for specified instance), and the current instance which the 
     * recovery is done by (in the lock file on the specified path)
     */
    private void doneRecovering(String logPath, String instance) {
        doneRecovering(log_path, instance, FOR);
        doneRecovering(logPath, instance_name, BY);
    }

    /**
     * Removes recovery data from the recovery lock file.
     * @return instance name if instance was unknown (null).
     */
    private String doneRecovering(String logPath, String instance, String prefix) {
        BufferedReader reader = null;
        FileWriter writer = null;
        String result = null;
        File recoveryLockFile = LogControl.recoveryLockFile(".", logPath);
        if (!recoveryLockFile.exists()) {
            _logger.log(Level.INFO, "Lock Fine not found: " + recoveryLockFile);
            return null;
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(recoveryLockFile, "rw");
            FileLock lock = raf.getChannel().lock();
            try {
                reader = new BufferedReader(new FileReader(recoveryLockFile));
                _logger.log(Level.INFO, "Updating File " + recoveryLockFile);
                String line = null;
                List<String> list_out = new ArrayList<String>();
                while( (line = reader.readLine()) != null) {
                    _logger.log(Level.INFO, "Processing line: " + line);
                    String[] parts = line.split(SEPARATOR);
                    if (parts.length != 3) {
                        // Remove such line
                        _logger.log(Level.INFO, "...skipping bad line ...");
                        continue;
                    } else if (parts[0].equals(prefix) && (instance == null || parts[1].equals(instance))) {
                        // Remove such line
                        _logger.log(Level.INFO, "...skipping found line ...");
                        result = parts[1];
                        continue;
                    }
    
                    list_out.add(line);
                } 

                reader.close();
                reader = null;

                writer = new FileWriter(recoveryLockFile);
                for (String out : list_out) {
                    _logger.log(Level.INFO, "Re-adding line: " + out);
                    writer.write(out);
                    writer.write(END_LINE);
                }
            } finally {
                lock.release();
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
                }
            }
        }

        return result;
    }

    /**
     * Writes into recovery lock file data about recovery for the running instance.
     */
    private void registerRecovery() {
        // Remove any stale data 
        doneRecovering(log_path, null, BY);

        // And mark that it's self-recovery
        registerRecovery(log_path, instance_name, OWN);
    }

    /**
     * Writes into recovery lock file data about recovery for the specified instance by
     * the current instance.
     */
    private void registerRecovery(String logPath, String instance) {
        // Remove stale data if there is any
        doneRecovering(log_path, null, FOR);

        registerRecovery(logPath, instance_name, BY);
        registerRecovery(log_path, instance, FOR);
    }
    
    /**
     * Writes data into recovery lock file on the specified path
     */
    private void registerRecovery(String logPath, String instance, String prefix) {
        FileWriter writer = null;
        File recoveryLockFile = LogControl.recoveryLockFile(".", logPath);
        if (!recoveryLockFile.exists()) {
            _logger.log(Level.INFO, "Lock File not found " + recoveryLockFile);
            return;
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(recoveryLockFile, "rw");
            FileLock lock = raf.getChannel().lock();
            try {
                writer = new FileWriter(recoveryLockFile, true);
                    _logger.log(Level.INFO, "Writing into file " + recoveryLockFile);
                StringBuffer b = (new StringBuffer()).append(prefix).append(SEPARATOR).append(instance).
                        append(SEPARATOR).append(System.currentTimeMillis()).append(END_LINE);
                _logger.log(Level.INFO, "Storing " + b);
                writer.write(b.toString());
            } finally {
                lock.release();
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    _logger.log(Level.WARNING, "jts.exception_in_recovery_file_handling", ex);
                }
            }
        }
    }

}
