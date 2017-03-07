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

import com.hazelcast.internal.serialization.impl.JavaDefaultSerializers;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;
import org.glassfish.internal.api.ServerContext;

/**
 *
 * @author lprimak
 */
public class PayaraHazelcastSerializer implements StreamSerializer<Object> {
    public PayaraHazelcastSerializer(ServerContext serverContext) {
        ctxUtil = new JavaEEContextUtil(serverContext);
        delegate = new JavaDefaultSerializers.JavaSerializer(true, false);
    }

    @SuppressWarnings("unchecked")
    public PayaraHazelcastSerializer(ServerContext serverContext, StreamSerializer<?> delegate) {
        ctxUtil = new JavaEEContextUtil(serverContext);
        this.delegate = (StreamSerializer<Object>) delegate;
    }


    @Override
    public void write(ObjectDataOutput out, Object object) throws IOException {
        delegate.write(out, ctxUtil.getApplicationName());
        delegate.write(out, object);
    }

    @Override
    public Object read(ObjectDataInput in) throws IOException {
        String appName = (String)delegate.read(in);
        ctxUtil.setApplicationContext(appName);
        return delegate.read(in);
    }

    @Override
    public int getTypeId() {
        return 1;
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    private final JavaEEContextUtil ctxUtil;
    private final StreamSerializer<Object> delegate;
}
