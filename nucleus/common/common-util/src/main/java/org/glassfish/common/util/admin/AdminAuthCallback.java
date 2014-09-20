/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.common.util.admin;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import org.jvnet.hk2.annotations.Contract;

/**
 * Prescribes behavior of authentication callbacks which modules can implement
 * to provide callback behavior without the authentication logic needing to 
 * know about specific implementations ahead of time.
 * 
 * @author tjquinn
 */
@Contract
public interface AdminAuthCallback extends Callback {
    /**
     * Prescribes behavior of callbacks that use request-time data in making
     * their decisions.
     * <p>
     * Token-based authentication, for example, uses a token conveyed with the
     * request as a stand-in for username/password-based authentication.  To
     * keep inter-module dependencies simpler an implementation will probably
     * check the type of the data and then cast it if appropriate.
     */
    public interface RequestAccepter extends AdminAuthCallback {
        public void setRequest(Object data);
    }
    
    public interface Validator extends AdminAuthCallback {
        public boolean isValid();
    }
    
    public interface Authenticator extends AdminAuthCallback {
        public Subject getSubject();
    }
}
