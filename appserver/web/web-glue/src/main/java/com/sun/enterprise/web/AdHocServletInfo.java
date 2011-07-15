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

package com.sun.enterprise.web;

import java.util.Map;

/**
 * Specifies behavior that the requester of an ad hoc path must implement.
 * <p>
 * The web container will call back to these methods when it needs to create an
 * instance of the servlet to support the path.
 *
 * @author Tim Quinn
 */
public interface AdHocServletInfo {
    
    /**
     * Returns the class type of the servlet that should be created to process
     * requests.  Note that the class must represent a subclass of HttpServlet.
     *
     * @return The servlet class
     */
    public Class getServletClass();
    
    /**
     * Returns the name of the servlet that the container should assign when it
     * adds a servlet to a web module.
     *
     * @return The servlet name
     */
    public String getServletName();
    
    /**
     * Returns a Map containing name and value pairs to be used in preparing
     * the init params in the servlet's ServletConfig object.
     *
     * @return Map containing the servlet init parameters
     */
    public Map<String,String> getServletInitParams();
    
}
