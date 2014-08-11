/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jvnet.hk2.annotations.Service;

/** Tooling for AdminCache {@link DataProvider} implementation.
 *
 * @author mmares
 */
@Service
public class AdminCacheUtils {
    
    private static final AdminCacheUtils instance = new AdminCacheUtils();
    
    private final Map<Class, DataProvider> providers = new HashMap<Class, DataProvider>();
    private final Pattern keyPattern = Pattern.compile("([-_.a-zA-Z0-9]+/?)+");
    //private final ServiceLoader<DataProvider> dataProviderLoader = ServiceLoader.<DataProvider>load(DataProvider.class);
    
    private static final DataProvider[] allProviders = new DataProvider[]{
        new StringDataProvider(), 
        new ByteArrayDataProvider(),
        new CommandModelDataProvider()
    };
    
    private AdminCacheUtils() {
    }
    
    public DataProvider getProvider(final Class clazz) {
        DataProvider result = providers.get(clazz);
        if (result == null) {
            //Use hardcoded data providers - fastest and not problematic
            for (DataProvider provider : allProviders) {
                if (provider.accept(clazz)) {
                    providers.put(clazz, provider);
                    return provider;
                }
            }
//            ServiceLocator habitat = Globals.getDefaultHabitat();
//            if (habitat != null) {
//                List<DataProvider> allServices = habitat.getAllServices(DataProvider.class);
//                for (DataProvider provider : allServices) {
//                    if (provider.accept(clazz)) {
//                        providers.put(clazz, provider);
//                        return provider;
//                    }
//                }
//            }
//            for (DataProvider provider : dataProviderLoader) {
//                if (provider.accept(clazz)) {
//                    providers.put(clazz, provider);
//                    return provider;
//                }
//            }
            
            return null;
        } else {
            return result;
        }
    }
    
    public final boolean validateKey(final String key) {
        return keyPattern.matcher(key).matches();
    }
    
    /** Return preferred {@link AdminCache}
     */
    public static AdminCache getCache() {
        return AdminCacheMemStore.getInstance();
    }
    
    public static AdminCacheUtils getInstance() {
        return instance;
    }
    
}
