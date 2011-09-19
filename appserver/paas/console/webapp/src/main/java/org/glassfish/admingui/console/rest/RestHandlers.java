/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.rest;

import java.util.*;
import org.glassfish.admingui.console.util.GuiUtil;

public class RestHandlers {

    public Map restRequest(String endPoint, Map attrs, Object data, String contentType, String method, boolean quiet,
            boolean throwException) {
        //refer to bug#6942284. Some of the faulty URL may get here with endpoint set to null
        if (GuiUtil.isEmpty(endPoint)){
            return new HashMap();
        }else{
            return RestUtil.restRequest(endPoint, attrs, method, data, contentType,  quiet);
        }
    }

}
