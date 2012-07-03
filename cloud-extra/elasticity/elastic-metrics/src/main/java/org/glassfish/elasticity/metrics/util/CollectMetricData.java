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
package org.glassfish.elasticity.metrics.util;

import org.glassfish.elasticity.metrics.util.MarshallingUtils;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.Response;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 3/28/12
 */
public class CollectMetricData {

    private static final String RESPONSE_TYPE = MediaType.APPLICATION_JSON;


    public static Map<String, Object> getRestData(String url) {

        Map<String, Object> res= null;

        try {
            String json = "";

/*
            URL dataURL = new URL(url + ".json");
            URLConnection conn = dataURL.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
//                System.out.println("** Got line: " + line);
                json = line;
            }
*/

            javax.ws.rs.client.Client client = ClientFactory.newClient();

           Response response = client.target(url).request(RESPONSE_TYPE).get(Response.class);
            if (response.getStatus() == 200)        // untill OE can tell us that the instance is ready
                res = getEntityValues(response);
 /*
            Map responseMap = MarshallingUtils.buildMapFromDocument(json);

            Object obj = responseMap.get("extraProperties");
            if (obj != null) {
                res = (Map) ((Map) obj).get("entity");
            } else {
                res = responseMap;
            }
*/
        } catch (Exception ex){
                ex.printStackTrace();
        }

          return res;
    }

    /**
      * This method will parse the provided XML document and return a map
      * of the attributes and values on the root element
      *
      * @param response
      * @return
      */
     protected static Map<String, Object> getEntityValues(Response response) {
         Map<String, Object> map = new HashMap<String, Object>();

         String xml = response.readEntity(String.class);
         Map responseMap = MarshallingUtils.buildMapFromDocument(xml);
         Object obj = responseMap.get("extraProperties");
         if (obj != null) {
             return (Map) ((Map) obj).get("entity");
         } else {
             return map;
         }
     }

}
