/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright 2022-2024 Payara Foundation and/or its affiliates

package org.apache.catalina.core;


public class Constants {

    public static final String PACKAGE = "org.apache.catalina.core";
    public static final int MAJOR_VERSION = 6;
    public static final int MINOR_VERSION = 1;

    public static final String JSP_SERVLET_CLASS
            = "org.glassfish.wasp.servlet.JspServlet";

    public static final String OLD_JSP_SERVLET_CLASS
            = "org.apache.jasper.servlet.JspServlet";

    public static final String JSP_SERVLET_NAME = "jsp";

    public static final String DEFAULT_SERVLET_NAME = "default";

    public static final String IS_DEFAULT_ERROR_PAGE_ENABLED_INIT_PARAM
            = "org.glassfish.web.isDefaultErrorPageEnabled";

    public static final String COOKIE_COMMENT_ATTR = "Comment";

    public static final String COOKIE_DOMAIN_ATTR = "Domain";

    public static final String COOKIE_HTTP_ONLY_ATTR = "HttpOnly";

    public static final String COOKIE_MAX_AGE_ATTR = "Max-Age"; // cookies auto-expire

    public static final String COOKIE_PATH_ATTR = "Path";

    public static final String COOKIE_SAME_SITE_ATTR = "SameSite";

    public static final String COOKIE_SECURE_ATTR = "Secure"; // cookies over SSL

}
