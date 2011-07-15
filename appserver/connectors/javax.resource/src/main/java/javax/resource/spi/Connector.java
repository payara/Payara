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
import javax.resource.spi.work.WorkContext;

/**
 * The <code>Connector</code> annotation is a component-defining annotation and
 * it can be used by the resource adapter developer to specify that the JavaBean
 * is a resource adapter JavaBean. The Connector annotation is applied to the
 * JavaBean class.
 * 
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Connector {

 	/**
     * Describes the resource adapter module.
     */
    String[] description() default {};

    /**
     * An optional short name, providing information about the
     * resource adapter module,  that is intended to be displayed 
     * by tools.
     */
  	String[] displayName() default {};

	/**
     * Specifies the file name for small GIF or JPEG icon images that are 
     * used to represent the resource adapter in a GUI tool. 
     *
     * Each smallIcon must be associated with a largeIcon element and the 
     * application server must use the ordinal value in their respective 
     * arrays to find the related pairs of icons.
     */
    String[] smallIcon() default {};

    /**
     * Specifies the file name for large GIF or JPEG icon images that are 
     * used to represent the resource adapter in a GUI tool. 
     * Each smallIcon must be associated with a largeIcon element and 
     * the application server must use the ordinal value in their 
     * respective arrays to find the related pairs of icons.
     */
	String[] largeIcon() default {};

	/**
	 * Specifies the name of the resource adapter provider vendor.
	 */
	String vendorName() default "";

	/**
	 * Contains information about the type of EIS. For example, the type of an
	 * EIS can be product name of the EIS independent of any version info.This
	 * helps in identifying EIS instances that can be used with this resource
	 * adapter.
	 */
	String eisType() default "";

	/**
	 * Specifies the version of the resource adapter implementation.
	 */
	String version() default "";

	/**
	 * Specifies licensing requirements for the resource adapter module and an
	 * optional description of the licensing terms .
	 */
	String[] licenseDescription() default {};

	/**
	 * Specifies whether a license is required to deploy and use this resource
	 * adapter
	 */
	boolean licenseRequired() default false;

	/**
	 * Specifies the authentication mechanisms supported by the resource
	 * adapter.
	 * 
	 * @see AuthenticationMechanism
	 */
	AuthenticationMechanism[] authMechanisms() default {};

	/**
	 * Specifies whether a license is required to deploy and use this resource
	 * adapter
	 */
	boolean reauthenticationSupport() default false;

	/**
	 * Specifies the extended security permissions required to be provided for
	 * the operation of the resource adapter module
	 * 
	 * @see SecurityPermission
	 */
	SecurityPermission[] securityPermissions() default {};

	/**
	 * Specifies the level of transaction support provided by the resource
	 * adapter.
	 * 
	 * @see TransactionSupport.TransactionSupportLevel
	 */
	TransactionSupport.TransactionSupportLevel transactionSupport() default TransactionSupport.TransactionSupportLevel.NoTransaction;

	/**
	 * Specifies a list of fully qualified classes that implements the
	 * {@link WorkContext WorkContext} interface that a resource adapter
	 * requires the application server to support.
	 */
	Class<? extends WorkContext>[] requiredWorkContexts() default {};
}
