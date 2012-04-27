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

package org.glassfish.virtualization.impl;

import org.glassfish.api.Startup;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.PostConstruct;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.IAAS;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;

/**
 * IAAS bootstrap code to avoid loading the IMS implementation (and its dependencies)
 * until there is some virtualization related config in the configuration.
 *
 * @author Jerome Dochez
 */
@Service
public class IAASBootstrap implements Startup, PostConstruct {

    @Inject(optional=true)
    Virtualizations virtualizations=null;

    @Inject
    Transactions transactions;

    @Inject
    Habitat services;

    @Override
    public Lifecycle getLifecycle() {
        return Lifecycle.SERVER;
    }

    @Override
    public void postConstruct() {
        if (virtualizations==null) {
            transactions.addListenerForType(Virtualizations.class, new ConfigListener() {
                    @Override
                    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                        services.forContract(IAAS.class).get();
                        return null;
                    }
                });
        } else {
            services.forContract(IAAS.class).get();
        }
    }
}
