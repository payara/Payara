/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.hazelcast;

import org.glassfish.internal.api.JavaEEContextUtil;
import com.hazelcast.internal.serialization.impl.JavaDefaultSerializers;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import java.io.IOException;

/**
 *
 * @author lprimak
 */
public class PayaraHazelcastSerializer implements StreamSerializer<Object> {
    @SuppressWarnings("unchecked")
    public PayaraHazelcastSerializer(JavaEEContextUtil ctxUtil, StreamSerializer<?> delegate) {
        this.ctxUtil = ctxUtil;
        this.delegate = delegate != null? (StreamSerializer<Object>)delegate : new JavaDefaultSerializers.JavaSerializer(true, false);
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
