/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.remote.writer;

import com.sun.enterprise.admin.remote.ParamsWithPayload;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import org.glassfish.api.admin.ParameterMap;

/** Writes ParameterMap into the POST
 *
 * @author mmares
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ParameterMapFormProprietaryWriter implements ProprietaryWriter {

    @Override
    public void writeTo(Object entity, HttpURLConnection urlConnection) throws IOException {
        ParameterMap pm;
        if (entity instanceof ParameterMap) {
            pm = (ParameterMap) entity;
        } else if (entity instanceof ParamsWithPayload) {
            pm = ((ParamsWithPayload) entity).getParameters();
        } else {
            pm = new ParameterMap();
        }
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : pm.entrySet()) {
            for (String value : entry.getValue()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                if (value != null) {
                    sb.append('=');
                    sb.append(URLEncoder.encode(value, "UTF-8"));
                }
            }
        }
        urlConnection.getOutputStream().write(sb.toString().getBytes("UTF-8"));
    }

    @Override
    public boolean isWriteable(Object entity) {
        if (entity instanceof ParameterMap) {
            return true;
        }
        if (entity instanceof ParamsWithPayload) {
            ParamsWithPayload pwp = (ParamsWithPayload) entity;
            return pwp.getPayloadOutbound() == null || pwp.getPayloadOutbound().size() == 0;
        }
        return false;
    }

}
