/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.jwtauth.tck;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.impl.enricher.resource.OperatesOnDeploymentAwareProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;

/**
 * A minimal replacement for the base URL provider backing the URL injection
 * in tests. This is needed to compensate for an ending slash difference.
 * 
 * @author Arjan Tijms
 *
 */
public class URLResourceProvider extends OperatesOnDeploymentAwareProvider {

    @Inject
    private Instance<ProtocolMetaData> protocolMetadataInstance;

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        try {
            
            return removeTrailingSlash(
                    protocolMetadataInstance
                            .get()
                            .getContexts(HTTPContext.class)
                            .iterator()
                            .next()
                            .getServlets()
                            .get(0)
                            .getBaseURI()
                            .toURL());
            
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Error converting to URL", e);
        }
    }
    
    private URL removeTrailingSlash(URL url) throws MalformedURLException {
        if (url.getPath().endsWith("/")) {
            String urlString = url.toExternalForm();
            urlString = urlString.substring(0, urlString.length() - 1);

            url = new URL(urlString);
        }

        return url;
    }
    
    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(URL.class);
    }

}
