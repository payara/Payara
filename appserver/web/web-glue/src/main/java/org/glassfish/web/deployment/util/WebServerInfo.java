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

package org.glassfish.web.deployment.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class holds information about a particular web server 
 * installation, its running engines and so on...
 *
 * @author Jerome Dochez
 */
public class WebServerInfo {
    
    /**
     * Holds value of property httpVS.
     */
    private VirtualServerInfo httpVS;
    
    /**
     * Holds value of property httpsVS.
     */
    private VirtualServerInfo httpsVS;
    
    /** Creates a new instance of WebServerInfo */
    public WebServerInfo() {
    }
    
    /**
     * Getter for property httpVS.
     * @return Value of property httpVS.
     */
    public VirtualServerInfo getHttpVS() {
        return this.httpVS;
    }
    
    /**
     * Setter for property httpVS.
     * @param httpVS New value of property httpVS.
     */
    public void setHttpVS(VirtualServerInfo httpVS) {
        this.httpVS = httpVS;
    }
    
    /**
     * Getter for property httpsVS.
     * @return Value of property httpsVS.
     */
    public VirtualServerInfo getHttpsVS() {
        return this.httpsVS;
    }
    
    /**
     * Setter for property httpsVS.
     * @param httpsVS New value of property httpsVS.
     */
    public void setHttpsVS(VirtualServerInfo httpsVS) {
        this.httpsVS = httpsVS;
    }
    
    public URL getWebServerRootURL(boolean secure) throws MalformedURLException {
        if (secure) {
            if (httpsVS!=null)
                return httpsVS.getWebServerRootURL();
            
        } else {
            if (httpVS!=null) 
                return httpVS.getWebServerRootURL();
        }
        return null;
    }
     
}
