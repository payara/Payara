/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sun.enterprise.module.ModuleLifecycleListener;
import com.sun.enterprise.module.single.SingleModulesRegistry;

public class APIClassLoaderServiceImplTest {
	int loadClassCalls;
	int getResourceCalls;

	@Before
	public void setUp() {
		loadClassCalls = 0;
		getResourceCalls = 0;
	}

	/**
	 * This test ensures that the ApiClassLoaderService will not attempt to load a class or find a resource after 
	 * an initial negative result until a module is installed or update. 
	 */
	@Test
	public void testBlackList() {
		APIClassLoaderServiceImpl apiClassLoaderService = new APIClassLoaderServiceImpl();

		// set up a fake ModulesRegistry to exercise ModulesRegistry lifecycle
		// events
		FakeClassLoader classLoader = new FakeClassLoader(getClass()
				.getClassLoader());
		FakeModulesRegistry mr = new FakeModulesRegistry(classLoader);

		apiClassLoaderService.mr = mr;

		assertEquals(0, mr.getLifecycleListeners().size());

		apiClassLoaderService.postConstruct();

		List<ModuleLifecycleListener> lifecycleListeners = mr
				.getLifecycleListeners();

		assertEquals(
				"apiClassLoaderService should have registered a lifecycle listener",
				1, mr.getLifecycleListeners().size());

		ModuleLifecycleListener lifecycleListener = lifecycleListeners
				.iterator().next();

		// assert that the classloader isn't called on to load the same bad
		// class twice
		assertEquals(0, loadClassCalls);

		final String BAD_CLASSNAME = "BADCLASS";

		try {
			apiClassLoaderService.getAPIClassLoader().loadClass(BAD_CLASSNAME);
		} catch (ClassNotFoundException e) {
			// ignore
		}

		assertEquals("Classloader.loadClass not called at all", 1,
				loadClassCalls);

		try {
			apiClassLoaderService.getAPIClassLoader().loadClass(BAD_CLASSNAME);
		} catch (ClassNotFoundException e) {
			// ignore
		}

		assertEquals(
				"blacklist not honored, excessive call to classloader.load", 1,
				loadClassCalls);

		// try same thing with resources

		assertEquals(0, getResourceCalls); // sanity

		final String BAD_RESOURCE = "BADRESOURCE";

		apiClassLoaderService.getAPIClassLoader().getResource(BAD_RESOURCE);

		assertEquals("Classloader.findResource not called at all", 1,
				getResourceCalls);

		apiClassLoaderService.getAPIClassLoader().getResource(BAD_RESOURCE);

		assertEquals(
				"blacklist not honored, excessive call to classloader.getResource",
				1, getResourceCalls);

		//
		// Now signal that a new module has been loaded, clearing the blacklist
		//

		lifecycleListener.moduleInstalled(null);

		apiClassLoaderService.getAPIClassLoader().getResource(BAD_RESOURCE);
		assertEquals("blacklist did not clear after a module was installed", 2,
				getResourceCalls);

		try {
			apiClassLoaderService.getAPIClassLoader().loadClass(BAD_CLASSNAME);
		} catch (ClassNotFoundException e) {
			// ignore
		}

		assertEquals("blacklist did not clear after a module was installed", 2,
				loadClassCalls);

		//
		// Now signal that a new module has been updated, clearing the blacklist
		//

		lifecycleListener.moduleUpdated(null);

		apiClassLoaderService.getAPIClassLoader().getResource(BAD_RESOURCE);
		assertEquals("blacklist did not clear after a module was updated", 3,
				getResourceCalls);

		try {
			apiClassLoaderService.getAPIClassLoader().loadClass(BAD_CLASSNAME);
		} catch (ClassNotFoundException e) {
			// ignore
		}

		assertEquals("blacklist did not clear after a module was updated", 3,
				loadClassCalls);

	}

	class FakeModulesRegistry extends SingleModulesRegistry {
		public FakeModulesRegistry(ClassLoader cl) {
			super(cl);
		}
	}

	class FakeClassLoader extends ClassLoader {
		public FakeClassLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Class<?> loadClass(String arg0) throws ClassNotFoundException {
			loadClassCalls++;
			return super.loadClass(arg0);
		}

		@Override
		protected URL findResource(String arg0) {
			getResourceCalls++;
			return super.findResource(arg0);
		}

	}
}
