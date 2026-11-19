/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util;

import com.sun.enterprise.util.net.NetUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.jvnet.hk2.annotations.Service;

import static com.sun.enterprise.admin.util.AuthenticationAttemptTracker.MAX_DELAY_SECONDS;

/**
 * Provides {@link AuthenticationAttemptTracker}s, including the server-wide {@link #getDefault() default tracker},
 * and periodically cleans up old entries of every tracker it {@linkplain #create(Predicate) creates}.
 */
@Service
@Singleton
public class AuthenticationAttemptTrackerFactory {

    @Inject
    private ScheduledExecutorService scheduledExecutor;

    private final List<ScheduledFuture<?>> cleanupTasks = new CopyOnWriteArrayList<>();
    private final List<AuthenticationAttemptTracker> trackers = new CopyOnWriteArrayList<>();

    private AuthenticationAttemptTracker defaultTracker;

    @PostConstruct
    public void init() {
        defaultTracker = create(NetUtils::isLocal);
    }

    @PreDestroy
    public void shutdown() {
        cleanupTasks.forEach(task -> task.cancel(false));
        trackers.forEach(AuthenticationAttemptTracker::clear);
    }

    /**
     * Returns the server-wide tracker, which exempts local hosts. It is shared by every caller, so that
     * attempts are counted together whichever way the server is accessed.
     */
    public AuthenticationAttemptTracker getDefault() {
        return defaultTracker;
    }

    /**
     * Creates a tracker whose old entries are periodically cleaned up by this factory.
     *
     * @param isExemptHost tests whether attempts from a remote host are exempt from tracking
     */
    public AuthenticationAttemptTracker create(Predicate<String> isExemptHost) {
        AuthenticationAttemptTracker tracker = new AuthenticationAttemptTracker(isExemptHost);
        trackers.add(tracker);
        // Schedule periodic cleanup of old entries
        cleanupTasks.add(scheduledExecutor.scheduleAtFixedRate(
                tracker::cleanupOldEntries,
                MAX_DELAY_SECONDS,
                MAX_DELAY_SECONDS,
                TimeUnit.SECONDS));
        return tracker;
    }
}
