/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.Set;

/**
 * A Junit4 test to test out the parser with various command lines. It works with examination of FirstPassResult
 * and SecondPassResult.
 *
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 */
public class ParserTest {

    @Test
    public void dummy() { }
    /*
     * Commented out until I get a chance to convert this to new classes.
    @Test(expected = ParserException.class)
    public void handleUnsupportedLegacyCommandMethod() throws ParserException {
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(new String[]{"create-cluster", "create-instance"});
    }

    @Test
    public void testHost1() throws ParserException {
        final String value = "1.2.3.4";
        final String CMD = "foo";
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(new String[]{CMD, Option.toCommandLineOption(HOST), value});  // foo --host 1.2.3.4
        Set<Option> pos = r.getProgramOptions();
        Option host = getOptionNamed(pos, HOST);
        assertEquals(r.getCommandName(), CMD);
        assertNotNull(host);
        assertEquals(value, host.getEffectiveValue());

        r.parseMetaOptions(new String[]{CMD, Option.toCommandLineOption(HOST_SYMBOL), value});
        pos = r.getProgramOptions();
        host = getOptionNamed(pos, HOST);
        assertEquals(r.getCommandName(), CMD);
        assertNotNull(host);
        assertEquals(value, host.getEffectiveValue());
    }

    @Test
    public void testHost2() throws ParserException {
        final String value = "1.2.3.4";
        final String CMD = "foo";
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(new String[]{CMD, Option.toCommandLineOption(HOST) + "=" + value});  // foo --host=1.2.3.4
        Set<Option> pos = r.getProgramOptions();
        Option host = getOptionNamed(pos, HOST);
        assertEquals(r.getCommandName(), CMD);
        assertNotNull(host);
        assertEquals(value, host.getEffectiveValue());

        r.parseMetaOptions(new String[]{CMD, Option.toCommandLineOption(HOST_SYMBOL) + "=" + value});
        pos = r.getProgramOptions();
        host = getOptionNamed(pos, HOST);
        assertEquals(r.getCommandName(), CMD);
        assertNotNull(host);
        assertEquals(value, host.getEffectiveValue());
    }

    @Test
    public void testAnyTwo1() throws ParserException {
        String hv = "foo.sun.com", pv = "3355", CMD = "bar";
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(new String[]{CMD, Option.toCommandLineOption(HOST), hv, Option.toCommandLineOption(PORT), pv}); //bar --host foo.sun.com --port 3355
        Option host = getOptionNamed(r.getProgramOptions(), HOST);
        Option port = getOptionNamed(r.getProgramOptions(), PORT);
        assertEquals(host.getEffectiveValue(), hv);
        assertEquals(port.getEffectiveValue(), pv);

        r.parseMetaOptions(new String[]{CMD, Option.toCommandLineOption(HOST), hv, Option.toCommandLineOption(PORT_SYMBOL), pv}); //bar --host foo.sun.com --p 3355
        host = getOptionNamed(r.getProgramOptions(), HOST);
        port = getOptionNamed(r.getProgramOptions(), PORT);
        assertEquals(host.getEffectiveValue(), hv);
        assertEquals(port.getEffectiveValue(), pv);
    }

    @Test
    public void testDefaults() throws ParserException {
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(new String[]{"cmd"});
        Option host = getOptionNamed(r.getProgramOptions(), HOST);
        assertTrue("default value of host is not: " + Constants.DEFAULT_HOST, Constants.DEFAULT_HOST.equals(host.getEffectiveValue()));

        Option port = getOptionNamed(r.getProgramOptions(), PORT);
        int pn = Integer.parseInt(port.getEffectiveValue());
        assertEquals(Constants.DEFAULT_PORT, pn);
    }

    @Test(expected = ParserException.class)
    public void testLimitation1() throws ParserException {
        String[] cmdline = new String[]{"cmd", "--host", "1.2.3.4", "--host", "5.6.7.8", "-p=4555", "-g"};
        //this should throw a ParserException because we know nothing about -g and the only way in which we could
        //support this is if option name/symbol + value is specified as a single argument, i.e. -g=value or --gggg=value
        //this is the parser limitation as it does not have the metadata for the command!
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
    }

    @Test
    public void testWorkAroundLimitation1() throws ParserException {
        //this is opposite of testLimitation1
        String[] cmdline = new String[]{"cmd", "--host", "1.2.3.4", "-p=4555", "-g=some value"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
        String[] ca = r.getCommandArguments();
        assertArrayEquals("arrays are not same", ca, new String[]{"-g=some value"}); //note that first pass always removes -- or -
    }

    @Test
    public void testIntermingled1() throws ParserException {
        //this uses the work-around
        String[] cmdline = new String[]{"create-jdbc-resource", "--cmdopt=abcde", "-H", "internal.sun.com", "--port=9999", "-s"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
        assertArrayEquals("arrays are not same", r.getCommandArguments(), new String[]{"--cmdopt=abcde"});
        assertTrue(r.usesDeprecatedSyntax());
    }

    @Test
    public void testBooleanOptionList() throws ParserException {
        String[] cmdline = new String[]{"cmd", "-eIt", "--", "abc"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
        Option bo = getOptionNamed(r.getProgramOptions(), INTERACTIVE);
        assertEquals(bo.getEffectiveValue(), "true");
        bo = getOptionNamed(r.getProgramOptions(), TERSE);
        assertEquals(bo.getEffectiveValue(), "true");
        bo = getOptionNamed(r.getProgramOptions(), ECHO);
        assertEquals(bo.getEffectiveValue(), "true");
    }

    @Test(expected = ParserException.class)
    public void rejectPassword1() throws ParserException {
        String[] cmdline = new String[]{"cmd", "--password=secret"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
    }

    @Test(expected = ParserException.class)
    public void rejectPassword2() throws ParserException {
        String[] cmdline = new String[]{"cmd", "-w", "secret"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
    }

    @Test
    public void assertDeprecated1() throws ParserException {
        String cmd = "create-http-listener"; //this is a legacy command, now, we will specify it the legacy way, deliberately.
        String[] cmdline = new String[]{cmd, "--host=localhost", "--port=1234", "--secure", "true", "listener1"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
        assertTrue(r.usesDeprecatedSyntax());
    }

    @Test
    public void assertDeprecated2() throws ParserException {
        String cmd = "possibly-new-command"; //this is a new command, but it is using the old syntax, it can't reuse the asadmin options then!
        String[] cmdline = new String[]{cmd, "--host=localhost", "--port=1234", "--secure", "true", "listener1"};
        CommandRunner r = new CommandRunner(System.out, System.err);
        r.parseMetaOptions(cmdline);
        assertTrue(r.usesDeprecatedSyntax());  //this is deprecated as --host can't be reused by possibly-new-command
    }

    static Option getOptionNamed(Set<Option> ops, String name) {
        for (Option op : ops)
            if (op.getName().equals(name))
                return op;
        return null;
    }
    */
}
