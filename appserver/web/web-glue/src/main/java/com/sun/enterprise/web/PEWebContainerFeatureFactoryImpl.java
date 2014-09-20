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

package com.sun.enterprise.web;

import com.sun.enterprise.web.pluggable.WebContainerFeatureFactory;
import org.jvnet.hk2.annotations.Service;

/**
 * Implementation of WebContainerFeatureFactory which returns web container
 * feature implementations for PE.
 */
@Service(name="pe")
public class PEWebContainerFeatureFactoryImpl
        implements WebContainerFeatureFactory {
        
    @Override
    public WebContainerStartStopOperation getWebContainerStartStopOperation() {
        return new PEWebContainerStartStopOperation();
    }
    
    @Override
    public HealthChecker getHADBHealthChecker(WebContainer webContainer) {
        return new PEHADBHealthChecker(webContainer);
    }
    
    @Override
    public ReplicationReceiver getReplicationReceiver(EmbeddedWebContainer embedded) {
        return new PEReplicationReceiver(embedded);
    }    
    
    @Override
    public SSOFactory getSSOFactory() {
        return new PESSOFactory();
    }    

    @Override
    public VirtualServer getVirtualServer() {
        return new VirtualServer();
    }
    
    @Override
    public String getSSLImplementationName(){
        return null;
    }

    /**
     * Gets the default access log file prefix.
     *
     * @return The default access log file prefix
     */
    @Override
    public String getDefaultAccessLogPrefix() {
        return "_access_log.";
    }

    /**
     * Gets the default access log file suffix.
     *
     * @return The default access log file suffix
     */
    @Override
    public String getDefaultAccessLogSuffix() {
        return ".txt";
    }

    /**
     * Gets the default datestamp pattern to be applied to access log files.
     *
     * @return The default datestamp pattern to be applied to access log files
     */
    @Override
    public String getDefaultAccessLogDateStampPattern() {
        return "yyyy-MM-dd";
    }

    /**
     * Returns true if the first access log file and all subsequently rotated
     * ones are supposed to be date-stamped, and false if datestamp is to be
     * added only starting with the first rotation.
     *
     * @return true if first access log file and all subsequently rotated
     * ones are supposed to be date-stamped, and false if datestamp is to be
     * added only starting with the first rotation. 
     */
    @Override
    public boolean getAddDateStampToFirstAccessLogFile() {
        return true;
    }

    /**
     * Gets the default rotation interval in minutes.
     *
     * @return The default rotation interval in minutes
     */
    @Override
    public int getDefaultRotationIntervalInMinutes() {
        return 15;
    }
}
