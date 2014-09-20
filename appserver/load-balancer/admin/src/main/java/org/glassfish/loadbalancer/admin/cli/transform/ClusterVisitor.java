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

package org.glassfish.loadbalancer.admin.cli.transform;

import org.glassfish.loadbalancer.admin.cli.reader.api.BaseReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.ClusterReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.InstanceReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.WebModuleReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.HealthCheckerReader;

import org.glassfish.loadbalancer.admin.cli.beans.Cluster;
import org.glassfish.loadbalancer.admin.cli.beans.WebModule;

/**
 * Provides transform capabilites for cluster
 *
 * @author Satish Viswanatham
 */
public class ClusterVisitor implements Visitor {

    // ------ CTOR ------
    public ClusterVisitor(Cluster c) {
        _c = c;
    }

    /**
     * Visit reader class 
     */
    @Override
    public void visit(BaseReader br) throws Exception {
        // FIXME, make as assert here about no class cast exception
		if (br instanceof ClusterReader) {
			ClusterReader cRdr = (ClusterReader) br;
			_c.setName(cRdr.getName());
			_c.setPolicy(cRdr.getLbPolicy());
			_c.setPolicyModule(cRdr.getLbPolicyModule());
			InstanceReader[] iRdrs = null;
			iRdrs = cRdr.getInstances();

			if ((iRdrs != null) && (iRdrs.length > 0)) {
				boolean[] values = new boolean[iRdrs.length];
				// XXX check if setting to true is required and is ok.
				for (int i = 0; i < iRdrs.length; i++) {
					values[i] = true;
				}
				_c.setInstance(values);
				for (int i = 0; i < iRdrs.length; i++) {
					iRdrs[i].accept(new InstanceVisitor(_c, i));
				}
			}

			HealthCheckerReader hcRdr = cRdr.getHealthChecker();

			if (hcRdr != null) {
				hcRdr.accept(new HealthCheckerVisitor(_c));
			}

			WebModuleReader[] wRdrs = cRdr.getWebModules();

			if ((wRdrs != null) && (wRdrs.length > 0)) {
				WebModule[] wMods = new WebModule[wRdrs.length];
				for (int i = 0; i < wRdrs.length; i++) {
					wMods[i] = new WebModule();
					wRdrs[i].accept(new WebModuleVisitor(wMods[i], _c));
				}
				_c.setWebModule(wMods);
			}
		}
    }
    //--- PRIVATE VARS ----
    Cluster _c = null;
}
