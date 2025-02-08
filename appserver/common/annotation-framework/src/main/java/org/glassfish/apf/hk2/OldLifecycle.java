/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.apf.hk2;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.v3.server.CommonClassLoaderServiceImpl;
import com.sun.enterprise.v3.server.ReadableArchiveScannerAdapter;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import jakarta.inject.Inject;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.ParsingContext;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.hk2.classmodel.reflect.util.CommonModelRegistry;
import org.glassfish.hk2.classmodel.reflect.util.ParsingConfig;
import org.glassfish.hk2.classmodel.reflect.util.ResourceLocator;
import org.glassfish.internal.deployment.DeploymentTracing;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.analysis.DeploymentSpan;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;
import org.jboss.jandex.Index;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

class OldLifecycle {
    @Inject
    CommonClassLoaderServiceImpl commonClassLoaderService;

    @Inject
    PayaraExecutorService executorService;

    @Inject
    JandexIndexer jandexIndexer;

    @Inject
    ArchiveFactory archiveFactory;

    private Types getDeplpymentTypes(DeploymentContext context) throws IOException{
        synchronized (context) {
            Types types = context.getTransientAppMetaData(Types.class.getName(), Types.class);
            if (types != null) {
                return types;
            }
            StructuredDeploymentTracing tracing = StructuredDeploymentTracing.load(context);
            Boolean skipScanExternalLibProp = Boolean.valueOf(context.getAppProps().getProperty(DeploymentProperties.SKIP_SCAN_EXTERNAL_LIB));

            if (skipScanExternalLibProp) {
                Index index = null; // jandexIndexer.getIndexFromArchive(context.getSource());
                if (index != null) {
                    context.addTransientAppMetaData(Index.class.getName(), index);
                    return null;
                }
            }
            Parser parser = getDeployableParser(context.getSource(), skipScanExternalLibProp, false, tracing,
                    context.getLogger(), context);
            ParsingContext parsingContext = parser.getContext();
            context.addTransientAppMetaData(Types.class.getName(), parsingContext.getTypes());
            context.addTransientAppMetaData(Parser.class.getName(), parser);
            return parsingContext.getTypes();
        }
    }


    private Parser getDeployableParser(ReadableArchive source, boolean skipScanExternalLibProp,
                                       boolean modelUnAnnotatedMembers, StructuredDeploymentTracing tracing,
                                       Logger logger, DeploymentContext deploymentContext) throws IOException {
        Parser parser = new Parser(createBuilder(modelUnAnnotatedMembers, logger).build());
        try(ReadableArchiveScannerAdapter scannerAdapter = new ReadableArchiveScannerAdapter(parser, source)) {
            DeploymentSpan mainScanSpan = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, source.getName());
            return processParsing(skipScanExternalLibProp, tracing, parser, scannerAdapter, mainScanSpan, deploymentContext);
        }
    }

    private Parser getDeployableParser(ReadableArchive source, boolean skipScanExternalLibProp,
                                       boolean modelUnAnnotatedMembers, StructuredDeploymentTracing tracing, Logger logger)
            throws java.io.IOException {
        Parser parser = new Parser(createBuilder(modelUnAnnotatedMembers, logger).build());
        ReadableArchiveScannerAdapter scannerAdapter = new ReadableArchiveScannerAdapter(parser, source);
        DeploymentSpan mainScanSpan = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, source.getName());
        return processParsing(source, skipScanExternalLibProp, tracing, parser, scannerAdapter, mainScanSpan);
    }

    private Parser processParsing(ReadableArchive source, boolean skipScanExternalLibProp,
                                  StructuredDeploymentTracing tracing, Parser parser,
                                  ReadableArchiveScannerAdapter scannerAdapter, DeploymentSpan mainScanSpan)
            throws IOException {
        try {
            parser.parse(scannerAdapter, () -> mainScanSpan.close());
            for (ReadableArchive externalLibArchive : getExternalLibraries(source, skipScanExternalLibProp)) {
                ReadableArchiveScannerAdapter libAdapter = null;
                try {
                    DeploymentSpan span = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, externalLibArchive.getName());
                    libAdapter = new ReadableArchiveScannerAdapter(parser, externalLibArchive);
                    parser.parse(libAdapter, () -> span.close());
                } finally {
                    if (libAdapter != null) {
                        libAdapter.close();
                    }
                }
            }
            parser.awaitTermination();
            scannerAdapter.close();
            return parser;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private Parser processParsing(boolean skipScanExternalLibProp,
                                  StructuredDeploymentTracing tracing, Parser parser,
                                  ReadableArchiveScannerAdapter scannerAdapter, DeploymentSpan mainScanSpan,
                                  DeploymentContext deploymentContext)
            throws IOException {
        try {
            parser.parse(scannerAdapter, () -> mainScanSpan.close());
            List<ReadableArchive> externalLibraries = getExternalLibraries(skipScanExternalLibProp, deploymentContext);
            for (ReadableArchive externalLibArchive : externalLibraries) {
                DeploymentSpan span = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, externalLibArchive.getName());
                try (ReadableArchiveScannerAdapter libAdapter = new ReadableArchiveScannerAdapter(parser, externalLibArchive)) {
                    parser.parse(libAdapter, () -> span.close());
                }
            }
            parser.awaitTermination();
            for(ReadableArchive externalLibArchive: externalLibraries) {
                externalLibArchive.close();
            }
            return parser;
        } catch (InterruptedException | java.net.URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private ParsingContext.Builder createBuilder(boolean modelUnAnnotatedMembers, Logger logger) {
        ResourceLocator locator = determineLocator();
        // scan the jar and store the result in the deployment context.
        ParsingContext.Builder parsingContextBuilder = new ParsingContext.Builder()
                .logger(logger)
                .executorService(executorService.getUnderlyingExecutorService())
                .config(new ParsingConfig() {
                    @Override
                    public Set<String> getAnnotationsOfInterest() {
                        return Collections.emptySet();
                    }

                    @Override
                    public Set<String> getTypesOfInterest() {
                        return Collections.emptySet();
                    }

                    @Override
                    public boolean modelUnAnnotatedMembers() {
                        return modelUnAnnotatedMembers;
                    }
                });
        // workaround bug in Builder
        parsingContextBuilder.locator(locator);
        return parsingContextBuilder;
    }

    private ResourceLocator determineLocator() {
        if (CommonModelRegistry.getInstance().canLoadResources()) {
            // common model registry will handle our external class dependencies
            return null;
        }
        return new ClassloaderResourceLocatorAdapter(commonClassLoaderService.getCommonClassLoader());
    }
    private List<ReadableArchive> getExternalLibraries(ReadableArchive source, Boolean skipScanExternalLibProp) throws IOException {
        List<ReadableArchive> externalLibArchives = new ArrayList<>();

        if (skipScanExternalLibProp) {
            // if we skip scanning external libraries, we should just
            // return an empty list here
            return Collections.emptyList();
        }

        List<URI> externalLibs = DeploymentUtils.getExternalLibraries(source);
        for (URI externalLib : externalLibs) {
            externalLibArchives.add(archiveFactory.openArchive(new File(externalLib.getPath())));
        }

        return externalLibArchives;
    }

    private List<ReadableArchive> getExternalLibraries(Boolean skipScanExternalLibProp,
                                                       DeploymentContext deploymentContext)
            throws IOException, URISyntaxException {
        List<ReadableArchive> externalLibArchives = new ArrayList<>();

        if (skipScanExternalLibProp) {
            // if we skip scanning external libraries, we should just
            // return an empty list here
            return Collections.emptyList();
        }

        for(URI externalLib : DeploymentUtils.getExternalLibraries(deploymentContext.getSource())) {
            externalLibArchives.add(archiveFactory.openArchive(new File(externalLib.getPath())));
        }

        for (URI externalLib : deploymentContext.getAppLibs()) {
            externalLibArchives.add(archiveFactory.openArchive(new File(externalLib.getPath())));
        }

        return externalLibArchives;
    }
}
