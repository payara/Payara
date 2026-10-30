/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.internal.deployment;

import java.io.IOException;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Susan Rai
 */
public class GetApplicationNameTest {

    private static final String USER_PROVIDED_APP_NAME = "originalAppName";
    //Mocks application name from reading the standard deployment descriptor.
    private static final String DD_APP_NAME = "DDAppName";
    private static final String DEFAULT_ARCHIVE_NAME = "defaultArchiveName";

    @InjectMocks
    private GenericHandlerStub genericHandler = new GenericHandlerStub();

    @Mock
    private ServiceLocator habitat;

    @Mock
    private ApplicationInfoProvider applicationInfoProvider;

    private ReadableArchive readableArchive;

    @Before
    public void initialiseMocks() {
        MockitoAnnotations.initMocks(this);
        readableArchive = mock(ReadableArchive.class);
        when(habitat.getService(ApplicationInfoProvider.class)).thenReturn(applicationInfoProvider);
    }

    /**
     * Application name provided in the deployment descriptor will always take
     * precedence over the one provided by a user.
     */
    @Test
    public void testWhenDDHasAppName() {
        assertTheAppName(DD_APP_NAME, DD_APP_NAME, USER_PROVIDED_APP_NAME);
    }

    /**
     * When application name is not provided in the deployment descriptor and
     * the user hasn't provided an application name, use the name of the
     * archive.
     */
    @Test
    public void testWithoutUserProvidedAppName() {
        assertTheAppName(DEFAULT_ARCHIVE_NAME, null, null);
    }

    /**
     * If application name is not provided in the deployment descriptor, it
     * should use the one provided by a user if present.
     */
    @Test
    public void testDDWithoutAppName() {
        assertTheAppName(USER_PROVIDED_APP_NAME, null, USER_PROVIDED_APP_NAME);
    }

    /**
     * When Application name is not set in the deployment descriptor, it should
     * use the one provided by a user if present, rather than replace it with
     * the name of the archive.
     */
    @Test
    public void testDDWithoutAppNameWithArchiveName() {
        assertTheAppName(USER_PROVIDED_APP_NAME, DEFAULT_ARCHIVE_NAME, USER_PROVIDED_APP_NAME);
    }

    private void assertTheAppName(String correctAppName, String appNameToBeprocessed, String userProvidedAppName) {
        when(applicationInfoProvider.getNameFor(readableArchive, null)).thenReturn(appNameToBeprocessed);
        when(readableArchive.getName()).thenReturn(DEFAULT_ARCHIVE_NAME);
        assertEquals(correctAppName, genericHandler.getDefaultApplicationName(readableArchive, null, userProvidedAppName));
    }

    class GenericHandlerStub extends GenericHandler {

        @Override
        public String getArchiveType() {
            return "jar";
        }

        @Override
        public boolean handles(ReadableArchive archive) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ClassLoader getClassLoader(ClassLoader parent, DeploymentContext context) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

}
