/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.datagridencryption.sfsb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.io.Serializable;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@ApplicationScoped
@Path("/TestEjb")
public class TestEjbEndpoints implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(TestEjbEndpoints.class.getName());
    private static final long serialVersionUID = 1L;

    @Inject
    TestEjb testEjb;

    @Inject
    TestEjb testEjb2;

    @GET
    public String testEjb() {
        testEjb.addItem("apple");
        testEjb.addItem("pear");
        testEjb.addItem("bear");
        testEjb.removeItem("bear");

        return testEjb.getItems();
    }

    @GET
    @Path("2")
    public String testEjb2() {
        testEjb2.addItem("bapple");
        testEjb2.addItem("bear");
        testEjb2.addItem("care");
        testEjb2.removeItem("bear");

        return testEjb2.getItems();
    }

    @GET
    @Path("Lookup")
    public String lookup() {

        try {
            for (int i = 0; i < 600; i++) {
                InitialContext initialContext = new InitialContext();
                TestEjb testEjbLookup = (TestEjb) initialContext.lookup(
                        "java:global/sfsb-passivation/TestEjbImpl!fish.payara.samples.datagridencryption.sfsb.TestEjb");

                if (new Random().nextBoolean()) {
                    testEjbLookup.addItem("bipple");
                } else {
                    testEjbLookup.addItem("bopple");
                }
            }

            return "Finished: check logs...";
        } catch (NamingException ne) {
            LOGGER.log(Level.SEVERE, "lookup", ne);
            return "Ruh Roh!";
        }
    }
}
