package fish.payara.samples.ejbhttp;

/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
import fish.payara.samples.ejbhttp.api.User;
import fish.payara.samples.ejbhttp.client.RemoteConnector;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertNull;
import org.junit.runner.RunWith;

/**
 * Test collections as arguments and return types
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class CollectionTypesIT extends AbstractClient {
    @Parameter
    public RemoteConnector connector;

    @Override
    protected final RemoteConnector getConnector() {
        return connector;
    }

    @Test
    public void testElementaryListReturnNull() {
        List<String> result = remoteService.elementaryListOperation(-1);
        assertNull(result);
    }

    @Test
    public void testElementaryListReturnEmpty() {
        List<String> result = remoteService.elementaryListOperation(0);
        assertThat(result).isEmpty();
    }

    @Test
    public void testElementaryList() {
        List<String> result = remoteService.elementaryListOperation(10);
        assertThat(result).hasSize(10);
    }

    @Test
    public void testElementaryMapReturnNull() {
        Map<String, String> result = remoteService.elementaryMapOperation(-1);
        assertThat(result).isNull();
    }

    @Test
    public void testElementaryMapReturnEmpty() {
        Map<String, String> result = remoteService.elementaryMapOperation(0);
        assertThat(result).isEmpty();
    }

    @Test
    public void testElementaryMap() {
        Map<String, String> result = remoteService.elementaryMapOperation(10);
        assertThat(result).hasSize(10);
    }

    @Test
    public void testListReturnAndArgNull() {
        List<User> result = remoteService.listUsers(null);
        assertThat(result).isNull();
    }

    @Test
    public void testListResultAndArgEmpty() {
        List<User> result = remoteService.listUsers(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    public void testListResultAndArg() {
        List<String> input = IntStream.range(0, 10).mapToObj(i -> "User" + i).collect(Collectors.toList());
        List<User> result = remoteService.listUsers(input);
        assertThat(result).hasSize(10)
                .extracting(u -> u.login).zipSatisfy(input, (remote, local) -> remote.equals(local));
    }

    @Test
    public void testArrayReturnAndArgNull() {
        String[] result = remoteService.someIds(null);
        assertThat(result).isNull();
    }

    @Test
    public void testArrayResultAndArgEmpty() {
        String[] result = remoteService.someIds(new User[0]);
        assertThat(result).isEmpty();
    }

    @Test
    public void testArrayResultAndArg() {
        User[] input = IntStream.range(0, 10).mapToObj(i -> {
            User u = new User();
            u.login = "User" + i;
            return u;
        }).toArray(User[]::new);
        String[] result = remoteService.someIds(input);
        assertThat(result).zipSatisfy(input, (remote, local) -> remote.equals(local.login));
    }

    @Deployment(testable = false)
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class).addPackages(true, AbstractClient.class.getPackage());
    }
}
