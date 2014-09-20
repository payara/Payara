/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.session;

import org.apache.catalina.*;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.LifecycleSupport;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.ServletContext;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * Standard implementation of the <b>Manager</b> interface that provides
 * simple session persistence across restarts of this component (such as
 * when the entire server is shut down and restarted, or when a particular
 * web application is reloaded.
 * <p>
 * <b>IMPLEMENTATION NOTE</b>:  Correct behavior of session storing and
 * reloading depends upon external calls to the <code>start()</code> and
 * <code>stop()</code> methods of this class at the correct times.
 *
 * @author Craig R. McClanahan
 * @author Jean-Francois Arcand
 * @version $Revision: 1.14.6.2 $ $Date: 2008/04/17 18:37:20 $
 */

public class StandardManager
    extends ManagerBase
    implements Lifecycle, PropertyChangeListener {

    @LogMessageInfo(
            message = "createSession: Too many active sessions",
            level = "WARNING"
    )
    public static final String TOO_MANY_ACTIVE_SESSION_EXCEPTION = "AS-WEB-CORE-00381";

    @LogMessageInfo(
            message = " Loading persisted sessions from {0}",
            level = "FINE"
    )
    public static final String LOADING_PERSISTED_SESSION = "AS-WEB-CORE-00382";

    @LogMessageInfo(
            message = "IOException while loading persisted sessions: {0}",
            level = "SEVERE",
            cause = "Could not creates an ObjectInputStream",
            action = "Verify if there are IO exceptions"
    )
    public static final String LOADING_PERSISTED_SESSION_IO_EXCEPTION = "AS-WEB-CORE-00383";

    @LogMessageInfo(
            message = "ClassNotFoundException while loading persisted sessions: {0}",
            level = "SEVERE",
            cause = "Could not deserialize and create StandardSession instance ",
            action = "Verify the class for an object being restored can be found"
    )
    public static final String CLASS_NOT_FOUND_EXCEPTION = "AS-WEB-CORE-00384";

    @LogMessageInfo(
            message = "Saving persisted sessions to {0}",
            level = "FINE"
    )
    public static final String SAVING_PERSISTED_SESSION = "AS-WEB-CORE-00385";

    @LogMessageInfo(
            message = "IOException while saving persisted sessions: {0}",
            level = "SEVERE",
            cause = "Could not creates an ObjectOutputStream instance",
            action = "Verify if there are any I/O exceptions"
    )
    public static final String SAVING_PERSISTED_SESSION_IO_EXCEPTION = "AS-WEB-CORE-00386";

    @LogMessageInfo(
            message = "Exception loading sessions from persistent storage",
            level = "SEVERE",
            cause = "Could not load any currently active sessions",
            action = "Verify if the serialized class is valid and if there are any I/O exceptions"
    )
    public static final String LOADING_SESSIONS_EXCEPTION = "AS-WEB-CORE-00387";

    @LogMessageInfo(
            message = "Exception unloading sessions to persistent storage",
            level = "SEVERE",
            cause = "Could not save any currently active sessions",
            action = "Verify if there are any I/O exceptions"
    )
    public static final String UNLOADING_SESSIONS_EXCEPTION = "AS-WEB-CORE-00388";

    // ---------------------------------------------------- Security Classes
    private class PrivilegedDoLoadFromFile
        implements PrivilegedExceptionAction<Void> {

        PrivilegedDoLoadFromFile() {
            // NOOP
        }

        public Void run() throws Exception{
           doLoadFromFile();
           return null;
        }                       
    }
        
    private class PrivilegedDoUnload
        implements PrivilegedExceptionAction<Void> {

        private boolean expire;
        private boolean isShutdown;

        PrivilegedDoUnload(boolean expire, boolean shutDown) {
            this.expire = expire;
            isShutdown = shutDown;
        }

        public Void run() throws Exception{
            doUnload(expire, isShutdown);
            return null;
        }            
           
    }        

    
    // ----------------------------------------------------- Instance Variables


    /**
     * The descriptive information about this implementation.
     */
    private static final String info = "StandardManager/1.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The maximum number of active Sessions allowed, or -1 for no limit.
     */
    private int maxActiveSessions = -1;


    /**
     * The descriptive name of this Manager implementation (for logging).
     */
    protected static final String name = "StandardManager";


    /**
     * Path name of the disk file in which active sessions are saved
     * when we stop, and from which these sessions are loaded when we start.
     * A <code>null</code> value indicates that no persistence is desired.
     * If this pathname is relative, it will be resolved against the
     * temporary working directory provided by our context, available via
     * the <code>javax.servlet.context.tempdir</code> context attribute.
     */
    private String pathname = "SESSIONS.ser";


    /**
     * Has this component been started yet?
     */
    private boolean started = false;

    // START SJSAS 6359401
    /*
     * The absolute path name of the file where sessions are persisted.
     */
    private String absPathName;
    // END SJSAS 6359401

    long processingTime=0;


    // ------------------------------------------------------------- Properties


    /**
     * Set the Container with which this Manager has been associated.  If
     * it is a Context (the usual case), listen for changes to the session
     * timeout property.
     *
     * @param container The associated Container
     */
    @Override
    public void setContainer(Container container) {

        // De-register from the old Container (if any)
        if ((this.container != null) && (this.container instanceof Context))
            ((Context) this.container).removePropertyChangeListener(this);

        // Default processing provided by our superclass
        super.setContainer(container);

        // Register with the new Container (if any)
        if ((this.container != null) && (this.container instanceof Context)) {
            setMaxInactiveIntervalSeconds
                ( ((Context) this.container).getSessionTimeout()*60 );
            ((Context) this.container).addPropertyChangeListener(this);
        }

    }


    /**
     * Return descriptive information about this Manager implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    @Override
    public String getInfo() {
        return info;
    }


    /**
     * Return the maximum number of active Sessions allowed, or -1 for
     * no limit.
     */
    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }


    public long getProcessingTime() {
        return processingTime;
    }


    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }


    /**
     * Set the maximum number of active Sessions allowed, or -1 for
     * no limit.
     *
     * @param max The new maximum number of sessions
     */
    public void setMaxActiveSessions(int max) {
        int oldMaxActiveSessions = this.maxActiveSessions;
        this.maxActiveSessions = max;
        support.firePropertyChange("maxActiveSessions",
                                   Integer.valueOf(oldMaxActiveSessions),
                                   Integer.valueOf(this.maxActiveSessions));
    }


    /**
     * Return the descriptive short name of this Manager implementation.
     */
    @Override
    public String getName() {
        return name;
    }


    /**
     * Return the session persistence pathname, if any.
     */
    public String getPathname() {
        return pathname;
    }


    /**
     * Set the session persistence pathname to the specified value.  If no
     * persistence support is desired, set the pathname to <code>null</code>.
     *
     * @param pathname New session persistence pathname
     */
    public void setPathname(String pathname) {
        String oldPathname = this.pathname;
        this.pathname = pathname;
        support.firePropertyChange("pathname", oldPathname, this.pathname);
    }


    // --------------------------------------------------------- Public Methods

    /**
     * Construct and return a new session object, based on the default
     * settings specified by this Manager's properties.  The session
     * id will be assigned by this method, and available via the getId()
     * method of the returned session.  If a new session cannot be created
     * for any reason, return <code>null</code>.
     *
     * @exception IllegalStateException if a new session cannot be
     *  instantiated for any reason
     */
    @Override
    public Session createSession() {
        if ((maxActiveSessions >= 0) &&
                (sessions.size() >= maxActiveSessions)) {
            rejectedSessions++;
            ((StandardContext)container).sessionRejectedEvent(
                maxActiveSessions);
            throw new IllegalStateException
                (rb.getString(TOO_MANY_ACTIVE_SESSION_EXCEPTION));
        }

        return (super.createSession());
    }

    // START S1AS8PE 4817642
    /**
     * Construct and return a new session object, based on the default
     * settings specified by this Manager's properties, using the specified
     * session id.
     *
     * IMPLEMENTATION NOTE: This method must be kept in sync with the
     * createSession method that takes no arguments.
     *
     * @param sessionId the session id to assign to the new session
     *
     * @exception IllegalStateException if a new session cannot be
     *  instantiated for any reason
     *
     * @return the new session, or <code>null</code> if a session with the
     * requested id already exists
     */
    @Override
    public Session createSession(String sessionId) {
        if ((maxActiveSessions >= 0) &&
                (sessions.size() >= maxActiveSessions)) {
            rejectedSessions++;
            throw new IllegalStateException
                (rb.getString(TOO_MANY_ACTIVE_SESSION_EXCEPTION));
        }

        return (super.createSession(sessionId));
    }
    // END S1AS8PE 4817642

    /*
     * Releases any resources held by this session manager.
     */
    @Override
    public void release() {
        super.release();
        clearStore();
    }


    // START SJSAS 6359401
    /*
     * Deletes the persistent session storage file.
     */
    public void clearStore() {
        File file = file();
        if (file != null && file.exists()) {
            deleteFile(file);
        }
    }
    // END SJSAS 6359401


    /**
     * Loads any currently active sessions that were previously unloaded
     * to the appropriate persistence mechanism, if any.  If persistence is not
     * supported, this method returns without doing anything.
     *
     * @exception ClassNotFoundException if a serialized class cannot be
     * found during the reload
     * @exception IOException if a read error occurs
     */
    public void load() throws ClassNotFoundException, IOException {
        if (SecurityUtil.isPackageProtectionEnabled()){   
            try{
                AccessController.doPrivileged(new PrivilegedDoLoadFromFile());
            } catch (PrivilegedActionException ex){
                Exception exception = ex.getException();
                if (exception instanceof ClassNotFoundException){
                    throw (ClassNotFoundException)exception;
                } else if (exception instanceof IOException) {
                    throw (IOException)exception;
                }
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Unreported exception in load() "
                            + exception);
                }
            }
        } else {
            doLoadFromFile();
        }       
    }


    /**
     * Loads any currently active sessions that were previously unloaded
     * to file
     *
     * @exception ClassNotFoundException if a serialized class cannot be
     * found during the reload
     * @exception IOException if a read error occurs
     */
    private void doLoadFromFile() throws ClassNotFoundException, IOException {
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Start: Loading persisted sessions");
        }

        // Open an input stream to the specified pathname, if any
        File file = file();
        if (file == null || !file.exists() || file.length() == 0) {
            return;
        }
        if (log.isLoggable(Level.FINE)) {
            String msg = MessageFormat.format(rb.getString(LOADING_PERSISTED_SESSION), pathname);
            log.log(Level.FINE, msg);
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file.getAbsolutePath());
            readSessions(fis);
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Finish: Loading persisted sessions");
            }
        } catch (FileNotFoundException e) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "No persisted data file found");
            }
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException f) {
                // ignore
            }
            // Delete the persistent storage file
            deleteFile(file);
        }
    }

    private void deleteFile(File file) {
        if (!file.delete() && log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Cannot delete file: " + file);
        }
    }

    /*
     * Reads any sessions from the given input stream, and initializes the
     * cache of active sessions with them.
     *
     * @param is the input stream from which to read the sessions
     *
     * @exception ClassNotFoundException if a serialized class cannot be
     * found during the reload
     * @exception IOException if a read error occurs
     */
    public void readSessions(InputStream is)
            throws ClassNotFoundException, IOException {

        // Initialize our internal data structures
        sessions.clear();

        ObjectInputStream ois = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            if (container != null) {
                ois = ((StandardContext)container).createObjectInputStream(bis);
            } else {
                ois = new ObjectInputStream(bis);
            }
        } catch (IOException ioe) {
            String msg = MessageFormat.format(rb.getString(LOADING_PERSISTED_SESSION_IO_EXCEPTION),
                                              ioe);

            log.log(Level.SEVERE, msg, ioe);
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException f) {
                     // Ignore
                }
                ois = null;
            }
            throw ioe;
        }

        synchronized (sessions) {
            try {
                Integer count = (Integer) ois.readObject();
                int n = count.intValue();
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Loading " + n + " persisted sessions");
                for (int i = 0; i < n; i++) {
                    StandardSession session =
                        StandardSession.deserialize(ois, this);
                    session.setManager(this);
                    sessions.put(session.getIdInternal(), session);
                    session.activate();
                }
            } catch (ClassNotFoundException e) {
                String msg = MessageFormat.format(rb.getString(CLASS_NOT_FOUND_EXCEPTION),
                                                  e);
                log.log(Level.SEVERE, msg, e);
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException f) {
                        // Ignore
                    }
                    ois = null;
                }
                throw e;
            } catch (IOException e) {
                String msg = MessageFormat.format(rb.getString(LOADING_PERSISTED_SESSION_IO_EXCEPTION),
                                                  e);
              log.log(Level.SEVERE, msg, e);
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException f) {
                        // Ignore
                    }
                    ois = null;
                }
                throw e;
            } finally {
                // Close the input stream
                try {
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException f) {
                    // ignore
                }
            }
        }
    }


    /**
     * Save any currently active sessions in the appropriate persistence
     * mechanism, if any.  If persistence is not supported, this method
     * returns without doing anything.
     *
     * @exception IOException if an input/output error occurs
     */
    public void unload() throws IOException {
        unload(true, false);
    }


    /**
     * Writes all active sessions to the given output stream.
     *
     * @param os the output stream to which to write
     *
     * @exception IOException if an input/output error occurs
     */
    public void writeSessions(OutputStream os) throws IOException {
        writeSessions(os, true);
    }


    /**
     * Save any currently active sessions in the appropriate persistence
     * mechanism, if any.  If persistence is not supported, this method
     * returns without doing anything.
     *
     * @doExpire true if the unloaded sessions are to be expired, false
     * otherwise
     * @param isShutdown true if this manager is being stopped as part of a
     * domain shutdown (as opposed to an undeployment), and false otherwise
     *
     * @exception IOException if an input/output error occurs
     */        
    protected void unload(boolean doExpire, boolean isShutdown) throws IOException {
        if (SecurityUtil.isPackageProtectionEnabled()){       
            try {
                AccessController.doPrivileged(
                    new PrivilegedDoUnload(doExpire, isShutdown));
            } catch (PrivilegedActionException ex){
                Exception exception = ex.getException();
                if (exception instanceof IOException){
                    throw (IOException)exception;
                }
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Unreported exception in unLoad() " + exception);
            }
        } else {
            doUnload(doExpire, isShutdown);
        }       
    }
        

    /**
     * Saves any currently active sessions to file.
     *
     * @doExpire true if the unloaded sessions are to be expired, false
     * otherwise
     *
     * @exception IOException if an input/output error occurs
     */
    private void doUnload(boolean doExpire, boolean isShutdown) throws IOException {
        if(isShutdown) {
            if(log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Unloading persisted sessions");
            }
            // Open an output stream to the specified pathname, if any
            File file = file();
            if(file == null || !isDirectoryValidFor(file.getAbsolutePath())) {
                return;
            }
            if(log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, SAVING_PERSISTED_SESSION, pathname);
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file.getAbsolutePath());
                writeSessions(fos, doExpire);
                if(log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Unloading complete");
                }
            } catch(IOException ioe) {
                if(fos != null) {
                    try {
                        fos.close();
                    } catch(IOException f) {
                        ;
                    }
                    fos = null;
                }
                throw ioe;
            } finally {
                try {
                    if(fos != null) {
                        fos.close();
                    }
                } catch(IOException f) {
                    // ignore
                }
            }
        }
    }

    /*
     * Writes all active sessions to the given output stream.
     *
     * @param os the output stream to which to write the sessions
     * @param doExpire true if the sessions that were written should also be
     * expired, false otherwise
     */
    public void writeSessions(OutputStream os, boolean doExpire) 
            throws IOException {
        ObjectOutputStream oos = null;
        try {
            if (container != null) {
                oos = ((StandardContext) container).createObjectOutputStream(
                        new BufferedOutputStream(os));
            } else {
                oos = new ObjectOutputStream(new BufferedOutputStream(os));
            }
        } catch (IOException e) {
            String msg = MessageFormat.format(rb.getString(SAVING_PERSISTED_SESSION_IO_EXCEPTION),
                                              e);
            log.log(Level.SEVERE, msg, e);
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    // Ignore
                }
                oos = null;
            }
            throw e;
        }

        // Write the number of active sessions, followed by the details
        StandardSession[] currentStandardSessions = null;
        synchronized (sessions) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Unloading " + sessions.size() + " sessions");
            try {
                // START SJSAS 6375689
                for (Session actSession : findSessions()) {
                    StandardSession session = (StandardSession) actSession;
                    session.passivate();
                }
                // END SJSAS 6375689
                Session[] currentSessions = findSessions();
                int size = currentSessions.length;
                currentStandardSessions = new StandardSession[size];
                oos.writeObject(Integer.valueOf(size));
                for (int i = 0; i < size; i++) {
                    StandardSession session =
                        (StandardSession) currentSessions[i];
                    currentStandardSessions[i] = session;
                    /* SJSAS 6375689
                    session.passivate();
                    */
                    oos.writeObject(session);
                }
            } catch (IOException e) {
                String msg = MessageFormat.format(rb.getString(SAVING_PERSISTED_SESSION_IO_EXCEPTION),
                        e);
                log.log(Level.SEVERE, msg, e);
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException f) {
                        // Ignore
                    }
                    oos = null;
                }
                throw e;
            }
        }

        // Flush and close the output stream
        try {
            oos.flush();
        } catch (IOException e) {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    // Ignore
                }
                oos = null;
            }
            throw e;
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException f) {
                // ignore
            }
        }

        if (doExpire) {
            // Expire all the sessions we just wrote
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Expiring " + currentStandardSessions.length + " persisted sessions");
            for (StandardSession session : currentStandardSessions) {
                try {
                    session.expire(false);
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }
    }


    /**
     * Check if the directory for this full qualified file
     * exists and is valid
     * Hercules: added method
     */    
    private boolean isDirectoryValidFor(String fullPathFileName) {
        int lastSlashIdx = fullPathFileName.lastIndexOf(File.separator);
        if(lastSlashIdx == -1) {
            return false;
        }
        String result = fullPathFileName.substring(0, lastSlashIdx);
        //System.out.println("PATH name = " + result);
        return new File(result).isDirectory();
    }    


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }


    /**
     * Gets the (possibly empty) list of lifecycle listeners
     * associated with this StandardManager.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        if( ! initialized )
            init();
        
        // Validate and update our current component state
        if (started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, PersistentManagerBase.MANAGER_STARTED_INFO);
            }
            return;
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Force initialization of the random number generator
        if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, "Force random number initialization starting");
        generateSessionId();
        if (log.isLoggable(Level.FINEST))
            log.log(Level.FINEST, "Force random number initialization completed");

        // Load unloaded sessions, if any
        try {
            load();
        } catch (Throwable t) {
            log.log(Level.SEVERE,
                    LOADING_SESSIONS_EXCEPTION, t);
        }

    }

    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {
        stop(false);
    }

    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @param isShutdown true if this manager is being stopped as part of a
     * domain shutdown (as opposed to an undeployment), and false otherwise
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop(boolean isShutdown) throws LifecycleException {

        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Stopping");

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (rb.getString(PersistentManagerBase.MANAGER_NOT_STARTED_INFO));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Write out sessions
        try {
            unload(false, isShutdown);
        } catch (IOException e) {
            log.log(Level.SEVERE, UNLOADING_SESSIONS_EXCEPTION, e);
        }

        // Expire all active sessions and notify their listeners
        Session sessions[] = findSessions();
        if (sessions != null) {
            for (Session session : sessions) {
                if (!session.isValid()) {
                    continue;
                }
                try {
                    session.expire();
                } catch (Throwable t) {
                    // Ignore
                } finally {
                    // Measure against memory leaking if references to the session
                    // object are kept in a shared field somewhere
                    session.recycle();
                }
            }
        }

        // Require a new random number generator if we are restarted
        resetRandom();

        if( initialized ) {
            destroy();
        }
    }


    // ----------------------------------------- PropertyChangeListener Methods


    /**
     * Process property change events from our associated Context.
     *
     * @param event The property change event that has occurred
     */
    public void propertyChange(PropertyChangeEvent event) {

        // Validate the source of this event
        if (!(event.getSource() instanceof Context))
            return;

        // Process a relevant property change
        if ("sessionTimeout".equals(event.getPropertyName())) {
            try {
                setMaxInactiveIntervalSeconds
                    ((Integer) event.getNewValue() *60 );
            } catch (NumberFormatException e) {
                log.log(Level.SEVERE,PersistentManagerBase.INVALID_SESSION_TIMEOUT_SETTING_EXCEPTION,
                        event.getNewValue().toString());
            }
        }

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Return a File object representing the pathname to our
     * persistence file, if any.
     */
    private File file() {

        // START SJSAS 6359401
        if (absPathName != null) {
            return new File(absPathName);
        }
        // END SJSAS 6359401

        if ((pathname == null) || (pathname.length() == 0))
            return (null);
        File file = new File(pathname);
        if (!file.isAbsolute()) {
            if (container instanceof Context) {
                ServletContext servletContext =
                    ((Context) container).getServletContext();
                File tempdir = (File)
                    servletContext.getAttribute(ServletContext.TEMPDIR);
                if (tempdir != null)
                    file = new File(tempdir, pathname);
            }
        }

        // START SJSAS 6359401
        if (file != null) {
            absPathName = file.getAbsolutePath();
        }
        // END SJSAS 6359401

        return (file);

    }


    /**
     * Invalidate all sessions that have expired.
     */
    public void processExpires() {

        long timeNow = System.currentTimeMillis();

        Session[] sessions = findSessions();
        if (sessions != null) {
            for (Session session : sessions) {
                StandardSession sess = (StandardSession) session;
                if (sess.lockBackground()) {
                    try {
                        sess.isValid();
                    } finally {
                        sess.unlockBackground();
                    }
                }
            }
        }

        long timeEnd = System.currentTimeMillis();
        processingTime += ( timeEnd - timeNow );
    }

}
