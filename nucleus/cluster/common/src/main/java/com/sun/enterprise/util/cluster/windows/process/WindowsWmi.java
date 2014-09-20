/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.cluster.windows.process;

import com.sun.enterprise.util.cluster.windows.SharedStrings;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jinterop.dcom.common.IJIUnreferenced;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JICallBuilder;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIFlags;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.jinterop.dcom.core.JIString;
import org.jinterop.dcom.core.JIVariant;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.jinterop.dcom.impls.automation.IJIEnumVariant;

/**
 * Windows Management Interface
 * Tests if it's possible to connect to WMI
 * @author bnevins
 */
public class WindowsWmi {
    private IJIDispatch dispatch;
    private JIComServer comStub;
    private IJIComObject unknown;
    private JISession session;
    private IJIComObject comObject;
    private Object[] crazyLongMicrosoftArgs;
    private JIVariant variant;
    private int count = -1;
    private String processInfo[];

    public WindowsWmi(WindowsCredentials bonafides) throws WindowsException {
        try {
            JISystem.getLogger().setLevel(Level.INFO);
            JISystem.setInBuiltLogHandler(false);
            JISystem.setAutoRegisteration(true);

            crazyLongMicrosoftArgs = new Object[]{
                new JIString(bonafides.getHost()),
                JIVariant.OPTIONAL_PARAM(),
                JIVariant.OPTIONAL_PARAM(),
                JIVariant.OPTIONAL_PARAM(),
                JIVariant.OPTIONAL_PARAM(),
                JIVariant.OPTIONAL_PARAM(),
                Integer.valueOf(0),
                JIVariant.OPTIONAL_PARAM()};

            session = JISession.createSession(bonafides.getDomain(),
                    bonafides.getUser(), bonafides.getPassword());
            session.useSessionSecurity(true);
            session.setGlobalSocketTimeout(5000);
            comStub = new JIComServer(JIProgId.valueOf("WbemScripting.SWbemLocator"),
                    bonafides.getHost(), session);

            unknown = comStub.createInstance();

            //ISWbemLocator
            comObject = (IJIComObject) unknown.queryInterface("76A6415B-CB41-11d1-8B02-00600806D9B6");
            //This will obtain the dispatch interface
            dispatch = (IJIDispatch) JIObjectFactory.narrowObject(comObject.queryInterface(IJIDispatch.IID));
            setCount();
            setInfo();
            killme();
        }
        catch (NoClassDefFoundError err) {
            throw new WindowsException(SharedStrings.get("missing_jinterop"));
        }
        catch (Exception e) {
            dispatch = null;
            count = -1;
        }

        if (!initialized())
            throw new WindowsException(Strings.get("WMI.init.error"));
    }

    public final int getCount() throws WindowsException {
        return count;
    }

    public final String[] getInfo() throws WindowsException {
        return processInfo;
    }

    private void setCount() throws WindowsException {
        try {
            JIVariant[] results = dispatch.callMethodA("ConnectServer", crazyLongMicrosoftArgs);
            IJIDispatch wbemServices_dispatch = (IJIDispatch) JIObjectFactory.narrowObject((results[0]).getObjectAsComObject());
            JIVariant[] results2 = wbemServices_dispatch.callMethodA("InstancesOf", new Object[]{new JIString("Win32_Process"), Integer.valueOf(0), JIVariant.OPTIONAL_PARAM()});
            IJIDispatch wbemObjectSet_dispatch = (IJIDispatch) JIObjectFactory.narrowObject((results2[0]).getObjectAsComObject());
            variant = wbemObjectSet_dispatch.get("_NewEnum");
            JIVariant Count = wbemObjectSet_dispatch.get("Count");
            count = Count.getObjectAsInt();
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
    }

    /**
     * not strictly necessary but it was so difficult to write the code that I can't
     * bear to throw it away!
     * @throws WindowsException
     */
    private void setInfo() throws WindowsException {
        try {
            processInfo = new String[count];
            IJIComObject comObj = variant.getObjectAsComObject();

            // todo is this needed?
            comObj.registerUnreferencedHandler(new IJIUnreferenced() {
                public void unReferenced() {
                }
            });

            IJIEnumVariant enumVARIANT =
                    (IJIEnumVariant) JIObjectFactory.narrowObject(comObj.queryInterface(IJIEnumVariant.IID));

            for (int i = 0; i < count; i++) {
                Object[] values = enumVARIANT.next(1);
                JIArray array = (JIArray) values[0];
                Object[] arrayObj = (Object[]) array.getArrayInstance();
                for (int j = 0; j < arrayObj.length; j++) {
                    IJIDispatch wbemObject_dispatch = (IJIDispatch) JIObjectFactory.narrowObject(((JIVariant) arrayObj[j]).getObjectAsComObject());
                    JIVariant variant2 = (JIVariant) (wbemObject_dispatch.callMethodA("GetObjectText_", new Object[]{Integer.valueOf(1)}))[0];

                    // normally arrayObj.length is 1
                    if (j == 0)
                        processInfo[i] = variant2.getObjectAsString().getString();
                }
            }
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
    }

    private void killme() throws JIException {
        JISession.destroySession(session);
    }

    private boolean initialized() {
        return dispatch != null;
    }
}
