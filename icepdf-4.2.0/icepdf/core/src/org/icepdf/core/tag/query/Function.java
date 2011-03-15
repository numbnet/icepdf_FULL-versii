/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEpdf 3.0 open source software code, released
 * May 1st, 2009. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2011 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"
 * License), in which case the provisions of the LGPL License are
 * applicable instead of those above. If you wish to allow use of your
 * version of this file only under the terms of the LGPL License and not to
 * allow others to use your version of this file under the MPL, indicate
 * your decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the LGPL License. If you do
 * not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the LGPL License."
 *
 */
package org.icepdf.core.tag.query;

import org.icepdf.core.tag.TaggedDocument;

/**
 * @author mcollette
 * @since 4.0
 */
public abstract class Function implements Expression {
    protected String[] arguments;

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

    public String describe(int indent) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indent; i++)
            sb.append("  ");
        String className = getClass().getName();
        className = className.substring(className.lastIndexOf(".") + 1);
        sb.append(className);
        sb.append(" ( ");
        int num = (arguments != null) ? arguments.length : 0;
        for(int i = 0; i < num; i++) {
            sb.append('\'');
            sb.append(arguments[i]);
            sb.append('\'');
            if (i < (num-1))
                sb.append(", ");
        }
        sb.append(" )\n");
        return sb.toString();
    }
}
