/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.jvnet.hk2.config.test;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.InjectionTarget;
import org.jvnet.hk2.config.NoopConfigInjector;

@Service(name = "web-container-availability", metadata = "target=org.jvnet.hk2.config.test.WebContainerAvailability,@availability-enabled=optional,@availability-enabled=default:true,@availability-enabled=datatype:java.lang.String,@availability-enabled=leaf,@persistence-type=optional,@persistence-type=default:replicated,@persistence-type=datatype:java.lang.String,@persistence-type=leaf,@persistence-frequency=optional,@persistence-frequency=default:web-method,@persistence-frequency=datatype:java.lang.String,@persistence-frequency=leaf,@persistence-scope=optional,@persistence-scope=default:session,@persistence-scope=datatype:java.lang.String,@persistence-scope=leaf,@persistence-store-health-check-enabled=optional,@persistence-store-health-check-enabled=default:false,@persistence-store-health-check-enabled=datatype:java.lang.Boolean,@persistence-store-health-check-enabled=leaf,@sso-failover-enabled=optional,@sso-failover-enabled=default:false,@sso-failover-enabled=datatype:java.lang.Boolean,@sso-failover-enabled=leaf,@http-session-store-pool-name=optional,@http-session-store-pool-name=datatype:java.lang.String,@http-session-store-pool-name=leaf,@disable-jreplica=optional,@disable-jreplica=default:false,@disable-jreplica=datatype:java.lang.Boolean,@disable-jreplica=leaf")
@InjectionTarget(WebContainerAvailability.class)
public class WebContainerAvailabilityInjector
    extends NoopConfigInjector
{


}
