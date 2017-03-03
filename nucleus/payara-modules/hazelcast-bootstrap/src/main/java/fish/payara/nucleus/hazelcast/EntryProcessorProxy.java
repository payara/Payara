/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014,2015,2016,2017 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.hazelcast;

import fish.payara.nucleus.hazelcast.JavaEEContextUtil.Context;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import org.glassfish.internal.api.ServerContext;

/**
 * push Java EE environment before invoking delegate
 *
 * @author lprimak
 * @param <K>
 * @param <V>
 * @param <T>
 */
public class EntryProcessorProxy<K, V, T> implements EntryProcessor<K, V, T>{
    public EntryProcessorProxy(EntryProcessor<K, V, T> delegate, ServerContext serverContext) {
        this.delegate = delegate;
        ctxUtil = new JavaEEContextUtil(serverContext);
    }


    @Override
    public T process(MutableEntry<K, V> me, Object... os) throws EntryProcessorException {
        Context ctx = null;
        try {
            ctx = ctxUtil.preInvoke();
            return delegate.process(me, os);
        }
        finally {
            ctxUtil.postInvoke(ctx);
        }
    }

    private final EntryProcessor<K, V, T> delegate;
    private final JavaEEContextUtil ctxUtil;
}
