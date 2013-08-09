/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.wadl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.stream.StreamResult;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.composite.LegacyCompositeResource;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author jdlee
 */
@Path("/schema.xsd")
@Service
public class RestModelSchemaResource extends LegacyCompositeResource {

    @GET
    @Path("old")
    public String getSchema() throws JAXBException, IOException {
        Set<Class<?>> classes = new TreeSet<Class<?>>(new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> t, Class<?> t1) {
                return t.getName().compareTo(t1.getName());
            }
        });
        for (String c : locateRestModels()) {
            try {
                Class<?> modelClass = loadClass(c);
                if (modelClass.getSimpleName().charAt(0) < 'C') {
                    classes.add(getCompositeUtil().getModel(modelClass).getClass());
                }
            } catch (ClassNotFoundException ex) {
                RestLogging.restLogger.log(Level.WARNING, null, ex);
            }
        }
        JAXBContext jc = JAXBContext.newInstance(classes.toArray(new Class<?>[classes.size()]));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        jc.generateSchema(new MySchemaOutputResolver(baos));
        return new String(baos.toByteArray());
    }

    @GET
    @Path("test1")
    public String getSchema1() throws JAXBException, IOException {
        Set<Class<?>> classes = new TreeSet<Class<?>>(new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> t, Class<?> t1) {
                return t.getName().compareTo(t1.getName());
            }
        });
        try {
            Class<?> modelClass = loadClass("org.glassfish.admin.rest.resources.composite.Job");
            classes.add(getCompositeUtil().getModel(modelClass).getClass());
            modelClass = loadClass("org.glassfish.admin.rest.resources.composite.Dummy");
            classes.add(getCompositeUtil().getModel(modelClass).getClass());
        } catch (ClassNotFoundException ex) {
            RestLogging.restLogger.log(Level.WARNING, null, ex);
        }
        JAXBContext jc = JAXBContext.newInstance(classes.toArray(new Class<?>[classes.size()]));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        jc.generateSchema(new MySchemaOutputResolver(baos));
        return new String(baos.toByteArray());
    }

    @GET
    public String getSchemaManually() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<xs:schema version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
//                + "\t<xs:element type=\"xs:object\" name=\"object\"/>\n");
        StringBuilder complexTypes = new StringBuilder();
        addElement(sb, "object");
        processClass(complexTypes, Object.class, "object");
        for (String c : locateRestModels()) {
            try {
                Class<?> modelClass = getCompositeUtil().getModel(loadClass(c)).getClass();
                String simpleName = modelClass.getSimpleName().toLowerCase(Locale.getDefault());
                if (simpleName.endsWith("impl")) {
                    simpleName = simpleName.substring(0, simpleName.length() -4);
                }
                addElement(sb, simpleName);
                processClass(complexTypes, modelClass, simpleName);
            } catch (ClassNotFoundException ex) {
                RestLogging.restLogger.log(Level.WARNING, null, ex);
            }
        }
        sb.append(complexTypes);

        return sb.append("</xs:schema>\n").toString();
    }

    /*
     * TODO: This is a bit ugly, but JAXB doesn't seem to like the way we're doing models. When
     * time permits, it may be best to revisit this and see why JAXB dies when given, for example,
     * a List or Object return type.  Once that's resolved, we may be able to use JAXB serialization.
     */
    protected void processClass(StringBuilder sb, Class<?> c, String simpleName) {
        sb.append("\t<xs:complexType name=\"")
                .append(simpleName)
                .append("\">\n\t\t<xs:sequence>\n");
        for (Class<?> i : c.getInterfaces()) {
            for (Method m : i.getDeclaredMethods()) {
                String name = m.getName();
                if (name.startsWith("get") && !"getClass".equals(name)) {
                    name = name.substring(3, 4).toLowerCase(Locale.getDefault()) + name.substring(4);
                    Class<?> returnType = m.getReturnType();
                    sb.append("\t\t\t<xs:element name=\"")
                            .append(name)
                            .append("\" ");
                    if (returnType.isPrimitive()) {
                        sb.append(getType(returnType));
                    } else if (WRAPPER_TYPES.contains(returnType) ||
                            String.class.equals(returnType) ||
                            Object.class.equals(returnType)) {
                        sb.append(getType(returnType))
                                .append(" minOccurs=\"0\"");
                    } else if (List.class.equals(returnType)) {
                        ParameterizedType rt = (ParameterizedType) m.getGenericReturnType();
                        returnType = (Class<?>)rt.getActualTypeArguments()[0];
                        sb.append(getType(returnType))
                                .append(" nillable=\"true\" minOccurs=\"0\" maxOccurs=\"unbounded\"");
                    }
                    sb.append("/>\n");
                }
            }
        }

        sb.append("\t\t</xs:sequence>\n\t</xs:complexType>\n");
    }

    private String getType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return "type=\"xs:" + clazz.getSimpleName() + "\"";
        } else if (WRAPPER_TYPES.contains(clazz)
                || String.class.equals(clazz)) {
            return "type=\"xs:"
                    + clazz.getSimpleName().toLowerCase(Locale.getDefault()) +"\"";
        } else {
            return "type=\"" + clazz.getSimpleName().toLowerCase(Locale.getDefault()) + "\"";
        }
    }

    private static final Set<Class<?>> WRAPPER_TYPES = new HashSet() {{
        add(Boolean.class);
        add(Character.class);
        add(Byte.class);
        add(Short.class);
        add(Integer.class);
        add(Long.class);
        add(Float.class);
        add(Double.class);
        add(Void.class);
    }};

    private Set<String> locateRestModels() {
        Set<String> classes = new HashSet<String>();

        List<ActiveDescriptor<?>> widgetDescriptors = Globals.getDefaultBaseServiceLocator()
                .getDescriptors(BuilderHelper.createContractFilter(RestModel.class.getName()));
        for (ActiveDescriptor ad : widgetDescriptors) {
            classes.add(ad.getImplementation());
        }

        return classes;
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException ex) {
            return getClass().getClassLoader().loadClass(className);
        }
    }

    private void addElement(StringBuilder sb, String simpleName) {
        sb.append("\t<xs:element name=\"")
                .append(simpleName)
                .append("\" type=\"")
                .append(simpleName)
                .append("\"/>\n");
    }

    private static class MySchemaOutputResolver extends SchemaOutputResolver {

        ByteArrayOutputStream baos;

        public MySchemaOutputResolver(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        @Override
        public javax.xml.transform.Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
            StreamResult r = new StreamResult("argh");
            r.setOutputStream(baos);
            return r;
        }
    }
}
