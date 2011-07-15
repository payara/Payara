/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.Singleton;

@Deprecated
public interface AccessLog
        extends Singleton, ConfigElement, PropertiesAccess {


    public String getFormat();

    public void setFormat(String param1);

    public String getBufferSizeBytes();

    public void setBufferSizeBytes(String param1);

    public String getWriteIntervalSeconds();

    public void setWriteIntervalSeconds(String param1);

    public String getRotationEnabled();

    public void setRotationEnabled(final String param1);

    public String getRotationIntervalInMinutes();

    public void setRotationIntervalInMinutes(final String param1);

    /**
     * Possible value for RotationPolicy.
     */
    public static final String ROTATION_POLICY_BY_TIME = "time";
    /**
     * Possible value for RotationPolicy.
     */
    public static final String ROTATION_POLICY_BY_SIZE = "size";
    /**
     * Possible value for RotationPolicy.
     */
    public static final String ROTATION_POLICY_ON_DEMAND = "on-demand";

    /**
     * Valid values are:
     * <ul>
     * <li>{@link #ROTATION_POLICY_BY_TIME}</li>
     * <li>{@link #ROTATION_POLICY_BY_SIZE}</li>
     * <li>{@link #ROTATION_POLICY_ON_DEMAND}</li>
     * </ul>
     */
    public void setRotationPolicy(final String param1);

    public String getRotationPolicy();

    public String getRotationSuffix();

    public void setRotationSuffix(final String param1);

    public String getMaxHistoryFiles();

    public void setMaxHistoryFiles(final String param1);

}
