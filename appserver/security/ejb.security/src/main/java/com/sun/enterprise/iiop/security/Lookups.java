/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.component.Habitat;

/**
 * This class is a local utility class to provide for hk2 lookups during runtime.
 * @author Sudarsan Sridhar
 */
class Lookups {

    private static WeakReference<SecurityMechanismSelector> sms = new WeakReference<SecurityMechanismSelector>(null);
    private static WeakReference<GlassFishORBHelper> orb = new WeakReference<GlassFishORBHelper>(null);
    private static WeakReference<SecurityContextUtil> sc = new WeakReference<SecurityContextUtil>(null);
    
    

    static SecurityMechanismSelector getSecurityMechanismSelector(Habitat habitat) {
        if (sms.get() != null) {
            return sms.get();
        }
        return _getSecurityMechanismSelector(habitat);
    }

    private static synchronized SecurityMechanismSelector _getSecurityMechanismSelector(Habitat habitat) {
        if (sms.get() == null) {
            sms = new WeakReference<SecurityMechanismSelector>(habitat.getComponent(SecurityMechanismSelector.class));
        }
        return sms.get();
    }

    static GlassFishORBHelper getGlassFishORBHelper(Habitat habitat) {
        if (orb.get() != null) {
            return orb.get();
        }
        return _getGlassFishORBHelper(habitat);
    }

    private static synchronized GlassFishORBHelper _getGlassFishORBHelper(Habitat habitat) {
        if (orb.get() == null) {
            orb = new WeakReference<GlassFishORBHelper>(habitat.getComponent(GlassFishORBHelper.class));
        }
        return orb.get();
    }

    static SecurityContextUtil getSecurityContextUtil(Habitat habitat) {
        if (sc.get() != null) {
            return sc.get();
        }
        return _getSecurityContextUtil(habitat);
    }

    private static synchronized SecurityContextUtil _getSecurityContextUtil(Habitat habitat) {
        if (sc.get() == null) {
            sc = new WeakReference<SecurityContextUtil>(habitat.getComponent(SecurityContextUtil.class));
        }
        return sc.get();
    }
}
