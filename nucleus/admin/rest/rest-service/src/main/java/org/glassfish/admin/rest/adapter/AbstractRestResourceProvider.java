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


package org.glassfish.admin.rest.adapter;

import java.io.Serializable;
import org.glassfish.admin.rest.RestConfigChangeListener;
import org.glassfish.admin.rest.resources.ReloadResource;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.common.util.admin.RestSessionManager;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.jettison.JettisonBinder;
import org.glassfish.jersey.media.multipart.MultiPartBinder;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.CsrfProtectionFilter;
import org.glassfish.jersey.server.filter.UriConnegFilter;
import org.jvnet.hk2.component.Habitat;

import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for various ReST resource providers
 *
 */
public abstract class AbstractRestResourceProvider implements RestResourceProvider, Serializable {
    // content of this class has been copied from RestAdapter.java
    protected Map<String, MediaType> mappings;
    protected Map<String, Boolean> features;

    protected AbstractRestResourceProvider() {
    }

    @Override
    public boolean enableModifAccessToInstances() {
        return false;
    }

    protected Map<String, MediaType> getMimeMappings() {
        if (mappings == null) {
            mappings = new HashMap<String, MediaType>();
            mappings.put("xml", MediaType.APPLICATION_XML_TYPE);
            mappings.put("json", MediaType.APPLICATION_JSON_TYPE);
            mappings.put("html", MediaType.TEXT_HTML_TYPE);
            mappings.put("js", new MediaType("text", "javascript"));
        }
        return mappings;
    }

    protected Map<String, Boolean> getFeatures() {
        if (features == null) {
            features = new HashMap<String, Boolean>();
            //    put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
        }
        return features;
    }

    protected AbstractBinder getJsonBinder() {
        return new JettisonBinder();
    }

    @Override
    public ResourceConfig getResourceConfig(Set<Class<?>> classes,
                                            final ServerContext sc,
                                            final Habitat habitat,
                                            final Class<? extends Factory<Ref<Subject>>> subjectReferenceFactory)
            throws EndpointRegistrationException {
        final Reloader r = new Reloader();

        ResourceConfig rc = new ResourceConfig(classes);

        //rc.services = habitat.getDefault();

        UriConnegFilter.enableFor(rc, getMimeMappings(), null);

        rc.addClasses(CsrfProtectionFilter.class);


//        TODO - JERSEY2
//        RestConfig restConf = ResourceUtil.getRestConfig(habitat);
//        if (restConf != null) {
//            if (restConf.getLogOutput().equalsIgnoreCase("true")) { //enable output logging
//                rc.getContainerResponseFilters().add(LoggingFilter.class);
//            }
//            if (restConf.getLogInput().equalsIgnoreCase("true")) { //enable input logging
//                rc.getContainerRequestFilters().add(LoggingFilter.class);
//            }
//            if (restConf.getWadlGeneration().equalsIgnoreCase("false")) { //disable WADL
//                rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
//            }
//        }
//        else {
//                 rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
//        }
//
        rc.addSingletons(r);
        rc.addClasses(ReloadResource.class);

        /**
         * JRW JRW
         *
         */
        rc.addBinders(getJsonBinder(), new MultiPartBinder(), new AbstractBinder() {

            @Override
            protected void configure() {
                AbstractActiveDescriptor<Reloader> descriptor = BuilderHelper.createConstantDescriptor(r);
                descriptor.addContractType(Reloader.class);
                bind(descriptor);

                AbstractActiveDescriptor<ServerContext> scDescriptor = BuilderHelper.createConstantDescriptor(sc);
                scDescriptor.addContractType(ServerContext.class);
                bind(scDescriptor);

                AbstractActiveDescriptor<Habitat> hDescriptor = BuilderHelper.createConstantDescriptor(habitat);
                hDescriptor.addContractType(Habitat.class);
                bind(hDescriptor);

                RestSessionManager rsm = habitat.getService(RestSessionManager.class);
                AbstractActiveDescriptor<RestSessionManager> rmDescriptor =
                        BuilderHelper.createConstantDescriptor(rsm);
                bind(rmDescriptor);

                bindFactory(subjectReferenceFactory).to(new TypeLiteral<Ref<Subject>>() {
                }).in(PerLookup.class);
                bindFactory(ReferencingFactory.<Subject>referenceFactory()).to(new TypeLiteral<Ref<Subject>>() {
                }).in(RequestScoped.class);

            }
        });

        rc.setProperty(MessageProperties.LEGACY_WORKERS_ORDERING, true);

        //add a rest config listener for possible reload of Jersey
        new RestConfigChangeListener(habitat, r, rc, sc);

        return rc;
    }

}
