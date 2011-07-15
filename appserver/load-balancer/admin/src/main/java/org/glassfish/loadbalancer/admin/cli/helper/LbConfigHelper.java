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

package org.glassfish.loadbalancer.admin.cli.helper;

import java.io.OutputStream;
import java.util.Date;

import com.sun.enterprise.config.serverbeans.LbConfig;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.LoadBalancer;

import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;
import org.glassfish.loadbalancer.admin.cli.reader.impl.LoadbalancerReaderImpl;
import org.glassfish.loadbalancer.admin.cli.transform.LoadbalancerVisitor;

import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;
import org.glassfish.loadbalancer.admin.cli.beans.Loadbalancer;

/**
 * Export support class
 * 
 * @author Kshitiz Saxena
 */
public class LbConfigHelper {

    /**
     * exports the loadbalancer.xml from the config to the outputstream provided
     * @param ctx ConfigContext
     * @param lbConfigName name of lb-config 
     * @param out OutputStream into which the loadbalancer.xml is written
     */
    public static LoadbalancerReader getLbReader(Domain domain, ApplicationRegistry appRegistry,
            String lbConfigName) throws Exception {
        // reads the load balancer related data
        LbConfig lbConfig = domain.getLbConfigs().getLbConfig(lbConfigName);
        if (lbConfig == null) {
            throw new Exception(LbLogUtil.getStringManager().getString("UnableToGetLbConfig", lbConfigName));
        }
        return new LoadbalancerReaderImpl(domain, appRegistry, lbConfig);
    }

    public static LoadBalancer getLoadBalancer(Domain domain, String lbName) throws Exception {
        LoadBalancer loadBalancer = domain.getLoadBalancers().getLoadBalancer(lbName);
        if (loadBalancer == null) {
            throw new Exception(LbLogUtil.getStringManager().getString("UnableToGetLoadbalancer", lbName));
        }
        return loadBalancer;
    }

    /**
     * exports the loadbalancer.xml from the config to the outputstream provided
     * @param ctx ConfigContext
     * @param lbConfigName name of lb-config 
     * @param out OutputStream into which the loadbalancer.xml is written
     */
    public static void exportXml(LoadbalancerReader lbRdr, OutputStream out)
            throws Exception {

        // tranform the data using visitor pattern
        Loadbalancer _lb = new Loadbalancer();

        LoadbalancerVisitor lbVstr = new LoadbalancerVisitor(_lb);
        lbRdr.accept(lbVstr);

        try {
            String footer = LbLogUtil.getStringManager().getString("GeneratedFileFooter",
                    new Date().toString());
            // write the header
            _lb.graphManager().setDoctype(PUBLICID, SYSTEMID);
            _lb.write(out);
            out.write(footer.getBytes());
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }
    private static final String PUBLICID =
            "-//Sun Microsystems Inc.//DTD Sun Java System Application Server 9.1//EN";
    private static final String SYSTEMID = "glassfish-loadbalancer_1_3.dtd";
}
