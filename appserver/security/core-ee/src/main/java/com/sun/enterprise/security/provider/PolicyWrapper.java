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
 * PolicyWrapper.java
 *
 * @author Harpreet Singh (harpreet.singh@sun.com)
 * @author Ron Monzillo
 * @version
5B
 * Created on May 23, 2002, 1:56 PM
 */

package com.sun.enterprise.security.provider;

/**
 * This class is a wrapper around the default jdk policy file 
 * implementation. PolicyWrapper is installed as the JRE policy object
 * It multiplexes policy decisions to the context specific instance of
 * com.sun.enterprise.security.provider.PolicyFile.
 * Although this Policy provider is implemented using another Policy class,
 * this class is not a "delegating Policy provider" as defined by JACC, and
 * as such it SHOULD not be configured using the JACC system property
 * javax.security.jacc.policy.provider.
 * @author Harpreet Singh (harpreet.singh@sun.com)  
 * @author Jean-Francois Arcand
 * @author Ron Monzillo
 *
 */
public class PolicyWrapper extends BasePolicyWrapper {
    
    // override to change the implementation of PolicyFile
    /** gets the underlying PolicyFile implementation
     * can be overridden by Subclass
     */
    @Override
    protected java.security.Policy getNewPolicy() {
	return (java.security.Policy) new sun.security.provider.PolicyFile();
    }
}

