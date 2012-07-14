/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.configapi.tests;

import java.beans.PropertyChangeEvent;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PerLookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Simple container code that is interested in getting notification of injected model changes
 *
 * @author Jerome Dochez
 */
@Service @PerLookup
public class HttpListenerContainer implements ConfigListener {

    @Inject
    @Named("http-listener-1")
    NetworkListener httpListener;

    volatile boolean received=false;
    
    public synchronized UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        if (received) {
            // I am alredy happy
            return null;
        }
        assertTrue(events.length==1);
        String listenerName = ((NetworkListener) events[0].getSource()).getName();
        assertEquals("http-listener-1",listenerName);
        assertEquals("8989",events[0].getNewValue().toString());
        received = true;
        return null;
    }

}
