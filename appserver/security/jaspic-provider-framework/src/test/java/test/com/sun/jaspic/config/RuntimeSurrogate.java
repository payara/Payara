/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.jaspic.config;

import com.sun.jaspic.config.factory.AuthConfigFileFactory;
import com.sun.jaspic.config.servlet.JAASServletAuthConfigProvider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigFactory.RegistrationContext;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.security.auth.message.config.RegistrationListener;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;

/**
 *
 * @author Ron Monzillo
 */
public class RuntimeSurrogate {

    static final Logger logger = Logger.getLogger(RuntimeSurrogate.class.getName());
    private static final String CONFIG_FILE_NAME_KEY = "config.file.name";
    static HashMap<String, String> providerProperties = new HashMap<String, String>();
    AuthConfigFactory factory;
    AuthConfigProvider provider;

    public RuntimeSurrogate(AuthConfigProvider provider, AuthConfigFactory factory) {
        String[] regIDS = factory.getRegistrationIDs(provider);
        for (String i : regIDS) {
            try {
                RegistrationContext r = factory.getRegistrationContext(i);
                System.out.println(contextToString(r));
                AuthConfigProvider p = factory.getConfigProvider(r.getMessageLayer(), r.getAppContext(), null);
                ServerAuthConfig c = p.getServerAuthConfig(r.getMessageLayer(), r.getAppContext(),
                        new CallbackHandler() {

                            public void handle(Callback[] clbcks)
                                    throws IOException, UnsupportedCallbackException {
                                throw new UnsupportedOperationException("Not supported yet.");
                            }
                        });
                ServerAuthContext s = c.getAuthContext("0", new Subject(), new HashMap());
            } catch (AuthException ex) {
                Logger.getLogger(RuntimeSurrogate.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public final String contextToString(RegistrationContext r) {
        String rvalue = r.getDescription() + "\n\t" + r.getAppContext() + "\n\t"
                + r.getMessageLayer() + "\n\t" + r.isPersistent() + "\n";
        return rvalue;
    }

    public static void main(String[] args) {
        System.out.println("Security Manager is "
                + (System.getSecurityManager() == null ? "OFF" : "ON"));
        System.out.println("user.dir: " + System.getProperty("user.dir"));

        for (String s : args) {
            StringTokenizer tokenizer = new StringTokenizer(s, "=");
            if (tokenizer.countTokens() == 2) {
                String key = tokenizer.nextToken();
                String value = tokenizer.nextToken();
                System.out.println("key: " + key + " value: " + value);
                providerProperties.put(key, value);
            }
        }

        AuthConfigFactory.setFactory(new AuthConfigFileFactory());
        final AuthConfigFactory f = AuthConfigFactory.getFactory();

        final AuthConfigProvider p = new JAASServletAuthConfigProvider(providerProperties, f);
        RuntimeSurrogate rS = new RuntimeSurrogate(p, f);
        /*
        p = new SpringServletAuthConfigProvider(properties, f);
        rS = new RuntimeSurrogate(p, f);
         */
        //listenertest
        RegistrationListener listener =
                new RegistrationListener() {

                    public void notify(String layer, String context) {
                        System.out.println("listener notified - layer: " + layer + " context: " + context);
                        f.getConfigProvider(layer, context, this);
                    }
                };

        String rid1 = f.registerConfigProvider(p, "x", null, "test");
        String rid2 = f.registerConfigProvider(p, "x", "y1", "test");

        f.getConfigProvider("x", "y1", listener);
        f.getConfigProvider("x", "y2", listener);

        f.removeRegistration(rid2);
        f.removeRegistration(rid1);

        providers[0] = null;
        for (int i = 1; i < providers.length; i++) {
            providers[i] = new JAASServletAuthConfigProvider(providerProperties, null);
        }
        f.detachListener(listener, null, null);
        testFactory();
    }
    static AuthConfigProvider[] providers = new AuthConfigProvider[4];
    static final TestThread[] threads = new TestThread[1024];

    public static void testFactory() {

        AuthConfigFactory.setFactory(new AuthConfigFileFactory());

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new TestThread();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (TestThread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, "thread: " + t.getId() + " caught exception", ex);
            } finally {
                logger.log(Level.INFO, "thread: {0} completed: {1}", new Object[]{t.getId(), t.runAsConsumer() ? "comsumer" : "producer"});
            }
        }
        logger.info("ALL THREADS JOINED");
        AuthConfigFactory f = AuthConfigFactory.getFactory();
        String[] rids = f.getRegistrationIDs(null);
        for (String i : rids) {
            RegistrationContext rc = f.getRegistrationContext(i);
            logger.log(Level.INFO, "removing registration - layer: {0} appContext: {1} description: {2} persistent: {3}",
                    new Object[]{rc.getMessageLayer(), rc.getAppContext(),
                        rc.getDescription(), rc.isPersistent()});
            f.removeRegistration(i);
        }
        logger.info("ALL REGISTRATIONS REMOVED");
    }

    static class TestThread extends Thread implements RegistrationListener {

        static Random random = new Random();
        static String[] layers = new String[4];
        static String[] contexts = new String[16];
        static int consumerCount = threads.length;
        boolean runAsConsumer = false;
        boolean stop;

        static {
            layers[0] = null;
            for (int i = 1; i < layers.length; i++) {
                layers[i] = "layer" + Integer.toString(i);
            }
            contexts[0] = null;
            for (int i = 1; i < contexts.length; i++) {
                contexts[i] = "context" + Integer.toString(i);
            }
        }

        @Override
        public void run() {
            synchronized (TestThread.class) {
                if (consumerCount == threads.length) {
                    runAsConsumer = false;
                } else {
                    runAsConsumer = (random.nextInt(threads.length / 10) != 1);
                }
            }
            AuthConfigFactory f = AuthConfigFactory.getFactory();
            if (runAsConsumer) {
                doConsumer(f, layers[random.nextInt(layers.length)], contexts[random.nextInt(contexts.length)]);
            } else {
                synchronized (TestThread.class) {
                    consumerCount--;
                    logger.log(Level.INFO, "creating producer, remaining consumers: " + consumerCount);
                }
                while (true) {
                    synchronized (TestThread.class) {
                        if (consumerCount == 0) {
                            return;
                        }
                    }
                    switch (random.nextInt(5)) {
                        case 0:
                            if (random.nextInt(25) == 1) {
                                try {
                                    f.refresh();
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, "producer thread: " + getId(), e);
                                }
                            }
                            break;
                        case 1:
                            if (random.nextInt(1000) == 1) {
                                try {
                                    f = AuthConfigFactory.getFactory();
                                    AuthConfigFactory.setFactory(f);
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, "producer thread: " + getId(), e);
                                }
                            }
                            break;
                        case 2:
                            try {
                                f.registerConfigProvider(
                                        "servlet.JAASServletAuthConfigProvider", providerProperties,
                                        layers[random.nextInt(layers.length)],
                                        contexts[random.nextInt(contexts.length)],
                                        "persistent registration");
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "producer thread: " + getId(), e);
                            }
                            break;
                        case 3:
                            try {
                                f.registerConfigProvider(
                                        providers[random.nextInt(providers.length)],
                                        layers[random.nextInt(layers.length)],
                                        contexts[random.nextInt(contexts.length)],
                                        "transient registration");
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "producer thread: " + getId(), e);
                            }
                            break;
                        case 4:
                            try {
                                String[] rids = f.getRegistrationIDs(
                                        providers[random.nextInt(providers.length)]);
                                int length = rids.length;
                                boolean removeNext = true;
                                for (String rid : rids) {
                                    RegistrationContext rc = f.getRegistrationContext(rid);
                                    if (rc == null) {
                                        removeNext = true;
                                    } else if (removeNext) {
                                        f.removeRegistration(rid);
                                        removeNext = false;
                                    } else {
                                        removeNext = true;
                                    }
                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "producer thread: " + getId(), e);
                            }
                            break;

                    }
                }
            }
        }

        public boolean runAsConsumer() {
            return runAsConsumer;
        }

        public void doConsumer(AuthConfigFactory f, String layer, String context) {

            synchronized (TestThread.class) {
                logger.log(Level.INFO, "creating consumer");
                this.stop = false;
            }
            try {
                while (true) {
                    f.getConfigProvider(layer, context, this);
                    sleep(100);
                    synchronized (TestThread.class) {
                        if (this.stop) {
                            break;
                        }
                    }
                }
                f.detachListener(this, null, null);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "consumer thread: " + getId(), e);
            } finally {
                synchronized (TestThread.class) {
                    consumerCount--;
                    logger.log(Level.INFO, "consumer thread: " + getId() + "stopping - remaining: " + consumerCount);
                }
            }
        }

        public void notify(String layer, String context) {
            if (random.nextInt(100) == 1) {
                synchronized (TestThread.class) {
                    this.stop = true;
                }
            }
        }
    }
}
