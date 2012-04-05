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
package org.glassfish.paas.admin;

import com.sun.enterprise.config.serverbeans.Config;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.api.admin.config.Container;
import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

/**
 * This is the config bean for all the Cloud related services
 * which will be  present under the config element in domain.xml
 *
 * This config bean contains the configuration specific information for
 * different CPAS services like TenantManager, ElasticityEngine etc.
 *
 * @author Bhakti Mehta
 */
@Configured
public interface CloudServices extends ConfigBeanProxy, Injectable,ConfigExtension {

    @Element("*")
    public List<CloudService> getCloudServices();

    @DuckTyped
    <T extends CloudService> T getCloudServiceByType(Class<T> type);

    @DuckTyped
    <T extends CloudService> T createDefaultChildByType(Class<T> type);



    class Duck {

        public static <T extends CloudService> T getCloudServiceByType(CloudServices c, Class<T> type) {
            for (CloudService extension : c.getCloudServices()) {
                try {
                    return type.cast(extension);
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;

        }


        public static  <T extends CloudService>
        T createDefaultChildByType(CloudServices cloudSer,Class<T> p)
                throws TransactionFailure {

            final Class<T> parentElem = p;

            ConfigSupport.apply(new SingleConfigCode<CloudServices>() {

                @Override
                public Object run(CloudServices parent) throws PropertyVetoException, TransactionFailure {
                    T child = parent.createChild(parentElem);
                    parent.getCloudServices().add(child);
                    return child;
                }
            }, cloudSer);
            return cloudSer.getCloudServiceByType(p);
        }



    }

}


