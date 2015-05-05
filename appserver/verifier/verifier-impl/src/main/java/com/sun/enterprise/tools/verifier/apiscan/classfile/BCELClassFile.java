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

/*
 * Main.java
 *
 * Created on August 10, 2004, 9:21 AM
 */

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.ConstantClass;
import com.sun.org.apache.bcel.internal.classfile.DescendingVisitor;
import com.sun.org.apache.bcel.internal.classfile.EmptyVisitor;
import com.sun.org.apache.bcel.internal.classfile.Field;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;

/**
 * This is an implementation of {@link ClassFile} interface. It uses Apache's
 * BCEL library in its implementation.
 * This is a thread safe implementation of ClassFile interface.
 * This is NOT a public class. Access thru' {@link ClassFile} interface.
 * Use {@link ClassFileLoaderFactory} to create new instances of this class.
 *
 * @author Sanjeeb.Sahoo@Sun.COM 
 */
class BCELClassFile implements ClassFile {

    private JavaClass jc;
    private Set<String> classNames;
    private HashSet<BCELMethod> methods = new HashSet<BCELMethod>();
    private static Logger logger = Logger.getLogger("apiscan.classfile"); // NOI18N
    //This is constant used during logging
    private static final String myClassName = "apiscan.classfile.BCELClassFile"; // NOI18N

    /**
     * @param is        is could be a Zip or Jar InputStream or a regular
     *                  InputStream.
     * @param file_name is the name of the .class file. If is a Zip or Jar Input
     *                  stream, then this name should just be the entry path,
     *                  because it is used internally in the is.getEntry(file_name)
     *                  to locate the .class file. If is is a regular stream,
     *                  then file_name should not be really needed, I don't have
     *                  a choice as BCEL ClassFile does not have a constructor
     *                  that just takes an InputStream. Any way, if is is a
     *                  regular stream, then it should either be the internal
     *                  name of the class or the path to the actual .class file
     *                  that the input stream represents. This constructor does
     *                  not check if the ClassFile created here indeed
     *                  represents the class that is being requested. That check
     *                  should be done by ClassFileLoader.
     */
    public BCELClassFile(InputStream is, String file_name) throws IOException {
        logger.entering(myClassName, "<init>(InputStream, String)", file_name); // NOI18N
        jc = new ClassParser(is, file_name).parse();
    }
    
    //In contrast to the other constructor, here class_path is the path to the
    // .class file.
    /**
     * @param file_path Absolute path to the .class file.
     */
    public BCELClassFile(String file_path) throws IOException {
        logger.entering(myClassName, "<init>(String)", file_path); // NOI18N
        jc = new ClassParser(file_path).parse();
    }

    /* Now the ClassFile interface implementation methods */
    
    //See ClassFile interface for description.
    public synchronized Collection getAllReferencedClassNamesInInternalForm() {
        if (classNames == null) {
            classNames = new HashSet<String>();//lazy instantiation
            logger.logp(Level.FINER, myClassName, "getAllReferencedClassNames", // NOI18N
                    "Starting to visit"); // NOI18N
            jc.accept(new DescendingVisitor(jc, new Visitor(this)));
            logger.logp(Level.FINER, myClassName, "getAllReferencedClassNames", // NOI18N
                    "Finished visting"); // NOI18N
            classNames = Collections.unmodifiableSet(classNames);
        }
        return classNames;
    }

    public synchronized Collection<String> getAllReferencedClassNames() {
        if (classNames == null) {
            getAllReferencedClassNamesInInternalForm();
        }
        HashSet<String> extClassNames = new HashSet<String>(classNames.size());
        for (Iterator i = classNames.iterator(); i.hasNext();) {
            extClassNames.add(Util.convertToExternalClassName((String) i.next()));
        }
        return extClassNames;
    }

    //See ClassFile interface for description.
    //see getInternalName() as well
    //IMPORTANT: Does not deal with the case where Goo is an inner class in
    // Foo$Bar class.
    //It should return Foo$Bar.Goo, but it returns Foo$Bar$Goo
    //Irrespective of this, it can be safely used from getInternalName()
    public String getName() {
        return jc.getClassName();
    }

    //See ClassFile interface for description.
    public String getInternalName() {
        return Util.convertToInternalClassName(getName());
    }

    //See ClassFile interface for description.
    public String getPackageName() {
        //not necessary as we always use external name for package. .replace('.','/');
        return jc.getPackageName();
    }

    public Collection<? extends com.sun.enterprise.tools.verifier.apiscan.classfile.Method>
            getMethods() {
        return Collections.unmodifiableSet(methods);
    }

    public com.sun.enterprise.tools.verifier.apiscan.classfile.Method
            getMethod(MethodRef methodRef) {
        throw new UnsupportedOperationException();
    }

    public String getNameOfSuperClass() {
        return jc.getSuperclassName();
    }

    public String getInternalNameOfSuperClass() {
        return Util.convertToInternalClassName(getNameOfSuperClass());
    }

    public String[] getNamesOfInterfaces() {
        return jc.getInterfaceNames();
    }

    public String[] getInternalNamesOfInterfaces() {
        String[] result = getNamesOfInterfaces();
        for(int i = 0; i< result.length; ++i) {
            result[i] = Util.convertToInternalClassName(result[i]);
        }
        return result;
    }

    public boolean isInterface() {
        return !jc.isClass();
    }

    public String toString() {
        return
                "External Name: " + getName() + "\n" + // NOI18N
                "Internal Name: " + getInternalName() + "\n" + // NOI18N
                jc.toString()
                + "\n------------CONSTANT POOL BEGIN--------------\n" // NOI18N
                + jc.getConstantPool()
                + "\n------------CONSTANT POOL END--------------"; // NOI18N
    }

    //returns a list of all the classnames embedded in the given signature string.
    //This method knows about basic data types, so for them classname is not returned.
    //e.g. given below are the signature string and class name that would be
    // returned by this call...
    // Ljava/lang/Integer;    {java/lang/Integer}
    // [Ljava/lang/Integer;   {java/lang/Integer}
    // [[I                    {}
    // I                      {I}
    // (F[La/b/P;La/b/Q;[[La/b/P;)I    {a/b/P, a/b/Q, a/b/P}
    // a method like "int foo(float f, a/b/P[] ps, a/b/Q q, a/b/P[][] pss)" will
    // have above signature.
    private static List<String> signatureToClassNames(String signature) {
        logger.entering(myClassName, "signatureToClassNames", signature); // NOI18N
        List<String> result = new ArrayList<String>();
        int i = 0;
        while ((i = signature.indexOf('L', i)) != -1) {
            int j = signature.indexOf(';', i);
            if (j > i) {
                // get name, minus leading 'L' and trailing ';'
                String className = signature.substring(i + 1, j);
                if (!Util.isPrimitive(className)) result.add(className);
                i = j + 1;
            } else
                break;
        }
        if (logger.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("Class Names are {"); // NOI18N
            int size = result.size();
            for (int k = 0; k < size; k++) {
                sb.append((String) result.get(k));
                if (k != size - 1) sb.append(", "); // NOI18N
            }
            sb.append("}"); // NOI18N
            logger.finer(sb.toString());
        }
        return result;
    }

    //an inner class
    private class Visitor extends EmptyVisitor {
        BCELClassFile cf;
        public Visitor(BCELClassFile cf) {
            this.cf = cf;
        }

        /* Now override the visitor methods of our interest from EmptyVisitor class. */
        /** 
         */
        public void visitConstantClass(ConstantClass obj) {
            logger.entering(myClassName, "visitConstantClass", obj); // NOI18N
            String className = obj.getBytes(jc.getConstantPool());
            logger.finer("Class name is " + className); // NOI18N
            //sometimes we get names like Ljava.lang.Integer; or [I. So we need
            // to decode the names.
            //A good test case is java/io/ObjectInputStream.class
            if (className.indexOf(';') != -1 || className.indexOf('[') != -1) {
                classNames.addAll(signatureToClassNames(className));
            } else {
                classNames.add(className);
            }
        }

        public void visitField(Field field) {
            logger.entering(myClassName, "visitField", field); // NOI18N
            String signature = field.getSignature();
            logger.finer("Signature is " + signature); // NOI18N
            //for BCEL 5.1, use field.getType().getSignature() if the above does
            //not work
            classNames.addAll(signatureToClassNames(signature));
        }

        public synchronized void visitMethod(Method method) {
            logger.entering(myClassName, "visitMethod", method); // NOI18N
            String signature = method.getSignature();
            logger.finer("Signature is " + signature); // NOI18N
            methods.add(new BCELMethod(cf, method));
            classNames.addAll(signatureToClassNames(signature));
        }
    }//class Visitor
}
