/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2007,2010 Oracle and/or its affiliates. All rights reserved.
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

package javax.resource.spi.security;

import javax.resource.spi.ManagedConnectionFactory;

/**
 * The class PasswordCredential acts as a holder for username and
 * password.
 *
 * @see javax.resource.spi.ManagedConnectionFactory
 *
 * @author  Rahul Sharma
 * @version 0.6
 * @since   0.6
 */

public final class PasswordCredential implements java.io.Serializable {

  private String userName;
  private char[] password;
  private ManagedConnectionFactory mcf;

  /**
   * Creates a new <code>PasswordCredential</code> object from the given
   * user name and password.
   *
   * <p> Note that the given user password is cloned before it is stored in
   * the new <code>PasswordCredential</code> object.
   *
   * @param userName the user name
   * @param password the user's password
  **/
  public 
  PasswordCredential(String userName, char[] password) {
    this.userName = userName;
    this.password = (char[])password.clone();
  }

  /**
   * Returns the user name.
   *
   * @return the user name
  **/
  public 
  String getUserName() {
    return userName;
  }

  /**
   * Returns the user password.
   *
   * <p> Note that this method returns a reference to the password. It is
   * the caller's responsibility to zero out the password information after
   * it is no longer needed.
   *
   * @return the password
  **/
  public 
  char[] getPassword() {
    return password;
  }

  /** Gets the target ManagedConnectionFactory for which the user name and 
   *  password has been set by the application server. A ManagedConnection-
   *  Factory uses this field to find out whether PasswordCredential should
   *  be used by it for sign-on to the target EIS instance.
   *
   *  @return    ManagedConnectionFactory instance for which user name and
   *             password have been specified
   **/
  public
  ManagedConnectionFactory getManagedConnectionFactory() {
    return mcf;
  }

 /**  Sets the target ManagedConenctionFactory instance for which the user 
  *   name and password has been set by the application server.
   *
   *  @param     mcf   ManagedConnectionFactory instance for which user name
   *                   and password have been specified
   **/
  public
  void setManagedConnectionFactory(ManagedConnectionFactory mcf) {
    this.mcf = mcf;
  }

  /** Compares this PasswordCredential with the specified object for 
   *  equality. The two PasswordCredential instances are the same if
   *  they are equal in username and password.
   *
   *  @param other  Object to which PasswordCredential is to be compared
   *  @return <tt>true</tt> if and if the specified object is a
   *            PasswordCredential whose username and password are
   *            equal to this instance.
  **/
  public 
  boolean equals(Object other) {
    if (!(other instanceof PasswordCredential))
      return false;

    PasswordCredential pc = (PasswordCredential)other;

    if (!(userName.equals(pc.userName)))
      return false;

    if (password.length != pc.password.length)
      return false;
    
    for (int i = 0; i < password.length;i++) {
      if (password[i] != pc.password[i]) 
	return false;
    }

    return true;
  }

  /** Returns the hash code for this PasswordCredential
   * 
   *  @return  hash code for this PasswordCredential
  **/
  public
  int hashCode() {
    String s = userName;

      int passwordHash = 0;
      for (char passChar : password) {
          passwordHash += passChar;
      }

    return s.hashCode() + passwordHash;
  }

}

