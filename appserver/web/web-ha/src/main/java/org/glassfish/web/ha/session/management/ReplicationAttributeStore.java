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

package org.glassfish.web.ha.session.management;

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.web.ServerConfigLookup;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.apache.catalina.*;
import org.apache.catalina.session.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Larry White
 * @author Rajiv Mordani
 */
public class ReplicationAttributeStore extends ReplicationStore {
    

    /** Creates a new instance of ReplicationAttributeStore */
    public ReplicationAttributeStore(JavaEEIOUtils ioUtils) {
        super(ioUtils);
        setLogLevel();
    }
    
    // HAStorePoolElement methods begin
    
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
        if( haSess.isPersistent() && !haSess.isDirty() ) {
            this.updateLastAccessTime(session);
        } else {
            this.doValveSave(session);
            haSess.setPersistent(true);
        }
        haSess.setDirty(false);        
    } 
    
    // Store method begin
    
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
    }
    
    
    /**
     * Save the specified Session into this Store.  Any previously saved
     * information for the associated session identifier is replaced.
     *
     * @param session Session to be saved
     *
     * @exception IOException if an input/output error occurs
     */
    @Override
    public void doValveSave(Session session) throws IOException {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationAttributeStore>>doValveSave:valid =" + ((StandardSession)session).getIsValid());
            if (session instanceof HASession) {
                _logger.fine("ReplicationAttributeStore>>valveSave:ssoId=" + ((HASession)session).getSsoId());
            }
        }         
        
        // begin 6470831 do not save if session is not valid
        if( !((StandardSession)session).getIsValid() ) {
            return;
        }
        // end 6470831

        if (!(session instanceof ModifiedAttributeHASession) ||
                !(session instanceof BaseHASession)) {
            return;
        }

        ModifiedAttributeHASession modAttrSession
                = (ModifiedAttributeHASession)session;
        String userName = "";
        if(session.getPrincipal() !=null){
            userName = session.getPrincipal().getName();
            ((BaseHASession)session).setUserName(userName);
        }
        BackingStore<String, CompositeMetadata> replicator = getCompositeMetadataBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationAttributeStore>>save: replicator: " + replicator);                    
        }         
        CompositeMetadata compositeMetadata
            = createCompositeMetadata(modAttrSession);
                
        try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("CompositeMetadata is " + compositeMetadata + " id is " + session.getIdInternal());
            }
            replicator.save(session.getIdInternal(), //id
                    compositeMetadata, !((HASession) session).isPersistent());
            modAttrSession.resetAttributeState();
            postSaveUpdate(modAttrSession);
        } catch (BackingStoreException ex) {
            //FIXME
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
    @Override
    public void doSave(Session session) throws IOException {
        // begin 6470831 do not save if session is not valid
        if( !((StandardSession)session).getIsValid() ) {
            return;
        }

        if (!(session instanceof ModifiedAttributeHASession) ||
                !(session instanceof HASession)) {
            return;
        }

        // end 6470831        
        ModifiedAttributeHASession modAttrSession
                = (ModifiedAttributeHASession)session;
        BackingStore<String, CompositeMetadata> replicator = getCompositeMetadataBackingStore();
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationAttributeStore>>doSave: replicator: " + replicator);                    
        }         
        CompositeMetadata compositeMetadata 
            = createCompositeMetadata(modAttrSession);
                
        try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("CompositeMetadata is " + compositeMetadata + " id is " + session.getIdInternal());
            }

            replicator.save(session.getIdInternal(), //id
                    compositeMetadata, !((HASession) session).isPersistent());
            modAttrSession.resetAttributeState();
            postSaveUpdate(modAttrSession);
        } catch (BackingStoreException ex) {
            //FIXME
        }
    }

    @SuppressWarnings("unchecked")
    private BackingStore<String, CompositeMetadata> getCompositeMetadataBackingStore() {
        ReplicationManagerBase<CompositeMetadata> mgr
                = (ReplicationManagerBase<CompositeMetadata>) this.getManager();
        return mgr.getBackingStore();
    }

    @Override
    public Session load(String id, String version)
            throws ClassNotFoundException, IOException {
        try {
            CompositeMetadata metaData =
                    getCompositeMetadataBackingStore().load(id, version);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("ReplicationAttributeStore>>load:id=" + id + ", metaData=" + metaData);
            }
            Session session = getSession(metaData);
            validateAndSave(session);
            return session;
        } catch (BackingStoreException ex) {
            IOException ex1 =
                    (IOException) new IOException("Error during load: " + ex.getMessage()).initCause(ex);
            throw ex1;
        }
    }

    private void validateAndSave(Session session) throws IOException {
        if (session != null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("ReplicationAttributeStore>>validateAndSave saving " +
                        "the session after loading it. Session=" + session);
            }
            //save session - save will reset dirty to false
            ((HASession) session).setDirty(true);
            valveSave(session); // TODO :: revisit this for third party backing stores;
        }
        if (session != null) {
            ((HASession) session).setDirty(false);
            ((HASession) session).setPersistent(false);
        }
    }


    public Session getSession(CompositeMetadata metadata)
        throws IOException 
    {
        if (metadata == null || metadata.getState() == null) {
            return null;
        }
        byte[] state = metadata.getState();
        Session _session = null;
        BufferedInputStream bis = null;
        ByteArrayInputStream bais = null;
        Loader loader = null;    
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        Container container = manager.getContainer();
        java.security.Principal pal=null; //MERGE chg added
        String ssoId = null;
        long version = 0L;
            
        try
        {
            bais = new ByteArrayInputStream(state);
            bis = new BufferedInputStream(bais);
            
            //Get the username, ssoId from metadata
            //ssoId = metadata.getSsoId();
            ssoId = metadata.getStringExtraParam();
            version = metadata.getVersion();
            //debug("ReplicationStore.getSession()  id="+id+"  username ="+username+";");    

            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("loaded session from replicationstore, length = "+state.length);
            }
            if (container != null) {
                loader = container.getLoader();
            }

            if (loader != null) {
                classLoader = loader.getClassLoader();
            }
            
            if (classLoader != null) {

                try {
                    ois = ioUtils.createObjectInputStream(bis, true, classLoader);
                } catch (Exception ex) {}

            }
            if (ois == null) {
                ois = new ObjectInputStream(bis); 
            }
            
            if(ois != null) {
                try {
                    _session = readSession(manager, ois);
                } 
                finally {

                    try {
                        ois.close();
                        bis = null;
                    }
                    catch (IOException e) {
                    }
                }
            }
        }
        catch(ClassNotFoundException e)
        {
            IOException ex1 = (IOException) new IOException(
                    "Error during deserialization: " + e.getMessage()).initCause(e);
            throw ex1;
        }
        catch(IOException e)
        {
            throw e;
        }
        String username = ((HASession)_session).getUserName(); 
        if((username !=null) && (!username.equals("")) && _session.getPrincipal() == null) {
            if (_debug > 0) {
                debug("Username retrieved is "+username);
            }
            pal = ((com.sun.web.security.RealmAdapter)container.getRealm()).createFailOveredPrincipal(username);
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
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationAttributeStore>>ssoId=" + ssoId);                       
        }        
/*
        ((BaseHASession)_session).setSsoId(ssoId);
        if((ssoId !=null) && (!ssoId.equals("")))
            associate(ssoId, _session);
*/
        ((HASession)_session).setVersion(version);
        ((HASession)_session).setDirty(false);
        
        //now load entries from deserialized entries collection
        ((ModifiedAttributeHASession)_session).clearAttributeStates();
        byte[] entriesState = metadata.getState();
        Collection entries = null;
        if(entriesState != null) {
            entries = this.deserializeStatesCollection(entriesState);
            loadAttributes((ModifiedAttributeHASession)_session, entries);
        }
        loadAttributes((ModifiedAttributeHASession)_session, metadata.getEntries());
        return _session;
    }
    

    //metadata related
    
    private void postSaveUpdate(ModifiedAttributeHASession modAttrSession) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationAttributeStore>>postSaveUpdate");                       
        }
        List<String> addedAttrs = modAttrSession.getAddedAttributes();
        List<String> modifiedAttrs = modAttrSession.getModifiedAttributes();
        List<String> deletedAttrs = modAttrSession.getDeletedAttributes();
        printAttrList("ADDED", addedAttrs);
        printAttrList("MODIFIED", modifiedAttrs);
        printAttrList("DELETED", deletedAttrs);

        postProcessSetAttrStates(modAttrSession, addedAttrs);
        postProcessSetAttrStates(modAttrSession, modifiedAttrs);
        
    }
    
    private void postProcessSetAttrStates(ModifiedAttributeHASession modAttrSession, List<String> attrsList) {
        for(int i=0; i<attrsList.size(); i++) {
            String nextStateName = attrsList.get(i);
            modAttrSession.setAttributeStatePersistent(nextStateName, true);
            modAttrSession.setAttributeStateDirty(nextStateName, false);
        }
    }
    
    private CompositeMetadata createCompositeMetadata(ModifiedAttributeHASession modAttrSession) {
        
        byte[] trunkState = null;
        if (modAttrSession.isNew()) {
            try {
                trunkState = this.getByteArray(modAttrSession);
            } catch(IOException ex) {
                //no op
            }
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationAttributeStore>>createCompositeMetadata:trunkState=" + trunkState);                       
        }         
       
        List<SessionAttributeMetadata> entries = new ArrayList<SessionAttributeMetadata>();
        List<String> addedAttrs = modAttrSession.getAddedAttributes();
        List<String> modifiedAttrs = modAttrSession.getModifiedAttributes();
        List<String> deletedAttrs = modAttrSession.getDeletedAttributes();
        printAttrList("ADDED", addedAttrs);
        printAttrList("MODIFIED", modifiedAttrs);
        printAttrList("DELETED", deletedAttrs);
        
        addToEntries(modAttrSession, entries, 
                SessionAttributeMetadata.Operation.ADD, addedAttrs);
        addToEntries(modAttrSession, entries, 
                SessionAttributeMetadata.Operation.UPDATE, modifiedAttrs);
        addToEntries(modAttrSession, entries,
                SessionAttributeMetadata.Operation.DELETE, deletedAttrs);

        CompositeMetadata result 
            = new CompositeMetadata(modAttrSession.getVersion(),
                modAttrSession.getLastAccessedTimeInternal(),
                modAttrSession.getMaxInactiveInterval()*1000L,
                entries, trunkState, null);
        return result;
    }
    
    private void printAttrList(String attrListType, List<String> attrList) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("AttributeType = " + attrListType);
            String nextAttrName = null;
            for(int i=0; i<attrList.size(); i++) {
                nextAttrName = attrList.get(i);
                _logger.fine("attribute[" + i + "]=" + nextAttrName);
            }
        }
    }
    
    private void addToEntries(ModifiedAttributeHASession modAttrSession,
            List<SessionAttributeMetadata> entries, SessionAttributeMetadata.Operation op,
            List<String> attrList) {
        String nextAttrName = null;
        Object nextAttrValue = null;
        byte[] nextValue = null;
        for(int i=0; i<attrList.size(); i++) {
            nextAttrName = attrList.get(i);
            nextAttrValue = ((StandardSession) modAttrSession).getAttribute(nextAttrName);
            nextValue = null;
            try {
                nextValue = getByteArray(nextAttrValue);
            } catch (IOException ex) {}
            SessionAttributeMetadata nextAttrMetadata
                = new SessionAttributeMetadata(nextAttrName, op, nextValue);
            entries.add(nextAttrMetadata);
        }
    }
    
    /**
    * Create an byte[] for the session that we can then pass to
    * the HA Store.
    *
    * @param attributeValue
    *   The attribute value we are serializing
    *
    */
    protected byte[] getByteArray(Object attributeValue)
      throws IOException {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;


        byte[] obs;
        try {
            bos = new ByteArrayOutputStream();
            

            try {
                oos = ioUtils.createObjectOutputStream(new BufferedOutputStream(bos), true);
            } catch (Exception ex) {}

            //use normal ObjectOutputStream if there is a failure during stream creation
            if(oos == null) {
                oos = new ObjectOutputStream(new BufferedOutputStream(bos)); 
            }            
            oos.writeObject(attributeValue);
            oos.close();
            oos = null;

            obs = bos.toByteArray();
        }
        finally {
            if ( oos != null )  {
                oos.close();
            }
        }

        return obs;
    }
    
    /**
    * Given a byte[] containing session data, return a session
    * object
    *
    * @param state
    *   The byte[] with the session attribute data
    *
    * @return
    *   A newly created object for the given session attribute data
    */
    protected Object getAttributeValue(byte[] state) 
        throws IOException, ClassNotFoundException 
    {
        Object attributeValue = null;
        BufferedInputStream bis = null;
        ByteArrayInputStream bais = null;
        Loader loader = null;    
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        Container container = manager.getContainer();
        
            
        try
        {
            bais = new ByteArrayInputStream(state);
            bis = new BufferedInputStream(bais);
            
            if (container != null) {
                loader = container.getLoader();
            }

            if (loader != null) {
                classLoader = loader.getClassLoader();
            }
            
            if (classLoader != null) {

                try {
                    ois = ioUtils.createObjectInputStream(bis, true, classLoader);
                } catch (Exception ex) {}

            }
            if (ois == null) {
                ois = new ObjectInputStream(bis); 
            }
            
            if(ois != null) {
                try {
                    attributeValue = ois.readObject();
                } 
                finally {

                    try {
                        ois.close();
                        bis = null;
                    }
                    catch (IOException e) {
                    }

                }
            }
        }
        catch(ClassNotFoundException e)
        {

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "ClassNotFoundException occurred in getAttributeValue", e);
            }
            throw e;
        }
        catch(IOException e)
        {
            throw e;
        }      

        return attributeValue;
    }
    
    //new serialization code for Collection
    /**
    * Create an byte[] for the session that we can then pass to
    * the HA Store.
    *
    * @param entries
    *   The Collection of entries we are serializing
    *
    */
    protected byte[] getByteArrayFromCollection(Collection entries)
      throws IOException {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;

        
        byte[] obs;
        try {
            bos = new ByteArrayOutputStream();
            

            try {
                oos = ioUtils.createObjectOutputStream(new BufferedOutputStream(bos), true);
            } catch (Exception ex) {}

            //use normal ObjectOutputStream if there is a failure during stream creation
            if(oos == null) {
                oos = new ObjectOutputStream(new BufferedOutputStream(bos)); 
            }
            //first write out the entriesSize
            int entriesSize = entries.size();
            oos.writeObject(Integer.valueOf(entriesSize));
            //then write out the entries
            Iterator it = entries.iterator();
            while(it.hasNext()) {
                oos.writeObject(it.next());
            }
            oos.close();
            oos = null;

            obs = bos.toByteArray();
        }
        finally {
            if ( oos != null )  {
                oos.close();
            }
        }

        return obs;
    }
    
    /**
    * Given a byte[] containing session data, return a session
    * object
    *
    * @param state
    *   The byte[] with the session attribute data
    *
    * @return
    *   A newly created object for the given session attribute data
    */
    protected Object getAttributeValueCollection(byte[] state) 
        throws IOException, ClassNotFoundException 
    {
        Collection<Object> attributeValueList = new ArrayList<Object>();
        BufferedInputStream bis = null;
        ByteArrayInputStream bais = null;
        Loader loader = null;    
        ClassLoader classLoader = null;
        ObjectInputStream ois = null;
        Container container = manager.getContainer();
            
        try
        {
            bais = new ByteArrayInputStream(state);
            bis = new BufferedInputStream(bais);
            
            if (container != null) {
                loader = container.getLoader();
            }

            if (loader != null) {
                classLoader = loader.getClassLoader();
            }
            
            if (classLoader != null) {

                try {
                    ois = ioUtils.createObjectInputStream(bis, true, classLoader);
                } catch (Exception ex) {}

            }
            if (ois == null) {
                ois = new ObjectInputStream(bis); 
            }
            if(ois != null) {
                try {
                    //first get List size
                    Object whatIsIt = ois.readObject();
                    int entriesSize = 0;
                    if(whatIsIt instanceof Integer) {
                        entriesSize = ((Integer)whatIsIt).intValue();
                    }
                    for (int i = 0; i < entriesSize; i++) {
                        Object nextAttributeValue = ois.readObject();
                        attributeValueList.add(nextAttributeValue);
                    }
                } 
                finally {
                    try {
                        ois.close();
                        bis = null;
                    }
                    catch (IOException e) {
                    }
                }
            }
        }
        catch(ClassNotFoundException e)
        {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "ClassNotFoundException occurred in getAttributeValueCollection", e);
            }
            throw e;
        }
        catch(IOException e)
        {
            throw e;
        }      

        return attributeValueList;
    }    
    
    //end new serialization code for Collection

    /**
    * Given a session, load its attributes
    *
    * @param modifiedAttributeSession
    *   The session (header info only) having its attributes loaded
    *
    * @param attributeList
    *   The List<AttributeMetadata> list of loaded attributes
    *
    * @return
    *   A newly created object for the given session attribute data
    */    
    protected void loadAttributes(ModifiedAttributeHASession modifiedAttributeSession, 
            Collection attributeList) {
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.finest("in loadAttributes -- ReplicationAttributeStore : session id=" + modifiedAttributeSession.getIdInternal());
        }

        String thisAttrName = null;
        //SessionAttributeMetadata.Operation thisAttrOp = null;
        Object thisAttrVal = null;
        Iterator it = attributeList.iterator();
        while (it.hasNext()) { 
            SessionAttributeMetadata nextAttrMetadata = (SessionAttributeMetadata)it.next();
            thisAttrName = nextAttrMetadata.getAttributeName();
            //thisAttrOp = nextAttrMetadata.getOperation();
            byte[] nextAttrState = nextAttrMetadata.getState();
            thisAttrVal = null;
            try { 
                thisAttrVal = getAttributeValue(nextAttrState);
            } catch (ClassNotFoundException ex1) {
                //FIXME log?
            } catch (IOException ex2) {}
            if(_logger.isLoggable(Level.FINEST)) {
                _logger.finest("Attr retrieved======" + thisAttrName);
            }

            if(thisAttrVal != null) { //start if
                if(_logger.isLoggable(Level.FINEST)) {
                    _logger.finest("Setting Attribute: " + thisAttrName);
                }
                modifiedAttributeSession.setAttribute(thisAttrName, thisAttrVal);
                modifiedAttributeSession.setAttributeStatePersistent(thisAttrName, false);
                modifiedAttributeSession.setAttributeStateDirty(thisAttrName, false);
            } //end if
        } //end while 
    } 
    


/*
    private byte[] serializeStatesCollection(Collection entries) {
        byte[] result = null;
        try {
            result = getByteArrayFromCollection(entries);
        } catch (IOException ex) {} 
        return result;
    }
    
    private byte[] serializeStatesCollectionPrevious(Collection entries) {
        byte[] result = null;
        try {
            result = getByteArray(entries);
        } catch (IOException ex) {} 
        return result;
    }    
*/


    private Collection deserializeStatesCollection(byte[] entriesState) {
        Collection result = new ArrayList();
        try {
            result = (Collection)getAttributeValueCollection(entriesState);
        } catch (ClassNotFoundException ex1) {
            // FIXME log?
        } catch (IOException ex2) {}        
        return result;
    } 
    
/*
    private Collection deserializeStatesCollectionPrevious(byte[] entriesState) {
        Collection result = new ArrayList();
        try {
            result = (Collection)getAttributeValue(entriesState);
        } catch (ClassNotFoundException ex1) {
              // FIXME log?
        } catch (IOException ex2) {}        
        return result;
    }     
*/

}
