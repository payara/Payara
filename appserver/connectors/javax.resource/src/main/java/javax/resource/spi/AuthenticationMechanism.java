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

package javax.resource.spi;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;
import static java.lang.annotation.RetentionPolicy.*;

@Documented
@Retention(RUNTIME)
@Target({})
/* The <code>AuthenticationMechanism</code> declared type is intended 
solely for use as a member type in complex annotation type declarations
like <code>Connector</code>.
*/

/**
 * An annotation used to specify the authentication mechanism 
 * supported by the resource adapter.
 *
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */
public @interface AuthenticationMechanism {

    /** 
     * An enumerated type that represents the various interfaces
     * that a resource adapter may support for the representation
     * of the credentials.
     *
     * @since 1.6
     * @version Java EE Connector Architecture 1.6
     */
    public enum CredentialInterface {
        /**
         * Corresponds to 
         * <code>javax.resource.spi.security.PasswordCredential</code>.
         * This is the default credential interface
         */
        PasswordCredential, 
        
        /**
         * Corresponds to <code>org.ietf.jgss.GSSCredential</code>
         */
        GSSCredential,
        
        /**
         * Corresponds to 
         * <code>javax.resource.spi.security.GenericCredential</code>
         */
        GenericCredential 
    };

    /**
     * The authentication-mechanismType specifies an authentication
     * mechanism supported by the resource adapter. Note that this
     * support is for the resource adapter and not for the
     * underlying EIS instance.
     *
     */
    String authMechanism() default  "BasicPassword";

    /**
     * The optional description specifies
     * any resource adapter specific requirement for the support of
     * security contract and authentication mechanism.
     */
    String[] description() default {};

    /**
     * Represents the interface that the resource adapter implementation
     * supports for the representation of the credentials.
     *
     * Note that BasicPassword mechanism type should support the
     * <code>javax.resource.spi.security.PasswordCredential</code> interface.
     * The Kerbv5 mechanism type should support the
     * <code>org.ietf.jgss.GSSCredential</code> interface or the deprecated
     * <code>javax.resource.spi.security.GenericCredential</code> interface.
     */
    CredentialInterface credentialInterface() 
    					default CredentialInterface.PasswordCredential;
}

