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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 
 // Portions Copyright [2017] [Payara Foundation and/or its affiliates.]

/*
 * JmsResourceHandler.java
 *
 * Created on January 9, 2013, 2:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import static java.util.logging.Level.FINE;
import static org.glassfish.admingui.common.util.GuiUtil.getCommonMessage;
import static org.glassfish.admingui.common.util.GuiUtil.getLogger;
import static org.glassfish.admingui.common.util.GuiUtil.getSessionValue;
import static org.glassfish.admingui.common.util.RestUtil.getAttributesMap;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

public class JmsResourceHandler {

	/**
	 * <p>
	 * This handler return the list of JMS Resources to be displayed in the table.
	 */
	@Handler(id = "getJmsResourcesInfo", input = { @HandlerInput(name = "resourcesList", type = List.class),
	        @HandlerInput(name = "isConnectionFactory", type = Boolean.class) }, output = {
	                @HandlerOutput(name = "result", type = List.class) })
	public static void getJmsResourcesInfo(HandlerContext handlerCtx) {

		List<Map<String, Object>> resourcesList = (List) handlerCtx.getInputValue("resourcesList");
		Boolean isConnectionFactory = (Boolean) handlerCtx.getInputValue("isConnectionFactory");
		
		String prefix = isConnectionFactory ? 
				getSessionValue("REST_URL") + "/resources/connector-resource/" : 
				getSessionValue("REST_URL") + "/resources/admin-object-resource/";
		
		try {
			for (Map<String, Object> one : resourcesList) {
				String encodedName = URLEncoder.encode((String) one.get("name"), "UTF-8");
				String endpoint = prefix + encodedName;
				Map attrs = (Map) getAttributesMap(endpoint);
				
				String description = null;
				if (isConnectionFactory) {
					String poolName = URLEncoder.encode((String) attrs.get("poolName"), "UTF-8");
					String e1 = (String) getSessionValue("REST_URL") + "/resources/connector-connection-pool/" + poolName;
					
					Map poolAttrs = (Map) getAttributesMap(e1);
					one.put("resType", (String) poolAttrs.get("connectionDefinitionName"));
					
					String lname = (String) one.get("logical-jndi-name");
					one.put("logicalJndiName", lname == null ? "" : lname);
					one.put("encodedPoolName", poolName);
					one.put("objectType", (String) attrs.get("objectType"));
					description = (String) poolAttrs.get("description");
				} else {
					one.put("resType", (String) attrs.get("resType"));
					description = (String) attrs.get("description");
				}
				
				one.put("selected", false);
				one.put("enabled", (String) attrs.get("enabled"));
				one.put("encodedName", encodedName);
				one.put("description", description == null ? "" : description);
			}
		} catch (Exception ex) {
			getLogger().info(getCommonMessage("log.error.getJMSResources") + ex.getLocalizedMessage());
			if (getLogger().isLoggable(FINE)) {
				ex.printStackTrace();
			}
		}
		
		handlerCtx.setOutputValue("result", resourcesList);
	}

}
