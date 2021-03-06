/*
 *     This file is part of mongowp.
 *
 *     mongowp is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     mongowp is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with mongowp. If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2014, 8Kdata Technology
 *     
 */


package com.eightkdata.mongowp.mongoserver.util;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

/**
 *
 */
public class ByteBufUtil {
    public static final byte CSTRING_BYTE_TERMINATION = (byte) '\0';

    /**
     * A method that reads a C-string from a ByteBuf.
     * This method modified the internal state of the ByteBuf, advancing the read pointer to the position after the
     * cstring.
     *
     * @param buffer
     * @return The C-String as a String object or null if there was no C-String in the ByteBuf
     */
    public static String readCString(ByteBuf buffer) {
        int pos = buffer.bytesBefore(CSTRING_BYTE_TERMINATION);
        if(pos == -1) {
            return null;
        }
        byte[] bytes = new byte[pos];
        buffer.readBytes(bytes);
        buffer.readByte();  // Discard the termination byte

        return new String(bytes, Charsets.UTF_8);
    }
}
