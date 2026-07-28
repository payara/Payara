/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.nucleus.microprofile.config;


import com.sun.enterprise.security.store.DomainScopedPasswordAliasStore;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

@Service
class RefreshingPasswordAliasStoreAccessor implements EventListener {
    // HK2 doesn't inject ServiceHandle<T>
    @Inject
    ServiceLocator locator;

    @Inject
    Events events;

    private volatile DomainScopedPasswordAliasStore currentStore;

    @PostConstruct
    void postConstruct() {
        events.register(this);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.DEPLOYMENT_START)) {
            DomainScopedPasswordAliasStore previousStore = currentStore;
            // we have no clear indication when a password alias is changed, but a good heuristics is
            // that it might get updated before deployment of an application. That way it is also
            // compatible with the previous behavior where the source would be created per application
            currentStore = null;
            if (previousStore != null) {
                locator.preDestroy(previousStore);
            }
        }
    }

    DomainScopedPasswordAliasStore getCurrentStore() {
        if (currentStore == null) {
            synchronized (this) {
                if (currentStore == null) {
                    currentStore = locator.getService(DomainScopedPasswordAliasStore.class);
                }
            }
        }
        return currentStore;
    }
}
