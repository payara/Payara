/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.provider;

import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.results.OptionsResult;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.util.Iterator;
import java.util.Set;

import static org.glassfish.admin.rest.provider.ProviderUtil.getEndXmlElement;
import static org.glassfish.admin.rest.provider.ProviderUtil.quote;

/**
 * XML provider for OptionsResult.
 *
 * @author Rajeshwar Patil
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class OptionsResultXmlProvider extends BaseProvider<OptionsResult> {
    private final static String QUERY_PARAMETERS = "queryParameters";
    private final static String MESSAGE_PARAMETERS = "messageParameters";
    private final static String METHOD = "method";

     public OptionsResultXmlProvider() {
         super(OptionsResult.class, MediaType.APPLICATION_XML_TYPE);
     }

     //get json representation for the given OptionsResult object
     @Override
     public String getContent(OptionsResult proxy) {
        String result;
        String indent = Constants.INDENT;
        result = "<" + proxy.getName() + ">" ;

        result = result + getRespresenationForMethodMetaData(proxy, indent);

        result = result + "\n" + getEndXmlElement(proxy.getName());
        return result;
    }


    String getRespresenationForMethodMetaData(OptionsResult proxy, String indent) {
        String result = "";
        Set<String> methods = proxy.methods();
        Iterator<String> iterator = methods.iterator();
        String method;

        while (iterator.hasNext()) {
           method = iterator.next();

           //method
           result = result + getMethod(method, indent);

           MethodMetaData methodMetaData = proxy.getMethodMetaData(method);

//           //query params
//           result = result + getQueryParams(methodMetaData,
//               indent + Constants.INDENT);

           //parameters (message parameters)
           result = result + getMessageParams(methodMetaData,
               indent + Constants.INDENT);

           result = result + "\n" + indent;
           result = result + getEndXmlElement(METHOD);
        }
        return result;
    }


    //get xml representation for the given method name
    private String getMethod(String method, String indent) {
        String result = "\n" + indent + "<";
        result = result + METHOD + " name=";
        result = result + quote(method);
        result = result + ">";
        return result;
    }


//    //get xml representation for the method query parameters
//    private String getQueryParams(MethodMetaData methodMetaData,
//            String indent) {
//        //TODO too many string concatenations happening here. Change this and other methods in this class to use StringBuffer
//        String result = "";
//        if (methodMetaData.sizeQueryParamMetaData() > 0) {
//            result = result + "\n" + indent;
//            result = result + "<" + QUERY_PARAMETERS + ">";
//
//            Set<String> queryParams = methodMetaData.queryParams();
//            Iterator<String> iterator = queryParams.iterator();
//            String queryParam;
//            while (iterator.hasNext()) {
//                queryParam = iterator.next();
//                ParameterMetaData parameterMetaData =
//                    methodMetaData.getQueryParamMetaData(queryParam);
//                result = result + getParameter(queryParam, parameterMetaData,
//                    indent + Constants.INDENT);
//            }
//            result = result + "\n" + indent;
//            result = result +  getEndXmlElement(QUERY_PARAMETERS);
//        }
//        return result;
//    }


    //get xml representation for the method message parameters
    private String getMessageParams(MethodMetaData methodMetaData,
            String indent) {
        String result = "";
        if (methodMetaData.sizeParameterMetaData() > 0) {
            result = result + "\n" + indent;
            result = result + "<" + MESSAGE_PARAMETERS + ">";

            Set<String> parameters = methodMetaData.parameters();
            Iterator<String> iterator = parameters.iterator();
            String parameter;
            while (iterator.hasNext()) {
               parameter = iterator.next();
               ParameterMetaData parameterMetaData =
                   methodMetaData.getParameterMetaData(parameter);
               result = result + getParameter(parameter, parameterMetaData,
                   indent + Constants.INDENT);
            }
            result = result + "\n" + indent;
            result = result + getEndXmlElement(MESSAGE_PARAMETERS);
        }
        return result;
    }


    //get xml representation for the given parameter
    private String getParameter(String parameter, ParameterMetaData parameterMetaData, String indent) {
        StringBuilder result = new StringBuilder("\n" + indent);

        result.append("<").append(parameter);

        Set<String> attributes = parameterMetaData.attributes();
        Iterator<String> iterator = attributes.iterator();
        String attributeName;
        while (iterator.hasNext()) {
           attributeName = iterator.next();
           String attributeValue =
               parameterMetaData.getAttributeValue(attributeName);
           result.append(getAttribute(attributeName, attributeValue));
        }
        result.append("/>");
        return result.toString();
    }


    //get xml representation for a give attribute of parameter
    private String getAttribute(String name, String value) {
        String result = " ";
        name = name.replace(' ', '-');
        result = result + name + "=" + quote(value);
        return result;
    }

}
