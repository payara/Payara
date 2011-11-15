/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;


import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.enterprise.util.Result;
import com.sun.hk2.component.ExistingSingletonInhabitant;
import com.sun.hk2.component.InhabitantParser;
import com.sun.hk2.component.InhabitantsParser;
import org.glassfish.api.FutureProvider;
import org.glassfish.api.Startup;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.hk2.ComponentException;
import org.glassfish.hk2.PostConstruct;
import org.glassfish.hk2.PreDestroy;
import org.glassfish.hk2.Services;
import org.glassfish.internal.api.Init;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.api.PostStartup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.HabitatFactory;
import org.jvnet.hk2.component.InhabitantsParserFactory;
import org.jvnet.hk2.component.RunLevelService;
import org.jvnet.hk2.junit.Hk2Runner;
import org.jvnet.hk2.junit.Hk2RunnerOptions;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * AppServerStartup tests.
 *
 * @author Tom Beerbower
 */
@RunWith(Hk2Runner.class)
@Hk2RunnerOptions(classpathFilter          = AppServerStartupTest.TestFileFilter.class,
                  habitatFactory           = AppServerStartupTest.TestHabitatFactory.class,
                  inhabitantsParserFactory = AppServerStartupTest.TestInhabitantsParserFactory.class)
public class AppServerStartupTest {

    // ----- data members ----------------------------------------------------

    /**
     * The AppServerStartup instance to test.
     */
    @Inject
    private AppServerStartup as;

    /**
     * The test results.
     */
    private static Results results;

    /**
     * Set of inhabitant type names that should be filtered out by the {@link TestInhabitantsParser}.
     * Used to filter out unused inhabitants that result in unresolved dependencies.
     */
    private static final Set<String> setFilteredInhabitantTypeNames = new HashSet<String>();

    /**
     * Set of run level service type names that we allow to pass through the {@link TestInhabitantsParser}.
     * Used to limit the run level services to those that are testable or required for testing.
     */
    private static final Set<String> setRunLevelServiceTypeNames = new HashSet<String>();

    /**
     * Map of exceptions to be thrown from the postConstruct.
     */
    private static final Map<Class, RuntimeException> mapPostConstructExceptions = new HashMap<Class, RuntimeException>();

    /**
     * List of {@link Future}s returned from {@link FutureProvider#getFutures()} by the {@link Startup}
     * services during progression to the start up run level.
     */
    private static List<TestFuture> listFutures = null;

    /**
     * Initialize the static sets of type names for the {@link TestInhabitantsParser}.
     */
    static {
        setFilteredInhabitantTypeNames.add("org.glassfish.internal.api.Globals");
        setFilteredInhabitantTypeNames.add("com.sun.enterprise.v3.server.ApplicationLoaderService");
        setFilteredInhabitantTypeNames.add("com.sun.enterprise.v3.admin.CommandRunnerImpl");

        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.AppServerStartupTest$TestInitService");
        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.AppServerStartupTest$TestInitRunLevelService");
        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.AppServerStartupTest$TestStartupService");
        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.AppServerStartupTest$TestPostStartupService");
        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.InitRunLevelBridge");
        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.StartupRunLevelBridge");
        setRunLevelServiceTypeNames.add("com.sun.enterprise.v3.server.PostStartupRunLevelBridge");
    }


    // ----- test initialization ---------------------------------------------

    /**
     * Reset the results prior to each test.
     */
    @Before
    public void beforeTest() {
        results = new Results(as.rls);
        as.events.register(results);
    }

    /**
     * Ensure that things are stopped after the test... if not then call stop.
     */
    @After
    public void afterTest() {
        if (as.rls.getState().getCurrentRunLevel() > 0) {
            // force a stop to ensure that the services are released
            as.env.setStatus(ServerEnvironment.Status.started);
            as.stop();
        }
    }


    // ----- tests -----------------------------------------------------------

    /**
     * Call the {@link AppServerStartup#run} method and make sure that
     * the run level services are constructed and destroyed at the proper
     * run levels.
     */
    @Test
    public void testRunLevelServices() {

        // create the list of Futures returned from TestStartupService
        listFutures = new LinkedList<TestFuture>();
        listFutures.add(new TestFuture());
        listFutures.add(new TestFuture());
        listFutures.add(new TestFuture());

        // assert that we have clean results to start
        Assert.assertFalse(results.isConstructed(TestInitService.class));
        Assert.assertFalse(results.isConstructed(TestInitRunLevelService.class));
        Assert.assertFalse(results.isConstructed(TestStartupService.class));
        Assert.assertFalse(results.isConstructed(TestPostStartupService.class));

        as.run();

        Assert.assertTrue(as.env.getStatus() == ServerEnvironment.Status.started);

        Assert.assertEquals(2, results.getListEvents().size());
        Assert.assertEquals(EventTypes.SERVER_STARTUP, results.getListEvents().get(0));
        Assert.assertEquals(EventTypes.SERVER_READY, results.getListEvents().get(1));

        // assert that the run level services have been constructed
        Assert.assertTrue(results.isConstructed(TestInitService.class, InitRunLevel.VAL));
        Assert.assertTrue(results.isConstructed(TestInitRunLevelService.class, InitRunLevel.VAL));
        Assert.assertTrue(results.isConstructed(TestStartupService.class));
        Assert.assertTrue(results.isConstructed(TestPostStartupService.class));

        as.stop();

        Assert.assertFalse(as.env.getStatus() == ServerEnvironment.Status.started);

        Assert.assertEquals(4, results.getListEvents().size());
        Assert.assertEquals(EventTypes.PREPARE_SHUTDOWN, results.getListEvents().get(2));
        Assert.assertEquals(EventTypes.SERVER_SHUTDOWN, results.getListEvents().get(3));

        // assert that the run level services have been destroyed
        Assert.assertTrue(results.isDestroyed(TestInitService.class));
        Assert.assertTrue(results.isDestroyed(TestInitRunLevelService.class));
        Assert.assertTrue(results.isDestroyed(TestStartupService.class));
        Assert.assertTrue(results.isDestroyed(TestPostStartupService.class));
    }

    /**
     * Test the {@link AppServerStartup#run} method with an exception thrown from the startup
     * service that should cause a failure during startup.  Make sure that the init and
     * startup run level services are constructed at the proper run levels.
     */
    @Test
    public void testRunLevelServicesWithException() {

        // set an exception to be thrown from TestStartupService.postConstruct()
        mapPostConstructExceptions.put(TestStartupService.class,
                new RuntimeException("Exception from TestStartupService.postConstruct"));

        try {
            // create the list of Futures returned from TestStartupService
            listFutures = new LinkedList<TestFuture>();
            listFutures.add(new TestFuture());

            // assert that we have clean results to start
            Assert.assertFalse(results.isConstructed(TestInitService.class));
            Assert.assertFalse(results.isConstructed(TestInitRunLevelService.class));
            Assert.assertFalse(results.isConstructed(TestStartupService.class));
            Assert.assertFalse(results.isConstructed(TestPostStartupService.class));

            as.run();

            // make sure that the server has not been started
            Assert.assertFalse(as.env.getStatus() == ServerEnvironment.Status.started);

            Assert.assertTrue(results.getListEvents().contains(EventTypes.SERVER_SHUTDOWN));

            // assert that the run level services have been constructed
            Assert.assertTrue(results.isConstructed(TestInitService.class, InitRunLevel.VAL));
            Assert.assertTrue(results.isConstructed(TestInitRunLevelService.class, InitRunLevel.VAL));
            Assert.assertTrue(results.isConstructed(TestStartupService.class));
            // assert that the post-startup service is not constructed since shutdown occurs during startup
            Assert.assertFalse(results.isConstructed(TestPostStartupService.class));

        } finally {
            mapPostConstructExceptions.remove(TestStartupService.class);
        }
    }

    /**
     * Test the {@link AppServerStartup#run} method with an exception that should cause
     * a failed result during startup.  Make sure that the init and startup run level
     * services are constructed at the proper run levels.  Also ensure that the failed
     * {@link java.util.concurrent.Future} causes a shutdown.
     */
    @Test
    public void testRunLevelServicesWithFuturesException() {

        // create the list of Futures returned from TestStartupService
        listFutures = new LinkedList<TestFuture>();
        listFutures.add(new TestFuture());
        listFutures.add(new TestFuture(new Exception("Exception from Future.")));
        listFutures.add(new TestFuture());

        // assert that we have clean results to start
        Assert.assertFalse(results.isConstructed(TestInitService.class));
        Assert.assertFalse(results.isConstructed(TestInitRunLevelService.class));
        Assert.assertFalse(results.isConstructed(TestStartupService.class));
        Assert.assertFalse(results.isConstructed(TestPostStartupService.class));

        as.run();

        // make sure that the server has not been started
        Assert.assertFalse(as.env.getStatus() == ServerEnvironment.Status.started);

        Assert.assertTrue(results.getListEvents().contains(EventTypes.SERVER_SHUTDOWN));

        // assert that the run level services have been constructed
        Assert.assertTrue(results.isConstructed(TestInitService.class, InitRunLevel.VAL));
        Assert.assertTrue(results.isConstructed(TestInitRunLevelService.class, InitRunLevel.VAL));
        Assert.assertTrue(results.isConstructed(TestStartupService.class));
    }

    // ----- Results inner class ---------------------------------------------

    /**
     * Test results
     */
    private static class Results implements EventListener {
        /**
         * Map of constructed run level services to run levels.
         */
        private Map<Class, Integer> mapConstructedLevels = new HashMap<Class, Integer>();

        /**
         * Map of destroyed run level services to run levels.
         */
        private Map<Class, Integer> mapDestroyedLevels = new HashMap<Class, Integer>();

        /**
         * List of server events.
         */
        private List<EventTypes> listEvents = new LinkedList<EventTypes>();

        /**
         * The run level service.
         */
        private RunLevelService<?> rls;

        public Results(RunLevelService<?> rls) {
            this.rls = rls;
        }

        public void recordConstruction(Class cl) {
            mapConstructedLevels.put(cl, rls.getState().getPlannedRunLevel());
        }

        public void recordDestruction(Class cl) {
            mapDestroyedLevels.put(cl, rls.getState().getPlannedRunLevel());
        }

        public boolean isConstructed(Class cl) {
            return mapConstructedLevels.keySet().contains(cl);
        }

        public boolean isConstructed(Class cl, Integer runLevel) {
            Integer recLevel = mapConstructedLevels.get(cl);
            return recLevel != null && recLevel.equals(runLevel);
        }

        public boolean isDestroyed(Class cl) {
            return mapDestroyedLevels.keySet().contains(cl);
        }

        public boolean isDestroyed(Class cl, Integer runLevel) {
            Integer recLevel = mapDestroyedLevels.get(cl);
            return recLevel != null && recLevel.equals(runLevel);
        }

        public List<EventTypes> getListEvents() {
            return listEvents;
        }

        @Override
        public void event(Event event) {
            listEvents.add(event.type());
        }
    }


    // ----- test services inner classes -------------------------------------

    /**
     * Abstract service that will update the test results from
     * {@link PostConstruct#postConstruct()}.
     */
    public static abstract class TestService implements PostConstruct, PreDestroy {
        @Override
        public void postConstruct() {
            AppServerStartupTest.results.recordConstruction(this.getClass());

            RuntimeException ex = mapPostConstructExceptions.get(getClass());
            if (ex != null) {
                throw ex;
            }
        }

        @Override
        public void preDestroy() {
            AppServerStartupTest.results.recordDestruction(this.getClass());
        }
    }

    /**
     * Init service that implements the old style {@link Init} interface.
     */
    @Service
    public static class TestInitService extends TestService implements Init {
    }

    /**
     * Init service annotated with the new style {@link InitRunLevel} annotation.
     */
    @Service
    @InitRunLevel
    public static class TestInitRunLevelService extends TestService  {
    }

    /**
     * Startup service that implements the old style {@link Startup} interface.
     */
    @Service
    public static class TestStartupService extends TestService implements Startup, FutureProvider {
        @Override
        public Lifecycle getLifecycle() {
            return Lifecycle.START;
        }

        @Override
        public List getFutures() {
            return listFutures;
        }
    }

    /**
     * Post-startup service that implements the old style {@link PostStartup} interface.
     */
    @Service
    public static class TestPostStartupService extends TestService implements PostStartup {
    }


    // ----- TestFuture inner classes ----------------------------------------

    /**
     * Future implementation used for test {@link Startup} implementations that
     * also implement {@link FutureProvider}.
     */
    public static class TestFuture implements Future<Result<Thread>> {

        private boolean   canceled        = false;
        private boolean   done            = false;
        private Exception resultException = null;

        public TestFuture() {
        }

        public TestFuture(Exception resultException) {
            this.resultException = resultException;
        }

        @Override
        public boolean cancel(boolean b) {
            if (done) {
                return false;
            }
            canceled = done = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return canceled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Result<Thread> get() throws InterruptedException, ExecutionException {

            Result<Thread> result = resultException == null ?
                            new Result<Thread>(Thread.currentThread()) :
                            new Result<Thread>(resultException);
            done = true;

            return result;
        }

        @Override
        public Result<Thread> get(long l, TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

    // ----- Hk2RunnerOptions inner classes  ---------------------------------

    /**
     * Test file filter that only accepts the files that we need to populate
     * a {@link Habitat} for this test.
     */
    public static class TestFileFilter implements FileFilter {

        private static final String HK2_PATH                       = "hk2" + File.separator + "hk2";
        private static final String GLASSFISH_API_PATH             = "glassfish-api";
        private static final String INTERNAL_API_PATH              = "internal-api";
        private static final String NUCLEUS_CORE_PATH              = "nucleus" + File.separator + "core" + File.separator +
                                                                     "kernel" + File.separator + "target";
        private static final String NUCLEUS_CORE_CLASSES_PATH      = NUCLEUS_CORE_PATH + File.separator + "classes";
        private static final String NUCLEUS_CORE_TEST_CLASSES_PATH = NUCLEUS_CORE_PATH + File.separator + "test-classes";

        @Override
        public boolean accept(File file) {
            String sFile = file.toString();

            return (sFile.contains(HK2_PATH) ||
                    sFile.contains(GLASSFISH_API_PATH) ||
                    sFile.contains(INTERNAL_API_PATH) ||
                    sFile.contains(NUCLEUS_CORE_CLASSES_PATH) ||
                    sFile.contains(NUCLEUS_CORE_TEST_CLASSES_PATH));
        }
    }

    /**
     * Factory to create a {@link Habitat} for this test.
     */
    public static class TestHabitatFactory implements HabitatFactory {

        @Override
        public Habitat newHabitat() throws ComponentException {
            return newHabitat(null, null);
        }

        @Override
        public Habitat newHabitat(Services parent, String name) throws ComponentException {

            Habitat habitat = new Habitat(parent, name);

            ModulesRegistry registry = new StaticModulesRegistry(this.getClass().getClassLoader());
            habitat.addIndex(new ExistingSingletonInhabitant<ModulesRegistry>(registry),
                    ModulesRegistry.class.getName(), null);

            ExecutorService service = new ThreadPoolExecutor(10, 100,
                20, TimeUnit.SECONDS,
                new SynchronousQueue(),
                new ThreadPoolExecutor.CallerRunsPolicy());

            habitat.addIndex(new ExistingSingletonInhabitant<ExecutorService>(service),
                    ExecutorService.class.getName(), null);

            CommonClassLoaderServiceImpl ccl = new CommonClassLoaderServiceImpl();
            habitat.addIndex(new ExistingSingletonInhabitant<CommonClassLoaderServiceImpl>(ccl),
                    CommonClassLoaderServiceImpl.class.getName(), null);

            SystemTasks systemTasks = new SystemTasks(){
                @Override
                public void writePidFile() {
                    // do nothing.
                }
            };
            habitat.addIndex(new ExistingSingletonInhabitant<SystemTasks>(systemTasks),
                    SystemTasks.class.getName(), null);

            return habitat;
        }
    }

    /**
     * Factory to create a new {@link InhabitantsParser} for this test.
     */
    public static class TestInhabitantsParserFactory implements InhabitantsParserFactory {
        @Override
        public InhabitantsParser createInhabitantsParser(Habitat habitat) {
            return new TestInhabitantsParser(habitat);
        }
    }

    /**
     * Inhabitant parser used for this test.  Allows for the filtering
     * of non-required inhabitants and inhabitants for non-testable run
     * level services.
     */
    public static class TestInhabitantsParser extends InhabitantsParser {

        public TestInhabitantsParser(Habitat habitat) {
            super(habitat);
        }

        @Override
        protected boolean isFilteredInhabitant(InhabitantParser inhabitantParser) {
            if (isRunLevelService(inhabitantParser)) {
                if (!setRunLevelServiceTypeNames.contains(inhabitantParser.getImplName())) {
                    return true;
                }
            }

            return super.isFilteredInhabitant(inhabitantParser);
        }

        @Override
        protected boolean isFilteredInhabitant(String typeName) {

            return setFilteredInhabitantTypeNames.contains(typeName) ||
                    super.isFilteredInhabitant(typeName);
        }

        /**
         * Determines whether or not the given inhabitant parser is for
         * a run level service (annotated with {@link RunLevel} or implements
         * {@link Init}, {@link Startup}, or {@link PostStartup}).
         *
         * @param inhabitantParser  the inhabitant parser
         *
         * @return true iff the given inhabitant parser is for a run level service
         */
        private boolean isRunLevelService(InhabitantParser inhabitantParser) {

            String runlevel = inhabitantParser.getMetaData().getOne(RunLevel.META_VAL_TAG);
            if (null != runlevel && runlevel.length() > 0) {
                return true;
            }

            Iterable<String> indexes = inhabitantParser.getIndexes();
            for (String index : indexes) {
                if (index.equals("org.glassfish.internal.api.Init") ||
                    index.equals("org.glassfish.api.Startup") ||
                    index.equals("org.glassfish.internal.api.PostStartup")) {
                    return true;
                }
            }
            return false;
        }
    }
}
