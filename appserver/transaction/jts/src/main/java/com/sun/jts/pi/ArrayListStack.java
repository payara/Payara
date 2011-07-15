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

/*
 * ArrayListStack.java
 * Author: darpan.dinker@sun.com
 * Created on June 21, 2002, 3:42 PM
 *
 */

package com.sun.jts.pi;

import java.util.ArrayList;

/**
 * The <code>ArrayListStack</code> class represents a last-in-first-out
 * (LIFO) stack of objects. It encapsulates class <tt>ArrayList</tt> with four
 * operations that allow a list to be treated as a stack. The usual
 * <tt>push</tt> and <tt>pop</tt> operations are provided, as well as a
 * method to <tt>peek</tt> at the top item on the stack, and a method to test
 * for whether the stack is <tt>empty</tt>
 * <p>
 * When a stack is first created, it contains no items.
 * @author  Darpan Dinker, $Author: tcfujii $
 * @version $Revision: 1.3 $ on $Date: 2005/12/25 04:12:09 $
 */
public class ArrayListStack {
    private int curIndex;
    private ArrayList list;
    
    /** Creates a stack with the given initial size */
    public ArrayListStack(int size) {
        curIndex = 0;
        list = new ArrayList(size);
    }
    
    /** Creates a stack with a default size */
    public ArrayListStack() {
        this(20);
    }
    
    /**
     * Provides the current size of the stack.
     * @return int return the current size.
     */
    public int size() {
        return curIndex;
    }
    
    /**
     * Pushes an item onto the top of this stack. This method will internally
     * add elements to the <tt>ArrayList</tt> if the stack is full.
     * @param   obj   the object to be pushed onto this stack.
     * @see     java.util.ArrayList#add
     */
    public void push(Object obj) {
        list.add(curIndex, obj);
        curIndex += 1;
    }
    
    /**
     * Removes the object at the top of this stack and returns that
     * object as the value of this function.
     * @return     The object at the top of this stack (the last item
     *             of the <tt>ArrayList</tt> object). Null if stack is empty.
     */
    public Object pop() {
        if (curIndex > 0) {
            curIndex -= 1;
            return list.remove(curIndex);
        }
        return null;
    }
    
    /**
     * Tests if this stack is empty.
     * @return  <code>true</code> if and only if this stack contains
     *          no items; <code>false</code> otherwise.
     */
    public boolean empty() {
        return curIndex == 0;
    }
    
    /**
     * Looks at the object at the top of this stack without removing it
     * from the stack.
     * @return     the object at the top of this stack (the last item
     *             of the <tt>ArrayList</tt> object).  Null if stack is empty.
     */
    public Object peek() {
        Object top = null;
        if (curIndex > 0) {
            top = list.get(curIndex - 1);
        }
        return top;
    }
}
