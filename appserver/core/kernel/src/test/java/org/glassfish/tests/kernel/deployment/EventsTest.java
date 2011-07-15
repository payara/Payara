/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.kernel.deployment;

import org.junit.*;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.tests.utils.Utils;
import org.glassfish.tests.utils.ConfigApiTest;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.config.support.GlassFishDocument;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.DomDocument;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.sun.hk2.component.ExistingSingletonInhabitant;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.StartupContext;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Mar 12, 2009
 * Time: 9:26:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class EventsTest extends ConfigApiTest {

    static Habitat habitat;
    static File application;
    static List<EventListener.Event> allEvents = new ArrayList<EventListener.Event>();
    static private EventListener listener = new EventListener() {
        public void event(Event event) {
            //System.out.println("Received event " + event.name());
            allEvents.add(event);
        }
    };

    public String getFileName() {
        return "DomainTest";
    }

    public DomDocument getDocument(Habitat habitat) {
       DomDocument doc = habitat.getByType(GlassFishDocument.class);
        if (doc==null) {
            return new GlassFishDocument(habitat, Executors.newCachedThreadPool(new ThreadFactory() {

                        public Thread newThread(Runnable r) {
                            Thread t = Executors.defaultThreadFactory().newThread(r);
                            t.setDaemon(true);
                            return t;
                        }

                    }));
        }
        return doc;
    }

    @Before
    public void setup() throws IOException {

        // cludge to run only once yet not depend on a static method.
        if (habitat!=null) {
            return;
        }
        habitat  = super.getHabitat();
        habitat.addIndex(new ExistingSingletonInhabitant<Server>(habitat.getComponent(Server.class, "server")),
                     Server.class.getName(), ServerEnvironment.DEFAULT_INSTANCE_NAME);

        try {
            application = File.createTempFile("kerneltest", "tmp");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;

        }

        application.delete();
        application.mkdirs();

        Events events = habitat.getByContract(Events.class);
        events.register(listener);
    }

    @AfterClass
    public static void tearDown() {
       application.delete();

    }

    public static List<EventTypes> getSingletonModuleSuccessfullDeploymentEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.MODULE_PREPARED);
        events.add(Deployment.MODULE_LOADED);
        events.add(Deployment.MODULE_STARTED);
        events.add(Deployment.APPLICATION_PREPARED);
        events.add(Deployment.APPLICATION_LOADED);
        events.add(Deployment.APPLICATION_STARTED);
        return events;
    }

    public static List<EventTypes> getSingletonModuleSuccessfullUndeploymentEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.MODULE_STOPPED);
        events.add(Deployment.MODULE_UNLOADED);
        events.add(Deployment.MODULE_CLEANED);
        events.add(Deployment.APPLICATION_STOPPED);
        events.add(Deployment.APPLICATION_UNLOADED);
        events.add(Deployment.APPLICATION_CLEANED);
        return events;
    }

    public static List<EventTypes> asynchonousEvents() {
        ArrayList<EventTypes> events = new ArrayList<EventTypes>();
        events.add(Deployment.DEPLOYMENT_START);
        events.add(Deployment.DEPLOYMENT_SUCCESS);        
        events.add(Deployment.UNDEPLOYMENT_START);
        events.add(Deployment.UNDEPLOYMENT_SUCCESS);
        events.add(Deployment.UNDEPLOYMENT_FAILURE);
        return events;
    }

    @Test
    public void deployTest() throws Exception {

        final List<EventTypes> myTestEvents = getSingletonModuleSuccessfullDeploymentEvents();
        Events events = habitat.getByContract(Events.class);
        EventListener listener = new EventListener() {
            public void event(Event event) {
                if (myTestEvents.contains(event.type())) {
                    myTestEvents.remove(event.type());
                }
            }
        };
        events.register(listener);
        Deployment deployment = habitat.getByContract(Deployment.class);
        DeployCommandParameters params = new DeployCommandParameters(application);
        params.name = "fakeApplication";
        ActionReport report = habitat.getComponent(ActionReport.class, "hk2-agent");
        ExtendedDeploymentContext dc = deployment.getBuilder(Logger.getAnonymousLogger(), params, report).source(application).build();
        deployment.deploy(dc);
        events.unregister(listener);
        Assert.assertEquals(report.getActionExitCode(), ActionReport.ExitCode.SUCCESS);
        for (EventTypes et : myTestEvents) {
            System.out.println("An expected event of type " + et.type() + " was not received");
        }
    }

    @Test
    public void undeployTest() throws Exception {

        try {
        final List<EventTypes> myTestEvents = getSingletonModuleSuccessfullUndeploymentEvents();
        Events events = habitat.getByContract(Events.class);
        EventListener listener = new EventListener() {
            public void event(Event event) {
                if (myTestEvents.contains(event.type())) {
                    myTestEvents.remove(event.type());
                }
            }
        };
        events.register(listener);
        Deployment deployment = habitat.getByContract(Deployment.class);
        UndeployCommandParameters params = new UndeployCommandParameters("fakeApplication");
        ActionReport report = habitat.getComponent(ActionReport.class, "hk2-agent");
        ExtendedDeploymentContext dc = deployment.getBuilder(logger, params, report).source(application).build();
        deployment.undeploy("fakeApplication", dc);
        Assert.assertEquals(report.getActionExitCode(), ActionReport.ExitCode.SUCCESS);
        for (EventTypes et : myTestEvents) {
            System.out.println("An expected event of type " + et.type() + " was not received");
        }
        } catch(Throwable t) {
            t.printStackTrace();
        }

    }

    @Test
    public void badUndeployTest() throws Exception {
        Deployment deployment = habitat.getByContract(Deployment.class);
        UndeployCommandParameters params = new UndeployCommandParameters("notavalidname");
        ActionReport report = habitat.getComponent(ActionReport.class, "hk2-agent");
        ExtendedDeploymentContext dc = deployment.getBuilder(Logger.getAnonymousLogger(), params, report).source(application).build();
        deployment.undeploy("fakeApplication", dc);
        Assert.assertEquals(report.getActionExitCode(), ActionReport.ExitCode.FAILURE);
    }

    @Test
    @Ignore
    public void asynchronousEvents() {
        List<EventTypes> asyncEvents =  asynchonousEvents();
        Iterator<EventTypes> itr = asyncEvents.iterator();
        while (itr.hasNext()) {
            EventTypes et = itr.next();
            for (EventListener.Event evt : allEvents) {
                if (evt.is(et)) {
                    itr.remove();
                }
            }
        }
        for (EventTypes et : asyncEvents) {
            System.out.println("Asynchronous event " + et.type() + " was not received");    
        }
        Assert.assertTrue(asyncEvents.size()==0);        
    }
}
