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

package org.glassfish.api.admin;

import org.glassfish.api.Param;

import java.util.Collection;
import java.util.ArrayList;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.beans.Introspector;


/**
 * Model for an administrative command
 *
 * @author Jerome Dochez
 */
public abstract class CommandModel {

    /**
     * Returns the command name as it is typed by the user.
     *
     * @return the command name
     */
    public abstract String getCommandName();

    /**
     * Returns a localized description for this command
     *
     * @return a localized displayable description
     */
    public abstract String getLocalizedDescription();

    /**
     * Returns a localized usage text for this command or null if the usage
     * text should be generated from this model.
     *
     * @return the usage text
     */
    public abstract String getUsageText();

    /**
     * Returns the parameter model for a particular parameter
     *
     * @param paramName the requested parameter model name
     * @return the parameter model if the command supports a parameter of the passed
     * name or null if not.
     */
    public abstract ParamModel getModelFor(String paramName);

    /**
     * Returns a collection of parameter names supported by this admininstrative command
     *
     * @return all the command's paramter names.
     */
    public abstract Collection<String> getParametersNames();

    /**
     * Return the class that defines the command.  Normally this will be
     * the class that provides the implementation of the command, but for
     * generic CRUD commands it might be the config class that defines the
     * command.  The command class is used to locate resources related to
     * the command, e.g., the command's man page.  If the command model
     * isn't associated with a command class, null is returned.
     *
     * @return the command class, or null if none
     */
    public abstract Class<?> getCommandClass();
    
    /** This command is managed job. It is preferred to listen using SSE
     * in case of remote execution.
     * 
     * @return {@code true} only if command is @ManagedJob
     */
    public abstract boolean isManagedJob();

    /**
     * Return the cluster parameters for this command  or null if none are
     * specified and defaults should be used.
     *
     * @return a {@link ExecuteOn} annotation instance or null
     */
    public abstract ExecuteOn getClusteringAttributes();

    /**
     * Add a ParamModel for this command
     * @param model the new param model to be added
     */
    public abstract void add(ParamModel model);

    /**
     * Returns a collection of parameter model for all the parameters supported
     * by this command.
     *
     * @return the command's parameters models.
     */
    public Collection<ParamModel> getParameters() {
        ArrayList<ParamModel> copy = new ArrayList<ParamModel>();
        for (String name : getParametersNames()) {
            copy.add(getModelFor(name));
        }
        return copy;
    }

    /**
     * Get the Param name.  First it checks if the annotated Param
     * includes a name, if not then it gets the name from the field.
     * If the parameter is a password, add the prefix and change the
     * name to upper case.
     *
     * @param param class annotation
     * @param annotated annotated field or method
     * @return the name of the param
     */
    public static String getParamName(Param param, AnnotatedElement annotated) {
        String name = param.name();
        if (name.equals("")) {
            if (annotated instanceof Field) {
                name = ((Field) annotated).getName();
            }
            if (annotated instanceof Method) {
                if ( ((Method) annotated).getName().startsWith("is")) {
                   name = ((Method) annotated).getName().substring(2);
                } else {
                   name = ((Method) annotated).getName().substring(3);
                }
                name = Introspector.decapitalize(name);
            }
        }
        return name;
    }

    /**
     * Model for a command parameter.
     *
     */
    public static abstract class ParamModel {

        /**
         * Returns the command parameter name.
         *
         * @return the command parameter name
         */
        public abstract String getName();

        /**
         * Returns the command @Param annotation values.
         *
         * @return the @Param instance for this parameter
         */
        public abstract Param getParam();

        /**
         * Returns a localized description for this parameter
         *
         * @return a localized String
         */
        public abstract String getLocalizedDescription();
        
        /**
         * Returns a localized prompt for this parameter
         * 
         * @return a localized String
         */
        public abstract String getLocalizedPrompt();
 
        /**
         * Returns a localized confirmation prompt for this parameter. This is
         * only used for passwords.
         * 
         * @return a localized String
         */
        public abstract String getLocalizedPromptAgain();

        /**
         * Returns the parameter type.
         *
         * @return the parameter type.
         */
        public abstract Class getType();

        public boolean isParamId(String key) {
            if (getParam().primary()) {
                return "DEFAULT".equals(key) || getName().equalsIgnoreCase(key);
            }
            
            return getName().equalsIgnoreCase(key) ||
		    getParam().shortName().equals(key) ||
		    getParam().alias().equalsIgnoreCase(key);
        }

    }

    /**
     * Should an unknown option be considered an operand by asadmin?
     * @return true if unknown options are operands.
     */
    public boolean unknownOptionsAreOperands() {
	    return false;	// default implementation
    }
    
}
