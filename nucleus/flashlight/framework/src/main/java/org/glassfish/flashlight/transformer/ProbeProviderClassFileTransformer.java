/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.flashlight.transformer;

import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.Utility;
import org.glassfish.flashlight.provider.FlashlightProbe;
import org.glassfish.flashlight.provider.ProbeRegistry;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.logging.*;
import org.glassfish.flashlight.impl.client.AgentAttacher;

/**
 * @author Mahesh Kannan
 * @author Byron Nevins
 */
public class ProbeProviderClassFileTransformer implements ClassFileTransformer {
    public ProbeProviderClassFileTransformer(Class providerClass) {
        this.providerClass = providerClass;
    }

    /**
     * This code can get confusing.  I didn't want to get this method confused with
     * ProbeRegistry.registerProbe() so I gave it a slightly different name...
     * @param probe
     */
    public void regProbe(FlashlightProbe probe) throws NoSuchMethodException {
        Method m = getMethod(probe);
        probes.put(probe.getProviderJavaMethodName() + "::" + Type.getMethodDescriptor(m), probe);
    }

    public void transform() {
        try {
            if (instrumentation != null) {
                instrumentation.addTransformer(this, true);
                instrumentation.retransformClasses(providerClass);
            }
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Error during re-transformation", e);
        }

        // note -- do NOT remove the Transformer.  If we transform it again we will need ALL transformers
    }

    // this method is called from the JDK itself!!!
    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer)
            throws IllegalClassFormatException {

        try {
            if (!AgentAttacher.canAttach()) {
                // return the buffer
            }
            else if (classBeingRedefined == providerClass) {

                cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);
                ClassReader cr = new ClassReader(classfileBuffer);
                cr.accept(new ProbeProviderClassVisitor(cw), null, 0);

                classfileBuffer = cw.toByteArray();
                if (_debug) {
                    ProbeProviderClassFileTransformer.writeFile(className.substring(className.lastIndexOf('/') + 1), classfileBuffer);
                }
            }
        }
        catch (Exception ex) {
            logger.log(Level.WARNING, "Error during registration of FlashlightProbe", ex);

        }
        return classfileBuffer;
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
            logger.log(Level.INFO, "Couldn't write the retransformed class data", th);
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
            for (String methodDesc : probes.keySet()) {
                logger.log(Level.FINE, "ProbeProviderClassVisitor will visit" + methodDesc);
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

    private class ProbeProviderMethodVisitor
            extends MethodAdapter {
        private FlashlightProbe probe;
        private int access;
        private String name;
        private String desc;

        ProbeProviderMethodVisitor(MethodVisitor mv, int access, String name, String desc, FlashlightProbe probe) {
            super(mv);
            this.probe = probe;

            this.access = access;
            this.name = name;
            this.desc = desc;
        }

        public void visitCode() {
            super.visitCode();

            GeneratorAdapter gen = new GeneratorAdapter(mv, access, name, desc);
            //Add the body
            gen.push(probe.getId());
            gen.loadArgArray();
            gen.invokeStatic(Type.getType(
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
    private static final Instrumentation instrumentation;
    private static boolean _debug = Boolean.parseBoolean(Utility.getEnvOrProp("AS_DEBUG"));
    private final Class providerClass;
    private Map<String, FlashlightProbe> probes = new HashMap<String, FlashlightProbe>();
    private ClassWriter cw;
    private static final Logger logger = Logger.getLogger(ProbeProviderClassFileTransformer.class.getName());
    private static boolean emittedAttachUnavailableMessageAlready = false;
    private static final String AGENT_CLASSNAME = "org.glassfish.flashlight.agent.ProbeAgentMain";

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
            logger.warning("Monitoring is disabled because there is no Attach API from the JVM available.");
        else if (instrumentation != null)
            logger.log(Level.INFO, "Successfully got INSTRUMENTATION: " + instrumentation);
        else if (throwable != null)
            logger.log(Level.WARNING, "Error while getting Instrumentation object from ProbeAgentmain", throwable);
        else
            logger.log(Level.WARNING, "Error while getting Instrumentation object from ProbeAgentmain");
    }
}
