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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.PropertiesAccess;
import com.sun.appserv.management.config.PropertyConfig;
import com.sun.appserv.management.util.misc.GSetUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.TestUtil;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

/**
 */
public final class PropertiesAccessTest
        extends AMXTestBase {
    public PropertiesAccessTest() {
    }

    private Set<ObjectName>
    getAllImplementorsOfProperties()
            throws Exception {
        final Set<AMX> amxs = getQueryMgr().queryInterfaceSet(
                PropertiesAccess.class.getName(), null);

        return (TestUtil.newSortedSet(Util.toObjectNames(amxs)));

    }


    private void
    testCreateEmptyProperty(final PropertiesAccess props) {
        final String NAME = "test.empty";

        final PropertyConfig pc = props.createPropertyConfig(NAME, "");
        assert props.getPropertyConfigMap().get(NAME) != null;
        props.removePropertyConfig(NAME);
        assert props.getPropertyConfigMap().get(NAME) == null;
    }

    private void
    testPropertiesGet(final PropertiesAccess props) {
        final Map<String, PropertyConfig> all = props.getPropertyConfigMap();

        for (final PropertyConfig prop : all.values() ) {
            final String name = prop.getName(); 
            final String value = prop.getValue();
        }
    }

    private void
    testPropertiesSetToSameValue(final PropertiesAccess props) {
        final Map<String, PropertyConfig> all = props.getPropertyConfigMap();

        // get each property, set it to the same value, the verify
        // it's the same.
        for ( final PropertyConfig prop : all.values() ) {
            
            final String value = prop.getValue();
            prop.setValue(value);

            assert (prop.getValue().equals(value));
        }
    }

    /**
     Adding or removing test properties to these types does not
     cause any side effects.  Plus, there is no need to test
     every MBean.
     */
    private static final Set<String> TEST_CREATE_REMOVE_TYPES =
            GSetUtil.newUnmodifiableStringSet(
                    XTypes.DOMAIN_CONFIG,
                    XTypes.CONFIG_CONFIG,
                    XTypes.PROFILER_CONFIG,
                    XTypes.STANDALONE_SERVER_CONFIG,
                    XTypes.CLUSTERED_SERVER_CONFIG,
                    XTypes.ORB_CONFIG,
                    XTypes.MODULE_MONITORING_LEVELS_CONFIG,
                    XTypes.NODE_AGENT_CONFIG
            );

    private void
    testPropertiesCreateRemove(final PropertiesAccess props) {

        final AMX amx = Util.asAMX(props);
        final String j2eeType = amx.getJ2EEType();
        if (!TEST_CREATE_REMOVE_TYPES.contains(j2eeType)) {
            return;
        }

        final Map<String, PropertyConfig> startProps = props.getPropertyConfigMap();
        // add some properties, then delete them
        final int numToAdd = 1;
        final long now = System.currentTimeMillis();
        for (int i = 0; i < numToAdd; ++i) {
            final String testName = "__junittest_" + i + now;

            if (props.getPropertyConfigMap().get(testName) != null) {
                failure("test property already exists: " + testName);
            }

            props.createPropertyConfig(testName, "value_" + i);
            assert (props.getPropertyConfigMap().get(testName) != null);
        }
        final int numProps = props.getPropertyConfigMap().keySet().size();

        if (numProps != numToAdd + startProps.keySet().size() ) {
            failure("expecting " + numProps + " have " + numToAdd + startProps.keySet().size());
        }

        // remove the ones we added
        for (int i = 0; i < numToAdd; ++i) {
            final String testName = "__junittest_" + i + now;

            props.removePropertyConfig(testName);
            assert props.getPropertyConfigMap().get(testName) == null;
        }

        assert (props.getPropertyConfigMap().size() == startProps.keySet().size() );
    }

    public void
    checkGetProperties(final ObjectName src)
            throws Exception {
        final AMX proxy = getProxy(src);

        if (!(proxy instanceof PropertiesAccess)) {
            throw new IllegalArgumentException(
                    "MBean does not implement PropertiesAccess: " + quote(src));
        }

        final PropertiesAccess props = (PropertiesAccess) proxy;
        testPropertiesGet(props);
    }

    public void
    checkSetPropertiesSetToSameValue(final ObjectName src)
            throws Exception {
        final PropertiesAccess props = (PropertiesAccess) getProxy(src);

        testPropertiesSetToSameValue(props);
    }


    public void
    checkCreateRemove(final ObjectName src)
            throws Exception {
        final PropertiesAccess props = (PropertiesAccess) getProxy(src);

        testPropertiesCreateRemove(props);
    }

    public synchronized void
    testPropertiesGet()
            throws Exception {
        final Set<ObjectName> all = getAllImplementorsOfProperties();

        testAll(all, "checkGetProperties");
    }

    public synchronized void
    testPropertiesSetToSameValue()
            throws Exception {
        final Set<ObjectName> all = getAllImplementorsOfProperties();

        testAll(all, "checkSetPropertiesSetToSameValue");
    }


    public synchronized void
    testPropertiesCreateRemove()
            throws Exception {
        if (checkNotOffline("testPropertiesCreateRemove")) {
            final Set<ObjectName> all = getAllImplementorsOfProperties();

            testAll(all, "checkCreateRemove");
		}
	}
	
}


