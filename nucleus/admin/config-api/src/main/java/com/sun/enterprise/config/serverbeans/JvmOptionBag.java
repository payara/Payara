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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.PropertyBag; //used only for Javadoc purpose {@link}
import java.util.List;
import java.beans.PropertyVetoException;

/** Factored out the list of jvm-options from at least two other interfaces that have them: java-config and profiler.
 *  Similar to {@link PropertyBag}
 */
public interface JvmOptionBag extends ConfigBeanProxy {
    @Element
    public List<String> getJvmOptions();

    void setJvmOptions(List<String> options) throws PropertyVetoException;

    /** It's observed that many a time we need the value of max heap size. This is useful when deciding if a
     *  user is misconfiguring the JVM by specifying -Xmx that is smaller than -Xms. Sun's JVM for example,
     *  bails out with <code> Incompatible minimum and maximum heap sizes specified</code> when that happens. 
     *  It's generally better to do some basic validations in those cases and that's when this method may be useful.
     * @return an integer specifying the actual max heap memory (-Xmx) configured. If it's specified as -Xmx2g, then 2*1024
     * i.e. 2048 is returned. Returns -1 if no -Xmx is specified.
     */
    @DuckTyped
    int getXmxMegs();

    /** see #getXmxMegs
     * @return integer specifying -Xms in megabytes, or -1*/
    @DuckTyped
    int getXmsMegs();

    @DuckTyped
    boolean contains(String option);

    @DuckTyped
    String getStartingWith(String start);

    class Duck {
     /* Note: It does not take defaults into account. Also,
     * I have tested that server does not start with a heap that is less than 1m, so I think I don't have to worry about
     * -Xmx that is specified to be less than 1 MB. Again, there is lots and lots of platform dependent code here,
     *  so this check should be minimal. Again, I am doing this kind of check here because while testing, I was
     *  able to get into a situation where -Xmx is configured smaller than -Xms and the server won't start. The user
     *  then must edit the domain.xml by hand!
     */
         public static int getXmxMegs(JvmOptionBag me) {
             return getMemory(me, "-Xmx");
         }
         public static int getXmsMegs(JvmOptionBag me) {
           return getMemory(me, "-Xms");
         }

        private static int getMemory(JvmOptionBag me, String which) {
            List<String> options = me.getJvmOptions();
            for (String opt : options) {
                if(opt.indexOf(which) >= 0) {
                    return toMeg(opt, which);
                }
            }
            return -1;
        }

        public static int toMeg(String whole, String which) {
            String first  = whole.substring(0, which.length());
            String second = whole.substring(which.length());
            if (first == null || second == null)
                return -1;
            char unit = second.charAt(second.length()-1);
            try {
                if (unit =='g' || unit == 'G')
                    return Integer.parseInt(second.substring(0, second.length()-1)) * 1024; //I don't think we'll have an overflow
                else if (unit == 'm' || unit == 'M')
                    return Integer.parseInt(second.substring(0, second.length()-1));
                else if (unit == 'k' || unit == 'K')
                    return Integer.parseInt(second.substring(0, second.length()-1)) / 1024; //beware, integer division
                else
                    return Integer.parseInt(second) / (1024*1024); //bytes, this is a rare case, hopefully -- who does -Xmx1073741824 to specify a meg?
            } catch(NumberFormatException e) {
                //squelch all exceptions
                return -1;
            } catch(RuntimeException e) {
                //squelch all exceptions
                return -1;
            }
        }

        public static boolean contains(JvmOptionBag me, String opt) {
            return me.getJvmOptions().contains(opt);
        }

        public static String getStartingWith(JvmOptionBag me, String start) {
            List<String> opts = me.getJvmOptions();
            for (String opt : opts) {
                if (opt.startsWith(start))
                    return opt;
            }
            return null;
        }
    }
}
