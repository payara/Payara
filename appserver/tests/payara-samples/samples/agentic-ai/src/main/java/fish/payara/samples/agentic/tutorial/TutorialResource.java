/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.agentic.tutorial;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST API for the tutorial UI. Firing the {@link TutorialRequest} event is
 * synchronous, so the agent workflow (including the LLM call) completes before
 * the method returns and the freshly produced HTML can be read from the
 * {@link TutorialStore}.
 */
@Path("")
@RequestScoped
public class TutorialResource {

    @Inject
    Event<TutorialRequest> trigger;

    @Inject
    TutorialStore store;

    @Inject
    CustomerFormSpec form;

    /** The form metadata; the page renders the live form from this. */
    @GET
    @Path("form")
    @Produces(MediaType.APPLICATION_JSON)
    public FormSpec form() {
        return form.spec();
    }

    /** The current tutorial HTML (empty until first generated). */
    @GET
    @Path("tutorial")
    @Produces(MediaType.TEXT_HTML)
    public String current() {
        return store.get();
    }

    /** Generate a fresh tutorial from the form. */
    @POST
    @Path("tutorial/generate")
    @Produces(MediaType.TEXT_HTML)
    public String generate() {
        trigger.fire(new TutorialRequest(form.spec(), null, null));
        return store.get();
    }

    /** Refine the current tutorial with a chat instruction. */
    @POST
    @Path("tutorial/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    public String refine(RefineRequest request) {
        String instruction = request == null ? null : request.instruction();
        trigger.fire(new TutorialRequest(form.spec(), instruction, store.get()));
        return store.get();
    }

    public record RefineRequest(String instruction) {
    }
}
