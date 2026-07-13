/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
// Portions Copyright [2026] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.util;

import com.sun.enterprise.util.net.NetUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.security.auth.login.LoginException;

import org.jvnet.hk2.annotations.Service;

/**
 * Tracks failed authentication attempts and enforces exponential delay to prevent brute-force attacks.
 * <p>
 * This service tracks failed login attempts per (username, remoteHost) tuple and applies exponential
 * delays after failures. If too many concurrent requests are being delayed for the same user/host,
 * additional requests are rejected immediately with HTTP 429 (Too Many Requests).
 *
 * @author Ondro Mihalyi
 */
@Service
@Singleton
public class AuthenticationAttemptTracker {

    private static final Logger LOG = System.getLogger(AuthenticationAttemptTracker.class.getName());

    /**
     * Sentinel username used to track failed authentication attempts for non-existent users.
     * All attempts with unknown usernames are grouped under this key per remote host,
     * preventing username enumeration attacks and unbounded tracker map growth.
     */
    public static final String UNKNOWN_USER_KEY = "__unknown_user__";

    // Configuration constants
    static final int MAX_DELAY_SECONDS = 60; // 1 minute
    static final int FAILURE_COUNT_REACHING_MAX_DELAY = (int) Math.floor(Math.sqrt(MAX_DELAY_SECONDS));
    static final int MAX_CONCURRENT_DELAYS = 3;
    static final int WARN_THRESHOLD_FAILURES = 5;

    @Inject
    private ScheduledExecutorService scheduledExecutor;

    private final Map<AttemptKey, AttemptData> attempts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Schedule periodic cleanup of old entries
        scheduledExecutor.scheduleAtFixedRate(
                this::cleanupOldEntries,
                MAX_DELAY_SECONDS,
                MAX_DELAY_SECONDS,
                TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        attempts.clear();
    }

    /**
     * Checks whether authentication should be rejected for the given user and host because too many
     * concurrent requests are already being delayed. Must be called BEFORE attempting authentication.
     * <p>
     * Skips tracking for localhost — those are server-side proxy calls (Admin Console calling
     * /management/sessions on behalf of a browser). The real remote host is passed via
     * X-GlassFish-Remote-Host header in that case.
     *
     * @param username the username attempting to authenticate
     * @param remoteHost the remote host from which the request originated
     * @param password the password being used (empty passwords are ignored for tracking)
     * @throws TooManyRequestsException if too many concurrent delays are active for this user/host
     */
    public void checkBeforeAuthentication(String username, String remoteHost, char[] password) throws TooManyRequestsException {
        // Skip tracking for null/empty inputs and localhost (server-side proxy calls)
        if (shouldIgnoreCheckes(username, remoteHost, password)) {
            return;
        }

        AttemptData data = attempts.get(new AttemptKey(username, remoteHost));
        if (data != null) {
            int concurrentDelays = data.concurrentDelays.get();
            if (concurrentDelays >= MAX_CONCURRENT_DELAYS) {
                LOG.log(Level.DEBUG, () -> "Too many concurrent authentication attempts for user=" + username
                        + ", host=" + remoteHost + ". Concurrent delays: " + concurrentDelays + ". Rejecting immediately.");
                throw new TooManyRequestsException(
                        "Too many concurrent authentication attempts. Please try again later.");
            }
        }
    }

    private static boolean shouldIgnoreCheckes(String username, String remoteHost, char[] password) {
        return username == null || remoteHost == null || isEmptyPassword(password) || NetUtils.isLocal(remoteHost);
    }

    /**
     * Records a successful authentication and resets the failure count for the given user/host.
     * Must be called AFTER successful authentication.
     *
     * @param username the username that successfully authenticated
     * @param remoteHost the remote host from which the request originated
     * @param password the password that was used (empty passwords are ignored for tracking)
     */
    public void recordSuccess(String username, String remoteHost, char[] password) {
        // Skip tracking for null/empty inputs and localhost
        if (shouldIgnoreCheckes(username, remoteHost, password)) {
            return;
        }

        if (null != attempts.remove(new AttemptKey(username, remoteHost))) {
            LOG.log(Level.DEBUG, () -> "Successful authentication for user=" + username
                    + ", host=" + remoteHost + ". Failure count reset.");
        }
    }

    /**
     * Records a failed authentication attempt and applies an exponential delay before returning.
     * Must be called AFTER authentication fails.
     *
     * @param username the username that failed to authenticate
     * @param remoteHost the remote host from which the request originated
     * @param password the password that was used (empty passwords are ignored for tracking)
     */
    public void recordFailureAndDelay(String username, String remoteHost, char[] password) {
        // Skip tracking for null/empty inputs and localhost
        if (shouldIgnoreCheckes(username, remoteHost, password)) {
            return;
        }

        AttemptData data = attempts.computeIfAbsent(new AttemptKey(username, remoteHost), k -> new AttemptData());

        int failureCount = data.failureCount.incrementAndGet();
        data.lastFailureTime.set(System.currentTimeMillis());

        // Calculate exponential delay
        int delaySeconds = failureCount - 1 < FAILURE_COUNT_REACHING_MAX_DELAY
                ? (int) Math.min(Math.pow(2, failureCount - 1), MAX_DELAY_SECONDS)
                : MAX_DELAY_SECONDS;
        boolean isAtDelayCap = delaySeconds >= MAX_DELAY_SECONDS;

        // Log at WARN on the 10th failure (first time the threshold is crossed) and the first time
        // the delay cap is reached; log at DEBUG for all other failures to avoid log spam.
        if (failureCount == WARN_THRESHOLD_FAILURES) {
            LOG.log(Level.WARNING, () -> "Failed authentication attempt #" + failureCount
                    + " for user=" + username + ", host=" + remoteHost
                    + ". Applying delay of " + delaySeconds + " seconds.");
        } else if (isAtDelayCap && data.delayCappedWarnLogged.compareAndSet(false, true)) {
            LOG.log(Level.WARNING, () -> "Failed authentication attempt #" + failureCount
                    + " for user=" + username + ", host=" + remoteHost
                    + ". Maximum delay cap of " + MAX_DELAY_SECONDS + " seconds reached.");
        } else {
            LOG.log(Level.DEBUG, () -> "Failed authentication attempt #" + failureCount
                    + " for user=" + username + ", host=" + remoteHost
                    + ". Applying delay of " + delaySeconds + " seconds.");
        }

        // Block the calling thread for the delay duration while tracking concurrent delayed requests.
        // The concurrent count is used by checkBeforeAuthentication() to reject excess requests.
        data.concurrentDelays.incrementAndGet();
        try {
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.DEBUG, () -> "Authentication delay interrupted for user=" + username + ", host=" + remoteHost);
        } finally {
            data.concurrentDelays.decrementAndGet();
        }
    }

    private void cleanupOldEntries() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(MAX_DELAY_SECONDS);
        attempts.entrySet().removeIf(entry -> entry.getValue().lastFailureTime.get() < cutoffTime);
        LOG.log(Level.DEBUG, () -> "Cleaned up old authentication attempt entries. Remaining entries: " + attempts.size());
    }

    /**
     * Checks if the password is null or empty. Empty passwords are not tracked to avoid
     * penalizing legitimate users who accidentally submit the form without entering a password.
     */
    private static boolean isEmptyPassword(char[] password) {
        return password == null || password.length == 0;
    }

    /**
     * Key for tracking attempts by username and remote host.
     */
    private static class AttemptKey {
        private final String username;
        private final String remoteHost;

        AttemptKey(String username, String remoteHost) {
            this.username = username;
            this.remoteHost = remoteHost;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AttemptKey that = (AttemptKey) o;
            return Objects.equals(username, that.username) && Objects.equals(remoteHost, that.remoteHost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, remoteHost);
        }
    }

    /**
     * Mutable state for a single (username, remoteHost) tracking entry.
     */
    private static class AttemptData {
        final AtomicInteger failureCount = new AtomicInteger(0);
        final AtomicLong lastFailureTime = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger concurrentDelays = new AtomicInteger(0);
        /** Ensures the WARN log for reaching the delay cap is emitted only once per entry. */
        final AtomicBoolean delayCappedWarnLogged = new AtomicBoolean(false);
    }

    /**
     * Thrown when too many concurrent authentication attempts are already being delayed for the
     * same (username, remoteHost) pair, resulting in an HTTP 429 response.
     */
    public static class TooManyRequestsException extends LoginException {
        public TooManyRequestsException(String message) {
            super(message);
        }
    }
}
