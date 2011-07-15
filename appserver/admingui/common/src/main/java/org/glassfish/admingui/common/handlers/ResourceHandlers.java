/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.handlers;

import java.util.Map;
import java.util.List;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import org.glassfish.admingui.common.util.DeployUtil;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.TargetUtil;


/**
 *
 * @author Anissa Lam
 */
public class ResourceHandlers {

    /*
     * This handler takes in a list of rows, there should be 'Enabled' attribute in each row.
     * Get the resource-ref of this resource and do a logical And with this Enabled attribute
     * to get the real status
     */
    @Handler(id = "gf.getResourceRealStatus",
        input = {
            @HandlerInput(name = "endpoint", type = String.class),
            @HandlerInput(name = "rows", type = java.util.List.class, required = true)},
        output = {
            @HandlerOutput(name = "result", type = List.class)})
    public static void getResourceRealStatus(HandlerContext handlerCtx) {
        List<Map> rows = (List) handlerCtx.getInputValue("rows");
        Map<String, String> targetsMap = new HashMap<String, String>();
        for (Map oneRow : rows) {
            try {
                String name = (String) oneRow.get("name");
                String encodedName = URLEncoder.encode(name, "UTF-8");
                List<String> targets = DeployUtil.getApplicationTarget(name, "resource-ref");
                if (targets == null || targets.size() == 0) {
                    continue; //The resource is only created on domain, no source-ref exists.
                }
                String enabledStr = DeployUtil.getTargetEnableInfo(encodedName, false, false);
                List<String> targetUrls = new ArrayList<String>();
                for (String target : targets) {
                    if (TargetUtil.isCluster(target)) {
                        targetsMap.put(GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster/" + target, target);
                        targetUrls.add(GuiUtil.getSessionValue("REST_URL") + "/clusters/cluster/" + target);
                    } else {
                        targetsMap.put(GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + target, target);
                        targetUrls.add(GuiUtil.getSessionValue("REST_URL") + "/servers/server/" + target);
                    }
                }
                oneRow.put("targetUrls", targetUrls);
                oneRow.put("targetsMap", targetsMap);
                oneRow.put("enabled", enabledStr);
            } catch (Exception ex) {
                GuiUtil.handleException(handlerCtx, ex);
            }
        }
        handlerCtx.setOutputValue("result", rows);
    }
}
