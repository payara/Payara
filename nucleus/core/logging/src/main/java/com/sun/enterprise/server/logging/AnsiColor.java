/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package com.sun.enterprise.server.logging;

/**
 *
 * @author steve
 */
public enum AnsiColor {
    
    BLACK("\u001B[0;30m"),
    RED("\u001B[0;31m"),
    YELLOW("\u001B[0;33m"),
    BLUE("\u001B[0;34m"),
    PURPLE("\u001B[0;35m"),
    CYAN("\u001B[0;36m"),
    WHITE("\u001B[0;37m"),
    GREEN("\u001B[0;32m"),
    INTENSE_BLACK("\u001B[0;90m"),
    INTENSE_RED("\u001B[0;91m"),
    INTENSE_YELLOW("\u001B[0;93m"),
    INTENSE_BLUE("\u001B[0;94m"),
    INTENSE_PURPLE("\u001B[0;95m"),
    INTENSE_CYAN("\u001B[0;96m"),
    INTENSE_WHITE("\u001B[0;97m"),
    INTENSE_GREEN("\u001B[0;92m"),
    BOLD_INTENSE_BLACK("\u001B[1;90m"),
    BOLD_INTENSE_RED("\u001B[1;91m"),
    BOLD_INTENSE_YELLOW("\u001B[1;93m"),
    BOLD_INTENSE_BLUE("\u001B[1;94m"),
    BOLD_INTENSE_PURPLE("\u001B[1;95m"),
    BOLD_INTENSE_CYAN("\u001B[1;96m"),
    BOLD_INTENSE_WHITE("\u001B[1;97m"),
    BOLD_INTENSE_GREEN("\u001B[1;92m"), 
    RESET("\u001b[0m"), 
    NOTHING("");
    
    AnsiColor(String color) {
        colorString = color;
    }
    
    public String toString() {
        return colorString;
    }
    
    private final String colorString;
    
}
