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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.session;


import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;

import javax.servlet.ServletContext;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete implementation of the <b>Store</b> interface that utilizes
 * a file per saved Session in a configured directory.  Sessions that are
 * saved are still subject to being expired based on inactivity.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2007/01/04 01:31:57 $
 */

public final class FileStore extends StoreBase {

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();

    // ----------------------------------------------------- Constants


    /**
     * The extension to use for serialized session filenames.
     */
    private static final String FILE_EXT = ".session";

    // ----------------------------------------------------- Instance Variables


    /**
     * The pathname of the directory in which Sessions are stored.
     * This may be an absolute pathname, or a relative path that is
     * resolved against the temporary work directory for this application.
     */
    private String directory = ".";


    /**
     * A File representing the directory in which Sessions are stored.
     */
    private File directoryFile = null;

    /**
     * The descriptive information about this implementation.
     */
    private static final String info = "FileStore/1.0";

    /**
     * Name to register for this Store, used for logging.
     */
    private static final String storeName = "fileStore";

    /**
     * Name to register for the background thread.
     */
    private static final String threadName = "FileStore";
    
    /**
    * Our write-through cache of session objects
    * HERCULES: addition
    */
    private Hashtable<String, Session> sessions = new Hashtable<String, Session>();     


    // ------------------------------------------------------------- Properties


    /**
     * Return the directory path for this Store.
     */
    public String getDirectory() {

        return (directory);

    }


    /**
     * Set the directory path for this Store.
     *
     * @param path The new directory path
     */
    public void setDirectory(String path) {

        String oldDirectory = this.directory;
        this.directory = path;
        this.directoryFile = null;
        support.firePropertyChange("directory", oldDirectory,
                                   this.directory);

    }
    

    /**
     * Return descriptive information about this Store implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (info);

    }

    /**
     * Return the thread name for this Store.
     */
    public String getThreadName() {
        return(threadName);
    }

    /**
     * Return the name for this Store, used for logging.
     */
    public String getStoreName() {
        return(storeName);
    }


    /**
     * Return the number of Sessions present in this Store.
     *
     * @exception IOException if an input/output error occurs
     */
    public int getSize() throws IOException {

        // Acquire the list of files in our storage directory
        File file = directory();
        if (file == null) {
            return (0);
        }
        String files[] = file.list();

        // Figure out which files are sessions
        int keycount = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].endsWith(FILE_EXT)) {
                keycount++;
            }
        }
        return (keycount);

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Remove all of the Sessions in this Store.
     *
     * @exception IOException if an input/output error occurs
     */
    public void clear()
        throws IOException {

        String[] keys = keys();
        for (int i = 0; i < keys.length; i++) {
            remove(keys[i]);
        }

    }


    /**
     * Return an array containing the session identifiers of all Sessions
     * currently saved in this Store.  If there are no such Sessions, a
     * zero-length array is returned.
     *
     * @exception IOException if an input/output error occurred
     */
    public String[] keys() throws IOException {

        // Acquire the list of files in our storage directory
        File file = directory();
        if (file == null) {
            return (new String[0]);
        }
        String files[] = file.list();

        // Build and return the list of session identifiers
        ArrayList<String> list = new ArrayList<String>();
        int n = FILE_EXT.length();
        for (int i = 0; i < files.length; i++) {
            if (files[i].endsWith(FILE_EXT)) {
                list.add(files[i].substring(0, files[i].length() - n));
            }
        }
        return list.toArray(new String[list.size()]);

    }


    /**
     * Load and return the Session associated with the specified session
     * identifier from this Store, without removing it.  If there is no
     * such stored Session, return <code>null</code>.
     *
     * @param id Session identifier of the session to load
     *
     * @exception ClassNotFoundException if a deserialization error occurs
     * @exception IOException if an input/output error occurs
     */
    public Session load(String id)
        throws ClassNotFoundException, IOException {
            
        //HERCULES:addition
        // Check to see if it's in our cache first
        Session sess = sessions.get(id);
        if ( sess != null ) {
            return sess;
        }
        //HERCULES:addition            

        // Open an input stream to the specified pathname, if any
        File file = file(id);
        if (file == null) {
            return (null);
        }

        if (! file.exists()) {
            return (null);
        }
        if (debug >= 1) {
            String msg = MessageFormat.format(rb.getString(LogFacade.LOADING_SESSION_FROM_FILE),
                                              new Object[] {id, file.getAbsolutePath()});
            log(msg);
        }

        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ObjectInputStream ois = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        try {
            fis = new FileInputStream(file.getAbsolutePath());
            bis = new BufferedInputStream(fis);
            Container container = manager.getContainer();
            if (container != null) {
                ois = ((StandardContext)container).createObjectInputStream(bis);
            } else {
                ois = new ObjectInputStream(bis);
            }
            //end HERCULES:mod
        } catch (FileNotFoundException e) {
            if (debug >= 1)
                log("No persisted data file found");
            return (null);
        } catch (IOException e) {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException f) {
                    // Ignore
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException f) {
                    // Ignore
                }
            }
            throw e;
        }

        try {
            StandardSession session =
                StandardSession.deserialize(ois, manager);
            session.setManager(manager);
            //HERCULES: addition
            // Put it in the cache
            sessions.put(session.getIdInternal(), session);     
            //HERCULES: addition            
            return (session);
        } finally {
            // Close the input stream
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException f) {
                    // Ignore
                }
            }
        }
    }


    /**
     * Remove the Session with the specified session identifier from
     * this Store, if present.  If no such Session is present, this method
     * takes no action.
     *
     * @param id Session identifier of the Session to be removed
     *
     * @exception IOException if an input/output error occurs
     */
    public void remove(String id) throws IOException {

        File file = file(id);
        if (file == null) {
            return;
        }
        if (debug >= 1) {
            String msg = MessageFormat.format(rb.getString(LogFacade.REMOVING_SESSION_FROM_FILE),
                                              new Object[] {id, file.getAbsolutePath()});
            log(msg);
        }
        //HERCULES: addition
        // Take it out of the cache 
        sessions.remove(id);        
        //HERCULES: addition
        if (!file.delete() && log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Cannot delete file: " + file);
        }
    }


    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */
    public void save(Session session) throws IOException {

        // Open an output stream to the specified pathname, if any
        File file = file(session.getIdInternal());
        if (file == null) {
            return;
        }
        if (debug >= 1) {

            String msg = MessageFormat.format(rb.getString(LogFacade.SAVING_SESSION_TO_FILE),
                                              new Object[] {session.getIdInternal(), file.getAbsolutePath()});
            log(msg);
        }
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath());
            Container container = manager.getContainer();
            if (container != null) {
                oos = ((StandardContext) container).createObjectOutputStream(
                        new BufferedOutputStream(fos));
            } else {
                oos = new ObjectOutputStream(new BufferedOutputStream(fos)); 
            }
        } catch (IOException e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException f) {
                    // Ignore
                }
            }
            throw e;
        }

        try {
            oos.writeObject(session);
        } finally {
            oos.close();
        }

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Return a File object representing the pathname to our
     * session persistence directory, if any.  The directory will be
     * created if it does not already exist.
     */
    private File directory() throws IOException {

        if (this.directory == null) {
            return (null);
        }
        if (this.directoryFile != null) {
            // NOTE:  Race condition is harmless, so do not synchronize
            return (this.directoryFile);
        }
        File file = new File(this.directory);
        if (!file.isAbsolute()) {
            Container container = manager.getContainer();
            if (container instanceof Context) {
                ServletContext servletContext =
                    ((Context) container).getServletContext();
                File work = (File)
                    servletContext.getAttribute(ServletContext.TEMPDIR);
                file = new File(work, this.directory);
            } else {
                throw new IllegalArgumentException
                    ("Parent Container is not a Context");
            }
        }
        if (!file.exists() || !file.isDirectory()) {
            if (!file.delete() && file.exists()) {
                String msg = MessageFormat.format(rb.getString(LogFacade.UNABLE_DELETE_FILE_EXCEPTION),
                                                  file);
                throw new IOException(msg);
            }
            if (!file.mkdirs() && !file.isDirectory()) {
                String msg = MessageFormat.format(rb.getString(LogFacade.UNABLE_CREATE_DIR_EXCEPTION),
                                                  file);
                throw new IOException(msg);
            }
        }
        this.directoryFile = file;
        return (file);

    }


    /**
     * Return a File object representing the pathname to our
     * session persistence file, if any.
     *
     * @param id The ID of the Session to be retrieved. This is
     *    used in the file naming.
     */
    private File file(String id) throws IOException {

        if (this.directory == null) {
            return (null);
        }
        String filename = id + FILE_EXT;
        File file = new File(directory(), filename);
        return (file);

    }


}
