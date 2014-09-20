/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import org.glassfish.api.I18n;
import org.jvnet.hk2.config.GenerateServiceFromMethod;
import org.jvnet.hk2.config.GeneratedServiceName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * List command annotation.
 *
 * Follow the same pattern as {@link Create} or {@link Delete} annotations
 * to generate a command implementation to list elements.
 *
 * Types of elements are listed are infered from the annotated method and
 * parent instance to get the list of elements from must be returned by
 * the resolver.
 *
 * See {@link Create} for initialization information
 *
 * @author Jerome Dochez
 */
@Retention(RUNTIME)
@Target(ElementType.METHOD)
@GenerateServiceFromMethod(implementation="org.glassfish.config.support.GenericListCommand",
                           advertisedContracts="org.glassfish.api.admin.AdminCommand")
public @interface Listing {

    /**
     * Name of the command that will be used to register this generic command implementation under.
     *
     * @return the command name as the user types it.
     */
    @GeneratedServiceName
    String value();

    /**
     * Returns the instance of the parent that should be used get the list of children.
     *
     * @return the parent instance.
     */
    Class<? extends CrudResolver> resolver() default CrudResolver.DefaultResolver.class;

    /**
     * Returns the i18n key that will be used to look up a localized string in the annotated
     * type module.
     *
     * @return the key to look up localized description for the command.
     */
    I18n i18n();    
}
