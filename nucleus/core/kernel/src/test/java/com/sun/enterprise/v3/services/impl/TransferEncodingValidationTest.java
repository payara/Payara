/*
 * Copyright 2026 Payara Foundation and/or its affiliates. All rights reserved.
 */
package com.sun.enterprise.v3.services.impl;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

/**
 * FISH-14432: unit tests for {@link GlassfishNetworkListener#isConformantTransferEncoding(java.util.List)}.
 * Each case lists the raw Transfer-Encoding header line value(s) a request would carry.
 */
public class TransferEncodingValidationTest {

    // --- conformant: must be accepted ---

    @Test
    public void noTransferEncodingIsAccepted() {
        Assert.assertTrue(GlassfishNetworkListener.isConformantTransferEncoding(Collections.emptyList()));
    }

    @Test
    public void chunkedAloneIsAccepted() {
        Assert.assertTrue(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("chunked")));
    }

    @Test
    public void chunkedIsCaseInsensitive() {
        Assert.assertTrue(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("Chunked")));
    }

    @Test
    public void chunkedWithSurroundingWhitespaceIsAccepted() {
        Assert.assertTrue(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList(" chunked ")));
    }

    @Test
    public void gzipThenChunkedIsRejected() {
        // variant B: Grizzly cannot decode the gzip transfer-coding, so only plain chunked is accepted
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("gzip, chunked")));
    }

    @Test
    public void gzipThenChunkedAcrossTwoLinesIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Arrays.asList("gzip", "chunked")));
    }

    // --- non-conformant: must be rejected ---

    @Test
    public void obfuscatedChunkedXIsRejected() {
        // variant A / C
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("chunkedX")));
    }

    @Test
    public void xchunkedIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("xchunked")));
    }

    @Test
    public void chunkedNotFinalOnOneLineIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("chunked, gzip")));
    }

    @Test
    public void chunkedThenGzipAcrossTwoLinesIsRejected() {
        // variant D: the finding that survives the upstream Grizzly fix
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Arrays.asList("chunked", "gzip")));
    }

    @Test
    public void repeatedChunkedIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("chunked, chunked")));
    }

    @Test
    public void unknownCodingAloneIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("gzip")));
    }

    @Test
    public void identityAloneIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("identity")));
    }

    @Test
    public void emptyValueIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("")));
    }

    @Test
    public void trailingEmptyTokenIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList("chunked,")));
    }

    @Test
    public void leadingEmptyTokenIsRejected() {
        Assert.assertFalse(GlassfishNetworkListener.isConformantTransferEncoding(Collections.singletonList(", chunked")));
    }
}
