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
 * ModifiedAttributeHASession.java
 *
 * Created on October 3, 2002, 3:45 PM
 */

package org.glassfish.web.ha.session.management;

import org.apache.catalina.Manager;
import org.apache.catalina.util.Enumerator;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author  lwhite
 * @author Rajiv Mordani
 */
public class ModifiedAttributeHASession extends BaseHASession {
    
    private static final Logger _logger = HAStoreBase._logger;

    private transient Map<String, SessionAttributeState> _attributeStates = new HashMap<String, SessionAttributeState>();
    private transient boolean _dirtyFlag = false;
    
    
    /** Creates a new instance of ModifiedAttributeHASession */
    public ModifiedAttributeHASession(Manager manager) {
        super(manager);
    }
    
    /**
     * return an ArrayList of Strings
     * whose elements are the names of the deleted attributes
     */      
    public List<String> getDeletedAttributes() {
        
        List<String> resultList = new ArrayList<String>();
        for (Map.Entry<String, SessionAttributeState> entry : _attributeStates.entrySet()) {
            SessionAttributeState nextAttrState = entry.getValue();
            String nextAttrName = entry.getKey();
            if(nextAttrState.isDeleted() && nextAttrState.isPersistent()) {
                resultList.add(nextAttrName);
            }
        }
        return resultList;
    }

    /**
     * return an ArrayList of Strings
     * whose elements are the names of the modified attributes
     * attributes must dirty, persistent and not deleted
     */      
    public List<String> getModifiedAttributes() {
        List<String> resultList = new ArrayList<String>();
        for (Map.Entry<String, SessionAttributeState> entry : _attributeStates.entrySet()) {
            SessionAttributeState nextAttrState = entry.getValue();
            String nextAttrName = entry.getKey();
            if(nextAttrState.isDirty() 
                    && nextAttrState.isPersistent() 
                    && (!nextAttrState.isDeleted())) {
                resultList.add(nextAttrName);
            }
        }
        return resultList; 
    } 

    /**
     * return an ArrayList of Strings
     * whose elements are the names of the added attributes
     */        
    public List<String> getAddedAttributes() {
        List<String> resultList = new ArrayList<String>();
        for (Map.Entry<String, SessionAttributeState> entry : _attributeStates.entrySet()) {
            SessionAttributeState nextAttrState = entry.getValue();
            String nextAttrName = entry.getKey();
            if(!nextAttrState.isPersistent() && !nextAttrState.isDirty()) {
                resultList.add(nextAttrName);
            }
        }
        return resultList;
    }  
    
    /**
     * return an ArrayList of Strings
     * whose elements are the names of the added attributes
     */        
    public List<String> getAddedAttributesPrevious() {
        List<String> resultList = new ArrayList<String>();
        for (Map.Entry<String, SessionAttributeState> entry : _attributeStates.entrySet()) {
            SessionAttributeState nextAttrState = entry.getValue();
            String nextAttrName = entry.getKey();
            if(!nextAttrState.isPersistent()) {
                resultList.add(nextAttrName);
            }
        }
        return resultList;
    }      

    /**
     * clear (empty) the attributeStates
     */     
    void clearAttributeStates() {
        if(_attributeStates == null) {
            _attributeStates = new HashMap<String, SessionAttributeState>();
        }
        _attributeStates.clear();
    }
    
    //this method should only be used for testing
    public void privateResetAttributeState() {
        this.resetAttributeState();
    }

    /**
     * this method called when session is loaded from persistent store
     * or after session state was stored
     * note: pre-condition is that the removed attributes have been
     * removed from _attributeStates; this is taken care of by removeAttribute
     * method
     */      
    void resetAttributeState() {
        clearAttributeStates();
        Enumeration<String> attrNames = getAttributeNames();
        while(attrNames.hasMoreElements()) {
            String nextAttrName = attrNames.nextElement();
            SessionAttributeState nextAttrState =
                SessionAttributeState.createPersistentAttribute();
            _attributeStates.put(nextAttrName, nextAttrState);
        }
        setDirty(false);
    }
    
    /**
     * set the attribute name to the value value
     * and update the attribute state accordingly
     * @param name
     * @param value
     */    
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        SessionAttributeState attributeState = getAttributeState(name);
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("ModifiedAttributeHASession>>setAttribute name=" + name + " attributeState=" + attributeState);
        }
        if(value == null) {
            if(attributeState != null) {
                if(attributeState.isPersistent()) {
                    attributeState.setDeleted(true);
                } else {
                    removeAttributeState(name);
                }
            }
        } else {
            if(attributeState == null) {
                SessionAttributeState newAttrState =
                    new SessionAttributeState();
                //deliberately we do no make this newly added attribute dirty
                _attributeStates.put(name, newAttrState);
            } else {
                //if marked for deletion, only un-delete it
                //do not change the dirti-ness
                if(attributeState.isDeleted()) {
                    attributeState.setDeleted(false);
                } else {
                    //only mark dirty if already persistent
                    //else do nothing
                    if(attributeState.isPersistent()) {
                        attributeState.setDirty(true);
                    }
                }
            }
        }
        setDirty(true);
    }  
    
    /**
     * remove the attribute name
     * and update the attribute state accordingly
     * @param name
     */     
    public void removeAttribute(String name) {


        super.removeAttribute(name);
        SessionAttributeState attributeState = getAttributeState(name);
        if(attributeState != null) {
            if(attributeState.isPersistent()) {
                attributeState.setDeleted(true);
            } else {
                removeAttributeState(name);
            }
        }
        setDirty(true);
    }

    /**
     * return the SessionAttributeState for attributeName
     * @param attributeName
     */       
    SessionAttributeState getAttributeState(String attributeName) {
        return _attributeStates.get(attributeName);
    }
   
    /**
     * set the SessionAttributeState for attributeName
     * based on persistent value
     * @param attributeName
     * @param persistent
     */     
    void setAttributeStatePersistent(String attributeName, boolean persistent) {

        SessionAttributeState attrState = _attributeStates.get(attributeName);
        if (attrState == null) {
                attrState = new SessionAttributeState();
                attrState.setPersistent(persistent);
                _attributeStates.put(attributeName, attrState);
        } else {
                attrState.setPersistent(persistent);
        }
    }

    /**
     * set the SessionAttributeState for attributeName
     * based on dirty value
     * @param attributeName
     * @param dirty
     */     
    void setAttributeStateDirty(String attributeName, boolean dirty) {

        SessionAttributeState attrState = _attributeStates.get(attributeName);
        if (attrState == null) {
                attrState = new SessionAttributeState();
                attrState.setDirty(dirty);
                _attributeStates.put(attributeName, attrState);
        } else {
                attrState.setDirty(dirty);
        }
    }

    /**
     * remove the SessionAttributeState for attributeName
     * @param attributeName
     */ 
    void removeAttributeState(String attributeName) {
        _attributeStates.remove(attributeName);
    }

    /**
     * return isDirty
     */    
    public boolean isDirty() {
        return _dirtyFlag;
    }

    /**
     * set isDirty
     * @param isDirty
     */    
    public void setDirty(boolean isDirty) {
        _dirtyFlag = isDirty;
    }
    
    /* Private Helper method to be used in HAAttributeStore only */ 
    Enumeration<String> privateGetAttributeList() {

        return (new Enumerator<String>(new ArrayList<String>(attributes.keySet())));

    }
}
