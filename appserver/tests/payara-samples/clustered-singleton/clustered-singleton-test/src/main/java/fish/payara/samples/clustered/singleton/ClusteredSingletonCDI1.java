/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.clustered.singleton;

import fish.payara.samples.clustered.singleton.api.SingletonAPI;
import fish.payara.cluster.Clustered;
import fish.payara.cluster.DistributedLockType;
import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

/**
 * @author lprimak
 */
@ApplicationScoped
@Clustered(keyName = "ClusteredSingletonCDI1", lock = DistributedLockType.LOCK)
@Default
public class ClusteredSingletonCDI1 implements SingletonAPI, Serializable {
    private static final Logger log = Logger.getLogger(ClusteredSingletonCDI1.class.getName());
    private static final long serialVersionUID = 1L;
    protected final SingletonCommon sc = new SingletonCommon(this);

    @Override
    public String getHello() {
        return String.format("CDI Bean Hello (1): %s", sc);
    }

    @PostConstruct
    void postConstruct() {
        log.info("CDI1 PostConstruct");
    }

    @PreDestroy
    void preDestroy() {
        log.info("CDI1 PreDestroy");
    }

    @Override
    public void randomizeState() {
        this.sc.randomizeState();
    }

    @Override
    public UUID getState() {
        return this.sc.getState();
    }
}
