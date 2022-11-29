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
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
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

import fish.payara.ejb.http.protocol.SerializationType;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.Parameter;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.ejbhttp.api.Product;
import fish.payara.samples.ejbhttp.api.Ranking;
import fish.payara.samples.ejbhttp.api.Stuff;
import fish.payara.samples.ejbhttp.api.Stuff.Container;
import fish.payara.samples.ejbhttp.client.RemoteConnector;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Ten customized JSON-B serialization
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class CustomizedTypesIT extends AbstractClient {
    @Parameter
    public RemoteConnector connector;

    @Override
    protected final RemoteConnector getConnector() {
        return connector;
    }

    @Test
    @Ignore("EE10 TODO: Yasson has bug handling custom serializers")
    public void annotatedSerializerTopLevelReturn() {
        int rankings=0, products = 0;
        for(int i=0; i<100 && (rankings == 0 || products == 0); i++) {
            Stuff result = remoteService.polymorphicReturn(false).get();
            if (result instanceof Product) {
                products++;
            } else if (result instanceof Ranking) {
                rankings++;
            } else {
                fail("What the hell is "+result);
            }
        }
        assertThat(rankings).isGreaterThan(0);
        assertThat(products).isGreaterThan(0);
    }

    @Test
    public void annotatedSerializerTopLevelReturnNull() {
        Container result = remoteService.polymorphicReturn(true);
        if (getSerializationType() == SerializationType.JSON) {
            assertThat(result.get()).isNull();
        } else {
            assertNull(result);
        }
    }

    @Test
    @Ignore("EE10 TODO: Yasson has bug regarding custom serializers")
    public void annotatedSerializerContainerReturn() {
        List<Stuff.Container> result = remoteService.polymorphicReturn(100);
        assertThat(result).hasSize(100)
            .extracting(Stuff.Container::get).allMatch(Stuff.class::isInstance);
    }

    @Test
    @Ignore // FIXME TODO multiple runs cause class loader leak on the server
    public void annotatedSerializerContainerArgument() {
        int result = remoteService.countProducts(Stream.of(
                new Product("1", BigDecimal.ONE),
                new Ranking("1", 4),
                new Product("2", BigDecimal.ZERO)
        ).map(Stuff.Container::new).collect(toList()));
        assertThat(result).isEqualTo(2);
    }

    @Deployment(testable = false)
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class).addPackages(true, AbstractClient.class.getPackage());
    }
}
