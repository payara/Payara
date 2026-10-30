/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.programatic;

import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import fish.payara.micro.PayaraInstance;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.boot.PayaraMicroBoot;
import fish.payara.micro.boot.PayaraMicroLauncher;

/**
 * A wrapper on the {@link PayaraMicroRuntime} and {@link PayaraInstance} to ease test setup and verification.
 *
 * This test setup works directly with the {@code payara-micro.jar} as its only dependency. While this has the benefit
 * of testing the actual artefact this comes as the downside that there are only a few API classes available at compile
 * time as the server itself is packaged within micro and only accessible at runtime using the right
 * {@link ClassLoader}.
 *
 * Tests using the {@link PayaraMicroServer} should call {@link #start()} before the test(s) and {@link #stop()} after.
 */
public final class PayaraMicroServer {

    static {
        System.setProperty("java.util.logging.config.file", "src/test/resources/logging.properties");
    }

    private static final PayaraMicroServer INSTANCE = new PayaraMicroServer();

    public static PayaraMicroServer newInstance() {
        return INSTANCE;
    }

    private static final String GLASSFISH_CLASS_NAME = "org.glassfish.embeddable.GlassFish";
    private static final String SERVICE_LOCATOR_CLASS_NAME = "org.glassfish.hk2.api.ServiceLocator";

    private static final int defaultHttpPort = 28989;

    private final AtomicBoolean isStarting = new AtomicBoolean(false);
    private final AtomicBoolean isStopping = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private int httpPort;
    private String[] options;
    private PayaraMicroBoot boot;
    private PayaraMicroRuntime runtime;
    private PayaraInstance instance;

    private ClassLoader serverClassLoader;
    private Object glassfish;
    private Object serviceLocator;
    private Object targetUtil;

    private PayaraMicroServer() {
    }

    public PayaraInstance getInstance() {
        return instance;
    }

    public PayaraMicroRuntime getRuntime() {
        return runtime;
    }

    /**
     * Only start if not already started. This means the caller is satisfied with whatever setup of the server.
     */
    public void start() {
        if (!isStarted()) {
            start("--port", "" + defaultHttpPort, "--autobindhttp", "--nohazelcast");
        }
    }

    public void start(String... args) {
        try {
            int port = port(args);
            if (port < 0) {
                port = defaultHttpPort;
                args = Arrays.copyOf(args, args.length + 2);
                args[args.length - 2] = "--port";
                args[args.length - 1] = "" + port;
            }
            if (!isStarting.compareAndSet(false, true)) {
                if (isRunning.get()) {
                    if (Arrays.equals(options, args)) {
                        return; // started with same options already
                    }
                    stop();
                } else {
                    fail("Previous start was not successful.");
                }
            }
            start(args, PayaraMicroLauncher.create(args), port);
        } catch (Exception e) {
            asAssertionError("Failed to start micro server", e);
        } finally {
            isStarting.set(false);
        }
    }

    private static int port(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) || "--sslport".equals(args[i])) {
                return Integer.parseInt(args[i+1]);
            }
        }
        return -1;
    }

    private void start(String[] args, PayaraMicroBoot boot, int port) throws Exception {
        this.options = args;
        this.httpPort = port;
        this.boot = boot;
        runtime = boot.getRuntime();
        if (isRunning.compareAndSet(false, true)) {
            isStarting.set(false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
        }
        instance = getField(PayaraInstance.class, runtime);
        // classes are on the CP of the server but not of the test
        serverClassLoader = instance.getClass().getClassLoader();
        glassfish = getField(getClass(GLASSFISH_CLASS_NAME), runtime);
        serviceLocator = getField(getClass(SERVICE_LOCATOR_CLASS_NAME), glassfish);
    }

    public void stop() {
        if (!isStarted()) {
            return; // don't try to stop if not started successfully
        }
        if (!isStopping.compareAndSet(false, true)) {
            return; // already stopping...
        }
        try {
            boot.shutdown();
            isRunning.set(false);
        } catch (Exception e) {
            // ignore...
        } finally {
            isStopping.set(false);
        }
    }

    private boolean isStarted() {
        return isRunning.get();
    }

    public int getHttpPort() {
        return httpPort;
    }

    public <T> T getExtensionByType(String target, Class<T> type) {
        return failAsAssertionError(() -> {
            Method getConfig = targetUtil.getClass().getMethod("getConfig", String.class);
            Object config = getConfig.invoke(targetUtil, target);
            Method getExtensionByType = config.getClass().getMethod("getExtensionByType", Class.class);
            return type.cast(getExtensionByType.invoke(config, type));
        });
    }

    public Class<?> getClass(String name) {
        return failAsAssertionError(() -> serverClassLoader.loadClass(name));
    }

    public <T> T getService(Class<T> type) {
        return failAsAssertionError(() -> type.cast(serviceLocator.getClass()
                        .getMethod("getService", Class.class, Annotation[].class)
                        .invoke(serviceLocator, type, new Annotation[0])));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAllServices(Class<T> type) {
        return failAsAssertionError(() -> (List<T>) serviceLocator.getClass()
                    .getMethod("getAllServices", Class.class, Annotation[].class)
                    .invoke(serviceLocator, type, new Annotation[0]));
    }

    private static <T> T getField(Class<T> fieldType, Object obj) {
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (fieldType.isAssignableFrom(f.getType())) {
                return failAsAssertionError(() -> {
                    f.setAccessible(true);
                    return fieldType.cast(f.get(obj));
                });
            }
        }
        fail("Could not find instance field in runtime");
        return null;
    }

    private static AssertionError asAssertionError(String msg, Throwable e) {
        return e.getClass() == AssertionError.class ? (AssertionError)e : new AssertionError(msg, e);
    }

    private final static <T> T failAsAssertionError(Callable<T> operation) {
        try {
            return operation.call();
        } catch (Throwable e) {
            throw asAssertionError("Operation failed: ", e);
        }
    }
}
