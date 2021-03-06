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


package com.eightkdata.mongowp.mongoserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

import com.eightkdata.mongowp.messages.request.RequestBaseMessage;
import com.eightkdata.mongowp.messages.request.RequestOpCode;
import com.eightkdata.mongowp.mongoserver.decoder.BaseMessageDecoder;
import com.eightkdata.mongowp.mongoserver.decoder.MessageDecoder;
import com.eightkdata.mongowp.mongoserver.decoder.MessageDecoderLocator;
import com.eightkdata.mongowp.mongoserver.exception.InvalidMessageException;
import com.eightkdata.mongowp.mongoserver.util.ChannelLittleEndianHandler;

/**
 *
 */
@NotThreadSafe
public class RequestMessageByteHandler extends ChannelLittleEndianHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMessageByteHandler.class);
    private static final String INVALID_OPCODE_MESSAGE = "Received and invalid message with opCode {}";
    private static final String OPERATION_NOT_IMPLEMENTED = "Message decoder not implemented for opCode {}";
    

    @Override
    protected void decodeLittleEndian(
            ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> objects
    ) throws Exception {
    	if (byteBuf instanceof EmptyByteBuf) {
    		//TODO: This is a workaround. Check how to prevent calling decode on channel inactive
    		return;
    	}
    	
        // Header
        RequestBaseMessage requestBaseMessage = BaseMessageDecoder.decode(channelHandlerContext, byteBuf);
        byteBuf.skipBytes(Ints.BYTES);  // Ignore responseTo field in header
        int requestOpCodeInt = byteBuf.readInt();
        RequestOpCode requestOpCode = RequestOpCode.getByOpcode(requestOpCodeInt);
        if (null == requestOpCode) {
            LOGGER.warn(INVALID_OPCODE_MESSAGE, requestOpCodeInt);
            throw new InvalidMessageException(INVALID_OPCODE_MESSAGE + requestOpCodeInt);
        }

        // Body
        MessageDecoder<?> messageDecoder = MessageDecoderLocator.getByOpCode(requestOpCode);
        if(null == messageDecoder) {
            LOGGER.error(OPERATION_NOT_IMPLEMENTED, requestOpCode);
            throw new UnsupportedOperationException(OPERATION_NOT_IMPLEMENTED + requestOpCode);
        }

        objects.add(messageDecoder.decode(byteBuf, requestBaseMessage));
    }
}
