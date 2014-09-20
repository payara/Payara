/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.api.admin.ExecuteOn;
import org.jvnet.hk2.config.GenerateServiceFromMethod;
import org.jvnet.hk2.config.GeneratedServiceName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Delete command annotation
 *
 * A method annotated with the Delete annotation will generate a generic
 * implementation of a delete administrative command responsible for
 * delete instances of the type as referenced by the annotated method.
 *
 * The deleted type is determine by the annotated method which must
 * follow one of the two following patterns :
 *      List<X> getXs();
 * or
 *      void setX(X x);
 *
 * X is the type of instance that will be deleted, the name of the
 * method is actually immaterial. So for example, the following
 * <pre><code>
 * @Delete
 * public List<Foo> getAllMyFoos();
 * @Delete
 * public void setMySuperFoo(Foo foo);
 * @Delete
 * public List<Foo> getFoos();
 * @Delete
 * public List<Foo> getFoo();
 * </code></pre>
 *
 * will all be valid annotated methods.
 *
 * The instance to be removed from the parent must be resolved by the
 * resolver attribute. The resolver can use injection or the command
 * invocation parameters (using the {@link org.glassfish.api.Param}
 * annotation) to get enough information to look up or retrieve the
 * instance to be deleted.
 *
 * Usually, most instances can be looked up by using the instance type
 * and its key (provided by the --name or --target parameters for instance).
 *
 * See {@link Create} for initialization information
 * 
 * @author Jerome Dochez
 */
@Retention(RUNTIME)
@Target(ElementType.METHOD)
@GenerateServiceFromMethod(implementation="org.glassfish.config.support.GenericDeleteCommand",
                           advertisedContracts="org.glassfish.api.admin.AdminCommand")
public @interface Delete {

    /**
     * Name of the command that will be used to register this generic command implementation under.
     *
     * @return the command name as the user types it.
     */    
    @GeneratedServiceName
    String value();

   /**
     * Returns the instance of the configured object that should be deleted.
     * The implementation of that interface can use the command parameters
     * to make a determination about which instance should be used.
     *
     * @return the instance targeted for deletion.
     */
    Class<? extends CrudResolver> resolver() default CrudResolver.DefaultResolver.class;

    /**
     * Returns a decorator type that should be looked up and called when a
     * configuration element of the annotated type is deleted.
     *
     * @return a deletion decorator for the annotated type
     */
    Class<? extends DeletionDecorator> decorator() default DeletionDecorator.NoDecoration.class;    

    /**
     * Returns the desired behaviors in a clustered environment. By default, using all the
     * {@link org.glassfish.api.admin.ExecuteOn} default values
     *
     * @return the cluster information
     */
    ExecuteOn cluster() default @ExecuteOn();

    /**
     * Returns the i18n key that will be used to look up a localized string in the annotated
     * type module.
     *
     * @return the key to look up localized description for the command.
     */
    I18n i18n();    
    
}
