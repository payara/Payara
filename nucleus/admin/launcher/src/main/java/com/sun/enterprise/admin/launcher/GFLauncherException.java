/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * The one and only type of Exception that will be thrown out of this package.
 * I18N is wired in.  If a String message is found in the resource bundle, it will
 * use that String.  If not, it will use the String itself.
 * @author bnevins
 */
public class GFLauncherException extends Exception {

    /**
     * 
     * @param msg The message is either pointing at a I18N key in the resource 
     * bundle or will be treated as a plain string.
     */
    public GFLauncherException(String msg)
    {
        super(strings.get(msg));
    }

    /**
     * 
     * @param msg The message is either pointing at a I18N key in the resource 
     * bundle or will be treated as a plain string that will get formatted with
     * objs.
     * @param objs Objects used for formatting the message.
     */
    public GFLauncherException(String msg, Object... objs)
    {
        super(strings.get(msg, objs));
    }

    /**
     * 
     * @param msg The message is either pointing at a I18N key in the resource 
     * bundle or will be treated as a plain string.
     * @param t The causing Throwable.
     */
    public GFLauncherException(String msg, Throwable t)
    {
        super(strings.get(msg), t);
    }

    /**
     * 
     * @param msg The message is either pointing at a I18N key in the resource 
     * bundle or will be treated as a plain string that will get formatted with
     * objs.
     * @param t The causing Throwable.
     * @param objs Objects used for formatting the message.
     */
    public GFLauncherException(String msg, Throwable t, Object... objs)
    {
        super(strings.get(msg, objs), t);
    }
    /**
     * 
     * @param t The causing Throwable.
     */
    public GFLauncherException(Throwable t)
    {
        super(t);
    }
    private final static LocalStringsImpl strings = new LocalStringsImpl(GFLauncherException.class);
}
