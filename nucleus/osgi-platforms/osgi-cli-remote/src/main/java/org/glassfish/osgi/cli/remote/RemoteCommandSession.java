/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.osgi.cli.remote;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.UUID;
import org.apache.felix.service.command.CommandSession;

/**
 * This delegating class is used to overcome some limitations of the
 * {@link CommandSession} interface when it comes to session management.
 * 
 * <p>
 * Once implementations are mature enough to not assume environmental behavior
 * this class will become obsolete.
 * </p>
 *
 * @author ancoron
 */
public class RemoteCommandSession {

    private final CommandSession delegate;
    private final String id;

    public RemoteCommandSession(CommandSession delegate)
    {
        this.delegate = delegate;
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Get the identifier for this session, which is a UUID of type 4.
     * 
     * @return 
     */
    public String getId() {
        return id;
    }

    /**
     * Attached the specified streams to the delegate of this instance and
     * returns the modified delegate.
     * 
     * @param in The "stdin" stream for the session
     * @param out The "stdout" stream for the session
     * @param err The "stderr" stream for the session
     * 
     * @return The modified {@link CommandSession} delegate
     * 
     * @see #detach()
     */
    public CommandSession attach(InputStream in, PrintStream out, PrintStream err) {
        set(this.delegate, "in", in);
        set(this.delegate, "out", out);
        set(this.delegate, "err", err);
        return this.delegate;
    }

    /**
     * Detaches all previously attached streams and hence, ensures that there
     * are no stale references left.
     * 
     * @see #attach(java.io.InputStream, java.io.PrintStream, java.io.PrintStream)
     */
    public void detach() {
        set(this.delegate, "in", null);
        set(this.delegate, "out", null);
        set(this.delegate, "err", null);
    }

    private void set(final Object obj, final String field, final Object value) {
        try {
            final Field f = obj.getClass().getDeclaredField(field);
            final boolean accessible = f.isAccessible();
            if(!accessible) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        f.setAccessible(true);
                        try {
                            f.set(obj, value);
                        } catch(Exception x) {
                            throw new RuntimeException(x);
                        }

                        // reset to previous state...
                        f.setAccessible(accessible);
                        return null;
                    }
                });
            } else {
                f.set(obj, value);
            }
        } catch(Exception x) {
            throw new RuntimeException(x);
        }
    }
}
