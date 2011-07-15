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
import com.sun.appserv.management.config.SystemPropertiesAccess;
import com.sun.appserv.management.config.SystemPropertyConfig;
import org.glassfish.admin.amxtest.AMXTestBase;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

/**
 */
public final class SystemPropertiesAccessTest
        extends AMXTestBase {
    public SystemPropertiesAccessTest() {
    }

    private Set<ObjectName>
    getAll()
            throws Exception {
        final Set<ObjectName> objectNames =
                getQueryMgr().queryInterfaceObjectNameSet(
                        SystemPropertiesAccess.class.getName(), null);

        return (objectNames);
    }


    private void
    checkPropertiesGet(final SystemPropertiesAccess props) {
        final Map<String, SystemPropertyConfig> all = props.getSystemPropertyConfigMap();

        for (final SystemPropertyConfig prop : all.values() ) {
            final String value = prop.getValue();
        }
    }

    private void
    testPropertiesSetToSameValue(final SystemPropertiesAccess props) {
        final Map<String, SystemPropertyConfig> all = props.getSystemPropertyConfigMap();

        // get each property, set it to the same value, the verify
        // it's the same.
        for ( final SystemPropertyConfig prop : all.values() ) {

            final String value = prop.getValue();
            prop.setValue(value);

            assert prop.getValue().equals(value);
        }
    }

    private void
    testCreateEmptySystemProperty(final SystemPropertiesAccess props) {
        final String NAME = "test.empty";

        props.createSystemPropertyConfig(NAME, "");
        assert props.getSystemPropertyConfigMap().get(NAME) != null;
        props.removeSystemPropertyConfig(NAME);
        assert props.getSystemPropertyConfigMap().get(NAME) == null;
    }

    private void
    testSystemPropertiesCreateRemove(final SystemPropertiesAccess props) {
        final Map<String, SystemPropertyConfig> all = props.getSystemPropertyConfigMap();

        // add some properties, then delete them
        final int numToAdd = 1;
        final long now = System.currentTimeMillis();
        for (int i = 0; i < numToAdd; ++i) {
            final String testName = "__junittest_" + i + now;

            if ( all.get(testName) != null) {
                failure("test property already exists: " + testName);
            }

            props.createSystemPropertyConfig(testName, "value_" + i);
            assert props.getSystemPropertyConfigMap().get(testName) != null;
        }
        final int numProps = props.getSystemPropertyConfigMap().keySet().size();

        if (numProps != numToAdd + all.keySet().size() ) {
            failure("expecting " + numProps + " have " + numToAdd + all.keySet().size());
        }

        // remove the ones we added
        for (int i = 0; i < numToAdd; ++i) {
            final String testName = "__junittest_" + i + now;

            props.removeSystemPropertyConfig(testName);
            assert props.getSystemPropertyConfigMap().get(testName) == null;
        }

        assert (props.getSystemPropertyConfigMap().keySet().size() == all.keySet().size() );

    }

    public synchronized void
    checkGetProperties(final ObjectName src)
            throws Exception {
        final AMX proxy = getProxy(src, AMX.class);

        if (!(proxy instanceof SystemPropertiesAccess)) {
            throw new IllegalArgumentException(
                    "MBean does not implement SystemPropertiesAccess: " + quote(src));
        }

        final SystemPropertiesAccess props = (SystemPropertiesAccess) proxy;
        checkPropertiesGet(props);
    }

    public void
    checkSetPropertiesSetToSameValue(final ObjectName src)
            throws Exception {
        final SystemPropertiesAccess props = getProxy(src, SystemPropertiesAccess.class);

        testPropertiesSetToSameValue(props);
    }


    public void
    checkCreateRemove(final ObjectName src)
            throws Exception {
        final SystemPropertiesAccess props =
                getProxy(src, SystemPropertiesAccess.class);

        testSystemPropertiesCreateRemove(props);
    }

    public synchronized void
    testPropertiesGet()
            throws Exception {
        final Set<ObjectName> all = getAll();

        testAll(all, "checkGetProperties");
    }

    public synchronized void
    testPropertiesSetToSameValue()
            throws Exception {
        final Set<ObjectName> all = getAll();

        testAll(all, "checkSetPropertiesSetToSameValue");
    }


    public synchronized void
    testCreateRemove()
            throws Exception {
        if (checkNotOffline("testCreateRemove")) {
            final Set<ObjectName> all = getAll();
            testAll(all, "checkCreateRemove");
        }
    }

}


