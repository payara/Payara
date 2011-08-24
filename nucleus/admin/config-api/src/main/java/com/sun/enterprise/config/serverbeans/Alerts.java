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
package com.sun.enterprise.config.serverbeans;


import org.glassfish.api.I18n;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.TypeAndNameResolver;
import org.glassfish.config.support.TypeResolver;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.DuckTyped;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/18/11
 */
@Configured
public interface Alerts  extends ConfigBeanProxy, Injectable {
    /**
   * Return the list of currently configured alert. Alerts can
   * be added or removed by using the returned {@link java.util.List}
   * instance
   *
   * @return the list of configured {@link Alerts}
   */
  @Create(value="_create-alert", resolver= TypeResolver.class,   i18n=@I18n("_register.alert.command"))
  @Delete(value="_delete-alert", resolver= TypeAndNameResolver.class,  i18n=@I18n("_unregister.alert.command"))
  @Element
   public List<Alert> getAlerts();

    @Element
   public void setAlert(Alert alert);

  /**
   * Return the alert with the given name, or null if no such alert exists.
   *
   * @param   name    the name of the alert
   * @return          the Alert object, or null if no such alert
   */
  @DuckTyped
  public Alerts  getAlert(String name);

  class Duck {
      public static Alert getAlert(Alerts instance, String name) {
          for (Alert alert : instance.getAlerts()) {
              if (alert.getName().equals(name)) {
                  return alert;
              }
          }
          return null;
      }

  }

}
