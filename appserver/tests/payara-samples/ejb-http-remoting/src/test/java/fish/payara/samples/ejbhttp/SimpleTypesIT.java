package fish.payara.samples.ejbhttp;

/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.Parameter;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.ejbhttp.client.RemoteConnector;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test values and operations involving primitive objects and their collections.
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class SimpleTypesIT extends AbstractClient {
    @Parameter
    public RemoteConnector connector;

    @Override
    protected final RemoteConnector getConnector() {
        return connector;
    }

    @Test
    public void testPrimitiveValue() {
        assertEquals(-1, remoteService.simpleOperation(null, null));
        assertEquals(0, remoteService.simpleOperation("", null));
        assertEquals(4, remoteService.simpleOperation("test", null));
        assertEquals(6, remoteService.simpleOperation("test", 2.1));
    }

    @Test
    public void testSimpleLists() {
        // yasson bug: Cannot deserialize null
        // assertNull(remoteService.elementaryListOperation(-1));
        assertEquals(0, remoteService.elementaryListOperation(0).size());
        assertEquals(16, remoteService.elementaryListOperation(16).size());
    }

    @Test
    public void testSimpleMaps() {
        // yasson bug: Cannot deserialize null
        // assertNull(remoteService.elementaryMapOperation(-1));
        assertEquals(0, remoteService.elementaryMapOperation(0).size());
        assertEquals(16, remoteService.elementaryMapOperation(16).size());
    }

    @Deployment(testable = false)
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class).addPackages(true, AbstractClient.class.getPackage());
    }
}
