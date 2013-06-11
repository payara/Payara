/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.SecurityLoggerInfo;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;

import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.Changed.TYPE;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * Tracks changes to secure admin configuration, basically so it can report
 * restart-required.
 *
 * @author Tim Quinn
 */
@Service
@RunLevel(PostStartupRunLevel.VAL)
public class SecureAdminConfigMonitor implements ConfigListener {

    private static final String restartRequiredMsg = Strings.get("secure.admin.change.requires.restart");

    /*
     * Must inject Domain to get notifications of SecureAdmin changes.  We
     * cannot inject SecureAdmin itself because it might be null and that
     * bothers some components.
     */
    @Inject
    private Domain domain;

    private Logger logger = SecurityLoggerInfo.getLogger();

    
    @Override
    public UnprocessedChangeEvents changed(final PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(
            events, new Changed() {
                @Override
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type,
                    Class<T> tClass, T t) {
                    if (t instanceof Domain) {
                        return processDomain(type, (Domain) t, events);
                    } else if (t instanceof SecureAdmin) {
                        return processSecureAdmin(type, (SecureAdmin) t, events);
                    }
                    return null;
                }
            }, logger);
    }

    private NotProcessed processDomain(final TYPE type, final Domain d,
            final PropertyChangeEvent[] events) {
        for (PropertyChangeEvent event : events) {
            if (   (event.getOldValue() instanceof SecureAdmin && type == Changed.TYPE.REMOVE)
                || (event.getNewValue() instanceof SecureAdmin && type == Changed.TYPE.ADD) ) {
                return new NotProcessed(restartRequiredMsg);
            }
        }
        return null;
    }

    private NotProcessed processSecureAdmin(final TYPE type, final SecureAdmin sa,
            final PropertyChangeEvent[] events) {
        /*
         * Any change to the secure admin config requires a restart.
         */
        return new NotProcessed(restartRequiredMsg);
    }
}
