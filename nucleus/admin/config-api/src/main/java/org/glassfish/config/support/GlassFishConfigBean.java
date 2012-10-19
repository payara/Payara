/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package org.glassfish.config.support;

import java.lang.reflect.Proxy;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigView;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.Transformer;

/**
 * Translated view of a configured objects where values can be represented
 * with a @{xx.yy.zz} name to be translated using a property value translator.
 *
 * @author Jerome Dochez
 */
public final class GlassFishConfigBean extends ConfigBean {

     transient TranslatedConfigView defaultView;

    /**
     * Returns the translated view of a configuration object
     * @param s the config-api interface implementation
     * @return the new interface implementation providing the raw view
     */
    public static <T  extends ConfigBeanProxy> T getRawView(T s) {

        Transformer rawTransformer = new Transformer() {
            @SuppressWarnings("unchecked")
            public <T  extends ConfigBeanProxy> T transform(T source) {
                    final ConfigView handler = (ConfigView) Proxy.getInvocationHandler(source);
                    return (T) handler.getMasterView().getProxy(handler.getMasterView().getProxyType());

            }
        };

        return rawTransformer.transform(s);
    }

    public GlassFishConfigBean(ServiceLocator habitat, DomDocument document, GlassFishConfigBean parent, ConfigModel model, XMLStreamReader in) {
        super(habitat, document, parent, model, in);                
    }

    public GlassFishConfigBean(Dom source, Dom parent) {
        super(source, parent);
    }

    @Override
    public <T extends ConfigBeanProxy> T createProxy(Class<T> proxyType) {
        TranslatedConfigView.setHabitat(getServiceLocator());
        if (defaultView==null) {
            defaultView = new TranslatedConfigView(this);
        }
        return defaultView.getProxy(proxyType);
    }

    /**
     * Returns a copy of itself
     *
     * @return a copy of itself.
     */
    @Override
    protected <T extends Dom> T copy(T parent) {
        return (T) new GlassFishConfigBean(this, parent);
    }


    @Override
    public void initializationCompleted() {
        super.initializationCompleted();
        //System.out.println( "GlassFishConfigBean.initializationCompleted() for " + getProxyType().getName() );
        for (ConfigBeanListener listener : getServiceLocator().<ConfigBeanListener>getAllServices(ConfigBeanListener.class)) {
            listener.onEntered(this);
        }
    }
    
    public String toString() {
        //final Set<String> attrNames = getAttributeNames();
        return "GlassFishConfigBean." + getProxyType().getName();
    }
}









