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
package fish.payara.security.openid.http;

import static java.util.Objects.nonNull;
import java.util.Optional;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.Cookie;
import static org.glassfish.common.util.StringHelper.isEmpty;

/**
 *
 * @author Gaurav Gupta
 */
public class CookieController implements HttpStorageController {

    private final HttpMessageContext httpContext;

    public CookieController(HttpMessageContext httpContext) {
        this.httpContext = httpContext;
    }

    @Override
    public void store(String name, String value, Integer maxAge) {
        Cookie cookie = new Cookie(name, value);
        if (maxAge != null) {
            cookie.setMaxAge(maxAge);
        }
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        String contextPath = httpContext.getRequest().getContextPath();
        cookie.setPath(isEmpty(contextPath) ? "/" : contextPath);

        httpContext.getResponse().addCookie(cookie);
    }

    @Override
    public Optional<Cookie> get(String name) {
        if (httpContext.getRequest().getCookies() != null) {
            for (Cookie cookie : httpContext.getRequest().getCookies()) {
                if (name.equals(cookie.getName())
                        && nonNull(cookie.getValue())
                        && !cookie.getValue().trim().isEmpty()) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getAsString(String name) {
        return get(name).map(Cookie::getValue);
    }

    @Override
    public void remove(String name) {
        store(name, null, 0);
    }

}
