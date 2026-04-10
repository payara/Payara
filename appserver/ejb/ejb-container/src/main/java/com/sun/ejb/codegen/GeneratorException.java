/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package com.sun.ejb.codegen;

/**
 * The Generator exception is thrown whenever there is an error in
 * generating the stubs and skeletons and other related files.
 */
public class GeneratorException extends Exception {

    private static final long serialVersionUID = -6932740662092591668L;


    /**
     * Constructs the Generator exception with the specified string.
     *
     * @param message string description
     */
    public GeneratorException(String message) {
        super(message);
    }


    /**
     * Constructs the Generator exception with the specified string.
     *
     * @param message string description
     * @param cause exception which caused the failure
     */
    public GeneratorException(String message, Exception cause) {
        super(message, cause);
    }


    /**
     * Return the string representation of the exception.
     *
     * @return the string representation of the exception.
     */
    @Override
    public String toString() {
        return getMessage();
    }
}
