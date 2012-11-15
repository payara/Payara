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

import java.util.Comparator;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ServerTags;
import org.glassfish.api.admin.config.ApplicationName;

/**
 * DeploymentType defines the various deployment entity types,
 * and deployment ordering within the types.
 * <p>
 * For an ordering among the various types, please refer to
 * <CODE>DeploymentOrder</CODE>.
 * <p>
 * Note that the comparator of the deployment types should
 * return a consistent value during the lifetime of the 
 * deployment objects, e.g. it should not depend on values of
 * mutable fields of the deployment objects.
 * <p>
 * The list of deployment types are as follows:
 * <p><ol>
 * <li> INTERNAL_APP
 * <li> JDBC_SYS_RES
 * <li> DEPLOYMENT_HANDLER
 * <li> JMS_SYS_RES
 * <li> RESOURCE_DEPENDENT_DEPLOYMENT_HANDLER
 * <li> STARTUP_CLASS
 * <li> WLDF_SYS_RES
 * <li> LIBRARY
 * <li> CONNECTOR
 * <li> DEFAULT_APP
 * <li> COHERENCE_CLUSTER_SYS_RES
 * <li> CUSTOM_SYS_RES
 * </ol>
 *
 */

public class DeploymentType implements Comparator {

  public static final String SYSTEM_PREFIX = "system-";
  public static final String USER = "user";
  public final static String INTERNAL_APP_NAME = "InternalApp";
  public final static String CONNECTOR_NAME = "Connector";
  public final static String DEFAULT_APP_NAME = "DefaultApp";

  private final String name;
  private final Class cls;

  public final static DeploymentType INTERNAL_APP = 
    new DeploymentType(INTERNAL_APP_NAME, ApplicationOrderInfo.class) {
      public boolean isInstance(Object obj) {
        if (super.isInstance(obj)) {
          ApplicationOrderInfo appOrderInfo = (ApplicationOrderInfo)obj;
          if (appOrderInfo.getApplication().getObjectType().startsWith(SYSTEM_PREFIX)) {
            return true;
          }
        }
        return false;
      }
    };

  public final static DeploymentType DEFAULT_APP = 
    new DeploymentType(DEFAULT_APP_NAME, ApplicationOrderInfo.class) {
      public boolean isInstance(Object obj) {
        if (super.isInstance(obj)) {
          ApplicationOrderInfo appOrderInfo = (ApplicationOrderInfo)obj;
          if (appOrderInfo.getApplication().getObjectType().equals(USER)) {
            return true;
          }
        }
        return false;
      }
    };

  public final static DeploymentType CONNECTOR = 
    new DeploymentType(CONNECTOR_NAME, ApplicationOrderInfo.class) {
      public boolean isInstance(Object obj) {
        if (super.isInstance(obj)) {
          ApplicationOrderInfo appOrderInfo = (ApplicationOrderInfo)obj;
          if ((appOrderInfo.getApplication().containsSnifferType(ServerTags.CONNECTOR)) &&
              (appOrderInfo.getApplication().isStandaloneModule())) {
            return true;
          }
        }
        return false;
      }
    };

  private DeploymentType(String name, Class cls) { 
    this.name = name;
    this.cls = cls;
  }

  public String toString() { return name; }
  public boolean isInstance(Object obj) { return cls.isInstance(obj); }
  public Comparator getComparator() { return this; }

  // Compares two instances of the current type
  public int compare(Object o1, Object o2) {
    if ((o1 instanceof ApplicationOrderInfo) && (o2 instanceof ApplicationOrderInfo)) {
      return compare((ApplicationOrderInfo)o1, (ApplicationOrderInfo)o2);
    }
    return defaultCompare(o1, o2);
  }

  protected int defaultCompare(Object o1, Object o2) {
    if (o1 instanceof ApplicationOrderInfo && o2 instanceof ApplicationOrderInfo ) {
      ApplicationOrderInfo o1App = (ApplicationOrderInfo)o1;
      ApplicationOrderInfo o2App = (ApplicationOrderInfo)o2;
      return o1App.getOriginalOrder() - o2App.getOriginalOrder();
    }
    /*
     * The following is for WLS compatibility where ties amone
     * applications with the same deployment order are resolved
     * by comparing the application name.
     */
    /*
    if (o1 instanceof ApplicationName && o2 instanceof ApplicationName ) {
      return ((ApplicationName)o1).getName().compareTo(((ApplicationName)o2).getName());
    }
    */
    return 0;
  }

  protected int compare(ApplicationOrderInfo d1, ApplicationOrderInfo d2) {
    int comp = new Integer(d1.getApplication().getDeploymentOrder()).compareTo(new Integer(d2.getApplication().getDeploymentOrder()));
    if (comp == 0) {
      return defaultCompare(d1,d2);
    }
    return comp;
  }
}


