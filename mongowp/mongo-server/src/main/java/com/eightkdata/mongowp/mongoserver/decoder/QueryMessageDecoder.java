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


package com.eightkdata.mongowp.mongoserver.decoder;

import com.eightkdata.mongowp.messages.request.QueryMessage;
import com.eightkdata.mongowp.messages.request.RequestBaseMessage;
import com.eightkdata.mongowp.mongoserver.exception.InvalidMessageException;
import com.eightkdata.mongowp.mongoserver.util.ByteBufUtil;
import com.eightkdata.nettybson.api.BSONDocument;
import com.eightkdata.nettybson.mongodriver.MongoBSONDocument;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnegative;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class QueryMessageDecoder implements MessageDecoder<QueryMessage> {
    @Override
    public @Nonnegative
    QueryMessage decode(ByteBuf buffer, RequestBaseMessage requestBaseMessage) throws InvalidMessageException {
        int flags = buffer.readInt();
        String fullCollectionName = ByteBufUtil.readCString(buffer);
        int numberToSkip = buffer.readInt();
        int numberToReturn = buffer.readInt();
        BSONDocument document = new MongoBSONDocument(buffer);
        BSONDocument returnFieldsSelector = buffer.readableBytes() > 0 ? new MongoBSONDocument(buffer) : null;

        return new QueryMessage(
                requestBaseMessage, flags, fullCollectionName, numberToSkip, numberToReturn, document, returnFieldsSelector
        );
    }
}
