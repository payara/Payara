/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.composite;

import com.sun.enterprise.v3.common.ActionReporter;
import java.net.URI;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.security.auth.Subject;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilderException;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.OptionsCapable;
import org.glassfish.admin.rest.RestResource;
import org.glassfish.admin.rest.composite.metadata.DefaultsGenerator;
import org.glassfish.admin.rest.composite.metadata.RestResourceMetadata;
import org.glassfish.admin.rest.model.ResponseBody;
import org.glassfish.admin.rest.resources.AbstractResource;
import org.glassfish.admin.rest.utils.DetachedCommandHelper;
import org.glassfish.admin.rest.utils.SseCommandHelper;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.media.sse.EventOutput;


/**
 * This is the base class for all composite resources. It provides all of the basic configuration and utilities needed
 * by composites.  For top-level resources, the <code>@Path</code> and <code>@Service</code> annotations are still
 * required, though, in order for the resource to be located and configured properly.
 * @author jdlee
 */
@Produces(Constants.MEDIA_TYPE_JSON)
public abstract class CompositeResource extends AbstractResource implements RestResource, DefaultsGenerator, OptionsCapable {
    // All methods that expect a request body should include the annotation:
    // @Consumes(CONSUMES_TYPE)
    public static final String CONSUMES_TYPE = Constants.MEDIA_TYPE_JSON;
    public static final String QUERY_PARAM_DETACHED = "__detached";
    // TODO: These should be configurable
    protected static final int THREAD_POOL_CORE = 5;
    protected static final int THREAD_POOL_MAX = 10;

    protected CompositeUtil compositeUtil = CompositeUtil.instance();

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    @Override
    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setSubjectRef(Ref<Subject> subjectRef) {
        this.subjectRef = subjectRef;
    }

    @Override
    public Object getDefaultValue(String propertyName) {
        return null;
    }

    public CompositeUtil getCompositeUtil() {
        return compositeUtil;
    }

    /**
     * This method will handle any OPTIONS requests for composite resources.
     * @return
     * @throws JSONException
     */
    @OPTIONS
    public String options() throws JSONException {
        RestResourceMetadata rrmd = new RestResourceMetadata(this);
        return rrmd.toJson().toString(Util.getFormattingIndentLevel());
    }

    /**
     * This method creates a sub-resource of the specified type. Since the JAX-RS does not allow for injection into
     * sub-resources (as it doesn't know or control the lifecycle of the object), this method performs a manual
     * "injection" of the various system objects the resource might need. If the requested Class can not be instantiated
     * (e.g., it does not have a no-arg public constructor), the system will throw a <code>WebApplicationException</code>
     * with an HTTP status code of 500 (internal server error).
     *
     * @param clazz The Class of the desired sub-resource
     * @return
     */
    public <T> T getSubResource(Class<T> clazz) {
        try {
            T resource = clazz.newInstance();
            CompositeResource cr = (CompositeResource)resource;
            cr.locatorBridge = locatorBridge;
            cr.subjectRef = subjectRef;
            cr.uriInfo = uriInfo;
            cr.securityContext = securityContext;
            cr.requestHeaders = requestHeaders;
            cr.serviceLocator = serviceLocator;

            return resource;
        } catch (Exception ex) {
            throw new WebApplicationException(ex, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Execute a delete <code>AdminCommand</code> with no parameters.
     * @param command
     * @return
     */
    protected ActionReporter executeDeleteCommand(String command) {
        return getCompositeUtil().executeDeleteCommand(getSubject(), command);
    }

    /**
     * Execute a delete <code>AdminCommand</code> with the specified parameters.
     * @param command
     * @param parameters
     * @return
     */
    protected ActionReporter executeDeleteCommand(String command, ParameterMap parameters) {
        return getCompositeUtil().executeDeleteCommand(getSubject(), command, parameters);
    }

    /**
     * Execute a writing <code>AdminCommand</code> with no parameters.
     * @param command
     * @return
     */
    protected ActionReporter executeWriteCommand(String command) {
        return getCompositeUtil().executeWriteCommand(getSubject(), command);
    }

    /**
     * Execute a writing <code>AdminCommand</code> with the specified parameters.
     * @param command
     * @param parameters
     * @return
     */
    protected ActionReporter executeWriteCommand(String command, ParameterMap parameters) {
        return getCompositeUtil().executeWriteCommand(getSubject(), command, parameters);
    }

    /**
     * Execute a read-only <code>AdminCommand</code> with the specified parameters.
     * @param command
     * @param parameters
     * @return
     */
    protected ActionReporter executeReadCommand(String command) {
        return getCompositeUtil().executeReadCommand(getSubject(), command);
    }

    /**
     * Execute a read-only <code>AdminCommand</code> with no parameters.
     * @param command
     * @param parameters
     * @return
     */
    protected ActionReporter executeReadCommand(String command, ParameterMap parameters) {
        return getCompositeUtil().executeReadCommand(getSubject(), command, parameters);
    }

    /**
     * Execute an <code>AdminCommand</code> with the specified parameters.
     * @param command
     * @param parameters
     * @param status
     * @param includeFailureMessage
     * @param throwOnWarning (vs.ignore warning)
     * @return
     */
    protected ActionReporter executeCommand(String command, ParameterMap parameters, Status status, boolean includeFailureMessage, boolean throwOnWarning) {
        return getCompositeUtil().executeCommand(getSubject(), command, parameters, status, includeFailureMessage, throwOnWarning);
    }

    /**
     * Execute an <code>AdminCommand</code> via SSE, but provide an <code>ActionReportProcessor</code> that allows
     * the calling resource, via an <code>EntityBuilder</code> instance, to return a <code>ResponseBody</code> that
     * includes the newly create entity, as well as any messages returned by the subsystem.
     */
    protected EventOutput executeSseCreateCommand(final Subject subject, final String command,
                                        final ParameterMap parameters,
                                        final ResponseBodyBuilder builder) {
        return getCompositeUtil().executeSseCommand(subject, command, parameters, new SseCommandHelper.ActionReportProcessor() {
            @Override
            public ActionReport process(ActionReport report, EventOutput ec) {
                if (report != null) {
                    ResponseBody rb = builder.build(report);
                    Properties props = new Properties();
                    props.put("response", rb);
                    report.setExtraProperties(props);
                }

                return report;
            }

        });
    }

    /** Execute an <code>AdminCommand</code> with the specified parameters and
     * return EventOutput suitable for SSE.
     */
    protected EventOutput executeSseCommand(final Subject subject, final String command,
                                        final ParameterMap parameters,
                                        final SseCommandHelper.ActionReportProcessor processor) {
        return getCompositeUtil().executeSseCommand(subject, command, parameters, processor);
    }

    /** Execute an <code>AdminCommand</code> with the specified parameters and
     * return EventOutput suitable for SSE.
     */
    protected EventOutput executeSseCommand(final Subject subject, final String command,
                                        final ParameterMap parameters) {
        return getCompositeUtil().executeSseCommand(subject, command, parameters);
    }

    /**
     * Every resource that returns a collection will need to return the URI for each item in the colleciton. This method
     * handles the creation of that URI, ensuring a correct and consistent URI pattern.
     * @param name
     * @return
     * @throws IllegalArgumentException
     * @throws UriBuilderException
     */
    protected URI getChildItemUri(String name) throws IllegalArgumentException, UriBuilderException {
        return uriInfo.getAbsolutePathBuilder().path("id").path(name).build();
    }

    /**
     * TBD - Jason Lee wants to move this into the defaults generators.
     *
     * Finds an unused name given the list of currently used names and a name prefix.
     *
     * @param namePrefix
     * @param usedNames
     * @return a String containing an unused dname, or an empty string if all candidate names are currently in use.
     */
    protected String generateDefaultName(String namePrefix, Collection<String> usedNames) {
        for (int i = 1; i <= 100; i++) {
            String name = namePrefix + "-" + i;
            if (!usedNames.contains(name)) {
                return name;
            }
        }
        // All the candidate names are in use.  Return an empty name.
        return "";
    }

    protected Response created(String name, String message, RestModel model) {
        return created(getChildItemUri(name), Util.responseBody().addSuccess(message).setEntity(model));
    }

    protected Response created(URI location, ResponseBody responseBody) {
        return Response
                .status(Status.CREATED)
                .header("Location", location)
                .entity(responseBody).build();
    }

    protected Response updated(String message, RestModel model) {
        return updated(Util.responseBody().addSuccess(message).setEntity(model));
    }

    protected Response updated(ResponseBody responseBody) {
        return Response.ok().entity(responseBody).build();
    }

    protected Response deleted(String message) {
        return deleted(Util.responseBody().addSuccess(message));
    }

    protected Response deleted(ResponseBody responseBody) {
        return Response.ok().entity(responseBody).build();
    }

    protected Response accepted(String command, ParameterMap parameters, URI childUri) {
        CommandRunner cr = Globals.getDefaultHabitat().getService(CommandRunner.class);
        final RestActionReporter ar = new RestActionReporter();
        final CommandRunner.CommandInvocation commandInvocation =
                cr.getCommandInvocation(command, ar, getSubject()).
                parameters(parameters);
        String jobId = DetachedCommandHelper.invokeAsync(commandInvocation);
        return Response
                .status(Response.Status.ACCEPTED)
                .header("Location", uriInfo.getBaseUriBuilder().path("jobs").path("id").path(jobId).build())
                .header("X-Location", childUri)
                .build();
    }

    /**
     * Convenience method for getting a path parameter.  Equivalent to getUriInfo().getPathParameters().getFirst(name)
     * @param name
     * @return
     */
    protected String getPathParam(String name) {
        return this.getUriInfo().getPathParameters().getFirst(name);
    }

    protected boolean detachedRequested(String detached) {
        return Boolean.parseBoolean(detached);
    }

    protected synchronized ExecutorService getExecutorService() {
        return ExecutorServiceHolder.INSTANCE;
    }

    private static class ExecutorServiceHolder {
        private static ExecutorService INSTANCE = new ThreadPoolExecutor(
                    THREAD_POOL_CORE, // core thread pool size
                    THREAD_POOL_MAX, // maximum thread pool size
                    1, // time to wait before resizing pool
                    TimeUnit.MINUTES,
                    new ArrayBlockingQueue<Runnable>(THREAD_POOL_MAX, true),
                    new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
