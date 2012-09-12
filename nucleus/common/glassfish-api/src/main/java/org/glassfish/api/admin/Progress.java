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

/** ProgressStatus of a command.  Indicates this command generates 
 * progress status as it executes.  Use this annotation to inject a 
 * {@link org.glassfish.api.admin.ProgressStatus} instance.  The 
 * ProgressStatus object can be used to asynchronously generate ongoing 
 * progress messages and command completion information.
 *
 * A Command annotated with @Progress will also be a ManagedJob which will be
 * managed by the Job Manager
 * @see org.glassfish.api.admin.ProgressStatus
 * @see org.glassfish.api.admin.JobManager
 * @author mmares
 * @author Bhakti Mehta
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@ManagedJob
public @interface Progress {
    
    /** Optional: Context of the progress.   Generally this is the command
     * name.  The name will be included in the command's progress output.  
     * Default: command name or the server instance name for replicated 
     * commands.  
     */
    public String name() default "";
    
    /** Number of steps necessary to complete the operation. 
     * Value is used to determine percentage of work completed and can be 
     * changed using {@code ProgressStatus.setTotalStepCount}
     * If the step count is not established then a completion percentage
     * will not be included in the progress output.
     * 
     * @see org.glassfish.api.admin.ProgressStatus
     */
    public int totalStepCount() default -1;
    
}
