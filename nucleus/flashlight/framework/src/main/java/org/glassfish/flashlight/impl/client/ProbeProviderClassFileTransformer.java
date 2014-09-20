/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.flashlight.impl.client;

import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.Utility;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.glassfish.flashlight.FlashlightLoggerInfo;
import static org.glassfish.flashlight.FlashlightLoggerInfo.*;
import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * July 2012 Byron Nevins says: We no longer allow outsiders to create
 * instances. Summary of the problem solved: All of the
 * transformation/untransformation is done to the entire class (ProbeProvider)
 * all at once. I.e. EVERY probe (method) is done all at once. BUT -- the
 * callers are calling once for every Probe which was a huge waste of time.
 *
 * @author Byron Nevins
 */
public class ProbeProviderClassFileTransformer implements ClassFileTransformer {

    ///////////////  instance variables  //////////////////
    // Don't hold a strong ref to the class, it will prevent entries in the weak map from being reclaimed
    private final WeakReference<Class> providerClassRef;
    private String providerClassName = null;
    private Map<String, FlashlightProbe> probes = new HashMap<String, FlashlightProbe>();
    private ClassWriter cw;
    private volatile boolean enabled = false;
    private boolean allProbesTransformed = true;
    private boolean transformerAdded = false;
    private int count = 0;  // Only used for debug so we can look at the before/after class dumps for each iteration
    ///////////////  static variables  //////////////////
    // weak map here so the classes don't prevent app classloaders from being reclaimed
    private static Map<Class, ProbeProviderClassFileTransformer> instances =
        new WeakHashMap<Class, ProbeProviderClassFileTransformer>();
    private static final Instrumentation instrumentation;
    private static boolean _debug = Boolean.parseBoolean(Utility.getEnvOrProp("AS_DEBUG"));
    private static boolean emittedAttachUnavailableMessageAlready = false;
    private static final String AGENT_CLASSNAME = "org.glassfish.flashlight.agent.ProbeAgentMain";
    private static final Logger logger = FlashlightLoggerInfo.getLogger();

    private ProbeProviderClassFileTransformer(Class providerClass) {
        providerClassRef = new WeakReference<Class>(providerClass);
        providerClassName = providerClass.getName(); // For debug purposes only in case the original class has been reclaimed
    }

    static ProbeProviderClassFileTransformer getInstance(Class aProbeProvider) {
        synchronized(instances) {
            if (!instances.containsKey(aProbeProvider)) {
                ProbeProviderClassFileTransformer tx = new ProbeProviderClassFileTransformer(aProbeProvider);
                instances.put(aProbeProvider, tx);
                return tx;
            } else {
                return instances.get(aProbeProvider);
            }
        }
    }

    static void transformAll() {
        synchronized(instances) {
            Set<Map.Entry<Class, ProbeProviderClassFileTransformer>> entries = instances.entrySet();

            for (Map.Entry<Class, ProbeProviderClassFileTransformer> entry : entries) {
                entry.getValue().transform();
            }
        }
    }

    static void untransformAll() {
        synchronized(instances) {
            Set<Map.Entry<Class, ProbeProviderClassFileTransformer>> entries = instances.entrySet();

            for (Map.Entry<Class, ProbeProviderClassFileTransformer> entry : entries) {
                entry.getValue().untransform();
            }
        }
    }

    static void untransform(Class aProviderClazz) {
        getInstance(aProviderClazz).untransform();
    }

    static void transform(Class aProviderClazz) {
        getInstance(aProviderClazz).transform();
    }

    synchronized void addProbe(FlashlightProbe probe) throws NoSuchMethodException {
        Method m = getMethod(probe);
        probes.put(probe.getProviderJavaMethodName() + "::" + Type.getMethodDescriptor(m), probe);
        
        // probes can be added piecemeal after the initial transformation is done, flagging when probes are added to detect that
        allProbesTransformed = false;
    }

    final synchronized void transform() {

        Class providerClass = providerClassRef.get();
        if (providerClass == null) {
            if (Log.getLogger().isLoggable(Level.FINER))
                Log.finer("provider class was reclaimed, not.transformed", providerClassName);
            return; // Nothing to do!            
        }
            
        if (enabled)  {
            if (allProbesTransformed) {
                if (Log.getLogger().isLoggable(Level.FINER))
                    Log.finer("all probes already.transformed", providerClass);
                return; // Nothing to do!
            }
            if (Log.getLogger().isLoggable(Level.FINER))
                Log.finer("some probes need to be.transformed", providerClass);
        }
        allProbesTransformed = true;
        
        //important!  The transform(...) callback method in this class uses this boolean!
        enabled = true;

        if (instrumentation == null)
            return;

        try {
            addTransformer();
            instrumentation.retransformClasses(providerClass);
        }
        catch (Exception e) {
            logger.log(Level.WARNING, RETRANSFORMATION_ERROR, e);
        }
    }

    final synchronized void untransform() {
        Class providerClass = providerClassRef.get();
        if (providerClass == null) {
            if (Log.getLogger().isLoggable(Level.FINER))
                Log.finer("provider class was reclaimed, not.untransformed", providerClassName);
            return; // Nothing to do!            
        }
        
        // Reset the flag to indicate the probes are not transformed
        allProbesTransformed = false;
        
        if (!enabled) {
            if (Log.getLogger().isLoggable(Level.FINER))
                Log.finer("already.not.transformed", providerClass);
            return; // Nothing to do!
        }

        //important!  The transform(...) callback method in this class uses this boolean!
        enabled = false;

        if (instrumentation == null)
            return;

        try {
            instrumentation.retransformClasses(providerClass);
        }
        catch (UnmodifiableClassException e) {
            logger.log(Level.WARNING, RETRANSFORMATION_ERROR, e);
        }
    }

    // this method is called from the JDK itself!!!
    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer)
            throws IllegalClassFormatException {

        byte[] ret = null;

        Class providerClass = providerClassRef.get();
        if (providerClass == null) {
            if (Log.getLogger().isLoggable(Level.FINER))
                Log.finer("provider class was reclaimed, not.transformed", providerClassName);
            return null; // Nothing to do!            
        }

        try {
            if (!AgentAttacher.canAttach()) {
                return null;
            }
            if (classBeingRedefined != providerClass) {
                return null;
            }

            if (enabled) {
                // we still need to write out the class file in debug mode if it is
                // disabled.
                cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
                ClassReader cr = new ClassReader(classfileBuffer);
                cr.accept(new ProbeProviderClassVisitor(cw), null, 0);
                ret = cw.toByteArray();
                Log.fine("transformed", providerClassName);
                if (_debug) {
                  ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1)+"supplied_"+count, classfileBuffer);
                  ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1)+"transformed_"+count, ret);
                  count++;
                }
            }
            else {
                if (_debug) {
                  ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1)+"supplied_"+count, classfileBuffer);
                  count++;
                }
                ret = null;
                Log.fine("untransformed", providerClass.getName());
            }
        }
        catch (Exception ex) {
            logger.log(Level.WARNING, REGISTRATION_ERROR, ex);
        }

        return ret;
    }

    private synchronized void addTransformer() {
        if (!transformerAdded) {
          instrumentation.addTransformer(this, true);
          transformerAdded = true;
        }
    }

    private static String makeKey(String name, String desc) {
        return name + "::" + desc;
    }

    private static final void writeFile(String name, byte[] data) {
        FileOutputStream fos = null;
        try {
            File installRoot = new File(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
            File dir = new File(installRoot, "flashlight-generated");

            if (!dir.isDirectory() && !dir.mkdirs())
                throw new RuntimeException("Can't create directory: " + dir);
            fos = new FileOutputStream(new File(dir, name + ".class"));
            fos.write(data);
        }
        catch (Throwable th) {
            logger.log(Level.WARNING, WRITE_ERROR, th);
        }
        finally {
            try {
                if (fos != null)
                    fos.close();
            }
            catch (Exception ex) {
                // nothing can be done...
            }
        }
    }

    private class ProbeProviderClassVisitor
            extends ClassAdapter {

        ProbeProviderClassVisitor(ClassVisitor cv) {
            super(cv);
            if (Log.getLogger().isLoggable(Level.FINER)) {
                for (String methodDesc : probes.keySet()) {
                    Log.finer("visit" + methodDesc);
                }
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            FlashlightProbe probe = probes.get(makeKey(name, desc));
            if (probe != null) {
                mv = new ProbeProviderMethodVisitor(mv, access, name, desc, probe);
            }

            return mv;
        }
    }

    private static class ProbeProviderMethodVisitor
            extends AdviceAdapter {

        private FlashlightProbe probe;
        private int stateLocal;
        private Label startFinally;

        ProbeProviderMethodVisitor(MethodVisitor mv, int access, String name, String desc, FlashlightProbe probe) {
            super(mv, access, name, desc);
            this.probe = probe;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // We only setup the end of the try/finally for stateful probes only
            if (probe.getStateful()) {
                Label endFinally = new Label();
                mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
                mv.visitLabel(endFinally);
                onFinally(ATHROW);
                mv.visitInsn(ATHROW);
            }
            mv.visitMaxs(maxStack, maxLocals);
        }
        
        @Override
        protected void onMethodEnter() {
            if (!probe.getStateful()) {
                // Stateless probe, generate the same way as before-only advice
                insertCode();
                return;
            }
            
            // Handle stateful probes:
            //     localState = ProbeRegistry.invokeProbeBefore(probeId, args);
            
            // Declare a local to hold the state and initialize it to null
            // Note that if we decide to make the state local variable available to the debugger in the local variable table,
            // we can do a visitLocalVariable here as well.
            stateLocal = newLocal(Type.getType(Object.class));
            visitInsn(ACONST_NULL);
            storeLocal(stateLocal);

            // stateful probe, create a local and start the try/finally block
            startFinally = new Label();
            visitLabel(startFinally);

            // invoke stateful begin
            push(probe.getId());
            loadArgArray();
            invokeStatic(Type.getType(
                    ProbeRegistry.class),
                    org.objectweb.asm.commons.Method.getMethod(
                    "Object invokeProbeBefore(int, Object[])"));

            // Store return to local
            storeLocal(stateLocal);
        }
        
        @Override
        protected void onMethodExit(int opcode) {
            // For normal return path handling, we call onFinally here.
            // The exception throw path is handled when we setup the try/finally
            if (opcode != ATHROW) {
                onFinally(opcode);
            }
        }
    
        private void onFinally(int opcode)  {
            // If this is a stateful probe, we don't add anything on exit
            if (!probe.getStateful())
                return;
           
            // For the exception handling path:
            //      ProbeRegistry.invokeProbeOnException(exceptionValue, probeid, localState);
            if (opcode == ATHROW) {
                // Push either a duplicate of the exception or a null
                if (probe.getStatefulException()) {
                    dup();                    
                } else {
                    visitInsn(ACONST_NULL);
                }

                // Push the probe id
                push(probe.getId());

                // Push the state from the local
                loadLocal(stateLocal);
                invokeStatic(Type.getType(ProbeRegistry.class),
                        org.objectweb.asm.commons.Method.getMethod(
                        "void invokeProbeOnException(Object, int, Object)"));

            } else {

                // For the normal return paths:
                //      ProbeRegistry.invokeProbeAfter(returnValue, probeid, localState);

                // Push the return value or null
                if (probe.getStatefulReturn()) {
                    if (opcode == RETURN) {
                        visitInsn(ACONST_NULL);
                    } else if (opcode == ARETURN) {
                        dup();
                    } else {
                        if(opcode == LRETURN || opcode == DRETURN) {
                            dup2();
                        } else {
                            dup();
                        }
                        box(Type.getReturnType(this.methodDesc));
                    }
                } else {
                    visitInsn(ACONST_NULL);    
                }

                // Push the probe id
                push(probe.getId());

                // Push the state from the local
                loadLocal(stateLocal);
                invokeStatic(Type.getType(ProbeRegistry.class),
                        org.objectweb.asm.commons.Method.getMethod(
                        "void invokeProbeAfter(Object, int, Object)"));
            }
        }
        
        // This handles the stateless probe invocations
        private void insertCode() {
            //Add the body
            push(probe.getId());
            loadArgArray();
            invokeStatic(Type.getType(
                    ProbeRegistry.class),
                    org.objectweb.asm.commons.Method.getMethod("void invokeProbe(int, Object[])"));
        }

    }

    private Method getMethod(FlashlightProbe probe) throws NoSuchMethodException {
        Method m = probe.getProbeMethod();

        if (m == null) {
            m = probe.getProviderClazz().getDeclaredMethod(
                    probe.getProviderJavaMethodName(), probe.getParamTypes());
            probe.setProbeMethod(m);
        }

        return m;
    }
   
    static {
        Instrumentation nonFinalInstrumentation = null;
        Throwable throwable = null;
        Class agentMainClass = null;
        boolean canAttach = false;

        // if tools.jar is not available (e.g. we are running in JRE --
        // then there is no point doing anything else!

        if (AgentAttacher.canAttach()) {
            canAttach = true;

            try {
                ClassLoader classLoader = ProbeProviderClassFileTransformer.class.getClassLoader().getSystemClassLoader();

                try {
                    agentMainClass = classLoader.loadClass(AGENT_CLASSNAME);
                }
                catch (Throwable t) {
                    // need throwable, not Exception - it may throw an Error!
                    // try one more time after attempting to attach.
                    AgentAttacher.attachAgent();
                    // might throw
                    agentMainClass = classLoader.loadClass(AGENT_CLASSNAME);
                }

                Method mthd = agentMainClass.getMethod("getInstrumentation", null);
                nonFinalInstrumentation = (Instrumentation) mthd.invoke(null, null);
            }
            catch (Throwable t) {
                nonFinalInstrumentation = null;
                // save it for nice neat message code below
                throwable = t;
            }
        }
        // set the final
        instrumentation = nonFinalInstrumentation;

        if (!canAttach)
            logger.log(Level.WARNING, NO_ATTACH_API);
        else if (instrumentation != null)
            Log.info("yes.attach.api", instrumentation);
        else
            logger.log(Level.WARNING, NO_ATTACH_GET, throwable);
    }
}
