/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
 * Allows command developers to declare what resources are affected by
 * the command and what actions must be authorized on each to allow the command
 * to execute. 
 * <p>
 * Use any or all of the following to control authorization:
 * <ul>
 * <li>
 * Use the {@code @AccessRequired} annotation at the class level to declare a resource 
 * name and action to be enforced; use {@code @AccessRequired.List to declare
 * more than one combination of resources and actions.
 * <li>
 * Use the {@code @AccessRequired.To} annotation on a field that is a ConfigBean
 * to declare one or more actions to be enforced on the resource derived from that config bean.
 * <li>
 * Have the command class implement {@code @AccessRequired.Authorizer} which
 * prescribes the {@code isAuthorized} method
 * that will make authorization decisions internally, without help from the
 * command framework.
 * </ul>
 * The command processor will find all {@code @AccessRequired} annotations and
 * subannotations and make sure all of them pass before allowing the command
 * to proceed.  
 * 
 * 
 * @author tjquinn
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessRequired {
    /**
     * Name(s) of the resource(s) to which access should be checked.  The name
     * string can contain one or more tokens of the form ${fieldName} where "fieldName"
     * is a field on the same command class (or a superclass) that contains a non-null value.
     * If the referenced field is a ConfigBean then at runtime the resource 
     * name of that ConfigBean replaces the token.  Otherwise the field's 
     * {@code toString()} return value replaces the token.
     */
    public String[] resource();
    
    /**
     * One or more actions to authorize against the named resource.
     */
    public String[] action();
   
    /**
     * Declares multiple class-level {@code @AccessRequired} authorization steps,
     * typically each identifying different resources and/or different actions
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface List {
        public AccessRequired[] value();
    }
    
    /**
     * Declares access control on an existing, non-null {@code ConfigBean}.  
     * The system gets the name of the resource 
     * from the {@code ConfigBean} itself and authorizes the specified actions
     * using that resource.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface To {
        /**
         * Action(s) to be authorized on the ConfigBean
         * @return 
         */
        public String[] value();
    }
    
    /**
     * Declares access control for creating a new child {@code ConfigBean} in
     * a collection on an existing {@code ConfigBean}.  
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NewChild {
        /**
         * Type of the new {@code ConfigBean} to be created. 
         */
        public Class type();
        
        /**
         * Action(s) to be authorized, defaulted to "create."
         */
        public String[] action() default "create";
        
        /**
         * Declares multiple authorization checks for creating the same
         * single new {@code ConfigBean}.
         */
        @Target(ElementType.FIELD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface List {
            public NewChild[] value();
        }
    }
    
    /**
     * Represents an authorization check: a resource and an action to be 
     * authorized on that resource.
     */
    public class AccessCheck {
        private final String resource;
        private final String action;
        private final String note;
        private final Class<? extends ConfigBeanProxy> childType;
        private final ConfigBeanProxy parent;

        /**
         * Creates a new {@code AccessCheck}.
         * @param resource the resource to be checked
         * @param action the action on the resource
         * @param note descriptive note about the access check; used during logging
         */
        public AccessCheck(final String resource, final String action, final String note) {
            this.resource = resource;
            this.action = action;
            this.note = note;
            childType = null;
            parent = null;
        }
        
        /**
         * Creates a new {@code AccessCheck}
         * @param resource the resource to be checked
         * @param action the action on the resource
         */
        public AccessCheck(final String resource, final String action) {
            this(resource, action, "");
        }
        
        public AccessCheck(final ConfigBeanProxy parent, final Class<? extends ConfigBeanProxy> childType, final String action, final String note) {
            this.parent = parent;
            this.childType = childType;
            this.action = action;
            this.note = note;
            this.resource = null;
        }
        
        public AccessCheck(final ConfigBeanProxy parent, final Class<? extends ConfigBeanProxy> childType, final String action) {
            this(parent, childType, action, "");
        }
        
        public String resource() {
            return resource;
        }
        
        public String action() {
            return action;
        }
        
        public Class<? extends ConfigBeanProxy> childType() {
            return childType;
        }
        
        public ConfigBeanProxy parent() {
            return parent;
        }
        
        public String note() {
            return note;
        }
        
        @Override
        public String toString() {
            return (new StringBuilder("AccessCheck ")).
                    append((resource != null) ? resource : parent.toString()).
                    append((resource == null) ? "/" : "").
                    append((resource == null) ? childType.getName() : "").
                    append("=").
                    append(action).
                    append("//").
                    append(note).
                    toString();
        }
    }
    
    /**
     * Behavior required of all command classes which provide any of their
     * own custom authorization enforcement. The system will invoke the
     * class's {@code getAccessChecks} method after it has injected {@code @Inject}
     * and {@code @Param} fields and after it has invoked any {@code @PostConstruct}
     * methods.  The getAccessChecks method returns one or more {@link AccessCheck}
     * objects, indicating additional authorization checking that secure
     * admin should perform beyond what is triggered by the annotations.
     */
    public interface Authorizer {
        Collection<AccessCheck> getAccessChecks();
    }
    
    /**
     * Commands that need the AdminCommandContext before they can provide their
     * own access checks implement this interface.
     * <p>
     * The admin command context is passed on the execute method, which the 
     * system invokes after it invokes {@link Authorizer#getAccessChecks}.  If
     * the command class uses the admin command context in preparing its own
     * access checks it implements this interface and the command framework
     * passes it the command context, which the command can save for use during 
     * the {@code getAccessChecks} invocation.
     */
    public interface CommandContextDependent {
        void setCommandContext(Object adminCommandContext);
    }
}
