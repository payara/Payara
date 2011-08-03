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
 * @(#)SecurityContext.java	1.5 00/10/24
 */

package com.sun.enterprise.common.iiop.security;


import javax.security.auth.*;

/*
 * This interface is part of the contract between CSIV2 interceptors
 * and the rest of J2EE RI.
 *
 * @author  Nithya Subramanian
 */


/**
 *  A subject is used a container for passing the security context
 *  information in the service context field. The security context
 *  information in the subject must be stored either as a private or
 *  a public credential according to the following convention:
 *
 *    PasswordCredential:
 *      Client authentication will be performed using the username
 *      and password in the PasswordCredential. PasswordCredential
 *      must be passed as a PrivateCredential.
 *
 *    X500Name:
 *      DN name specified in X500Name will be asserted. X500Name must 
 *      be passed as a PublicCredential.
 *
 *    GSSUPName:
 *      Identity specified in GSSUPName will be asserted. GSSUPName must
 *      be passed as a PublicCredential.
 *
 *    X509CertificateCredential:
 *      The certificate chain in the credential will be asserted. The 
 *      credential must be passed as a PublicCredential.
 *
 *    AnonCredential:
 *      Anonymous identity will be asserted. Credential must be passed
 *      as a PublicCredential.
 *
 *    Class fields in the SecurityContext are used for credential selection.
 *    There are two class fields: authcls and identcls.
 *    
 *    authcls is a Class object that identifies the credential for 
 *    client authentication.
 *          
 *    identcls is a Class object that identifies the credential for
 *    identity assertion.
 *
 *  The following semantics must be observed:
 * 
 *  1. A client authentication token is always passed as a private
 *     credential. authcls set to the class of the authentication token
 *     
 *  2. An identity token is always passed as a public credential.
 *     identcls is set to the class of the identity token.
 *  
 *  3. authcls is set to null if there is no client auth token
 * 
 *  4. identcls is set to null if there is no ident token
 *
 *  5. There must not be more than one instance of class identified
 *     by authcls or identcls. However, there can be one instance of
 *     identcls *and* authcls (this allows both a client auth token
 *     and an identity token to be passed across the interface).
 */

public class SecurityContext {
    public Subject subject;
    public Class   authcls;
    public Class   identcls;
}
