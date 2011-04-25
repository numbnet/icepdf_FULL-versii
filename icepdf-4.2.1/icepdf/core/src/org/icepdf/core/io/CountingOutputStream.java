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
package org.icepdf.core.io;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Keeps track of how many bytes have been written out
 * @since 4.0
 */

public class CountingOutputStream extends OutputStream {
    private OutputStream wrapped;
    private long count;

    public CountingOutputStream(OutputStream wrap) {
        wrapped = wrap;
        count = 0L;
    }

    public long getCount() {
        return count;
    }

    public void write(int i) throws IOException {
        wrapped.write(i);
        count++;
    }

    public void write(byte[] bytes) throws IOException {
        wrapped.write(bytes);
        count += bytes.length;
    }

    public void write(byte[] bytes, int offset, int len) throws IOException {
        wrapped.write(bytes, offset, len);
        int num = Math.min(len, bytes.length - offset);
        count += num;
    }

    public void flush() throws IOException {
        wrapped.flush();
    }

    public void close() throws IOException {
        wrapped.close();
    }
}
