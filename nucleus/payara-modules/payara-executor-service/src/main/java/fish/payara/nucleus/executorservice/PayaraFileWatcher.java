/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.executorservice;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.WARNING;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.deployment.Deployment;
import org.jvnet.hk2.annotations.Service;

@Service(name = "payara-file-watcher")
@RunLevel(StartupRunLevel.VAL)
public class PayaraFileWatcher implements EventListener {

    private static final Logger LOGGER = Logger.getLogger(PayaraFileWatcher.class.getName());

    private static final Map<Path, Runnable> LISTENER_MAP = new HashMap<>();
    private static final Set<Path> PATHS_TO_WATCH = new HashSet<>();

    private volatile boolean running;

    private WatchService watcher;

    @Inject
    private Events events;

    @Inject
    private PayaraExecutorService executor;

    // Lifecycle methods

    @PostConstruct
    protected void postConstruct() {
        if (events != null) {
            events.register(this);
        }
        initialise();
    }

    @PreDestroy
    protected void preDestroy(){
        if (events != null) {
            events.unregister(this);
        }
        terminate();
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.ALL_APPLICATIONS_LOADED)) {
            // Embedded containers can be started and stopped multiple times.
            // Thus we need to initialize anytime the server instance is started.
            initialise();
        } else if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            terminate();
        }
    }

    // Private methods

    /**
     * The event loop method
     */
    private void run() {
        registerQueuedPaths();
        try {
            // Block, waiting for an event on a watched file
            WatchKey key = watcher.take();

            // Loop through the events
            for (WatchEvent<?> event : key.pollEvents()) {
                // Find the absolute path of the modified file
                final Path modifiedPath = ((Path) key.watchable()).resolve((Path) event.context());

                // Loop through the watched paths to find the action associated with it
                Iterator<Entry<Path, Runnable>> watchIterator = LISTENER_MAP.entrySet().iterator();
                while (watchIterator.hasNext()) {
                    Entry<Path, Runnable> watchEntry = watchIterator.next();

                    // If this entry corresponds to the modified file
                    if (modifiedPath.endsWith(watchEntry.getKey())) {
                        // Ignore overflow events
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        File modifiedFile = modifiedPath.toFile();

                        // If it's been deleted, remove the file watcher
                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE && !modifiedFile.exists()) {
                            LOGGER.info(format("Watched file %s was deleted; removing the file watcher", modifiedPath));
                            watchIterator.remove();
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && modifiedFile.length() > 0) {
                            // Run the associated action
                            LOGGER.fine(format("Watched file %s modified, running the listener", modifiedPath));
                            watchEntry.getValue().run();
                        }
                    }
                }
            }
            key.reset();
        } catch (InterruptedException ex) {
            LOGGER.log(WARNING, "The file watcher thread was interrupted", ex);
        }
    }

    /**
     * Empty the map of file listeners, registering each one with the watch service
     */
    private void registerQueuedPaths() {
        if (!PATHS_TO_WATCH.isEmpty()) {

            // Iterate through paths
            Iterator<Path> pathIterator = PATHS_TO_WATCH.iterator();
            while (pathIterator.hasNext()) {

                // Get the directory of a registered path
                Path path = pathIterator.next();
                if (path.toFile().isFile()) {
                    path = path.getParent();
                }

                try {
                    path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                    LOGGER.fine(format("Watching path: %s", path));
                } catch (IOException ex) {
                    LOGGER.log(WARNING, format("Failed to register path %s with the watch service", path), ex);
                }

                pathIterator.remove();
            }
        }
    }

    private synchronized void initialise() {
        if (!running && !PATHS_TO_WATCH.isEmpty()) {
            try {
                watcher = FileSystems.getDefault().newWatchService();
                executor.scheduleWithFixedDelay(this::run, 0, 1, SECONDS);
                running = true;
                LOGGER.info("Initialised the file watcher service");
            } catch (IOException ex) {
                LOGGER.log(WARNING, "Failed to initialise the watch service", ex);
            }
        }
    }


    private synchronized void terminate() {
        if (running) {
            try {
                watcher.close();
                running = false;
                LOGGER.info("Terminated the file watcher service");
            } catch (IOException ex) {
                LOGGER.log(WARNING, "Failed to terminate the watch service", ex);
            }
        }
    }

    // Static methods

    public static void watch(Path path, Runnable runnable) {
        PATHS_TO_WATCH.add(path);
        LISTENER_MAP.put(path, runnable);
    }

}
