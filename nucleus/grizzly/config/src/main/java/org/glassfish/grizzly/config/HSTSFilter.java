/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021-2023] Payara Foundation and/or affiliates
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package org.glassfish.grizzly.config;

import java.io.IOException;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Filter that adds HSTS header to all requests
 * @author jonathan coustick
 * @since 5.28.0
 */
public class HSTSFilter extends BaseFilter implements ConfigAwareElement<Ssl> {
    
    private static final String HSTS_HEADER = "Strict-Transport-Security";
    private static final String MAX_AGE = "max-age=31536000"; //1 year
    private static final String SUBDOMAINS_HEADER = "; includeSubDomains";
    private static final String PRELOAD_HEADER = "; preload";
    
    
    private boolean enabled;
    private boolean includeSubdomains;
    private boolean preload;
    
    private String header;


    @Override
    public void configure(ServiceLocator habitat, NetworkListener networkListener, Ssl configuration) {
        enabled = Boolean.parseBoolean(configuration.getHstsEnabled());
        includeSubdomains = Boolean.parseBoolean(configuration.getHstsSubdomains());
        preload = Boolean.parseBoolean(configuration.getHstsPreload());
        
        header = MAX_AGE;
        if (includeSubdomains) {
            header += SUBDOMAINS_HEADER;
        }
        if (preload) {
            header += PRELOAD_HEADER;
        }
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        Object message = ctx.getMessage();
        if (message instanceof HttpContent && enabled) {
            HttpContent content = (HttpContent) message;
            if(!content.getHttpHeader().containsHeader(HSTS_HEADER)) {
                content.getHttpHeader().addHeader(HSTS_HEADER, header);
            }
        }     
        
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        Object message = ctx.getMessage();
        if (message instanceof HttpContent && enabled) {
            HttpContent content = (HttpContent) message;
            if(!content.getHttpHeader().containsHeader(HSTS_HEADER)) {
                content.getHttpHeader().addHeader(HSTS_HEADER, header);
            }
        }     
        
        return ctx.getInvokeAction();
    }
    
}
