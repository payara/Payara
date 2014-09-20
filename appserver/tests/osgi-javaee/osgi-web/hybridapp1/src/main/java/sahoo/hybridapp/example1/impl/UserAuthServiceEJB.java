/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package sahoo.hybridapp.example1.impl;

import sahoo.hybridapp.example1.UserAuthService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

@Stateless
public class UserAuthServiceEJB implements UserAuthService
{

    @Resource(mappedName= Activator.dsName)
    private DataSource ds;

    @PostConstruct
    public void postConstruct() {
        System.out.println("UserAuthServiceEJB.postConstruct");
    }

    public boolean login(String name, String password)
    {
        System.out.println("UserAuthServiceEJBuser: logging in " + name);
        Connection c = null;
        Statement s = null;
        try
        {
            c = ds.getConnection();
            s = c.createStatement();
            String sql = "select count(*) as record_count from " +
                    Activator.tableName +" where name = '" + name +
                    "' and password= '" + password + "'";
            System.out.println("sql = " + sql);
            ResultSet rs = s.executeQuery(sql);
            rs.next();
            if (rs.getInt("record_count") == 1) {
                System.out.println("Login successful");
                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (c!= null) c.close();
                if (s!=null) s.close();
            }
            catch (Exception e)
            {
            }
        }
        return false;
    }

    public boolean register(String name, String password)
    {
        System.out.println("UserAuthServiceEJB: registering " + name);
        Connection c = null;
        Statement s = null;
        try
        {
            c = ds.getConnection();
            s = c.createStatement();
            String sql = "insert into " + Activator.tableName +
                    " values('" + name + "', '" + password + "')";
            System.out.println("sql = " + sql);
            s.executeUpdate(sql);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (c!= null) c.close();
                if (s!=null) s.close();
            }
            catch (Exception e)
            {
            }
        }
        return false;
    }
}
