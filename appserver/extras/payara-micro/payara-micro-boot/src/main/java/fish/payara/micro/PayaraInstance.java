/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.micro;

import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.micro.event.CDIEventListener;
import fish.payara.micro.event.PayaraClusterListener;
import fish.payara.micro.event.PayaraClusteredCDIEvent;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.jvnet.hk2.annotations.Contract;

/**
 * An instance of this class can be Injected into any application that is 
 * running on Payara Micro. It provides similar methods as the PayaraMicroRuntime
 * 
 * 
 * @author Steve Millidge (Payara Foundation)
 */
@Contract
public interface PayaraInstance {

    void addBootstrapListener(PayaraClusterListener listener);

    void addCDIListener(CDIEventListener listener);

    Map<String, Future<ClusterCommandResult>> executeClusteredASAdmin(String command, String... parameters);

    Map<String, Future<ClusterCommandResult>> executeClusteredASAdmin(Collection<String> memberGUIDs, String command, String... parameters);

    ClusterCommandResult executeLocalAsAdmin(String command, String... parameters);

    Set<InstanceDescriptor> getClusteredPayaras();

    InstanceDescriptor getDescriptor(String member);

    String getInstanceName();

    InstanceDescriptor getLocalDescriptor();

    /**
     * Checks whether or not this instance is in a Hazelcast cluster
     * @return true if this instance is in a Hazelcast cluster
     */
    boolean isClustered();

    void publishCDIEvent(PayaraClusteredCDIEvent event);

    void removeBootstrapListener(PayaraClusterListener listener);

    void removeCDIListener(CDIEventListener listener);

    <T extends Serializable> Map<String, Future<T>> runCallable(Collection<String> memberUUIDS, Callable<T> callable);

    <T extends Serializable> Map<String, Future<T>> runCallable(Callable<T> callable);

    void setInstanceName(String instanceName);
    
}
