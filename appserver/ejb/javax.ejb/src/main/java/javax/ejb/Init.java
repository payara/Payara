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

package javax.ejb;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/**
 * Designates a method of a session bean that corresponds to a
 * <code>create&#060;METHOD&#062;</code> method of an adapted home or
 * local home interface (an interface that adapts an EJB 2.1 or earlier
 * EJBHome or EJBLocalHome client view respectively).
 *
 * <p> The result type of such an <code>Init</code> method is required
 * to be <code>void</code>, and its parameter types must be exactly the same as
 * those of the referenced <code>create&#060;METHOD&#062;</code>
 * method(s).
 *
 * <p>
 * An <code>Init</code> method is only required to be specified for
 * stateful session beans that provide a <code>RemoteHome</code> or
 * <code>LocalHome</code> interface. 
 *
 * <p> The name of the adapted <code>create&#060;METHOD&#062;</code>
 * method of the home or local home interface must be specified if the
 * adapted interface has more than one
 * <code>create&#060;METHOD&#062;</code> method and the method
 * signatures are not the same.
 *
 * @see LocalHome
 * @see RemoteHome
 *
 * @since EJB 3.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Init {

    /**
     * The name of the corresponding
     * <code>create&#060;METHOD&#062;</code> method of the adapted
     * home or local home interface.  This value is used to
     * disambiguate the case where there are multiple
     * <code>create&#060;METHOD&#062;</code> methods on an adapted
     * home and/or local home interface.  If there are multiple
     * <code>create&#060;METHOD&#062;</code> methods on the adapted
     * interface(s) and no value is specified, the
     * <code>create&#060;METHOD&#062;</code> matching is based on
     * signature only.
     * <p>
     * Note that this value is not required to be specified if there
     * is only a single <code>create&#060;METHOD&#062;</code> method
     * even if the name of the method to which the <code>Init</code>
     * annotation is applied does not match that of the
     * <code>create&#060;METHOD&#062;</code> method.
     */
    String value() default "";

}
