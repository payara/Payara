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
import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.net.NetUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.*;
import static org.glassfish.config.support.Constants.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.Injectable;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.ReferenceContainer;

import javax.validation.OverridesAttribute;
import javax.validation.Payload;
import java.beans.PropertyVetoException;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.constraints.Pattern;
/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 8/18/11
 */

@Configured
public interface AlertConfig extends ConfigBeanProxy {

    /**
     * Sets the alert name
     * @param value alert name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    public void setName(String value) throws PropertyVetoException;

    @Attribute
    public String getName();
    /**
     * Sets the alert schedule
     * @param value alert schedule
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="schedule", optional=true, defaultValue="10m")
    public void setSchedule(String value) throws PropertyVetoException;

    @Attribute (defaultValue = "10m")
    public String getSchedule();

    /**
     * Sets the alert sample interval in minutes
     * @param value alert interval
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="sample-interval", optional=true, defaultValue="5")
    public void setSampleInterval(int value) throws PropertyVetoException;

    @Attribute (defaultValue = "5")
    public int getSampleInterval();

    /**
     * Sets the alert sample interval in minutes
     * @param value alert interval
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="enabled", optional=true, defaultValue="true")
    public void setEnabled(boolean value) throws PropertyVetoException;

    @Attribute  (defaultValue = "true")
    public int getEnabled();

    /**
     * Sets the alert expression
     * @param value alert expression
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="expression")
    public void setExpression(String value) throws PropertyVetoException;

    @Attribute
    public String getExpression();

   /**
     * Sets the alert service
     * @param value alert service
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="service")
    public void setService(String value) throws PropertyVetoException;

    @Attribute
    public String getService();

    /**
     * List of actions associated with this alert
     */
    @Element
    AlertActions getAlertActions();

      /**
     * Sets the AlertActions element
     * @param actions
     */

    void setAlertActions(AlertActions alertActions);

}
