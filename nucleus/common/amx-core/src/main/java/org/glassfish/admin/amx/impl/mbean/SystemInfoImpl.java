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
package org.glassfish.admin.amx.impl.mbean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.management.MBeanServer;
import org.glassfish.admin.amx.base.SystemInfo;
import org.glassfish.external.amx.AMXGlassfish;

/**
Loaded as MBean "amx:j2eeType=X-SystemInfo,name=na"
 */
public final class SystemInfoImpl extends AMXImplBase //implements SystemInfo
{
    private final ConcurrentMap<String, Boolean> mFeatures;

    public SystemInfoImpl(
            final MBeanServer server) {

        super(AMXGlassfish.DEFAULT.domainRoot(), SystemInfo.class);

        // must be thread-safe, because features can be added at a later time
        mFeatures = new ConcurrentHashMap<String, Boolean>();
    }

    /**
    Advertise the presence of a feature.  For consistency, feature names should normally be
    of the form <description>_FEATURE.  For example: "HELLO-WORLD_FEATURE".
    <p>
    To change a feature�s availability to unavailable, pass 'false' for 'available' (there is no
    removeFeature() call).  This is discouraged unless dynamic presence/absence is an inherent
    characteristic of the feature; clients might check only once for presence or absence.

    @param featureName name of the feature
    @param available  should be 'true' unless an explicit 'false' (unavailable) is desired
     */
    public void addFeature(final String featureName, final boolean available) {
        if (featureName == null || featureName.length() == 0) {
            throw new IllegalArgumentException();
        }

        mFeatures.put(featureName, Boolean.valueOf(available));
    }

    public String[] getFeatureNames() {
        // make a copy so that we can reliably call List.size()
        // According to Brian Goetz, this approach is thread safe for using the keySet.
        final List<String> nameList = new ArrayList<String>(mFeatures.keySet());

        final String[] names = new String[nameList.size()];
        nameList.toArray(names);
        return names;
    }

    public boolean supportsFeature(final String key) {
        boolean supports = false;

        Boolean result = mFeatures.get(key);
        if (result == null) {
            result = Boolean.FALSE;
        }

        return (result);
    }

    /**
    Return a Map keyed by an arbitrary String denoting some feature.  The value
    is the time in milliseconds.  Code should not rely on the keys as they are subject to
    changes, additions, or removal at any time, except as otherwise documented.
    Even documented items should be used only for informational purposes,
    such as assessing performance.
     */
    public Map<String, Long> getPerformanceMillis() {
        return new HashMap<String, Long>();
    }
}
