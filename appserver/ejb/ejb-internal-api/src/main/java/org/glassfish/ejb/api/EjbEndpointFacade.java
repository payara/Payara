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

package org.glassfish.ejb.api;


import org.glassfish.api.invocation.ComponentInvocation;


/**
 * This interface provides services needed by the web services runtime
 * to flow an invocation through the ejb container to an EJB
 * web service endpoint.
 *
 * @author Kenneth Saks
 */


public interface EjbEndpointFacade {


    /**
     * Returns the application class loader associated with this
     * web service endpoint.  This class loader must be the
     * Thread's context class loader when startInvocation() is called
     * and must remain the Thread's context class loader until
     * after endInvocation() returns.   
     */
    public ClassLoader getEndpointClassLoader();


    /**
     * Start an invocation for the EJB web service endpoint.
     * Once startInvocation() is called, endInvocation() must be called
     * at some later time on the same thread.  Interleaved invocations
     * on the same thread are not allowed.
     *
     * @return A component invocation for this invocation.  Must be
     *         passed to the corresponding endInvocation.
     */
    public ComponentInvocation startInvocation();
    

    /**
     * Perform post-processing for the web service endpoint invocation.
     * @argument inv The ComponentInvocation returned from the original
     *               startInvocation
     */
    public void endInvocation(ComponentInvocation inv);

}
