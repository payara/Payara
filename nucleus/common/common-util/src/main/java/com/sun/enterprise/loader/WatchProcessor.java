/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package com.sun.enterprise.loader;

import com.sun.enterprise.util.CULoggerInfo;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

class WatchProcessor {
    protected static final Logger LOGGER = CULoggerInfo.getLogger();
    protected final Path root;
    protected final WatchService watchService;
    protected boolean registered;
    protected boolean overflowed;
    protected boolean cancelled;
    private int subscribers = 1;

    public WatchProcessor(Path root, WatchService watchService) {
        this.root = root;
        this.watchService = watchService;
    }

    protected boolean overflowed() {
        overflowed = true;
        return false;
    }

    protected void register() {
        if (!registered) {
            registered = registerFilesystemWatch();
        }
    }

    protected boolean registerFilesystemWatch() {
        try {
            // we only care when a file is added to the directory
            AccessController.doPrivileged(
                    (PrivilegedExceptionAction<WatchKey>) () -> root.register(watchService, ENTRY_CREATE));
            return true;
        } catch (PrivilegedActionException e) {
            LOGGER.log(Level.WARNING, e, () -> "Failed to register watcher for " + root);
        }
        return false;
    }

    protected boolean created(Path filename) {
        return true;
    }

    protected boolean deleted(Path filename) {
        return true;
    }

    protected void cancelled() {
        this.cancelled = true;
    }

    protected boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Register additional subscriber to path.
     * @return
     */
    WatchProcessor subscribe() {
        subscribers++;
        return this;
    }

    boolean unsubscribe() {
        return --subscribers == 0;
    }
}
