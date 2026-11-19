/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License ("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 */
package org.glassfish.internal.api;

import org.glassfish.grizzly.http.util.HttpRequestURIDecoder;

/**
 * Shares Grizzly's URI backslash setting with the web container.
 */
public final class GrizzlyUriPolicy extends HttpRequestURIDecoder {
    public static final boolean ALLOW_BACKSLASH = HttpRequestURIDecoder.ALLOW_BACKSLASH;

    private GrizzlyUriPolicy() {
    }
}
