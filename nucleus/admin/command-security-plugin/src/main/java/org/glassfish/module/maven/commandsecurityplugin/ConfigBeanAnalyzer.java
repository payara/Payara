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
package org.glassfish.module.maven.commandsecurityplugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Analyzes a ConfigBean class to see if it has CRUD annotations and, if so,
 * collects the command authorization information about it.
 * 
 * @author tjquinn
 */
public class ConfigBeanAnalyzer  {
    
    private final InputStream classStream;
    private StringBuilder trace = null;
    private CommandAuthorizationInfo commandAuthInfo = null;
    private boolean isCommand = false;
    private final TypeProcessor typeProcessor;
    
    ConfigBeanAnalyzer(final InputStream classStream, final TypeProcessor typeProcessor) {
        this.typeProcessor = typeProcessor;
        this.classStream = classStream;
    }
    
    void setTrace(final StringBuilder trace) {
        this.trace = trace;
    }
    
    void run() throws IOException {
        final ClassReader classReader = new ClassReader(classStream);
        final ConfigBeanScanner cs = new ConfigBeanScanner();
        classReader.accept(cs, ClassReader.SKIP_CODE + ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES);
        isCommand = cs.isCommand();
//        if (cs.isCommand()) {
//            commandAuthInfo = cs.commandInfo();
//        }
    }
    
    CommandAuthorizationInfo commandAuthInfo() {
        return commandAuthInfo;
    }
    
    boolean isCommand() {
        return isCommand;
    }

    /**
     * Scans the config bean class looking for CRUD annotations on the class
     * or on methods, creating a Command
     */
    private class ConfigBeanScanner extends ClassVisitor {
        
        private boolean isCommand = false;
        private Collection<CommandAuthorizationInfo> commandInfos = Collections.EMPTY_LIST;
        
        private ConfigBeanScanner() {
            super(Opcodes.ASM4);
        }
        
        private boolean isCommand() {
            return isCommand;
        }
        
        private Collection<CommandAuthorizationInfo> commandInfos() {
            return commandInfos;
        }
        
        @Override
        public void visit(int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
            
        }
    }
    
    
    
    
}
