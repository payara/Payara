/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.internal.api;

import org.glassfish.hk2.runlevel.RunLevel;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Startup level for "post-boot" phase. This is for services that need to startup after the typical set of dependent
 * services have booted ({@link org.glassfish.api.StartupRunLevel}), but before application deployment has begun.
 * Most prominent use case for this startup level is for the service that executes post-boot commands
 * (e.g. {@linkplain com.sun.enterprise.v3.bootstrap.BootCommandService}); post-boot commands are typically asadmin
 * commands and so require most services to have started (e.g. so they can use
 * {@link org.glassfish.config.support.TranslatedConfigView} for variable substitution).
 * <br><br>
 * Note that post-boot command files can deploy applications themselves. Applications may therefore be getting deployed
 * at this run level, which will typically be before that of previously deployed applications
 * ({@link DeployPreviousApplicationsRunLevel}) and autodeploying applications
 * ({@link org.glassfish.internal.api.PostStartupRunLevel}).
 *
 * @author Andrew Pielage
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
@RunLevel(12)
public @interface PostBootRunLevel {
    int VAL = 12;
}
