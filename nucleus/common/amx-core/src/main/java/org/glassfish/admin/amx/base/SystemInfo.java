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

package org.glassfish.admin.amx.base;

import java.util.Map;

import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
Provides information about the capabilities of the running server.
Callers should check only for specific capabilities, never whether
the server is PE/SE/EE, since the feature assortment could vary with
release.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true)
public interface SystemInfo extends AMXProxy, Singleton, Utility
{
    /**
    Call supportsFeature() with this value to determine if the server
    supports clusters.
     */
    public final String CLUSTERS_FEATURE = "SupportsClusters";

    /**
    Call supportsFeature() with this value to determine if the server
    supports more than one server.
     */
    public final String MULTIPLE_SERVERS_FEATURE = "SupportsMultipleServers";

    /**
    Call supportsFeature() with this value to determine if this MBean
    is running in the Domain Admin Server.
     */
    public final String RUNNING_IN_DAS_FEATURE = "RunningInDomainAdminServer";

    /**
    Call supportsFeature() with this value to determine if the
    high availability feature (HADB) is available.
     */
    public final String HADB_CONFIG_FEATURE = "HighAvailabilityDatabase";

    /**
    Query whether a feature is supported.  Features require the use
    of a key, which may be any of:
    <ul>
    <li>#CLUSTERS_FEATURE</li>
    <li>#MULTIPLE_SERVERS_FEATURE</li>
    <li>#RUNNING_IN_DAS_FEATURE</li>
    <li>any dynamically-added feature (see {@link #getFeatureNames}) </li>
    </ul>

    Other features might also be added dynamically, see {@link #getFeatureNames}.

    @param key	the feature name to query
     */
    public boolean supportsFeature(String key);

    /**
    Return all features names.

    @return Set
     */
    @ManagedAttribute
    public String[] getFeatureNames();

    /**
    Key for time for server to complete its startup sequence.  The presence of this item
    in the Map returned by {@link #getPerformanceMillis} indicates that the server has
    completed its startup sequence. However, some server features might still be initializing
    asynchronously, or might be lazily loaded.
    @see #getPerformanceMillis
     */
    public static final String STARTUP_SEQUENCE_MILLIS_KEY = "StartupMillis";

    /**
    Return a Map keyed by an arbitrary String denoting some feature.  The value
    is the time in milliseconds.  Code should not rely on the keys as they are subject to
    changes, additions, or removal at any time, except as otherwise documented.
    Even documented items should be used only for informational purposes,
    such as assessing performance.

    @return Map<String,Long>
     */
    @ManagedAttribute
    public Map<String, Long> getPerformanceMillis();

}


