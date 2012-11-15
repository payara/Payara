/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.deployment;

import java.util.Iterator;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import com.sun.enterprise.config.serverbeans.Application;

public class DeploymentOrder {

  /**
   * Deployment ordering among different deployment types.
   *
   * For deployment ordering within a particular type, please refer 
   * to <CODE>DeploymentType</CODE>.
   */
  /*
  public final static DeploymentType[] DEPLOYMENT_ORDER = {
    DeploymentType.INTERNAL_APP,
    DeploymentType.JDBC_SYS_RES,
    DeploymentType.DEPLOYMENT_HANDLER,
    DeploymentType.JMS_SYS_RES,
    DeploymentType.RESOURCE_DEPENDENT_DEPLOYMENT_HANDLER,
    DeploymentType.STARTUP_CLASS,
    DeploymentType.WLDF_SYS_RES,
    DeploymentType.LIBRARY,
    DeploymentType.CONNECTOR,
    DeploymentType.DEFAULT_APP,
    DeploymentType.COHERENCE_CLUSTER_SYS_RES,
    DeploymentType.CUSTOM_SYS_RES
  };
  */

  public final static DeploymentType[] APPLICATION_DEPLOYMENT_ORDER = {
    DeploymentType.INTERNAL_APP,
    DeploymentType.CONNECTOR,
    DeploymentType.DEFAULT_APP
  };

  /**
   * A comparator that imposes deployment ordering as defined by
   * <CODE>DEPLOYMENT_ORDER</CODE> above (for ordering among deployment 
   * types) and by the various DeploymentTypes (for ordering within
   * deployment types).
   */
  public final static Comparator APPLICATION_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      if (o1 == o2) {
        return 0;
      }
      for (int i = 0; i < APPLICATION_DEPLOYMENT_ORDER.length; i++) {
        DeploymentType depType = APPLICATION_DEPLOYMENT_ORDER[i];
        if (depType.isInstance(o1) && !depType.isInstance(o2)) {
          return -1;
        } else if (!depType.isInstance(o1) && depType.isInstance(o2)) {
          return 1;
        } else if (depType.isInstance(o1) && depType.isInstance(o2)) {
          return depType.compare(o1, o2);
        }
      }
      // unrecognized type
      throw new RuntimeException("unrecognized type");
    };
  };

  private static final TreeSet application_deployments = 
    new TreeSet(APPLICATION_COMPARATOR);

  public static void addApplicationDeployment(ApplicationOrderInfo app) {
    application_deployments.add(app);
  }

  public static Iterator getApplicationDeployments() {
    List<Application> appList = new ArrayList<Application>();
    Iterator<ApplicationOrderInfo> it = application_deployments.iterator();
    while (it.hasNext()) {
      ApplicationOrderInfo appOrderInfo = it.next();
      appList.add(appOrderInfo.getApplication());
    }
    return appList.iterator();
  }
}
