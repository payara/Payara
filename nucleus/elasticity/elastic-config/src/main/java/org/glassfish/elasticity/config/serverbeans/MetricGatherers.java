/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.elasticity.config.serverbeans;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.I18n;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.TypeAndNameResolver;
import org.glassfish.config.support.TypeResolver;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/18/11
 * Time: 5:32 PM
 */
@Configured
public interface MetricGatherers extends ConfigBeanProxy    {
    /**
   * Return the list of currently configured alert. Alerts can
   * be added or removed by using the returned {@link java.util.List}
   * instance
   *
   * @return the list of configured {@link Alerts}
   */
  @Element
  public List<MetricGatherer> getMetricGatherer();

  /**
   * Return the metric gatherer with the given type, or null if no such alert exists.
   *
   * @param   name    the type of the metric gatherer
   * @return          the MetricGatherer object, or null if no such type
   */

  @DuckTyped
  public MetricGatherer  getMetricGatherer(String type);


  class Duck {
      public static MetricGatherer getMetricGatherer(MetricGatherers instance, String type) {
          for (MetricGatherer mg : instance.getMetricGatherer()) {
              if (mg.getName().equals(type)) {
                  return mg;
              }
          }
          return null;
      }

  }

}
