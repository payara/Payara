/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.microprofile.metrics.cdi.extension;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public final class AnnotatedTypeDecorator<X> extends AnnotatedDecorator implements AnnotatedType<X> {

    private final AnnotatedType<X> decoratedType;

    private final Set<AnnotatedMethod<? super X>> decoratedMethods;

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Annotation decoratingAnnotation) {
        this(decoratedType, decoratingAnnotation, Collections.<AnnotatedMethod<? super X>>emptySet());
    }

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Set<AnnotatedMethod<? super X>> decoratedMethods) {
        super(decoratedType, Collections.emptySet());
        this.decoratedType = decoratedType;
        this.decoratedMethods = decoratedMethods;
    }
    
    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Annotation decoratingAnnotation,
            Set<AnnotatedMethod<? super X>> decoratedMethods) {
        super(decoratedType, Collections.singleton(decoratingAnnotation));
        this.decoratedType = decoratedType;
        this.decoratedMethods = decoratedMethods;
    }

    @Override
    public Class<X> getJavaClass() {
        return decoratedType.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return decoratedType.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        Set<AnnotatedMethod<? super X>> methods = new HashSet<>(decoratedType.getMethods());
        for (AnnotatedMethod<? super X> method : decoratedMethods) {
            methods.remove(method);
            methods.add(method);
        }

        return Collections.unmodifiableSet(methods);
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return decoratedType.getFields();
    }
}
