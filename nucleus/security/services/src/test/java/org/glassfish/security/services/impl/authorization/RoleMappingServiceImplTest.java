/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.impl.authorization;

import java.net.URI;
import javax.security.auth.Subject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;

import org.glassfish.security.services.api.authorization.AuthorizationService;
import static org.glassfish.security.services.impl.authorization.RoleMappingServiceImpl.InitializationState.*;

/**
 * @see RoleMappingServiceImpl
 */
public class RoleMappingServiceImplTest {

	// Use Authorization for creating the Az typed arguments on Role Service
	private AuthorizationService authorizationService = new AuthorizationServiceImpl();
	private RoleMappingServiceImpl impl;

	@Before
	public void setUp() throws Exception {
		impl = new RoleMappingServiceImpl();
	}

	@After
	public void tearDown() throws Exception {
		impl = null;
	}

	@Test
	public void testInitialize() throws Exception {
		assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );
		try {
			impl.initialize(null);
		} catch ( RuntimeException e ) {
			fail( "Expected service to allow no specified configuration" );
		}

		try {
			impl.isUserInRole("test",
					authorizationService.makeAzSubject(new Subject()),
					authorizationService.makeAzResource(URI.create("test://test")),
					"aRole");
			fail( "Expected fail illegal state exception." );
		} catch ( IllegalStateException e ) {
			assertNotNull("Service fails at run-time", e);
		}

		// The service will fail internally to prevent method calls
		assertSame( "FAILED_INIT", FAILED_INIT, impl.getInitializationState() );
		assertNotNull( "getReasonInitializationFailed", impl.getReasonInitializationFailed() );
	}

	@Test
	public void testIsUserRole() throws Exception {
		assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );
		try {
			impl.isUserInRole("test",
					authorizationService.makeAzSubject(new Subject()),
					authorizationService.makeAzResource(URI.create("test://test")),
					"aRole");
			fail( "Expected fail not initialized." );
		} catch ( RuntimeException e ) {
		}

		assertSame("NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState());
		assertNotNull( "getReasonInitializationFailed", impl.getReasonInitializationFailed() );
	}

	@Test
	public void testIsUserRoleNoAzArgs() throws Exception {
		assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );
		try {
			impl.isUserInRole("test",
					new Subject(),
					URI.create("test://test"),
					"aRole");
			fail( "Expected fail not initialized." );
		} catch ( RuntimeException e ) {
		}

		assertSame("NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState());
		assertNotNull( "getReasonInitializationFailed", impl.getReasonInitializationFailed() );
	}

	@Test
	public void testIsUserRoleNullArgs() throws Exception {
		// Arguments checked before service state
		try {
			impl.isUserInRole("test",
					null,
					authorizationService.makeAzResource(URI.create("test://test")),
					"aRole");
			fail( "Expected fail illegal argument." );
		} catch ( IllegalArgumentException e ) {
			assertNotNull("Subject null test", e);
		}
		try {
			impl.isUserInRole("test",
					authorizationService.makeAzSubject(new Subject()),
					null,
					"aRole");
			fail( "Expected fail illegal argument." );
		} catch ( IllegalArgumentException e ) {
			assertNotNull("Resource null test", e);
		}
	}

	@Test
	public void testIsUserRoleNoAzArgsNullArgs() throws Exception {
		// Arguments checked before service state
		try {
			impl.isUserInRole("test",
					null,
					URI.create("test://test"),
					"aRole");
			fail( "Expected fail illegal argument." );
		} catch ( IllegalArgumentException e ) {
			assertNotNull("Subject null test", e);
		}
		try {
			impl.isUserInRole("test",
					new Subject(),
					null,
					"aRole");
			fail( "Expected fail illegal argument." );
		} catch ( IllegalArgumentException e ) {
			assertNotNull("Subject null test", e);
		}
	}

	@Test
	public void testFindOrCreateDeploymentContext() throws Exception {
		assertSame( "NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState() );
		try {
			impl.findOrCreateDeploymentContext("test");
			fail( "Expected fail not initialized." );
		} catch ( RuntimeException e ) {
		}

		assertSame("NOT_INITIALIZED", NOT_INITIALIZED, impl.getInitializationState());
		assertNotNull("getReasonInitializationFailed", impl.getReasonInitializationFailed());
	}
}
