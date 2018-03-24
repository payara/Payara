/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.nucleus.microprofile.config.source;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;

/**
 * Config Source that gives you information about the Payara instance you are
 * running in
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class PayaraServerProperties extends PayaraConfigSource implements ConfigSource {

    private HashMap<String, String> properties;

    public PayaraServerProperties() {
        ServerEnvironment server = Globals.getDefaultHabitat().getService(ServerEnvironment.class);
        ServerContext serverCtx = Globals.getDefaultHabitat().getService(ServerContext.class);
        properties = new HashMap<>(20);
        properties.put("payara.instance.type", server.getRuntimeType().name());
        properties.put("payara.instance.name", server.getInstanceName());
        properties.put("payara.instance.root", server.getInstanceRoot().getAbsolutePath());
        properties.put("payara.domain.name", server.getDomainName());
        properties.put("payara.domain.installroot", serverCtx.getInstallRoot().getAbsolutePath());
        properties.put("payara.config.dir", server.getConfigDirPath().getAbsolutePath());
        properties.put("payara.instance.starttime", Long.toString(server.getStartupContext().getCreationTime()));
        properties.put("payara.instance.config.name", serverCtx.getConfigBean().getConfig().getName());
        properties.put("payara.instance.admin.port", Integer.toString(serverCtx.getConfigBean().getAdminPort()));
        properties.put("payara.instance.admin.host", serverCtx.getConfigBean().getAdminHost());

        NetworkListener listener = serverCtx.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("http-listener-1");
        if (listener == null) {
            listener = serverCtx.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("http-listener");
        }
        if (listener != null) {
            properties.put("payara.instance.http.port", listener.getPort());
            properties.put("payara.instance.http.address", listener.getAddress());
            properties.put("payara.instance.http.enabled", listener.getEnabled());
        }
        listener = serverCtx.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("http-listener-2");
        if (listener == null) {
            listener = serverCtx.getConfigBean().getConfig().getNetworkConfig().getNetworkListener("https-listener");            
        }
        if (listener != null) {
            properties.put("payara.instance.https.port", listener.getPort());
            properties.put("payara.instance.https.address", listener.getAddress());
            properties.put("payara.instance.https.enabled", listener.getEnabled());
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public int getOrdinal() {
        return 1000;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return "Payara";
    }

}
