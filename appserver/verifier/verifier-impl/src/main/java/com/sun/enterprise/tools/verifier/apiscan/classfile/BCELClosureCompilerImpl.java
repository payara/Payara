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
 * BCELClosureCompilerImpl.java
 *
 * Created on August 13, 2004, 3:34 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * An implementation of {@link ClosureCompilerImplBase} based on
 * BCEL.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class BCELClosureCompilerImpl extends ClosureCompilerImplBase {

    /*
     * Earlier this class used to be called as ClosureCompilerImpl.
     * So to get the history of this file, use above name in CVS
     */

    private Stack<ClassFile> callStack = new Stack<ClassFile>();

    private HashSet<String> closure = new HashSet<String>();

    private HashSet<String> nativeMethods = new HashSet<String>();

    //map of refencing path to list of not found classes.
    private Map<String, List<String>> failed = new HashMap<String, List<String>>();

    private static final String myClassName = "BCELClosureCompilerImpl"; // NOI18N

    /**
     * @param loader the ClassFileLoader that is used to load the referenced
     *               classes.
     */
    public BCELClosureCompilerImpl(ClassFileLoader loader) {
        super(loader);
    }

    //See corresponding method of ClosureCompiler for javadocs
    public boolean buildClosure(String className) {
        logger.entering(myClassName, "buildClosure", className); // NOI18N
        ClassFile cf;
        if (!needToBuildClosure(className))
            return true;
        try {
            cf = loader.load(className);
        } catch (IOException e) {
            handleFailure(className);
            return false;
        }
        return buildClosure(cf);
    }

    /**
     * @param cf class file whose closure needs to be computed. The behavior is
     *           same as the other buildClosure() method.
     */
    private boolean buildClosure(ClassFile cf) {
        boolean result = true;
        callStack.push(cf);
        if (needToBuildClosure(cf.getName())) {
            visitedClasses.add(cf.getName());
            Collection<String> names = cf.getAllReferencedClassNames();
            closure.addAll(names);
            // TODO: We should not be doing this here. Its just a quick &
            // dirty solution.
            for(Method m : cf.getMethods()) {
                if(m.isNative()) {
                    final String methodDesc =
                            m.getOwningClass().getName()+ "." + m.getName(); // NOI18N
                    nativeMethods.add(methodDesc);
                }
            }
            for (Iterator i = names.iterator(); i.hasNext();) {
                String nextExternalName = (String) i.next();
                if (!needToBuildClosure(nextExternalName)) continue;
                ClassFile next;
                try {
                    next = loader.load(nextExternalName);
                } catch (IOException e) {
                    result = false;
                    handleFailure(nextExternalName);
                    continue;
                }
                boolean newresult = buildClosure(next);//recurssive call
                result = newresult && result;
            }
        }
        callStack.pop();
        return result;
    }

    private void handleFailure(String referencedClass) {
        String referencingPath = "";
        try {
            StringBuilder referencingPathBuffer = new StringBuilder();
            for (Iterator i = callStack.iterator(); i.hasNext();) {
                if (referencingPathBuffer.length() != 0)
                    referencingPathBuffer.append(File.separator);
                referencingPathBuffer.append(((ClassFile) i.next()).getName());
            }
            referencingPath = referencingPathBuffer.toString();
        } catch (EmptyStackException e) {
        }
        logger.finer(
                "Could not locate " + referencingPath + File.separator + // NOI18N
                referencedClass);
        List<String> failedList = failed.get(referencingPath);
        if (failedList == null) {
            failedList = new ArrayList<String>();
            failed.put(referencingPath, failedList);
        }
        failedList.add(referencedClass);
    }

    //See corresponding method of ClosureCompiler for javadocs
    public Collection getClosure() {
        return Collections.unmodifiableCollection(closure);
    }

    //See corresponding method of ClosureCompiler for javadocs
    public Map getFailed() {
        return Collections.unmodifiableMap(failed);
    }
    
    /**
     * Reset the closure for next closure computation.
     * Clear the internal cache. It includes the result it has collected since
     * last reset(). But it does not clear the excludedd list. If you want to
     * reset the excluded list, create a new ClosureCompiler.
     */
    public void reset() {
        closure.clear();
        visitedClasses.clear();
        failed.clear();
        nativeMethods.clear();
    }

    public Collection<String> getNativeMethods() {
        return Collections.unmodifiableCollection(nativeMethods);
    }

    public String toString() {
        StringBuilder sb=new StringBuilder();
        if(logger.isLoggable(Level.FINER)){
            sb.append("\n<Closure>"); // NOI18N

            sb.append("\n\t<ExcludedClasses>"); // NOI18N
            for(Iterator i=excludedClasses.iterator(); i.hasNext();) {
                sb.append("\n\t\t"); // NOI18N
                sb.append((String)i.next());
            }
            sb.append("\n\t</ExcludedClasses>"); // NOI18N

            sb.append("\n\t<ExcludedPackages>"); // NOI18N
            for(Iterator i=excludedPackages.iterator(); i.hasNext();){
                sb.append("\n\t\t"); // NOI18N
                sb.append((String)i.next());
            }
            sb.append("\n\t</ExcludedPackages>"); // NOI18N
            
            sb.append("\n\t<ExcludedPatterns>"); // NOI18N
            for(Iterator i=excludedPatterns.iterator(); i.hasNext();){
                sb.append("\n\t\t"); // NOI18N
                sb.append((String)i.next());
            }
            sb.append("\n\t</ExcludedPatterns>"); // NOI18N
            
            sb.append("\n\t<Classes>"); // NOI18N
            for(Iterator i=closure.iterator(); i.hasNext();){
                sb.append("\n\t\t"); // NOI18N
                sb.append((String)i.next());
            }
            sb.append("\n\t</Classes>"); // NOI18N
        }
        sb.append("\n\t<Failed>"); // NOI18N
        for(Iterator i=failed.entrySet().iterator(); i.hasNext();) {
            Map.Entry referencingPathToFailedList=(Map.Entry)i.next();
            sb.append("\n\t\t"); // NOI18N
            sb.append("<ReferencingPath>"); // NOI18N
            sb.append("\n\t\t\t"); // NOI18N
            sb.append(referencingPathToFailedList.getKey());
            sb.append("\n\t\t"); // NOI18N
            sb.append("</ReferencingPath>"); // NOI18N
            sb.append("\n\t\t"); // NOI18N
            sb.append("<Classes>"); // NOI18N
            for(Iterator iii=((List)referencingPathToFailedList.getValue()).iterator(); iii.hasNext();){
                sb.append("\n\t\t\t"); // NOI18N
                sb.append((String)iii.next());
            }
            sb.append("\n\t\t"); // NOI18N
            sb.append("</Classes>"); // NOI18N
        }
        sb.append("\n\t</Failed>"); // NOI18N

        sb.append("\n\t<NativeMethods>"); // NOI18N
        for(String s : nativeMethods) {
            sb.append("\n\t\t"); // NOI18N
            sb.append(s);
        }
        sb.append("\n\t</NativeMethods>"); // NOI18N

        if(logger.isLoggable(Level.FINER)){
            sb.append("\n</Closure>"); // NOI18N
        }
        return sb.toString();        
    }

}
