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

package org.glassfish.api.admin;

import org.glassfish.hk2.api.Metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Annotation to define a supplemental command
 *
 * A supplemental command runs when a main command implementation is ran, it can be
 * used to attach behaviours to existing commands without modifying the original
 * implementation.
 *
 * <p>A supplemental command can be very useful to configure extra or external components
 * of an installation. For instance, a load-balancer module can be installed
 * when a load-balancer is added to a glassfish installation. Such module can contain
 * supplemental commands to supplement commands like "create-instance" in order to update
 * the load-balancer specific information.
 *
 * <p>An implementation must use the value() attribute of the @Supplemental annotation
 * to express the supplemented command. Its value is the name of the command as defined
 * by the supplemented command @Service annotation.
 *
 * <p>Example : take a command implementation
 * <pre>
 * <code>
 * &#64Service(name="randomCommand")
 * public MyRandomCommand implements AdminCommand {
 * ...
 * }
 * </code>
 * </pre>
 * <p>a supplemental command may be defined as follows :
 * <pre>
 * <code>
 * &#64Service(name="mySupCommand")
 * &#64Supplemental("randomCommand")
 * public MySupplementalCommand implements AdminCommand {
 * ...
 * }
 * </code>
 * </pre>
 * <p>
 * Another implementation that does not use the same parameter names as the
 * supplemented command will need to use a @Bridge annotation
 * <pre>
 * <code>
 * &#64Service(name="otherSupCommand")
 * &#64Supplemental(value="randomCommand" bridge=MyParameterMapper.class)
 * public OtherSupplementedCommand implements AdminCommand {
 * ...
 * }
 * </code>
 * </pre>
 * <p>There can be several supplemental commands for a command implementation.
 *
 * <p>A supplemental command can be executed in "isolation" (not part of the supplemented
 * command execution) and should not make any assumption that it is called as part
 * of its supplemented command execution. If a command should not be invokable in
 * isolation, it must not define a name() attribute on the @Service annotation :
 * <pre>
 * <code>
 * &#64Service
 * &#64Supplemental("randomCommand")
 * public MySupplementalCommand implements AdminCommand {
 *  // can only be invoked as a supplemental command
 * }
 * </code>
 * </pre>
 * <p>If a supplemental command is annotated with @Rollback, the annotation will be ignored
 * when the supplemental command is executed in isolation.
 *
 * <p>If a supplemental command is annotated with @Rollback, it is still subject to the
 * supplemented command {@link org.glassfish.api.admin.ExecuteOn#ifFailure()} value to decide
 * whether or not roll-backing should happen in case of failure.
 *
 * <p>When associating a supplemental command to a command X, it's always a good idea
 * to associate a roll-backing supplemental command to the rollbacking command of X.
 * For instance, if an "add-lb-config" supplemental command is attached to the
 * "create-instance" command, a "delete-lb-config" supplemental command should be
 * attached to the "delete-instance" command.
 *
 * @author Jerome Dochez
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Qualifier
public @interface Supplemental {

    /**
     * enumeration of when a supplemental command can be invoked with regards to the supplemented
     * command execution.
     */
    public enum Timing {Before, After, AfterReplication}

    /**
     * Name of the supplemented command as it can be looked up in the habitat.     *
     * Therefore the supplemented command must have the {@link AdminCommand} type and the
     * provided registration name.
     * 
     * @return habitat registration name for the command
     */
    @Metadata("target")
    public String value();

    public Class<? extends ParameterBridge> bridge() default ParameterBridge.NoMapper.class;

    /**
     * Supplemental commands can be run before or after the supplemented command.
     * Returns when this supplemental command is expecting its execution.
     *
     * @return Before if it should be run before the supplemented method
     * or After if it should run after the supplemented method.
     */
    public Timing on() default Timing.After;

    /**
     * Indicates to the framework what type of action should be taken if the
     * execution of this command was to return a failure exit code.
     * The action will apply on the supplemented command as well as all
     * supplemental commands.
     *
     * <p>If rollback is expected, the failure of this
     * supplemental command will cause the rollbacking of all the already
     * executed supplemented commands as well as the main supplemented command.
     *
     * @return the action the framework is expected to invoke when this
     * supplemental command execution failed.
     */
    public FailurePolicy ifFailure() default FailurePolicy.Error;

}
