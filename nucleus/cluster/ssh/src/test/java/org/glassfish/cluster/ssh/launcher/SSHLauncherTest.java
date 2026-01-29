/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package org.glassfish.cluster.ssh.launcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

public class SSHLauncherTest {

    private SSHLauncher sshLauncher;

    @Before
    public void clearAndInit() {
        sshLauncher = new SSHLauncher();
        sshLauncher.init(Logger.getLogger(SSHLauncherTest.class.getName()));
        System.clearProperty(SSHLauncher.TIMEOUT_PROPERTY);
    }

    @Test
    public void defaultTimeoutTest() {
        Assert.assertEquals(SSHLauncher.DEFAULT_TIMEOUT_MSEC, sshLauncher.getTimeout());
    }

    @Test
    public void customTimeoutTest() {
        final String customTimeout = "12345";
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertNotEquals(SSHLauncher.DEFAULT_TIMEOUT_MSEC, sshLauncher.getTimeout());
        Assert.assertEquals(Integer.parseInt(customTimeout), sshLauncher.getTimeout());
    }

    @Test
    public void nonParseableTimeoutIgnoredTest() {
        final String customTimeout = "asdsa";
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertEquals(SSHLauncher.DEFAULT_TIMEOUT_MSEC, sshLauncher.getTimeout());
    }

    @Test
    public void negativeTimeoutIgnoredTest() {
        final String customTimeout = "-123456";
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertEquals(SSHLauncher.DEFAULT_TIMEOUT_MSEC, sshLauncher.getTimeout());
    }

    @Test
    public void minimumTimeoutAllowedTest() {
        final String customTimeout = "1";
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertEquals(Integer.parseInt(customTimeout), sshLauncher.getTimeout());
    }

    @Test
    public void zeroTimeoutIgnoredTest() {
        final String customTimeout = "0";
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertEquals(SSHLauncher.DEFAULT_TIMEOUT_MSEC, sshLauncher.getTimeout());
    }

    @Test
    public void maximumTimeoutAllowedTest() {
        final String customTimeout = Integer.toString(Integer.MAX_VALUE);
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertEquals(Integer.parseInt(customTimeout), sshLauncher.getTimeout());
    }

    @Test
    public void overMaximumTimeoutIgnoredTest() {
        final String customTimeout = Integer.toString(Integer.MAX_VALUE) + 1;
        System.out.println(customTimeout);
        System.setProperty(SSHLauncher.TIMEOUT_PROPERTY, customTimeout);
        Assert.assertEquals(SSHLauncher.DEFAULT_TIMEOUT_MSEC, sshLauncher.getTimeout());
    }
}
