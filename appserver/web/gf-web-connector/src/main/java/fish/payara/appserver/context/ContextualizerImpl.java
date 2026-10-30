/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.appserver.context;

import org.glassfish.internal.api.ContextProducer;
import org.glassfish.internal.api.Contextualizer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.glassfish.internal.api.JavaEEContextUtil;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author lprimak
 */
@Service
public class ContextualizerImpl implements Contextualizer {
    @Inject
    private JavaEEContextUtil ctxUtil;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T contextualize(T object, Class<T> intf) {
        return contextualize(object, ctxUtil.currentInvocation(), Stream.of(intf));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T contextualize(T object, ContextProducer.Instance context, Class<T> intf) {
        return contextualize(object, context, Stream.of(intf));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T contextualize(T object, ContextProducer.Instance context, Stream<Class<?>> interfaces) {
        return (T) Proxy.newProxyInstance(ctxUtil.getInvocationClassLoader(), interfaces.toArray(Class<?>[]::new),
                new InvocationHandlerImpl(context, object));
    }

    public class InvocationHandlerImpl implements InvocationHandler {
        private final ContextProducer.Instance inv;
        private final Object delegate;
        private boolean ignore;


        private InvocationHandlerImpl(ContextProducer.Instance currentInvocation, Object delegate) {
            this.inv = currentInvocation;
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try (ContextProducer.Context ctx = inv.pushRequestContext()) {
                if (!ignore && ctx.isValid()) {
                    return method.invoke(delegate, args);
                } else {
                    // first time the context is invalid, never run again!
                    // class loader is fixed the moment it leaks, so the
                    // subsequent calls would be with the correct class loader
                    // however delegate would be still out of date
                    ignore = true;
                    return null;
                }
            }
        }
    }
}
