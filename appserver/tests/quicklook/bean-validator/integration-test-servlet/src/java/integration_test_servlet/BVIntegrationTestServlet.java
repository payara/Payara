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

package integration_test_servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;

public class BVIntegrationTestServlet extends HttpServlet {

    public static String validationXml =
"<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n" +
"<constraint-mappings xmlns=\"http://jboss.org/xml/ns/javax/validation/mapping\"\n" +
"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"  xsi:schemaLocation=\"http://jboss.org/xml/ns/javax/validation/mapping validation-mapping-1.0.xsd\">\n" +
"      <default-package>integration_test_servlet</default-package>\n" +
"      <bean class=\"Person\" ignore-annotations=\"true\">\n" +
"          <field name=\"firstName\" ignore-annotations=\"true\">\n" +
"              <constraint annotation=\"javax.validation.constraints.NotNull\" />\n" +
"          </field>\n" +
"          <field name=\"lastName\" ignore-annotations=\"true\">\n" +
"              <constraint annotation=\"javax.validation.constraints.NotNull\" />\n" +
"          </field>\n" +
"          <getter name=\"listOfString\"  ignore-annotations=\"true\">\n" +
"              <constraint annotation=\"javax.validation.constraints.NotNull\" />\n" +
"          </getter>\n" +
"      </bean>\n" +
"</constraint-mappings>\n";


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");
        out.print("<html><head><title>SimpleBVServlet</title></head><body>");
        
        javax.validation.Validator beanValidator = configureValidation(req, resp);

        out.print("<h1>");
        out.print("Validating person class using validateValue with valid property");
        out.print("</h1>");
	
        List<String> listOfString = new ArrayList<String>();
        listOfString.add("one");
        listOfString.add("two");
        listOfString.add("three");

        Set<ConstraintViolation<Person>> violations =
                beanValidator.validateValue(Person.class, "listOfString", listOfString);
        
        printConstraintViolations(out, violations, "case1");

        out.print("<h1>");
        out.print("Validating person class using validateValue with invalid property");
        out.print("</h1>");

        try {
            violations =
                    beanValidator.validateValue(Person.class, "nonExistentProperty", listOfString);
        } catch (IllegalArgumentException iae) {
            out.print("<p>");
            out.print("case2: caught IllegalArgumentException.  Message: " +
                    iae.getMessage());
            out.print("</p>");
        }
        Person person = new Person();
        
        out.print("<h1>");
        out.print("Validating invalid person instance using validate.");
        out.print("</h1>");
        
        violations = beanValidator.validate(person);
        
        printConstraintViolations(out, violations, "case3");
        
        out.print("<h1>");
        out.print("Validating valid person.");
        out.print("</h1>");
        
        person.setFirstName("John");
        person.setLastName("Yaya");
        person.setListOfString(listOfString);
        
        violations = beanValidator.validate(person);
        printConstraintViolations(out, violations, "case4");
        
        out.print("</body></html>");
        
    }

    private void printConstraintViolations(PrintWriter out,
            Set<ConstraintViolation<Person>> violations, String caseId) {
        if (violations.isEmpty()) {
            out.print("<p>");
            out.print(caseId + ": No ConstraintViolations found.");
            out.print("</p>");
        } else {
            for (ConstraintViolation<Person> curViolation : violations) {
                out.print("<p>");
                out.print(caseId + ": ConstraintViolation: message: " + curViolation.getMessage() +
                        " propertyPath: " + curViolation.getPropertyPath());
                out.print("</p>");
            }
        }

    }


    private Validator configureValidation(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();

        GenericBootstrap bootstrap = Validation.byDefaultProvider();
        Configuration config = bootstrap.configure();
        InputStream mappingsXml = new ByteArrayInputStream(validationXml.getBytes());
        config.addMapping(mappingsXml);
        ValidatorFactory factory = config.buildValidatorFactory();
        ValidatorContext validatorContext = factory.usingContext();
        Validator validator = validatorContext.getValidator();

        if (null == validator) {
            factory = Validation.byDefaultProvider().configure().buildValidatorFactory();
            validator = factory.getValidator();
        }

        out.print("<p>");
        out.print("Obtained ValidatorFactory: " + factory + ".");
        out.print("</p>");



        return validator;
    }
}
