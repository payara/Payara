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
package org.glassfish.api.admin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;

/**
 * Allows command developers to declare what resources are affected by
 * the command and what actions must be authorized on each to allow the command
 * to execute. 
 * <p>
 * Use any or all of the following to control authorization:
 * <ul>
 * <li>
 * Use the {@code @AccessRequired} annotation at the class level to declare a resource 
 * name and action to be enforced; use {@code @AccessRequired.List} to declare
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
    
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Delegate {
        public Class value();
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
         * Name of the collection on the owner that will contain the new child.
         */
        public String collection() default "";
        
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
     * <p>
     * Note that the resource can be identified in one of several ways:
     * <ul>
     * <li>with the resource name
     * <li>with a resource {@code ConfigBean}
     * <li>with a {@code ConfigBean} parent and a child type
     * </ul>
     * <p>
     * Secure admin submits each {@code AccessCheck} to the authorization service
     * separately and records the result as {@link #isSuccessful} which can be
     * retrieved by commands that prepare their own access checks.
     * <p>
     * A command which prepares its own access checks can also indicate if a
     * failure of the access check should or should not be fatal to the overall
     * authorization operation.  This is useful, for example, in attempting to list
     * all tenants.  The command could prepare an {@code AccessCheck} for each
     * tenant of interest, marking each as non-fatal.  Because secure admin
     * records the success of each access check, the "list tenant {@code execute}
     * command can check each of its custom {@code AccessCheck}s and report
     * on only those accounts whose access checks succeeded.
     * <p>
     * Often, commands which prepare their own access checks need to associate
     * an object of some type with the access check.  As a convenience such
     * classes can optionally pass the related object to one of the constructors
     * which accepts it and then retrieve it later.  This helps avoid having to
     * extend AccessCheck as a private inner class in a command so as to link a
     * given AccessCheck with a given object of interest in the command.
     * 
     */
    public class AccessCheck<T> {
        
        /**
         * Implements a collection of the related objects in an AccessCheck 
         * collection.
         * <p>
         * Some classes which use collections of AccessChecks need to iterate
         * through the related objects associated with each of the AccessChecks.
         * Those classes can then use the AccessCheck.accessibleRelatedObjects() 
         * method, passing the typed collection of access checks.  
         * Normally the related object collection
         * will contain the related objects for only those access checks which
         * were successful but the caller can ask for all related objects 
         * if needed.
         * 
         * @param <U> the type of related object stored in the access checks in the collection
         */
        private static class RelatedObjectCollection<U> extends AbstractCollection<U> {

            private final Collection<AccessCheck<U>> accessChecks;
            private final boolean accessibleOnly;
            
            private RelatedObjectCollection(final Collection<AccessCheck<U>> accessChecks, final boolean accessibleOnly) {
                this.accessChecks = accessChecks;
                this.accessibleOnly = accessibleOnly;
            }
            
            @Override
            public Iterator<U> iterator() {
                return new Iterator<U>() {
                    
                    private final Iterator<AccessCheck<U>> baseIt = accessChecks.iterator();
                    
                    /* will be null if there is no matching next access check */
                    private AccessCheck<U> nextAccessCheck = chooseNext();
                    
                    private AccessCheck<U> chooseNext() {
                        while (baseIt.hasNext()) {
                            final AccessCheck<U> next = baseIt.next();
                            if ( ! accessibleOnly || next.isSuccessful) {
                                return next;
                            }
                        }
                        return null;
                    }

                    @Override
                    public boolean hasNext() {
                        return nextAccessCheck != null;
                    }

                    @Override
                    public U next() {
                        if (nextAccessCheck == null) {
                            throw new NoSuchElementException();
                        }
                        final U result = nextAccessCheck.relatedObject;
                        nextAccessCheck = chooseNext();
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                int count = 0;
                for (AccessCheck<U> accessCheck : accessChecks) {
                    if ( ! accessibleOnly || accessCheck.isSuccessful()) {
                        count++;
                    }
                }
                return count;
            }
        }
        
        /**
         * Returns an immutable collection of the related objects associated
         * with all of the specified access checks provided that the associated
         * access check was successful.
         * 
         * @param <U>
         * @param accessChecks the collection of access checks (with related objects) to process
         * @return 
         */
        public static <U> Collection<U> relatedObjects(final Collection<AccessCheck<U>> accessChecks) {
            return relatedObjects(accessChecks, true);
        }
        
        /**
         * Returns an immutable collection of the related objects associated
         * with all of the specified access checks, regardless of whether the
         * access was granted for each.
         * 
         * @param <U>
         * @param accessChecks the collection of access checks (with related objects) to process
         * @param successfulOnly whether to return the related objects for only the successful checks or for all checks
         * @return 
         */
        public static <U> Collection<U> relatedObjects(final Collection<AccessCheck<U>> accessChecks, final boolean successfulOnly) {
            return new RelatedObjectCollection<U>(accessChecks, successfulOnly);
        }
        
        
        private final String resourceName;
        private final String action;
        private final String note;
        private final Class<? extends ConfigBeanProxy> childType;
        private final ConfigBeanProxy parent;
        private final String childName;
        private final ConfigBeanProxy resource;
        private final boolean isFailureFatal;
        private boolean isSuccessful = false;
        private final T relatedObject;

        /**
         * Creates a new AccessCheck object linked with a given related object
         * that is of interest to the caller (typically a command).
         * @param relatedObject the related object to which this AccessCheck is linked
         * @param resourceName the resource being acted upon
         * @param action the action performed on the resource
         * @param note a note related to this resource/action pair
         * @param isFailureFinal if a failure of this AccessCheck should cause the entire authorization to fail
         */
        public AccessCheck(final T relatedObject,
                final String resourceName, 
                final String action, 
                final String note, 
                final boolean isFailureFinal) {
            this.relatedObject = relatedObject;
            this.resourceName = resourceName;
            this.action = action;
            this.note = note;
            childType = null;
            parent = null;
            this.isFailureFatal = isFailureFinal;
            resource = null;
            childName = null;
        }
                
        /**
         * Creates a new {@code AccessCheck}.
         * @param resourceName the resource to be checked
         * @param action the action on the resource
         * @param note descriptive note about the access check; used during logging
         * @param isFailureFinal whether a failure of this access check should cause the entire authorization to fail
         */
        public AccessCheck(final String resourceName, 
                final String action, 
                final String note, 
                final boolean isFailureFinal) {
            this(null, resourceName, action, note, isFailureFinal);
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param resourceName the name of the resource to be checked
         * @param action the action on the resource
         * @param note descriptive note about the access check; used during logging
         */
        public AccessCheck(final String resourceName, final String action, final String note) {
            this(resourceName, action, note, true /* isFailureFinal */);
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param resourceName the name of the resource to be checked
         * @param action the action on the resource
         * @param isFailureFinal whether a failure of this access check should force a failure of the entire authorization operation
         */
        public AccessCheck(final String resourceName, final String action, final boolean isFailureFinal) {
            this(resourceName, action, "", isFailureFinal);
        }
        
        /**
         * Creates a new {@code AccessCheck} with the specified related object.
         * @param relatedObject an object the commmand wants to associate with this AccessCheck
         * @param resourceName the resource to be checked
         * @param action the action on the resource
         * @param isFailureFinal whether a failure of this access check should force a failure of the entire authorization
         */
        public AccessCheck(final T relatedObject,
                final String resourceName, 
                final String action, 
                final boolean isFailureFinal) {
            this(relatedObject, resourceName, action, "", isFailureFinal);
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param resource the config bean that is the resource to check
         * @param action the action on the resource
         * @param isFailureFatal whether a failure of this access check should force a failure of the entire authorization operation
         */
        public AccessCheck(final ConfigBeanProxy resource,
                final String action,
                final boolean isFailureFatal) {
            this.resourceName = null;
            this.resource = resource;
            this.action = action;
            this.note = null;
            childType = null;
            parent = resource.getParent();
            this.isFailureFatal = isFailureFatal;
            childName = null;
            relatedObject = null;
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param resourceName the resource to be checked
         * @param action the action on the resource
         */
        public AccessCheck(final String resourceName, final String action) {
            this(resourceName, action, "", true /* isFailureFinal */);
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param parent the config bean of the parent resource to which a child is to be added
         * @param childType the type of the child to be added
         * @param action the action on the resource (typically "create")
         * @param note descriptive note about the access check; used during logging
         * @param isFailureFinal whether a failure of this access check should force a failure of the entire authorization operation
         */
        public AccessCheck(final ConfigBeanProxy parent, 
                final Class<? extends ConfigBeanProxy> childType, 
                final String action, 
                final String note,
                final boolean isFailureFinal) {
            this(parent, childType, null /* childName */, action, note, isFailureFinal);
        }
        
        public AccessCheck(final ConfigBeanProxy parent, 
                final Class<? extends ConfigBeanProxy> childType, 
                final String childName,
                final String action, 
                final String note,
                final boolean isFailureFinal) {
            this.parent = parent;
            this.childType = childType;
            this.action = action;
            this.note = note;
            this.resourceName = null;
            this.isFailureFatal = isFailureFinal;
            resource = null;
            this.childName = childName;
            relatedObject = null;
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param parent the config bean of the parent resource to which a child is to be added
         * @param childType the type of the child to be added
         * @param action the action on the resource (typically "create")
         * @param isFailureFinal whether a failure of this access check should force a failure of the entire authorization operation
         */
        public AccessCheck(final ConfigBeanProxy parent, 
                final Class<? extends ConfigBeanProxy> childType, 
                final String action,
                final boolean isFailureFinal) {
            this(parent, childType, action, "", isFailureFinal);
        }
        
        /**
         * Creates a new {@code AccessCheck}.
         * @param parent the config bean of the parent resource to which a child is to be added
         * @param childType the type of the child to be added
         * @param action the action on the resource (typically "create")
         */
        public AccessCheck(final ConfigBeanProxy parent, 
                final Class<? extends ConfigBeanProxy> childType, 
                final String action) {
            this(parent, childType, action, true);
        }
        
        /**
         * Creates a new {@code AccessCheck} (typically for an existing target child)
         * @param parent the parent of the config bean to be accessed
         * @param childType the type of the child config bean
         * @param childName the name of the child config bean
         * @param action the action on the resource
         */
        public AccessCheck(final ConfigBeanProxy parent,
                final Class<? extends ConfigBeanProxy> childType,
                final String childName,
                final String action) {
            this(Util.resourceNameFromConfigBeanTypeAndName(parent, childType, childName), action);
        }
        
        public T relatedObject() {
            return relatedObject;
        }
        
        /**
         * Returns the resource name, if any was set when the access check was created.
         * @return 
         */
        public String resourceName() {
            if (resource != null) {
                return Util.resourceNameFromConfigBeanProxy(resource);
            }
            if (parent != null) {
                if (childName == null) {
                    return Util.resourceNameFromConfigBeanType(parent, null, childType);
                } else {
                    return Util.resourceNameFromConfigBeanTypeAndName(parent, childType, childName);
                }
            }
            return resourceName;
        }
        
        /**
         * Returns the action for the access check.
         * @return 
         */
        public String action() {
            return action;
        }
        
        /**
         * Returns the type of the child to be added as part of a create-style
         * operation, as set when the access check was created.
         * @return 
         */
        public Class<? extends ConfigBeanProxy> childType() {
            return childType;
        }
        
        /**
         * Returns the parent config bean to which a child was to be added.
         * @return 
         */
        public ConfigBeanProxy parent() {
            return parent;
        }
        
        /**
         * Returns the note associated with the access check.
         * @return 
         */
        public String note() {
            return note;
        }
        
        /**
         * Returns the config bean to be acted upon 
         * @return 
         */
        public ConfigBeanProxy resource() {
            return resource;
        }
        
        /**
         * Returns whether a failure of this access check would automatically
         * trigger a failure of the entire authorization operation of which
         * it is a part.
         * @return 
         */
        public boolean isFailureFinal() {
            return isFailureFatal;
        }
        
        /**
         * Invoked by secure admin to record the result of performing the 
         * access check; <b>command developers should not typically use this
         * method themselves.</b>
         * @param passed 
         */
        public void setSuccessful(final boolean passed) {
            isSuccessful = passed;
        }
        
        /**
         * Returns whether the access check succeeded.
         * @return 
         */
        public boolean isSuccessful() {
            return isSuccessful;
        }
        
        /**
         * Formats the access check as a human-friendly string.
         * @return 
         */
        @Override
        public String toString() {
            return (new StringBuilder("AccessCheck ")).
                    append(resourceName()).
                    append("=").
                    append(action).
                    append(", isSuccessful=").
                    append(isSuccessful).
                    append(", isFailureFatal=").
                    append(isFailureFatal).
                    append("//").
                    append(note).
                    toString();
        }
    }
    
    
    
    /**
     * Utility methods used both from AccessCheck and from CommandSecurityChecker.
     */
    public static class Util {
        public static String resourceNameFromDom(Dom d) {
            Dom lastDom = null;
            final StringBuilder path = new StringBuilder();
            while (d != null) {
                if (path.length() > 0) {
                    path.insert(0, '/');
                }
                final ConfigModel m = d.model;
                lastDom = d;
                final String key = d.getKey();
                final String pathSegment = m.getTagName() + (key == null ? "" : "/" + key);
                path.insert(0, pathSegment);
                d = d.parent();
            }
            if (lastDom != null) {
                if (lastDom.getKey() != null) {
                    path.insert(0, pluralize(lastDom.model.getTagName()) + '/');
                }
            }
            return path.toString();
        }
        
        public static String resourceNameFromConfigBeanProxy(ConfigBeanProxy b) {
            return (b == null ? null : resourceNameFromDom(Dom.unwrap(b)));
        }
     
        public static String resourceNameFromConfigBeanType(
                final ConfigBeanProxy parent, 
                final String collectionName,
                final Class<? extends ConfigBeanProxy> childType) {
            return (parent == null ? null : resourceNameFromConfigBeanType(Dom.unwrap(parent), collectionName, childType));
            
        }
        
        public static String resourceNameFromConfigBeanType(
                final Dom parent, 
                String collectionName,
                final Class<? extends ConfigBeanProxy> childType) {
            final StringBuilder sb = new StringBuilder(resourceNameFromDom(parent)).append('/');
            final String tagName = parent.document.buildModel(childType).getTagName();
            if (collectionName != null) {
                if (collectionName.isEmpty()) {
                    collectionName = pluralize(tagName);
                }
                sb.append(collectionName).append('/');
            }
            sb.append(tagName);
            return sb.toString();
        }
        
        static String resourceNameFromConfigBeanTypeAndName(
                final ConfigBeanProxy parent, 
                final Class<? extends ConfigBeanProxy> childType,
                final String childName) {
            return resourceNameFromConfigBeanType(parent, null, childType) + (childName != null && ! childName.isEmpty() ? "/" + childName : "");
        }
        
        private static String pluralize(final String s) {
            final char lastChar = s.charAt(s.length() - 1);
            if (lastChar == 's' || lastChar == 'S' || lastChar == 'x' || lastChar == 'X') {
                return s + "es";
            } else {
                return s + "s";
            }
        }
    }
}
