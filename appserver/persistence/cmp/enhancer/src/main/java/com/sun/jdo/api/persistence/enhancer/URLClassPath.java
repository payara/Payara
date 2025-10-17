/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2022 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package com.sun.jdo.api.persistence.enhancer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

/**
 *
 * @author Gaurav Gupta
 */
public class URLClassPath {

    private static final String URL_CLASSPATH_CLASS_NAME = "jdk.internal.loader.URLClassPath";
    private static Constructor constructor;
    private static final String GET_RESOURCE_METHOD_NAME = "getResource";
    private static Method getResource;

    private Object instance;

    public URLClassPath(URL[] urls, AccessControlContext acc) {
        try {
            if (constructor == null) {
                Class clazz = Class.forName(URL_CLASSPATH_CLASS_NAME);
                constructor = clazz.getConstructor(URL[].class, AccessControlContext.class);
            }
            instance = constructor.newInstance(urls, acc);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

     Resource getResource(String name, boolean check) {
        if (getResource == null) {
            getResource = getMethod(instance, GET_RESOURCE_METHOD_NAME, String.class, boolean.class);
        }
        return new Resource(invoke(instance, getResource, name, check));
    }

    static Method getMethod(Object instance, String name, Class<?>... parameterTypes) {
        try {
            return instance.getClass().getMethod(name, parameterTypes);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static Object invoke(Object instance, Method method, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}

class Resource {

    private final Object instance;
    private static final String GET_CODE_SOURCE_URL_METHOD_NAME = "getCodeSourceURL";
    private static Method getCodeSourceURL;
    private static final String GET_MANIFEST_METHOD_NAME = "getManifest";
    private static Method getManifest;
    private static final String GET_BYTES_METHOD_NAME = "getBytes";
    private static Method getBytes;
    private static final String GET_CERTIFICATES_METHOD_NAME = "getCertificates";
    private static Method getCertificates;

    public Resource(Object instance) {
        this.instance = instance;
    }

    public URL getCodeSourceURL() {
        if (getCodeSourceURL == null) {
            getCodeSourceURL = URLClassPath.getMethod(instance, GET_CODE_SOURCE_URL_METHOD_NAME);
        }
        return (URL) URLClassPath.invoke(instance, getCodeSourceURL);
    }

    public Manifest getManifest() throws IOException {
        if (getManifest == null) {
            getManifest = URLClassPath.getMethod(instance, GET_MANIFEST_METHOD_NAME);
        }
        return (Manifest) URLClassPath.invoke(instance, getManifest);
    }

    public byte[] getBytes() throws IOException {
        if (getBytes == null) {
            getBytes = URLClassPath.getMethod(instance, GET_BYTES_METHOD_NAME);
        }
        return (byte[]) URLClassPath.invoke(instance, getBytes);
    }

    public Certificate[] getCertificates() {
        if (getCertificates == null) {
            getCertificates = URLClassPath.getMethod(instance, GET_CERTIFICATES_METHOD_NAME);
        }
        return (Certificate[]) URLClassPath.invoke(instance, getCertificates);
    }
}
