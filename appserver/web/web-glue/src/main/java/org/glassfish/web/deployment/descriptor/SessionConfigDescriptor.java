/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.descriptor;

import java.util.EnumSet;
import java.util.Set;
import javax.servlet.SessionTrackingMode;

import com.sun.enterprise.deployment.web.CookieConfig;
import com.sun.enterprise.deployment.web.SessionConfig;
import org.glassfish.deployment.common.Descriptor;

/**
 * This represents the session-config in web.xml.
 *
 * @author Shing Wai Chan
 */

public class SessionConfigDescriptor extends Descriptor implements SessionConfig {
    public static final int SESSION_TIMEOUT_DEFAULT = 30;

    private int sessionTimeout;
    private CookieConfig cookieConfig = null;
    private Set<SessionTrackingMode> trackingModes = null;

    public SessionConfigDescriptor() {
        sessionTimeout = SESSION_TIMEOUT_DEFAULT;
    }

    /**
     * @return the value in seconds of when requests should time out.
     */
    public int getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * Sets thew value in seconds after sessions should timeout.
     */
    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public CookieConfig getCookieConfig() {
        return cookieConfig;
    }

    public void setCookieConfig(CookieConfig cookieConfig) {
        this.cookieConfig = cookieConfig;
    }

    /**
     * DeploymentDescriptorNode.addNodeDescriptor(node) need this.
     */
    public void setCookieConfig(CookieConfigDescriptor cookieConfigDesc) {
        this.cookieConfig = cookieConfigDesc;
    }

    public void addTrackingMode(String trackingMode) {
        if (trackingModes == null) {
            trackingModes = EnumSet.noneOf(SessionTrackingMode.class);
        }
        trackingModes.add(Enum.valueOf(SessionTrackingMode.class, trackingMode));
    }

    public void removeTrackingMode(String trackingMode) {
        if (trackingModes == null) {
            return;
        }
        trackingModes.remove(Enum.valueOf(SessionTrackingMode.class, trackingMode));
    }

    public Set<SessionTrackingMode> getTrackingModes() {
        if (trackingModes == null) {
            trackingModes = EnumSet.noneOf(SessionTrackingMode.class);
        }
        return trackingModes;
    }

    public void print(StringBuffer toStringBuffer) {
        toStringBuffer.append("\n sessionTimeout ").append(sessionTimeout);
        if (cookieConfig!= null) {
            toStringBuffer.append(cookieConfig);
        }
        if (trackingModes != null) {
            toStringBuffer.append("\n trackingModes ");
            for (SessionTrackingMode tm : trackingModes) {
                toStringBuffer.append(tm);
            }
        }
    }
}
