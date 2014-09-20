/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
 * RegisgterHandlers.java
 *
 * Created on March 24, 2008
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.glassfish.admingui.registration;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.*;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import com.sun.enterprise.registration.impl.RelayService;
import com.sun.enterprise.registration.glassfish.RegistrationUtil;
import org.glassfish.admingui.common.util.GuiUtil;

/**
 * @author Siraj Ghaffar
 * @author Anissa Lam
 */

public class RegisterHandlers {

    
    /** Creates a new instance of RegisterHandlers */
    public RegisterHandlers() {
    }

               
    @Handler(id="getSupportImages",
        input={
            @HandlerInput(name="count", type=Integer.class, defaultValue="5")
        },
     output={
        @HandlerOutput(name="imageList", type=List.class)
        })
    public static void getSupportImages(HandlerContext handlerCtx) {
        int maxImageCount = 25+1; //the # of images we have under images/square
        List result = new ArrayList();
        int cnt = ((Integer) handlerCtx.getInputValue("count")).intValue();
        Random random = new Random();
        for(int i=0; i < cnt; i++){
            for(;;){
                int num = Math.abs(random.nextInt() % maxImageCount);
                String imgName="square-"+num+".gif";
                if (! result.contains(imgName)){
                    result.add(imgName);
                    break;
                }
            }
        }
        handlerCtx.setOutputValue("imageList", result);       
    }

    @Handler(id="getIssueQueryString",
     output={
        @HandlerOutput(name="query", type=String.class)
        })
    public static void getIssueQueryString(HandlerContext handlerCtx)
    {
        Calendar current = new GregorianCalendar();
        current.add(Calendar.HOUR,  -168);
        int month = Integer.parseInt(""+current.get(Calendar.MONTH)) + 1;
        String startTime=""+current.get(Calendar.YEAR)+"-"+month+"-"+current.get(Calendar.DAY_OF_MONTH);
        String query = "https://glassfish.dev.java.net/issues/buglist.cgi?component=glassfish&issue_status=RESOLVED&chfield=issue_status&chfieldto=Now&cmdtype=doit&chfieldfrom="+startTime;
        handlerCtx.setOutputValue("query", query);       

    }

    /* generate the default registration html page, if necessary */
    
    private static File getDefaultRegistrationPage() {
        Logger logger = GuiUtil.getLogger();
        File f = new File(RegistrationUtil.getRegistrationHome(), "registration.html");
        if (f.exists())
            return f;
        try {
            String regPage = f.getAbsolutePath();
            if (!f.exists()) {
                RelayService rs = new RelayService(RegistrationUtil.getServiceTagRegistry());
                rs.generateRegistrationPage(regPage);
            }
        } catch (Exception ex) {
            logger.fine(ex.getMessage());
        }
        return f;
    }


    @Handler(id="gf.isRegistrationEnabled",
     output={
        @HandlerOutput(name="isEnabled", type=Boolean.class)
        })
    public static void isRegistrationEnabled(HandlerContext handlerCtx) {
        File dir = RegistrationUtil.getRegistrationHome();
        File regFile = new File(dir, "registration.html");
        if (dir.exists() && (dir.canWrite() || regFile.exists()))
            handlerCtx.setOutputValue("isEnabled", Boolean.TRUE);
        else
            handlerCtx.setOutputValue("isEnabled", Boolean.FALSE);
    }


    /* get the contents of the registration page. Generate the page if needed */
    @Handler(id="gf.getRegistrationPage",
     output={
        @HandlerOutput(name="registrationPage", type=String.class)
        })
    public static void getRegistrationLandingPage(HandlerContext handlerCtx) {
        File f = getDefaultRegistrationPage();
        if (!f.exists())
            return;
        BufferedReader br = null;
        try {
            FileReader fr = new FileReader(f);
            br = new BufferedReader(fr);
            String s;
            StringBuilder pageContent = new StringBuilder("");

            while((s = br.readLine()) != null) {
                pageContent.append(s);
            }
            handlerCtx.setOutputValue("registrationPage", pageContent.toString());
        } catch (Exception ex) {
            Logger logger = GuiUtil.getLogger();
            logger.fine(ex.getMessage());
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                }
            }
        }
    }
}
