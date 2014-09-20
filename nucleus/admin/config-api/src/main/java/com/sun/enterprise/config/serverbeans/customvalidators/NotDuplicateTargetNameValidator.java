/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans.customvalidators;

import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Domain;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.glassfish.api.admin.config.Named;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;


/**
 * Validation logic for NotDuplicateTargetName constraint
 *
 * @author Joe Di Pol
 */
public class NotDuplicateTargetNameValidator implements
                        ConstraintValidator<NotDuplicateTargetName, Named> {

    Domain domain = null;

    @Override
    public void initialize(NotDuplicateTargetName constraintAnnotation) {
    	ServiceLocator locator = ServiceLocatorFactory.getInstance().find("default");
    	if (locator == null) return;
    	
    	ConfigBeansUtilities cbu = locator.getService(ConfigBeansUtilities.class);
    	if (cbu == null) return;
    	
        domain = cbu.getDomain();
    }

    @Override
    public boolean isValid(Named bean, ConstraintValidatorContext context ) {

        // When we search for name clashes we typically do not check the
        // type of object being created since that check has already been
        // done by the config framework, and checking for duplicates is not cheap
        // We use these booleans for readability
        boolean checkCluster = true;
        boolean checkConfig = true;
        boolean checkNode = true;
        boolean checkServer = true;

        if (bean instanceof Server) {
            checkServer = false;
        } else if (bean instanceof Cluster) {
            checkCluster = false;
        } else if (bean instanceof Config) {
            checkConfig = false;
        } else if (bean instanceof Node) {
            checkNode = false;
        } else {
            // Unknown bean type. In this case we just go ahead and check
            // against Server, Cluster, Config, and Node
        }

        String name = bean.getName();

        if (domain == null) {
            return true;
        }

        if ((checkCluster && domain.getClusterNamed(name) != null) ||
            (checkConfig  && domain.getConfigNamed(name) != null) ||
            (checkNode    && domain.getNodeNamed(name) != null) ||
            (checkServer  && domain.getServerNamed(name) != null)) {
            return false;
        } else {
           return true;
        }
    }
}
