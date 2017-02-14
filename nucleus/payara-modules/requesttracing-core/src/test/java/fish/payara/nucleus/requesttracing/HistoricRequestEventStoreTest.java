/*
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.requesttracing.domain.HistoricRequestEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.when;

/**
 * @author mertcaliskan
 */
@RunWith(MockitoJUnitRunner.class)
public class HistoricRequestEventStoreTest {

    @Mock
    HazelcastCore hzCore;

    @InjectMocks
    HistoricRequestEventStore store = new HistoricRequestEventStore();

    @Before
    public void setup() {
        when(hzCore.isEnabled()).thenReturn(false);

        store.initialize(5);
    }

    @Test
    public void threeTracesStoredSuccessfully() {
        store.addTrace(100, "quick execution");
        store.addTrace(300, "normal execution");
        store.addTrace(2000, "slow execution");

        assertNotNull(store.getTraces());
        assertThat(store.getTraces().length, is(3));
    }

    @Test
    public void sixTracesTriedToBeStoredAndSlowestFiveStoredSuccessfully() {
        store.addTrace(101, "quick execution 2");
        store.addTrace(102, "quick execution 3");
        store.addTrace(100, "quick execution 1");
        store.addTrace(300, "normal execution");
        store.addTrace(2001, "slow execution 2");
        store.addTrace(2000, "slow execution 1");

        assertNotNull(store.getTraces());
        assertThat(store.getTraces().length, is(5));
        assertThat(Arrays.asList(store.getTraces()), not(hasItem(new HistoricRequestEvent(100, "quick execution 1"))));
        assertThat(Arrays.asList(store.getTraces()), hasItem(new HistoricRequestEvent(101, "quick execution 2")));
    }
}