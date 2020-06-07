/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 */
package fish.payara.security.identitystores;

import org.glassfish.soteria.cdi.AnnotationELPProcessor;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * Class to retrieve value from config whether Microprofile, Payara/glassfish, EL, or a raw value. In that order.
 * @author Mark Wareham
 */
public class ConfigRetriever {

    private static final Config CONFIG = ConfigProvider.getConfig();
    
    private ConfigRetriever(){}//class should not be instanciated.
    
    /**
     * Grabs the config value regardless of where it's stored.
     * Should a microprofile config exist, that will be returned as top priority.
     * Otherwise returns the value translated from one of 
     * - payara/glassfish config ${}
     * - EL #{}
     * - raw. Returns the value given if none of the above exist.
     * @param expression the expression you are looking for
     * @param microProfileConfigKey a microprofile key.
     * @return a config value.
     */
    public static String resolveConfigAttribute(String expression, String microProfileConfigKey) {
        
        Optional<String> microProfileConfigValue = CONFIG.getOptionalValue(microProfileConfigKey, String.class);
        if (microProfileConfigValue.isPresent()) {
           return microProfileConfigValue.get();
        }
        if(isPayaraConfigFormat(expression)) {
            String translatedValue = TranslatedConfigView.expandValue(expression);
            if(translatedValue.equals(expression) && isELImmediateFormat(expression)) {
                return AnnotationELPProcessor.evalImmediate(expression);
            }
            return translatedValue;
        }
        if(isELDeferredFormat(expression)) {
            return AnnotationELPProcessor.evalELExpression(expression);
        }
        return expression;
    }
    
    /**
     * Grabs the config value regardless of where it's stored. Accounts for overriding expression parameters
     * Should a microprofile config exist, that will be returned as top priority.
     * Otherwise returns the value translated from one of 
     * - payara/glassfish config ${}
     * - EL #{}
     * - raw. Returns the value given if none of the above exist.
     * @param attribute the attribute value to fall back on if there's no value expression param
     * @param microProfileConfigKey a microprofile key.
     * @param expression the expression you are looking for
     * @return a config value.
     */
    public static String resolveConfigAttribute(String attribute, String expression, String microProfileConfigKey){
        if(expression==null || expression.isEmpty()){
            return resolveConfigAttribute(attribute, microProfileConfigKey);
        }else{
            return resolveConfigAttribute(expression, microProfileConfigKey);
        }
    }

    private static boolean isPayaraConfigFormat(String attribute) {
        return isELImmediateFormat(attribute);//payara format is the same as EL format.
    }

    private static boolean isELImmediateFormat(String attribute) {
        return attribute!=null && attribute.startsWith("${") && attribute.endsWith("}");
    }
    
    private static boolean isELDeferredFormat(String attribute) {
        return attribute!=null && attribute.startsWith("#{") && attribute.endsWith("}");
    }
    
}
