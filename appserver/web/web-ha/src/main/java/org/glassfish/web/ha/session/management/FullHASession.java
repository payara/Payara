/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * FullHASession.java
 *
 * Created on October 3, 2002, 3:15 PM
 */

package org.glassfish.web.ha.session.management;

import java.io.Serializable;

import org.apache.catalina.Manager;

/**
 *
 * @author  lwhite
 * @author  Rajiv Mordani
 */
public class FullHASession extends BaseHASession {
    
    /**
     * Creates a new instance of FullHASession
     * @param manager 
     */
    public FullHASession(Manager manager) {
        super(manager);
    }
    
    /**
     * always return true for isDirty()
     * this type of session is always dirty
     */
    public boolean isDirty() {
        return true;
    }

    /** 
     * this is deliberately a no-op
     * store framework calls this method
     * so it must be there but must not have
     * any effect
     * @param isDirty
     */ 
    public void setDirty(boolean isDirty) {
    }
    
    public void removeAttribute(String name) {
        super.removeAttribute(name);
        setDirty(true);
    }

    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        setDirty(true);
    }

    public Object getAttribute(String name) {
        setDirty(true);
        return super.getAttribute(name);
    }                
}
