/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.truffleruby.core.rope;

import org.jcodings.Encoding;

public class ValidLeafRope extends LeafRope {

    public ValidLeafRope(byte[] bytes, Encoding encoding, int characterLength) {
        super(bytes, encoding, CodeRange.CR_VALID, encoding.isSingleByte(), characterLength);

        assert !RopeOperations.isAsciiOnly(bytes, encoding) : "ASCII-only string incorrectly marked as CR_VALID";
        assert !RopeOperations.isInvalid(bytes, encoding) : "Broken string incorrectly marked as CR_VALID";
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            throw new UnsupportedOperationException("Cannot fast-path updating encoding with different code range.");
        }

        // TODO (nirvdrum 08-Mar-16): This should recalculate the character length since the new encoding may treat the bytes differently.
        return new ValidLeafRope(getRawBytes(), newEncoding, characterLength());
    }
}
