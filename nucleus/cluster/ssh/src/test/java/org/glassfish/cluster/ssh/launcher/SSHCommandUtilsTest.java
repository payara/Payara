/*
 * Portions Copyright 2026 Payara Foundation and/or its affiliates
 */
package org.glassfish.cluster.ssh.launcher;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class SSHCommandUtilsTest {

    @Test
    public void listInputStream_usesUnixNewlineRegardlessOfPlatform() throws IOException {
        List<String> lines = List.of("AS_ADMIN_PASSWORD=secret", "AS_ADMIN_MASTERPASSWORD=changeit");
        byte[] bytes = readAll(SSHCommandUtils.listInputStream(lines));
        String result = new String(bytes, StandardCharsets.UTF_8);

        // Must contain exactly \n (0x0A), never \r (0x0D) as line terminator
        assertFalse("stdin stream must not contain carriage returns", result.contains("\r"));
        assertEquals(
                "AS_ADMIN_PASSWORD=secret\nAS_ADMIN_MASTERPASSWORD=changeit\n",
                result);
    }

    @Test
    public void listInputStream_returnsNullForNullInput() throws IOException {
        assertNull(SSHCommandUtils.listInputStream(null));
    }

    private static byte[] readAll(InputStream is) throws IOException {
        if (is == null) return null;
        return is.readAllBytes();
    }
}
