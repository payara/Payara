/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package jaxb1.impl.runtime;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Formats error messages.
 * 
 * @since 1.0
 */
public class Messages
{
    public static String format( String property ) {
        return format( property, null );
    }
    
    public static String format( String property, Object arg1 ) {
        return format( property, new Object[]{arg1} );
    }
    
    public static String format( String property, Object arg1, Object arg2 ) {
        return format( property, new Object[]{arg1,arg2} );
    }
    
    public static String format( String property, Object arg1, Object arg2, Object arg3 ) {
        return format( property, new Object[]{arg1,arg2,arg3} );
    }
    
    // add more if necessary.
    
    /** Loads a string resource and formats it with specified arguments. */
    public static String format( String property, Object[] args ) {
        String text = ResourceBundle.getBundle(Messages.class.getName()).getString(property);
        return MessageFormat.format(text,args);
    }
    
//
//
// Message resources
//
//
    public static final String CI_NOT_NULL= // 0 args
        "DefaultJAXBContextImpl.CINotNull";
       
    public static final String CI_CI_NOT_NULL = // 0 args
        "DefaultJAXBContextImpl.CICINotNull";
        
    public static final String NO_BGM = // 1 arg
        "GrammarInfo.NoBGM";
        
    public static final String UNABLE_TO_READ_BGM = // 0 args
        "GrammarInfo.UnableToReadBGM";

    public static final String COLLISION_DETECTED = // 2 args
        "GrammarInfoFacade.CollisionDetected";

    public static final String INCOMPATIBLE_VERSION = // 3 args
        "GrammarInfoFacade.IncompatibleVersion";

    public static final String MISSING_INTERFACE = // 1 arg
        "ImplementationRegistry.MissingInterface";

    public static final String BUILD_ID = // 0 args
        "DefaultJAXBContextImpl.buildID";
    
    public static final String INCORRECT_VERSION =
        "ContextFactory.IncorrectVersion";

    public static final String ERR_TYPE_MISMATCH = // arg:3 since JAXB 1.0.3
        "Util.TypeMismatch";
    
/*        
    static final String = // arg
        "";
    static final String = // arg
        "";
    static final String = // arg
        "";
    static final String = // arg
        "";
  */      
}
