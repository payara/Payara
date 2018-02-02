/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * Implementation of this abstract class are handling injection resolution
 * for a particular injection annotation {@see Inject}
 *
 * Injection targets are identified by the generic parameter and the constructor
 * of this class. Potential injection targets are fields and methods of the
 * injected type.
 *
 * @param <U> U is the annotation used to identify the injection targets.
 *
 * @author Jerome Dochez
 */
public abstract class InjectionResolver<U extends Annotation> implements InjectionResolverQuery {
    
    public final Class<U> type;

    /**
     * Construct a resolver with a particular injection type
     * @param type the injection annotation type
     */
    public InjectionResolver(Class<U> type) {
        this.type = type;
    }

    /**
     * Returns the setter method responsible for setting the resource identified by the
     * passed annotation on the passed annotated method.
     *
     * This is useful when the annotation is specified on the getter for instance (due
     * to external specification requirements for instance) while the setter should be used if
     * values must be set using this injection resolver.
     *
     * By default, the setter method is the annotated method.
     *
     * @param annotated is the annotated {@link java.lang.reflect.Method}
     * @param annotation the annotation on the method
     * @return the setter method to use for injecting the annotation identified resource
     */
    public Method getSetterMethod(Method annotated, U annotation) {
        return annotated;
    }

    /**
     * Returns true if the resolution of this injection identified by the
     * passed annotation instance is optional
     * @param annotated is the annotated java element {@link java.lang.reflect.Method}
     * or {@link java.lang.reflect.Field}
     * @param annotation the injection metadata
     * @return true if the {@see getValue()} can return null without generating a
     * faulty injection operation
     */
    public boolean isOptional(AnnotatedElement annotated, U annotation) {
        return false;
    }

}
