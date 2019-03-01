/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.test.util;

import static fish.payara.test.util.BeanProxy.asAssertionError;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import fish.payara.micro.PayaraInstance;
import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;

/**
 * A wrapper on the {@link PayaraMicroRuntime} and {@link PayaraInstance} to ease test setup and verification.
 * 
 * This test setup works directly with the {@code payara-micro.jar} as its only dependency. While this has the benefit
 * of testing the actual artefact this comes as the downside that there are only a few API classes available at compile
 * time as the server itself is packaged within micro and only accessible at runtime using the right
 * {@link ClassLoader}.
 * 
 * Tests using the {@link PayaraMicroServer} should call {@link #start()} before the test(s) and {@link #stop()} after
 * even when using the {@link #DEFAULT} instance.
 */
public final class PayaraMicroServer {

    static {
        System.setProperty("java.util.logging.config.file", "src/test/resources/logging.properties");
    }

    /**
     * A instance to reuse during tests running in the same VM that do not have a problem with unknown start condition.
     * 
     * Tests using this instance may change it but should not perform operations that make further usage hard or
     * impossible, like for instance shutting down the server would.
     */
    public final static PayaraMicroServer DEFAULT = new PayaraMicroServer(true);

    public static PayaraMicroServer newInstance() {
        return new PayaraMicroServer(false);
    }

    private static final String GALSSFISH_CLASS_NAME = "org.glassfish.embeddable.GlassFish";
    private static final String SERVICE_LOCATOR_CLASS_NAME = "org.glassfish.hk2.api.ServiceLocator";
    private static final String TARGET_CLASS_NAME = "org.glassfish.internal.api.Target";
    private static final String DOMAIN_CLASS_NAME = "com.sun.enterprise.config.serverbeans.Domain";

    private final static String DEPLOYMENT_DIR = System.getProperty("user.dir") + File.separator + "target/deployments";

    /**
     * The port number to use next if a {@link PayaraMicroServer} instance is started.
     */
    private static AtomicInteger nextPort = new AtomicInteger(28989);

    private final boolean defaultInstance;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean startedSuccessfully = new AtomicBoolean(false);
    private PayaraMicro boot;
    private PayaraMicroRuntime runtime;
    private PayaraInstance instance;

    private ClassLoader serverClassLoader;
    private Object glassfish;
    private Object serviceLocator;
    private Object targetUtil;
    private BeanProxy domain;

    private PayaraMicroServer(boolean defaultInstance) {
        this.defaultInstance = defaultInstance;
    }

    public PayaraInstance getInstance() {
        return instance;
    }

    public PayaraMicroRuntime getRuntime() {
        return runtime;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            return; // only start it once
        }
        try {
            boot = PayaraMicro.getInstance();
            File dir = new File(DEPLOYMENT_DIR);
            createDummyFile(dir);
            int port = nextPort.getAndIncrement();
            runtime = boot.setInstanceName("micro-test")
                    .setDeploymentDir(dir)
                    .setHttpPort(port)
                    .setHttpAutoBind(true)
                    .bootStrap();
            startedSuccessfully.compareAndSet(false, true);
            instance = getField(PayaraInstance.class, runtime);
            // classes are on the CP of the server but not of the test
            serverClassLoader = instance.getClass().getClassLoader();
            glassfish = getField(getClass(GALSSFISH_CLASS_NAME), runtime);
            serviceLocator = getField(getClass(SERVICE_LOCATOR_CLASS_NAME), glassfish);
            targetUtil = getService(getClass(TARGET_CLASS_NAME));
            domain = new BeanProxy(getService(getClass(DOMAIN_CLASS_NAME)));
            if (defaultInstance) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> doStop()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            asAssertionError("Failed to start micro server", e);
        }
    }

    public void stop() {
        if (defaultInstance) {
            return; // is stopped by JVM shutdown hook as it is shared amoung many tests
        }
        doStop();
    }

    private void doStop() {
        if (!isStarted()) {
            return; // don't try to stop if not started successfully
        }
        try {
            boot.shutdown();
        } catch (Exception e) {
            // ignore...
        }
    }

    private boolean isStarted() {
        return started.get() && startedSuccessfully.get();
    }

    public <T> T getDomainExtensionByType(Class<T> type) {
        return type.cast(domain.callMethod("getExtensionByType",
                "Failed to get Domain extension of type " + type.getName(), Class.class, type));
    }

    public <T> T getExtensionByType(String target, Class<T> type) {
        try {
            Method getConfig = targetUtil.getClass().getMethod("getConfig", String.class);
            Object config = getConfig.invoke(targetUtil, target);
            Method getExtensionByType = config.getClass().getMethod("getExtensionByType", Class.class);
            return type.cast(getExtensionByType.invoke(config, type));
        } catch (Throwable e) {
            throw asAssertionError("Failed to get extension", e);
        }
    }

    public Class<?> getClass(String name) {
        try {
            return serverClassLoader.loadClass(name);
        } catch (Throwable e) {
            throw asAssertionError("Failed to load class for name: " + name, e);
        }
    }

    public <T> T getService(Class<T> type) {
        try {
            Method getService = serviceLocator.getClass().getMethod("getService", Class.class, Annotation[].class);
            return type.cast(getService.invoke(serviceLocator, type, new Annotation[0]));
        } catch (Throwable e) {
            throw asAssertionError("Failed to resolve service", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAllServices(Class<T> type) {
        try {
            Method getAllServices = serviceLocator.getClass().getMethod("getAllServices", Class.class, Annotation[].class);
            return (List<T>) getAllServices.invoke(serviceLocator, type, new Annotation[0]);
        } catch (Throwable e) {
            throw asAssertionError("Failed to resolve services", e);
        }
    }

    private static <T> T getField(Class<T> fieldType, Object obj) {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (fieldType.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    return fieldType.cast(f.get(obj));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalStateException("Could not find instance field in runtime");
    }

    /**
     * Currently the deployment dir is not allowed to be empty 
     */
    private static void createDummyFile(File dir) throws IOException {
        File dummy = new File(dir, "dummy.txt");
        dummy.getParentFile().mkdirs(); 
        dummy.createNewFile();
    }
}
