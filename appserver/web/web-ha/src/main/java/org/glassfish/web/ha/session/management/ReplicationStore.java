/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/*
 * ReplicationStore.java
 *
 * Created on November 17, 2005, 10:24 AM
 */

package org.glassfish.web.ha.session.management;


import com.sun.appserv.util.cache.BaseCache;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.web.ServerConfigLookup;
import org.apache.catalina.Container;
import org.apache.catalina.Session;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Loader;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.Storeable;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.*;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author Larry White
 * @author Rajiv Mordani
 */
public class ReplicationStore extends HAStoreBase {

    @LogMessageInfo(
            message = "Exception during removing synchronized from backing store",
            level = "WARNING")
    public static final String EXCEPTION_REMOVING_SYNCHRONIZED = "AS-WEB-HA-00001";

    @LogMessageInfo(
            message = "Exception during removing expired session from backing store",
            level = "WARNING")
    public static final String EXCEPTION_REMOVING_EXPIRED_SESSION = "AS-WEB-HA-00002";

    @LogMessageInfo(
            message = "Error creating inputstream",
            level = "WARNING")
    public static final String ERROR_CREATING_INPUT_STREAM = "AS-WEB-HA-00003";

    @LogMessageInfo(
            message = "Exception during deserializing the session",
            level = "WARNING")
    public static final String EXCEPTION_DESERIALIZING_SESSION = "AS-WEB-HA-00004";

    @LogMessageInfo(
            message = "Exception occurred in getSession",
            level = "WARNING")
    public static final String EXCEPTION_GET_SESSION = "AS-WEB-HA-00005";

    /**
     * Creates a new instance of ReplicationStore
     */
    public ReplicationStore(JavaEEIOUtils ioUtils) {
        super(ioUtils);
        //setLogLevel();
    }


    public String getApplicationId() {
        if(applicationId != null) {
            return applicationId;
        }
        applicationId = "WEB:" + super.getApplicationId();
        return applicationId;
    }

    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */
    public void valveSave(Session session) throws IOException {

        if (!(session instanceof HASession)) {
            return;
        }
        HASession haSess = (HASession)session;
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>valveSave id=" + haSess.getIdInternal() +
            " isPersistent=" + haSess.isPersistent() + " isDirty=" + haSess.isDirty());
        }
        if( haSess.isPersistent() && !haSess.isDirty() ) {
            this.updateLastAccessTime(session);
        } else {
            this.doValveSave(session);
            haSess.setPersistent(true);
        }
        haSess.setDirty(false);

        this.doValveSave(session);
    }

    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */
    public void doValveSave(Session session) throws IOException {
        if(_logger.isLoggable(Level.FINE)) {
            if (session instanceof HASession) {
                _logger.fine("ReplicationStore>>doValveSave:id =" + ((HASession)session).getIdInternal());
            }
            _logger.fine("ReplicationStore>>doValveSave:valid =" + session.getIsValid());
        }
        // begin 6470831 do not save if session is not valid
        if( !session.getIsValid() ) {
            return;
        }
        if (!(session instanceof BaseHASession)) {
            return;
        }
        // end 6470831
        String userName = "";
        if(session.getPrincipal() !=null){
            userName = session.getPrincipal().getName();
            ((BaseHASession)session).setUserName(userName);
        }
        byte[] sessionState = this.getByteArray(session, isReplicationCompressionEnabled());

        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("ReplicationStore->Byte array to save");
            StringBuilder sb = new StringBuilder("Session data{");
            for (byte b: sessionState) {
                sb.append(b + "_");
            }
            sb.append("}");
            _logger.finest(sb.toString());
        }
        BackingStore<String, SimpleMetadata> replicator = getSimpleMetadataBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doValveSave replicator: " + replicator);
            _logger.fine("ReplicationStore>>doValveSave version:" + session.getVersion());                       
        }        
        SimpleMetadata simpleMetadata =
            SimpleMetadataFactory.createSimpleMetadata(session.getVersion(),  //version
                session.getLastAccessedTime(), //lastaccesstime
                session.getMaxInactiveInterval()*1000L, //maxinactiveinterval
                sessionState); //state
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("In doValveSave metadata is " + simpleMetadata);
        }
        try {
            HASession haSess = (HASession)session;
            replicator.save(session.getIdInternal(), //id
                    simpleMetadata, haSess.isPersistent());

            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Save succeeded.");
            }

        } catch (BackingStoreException ex) {
            IOException ex1 =
                (IOException) new IOException("Error during save: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }

    public void cleanup() {
        //FIXME;
    }

    public BaseCache getSessions(){
        //FIXME
        return null;
    }

    public void setSessions(BaseCache sesstable) {
        //FIXME;
    }


    public void stop() {
        try {
            super.stop();
            BackingStore<String, ? extends Storeable> backingStore = getStoreableBackingStore();
            backingStore.destroy();
        } catch (BackingStoreException e) {

        } catch (LifecycleException le) {
         
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
        if (!(session instanceof HASession)) {
            return;
        }
        HASession haSess = (HASession)session;
        if( haSess.isPersistent() && !haSess.isDirty() ) {
            this.updateLastAccessTime(session);
        } else {
            this.doSave(session);
            haSess.setPersistent(true);
        }
        haSess.setDirty(false);
//        this.doSave(session);
    }

    protected boolean isReplicationCompressionEnabled() {
        //XXX Need to fix this.
        return false;
    }

    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */
    public void doSave(Session session) throws IOException {
        if (!(session instanceof HASession)) {
            return;
        }
        byte[] sessionState = this.getByteArray(session, isReplicationCompressionEnabled());
        BackingStore<String, SimpleMetadata> backingStore = getSimpleMetadataBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>save: backing store : " + backingStore);
        }
        SimpleMetadata simpleMetadata =
            SimpleMetadataFactory.createSimpleMetadata(session.getVersion(),  //version
                session.getLastAccessedTime(), //lastaccesstime
                session.getMaxInactiveInterval()*1000L, //maxinactiveinterval
                sessionState); //state

        try {
            backingStore.save(session.getIdInternal(), //id
                    simpleMetadata, !((HASession)session).isPersistent());  //TODO: Revist the last param
        } catch (BackingStoreException ex) {
            IOException ex1 =
                (IOException) new IOException("Error during save: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }

    /**
    * Clear sessions
    *
    * @exception IOException if an input/output error occurs
    */
    public synchronized void clear() throws IOException {
        //FIXME

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
    public void doRemove(String id) throws IOException  {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doRemove");
        }
        BackingStore<String, ? extends Storeable> replicator = getStoreableBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>doRemove: replicator: " + replicator);                       
        }        
        try {
            replicator.remove(id);
        } catch (BackingStoreException ex) {
            _logger.log(Level.WARNING, EXCEPTION_REMOVING_SYNCHRONIZED, ex);
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
    public synchronized void removeSynchronized(String id) throws IOException  {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>removeSynchronized");                       
        }
        BackingStore<String, ? extends Storeable> replicator = getStoreableBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>removeSynchronized: replicator: " + replicator);                       
        }        
        try {
            replicator.remove(id);
        } catch (BackingStoreException ex) {
            _logger.log(Level.WARNING, EXCEPTION_REMOVING_SYNCHRONIZED, ex);
        }
    }
    

    /**
     * Called by our background reaper thread to remove expired
     * sessions in the replica - this can be done on the same
     * instance - i.e. each instance will do its own
     *
     */
    public void processExpires() {        
        removeExpiredSessions();
    }
    
    /** This method deletes all the sessions corresponding to the "appId"
     * that should be expired
     * @return number of removed sessions
     */    
    public int removeExpiredSessions() {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("IN ReplicationStore>>removeExpiredSessions");
        }
        int result = 0;
        ReplicationManagerBase<? extends Storeable> mgr = getStoreableReplicationManager();
        if(mgr == null) {
            return result;
        }
        BackingStore<String, ? extends Storeable> backingStore = mgr.getBackingStore();
        if (backingStore != null) {
            try {
                result = backingStore.removeExpired(mgr.getMaxInactiveInterval());
            } catch (BackingStoreException ex) {
                _logger.log(Level.WARNING, EXCEPTION_REMOVING_EXPIRED_SESSION, ex);
            }
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>removeExpiredSessions():number of expired sessions = " + result);
        }
        return result;

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
    public Session load(String id) throws ClassNotFoundException, IOException {
        return load(id, null);
    }    
    
    public Session load(String id, String version) throws ClassNotFoundException, IOException {
        try {
                return loadFromBackingStore(id, version);
        } catch (BackingStoreException ex) {
            IOException ex1 =
                    (IOException) new IOException("Error during load: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }


    private Session loadFromBackingStore(String id, String version)
            throws IOException, ClassNotFoundException, BackingStoreException {
        SimpleMetadata metaData = getSimpleMetadataBackingStore().load(id, version);
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("ReplicationStore>>loadFromBackingStore:id=" +
                    id + ", metaData=" + metaData);
        }

        Session session = getSession(metaData);
        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("ReplicationStore->Session is " + session);
        }

        return session;
    }


    private BackingStore<String, ? extends Storeable> getStoreableBackingStore() {
        return getStoreableReplicationManager().getBackingStore();
    }

    @SuppressWarnings("unchecked")
    private BackingStore<String, SimpleMetadata> getSimpleMetadataBackingStore() {
        ReplicationManagerBase<SimpleMetadata> mgr
                = (ReplicationManagerBase<SimpleMetadata>) this.getManager();
        return mgr.getBackingStore();
    }

    @SuppressWarnings("unchecked")
    private ReplicationManagerBase<? extends Storeable> getStoreableReplicationManager() {
        return (ReplicationManagerBase<? extends Storeable>)this.getManager();
    }


    /**
     * update the lastaccess time of the specified Session into this Store.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */    
    public void updateLastAccessTime(Session session) throws IOException {

        if (!(session instanceof BaseHASession)) {
            return;
        }
        BackingStore<String, ? extends Storeable> backingStore = getStoreableBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>updateLastAccessTime: replicator: " + backingStore);                       
        }         
        try {
            backingStore.updateTimestamp(session.getIdInternal(), ""+session.getVersion(),
                    ((BaseHASession)session).getLastAccessedTimeInternal());
        } catch (BackingStoreException ex) {
            //FIXME
        }
    }        
    
    /**
    * Return an array containing the session identifiers of all Sessions
    * currently saved in this Store.  If there are no such Sessions, a
    * zero-length array is returned.
    *
    * @exception IOException if an input/output error occurred
    */  
    public String[] keys() throws IOException  {
        //FIXME
        return new String[0];
    }
    
    public int getSize() throws IOException {
        int result = 0;
        BackingStore<String, ? extends Storeable> replicator = getStoreableBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>getSize: replicator: " + replicator);                       
        }         
        try {
            result = replicator.size();
        } catch (BackingStoreException ex) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Exception is getSize",ex);    
            }

        }
        return result;
    }    
    
    // Store methods end

    private Session getSession(SimpleMetadata metaData) throws IOException {
        ClassLoader classLoader;
        if (metaData == null || metaData.getState() == null) {
            return null;
        } else {
            return getSession(metaData.getState(), metaData.getVersion());
        }
    }


    public Session getSession(byte[] state,  long version) throws IOException {
        Session _session = null;
        InputStream is;
        BufferedInputStream bis;
        ByteArrayInputStream bais;    
        ObjectInputStream ois = null;
        Loader loader = null;
        ClassLoader classLoader = null;
        Container container = manager.getContainer();
        java.security.Principal pal=null; //MERGE chg added
        try
        {
            bais = new ByteArrayInputStream(state);
            bis = new BufferedInputStream(bais);
            if(isReplicationCompressionEnabled()) {
                is = new GZIPInputStream(bis);
            } else {
                is = bis;
            }            
            

            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("loaded session from replicationstore, length = "+state.length);
            }

            loader = container.getLoader();
            if (loader !=null) {
                classLoader = loader.getClassLoader(); 
            }

            if (classLoader != null) {
                try {
                    ois = ioUtils.createObjectInputStream(is,true,classLoader);
                } catch (Exception ex) {
                    _logger.log(Level.WARNING, ERROR_CREATING_INPUT_STREAM, ex);
                }
            }
            

            if (ois == null) {
                ois = new ObjectInputStream(is);
            }
            try {
                _session = readSession(manager, ois);
            } 
            finally {
                if (ois != null) {
                    try {
                        ois.close();
                        bis = null;
                    }
                    catch (IOException e) {
                    }
                }
            }
        } catch(ClassNotFoundException e) {
            IOException ex1 = (IOException) new IOException(
                    _logger.getResourceBundle().getString(EXCEPTION_DESERIALIZING_SESSION) + e.getMessage()).initCause(e);
            _logger.log(Level.WARNING, EXCEPTION_DESERIALIZING_SESSION, ex1);
            throw ex1;
        }
        catch(IOException e) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, EXCEPTION_GET_SESSION, e);
            }
            throw e;
        }
        String username = ((HASession)_session).getUserName();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationStore>>getSession: username=" + username + " principal=" + _session.getPrincipal());                                  
        }        
        if((username !=null) && (!username.equals("")) && _session.getPrincipal() == null) {
            if (_debug > 0) {
                debug("Username retrieved is "+username);
            }
            pal = ((com.sun.web.security.RealmAdapter)container.getRealm()).createFailOveredPrincipal(username);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("ReplicationStore>>getSession:created pal=" + pal);                                  
            }             
            if (_debug > 0) {
                debug("principal created using username  "+pal);
            }
            if(pal != null) {
                _session.setPrincipal(pal);
                if (_debug > 0) {
                    debug("getSession principal="+pal+" was added to session="+_session); 
                }                
            }
        }
        //--SRI        
        
        _session.setNew(false);

        ((HASession)_session).setVersion(version);
        ((HASession)_session).setDirty(false);
        ((HASession)_session).setPersistent(false);        
        return _session;
    }    
}
