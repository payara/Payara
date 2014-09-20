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

/**

<h2>Admin commands in GlassFish</h2>

 <h3>Basic Support</h3>
 
 Commands are annotated with {@link org.jvnet.hk2.annotations.Service} annotation and must implement the
 {@link org.glassfish.api.admin.AdminCommand} contract. Commands are looked up based on the @Service
 {@link org.jvnet.hk2.annotations.Service#name} attribute.

 <p>Commands can be singleton or instantiated at each invocation depending on the {@link org.jvnet.hk2.annotations.Scope}
 annotation. Singleton commands will be injected once for all {@link javax.inject.Inject} dependencies but
 will have all @{link org.glassfish.api.admin.Param} dependencies injected for each invocation.

 <p>more to be added to describe the v3 behaviors.
 

 <h3>Supplemental Commands</h3>

 A supplemental command is an administrative command that will supplement the execution of an existing command. Although most
 supplemental commands can execute in isolation, they usually represent added administrative tasks that can be
 optionally installed inside a GlassFish installation and need to be notified when higher level administrative
 requests are invoked.

 A supplemental command must be annotated with the {@link Supplemental} annotation.

 <p>For instance, a supplemental command might configure a particular load-balancer when instances are created or
 deleted within a cluster. Usually, this required complicated scripts to first create the instance and then invoke
 some subsequent commands to configure the load-balancer. With supplemental commands, the load-balancer supplemental
 command can attach itself to the create or delete instance methods and be automatically invoked by the administration
 framework when the supplemented command is invoked by the user.

 <p>A supplemental command usually run after the supplemented command has finished running successfully. If the
 supplemented command fail to execute, none of the supplemental commands should be invoked. A supplemental command can
 request to be invoked before the supplemented command. In such a case, these commands must have a
 {@link UndoableCommand} annotation to undo any changes in case the supplemented command fail to
 execute successfully. If a supplemental command is executing before and does not have {@link Rollbak} annotation, the
 system should flag this as an error, and prevent the supplemental method execution.
  <p>
 It might be possible for a supplemental command to fail executing and since such commands execute usually after the
 supplemented command has finished executing, a rollbacking mechanism can be described though the combination of the
 {@link org.glassfish.api.admin.ExecuteOn#ifFailure()} annotation value and the {@link UndoableCommand}}. If a clustered
 command requires rollbacking when any of its supplemented command fail then it must be annotated with {@link UndoableCommand}
 and all of its supplemented commands must also be annotated with {@link UndoableCommand}, otherwise, it must be flagged by
 the system as an error.
 <p>
 Supplemental commands can be a wrapper for existing external commands, and therefore can use a very different set
 of parameters than the ones provided to the supplemented command. Supplemental commands can use {@link org.glassfish.api.admin.Supplemental#bridge()}
 annotation value to translate or bridge the parameters names and values from the supplemented command to the supplemental
 ones.
 <p>

 <h3>Clustering support</h3>

 A command can be optionally annotated with {@link org.glassfish.api.admin.ExecuteOn} to specify the clustering
 support. A command not annotated with {@link ExecuteOn} will have a virtual @ExecuteOn annotation with the default values.
 (note to asarch, this is mainly for backward compatibility, can be revisited).

 <p>A Clustered command will be executed on the server receiving the command from the user (the DAS usually) and any
 of the remote instances identified by the {@link org.glassfish.api.admin.ExecuteOn#executor()} instance.

 <p>For a command to not be "cluster" available requires the following annotation :
 <pre><code>
 &#64ExecuteOn(RuntimeType.DAS)
 </code></pre>
 <p>Whether commands are executed in parallel or synchronously is immaterial to the user as long as he gets proper
 feedback on the remote commands executions successes or failures.

 <h3>Rollbacking</h3>

 Supplemental and clustered commands execute separately and can create issues when one of the invocation fails.
 Commands can optionally implement the {@link UndoableCommand} interface to allow for roll-backing a previously successful
 execution of an administrative change. 

 <p>In a clustering environment, any of the remote invocations can rollback the entire set of changes depending on the
 values of {@link org.glassfish.api.admin.ExecuteOn#ifFailure()} and {@link org.glassfish.api.admin.ExecuteOn#ifOffline()}
 annotation values.

 <p>A Supplemental command can force the roll-backing of the supplemented command using the {@link org.glassfish.api.admin.Supplemental#ifFailure()}
 annotation value.
*/

package org.glassfish.api.admin;
