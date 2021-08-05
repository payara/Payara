/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.openid.api;

import static java.util.Objects.isNull;

/**
 * Display specifies how the Authorization Server displays the authentication
 * and consent user interface pages to the End-User.
 *
 * @author Gaurav Gupta
 */
public enum DisplayType {

    /**
     * The Authorization Server should display authentication and consent UI
     * consistent with a full User-Agent page view. If the display parameter is
     * not specified this is the default display mode.
     *
     */
    PAGE,
    /**
     * The Authorization Server should display authentication and consent UI
     * consistent with a popup User-Agent window.
     *
     */
    POPUP,
    /**
     * The Authorization Server should display authentication and consent UI
     * consistent with a device that leverages a touch interface.
     *
     */
    TOUCH,
    /**
     * The Authorization Server should display authentication and consent UI
     * consistent with a "feature phone" type display.
     *
     */
    WAP;

    public static DisplayType fromString(String key) {
        return isNull(key) || key.trim().isEmpty() ? null : valueOf(key.toUpperCase());
    }

}
