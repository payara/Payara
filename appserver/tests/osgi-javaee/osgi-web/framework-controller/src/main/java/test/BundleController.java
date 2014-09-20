/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 *
 * @author mohit
 */

@Path("/bundlecontroller")
public class BundleController {

    @Context ServletContext ctx;
    @Context HttpServletResponse resp;
    
    @POST
    public void bundleController(@FormParam("id") String bundleId, @FormParam("bsubmit") String requestType) {
        System.out.println("Controller :" + requestType);
        long uninstallId = 0;
        BundleContext bundleContext = (BundleContext) ctx.getAttribute("osgi-bundlecontext");

        if(bundleId == null) {
            uninstallId = (Long) ctx.getAttribute("bundleId");
        } else {
            uninstallId = Long.parseLong(bundleId);
        }
        try {
            if(uninstallId != 0) {
                Bundle bundle = bundleContext.getBundle(uninstallId);
                if (bundle != null) {
                    if (requestType.equalsIgnoreCase("Stop") && bundle.getState() == bundle.ACTIVE) {
                        bundle.stop();
                    } else if (requestType.equalsIgnoreCase("Start") &&
                            (bundle.getState() == bundle.RESOLVED || bundle.getState() == bundle.INSTALLED)) {
                        bundle.start();
                    } else if (requestType.equalsIgnoreCase("Uninstall")) {
                        bundle.uninstall();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                resp.sendRedirect(resp.encodeRedirectURL("bundleviewer"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
