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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
 */
//Portions Copyright [2016-2022] [Payara Foundation and/or affiliates]
package org.glassfish.flashlight.impl.client;

import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.Utility;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private final WeakReference<Class<?>> providerClassRef;
    private final String providerClassName;
    private final Map<String, FlashlightProbe> probes = new ConcurrentHashMap<>();
    private AtomicBoolean flaggedForUpdate = new AtomicBoolean(false);
    private boolean transformerAdded = false;
    private int count = 0;  // Only used for debug so we can look at the before/after class dumps for each iteration
    ///////////////  static variables  //////////////////
    // uses String as key so no reference to Class is held => allow being reclaimed
    private static Map<String, ProbeProviderClassFileTransformer> instances = new ConcurrentHashMap<>();
    private static final Instrumentation instrumentation;
    private static boolean _debug = Boolean.parseBoolean(Utility.getEnvOrProp("AS_DEBUG"));
    private static final String AGENT_CLASSNAME = "org.glassfish.flashlight.agent.ProbeAgentMain";
    private static final Logger logger = FlashlightLoggerInfo.getLogger();
    private static final AtomicReference<Thread> updater = new AtomicReference<>();

    private ProbeProviderClassFileTransformer(Class<?> providerClass) {
        providerClassRef = new WeakReference<>(providerClass);
        providerClassName = providerClass.getName(); // For debug purposes only in case the original class has been reclaimed
    }

    static ProbeProviderClassFileTransformer getInstance(Class<?> aProbeProvider) {
        return instances.computeIfAbsent(aProbeProvider.getName(), key -> new ProbeProviderClassFileTransformer(aProbeProvider));
    }

    static void update(Class<?> aProviderClazz) {
        getInstance(aProviderClazz).update();
    }

    void addProbe(FlashlightProbe probe) throws NoSuchMethodException {
        probes.put(probe.getProviderJavaMethodName() + "::" + Type.getMethodDescriptor(getMethod(probe)), probe);
        update();
    }

    private static Thread newUpdater() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(200); // wait first so triggering calls flag first before the update runs
                    Iterator<ProbeProviderClassFileTransformer> iter = instances.values().iterator();
                    while (iter.hasNext()) {
                        ProbeProviderClassFileTransformer transformer = iter.next();
                        if (transformer.providerClassRef.get() == null) {
                            iter.remove();
                        } else if (transformer.flaggedForUpdate.get()) {
                            transformer.runUpdate();
                        }
                    }
                }
            } catch (InterruptedException ex) {
                // end this updater, a new one will be spawned in case of probe is added
            }
        });
        t.setName("ProbeProviderClassFileTransformer");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private void update() {
        flaggedForUpdate.set(true);
        // make sure a updater is running
        updater.updateAndGet(thread -> thread != null && thread.isAlive() ? thread : newUpdater());
    }

    private final void runUpdate() {
        flaggedForUpdate.set(false);
        Class<?> providerClass = providerClassRef.get();
        if (providerClass == null) {
            if (Log.getLogger().isLoggable(Level.FINER))
                Log.finer("provider class was reclaimed, not.transformed", providerClassName);
            return; // Nothing to do!
        }
        if (Log.getLogger().isLoggable(Level.FINER))
            Log.finer("some probes need to be.transformed", providerClass);

        if (instrumentation == null)
            return;
        try {
            addTransformer();
            instrumentation.retransformClasses(providerClass);
        } catch (Exception e) {
            logger.log(Level.WARNING, RETRANSFORMATION_ERROR, e);
        }
    }

    private boolean hasEnabledInvokers() {
        for (FlashlightProbe probe : probes.values()) {
            if (probe.isEnabled() && probe.getInvokerCount() > 0) {
                return true;
            }
        }
        return false;
    }

    // this method is called from the JDK itself!!!
    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer)
            throws IllegalClassFormatException {

        Class<?> providerClass = providerClassRef.get();
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

            if (hasEnabledInvokers()) {
                // we still need to write out the class file in debug mode if it is
                // disabled.
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
                ClassReader cr = new ClassReader(classfileBuffer);
                cr.accept(new ProbeProviderClassVisitor(cw), null, 0);
                byte[] instrumentedClassBytes = cw.toByteArray();
                Log.fine("transformed", providerClassName);
                if (_debug) {
                  ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1)+"supplied_"+count, classfileBuffer);
                  ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1)+"transformed_"+count, instrumentedClassBytes);
                  count++;
                }
                return instrumentedClassBytes;
            }
            if (_debug) {
              ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1)+"supplied_"+count, classfileBuffer);
              count++;
            }
            Log.fine("untransformed", providerClass.getName());
            return null;
        }
        catch (Exception ex) {
            logger.log(Level.WARNING, REGISTRATION_ERROR, ex);
            return null;
        }
    }

    private void addTransformer() {
        if (!transformerAdded) {
          instrumentation.addTransformer(this, true);
          transformerAdded = true;
        }
    }

    private static String makeKey(String name, String desc) {
        return name + "::" + desc;
    }

    private static final void writeFile(String name, byte[] data) {
        File installRoot = new File(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        File dir = new File(installRoot, "flashlight-generated");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            logger.log(Level.WARNING, WRITE_ERROR, new RuntimeException("Can't create directory: " + dir));
            return;
        }
        try (
            FileOutputStream fos = new FileOutputStream(new File(dir, name + ".class"))) {
            fos.write(data);
        } catch (Throwable th) {
            logger.log(Level.WARNING, WRITE_ERROR, th);
        }
    }

    private class ProbeProviderClassVisitor
            extends ClassVisitor {

        ProbeProviderClassVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
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
            if (probe != null && probe.isEnabled() && probe.getInvokerCount() > 0) {
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
            super(Opcodes.ASM9, mv, access, name, desc);
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
        Class<?> agentMainClass = null;
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
