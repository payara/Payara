/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.PostConstruct;
import org.glassfish.hk2.PreDestroy;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Priority;
import org.jvnet.hk2.annotations.RunLevel;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.InhabitantActivator;

/**
 * Abstract base for all run level bridges.
 * 
 * @author Jeff Trent
 * @author Tom Beerbower
 */
@SuppressWarnings("rawtypes")
abstract class RunLevelBridge implements PostConstruct, PreDestroy {

    private final static Logger logger = Logger.getLogger(RunLevelBridge.class.getName());

    private final static Level level = AppServerStartup.level;

    @Inject
    private Habitat habitat;

    /**
     * The legacy type we are bridging to.
     */
    private final Class bridgeClass;

    /**
     * The class to stop during shutdown as well (optional).
     */
    private final Class additionalShutdownClass;


    // ----- Constructors ----------------------------------------------------

    RunLevelBridge(Class bridgeClass) {
        this.bridgeClass = bridgeClass;
        this.additionalShutdownClass = null;
    }

    RunLevelBridge(Class bridgeClass, Class additionalShutdownClass) {
        this.bridgeClass = bridgeClass;
        this.additionalShutdownClass = additionalShutdownClass;
    }


    // ----- PostConstruct ---------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public void postConstruct() {
        List<Inhabitant<?>> inhabitants = sort(habitat.getInhabitants(bridgeClass));
        for (Inhabitant<?> i : inhabitants) {
            if (qualifies(true, i)) {
                long start = System.currentTimeMillis();
                logger.log(level, "starting {0}", i);
                try {
                    activate(i);
                } catch (RuntimeException e) {
                    logger.log(Level.SEVERE, "problem starting {0}: {1}", new Object[] {i, e.getMessage()});
                    logger.log(level, "nested error", e);
                    throw e;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "problem starting {0}: {1}", new Object[] {i, e.getMessage()});
                    logger.log(level, "nested error", e);
                }
                if (logger.isLoggable(level)) {
                    logger.log(level, "start of " + i + " done in "
                        + (System.currentTimeMillis() - start) + " ms");
                }
            }
        }
    }


    // ----- PreDestroy ------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public void preDestroy() {
        List<Inhabitant<?>> inhabitants;
        if (null == additionalShutdownClass) {
            inhabitants = sort(habitat.getInhabitants(bridgeClass));
        } else {
            // Startup and PostStartup are merged according to their priority level and released
            Collection<Inhabitant<?>> inhabitants1 = habitat.getInhabitants(bridgeClass);
            Collection<Inhabitant<?>> inhabitants2 = habitat.getInhabitants(additionalShutdownClass);

            inhabitants = new ArrayList<Inhabitant<?>>();
            inhabitants.addAll(inhabitants2);
            inhabitants.addAll(inhabitants1);

            Collections.reverse(sort(inhabitants));
        }
        
        for (Inhabitant<?> i : inhabitants) {
            if (qualifies(false, i)) {
                logger.log(level, "releasing {0}", i);
                try {
                    deactivate(i);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "problem releasing {0}: {1}", new Object[] {i, e.getMessage()});
                    logger.log(level, "nested error", e);
                }
            }
        }
    }


    // ----- helper methods --------------------------------------------------

    /**
     * Activate the given inhabitant.
     *
     * @param i  the inhabitant
     */
    protected void activate(Inhabitant<?> i) {
        i.get();
    }

    /**
     * Deactivate the given inhabitant.
     *
     * @param i  the inhabitant
     */
    protected void deactivate(Inhabitant<?> i) {
        i.release();
    }

    /**
     * Determine if the given {@link Inhabitant} is using the new {@link RunLevel}-based mechanism.
     *
     * @return true if the given inhabitant is annotated with a run level annotation
     */
    protected boolean qualifies(boolean startup, Inhabitant<?> i) {
        RunLevel rl = i.type().getAnnotation(RunLevel.class);
        if (startup) {
            // in this case we handle anything that doesn't have a RunLevel annotation on it
            return (null == rl);
        } else {
            // in shutdown case we forcibly stop anything without a RunLevel OR anything that is
            // not a "strict" type RunLevel - see javadoc and site documentation for details.
            return (i.isActive() && (null == rl || !rl.strict()));
        }
    }

    /**
     * Get a sorted {@link List} from the the given {@link Collection}, reusing the same
     * {@link Collection} if provided as a {@link List} type.
     *
     * @return the sorted list
     */
    @SuppressWarnings("unchecked")
    protected List<Inhabitant<?>> sort(Collection<Inhabitant<?>> coll) {
        List<Inhabitant<?>> sorted = (List.class.isInstance(coll)) ?
                List.class.cast(coll) : new ArrayList<Inhabitant<?>>(coll);

        if (sorted.size() > 1) {
            logger.log(level, "sorting {0},{1}", new Object[] {bridgeClass, additionalShutdownClass});
            Collections.sort(sorted, getInhabitantComparator());
        }
        return sorted;
    }

    /**
     * Get a comparator based on {@link Inhabitant} priority.
     *
     * @return a comparator
     */
    private static Comparator<Inhabitant<?>> getInhabitantComparator() {
        return new Comparator<Inhabitant<?>>() {
            public int compare(Inhabitant<?> o1, Inhabitant<?> o2) {
                int o1level = (o1.type().getAnnotation(Priority.class)!=null?
                        o1.type().getAnnotation(Priority.class).value():5);
                int o2level = (o2.type().getAnnotation(Priority.class)!=null?
                        o2.type().getAnnotation(Priority.class).value():5);
                if (o1level==o2level) {
                    return 0;
                } else if (o1level<o2level) {
                    return -1;
                } else return 1;
            }
        };
    }
}
