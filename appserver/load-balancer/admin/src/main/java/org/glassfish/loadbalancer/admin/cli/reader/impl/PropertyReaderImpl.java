/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loadbalancer.admin.cli.reader.impl;

import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.PropertyVisitor;

import org.glassfish.loadbalancer.admin.cli.reader.api.PropertyReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;

import org.glassfish.loadbalancer.config.LbConfig;

import java.util.Iterator;
import java.util.Properties;
import org.jvnet.hk2.config.types.Property;

/**
 * Provides property information relavant to Load balancer tier.
 *
 * @author Kshitiz Saxena
 */
public class PropertyReaderImpl implements PropertyReader {

    public static PropertyReader[] getPropertyReaders(Properties properties) {
        if (properties == null) {
            properties = new Properties();
        }

        if (properties.getProperty(
                LoadbalancerReader.ACTIVE_HEALTH_CHECK) == null) {
            properties.setProperty(
                    LoadbalancerReader.ACTIVE_HEALTH_CHECK,
                    LoadbalancerReader.ACTIVE_HEALTH_CHECK_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.NUM_HEALTH_CHECK) == null) {
            properties.setProperty(
                    LoadbalancerReader.NUM_HEALTH_CHECK,
                    LoadbalancerReader.NUM_HEALTH_CHECK_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.REWRITE_LOCATION) == null) {
            properties.setProperty(
                    LoadbalancerReader.REWRITE_LOCATION,
                    LoadbalancerReader.REWRITE_LOCATION_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.REWRITE_COOKIES) == null) {
            properties.setProperty(
                    LoadbalancerReader.REWRITE_COOKIES,
                    LoadbalancerReader.REWRITE_COOKIES_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.RESP_TIMEOUT) == null) {
            properties.setProperty(
                    LoadbalancerReader.RESP_TIMEOUT,
                    LoadbalancerReader.RESP_TIMEOUT_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.RELOAD_INTERVAL) == null) {
            properties.setProperty(
                    LoadbalancerReader.RELOAD_INTERVAL,
                    LoadbalancerReader.RELOAD_INTERVAL_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.HTTPS_ROUTING) == null) {
            properties.setProperty(
                    LoadbalancerReader.HTTPS_ROUTING,
                    LoadbalancerReader.HTTPS_ROUTING_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.REQ_MONITOR_DATA) == null) {
            properties.setProperty(
                    LoadbalancerReader.REQ_MONITOR_DATA,
                    LoadbalancerReader.REQ_MONITOR_DATA_VALUE);
        }

        if (properties.getProperty(
                LoadbalancerReader.PREFERRED_FAILOVER_INSTANCE) == null) {
            properties.setProperty(
                    LoadbalancerReader.PREFERRED_FAILOVER_INSTANCE,
                    LoadbalancerReader.PREFERRED_FAILOVER_INSTANCE_VALUE);
        }

        int i = 0;
        int propSize = properties.size();
        PropertyReaderImpl[] props = new PropertyReaderImpl[propSize];


        Iterator iter = properties.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            props[i++] = new PropertyReaderImpl(key,
                    properties.getProperty(key));
        }
        return props;
    }

    public static PropertyReader[] getPropertyReaders(LbConfig _lbConfig) {
        Properties properties = new Properties();
        properties.setProperty(LoadbalancerReader.HTTPS_ROUTING, _lbConfig.getHttpsRouting());
        properties.setProperty(LoadbalancerReader.REQ_MONITOR_DATA, _lbConfig.getMonitoringEnabled());
        properties.setProperty(LoadbalancerReader.RELOAD_INTERVAL, _lbConfig.getReloadPollIntervalInSeconds());
        properties.setProperty(LoadbalancerReader.RESP_TIMEOUT, _lbConfig.getResponseTimeoutInSeconds());
        Iterator<Property> propertyList = _lbConfig.getProperty().iterator();
        while(propertyList.hasNext()){
            Property property = propertyList.next();
            if(property.getName().equals(LbConfig.LAST_APPLIED_PROPERTY) ||
                    property.getName().equals(LbConfig.LAST_EXPORTED_PROPERTY)){
                continue;
            }
            properties.setProperty(property.getName(), property.getValue());
        }
        return getPropertyReaders(properties);
    }

    // --- CTOR METHOD ------
    private PropertyReaderImpl(String name, String value) {
        _name = name;
        _value = value;
    }

    // -- READER IMPLEMENTATION ----
    /**
     * Returns name of the property
     *
     * @return String           name of the property
     */
    @Override
    public String getName() throws LbReaderException {
        return _name;
    }

    /**
     * Returns value of the property
     *
     * @return String           name of the value
     */
    @Override
    public String getValue() throws LbReaderException {
        return _value;
    }

    /**
     * Returns description of the property
     *
     * @return String           description of the property
     */
    @Override
    public String getDescription() throws LbReaderException {
        return _description;
    }

    // --- VISITOR IMPLEMENTATION ---
    @Override
    public void accept(Visitor v) throws Exception {
		if (v instanceof PropertyVisitor) {
			PropertyVisitor pv = (PropertyVisitor) v;
			pv.visit(this);
		}
    }

    // -- PRIVATE VARS ---
    private String _value = null;
    private String _name = null;
    private String _description = null;
}
