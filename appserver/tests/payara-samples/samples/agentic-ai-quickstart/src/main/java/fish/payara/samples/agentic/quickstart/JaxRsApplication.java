/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 */
package fish.payara.samples.agentic.quickstart;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application configuration.
 * Maps the REST API to /api path.
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {
}
