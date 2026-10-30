/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.security.realm;

import com.sun.enterprise.admin.report.PlainTextActionReporter;
import com.sun.enterprise.v3.admin.InserverCommandRunnerHelper;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import static java.util.stream.Collectors.joining;
import jakarta.el.ELProcessor;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.Config;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.EmbeddedSystemAdministrator;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author Gaurav Gupta
 */
public interface RealmUtil {

    String JAAS_CONTEXT = "jaas-context";

    String ASSIGN_GROUPS = "assign-groups";

    static <T> T getConfiguredValue(Class<T> type, T value, Config provider, String mpConfigKey) {
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

    static boolean isELExpression(String expression) {
        return !expression.isEmpty() && isDeferredExpression(expression);
    }

    static boolean isDeferredExpression(String expression) {
        return expression.startsWith("#{") && expression.endsWith("}");
    }

    static String toRawExpression(String expression) {
        return expression.substring(2, expression.length() - 1);
    }

    static <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }

    static void createAuthRealm(String name, String realmClass, String loginModule, Properties props) {
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        InserverCommandRunnerHelper commandRunnerHelper = serviceLocator.getService(InserverCommandRunnerHelper.class);
        EmbeddedSystemAdministrator administrator = serviceLocator.getService(EmbeddedSystemAdministrator.class);
        ParameterMap parameters = new ParameterMap();
        parameters.insert("authrealmname", name);
        parameters.insert("property", props.entrySet()
                .stream()
                .map(prop -> escapeRealmProperty(prop.getKey().toString()) + '=' + escapeRealmProperty(prop.getValue().toString()))
                .collect(joining(":"))
        );
        parameters.insert("classname", realmClass);
        if (loginModule != null) {
            parameters.insert("login-module", loginModule);
        }
        ActionReport report = new PlainTextActionReporter();
        ActionReport outreport = commandRunnerHelper.runCommand("create-auth-realm", parameters, report, administrator.getSubject());
        if (outreport.getActionExitCode() == ActionReport.ExitCode.FAILURE) {
            throw new IllegalStateException("Error in creating Auth realm: " + name);
        }
    }
    
    static String escapeRealmProperty(String component) {
        return component
                .replaceAll("\\:", "\\\\:")
                .replaceAll("\\=", "\\\\=");
    }
}
