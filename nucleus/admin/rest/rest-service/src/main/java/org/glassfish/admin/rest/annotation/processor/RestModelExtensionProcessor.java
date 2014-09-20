/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.glassfish.admin.rest.composite.RestModelExtension;

/**
 * Hello world!
 *
 */
@SupportedAnnotationTypes("org.glassfish.admin.rest.composite.RestModelExtension")
public class RestModelExtensionProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
        Messager messager = processingEnv.getMessager();
        BufferedWriter bw = null;
        try {
            Map<String, List<String>> classes = new HashMap<String, List<String>>();

            for (TypeElement te : elements) {
                for (Element e : env.getElementsAnnotatedWith(te)) {
                    final RestModelExtension annotation = e.getAnnotation(RestModelExtension.class);
                    final String parent = annotation.parent();
                    List<String> list = classes.get(parent);
                    if (list == null) {
                        list = new ArrayList<String>();
                        classes.put(parent, list);
                    }
                    list.add(e.toString());
                }
            }

            if (!classes.isEmpty()) {
                final Filer filer = processingEnv.getFiler();
                FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/restmodelextensions");
                bw = new BufferedWriter(fo.openWriter());
                // parent model:model extension
                for (Map.Entry<String, List<String>> entry : classes.entrySet()) {
                    final String key = entry.getKey();
                    for (String ext : entry.getValue()) {
                        bw.write(key + ":" + ext + "\n");
                    }
                }
                bw.close();
            }
        } catch (IOException ex) {
            messager.printMessage(Kind.ERROR, ex.getLocalizedMessage());
            if (bw != null) {
                try {
                    bw.close();
                } catch (Exception e) {
                    
                }
            }
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
