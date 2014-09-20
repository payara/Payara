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

package com.sun.enterprise.security;

import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;

/**
 *
 * @author nithyasubramanian
 */
@AMXMetadata(type="web-security-deployer-mon", group="monitoring", isSingleton=false)
@ManagedObject
@Description( "Web application Security Deployment statistics" )
public class WebSecurityDeployerStatsProvider {
    
    //Commenting the TimeStatistics to be implemented later

   /* TimeStatisticImpl deploymentTime = new TimeStatisticImpl(0, 0, 0, 0, "DeploymentTime", "milliseconds", "Deployment Time", 0, 0);

    TimeStatisticImpl generationTime = new TimeStatisticImpl(0, 0, 0, 0, "GenerationTime", "milliseconds", "Generation Time", 0, 0);

    TimeStatisticImpl undeploymentTime = new TimeStatisticImpl(0, 0, 0, 0, "UndeploymentTime", "milliseconds", "Undeployment Time", 0, 0);

    TimeStatisticImpl removalTime = new TimeStatisticImpl(0, 0, 0, 0, "RemovalTime", "milliseconds", "Removal Time", 0, 0);*/

    CountStatisticImpl secMgrCount = new CountStatisticImpl("WebSecurityManagerCount", "count", "No of Web security managers");

    CountStatisticImpl policyConfCount= new CountStatisticImpl("WebPolicyConfigurationCount", "count", "No of Policy Configuration Objects");

 /*   @ManagedAttribute(id="depolymenttime")
    public TimeStatistic getDeploymentTime() {
        return deploymentTime.getStatistic();
    }

    @ManagedAttribute(id="generationtime")
    public TimeStatistic getGenerationTime() {
        return generationTime.getStatistic();
    }

    @ManagedAttribute(id="undepolymenttime")
    public TimeStatistic getUndeploymentTime() {
        return undeploymentTime.getStatistic();
    }


    @ManagedAttribute(id="removaltime")
    public TimeStatistic getRemovalTime() {
        return removalTime.getStatistic();
    }*/

    @ManagedAttribute(id="websecuritymanagercount")
    public CountStatistic getWebSMCount() {
        return secMgrCount.getStatistic();

    }

    @ManagedAttribute(id="webpolicyconfigurationcount")
    public CountStatistic getPCCount() {
        return policyConfCount.getStatistic();
    }
/*   @ProbeListener("glassfish:core:web:webDeploymentStartedEvent")
    public void webDeploymentStartedEvent(@ProbeParam("appName")String appName){
       deploymentTime.setStartTime(System.currentTimeMillis());
    }

    @ProbeListener("glassfish:core:web:webDeploymentEndedEvent")
    public void webDeploymentEndedEvent(@ProbeParam("appName")String appName){

    }*/

    @ProbeListener("glassfish:security:web:securityManagerCreationEvent")
    public void securityManagerCreationEvent(
            @ProbeParam("appName") String appName) {
        secMgrCount.increment();
    }

    @ProbeListener("glassfish:security:web:securityManagerDestructionEvent")
    public void securityManagerDestructionEvent(
            @ProbeParam("appName") String appName) {
        secMgrCount.decrement();
    }

    @ProbeListener("glassfish:security:web:policyCreationEvent")
    public void policyConfigurationCreationEvent(
            @ProbeParam("contextId") String contextId) {
        policyConfCount.increment();
    }

    @ProbeListener("glassfish:security:web:policyDestructionEvent")
    public void policyConfigurationDestructionEvent(
            @ProbeParam("contextId") String contextId) {
        policyConfCount.decrement();
    }





}
