/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api;

import org.jvnet.hk2.config.Attribute;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Param is a parameter to a command. This annotation can be placed on a field or
 * setter method to identify the parameters of a command and have those parameters
 * injected by the system before the command is executed.
 *
 * The system will check that all non optional parameters are satisfied before invoking
 * the command.
 *
 * @author Jerome Dochez
 */
@Retention(RUNTIME)
@Target({METHOD,FIELD})
public @interface Param {

    /**
     * Returns the name of the parameter as it has be specified by the client when invoking
     * the command. By default the name is deducted from the name of the annotated element.
     * If the annotated element is a field, it is the filed name.
     * If the annotated element is a method, it is the JavaBeans property name from the setter
     * method name
     *
     * @return the parameter name.
     */
    public String name() default "";

    /**
     * Returns a list of comma separated acceptable values for this parameter. The system
     * will check that one of the value is used before invoking the command.
     *
     * @return the list of comma separated acceptable values
     */
    public String acceptableValues() default "";

    /**
     * Returns true if the parameter is optional to the successful invocation of the command
     * @return true if the parameter is optional
     */
    public boolean optional() default false;

    /**
     * Returns the short name associated with the parameter so that the user can specify
     * -p as well as -password when invoking the command.
     *
     * @return the parameter short name
     */
    public String shortName() default "";

    /**
     * Returns true if this is the primary parameter for the command which mean that the
     * client does not have to pass the parameter name but just the value to the command.
     *
     * @return true if this is the primary command parameter.
     */
    public boolean primary() default false;

    /**
     * Returns the default value associated with the parameter
     *  so that the user can specify
     *
     * @return the parameter default value
     */
    public String defaultValue() default "";
    
    /**
     * Returns a class that calculates the default value associated with the 
     * parameter.
     * 
     * @return a parameter default value calculator
     */
    public Class<? extends ParamDefaultCalculator> defaultCalculator() default ParamDefaultCalculator.class;
    
    /**
     * Returns true if the parameter is a password
     * @return true if the parameter is password
     */
    public boolean password() default false;

    /**
     * Returns the character used to separate items in a list.
     * Applies to parameters of type List, Properties, and String[].
     * The default separator is comma.
     *
     * @return the separator character
     */
    public char separator() default ',';

    /**
     * Returns true if multiple instances of the parameter are allowed.
     *
     * @return true if multiple instances are allowed
     */
    public boolean multiple() default false;

    /**
     * Returns true if this parameter is obsolete.
     * Obsolete parameters produce warnings when used in asadmin,
     * are ignored, and are not included in the command usage.
     *
     * @return true if the parameter is obsolete
     */
    public boolean obsolete() default false;

    /**
     * Returns an alias for the option.
     * This supports renaming options and supporting both the old name
     * and the new name.
     *
     * @return the parameter alias
     */
    public String alias() default "";
}
