/*
 * Copyright 2006-2012 ICEsoft Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepdf.core.pobjects.functions.postscript;

import java.util.Stack;

/**
 * Representing a generic Expression which follows the Command pattern for delayed
 * or differed execution.  Expression is just another type of Operator but we
 * can use an instanceof check to find occurrences of the object.
 *
 * @author ICEsoft Technologies Inc.
 * @since 4.2
 */
public class Expression extends Operator {


    protected Expression(int type) {
        super(type);
    }

    @Override
    public void eval(Stack stack) {
        // nothing to do for an expression
    }
}
