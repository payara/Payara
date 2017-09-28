/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.admingui.extras;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author jonathan coustick
 */
public class PayaraUtilHandlers {
    
    /**
     * 
     * @param context 
     */
    @Handler(id = "mapRemove",
            input={@HandlerInput(name="map", type = Map.class, required=true),
                @HandlerInput(name="key", type=Object.class, required=true)
            })
    public static void mapRemove(HandlerContext context){
        Map map = (Map) context.getInputValue("map");
        Object key = context.getInputValue("key");
        map.remove(key);
    }
    
    /**
     * A handler which converts a string of millseconds since the epoch into a human-friendly datetime format.
     * The output format is whatever is the default for the locale where the user is.
     * @param context
     * @since 4.1.2.174
     */
    @Handler(id = "py.prettyDateTimeFormat",
            input={@HandlerInput(name="milliseconds", type = String.class, required = true)},
            output=@HandlerOutput(name="prettyString", type=String.class))
    public static void prettyDateTimeFormat(HandlerContext context){
        String value = (String) context.getInputValue("milliseconds");
        String result = DateFormat.getDateTimeInstance().format(new Date(Long.valueOf(value)));
        context.setOutputValue("prettyString", result);
    }
    
}
