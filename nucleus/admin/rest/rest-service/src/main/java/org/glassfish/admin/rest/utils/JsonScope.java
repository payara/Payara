/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.utils;

import java.util.Stack;

import org.glassfish.admin.rest.model.ResponseBody;

/**
 * @author tmoreau
 */
public class JsonScope {

    private String scope;
    private Stack<ScopeElement> scopeStack = new Stack<ScopeElement>();

    public JsonScope() {}

    public void beginObjectAttr(String name) {
        if (!scopeStack.isEmpty() && scopeStack.peek() instanceof ArrayAttr) {
            if (!((ArrayAttr)(scopeStack.peek())).inElement()) {
                throw new IllegalStateException("Not currently in an array element");
            }
        }
        scopeStack.push(new ObjectAttr(name));
        computeScope();
    }

    public void endObjectAttr() {
        if (!(scopeStack.peek() instanceof ObjectAttr)) {
            throw new IllegalStateException("Not currently in an object attribute");
        }
        scopeStack.pop();
        computeScope();
    }

    public void beginArrayAttr(String name) {
        if (!scopeStack.isEmpty() && scopeStack.peek() instanceof ArrayAttr) {
            if (!((ArrayAttr)(scopeStack.peek())).inElement()) {
                throw new IllegalStateException("Not currently in an array element");
            }
        }
        scopeStack.push(new ArrayAttr(name));
        computeScope();
    }

    public void endArrayAttr() {
        if (!(scopeStack.peek() instanceof ArrayAttr)) {
            throw new IllegalStateException("Not currently in an array attribute");
        }
        scopeStack.pop();
        computeScope();
    }

    public void beginArrayElement() {
        if (!(scopeStack.peek() instanceof ArrayAttr)) {
            throw new IllegalStateException("Not currently in an array attribute");
        }
        ((ArrayAttr)(scopeStack.peek())).beginElement();
        computeScope();
    }

    public void endArrayElement() {
        if (!(scopeStack.peek() instanceof ArrayAttr)) {
            throw new IllegalStateException("Not currently in an array attribute");
        }
        ((ArrayAttr)(scopeStack.peek())).endElement();
        computeScope();
    }

    private void computeScope() {
        if (scopeStack.isEmpty()) {
            this.scope = null;
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ScopeElement e : scopeStack) {
            if (!first) {
                sb.append(".");
            } else {
                first = false;
            }
            sb.append(e.toString());
        }
        this.scope = sb.toString();
    }

    @Override
    public String toString() {
        return this.scope;
    }

    private interface ScopeElement {}

    private static class ObjectAttr implements ScopeElement {
        private String name;
        private ObjectAttr(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return this.name;
        }
    }

    private static class ArrayAttr implements ScopeElement {
        private boolean inElement;
        private String name;
        int index;
        private ArrayAttr(String name) {
            this.name = name;
            this.inElement = false;
            this.index = -1;
        }
        private boolean inElement() {
            return this.inElement;
        }
        private void beginElement() {
            if (this.inElement) {
                throw new IllegalStateException("Already in an array element");
            }
            this.inElement = true;
            this.index++;
        }
        private void endElement() {
            if (!this.inElement) {
                throw new IllegalStateException("Not in an array element");
            }
            this.inElement = false;
        }
        @Override
        public String toString() {
            if (inElement) {
                return this.name + "[" + index + "]";
            } else {
                return this.name;
            }
        }
    }
}

