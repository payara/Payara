/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.container.filter.CsrfProtectionFilter;
import com.sun.jersey.api.core.ResourceConfig;
import org.glassfish.admin.rest.resources.StatusGenerator;
import org.glassfish.admin.rest.resources.custom.ManagementProxyResource;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.container.EndpointRegistrationException;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.adapter.Reloader;
import org.glassfish.admin.rest.generator.ASMResourcesGenerator;
import org.glassfish.admin.rest.generator.client.ClientGenerator;
import org.glassfish.admin.rest.generator.ResourcesGenerator;
import org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider;
import org.glassfish.admin.rest.provider.ActionReportResultJsonProvider;
import org.glassfish.admin.rest.provider.ActionReportResultXmlProvider;
import org.glassfish.admin.rest.provider.BaseProvider;
import org.glassfish.admin.rest.resources.GeneratorResource;
import org.glassfish.admin.rest.resources.ReloadResource;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

/**
 *
 * @author ludovic champenois ludo@dev.java.net
 */
/**
 * Class that initialize the Jersey container. It is called via introspection from RestAdapter
 * so that RestAdapter does not depend on Jersey classes. This way, we gain 90ms at startup time
 * and load the jersey classes only at the very last time, when needed.
 * It is safe to have imports on com.sun.jersey.* APIs in this class.
 * @author ludo
 */
public class LazyJerseyInit implements LazyJerseyInterface {

    /**
     * Called via introspection in the RestAdapter service() method only when the GrizzlyAdapter is not initialized
     * @param classes set of Jersey Resources classes
     * @param sc the current ServerContext, needed to find the correct classpath
     * @return the correct GrizzlyAdapter
     * @throws EndpointRegistrationException
     */
    @Override
    public HttpHandler exposeContext(Set classes, ServerContext sc, Habitat habitat)
            throws EndpointRegistrationException {

        HttpHandler httpHandler = null;
        Reloader r = new Reloader();

        ResourceConfig rc = new DefaultResourceConfig(classes);
        rc.getMediaTypeMappings().put("xml", MediaType.APPLICATION_XML_TYPE);
        rc.getMediaTypeMappings().put("json", MediaType.APPLICATION_JSON_TYPE);
        rc.getMediaTypeMappings().put("html", MediaType.TEXT_HTML_TYPE);
        rc.getMediaTypeMappings().put("js", new MediaType("application", "x-javascript"));
        
        rc.getContainerRequestFilters().add(CsrfProtectionFilter.class);

        RestConfig restConf = ResourceUtil.getRestConfig(habitat);
        if (restConf != null) {
            if (restConf.getLogOutput().equalsIgnoreCase("true")) { //enable output logging
                rc.getContainerResponseFilters().add(LoggingFilter.class);
            }
            if (restConf.getLogInput().equalsIgnoreCase("true")) { //enable input logging
                rc.getContainerRequestFilters().add(LoggingFilter.class);
            }
            if (restConf.getWadlGeneration().equalsIgnoreCase("false")) { //disable WADL
                rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);
            }
        }
        else {
                 rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, Boolean.TRUE);          
        }

        rc.getProperties().put(ResourceConfig.PROPERTY_CONTAINER_NOTIFIER, r);
        rc.getClasses().add(ReloadResource.class);

        //We can only inject these 4 extra classes in Jersey resources...
        rc.getSingletons().add(new SingletonTypeInjectableProvider<Context, Reloader>(Reloader.class, r) {});
        rc.getSingletons().add(new SingletonTypeInjectableProvider<Context, ServerContext>(ServerContext.class, sc) {});
        rc.getSingletons().add(new SingletonTypeInjectableProvider<Context, Habitat>(Habitat.class, habitat) {});
        rc.getSingletons().add(new SingletonTypeInjectableProvider<Context, SessionManager>(SessionManager.class, habitat.getComponent(SessionManager.class)) {});

        //Use common classloader. Jersey artifacts are not visible through
        //module classloader
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader apiClassLoader = sc.getCommonClassLoader();
            Thread.currentThread().setContextClassLoader(apiClassLoader);
            httpHandler = ContainerFactory.createContainer(HttpHandler.class, rc);
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
        //add a rest config listener for possible reload of Jersey
        new RestConfigChangeListener(habitat, r, rc, sc);
        return httpHandler;
    }

    @Override
    public void reportError(Request req, Response res, int statusCode, String msg) {
        try {
            // TODO: There's a lot of arm waving and flailing here.  I'd like this to be cleaner, but I don't
            // have time at the moment.  jdlee 8/11/10
            RestActionReporter report = new RestActionReporter(); //getClientActionReport(req);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setActionDescription("Error");
            report.setMessage(msg);
            BaseProvider<ActionReportResult> provider;
            String type = getAcceptedMimeType(req);
            if ("xml".equals(type)) {
                res.setContentType("application/xml");
                provider = new ActionReportResultXmlProvider();
            } else if ("json".equals(type)) {
                res.setContentType("application/json");
                provider = new ActionReportResultJsonProvider();
            } else {
                res.setContentType("text/html");
                provider = new ActionReportResultHtmlProvider();
            }
            res.setStatus(statusCode);
            res.getOutputStream().write(provider.getContent(new ActionReportResult(report)).getBytes());
            res.getOutputStream().flush();
            res.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getAcceptedMimeType(Request req) {
        String type = null;
        String requestURI = req.getRequestURI();
        Set<String> acceptableTypes = new HashSet<String>() {

            {
                add("html");
                add("xml");
                add("json");
            }
        };

        // first we look at the command extension (ie list-applications.[json | html | mf]
        if (requestURI.indexOf('.') != -1) {
            type = requestURI.substring(requestURI.indexOf('.') + 1);
        } else {
            String userAgent = req.getHeader("User-Agent");
            if (userAgent != null) {
                String accept = req.getHeader("Accept");
                if (accept != null) {
                    if (accept.indexOf("html") != -1) {//html is possible so get it...
                        return "html";
                    }
                    StringTokenizer st = new StringTokenizer(accept, ",");
                    while (st.hasMoreElements()) {
                        String scheme = st.nextToken();
                        scheme = scheme.substring(scheme.indexOf('/') + 1);
                        if (acceptableTypes.contains(scheme)) {
                            type = scheme;
                            break;
                        }
                    }
                }
            }
        }

        return type;
    }

    @Override
    public Set<Class<?>> getResourcesConfigForMonitoring(Habitat habitat) {
        final Set<Class<?>> r = new HashSet<Class<?>>();
        r.add(org.glassfish.admin.rest.resources.MonitoringResource.class);

        r.add(org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider.class);
        r.add(org.glassfish.admin.rest.provider.ActionReportResultJsonProvider.class);
        r.add(org.glassfish.admin.rest.provider.ActionReportResultXmlProvider.class);

        return r;
    }

    @Override
    public Set<Class<?>> getResourcesConfigForManagement(Habitat habitat) {
        Class domainResourceClass = null;//org.glassfish.admin.rest.resources.generated.DomainResource.class;

        generateASM(habitat);
        try {
            domainResourceClass = Class.forName("org.glassfish.admin.rest.resources.generatedASM.DomainResource");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LazyJerseyInit.class.getName()).log(Level.SEVERE, null, ex);
        }

        final Set<Class<?>> r = new HashSet<Class<?>>();

        // uncomment if you need to run the generator:
        r.add(GeneratorResource.class);
        r.add(StatusGenerator.class);
        r.add(ClientGenerator.class);
        //r.add(ActionReportResource.class);

        r.add(domainResourceClass);
//        r.add(DomainResource.class);
        r.add(ManagementProxyResource.class);
        r.add(org.glassfish.admin.rest.resources.SessionsResource.class); 

        //TODO this needs to be added to all rest adapters that want to be secured. Decide on it after the discussion to unify RestAdapter is concluded
        r.add(org.glassfish.admin.rest.resources.StaticResource.class);

        //body readers, not in META-INF/services anymore
        r.add(org.glassfish.admin.rest.readers.FormReader.class);
        r.add(org.glassfish.admin.rest.readers.ParameterMapFormReader.class);
        r.add(org.glassfish.admin.rest.readers.JsonHashMapProvider.class);
        r.add(org.glassfish.admin.rest.readers.JsonPropertyListReader.class);
        r.add(org.glassfish.admin.rest.readers.JsonParameterMapProvider.class);

        r.add(org.glassfish.admin.rest.readers.XmlHashMapProvider.class);
        r.add(org.glassfish.admin.rest.readers.XmlPropertyListReader.class);

        //body writers
        r.add(org.glassfish.admin.rest.provider.ActionReportResultHtmlProvider.class);
        r.add(org.glassfish.admin.rest.provider.ActionReportResultJsonProvider.class);
        r.add(org.glassfish.admin.rest.provider.ActionReportResultXmlProvider.class);


        r.add(org.glassfish.admin.rest.provider.FormWriter.class);

        r.add(org.glassfish.admin.rest.provider.GetResultListHtmlProvider.class);
        r.add(org.glassfish.admin.rest.provider.GetResultListJsonProvider.class);
        r.add(org.glassfish.admin.rest.provider.GetResultListXmlProvider.class);

        r.add(org.glassfish.admin.rest.provider.OptionsResultJsonProvider.class);
        r.add(org.glassfish.admin.rest.provider.OptionsResultXmlProvider.class);

        return r;
    }

    private void generateASM(Habitat habitat) {
        try {
            Domain entity = habitat.getComponent(Domain.class);
            Dom dom = Dom.unwrap(entity);
            DomDocument document = dom.document;
            ConfigModel rootModel = dom.document.getRoot().model;

            ResourcesGenerator resourcesGenerator = new ASMResourcesGenerator(habitat);
            resourcesGenerator.generateSingle(rootModel, document);
            resourcesGenerator.endGeneration();
        } catch (Exception ex) {
            Logger.getLogger(GeneratorResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
