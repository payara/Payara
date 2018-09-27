/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
package org.jvnet.hk2.config.generator.maven;

import java.io.File;
import java.util.*;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.jvnet.hk2.config.generator.ConfigInjectorGenerator;

/**
 * @author jwells
 * 
 * Abstract Mojo for config generator
 */
public abstract class AbstractConfigGeneratorMojo extends AbstractMojo {
    protected final static String GENERATED_SOURCES = "generated-sources/hk2-config-generator/src";
    protected final static String MAIN_NAME = "main";
    protected final static String TEST_NAME = "test";
    protected final static String JAVA_NAME = "java";

    /**
     * The maven project.
     *
     * @parameter expression="${project}" @required @readonly
     */
    protected MavenProject project;
    
    /**
     * @parameter
     */
    private boolean verbose;
    
    /**
     * @parameter expression="${supportedProjectTypes}" default-value="jar"
     */
    private String supportedProjectTypes;
    
    /**
     * @parameter expression="${includes}" default-value="**\/*.java"
     */
    private String includes;
    
    /**
     * @parameter expression="${excludes}" default-value=""
     */
    private String excludes;
    
    protected abstract File getSourceDirectory();
    protected abstract File getGeneratedDirectory();
    protected abstract File getOutputDirectory();
    protected abstract void addCompileSourceRoot(String path);
    
    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    private void internalExecute() throws Throwable {
        List<String> projectTypes = Arrays.asList(supportedProjectTypes.split(","));
        if(!projectTypes.contains(project.getPackaging())
                || !getSourceDirectory().exists() 
                || !getSourceDirectory().isDirectory()){
            return;
        }
        getLog().info(getGeneratedDirectory().getAbsolutePath());
        if (!getGeneratedDirectory().exists()) {
            if (!getGeneratedDirectory().mkdirs()) {
                getLog().info("Could not create output directory " +
                        getOutputDirectory().getAbsolutePath());
                return;
            }
        }
        if (!getGeneratedDirectory().exists()) {
            getLog().info("Exiting hk2-config-generator because could not find generated directory " +
                  getGeneratedDirectory().getAbsolutePath());
            return;
        }
        String outputPath = getGeneratedDirectory().getAbsolutePath();

        // prepare command line arguments
        List<String> options = new ArrayList<String>();
        options.add("-proc:only");
        options.add("-s");
        options.add(outputPath);
        options.add("-d");
        options.add(outputPath);
        options.add("-cp");
        options.add(getBuildClasspath());
        List<String> classNames = new ArrayList<String>();
        classNames.addAll(FileUtils.getFileNames(getSourceDirectory(), includes, excludes,true));
        
        if(classNames.isEmpty()){
            getLog().info("No source file");
            return;
        }
        
        if(verbose){
            getLog().info("");
            getLog().info("-- AnnotationProcessing Command Line --");
            getLog().info("");
            getLog().info(options.toString());
            getLog().info(classNames.toString());
            getLog().info("");
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(classNames);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
        task.setProcessors(Collections.singleton(new ConfigInjectorGenerator()));

        boolean compilationResult = task.call();
        if(verbose) {
            getLog().info("Result: " + (compilationResult ? "OK" : "!!! failed !!!"));
        }

        // make the generated source directory visible for compilation
        addCompileSourceRoot(outputPath);
        if (getLog().isInfoEnabled()) {
            getLog().info("Source directory: " + outputPath + " added.");
        }
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
          internalExecute();
        }
        catch (Throwable th) {
            if (th instanceof MojoExecutionException) {
                throw (MojoExecutionException) th;
            }
            if (th instanceof MojoFailureException) {
                throw (MojoFailureException) th;
            }
            
            Throwable cause = th;
            int lcv = 0;
            while (cause != null) {
                getLog().error("Exception from hk2-config-generator[" + lcv++ + "]=" + cause.getMessage());
                cause.printStackTrace();
                
                cause = cause.getCause();
            }
            
            throw new MojoExecutionException(th.getMessage(), th);
        }
    }
    
    private String getBuildClasspath() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(project.getBuild().getOutputDirectory());
        sb.append(File.pathSeparator);
        
        if (!getOutputDirectory().getAbsolutePath().equals(
                project.getBuild().getOutputDirectory())) {
            
            sb.append(getOutputDirectory().getAbsolutePath());
            sb.append(File.pathSeparator);
        }
        
        List<Artifact> artList = new ArrayList<Artifact>(project.getArtifacts());
        Iterator<Artifact> i = artList.iterator();
        
        if (i.hasNext()) {
            sb.append(i.next().getFile().getPath());

            while (i.hasNext()) {
                sb.append(File.pathSeparator);
                sb.append(i.next().getFile().getPath());
            }
        }
        
        String classpath = sb.toString();
        if(verbose){
            getLog().info("");
            getLog().info("-- Classpath --");
            getLog().info("");
            getLog().info(classpath);
            getLog().info("");
        }
        return classpath;
    }
}
