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
 */

package fish.payara.security.identitystores;

import fish.payara.security.annotations.YubikeyIdentityStoreDefinition;
import javax.enterprise.util.AnnotationLiteral;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.soteria.cdi.AnnotationELPProcessor;

import static org.glassfish.soteria.cdi.AnnotationELPProcessor.emptyIfImmediate;
import static org.glassfish.soteria.cdi.AnnotationELPProcessor.evalImmediate;

/**
 * A literal representation of the {@link  fish.payara.security.annotations.YubikeyIdentityStoreDefinitionDefinition} 
 * 
 * @author Mark Wareham
 */
@SuppressWarnings("AnnotationAsSuperInterface")
public class YubikeyIdentityStoreDefinitionAnnotationLiteral extends AnnotationLiteral<YubikeyIdentityStoreDefinition>
        implements YubikeyIdentityStoreDefinition {

    private final int yubikeyAPIClientID;
    private final String yubikeyAPIKey;
    private final int priority;
    private final String priorityExpression;
    private final String yubikeyAPIClientIDExpression;
    
    public YubikeyIdentityStoreDefinitionAnnotationLiteral(
            int yubikeyAPIClientID, String yubikeyAPIKey, int priority, String priorityExpression, String yubikeyAPIClientIDExpression) {
        
        this.yubikeyAPIClientID = (Integer) TranslatedConfigView.getTranslatedValue(yubikeyAPIClientID);
        this.yubikeyAPIKey = yubikeyAPIKey;
        this.priority = priority;
        this.priorityExpression = priorityExpression;
        this.yubikeyAPIClientIDExpression = yubikeyAPIClientIDExpression;
    }

    /**
     * Evaluates the provided object's fields for EL expressions
     * @param in the object to be evaluated
     * @return an evaluated object
     */
    public static YubikeyIdentityStoreDefinition eval(YubikeyIdentityStoreDefinition in) {
        //If any of the fields do not contain an EL expression, no need to eval
        if (!hasAnyELExpression(in)) {
            return in;
        }

        YubikeyIdentityStoreDefinitionAnnotationLiteral out
                = new YubikeyIdentityStoreDefinitionAnnotationLiteral(
                        evalImmediate(in.yubikeyAPIClientIDExpression(), in.yubikeyAPIClientID()), 
                        evalImmediate(in.yubikeyAPIKey()),
                        evalImmediate(in.priorityExpression(), in.priority()),
                        emptyIfImmediate(in.priorityExpression()),
                        emptyIfImmediate(in.yubikeyAPIClientIDExpression())
                        );
        return out;
    }

    private static boolean hasAnyELExpression(YubikeyIdentityStoreDefinition in) {
        return AnnotationELPProcessor.hasAnyELExpression(in.yubikeyAPIClientIDExpression()) 
                && AnnotationELPProcessor.hasAnyELExpression(in.yubikeyAPIKey())
                && AnnotationELPProcessor.hasAnyELExpression(in.priorityExpression());
    }

    @Override
    public int yubikeyAPIClientID() {
        return yubikeyAPIClientID;
    }

    @Override
    public String yubikeyAPIKey() {
        return yubikeyAPIKey;
    }

    @Override
    public int priority(){
        return priority;
    }
    
    @Override
    public String priorityExpression(){
        return priorityExpression;
    }

    @Override
    public String yubikeyAPIClientIDExpression() {
        return yubikeyAPIClientIDExpression;
    }


    

}
