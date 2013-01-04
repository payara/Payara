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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl.algorithm;

/**
 * Perform's string substitution for the given input.  Substitution process look 
 * for the matching input in the given {@link RadixTree} and replaced the string
 * with the corresponding matching value.
 *
 * @see {@link RadixTree}
 */
class RadixTreeSubstitution {
    /** {@link RadixTree} used sub. */
    private RadixTree _tree;

    /** Buffer to store the current processing characters, reset when match found
     *  for the processed character. */
    private StringBuffer _processedChars;

    /** Reference to the currently processing node.*/
    private RadixTreeNode _currentNode;

    /** No of matched character in currently processing node. */
    private int _nodeMatchedChars;

    /** Last matched node value. */
    private String _lastMatchedValue;

    /** Buffer to store the characters need to re-process from root. */
    private StringBuffer _reProcessChars;

    /**
     * Construct {@link RadixTreeSubstitution} for the given {@link RadixTree}.
     * @param tree
     */
    RadixTreeSubstitution(RadixTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("Invalid tree.");
        }
        _tree = tree;
        _processedChars = new StringBuffer();
        _currentNode = _tree.getRootNode();
    }

    /**
     * Perform substitution by allowing continuous character feeding. Once
     * the character sequence matched completely to {@link RadixTree} node
     * and no further/extended match is available then the output is returned
     * and the values reset to re-look the new input from root node.
     * <p>
     * Method maintains the processed characters, currently processing
     * node and other parameters require in substitution.
     * </p>
     * <p>
     * <b>NOTE:</b> A <code>null</code> input signify the end of the processing.
     * </p>
     * @param c Input character to match with the node key.
     * @return
     * <li>Value of the matching node, if no further match available.</li>
     * <li>Return the string of processed characters if no matching node found
     *   or the node value is null.</li>
     * <li><code>null</code> if waiting for more input char.</li> 
     */
    String substitute(Character c) {
        StringBuffer outputBuffer = null;
        boolean finalCall = (c == null) ? true : false;
        do {
            if (c != null) {
                if (_reProcessChars != null && _reProcessChars.length() > 0) {
                    c = _reProcessChars.charAt(0);
                    _reProcessChars.delete(0, 1);
                }
                String nodeKey = _currentNode.getKey();
                if (_nodeMatchedChars < nodeKey.length()) {
                    if (c == nodeKey.charAt(_nodeMatchedChars)) {
                        _processedChars.append(c);
                        _nodeMatchedChars++;
                        continue;
                    }
                } else {
                    if (_currentNode.getValue() != null) {
                        _lastMatchedValue = _currentNode.getValue();
                        _processedChars.delete(0, _processedChars.length());
                    }
                    RadixTreeNode childNode =  _currentNode.getChildNode(c);
                    if (childNode != null) {
                        _processedChars.append(c);
                        _currentNode = childNode;
                        _nodeMatchedChars = 1;
                        continue;
                    }
                }
            }
            else if(_currentNode.getValue() != null &&
                    _nodeMatchedChars == _currentNode.getKey().length()) {
                _lastMatchedValue = _currentNode.getValue();
                _processedChars.delete(0, _processedChars.length());
            }

            if (outputBuffer == null) {
                outputBuffer = new StringBuffer();
            }
            // write to the output buffer.
            if (_lastMatchedValue != null) {
                outputBuffer.append(_lastMatchedValue);
                if (c != null) {
                    _processedChars.append(c);
                }
                _lastMatchedValue = null;
            } else {
                // If no match found than append the first character and start fresh from second character.
                if (_processedChars.length() > 0) {
                    outputBuffer.append(_processedChars.charAt(0));
                    _processedChars.delete(0, 1);
                    if (c != null) {
                        _processedChars.append(c);
                    }
                } else {
                    if (c != null) {
                        outputBuffer.append(c);
                    }
                    _processedChars.delete(0, _processedChars.length());
                }
            }
            if (_processedChars.length() > 0) {
                if (_reProcessChars == null) {
                    _reProcessChars = new StringBuffer(_processedChars);
                } else {
                    _processedChars.append(_reProcessChars);
                    _reProcessChars.delete(0, _reProcessChars.length());
                    _reProcessChars.append(_processedChars);
                }
                c = _reProcessChars.charAt(0);
                _processedChars.delete(0, _processedChars.length());
            }
            _currentNode = _tree.getRootNode();
            _nodeMatchedChars = 0;
        } while (_reProcessChars != null && _reProcessChars.length() > 0);

        //Append the last process character sequence.
        if (finalCall) {
            if (_nodeMatchedChars == _currentNode.getKey().length()
                    && _currentNode.getValue() != null) {
                outputBuffer.append(_currentNode.getValue());
            } else {
                outputBuffer.append(_currentNode.getKey().substring(0, _nodeMatchedChars));
            }
            _processedChars.delete(0, _processedChars.length());
            _currentNode = _tree.getRootNode();
            _nodeMatchedChars = 0;
            _lastMatchedValue = null;
        }
        return outputBuffer == null || outputBuffer.toString().isEmpty() ? null : outputBuffer.toString();
    }
}