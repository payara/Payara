/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.internal.embedded;

import org.glassfish.api.container.Sniffer;
import org.jvnet.hk2.annotations.Contract;

import java.util.List;

/**
 * Embedded container definition, although most containers will be bound
 * to a {@link Port} instance, it's not automatic, for instance JPA and
 * other non network based containers might not.
 *
 * @author Jerome Dochez
 */
@Contract
public interface EmbeddedContainer {

    /**
     * Binds a port using a specific protocol to this container.
     * @param port the port instance to bind
     * @param protocol the protocol the port should be used for, can
     * be null and the container can use the port for any protocol(s)
     * it needs to.
     */
    public void bind(Port port, String protocol);

    /**
     * Returns the list of sniffers associated with this container.
     *
     * @return a list of sniffers that will be used when application are
     * deployed to the embedded server.
     */
    public List<Sniffer> getSniffers();

    /**
     * Starts the embedded container instance
     *
     * @throws LifecycleException if the container cannot started
     */
    public void start() throws LifecycleException;

    /**
     * Stops the embedded container instance
     * 
     * @throws LifecycleException if the container cannot be stopped
     */
    public void stop() throws LifecycleException;

}
