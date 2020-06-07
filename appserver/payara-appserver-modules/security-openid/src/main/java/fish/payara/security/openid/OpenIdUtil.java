/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.openid;

import java.util.Optional;
import java.util.function.Predicate;
import javax.el.ELProcessor;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.Config;
import org.glassfish.config.support.TranslatedConfigView;

/**
 *
 * @author Gaurav Gupta
 */
public final class OpenIdUtil {

    public static final String DEFAULT_JWT_SIGNED_ALGORITHM = "RS256";

    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private OpenIdUtil() {
    }

    public static <T> T getConfiguredValue(Class<T> type, T value, Config provider, String mpConfigKey) {
        T result = value;
        Optional<T> configResult = provider.getOptionalValue(mpConfigKey, type);
        if (configResult.isPresent()) {
            return configResult.get();
        }
        if (type == String.class) {
            result = (T) TranslatedConfigView.expandValue((String) result);
        }
        if (type == String.class && isELExpression((String) value)) {
            ELProcessor elProcessor = new ELProcessor();
            BeanManager beanManager = CDI.current().getBeanManager();
            elProcessor.getELManager().addELResolver(beanManager.getELResolver());
            result = (T) elProcessor.getValue(toRawExpression((String) result), type);
        }
        return result;
    }

    public static boolean isELExpression(String expression) {
        return !expression.isEmpty() && isDeferredExpression(expression);
    }

    public static boolean isDeferredExpression(String expression) {
        return expression.startsWith("#{") && expression.endsWith("}");
    }

    public static String toRawExpression(String expression) {
        return expression.substring(2, expression.length() - 1);
    }

    public static <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }
}
