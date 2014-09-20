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

package org.glassfish.config.support;

import com.sun.enterprise.config.serverbeans.*;

import java.util.List;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * CommandTarget is an enumeration of valid configuration target for a command execution
 * 
 */
public enum CommandTarget implements TargetValidator {

    /**
     * a domain wide configuration change
     */
    DOMAIN {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            return target.equals("domain");
        }

        @Override
        public String getDescription() {
            return "Domain";
        }
    },
    /**
     * configuration change to default server
     */
    DAS {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            return target.equals("server");
        }

        @Override
        public String getDescription() {
            return "Default server";
        }
    },
    /**
     * a clustered instance configuration change
     */
    CLUSTERED_INSTANCE {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            Domain domain = habitat.getService(Domain.class);
            return (domain.getClusterForInstance(target) != null);
        }

        @Override
        public String getDescription() {
            return "Clustered Instance";
        }
    },
    /**
     * a standalone instance configuration change
     */
    STANDALONE_INSTANCE {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            Domain domain = habitat.getService(Domain.class);
            return (domain.getServerNamed(target) != null);
        }

        @Override
        public String getDescription() {
            return "Stand alone instance";
        }
    },
    /**
     * a config configuration change
     */
    CONFIG {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            Domain domain = habitat.getService(Domain.class);
            return domain.getConfigNamed(target) != null;
        }

        @Override
        public String getDescription() {
            return "Config";
        }
    },
    /**
     * a cluster configuration change
     */
    CLUSTER {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            Domain domain = habitat.getService(Domain.class);
            return domain.getClusterNamed(target) != null;
        }

        @Override
        public String getDescription() {
            return "Cluster";
        }
    },
    /**
     * a node configuration change
     */
    NODE {
        @Override
        public boolean isValid(ServiceLocator habitat, String target) {
            Domain domain = habitat.getService(Domain.class);
            return domain.getNodeNamed(target) != null;
        }

        @Override
        public String getDescription() {
            return "Node";
        }
    };

    @Override
    public boolean isValid(ServiceLocator habitat, String target) {
        return false;
    }

    @Override
    public String getDescription() {
        return this.name();
    }
}
