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

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.jvm.Pool;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Collections;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
// import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import static javax.tools.JavaFileObject.Kind.CLASS;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Used to generate a "symbol file" representing rt.jar that only
 * includes supported or legacy proprietary API.  Valid annotation
 * processor options:
 *
 * <dl>
 * <dt>com.sun.tools.javac.sym.Jar</dt>
 * <dd>Specifies the location of rt.jar.</dd>
 * <dt>com.sun.tools.javac.sym.Dest</dt>
 * <dd>Specifies the destination directory.</dd>
 * </dl>
 *
 * <p><b>This is NOT part of any API supported by Sun Microsystems.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 *
 */
@SupportedOptions({"com.sun.tools.javac.sym.Jar","com.sun.tools.javac.sym.Dest"})
@SupportedAnnotationTypes("*")
public class DIY extends AbstractProcessor {

    static Set<String> getLegacyPackages() {
        return Collections.emptySet();
    }

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
        if (!renv.processingOver())
            return true;

        Set<String> legacy = getLegacyPackages();
        Set<String> legacyProprietary = getLegacyPackages();
        Set<String> documented = new HashSet<String>();
        Set<PackageSymbol> packages =
            ((JavacProcessingEnvironment)processingEnv).getSpecifiedPackages();
        String jarName = processingEnv.getOptions().get("com.sun.tools.javac.sym.Jar");
        if (jarName == null)
            throw new RuntimeException("Must use -Acom.sun.tools.javac.sym.Jar=LOCATION_OF_JAR");
        String destName = processingEnv.getOptions().get("com.sun.tools.javac.sym.Dest");
        if (destName == null)
            throw new RuntimeException("Must use -Acom.sun.tools.javac.sym.Dest=LOCATION_OF_JAR");

        for (PackageSymbol psym : packages) {
            String name = psym.getQualifiedName().toString();
            legacyProprietary.remove(name);
            documented.add(name);
        }

	    ClassSymbol cs = null;
        Set<String> hiddenPackages = new HashSet<String>();
        Set<String> crisp = new HashSet<String>();
        try {
            javax.tools.JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null);
            Location jarLocation = StandardLocation.locationFor(jarName);
            File jarFile = new File(jarName);
            fm.setLocation(jarLocation, List.of(jarFile));
            fm.setLocation(StandardLocation.CLASS_PATH, List.<File>nil());
            fm.setLocation(StandardLocation.SOURCE_PATH, List.<File>nil());
            {
                ArrayList<File> bootClassPath = new ArrayList<File>();
                bootClassPath.add(jarFile);
    	    // ADD EXTRA DEPENDENCIES HERE:
    	    bootClassPath.add(new File("javax.mail.jar"));
    	    bootClassPath.add(new File("../../../target/javax.javaee.jar"));
                for (File path : fm.getLocation(StandardLocation.PLATFORM_CLASS_PATH)) {
                    // if (!new File(path.getName()).equals(new File("rt.jar")))
                        bootClassPath.add(path);
                }
                System.err.println("Using boot class path = " + bootClassPath);
                fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, bootClassPath);
                fm.setLocation(StandardLocation.CLASS_PATH, bootClassPath);
            }
            // System.out.println(fm.getLocation(StandardLocation.PLATFORM_CLASS_PATH));
            File destDir = new File(destName);
            if (!destDir.exists())
                if (!destDir.mkdirs())
                    throw new RuntimeException("Could not create " + destDir);
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(destDir));
            List<String> options = List.of("-XDdev");
             options = options.prepend("-doe");
             options = options.prepend("-verbose");
            JavacTaskImpl task = (JavacTaskImpl)
                tool.getTask(null, fm, null, options, null, null);
            JavaCompiler compiler = JavaCompiler.instance(task.getContext());
            ClassReader reader = ClassReader.instance(task.getContext());
            ClassWriter writer = ClassWriter.instance(task.getContext());
            Name.Table names = Name.Table.instance(task.getContext());
            Type.moreInfo = true;
            Pool pool = new Pool();
            for (JavaFileObject file : fm.list(jarLocation, "", EnumSet.of(CLASS), true)) {
                String className = fm.inferBinaryName(jarLocation, file);
                int index = className.lastIndexOf('.');
                String pckName = index == -1 ? "" : className.substring(0, index);
                if (documented.contains(pckName)) {
                    if (!legacy.contains(pckName))
                        crisp.add(pckName);
                    // System.out.println("Documented: " + className);
                } else if (legacyProprietary.contains(pckName)) {
                    // System.out.println("Legacy proprietary: " + className);
                } else {
                    // System.out.println("Hidden " + className);
                    hiddenPackages.add(pckName);
                    continue;
                }
                // PackageSymbol psym = reader.enterPackage(names.fromString(pckName));
                // psym.complete();
                TypeSymbol sym = (TypeSymbol)compiler.resolveIdent(className);
                if (sym.kind != Kinds.TYP) {
                    if (className.indexOf('$') < 0) {
                         System.err.println("Ignoring (other) " + className + " : " + sym);
                         System.err.println("   " + sym.getClass().getSimpleName() + " " + sym.type);
                    }
                    continue;
                }
                sym.complete();
                if (sym.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
                    System.err.println("Ignoring (bad) " + sym.getQualifiedName());
                }
        		if (false) {
        		    /*
        		     * Following eliminates non-public classes from output,
        		     * but is too aggressive because it also eliminates
        		     * non-public superclasses of public classes, which
        		     * makes the output unusable.
        		     */
        		    if (sym.owner.kind == Kinds.PCK &&
        			(sym.flags() & Flags.AccessFlags) == Flags.PUBLIC) {
        			cs = (ClassSymbol) sym;
        			writeClass(pool, cs, writer);
        			cs = null;
        		    }
        		} else {
        		    cs = (ClassSymbol) sym;
        		    writeClass(pool, cs, writer);
        		    cs = null;
        		}
            }
        } catch (IOException ex) {
	        reportError(ex, cs);
        } catch (CompletionFailure ex) {
	        reportError(ex, cs);
        } catch (RuntimeException ex) {
	        reportError(ex, cs);
	}
        if (false) {
            for (String pckName : crisp)
                System.out.println("Crisp: " + pckName);
            for (String pckName : hiddenPackages)
                System.out.println("Hidden: " + pckName);
            for (String pckName : legacyProprietary)
                System.out.println("Legacy proprietary: " + pckName);
            for (String pckName : documented)
                System.out.println("Documented: " + pckName);
        }

        return true;
    }

    void reportError(Throwable ex, Element element) {
	if (element != null)
	    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						     ex.getLocalizedMessage(),
						     element);
	else
	    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						     ex.getLocalizedMessage());
    }

    void writeClass(final Pool pool, final ClassSymbol cs, final ClassWriter writer) {
        try {
            pool.reset();
            cs.pool = pool;
            writer.writeClass(cs);
            for (Scope.Entry e = cs.members().elems; e != null; e = e.sibling) {
                if (e.sym.kind == Kinds.TYP) {
                    ClassSymbol nestedClass = (ClassSymbol)e.sym;
                    nestedClass.complete();
                    writeClass(pool, nestedClass, writer);
                }
            }
        } catch (ClassWriter.StringOverflow ex) {
            throw new RuntimeException(ex);
        } catch (ClassWriter.PoolOverflow ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

}
