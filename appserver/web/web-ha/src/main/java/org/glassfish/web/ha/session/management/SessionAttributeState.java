/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * SessionAttributeState.java
 *
 * Created on October 3, 2002, 3:42 PM
 */

package org.glassfish.web.ha.session.management;

/**
 *
 * @author  lwhite
 * @author Rajiv Mordani
 * 
 * SessionAttributeState represents the state (with regards to persistence
 * activity) of each attribute in a session.
 *
 * Invariants:
 * 1. newly added attribute (not yet persistent) 
 *      not persistent
 *      not dirty
 *      not deleted
 * 2. modified already existing session
 *      persistent
 *      dirty
 *      not deleted
 * 3. already existing session to be deleted
 *      persistent
 *      dirty or not dirty
 *      deleted
 */
public class SessionAttributeState {
        
    /** Creates a new instance of SessionAttributeState */
    public SessionAttributeState() {
    }
    
    /**
     * create an instance of SessionAttributeState
     * representing a persistent attribute
     */     
    public static SessionAttributeState createPersistentAttribute() {
        SessionAttributeState result = new SessionAttributeState();
        result.setPersistent(true);
        return result;
    }
    
    /**
     * return isDirty
     */     
    public boolean isDirty() {
        return _dirtyFlag;
    }
    
    /**
     * set isDirty
     * @param value
     */    
    public void setDirty(boolean value) {
        _dirtyFlag = value;
    }
    
    /**
     * return isPersistent
     */      
    public boolean isPersistent() {
        return _persistentFlag;
    }
    
    /**
     * set persistentFlag
     * @param value
     */     
    public void setPersistent(boolean value) {
        _persistentFlag = value;
    }    
    
    /**
     * return isDeleted
     */     
    public boolean isDeleted() {
        return _deletedFlag;
    }
    
    /**
     * set deletedFlag
     * @param value
     */     
    public void setDeleted(boolean value) {
        _deletedFlag = value;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SessionAttributeState");
        sb.append("\n__dirtyFlag = " + _dirtyFlag);
        sb.append("\n__persistentFlag = " + _persistentFlag);
        sb.append("\n_deletedFlag = " + _deletedFlag);
        return sb.toString();
    }
    
    boolean _dirtyFlag = false;
    boolean _persistentFlag = false;
    boolean _deletedFlag = false;    
    
}
