/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.test.domain;

import fish.payara.test.util.BeanProxy;
import fish.payara.test.util.PayaraMicroServer;

public final class HealthCheckServiceConfiguration extends BeanProxy {

    private static final String CONFIG_CLASS_NAME =
            "fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration";

    public static HealthCheckServiceConfiguration from(PayaraMicroServer server) {
        return from(server, "server-config");
    }

    private static HealthCheckServiceConfiguration from(PayaraMicroServer server, String target) {
        return new HealthCheckServiceConfiguration(
                server.getExtensionByType(target, server.getClass(CONFIG_CLASS_NAME)));
    }

    private HealthCheckServiceConfiguration(Object config) {
        super(config);
    }

    public boolean getEnabled() {
        return booleanValue("getEnabled");
    }

    public boolean getHistoricalTraceEnabled() {
        return booleanValue("getHistoricalTraceEnabled");
    }

    public int getHistoricalTraceStoreSize() {
        return intValue("getHistoricalTraceStoreSize");
    }

    public int getHistoricalTraceStoreTimeout() {
        return intValue("getHistoricalTraceStoreTimeout");
    }

    public Checker getCheckerByType(Class<?> checkerType) {
        return Checker.from(this, checkerType);
    }

    public Notifier getNotifierByType(Class<?> notifierType) {
        return Notifier.from(this, notifierType);
    }
}
