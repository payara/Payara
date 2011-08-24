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

package org.glassfish.virtualization.util;

import org.glassfish.virtualization.spi.ListenableFuture;
import org.glassfish.virtualization.spi.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 8/3/11
 * Time: 1:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class ListenableFutureImpl<T extends Enum, U> implements ListenableFuture<T, U>, EventSource<T> {

    final CountDownLatch latch;
    final EventSource<T> sink;
    final U guarded;
    final List<Tuple> listeners = new ArrayList<Tuple>();

    public ListenableFutureImpl(CountDownLatch latch, U guarded, EventSource<T> sink) {
        this.latch = latch;
        this.guarded = guarded;
        this.sink = sink;
    }

    @Override
    public void addListener(Listener<T> tListener, ExecutorService executor) {
        sink.addListener(tListener, executor);
    }

    @Override
    public T getCurrentPhase() {
        return null;
    }

    public void fireEvent(final T phase) {
        sink.fireEvent(phase);
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return latch.getCount() != 0;
    }

    @Override
    public U get() throws InterruptedException, ExecutionException {
        latch.await();
        return guarded;
    }

    @Override
    public U get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, unit);
        return guarded;
    }

    private class Tuple {
        final Listener<T> listener;
        final ExecutorService executorService;

        private Tuple(Listener<T> listener, ExecutorService executorService) {
            this.listener = listener;
            this.executorService = executorService;
        }
    }
}
