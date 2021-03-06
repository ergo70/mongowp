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


package com.eightkdata.nettybson.api;


import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Set;

/**
 *
 */
@Immutable
public interface BSONDocument {
    public boolean hasKey(@Nonnull String key);
    @Nonnull public Set<String> getKeys();
    public Object getValue(@Nonnull String key);

    /**
     * Writes the BSON document to the given ByteBuf.
     * This method advances the internal write position of the ByteBuf, leaving it just after writing the document
     * @param buffer
     */
    public void writeToByteBuf(@Nonnull ByteBuf buffer);
}
