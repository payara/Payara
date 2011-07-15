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

package org.glassfish.admin.runtime.apt;

import com.sun.mirror.apt.*;
import com.sun.mirror.declaration.*;
import com.sun.mirror.type.*;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.management.modelmbean.*;
import javax.management.*;

import org.glassfish.admin.runtime.annotations.ManagedAttribute;
import org.glassfish.admin.runtime.annotations.MBean;
import org.glassfish.admin.runtime.annotations.ManagedOperation;

class RuntimeMgmtAptProcessor implements AnnotationProcessor {

    private final AnnotationProcessorEnvironment env;
    private final Messager msg;
    private final boolean debug;

    private String pojoName;
    private String pojoClassName;
    private String packageName;

    private ArrayList<ModelMBeanOperationInfo> mmoiArr;
    private ArrayList<ModelMBeanAttributeInfo> mmaiArr;
    private HashMap<String, ArrayList> attrMap;
    private ModelMBeanInfo mmbinfo;

    private void init() {
        mmoiArr = new ArrayList<ModelMBeanOperationInfo>();
        mmaiArr = new ArrayList<ModelMBeanAttributeInfo>();
        attrMap = new HashMap<String, ArrayList>();
        mmbinfo = null;
    }

    public RuntimeMgmtAptProcessor(AnnotationProcessorEnvironment env) {
        this.env = env;
        msg = env.getMessager();
        debug = env.getOptions().containsKey("-Adebug");
    }
    
    public void process() {
        for (TypeDeclaration decl : env.getSpecifiedTypeDeclarations()) {
            if (debug) {
                msg.printNotice("type = " + decl.getQualifiedName());
                msg.printNotice("package = " + decl.getPackage().getQualifiedName());
            }
            generateMBeanInfo(decl);
        }
    }

    private void generateMBeanInfo(TypeDeclaration decl) {

        init();

        if (decl.getAnnotation(MBean.class) == null) return;

        pojoName = decl.getSimpleName();
        pojoClassName = decl.getQualifiedName();
        packageName = decl.getPackage().getQualifiedName();

        if (debug)
            msg.printNotice("generating mbeaninfo ...");
        try {
            // generate attributes and operations
            /*
            for (MethodDeclaration mdecl : decl.getMethods()) {
                generateMethods(mdecl);
            }
            */

            ClassDeclaration cd = (ClassDeclaration) decl;
            msg.printNotice("before super classdecl = " + cd);
            processSuper((ClassDeclaration) decl);

            msg.printNotice("before attrinfo ...");
            generateAttrInfo();

            // mbean info
            Descriptor descriptor = new DescriptorSupport(new String[] {
                ("name=" + pojoName),
                "descriptorType=mbean",
                ("displayName=" + pojoClassName)
                });

            ModelMBeanAttributeInfo [] mbaiArr = null;
            int k = 0;
            if (mmaiArr.size() > 0) {
                mbaiArr = new ModelMBeanAttributeInfo[mmaiArr.size()];
                for (ModelMBeanAttributeInfo mbai : mmaiArr) {
                    mbaiArr[k] = mbai;
                    k++;
                }
            }

            ModelMBeanOperationInfo [] mboiArr = null;
            k = 0;
            if (mmoiArr.size() > 0) {
                mboiArr = new ModelMBeanOperationInfo[mmoiArr.size()];
                for (ModelMBeanOperationInfo mboi : mmoiArr) {
                    mboiArr[k] = mboi;
                    k++;
                }
            }

            mmbinfo = new ModelMBeanInfoSupport(
                pojoClassName,
                null,
                mbaiArr,
                null,
                mboiArr,
                null
                );

            mmbinfo.setMBeanDescriptor(descriptor);

            msg.printNotice("mbeanInfo = " + mmbinfo);

        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }

        // serialize data
        try {
            OutputStream fos = env.getFiler().createBinaryFile( 
                Filer.Location.CLASS_TREE, packageName, new File(pojoName + ".ser"));
            ObjectOutputStream out = new ObjectOutputStream( fos );
            out.writeObject(mmbinfo);
            out.flush();
            out.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    private void processSuper(ClassDeclaration cdecl) {
        msg.printNotice("processSuper, cdecl = " + cdecl);
        if ((cdecl == null) || ("java.lang.Object".equals(cdecl.getQualifiedName()))) return;
        for (MethodDeclaration mdecl : cdecl.getMethods()) {
            generateMethods(mdecl);
        }
        processSuper(cdecl.getSuperclass().getDeclaration());
    }

    private void generateMethods(MethodDeclaration mdecl) {
        if ((mdecl.getAnnotation(ManagedOperation.class) == null) &&
            (mdecl.getAnnotation(ManagedAttribute.class) == null)) 
            return;

        try {
            if (! mdecl.getModifiers().contains(Modifier.PUBLIC)) return;

            generateOperation(mdecl);
            if (mdecl.getAnnotation(ManagedAttribute.class) != null) {
                generateAttribute(mdecl);
            }
        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }
    }


    private void generateOperation(MethodDeclaration mdecl) {
        try {
            String methodName = mdecl.getSimpleName();
            Descriptor descriptor = new DescriptorSupport(new String[] {
                ("name=" + methodName),
                "descriptorType=operation",
                ("class=" + pojoClassName),
                "role=operation"
                });
            String returnType = (mdecl.getReturnType()).toString();
            MBeanParameterInfo [] mbpiArr = null;

            int paramSize = mdecl.getParameters().size();
            if (paramSize > 0) {
                mbpiArr = new MBeanParameterInfo[paramSize];
                int k = 0;
                for (ParameterDeclaration pdecl : mdecl.getParameters()) {
                    mbpiArr [k] = new MBeanParameterInfo(
                        pdecl.getSimpleName(),
                        pdecl.getType().toString(),
                        null);
                    k++;
                }
            }

            mmoiArr.add(new ModelMBeanOperationInfo(
                methodName,
                null, 
                mbpiArr,
                returnType,
                MBeanOperationInfo.ACTION,
                descriptor));

        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }
    }


    private void generateAttribute(MethodDeclaration mdecl) {
        try {
            String attrName = null;
            String name = mdecl.getSimpleName();
            String getterName = null;
            String setterName = null;
            String isIsName = null;
            String returnType = null;

            if (name.startsWith("get")) {
                attrName = name.substring(3);
                getterName = name;
                returnType = (mdecl.getReturnType()).toString();
            } else if (name.startsWith("is")) {
                attrName = name.substring(2);
                isIsName = name;
                returnType = (mdecl.getReturnType()).toString();
            } else if (name.startsWith("set")) {
                attrName = name.substring(3);
                setterName = name;
                // FIXME: need to optimize this later
                for (ParameterDeclaration pdecl : mdecl.getParameters()) {
                    returnType = pdecl.getType().toString();
                }
            }

            if (attrMap == null) {
                setAttrMap(new ArrayList(4), attrName, getterName, setterName, isIsName, returnType);
            } else {
                ArrayList al = attrMap.get(attrName);
                if (al == null) {
                    setAttrMap(new ArrayList(4), attrName, getterName, setterName, isIsName, returnType);
                } else {
                    setAttrMap(al, attrName, getterName, setterName, isIsName, returnType);
                }
            }

        } catch (Exception ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void setAttrMap(ArrayList al, String name,
        String getterName, String setterName, String isIsName, String returnType) {

                if (al.size() == 0) {
                    al.add(0, null);
                    al.add(1, null);
                    al.add(2, null);
                    al.add(3, null);
                }

                // element-0 getter
                if (getterName != null) {
                    al.set(0, getterName);
                }
                // element-1 setter
                if (setterName != null) {
                    al.set(1, setterName);
                }
                // element-2 is
                if (isIsName != null) {
                    al.set(2, isIsName);
                }
                // element-3 type
                al.set(3, returnType);

                attrMap.put(name, al);
    }

    private void generateAttrInfo() {
        String key = null;
        ArrayList al = null;
        boolean isReadable = false;
        boolean isWritable = false;
        boolean isIs = false;
        try {
            Iterator attrIter = attrMap.keySet().iterator();
            while (attrIter.hasNext()) {
                key = (String) attrIter.next();
                al =  (ArrayList) attrMap.get(key);

                // get method or is method
                String gm = null;
                if (al.get(0) != null) {
                    gm = ("getMethod=" + (String)al.get(0));
                    isReadable = true;
                } else if (al.get(2) != null) {
                    gm = ("getMethod=" + (String)al.get(2));
                    isReadable = true;
                    isIs = true;
                }
                String sm = null;
                if (al.get(1) != null) {
                    sm = ("setMethod=" + (String) al.get(1));
                    isWritable = true;
                }
                // descriptor
                Descriptor descriptor = new DescriptorSupport(new String[] {
                    ("name=" + key),
                    "descriptorType=attribute",
                    gm,
                    sm
                });

                // attributeinfo
                mmaiArr.add(new ModelMBeanAttributeInfo(
                    key,
                    (String) al.get(3),
                    null,
                    isReadable,
                    isWritable,
                    isIs,
                    descriptor));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
