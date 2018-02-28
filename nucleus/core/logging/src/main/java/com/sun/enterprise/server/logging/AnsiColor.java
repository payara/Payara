/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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

import org.fusesource.jansi.Ansi;

/**
 * @since 4.1.1.173
 * @author steve
 */
public enum AnsiColor {
    
    BLACK(Ansi.ansi().fgBlack()),
    RED(Ansi.ansi().fgRed()),
    YELLOW(Ansi.ansi().fgYellow()),
    BLUE(Ansi.ansi().fgBlue()),
    PURPLE(Ansi.ansi().fgMagenta()),
    CYAN(Ansi.ansi().fgCyan()),
    WHITE(Ansi.ansi().fg(Ansi.Color.WHITE)),
    GREEN(Ansi.ansi().fgGreen()),
    INTENSE_BLACK(Ansi.ansi().fgBrightBlack()),
    INTENSE_RED(Ansi.ansi().fgBrightRed()),
    INTENSE_YELLOW(Ansi.ansi().fgBrightYellow()),
    INTENSE_BLUE(Ansi.ansi().fgBrightBlue()),
    INTENSE_PURPLE(Ansi.ansi().fgBrightMagenta()),
    INTENSE_CYAN(Ansi.ansi().fgBrightCyan()),
    INTENSE_WHITE(Ansi.ansi().fgBright(Ansi.Color.WHITE)),
    INTENSE_GREEN(Ansi.ansi().fgBrightGreen()),
    BOLD_INTENSE_BLACK(Ansi.ansi().bold().fgBrightBlack()),
    BOLD_INTENSE_RED(Ansi.ansi().bold().fgBrightRed()),
    BOLD_INTENSE_YELLOW(Ansi.ansi().bold().fgBrightYellow()),
    BOLD_INTENSE_BLUE(Ansi.ansi().bold().fgBrightBlue()),
    BOLD_INTENSE_PURPLE(Ansi.ansi().bold().fgBrightMagenta()),
    BOLD_INTENSE_CYAN(Ansi.ansi().bold().fgBrightCyan()),
    BOLD_INTENSE_WHITE(Ansi.ansi().bold().fgBright(Ansi.Color.WHITE)),
    BOLD_INTENSE_GREEN(Ansi.ansi().bold().fgBrightGreen()), 
    RESET(Ansi.ansi().reset()),
    NOTHING(Ansi.ansi());
    
    AnsiColor(Ansi colour) {
        colourAnsi = colour;
    }
    
    @Override
    public String toString() {
        return colourAnsi.toString();
    }
    
    public Ansi getANsi(){
        return colourAnsi;
    }
    
    private final Ansi colourAnsi;
    
}
