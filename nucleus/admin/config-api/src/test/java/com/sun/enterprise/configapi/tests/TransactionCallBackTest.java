/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2015 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.configapi.tests;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionCallBack;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.WriteableView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Jerome Dochez
 * Date: Mar 28, 2008
 * Time: 4:23:31 PM
 */
public class TransactionCallBackTest extends ConfigPersistence {

    ServiceLocator habitat = Utils.instance.getHabitat(this);

    /**
     * Returns the file name without the .xml extension to load the test configuration
     * from. By default, it's the name of the TestClass.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }

    @Override
    public ServiceLocator getBaseServiceLocator() {
        return habitat;
    }
    
    @Override
    public ServiceLocator getHabitat() {
    	return getBaseServiceLocator();
    }
    
    public void doTest() throws TransactionFailure {
        ConfigBean serviceBean = (ConfigBean) ConfigBean.unwrap(habitat.<NetworkListeners>getService(NetworkListeners.class));
        Map<String, String> configChanges = new HashMap<String, String>();
        configChanges.put("name", "funky-listener");

        ConfigSupport.createAndSet(serviceBean, NetworkListener.class, configChanges,
                new TransactionCallBack<WriteableView>() {
                    @SuppressWarnings({"unchecked"})
                    public void performOn(WriteableView param) throws TransactionFailure {
                        // if you know the type...
                        NetworkListener listener = param.getProxy(NetworkListener.class);
                        listener.setName("Aleksey");
                        // if you don't know the type
                        Method m;
                        try {
                            m = param.getProxyType().getMethod("setAddress", String.class);
                            m.invoke(param.getProxy(param.getProxyType()), "localhost");
                        } catch (NoSuchMethodException e) {
                            throw new TransactionFailure("Cannot find getProperty method", e);
                        } catch (IllegalAccessException e) {
                            throw new TransactionFailure("Cannot call getProperty method", e);
                        } catch (InvocationTargetException e) {
                            throw new TransactionFailure("Cannot call getProperty method", e);
                        }
                    }
                });
    }

    public boolean assertResult(String s) {
        return s.contains("Aleksey") && s.contains("localhost");
    }    
}
