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

package org.glassfish.security.services.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.util.LocalStringManagerImpl;


public class SecureServiceAccessPermission extends BasicPermission {

    private static final long serialVersionUID = -274181305911341984L;
        
    public static final String RW_ACTION = "read,write";
    public static final String READ_ACTION = "read";
    public static final String WRITE_ACTION = "write";

    private static final Logger _log =
            Logger.getLogger("org.glassfish.security.services");
    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(SecureServiceAccessPermission.class);

    /**
     * Read action.
     */
    private final static int READ    = 0x1;

    /**
     * Write action.
     */
    private final static int WRITE   = 0x2;
    /**
     * All actions (read,write);
     */
    private final static int ALL     = READ|WRITE;

    private final static int NONE    = 0x0;

    private transient int mask;

    private transient boolean wildcard;

    private transient String path;
        
    private String target;  //not used for now
    private String actions; //not used for now

    /**
     * 
     * @param accessPermissionName  the permission name used inside the 'Secure' annotation of the protected service
     */
    public SecureServiceAccessPermission(String accessPermissionName) {
        this(accessPermissionName, null);
    }

    
    /**
     * 
     * @param accessPermissionName the permission name used inside the 'Secure' annotation of the protected service
     * @param actions  use null (not used for now) 
     */
    public SecureServiceAccessPermission(String accessPermissionName, String actions) {
        super(accessPermissionName, actions);
        this.actions = actions;
        initWildCardPath(accessPermissionName);
        init(getMask(actions));
    }

    /**
     * 
     * @param accessPermissionName the permission name used inside the 'Secure' annotation of the protected service
     * @param actions use null (not used for now)
     * @param targetName use null (not used for now)
     */
    public SecureServiceAccessPermission(String accessPermissionName, String actions,
            String targetName) {
        this(accessPermissionName, actions);
        this.target = targetName;
    }
        
    private void init(int mask)
    {

        if ((mask & ALL) != mask)
            throw new IllegalArgumentException(
                localStrings.getLocalString("perm.invalid.action", "invalid actions mask"));

        if (getName() == null)
            throw new NullPointerException(
                localStrings.getLocalString("perm.null.name", "name can't be null"));

        this.mask = mask;
    }

    //base on J2SE implementation
    private static int getMask(String actions) {

        int mask = NONE;

        if (actions == null) {
            return mask;
        }

        // Check against use of constants (used heavily within the JDK)
        if (actions.equalsIgnoreCase(READ_ACTION)) {
            return READ;
        } if (actions.equalsIgnoreCase(WRITE_ACTION)) {
            return WRITE;
        } else if (actions.equalsIgnoreCase(RW_ACTION)) {
            return READ|WRITE;
        }

        char[] a = actions.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;

            // skip whitespace
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                               c == '\r' ||
                               c == '\n' ||
                               c == '\f' ||
                               c == '\t'))
                i--;

            // check for the known strings
            int matchlen;

            if (i >= 3 && (a[i-3] == 'r' || a[i-3] == 'R') &&
                          (a[i-2] == 'e' || a[i-2] == 'E') &&
                          (a[i-1] == 'a' || a[i-1] == 'A') &&
                          (a[i] == 'd' || a[i] == 'D'))
            {
                matchlen = 4;
                mask |= READ;

            } else if (i >= 4 && (a[i-4] == 'w' || a[i-4] == 'W') &&
                                 (a[i-3] == 'r' || a[i-3] == 'R') &&
                                 (a[i-2] == 'i' || a[i-2] == 'I') &&
                                 (a[i-1] == 't' || a[i-1] == 'T') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 5;
                mask |= WRITE;

            } else {
                // parse error
                throw new IllegalArgumentException(
                                localStrings.getLocalString(
                                                "perm.invalid.action", "invalid actions: {0}", actions));
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfaccept".  Also, skip to the comma.
            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch(a[i-matchlen]) {
                case ',':
                    seencomma = true;
                    /*FALLTHROUGH*/
                case ' ': case '\r': case '\n':
                case '\f': case '\t':
                    break;
                default:
                    throw new IllegalArgumentException(
                        localStrings.getLocalString(
                                        "perm.invalid.actions", "invalid actions: {0}", actions));
                }
                i--;
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen;
        }

        return mask;
    }

    @Override
    public String getActions() {
            return actions;
    }
        
    int getActionMask() {
        return mask;
    }

        
    public String getTarget() {
            return target;
    }
        
        @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if ((obj == null) || (obj.getClass() != getClass()))
            return false;

        SecureServiceAccessPermission sp = (SecureServiceAccessPermission)obj;
        
        return twoStringEq(getName(), sp.getName()) &&
                   mask == sp.getActionMask() &&
                   twoStringEq(this.getTarget(), sp.getTarget());
    }

    //compare two strings
    private static boolean twoStringEq(String s1, String s2) {
            
            if (s1 == null && s2 == null)
                return true;
            
            if (s1 == null) {
                //s2 not null, s1 is null
                return false;
            } else 
                //s1 not null, 
                return s1.equals(s2);
    }
    
    @Override
    public int hashCode() {
        return getName().hashCode(); 
    }
        
        
    @Override
    public boolean implies(Permission p) {
        if ((p == null) || (p.getClass() != getClass()))
            return false;

        if (!(p instanceof SecureServiceAccessPermission))
            return false;
        
        SecureServiceAccessPermission that = (SecureServiceAccessPermission) p;

        
        boolean result = ((this.mask & that.mask) == that.mask) && nameImplies(that);
        
        if (_log.isLoggable(Level.FINE)) {
                _log.log(Level.FINE, "Implies for permission " + p + " return " + result);
        }
        
        return result;
    }
    
    private boolean nameImplies(SecureServiceAccessPermission that) {
        
        if (this.wildcard) {
            if (that.wildcard) {
                // one wildcard can imply another
                return that.path.startsWith(path);
            } else {
                // make sure ap.path is longer so a/b/* doesn't imply a/b
                return (that.path.length() > this.path.length()) &&
                    that.path.startsWith(this.path);
            }
        } else {
            if (that.wildcard) {
                // a non-wildcard can't imply a wildcard
                return false;
            }
            else {
                return this.path.equals(that.path);
            }
        }
    }
    

    private void initWildCardPath(String name)
    {
        if (name == null)
            throw new NullPointerException(
                        localStrings.getLocalString("perm.null.name", "name can't be null"));

        int len = name.length();

        if (len == 0) {
            throw new IllegalArgumentException(
                        localStrings.getLocalString("perm.empty.name", "name can't be empty"));
        }

        char last = name.charAt(len - 1);

        // Is wildcard or ends with "/*"?
        if (last == '*' && (len == 1 || name.charAt(len - 2) == '/')) {
            wildcard = true;
            if (len == 1) {
                path = "";
            } else {
                path = name.substring(0, len - 1);
            }
        } else {
            path = name;
        }
    }
    

    final String getCanonicalName() {
        return  getName();
    }

    public PermissionCollection newPermissionCollection() {
        return new SecurityAccessPermissionCollection(this.getClass(), _log, localStrings);
    }

}



final class SecurityAccessPermissionCollection extends PermissionCollection  {

        private static final long serialVersionUID = 2568719859057815986L;

        /**
         * Key is name, value is permission. All permission objects in collection
         * must be of the same type. Not serialized; see serialization section at
         * end of class.
         */
        private transient Map<String, Permission> perms;

        /**
         * This is set to <code>true</code> if this SecurityAccessPermissionCollection
         * contains a BasicPermission with '*' as its permission name.
         * 
         * @see #serialPersistentFields
         */
        private boolean all_allowed;

        /**
         * The class to which all BasicPermissions in this SecurityAccessPermissionCollection
         * belongs.
         * 
         * @see #serialPersistentFields
         */
        private Class permClass;

        
        private Logger log;
        private LocalStringManagerImpl localStrings;

        public SecurityAccessPermissionCollection(Class clazz, Logger log, LocalStringManagerImpl localStrings) {
                perms = new HashMap<String, Permission>(11);
                all_allowed = false;
                permClass = clazz;
                this.log = log;
                this.localStrings = localStrings;
        }

        /**
         * Adds a permission to the BasicPermissions. The key for the hash is
         * permission.path.
         * 
         * @param permission
         *            the Permission object to add.
         * 
         * @exception IllegalArgumentException
         *                - if the permission is not a BasicPermission, or if the
         *                permission is not of the same Class as the other
         *                permissions in this collection.
         * 
         * @exception SecurityException
         *                - if this SecurityAccessPermissionCollection object has been marked
         *                readonly
         */

        public void add(Permission permission) {
                if (!(permission instanceof SecureServiceAccessPermission))
                        throw new IllegalArgumentException("invalid permission: "
                                        + permission);
                if (isReadOnly())
                        throw new SecurityException(
                                        localStrings.getLocalString("perm.readonly",
                                        "attempt to add a Permission to a readonly PermissionCollection"));

                SecureServiceAccessPermission bp = (SecureServiceAccessPermission) permission;

                // make sure we only add new SecureServiceAccessPermissions of the same class
                // Also check null for compatibility with deserialized form from
                // previous versions.
                if (permClass == null) {
                        // adding first permission
                        permClass = bp.getClass();
                } else {
                        if (bp.getClass() != permClass)
                                throw new IllegalArgumentException(
                        localStrings.getLocalString(                                            
                                        "perm.invalid.perm", "invalid permission: {0}", permission));
                                                 
                }

                synchronized (this) {
                        perms.put(bp.getCanonicalName(), permission);
                }

                // No sync on all_allowed; staleness OK
                if (!all_allowed) {
                        if (bp.getCanonicalName().equals("*"))
                                all_allowed = true;
                }
        }

        /**
         * Check and see if this set of permissions implies the permissions
         * expressed in "permission".
         * 
         * @param p
         *            the Permission object to compare
         * 
         * @return true if "permission" is a proper subset of a permission in the
         *         set, false if not.
         */

        public boolean implies(Permission permission) {
                if (!(permission instanceof SecureServiceAccessPermission))
                        return false;

                SecureServiceAccessPermission bp = (SecureServiceAccessPermission) permission;

                // random subclasses of SecureServiceAccessPermission do not imply each other
                if (bp.getClass() != permClass)
                        return false;

                // short circuit if the "*" Permission was added
                if (all_allowed)
                        return true;

                // strategy:
                // Check for full match first. Then work our way up the
                // path looking for matches on a.b..*

                String path = bp.getCanonicalName();
                // System.out.println("check "+path);

                Permission x;

                synchronized (this) {
                        x = perms.get(path);
                }

                if (x != null) {
                        // we have a direct hit!
                        return x.implies(permission);
                }

                // work our way up the tree...
                int last, offset;

                offset = path.length() - 1;

                while ((last = path.lastIndexOf("/", offset)) != -1) {

                        path = path.substring(0, last + 1) + "*";
                        // System.out.println("check "+path);

                        synchronized (this) {
                                x = perms.get(path);
                        }

                        if (x != null) {
                                return x.implies(permission);
                        }
                        offset = last - 1;
                }

                if (log.isLoggable(Level.FINE))
                        log.log(Level.FINE, "pemission collection returns false");
                
                // we don't have to check for "*" as it was already checked
                // at the top (all_allowed), so we just return false
                return false;
        }

        /**
         * Returns an enumeration of all the SecureServiceAccessPermission objects in the
         * container.
         * 
         * @return an enumeration of all the SecureServiceAccessPermission objects.
         */

        public Enumeration<Permission> elements() {
                // Convert Iterator of Map values into an Enumeration
                synchronized (this) {
                        return Collections.enumeration(perms.values());
                }
        }

        // Need to maintain serialization interoperability with earlier releases,
        // which had the serializable field:
        //
        // @serial the Hashtable is indexed by the SecureServiceAccessPermission name
        //
        // private Hashtable permissions;
        /**
         * @serialField
         *                  permissions java.util.Hashtable The SecureServiceAccessPermissions in
         *                  this SecurityAccessPermissionCollection. All SecureServiceAccessPermissions in
         *                  the collection must belong to the same class. The
         *                  Hashtable is indexed by the SecureServiceAccessPermission name; the
         *                  value of the Hashtable entry is the permission.
         * @serialField
         *                  all_allowed boolean This is set to <code>true</code> if
         *                  this SecurityAccessPermissionCollection contains a
         *                  SecureServiceAccessPermission with '*' as its permission name.
         * @serialField
         *                  permClass java.lang.Class The class to which all
         *                  SecureServiceAccessPermissions in this SecurityAccessPermissionCollection
         *                  belongs.
         */
        private static final ObjectStreamField[] serialPersistentFields = {
                        new ObjectStreamField("permissions", Hashtable.class),
                        new ObjectStreamField("all_allowed", Boolean.TYPE),
                        new ObjectStreamField("permClass", Class.class), };

        /**
         * @serialData Default fields.
         */
        /*
         * Writes the contents of the perms field out as a Hashtable for
         * serialization compatibility with earlier releases. all_allowed and
         * permClass unchanged.
         */
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Don't call out.defaultWriteObject()
    
            // Copy perms into a Hashtable
            Hashtable<String, Permission> permissions =
                    new Hashtable<String, Permission>(perms.size()*2);
    
            synchronized (this) {
                permissions.putAll(perms);
            }
    
            // Write out serializable fields
            ObjectOutputStream.PutField pfields = out.putFields();
            pfields.put("all_allowed", all_allowed);
            pfields.put("permissions", permissions);
            pfields.put("permClass", permClass);
            out.writeFields();
        }

        /**
         * readObject is called to restore the state of the
         * SecurityAccessPermissionCollection from a stream.
         */
        private void readObject(java.io.ObjectInputStream in) throws IOException,
                        ClassNotFoundException {
            // Don't call defaultReadObject()

            // Read in serialized fields
            ObjectInputStream.GetField gfields = in.readFields();

            // Get permissions
            Hashtable<String, Permission> permissions = (Hashtable<String, Permission>) gfields
                            .get("permissions", null);
            perms = new HashMap<String, Permission>(permissions.size() * 2);
            perms.putAll(permissions);

            // Get all_allowed
            all_allowed = gfields.get("all_allowed", false);

            // Get permClass
            permClass = (Class) gfields.get("permClass", null);

            if (permClass == null) {
                    // set permClass
                    Enumeration<Permission> e = permissions.elements();
                    if (e.hasMoreElements()) {
                            Permission p = e.nextElement();
                            permClass = p.getClass();
                    }
            }
        }
}
