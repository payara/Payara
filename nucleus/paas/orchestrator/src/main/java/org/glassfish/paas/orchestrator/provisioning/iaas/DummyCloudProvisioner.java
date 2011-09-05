/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.provisioning.iaas;

import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
@Service
public class DummyCloudProvisioner implements CloudProvisioner {

    private static Logger logger = Logger.getLogger(DummyCloudProvisioner.class.getName());

    Properties cloudConfiguration;

    private static int id = 1;

    public void terminateAll() {
    }

    public String createInstance(String imageId) {
        logger.entering(getClass().getName(), "createInstance");
        return Integer.toString(id++);
    }

    public String createMasterInstance() {
        logger.entering(getClass().getName(), "createMasterInstance");
        return Integer.toString(id++);
    }

    public List<String> createSlaveInstances(int count) {
        ArrayList<String> instances = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            instances.add(Integer.toString(id++));
        }
        logger.entering(getClass().getName(), "createSlaveInstances");
        return instances;
    }

    public void deleteInstances(List<String> instanceIDs) {
        logger.entering(getClass().getName(), "deleteInstances");
    }

    public void startInstances(Map<String, String> instances) {
        logger.entering(getClass().getName(), "startInstances");
    }

    public void stopInstances(Collection<String> instances) {
        logger.entering(getClass().getName(), "stopInstances");
    }

    public void describeInstances() {
        logger.entering(getClass().getName(), "describeInstances");
    }

    public String getIPAddress(String instanceId) {
        return "127.0.0.1";
    }

    public void uploadCredentials(String ipAddress) {
        logger.entering(getClass().getName(), "uploadCredentials");
    }

    public boolean handles(Properties metaData) {
        String provider = metaData.getProperty("CLOUD_PROVIDER");
        if (provider == null || provider.length() == 0 || "dummy".equalsIgnoreCase(provider)) {
            return true;
        }
        return false;
    }

    public void initialize(Properties properties) {
        logger.entering(getClass().getName(), "initialize");
        this.cloudConfiguration = properties;
    }

    public void terminateInstancesAndIps(Map<String, String> instanceIds) {
        logger.entering(getClass().getName(), "terminateInstancesAndIps");
    }
}
