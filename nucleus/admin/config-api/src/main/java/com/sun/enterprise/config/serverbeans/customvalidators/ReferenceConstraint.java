/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.config.serverbeans.customvalidators;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
import javax.validation.Constraint;
import javax.validation.Payload;
import org.jvnet.hk2.config.ConfigBeanProxy;

/** Annotated {@code ConfigBeanProxy} class contains at least one {@code String} 
 * field, which value must point to key attribute of some other existing 
 * {@code ConfigBeanProxy} instance.<br/>
 * Use {@link ReferenceConstraint.RemoteKey} annotation on appropriate getters 
 * to define such fields.<br/>
 * This constraint is supported for {@code ConfigBeanProxy} only.
 *
 * @author Martin Mares
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Constraint(validatedBy = ReferenceValidator.class)
public @interface ReferenceConstraint {
    String message() default "Invalid reference in provided configuration.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /** 
     * In Payara a lot of configurations are made in batch and its references
     * could not be fulfilled during creation process.
     * @return 
     */
    boolean skipDuringCreation();
    
    /** This annotation gets set only on getter method and in combination with 
     * {@link ReferenceConstraint} annotation on the class.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface RemoteKey {
        String message() default "";
        /** Type of {@code ConfigBeanProxy} where this remote key points to.
         * @return 
         */
        Class<? extends ConfigBeanProxy> type(); 
    }
}
