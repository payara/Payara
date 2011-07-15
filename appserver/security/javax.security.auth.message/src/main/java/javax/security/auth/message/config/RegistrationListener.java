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

package javax.security.auth.message.config;

import javax.security.auth.message.*;

/**
 * An implementation of this interface may be associated with an 
 * AuthConfigProvider registration at an AuthConfigFactory at the 
 * time the AuthConfigProvider is obtained for use from the factory. 
 * The AuthConfigFactory will invoke the notify
 * method of the RegistrationListener if the corresponding provider 
 * registration is unregistered or replaced at the factory. 
 */

public interface RegistrationListener {

    /**
     * Notify the listener that a registration with which it was
     * associated was replaced or unregistered.
     *
     * <p> When a RegistrationListener is associated with a provider
     * registration within the factory, the factory must call its
     * <code>notify</code> method when the corresponding registration 
     * is unregistered or replaced.
     
     * @param layer A String identifying the one or more message layers 
     * corresponding to the registration for which the listerner is being 
     * notified.
     *
     * @param appContext A String value identifying the application 
     * contexts corresponding to the registration for which the listener is 
     * being notified.
     *
     * The factory detaches the listener from the corresponding 
     * registration once the listener has been notified for the 
     * registration.
     * 
     * The <code>detachListerner</code> method
     * must be called to detach listeners that are no longer in use.
     */
    public void notify(String layer, String appContext);
    
}




















