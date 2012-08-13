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

package com.sun.enterprise.iiop.security;

import java.lang.ref.WeakReference;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * This class is a local utility class to provide for hk2 lookups during runtime.
 * @author Sudarsan Sridhar
 */
public class Lookups {

    @Inject
    private Provider<SecurityMechanismSelector> securityMechanismSelectorProvider;
    
    @Inject
    private Provider<GlassFishORBHelper> glassFishORBHelperProvider;

    @Inject
    private Provider<SecurityContextUtil> securityContextUtilProvider;

    @Inject
    private Provider<GMSAdapterService> gmsAdapterServiceProvider;

    /**
     * Static singleton {@link Habitat} instance.
     */
    private static final ServiceLocator habitat = Globals.getDefaultHabitat();

    /**
     * Static singleton {@link Lookups} instance.  Note that this is assigned lazily and may
     * remain null if the {@link Habitat} can not be obtained.
     */
    private static Lookups singleton;

    private static WeakReference<SecurityMechanismSelector> sms = new WeakReference<SecurityMechanismSelector>(null);
    private static WeakReference<GlassFishORBHelper> orb = new WeakReference<GlassFishORBHelper>(null);
    private static WeakReference<SecurityContextUtil> sc = new WeakReference<SecurityContextUtil>(null);


    private Lookups() {
    }

    /**
     * Check to see if the singleton {@link Lookups} reference has been assigned. If null,
     * then attempt to obtain and assign the singleton {@link Lookups} instance.
     *
     * @return true if the singleton instance has been successfully assigned; false otherwise
     */
    private static synchronized boolean checkSingleton(){
        if (singleton == null && habitat != null) {
            // Obtaining the singleton through the habitat will cause the injections to occur.
            singleton = habitat.create(Lookups.class);
            habitat.inject(singleton);
            habitat.postConstruct(singleton);
        }
        return singleton != null;
    }

    /**
     * Get the {@link SecurityMechanismSelector}.
     *
     * @return the {@link SecurityMechanismSelector}; null if not available
     */
    static SecurityMechanismSelector getSecurityMechanismSelector() {
        if (sms.get() != null) {
            return sms.get();
        }
        return _getSecurityMechanismSelector();
    }

    private static synchronized SecurityMechanismSelector _getSecurityMechanismSelector() {
        if (sms.get() == null && checkSingleton()) {
            sms = new WeakReference<SecurityMechanismSelector>(singleton.securityMechanismSelectorProvider.get());
        }
        return sms.get();
    }

    /**
     * Get the {@link GlassFishORBHelper}.
     *
     * @return the {@link GlassFishORBHelper}; null if not available
     */
    static GlassFishORBHelper getGlassFishORBHelper() {
        if (orb.get() != null) {
            return orb.get();
        }
        return _getGlassFishORBHelper();
    }

    private static synchronized GlassFishORBHelper _getGlassFishORBHelper() {
        if (orb.get() == null && checkSingleton()) {
            orb = new WeakReference<GlassFishORBHelper>(singleton.glassFishORBHelperProvider.get());
        }
        return orb.get();
    }

    /**
     * Get the {@link SecurityContextUtil}.
     *
     * @return the {@link SecurityContextUtil}; null if not available
     */
    static SecurityContextUtil getSecurityContextUtil() {
        if (sc.get() != null) {
            return sc.get();
        }
        return _getSecurityContextUtil();
    }

    private static synchronized SecurityContextUtil _getSecurityContextUtil() {
        if (sc.get() == null && checkSingleton()) {
            sc = new WeakReference<SecurityContextUtil>(singleton.securityContextUtilProvider.get());
        }
        return sc.get();
    }

    /**
     * Get the {@link GMSAdapterService}.
     *
     * @return the {@link GMSAdapterService}; null if not available
     */
    static GMSAdapterService getGMSAdapterService() {
        return checkSingleton() ? singleton.gmsAdapterServiceProvider.get() : null;
    }
}
