/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.healthcheck.config;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.InjectionTarget;
import org.jvnet.hk2.config.NoopConfigInjector;

/**
 * HK2 config injector for {@link MicroprofileHealthCheckConfiguration}.
 *
 * Normally generated at compile time by the {@code config-generator} annotation processor.
 * Manually maintained here because the AP does not generate source files when the module
 * is compiled as a JPMS named module (module-info.java present). Must be kept in sync
 * with the {@code @Attribute} declarations on {@link MicroprofileHealthCheckConfiguration}.
 * Attributes are listed alphabetically in the metadata string, matching AP output order.
 */
@Service(name = "microprofile-healthcheck-configuration",
        metadata = "@enabled=optional,@enabled=default:true,@enabled=datatype:java.lang.Boolean,@enabled=leaf,"
                + "@endpoint=optional,@endpoint=default:health,@endpoint=datatype:java.lang.String,@endpoint=leaf,"
                + "@roles=optional,@roles=default:microprofile,@roles=datatype:java.lang.String,@roles=leaf,"
                + "@security-enabled=optional,@security-enabled=default:false,@security-enabled=datatype:java.lang.Boolean,@security-enabled=leaf,"
                + "@virtual-servers=optional,@virtual-servers=default:,@virtual-servers=datatype:java.lang.String,@virtual-servers=leaf,"
                + "target=fish.payara.microprofile.healthcheck.config.MicroprofileHealthCheckConfiguration")
@InjectionTarget(MicroprofileHealthCheckConfiguration.class)
public class MicroprofileHealthCheckConfigurationInjector extends NoopConfigInjector {

}