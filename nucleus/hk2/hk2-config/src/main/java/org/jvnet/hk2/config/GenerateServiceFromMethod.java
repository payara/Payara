/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is put on user-supplied annotations in order to cause
 * the hk2-inhabitant-generator tool to create descriptors with certain
 * properties.
 * <p>
 * The user-supplied annotation must have the following properties:<UL>
 * <LI>Must only apply to methods (or classes also annotated with {@link Decorator})</LI>
 * <LI>Must be available at runtime</LI>
 * <LI>Must only be applied to interfaces marked with {@link Configured}</LI>
 * <LI>May have one or zero String fields marked with {@link GeneratedServiceName}</LI>
 * </UL>
 * Only methods of type java.util.List with a parameterized type (e.g. List<Config>) or
 * which take a single parameter and a void return type may be
 * annotated with the user-supplied annotation.  The parameterized actual type (or the type of
 * the parameter) will end up being used
 * as a field in the descriptor.  A single method may have multiple user-supplied annotations
 * marked with this annotation, in which case a different descriptor will be generated for each
 * user-supplied annotation.
 * <p>
 * When a method on an {@link Configured} interface has a user-supplied annotation
 * that is annotated with this interface the hk2-inhabitant-generator will generate
 * a service descriptor with the following properties:<UL>
 * <LI>The implementation class will be as specified in this annotation</LI>
 * <LI>The available contracts will be as specified in this annotation</LI>
 * <LI>The scope will be as specified in this annotation</LI>
 * <LI>If the user-supplied annotation has a field marked with {@link GeneratedServiceName} it will be the name in the descriptor</LI>
 * <LI>It will have a metadata entry with the name of the actual type of the List parameterized return type (or the single parametere type) with key METHOD_ACTUAL</LI>
 * <LI>It will have a metadata entry with the name of the method with key METHOD_NAME</LI>
 * <LI>It will have a metadata entry with the name of the parent {@link Configured} class with key PARENT_CONFGIURED</LI>
 * </UL>
 * <p>
 * If a class is annotated with a user-supplied annotation marked with this annotation then that
 * class *must* also be annotated with the {@link Decorate} annotation.  The {@link Decorate} annotation
 * will provide the values for several of the fields as described below.
 * <p>
 * When a class on an {@link Configured} interface has a user-supplied annotation
 * that is annotated with this interface the hk2-inhabitant-generator will generate
 * a service descriptor with the following properties:<UL>
 * <LI>The implementation class will be as specified in this annotation</LI>
 * <LI>The available contracts will be as specified in this annotation</LI>
 * <LI>The scope will be as specified in this annotation</LI>
 * <LI>The name will come from the field annotated with {@link GeneratedServiceName} from the user-supplied annotation defined by
 * the {@link Decorate#with()} method</LI>
 * <LI>It will have a metadata entry with the {@link Decorate#targetType()} value with key METHOD_ACTUAL</LI>
 * <LI>It will have a metadata entry with the {@link Decorate#methodName()} value with key METHOD_NAME</LI>
 * <LI>It will have a metadata entry with the name of the parent {@link Configured} class with key PARENT_CONFGIURED</LI>
 * </UL> 
 * 
 * @author jwells
 */
@Documented
@Retention(RUNTIME)
@Target( ANNOTATION_TYPE )
public @interface GenerateServiceFromMethod {
    /**
     * This is the key in the metadata that will contain the actual type of the List return type of the
     * method where the user-supplied annotation has been placed
     */
    public final static String METHOD_ACTUAL = "MethodListActual";
    
    /**
     * This is the key in the metadata that will contain the name of the method where the user-supplied
     * annotation has been placed
     */
    public final static String METHOD_NAME = "MethodName";
    
    /**
     * This is the key in the metadata that will contain the fully qualified class name of the class marked
     * {@link Configured} that contains this annotation
     */
    public final static String PARENT_CONFIGURED = "ParentConfigured";
    
    /**
     * This must have the fully qualified class name of the implementation that is to be used in the
     * generated descriptor
     * 
     * @return The fully qualified class name of the implementation
     */
    public String implementation();
    
    /**
     * The set of fully qualified class names of the advertised contracts that are to be used in
     * the generated descriptor.  Note that the implementation class is not automatically added
     * to this list
     * 
     * @return The fully qualified class names of the advertised contracts the generated descriptor
     * should take
     */
    public String[] advertisedContracts();
    
    /**
     * The scope that the descriptor should take.  Defaults to PerLookup
     * 
     * @return The fully qualified class names of the scope the descriptor should take
     */
    public String scope() default "org.glassfish.hk2.api.PerLookup";
}
