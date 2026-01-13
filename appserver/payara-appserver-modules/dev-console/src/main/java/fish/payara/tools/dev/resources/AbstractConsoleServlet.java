/*
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.tools.dev.resources;

import fish.payara.tools.dev.core.DevConsoleRegistry;
import fish.payara.tools.dev.core.DevConsoleService;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.util.Set;
import org.glassfish.internal.api.Globals;

public abstract class AbstractConsoleServlet extends HttpServlet {

    /**
     * Jsonb is thread-safe according to the spec and can be safely reused.
     */
    private static final Jsonb JSONB = JsonbBuilder.create();

    /**
     * @param response
     * @return true if request processing should continue
     * @throws java.io.IOException
     */
    protected boolean guard(HttpServletResponse response) throws IOException {
        DevConsoleService devConsoleService = Globals.getDefaultBaseServiceLocator().getService(DevConsoleService.class);

        if (!devConsoleService.isEnabled()) {
            response.sendError(SC_FORBIDDEN, "Dev Console Service is disabled");
            return false;
        }
        return true;
    }

    protected void writeJson(HttpServletResponse resp, Object dto)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        JSONB.toJson(dto, resp.getWriter());
    }

    protected DevConsoleRegistry getRegistry(String appName) {

        DevConsoleService service = Globals
                .getDefaultBaseServiceLocator()
                .getService(DevConsoleService.class);

        if (service == null) {
            return null;
        }

        if (appName != null && !appName.isBlank()) {
            return service.getRegistry(appName);
        }

        var all = service.getAllRegistries();

        if (all.size() == 1) {
            return all.values().iterator().next();
        }

        return null;
    }

    protected String resolveAppName(HttpServletRequest req) {
        String app = req.getParameter("app");
        if (app != null && !app.isBlank()) {
            return app;
        }

        // fallback: use the webapp hosting the servlet
        return req.getContextPath().replace("/", "");
    }

    public static <T> T getBean(BeanManager bm, Class<T> type) {
        Set<Bean<?>> beans = bm.getBeans(type);
        Bean<?> bean = bm.resolve(beans);

        CreationalContext<?> ctx = bm.createCreationalContext(bean);
        return (T) bm.getReference(bean, type, ctx);
    }

}
