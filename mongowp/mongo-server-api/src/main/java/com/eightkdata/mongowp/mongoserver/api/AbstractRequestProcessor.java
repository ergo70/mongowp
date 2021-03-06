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


package com.eightkdata.mongowp.mongoserver.api;

import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.eightkdata.mongowp.messages.request.QueryMessage;
import com.eightkdata.mongowp.messages.request.RequestBaseMessage;
import com.eightkdata.mongowp.messages.request.RequestOpCode;
import com.eightkdata.mongowp.mongoserver.api.QueryCommandProcessor.QueryCommand;
import com.eightkdata.mongowp.mongoserver.api.QueryCommandProcessor.QueryCommandGroup;
import com.eightkdata.mongowp.mongoserver.api.callback.MessageReplier;
import com.eightkdata.mongowp.mongoserver.api.callback.RequestProcessor;
import com.eightkdata.mongowp.mongoserver.api.commands.QueryReply;
import com.eightkdata.mongowp.mongoserver.api.commands.QueryRequest;
import com.eightkdata.nettybson.api.BSONDocument;
import com.eightkdata.nettybson.mongodriver.MongoBSONDocument;
import org.bson.BSONObject;

/**
 *
 */
public abstract class AbstractRequestProcessor implements RequestProcessor {
    public static final String QUERY_MESSAGE_COMMAND_COLLECTION = "$cmd";
    public static final String QUERY_MESSAGE_ADMIN_DATABASE = "admin";
    
    public static final AttributeKey<QueryCommand> QUERY_COMMAND = AttributeKey.valueOf("queryCommand");

    private final QueryCommandProcessor queryCommandProcessor;
    private final MetaQueryProcessor metaQueryProcessor;

    @Inject
    public AbstractRequestProcessor(
            @Nonnull QueryCommandProcessor queryCommandProcessor,
            @Nonnull MetaQueryProcessor metaQueryProcessor
    ) {
        this.queryCommandProcessor = queryCommandProcessor;
        this.metaQueryProcessor = metaQueryProcessor;
    }

    @Override
    public void queryMessage(@Nonnull QueryMessage queryMessage, @Nonnull MessageReplier messageReplier) throws Exception {
    	AttributeMap attributeMap = messageReplier.getAttributeMap();
    	RequestBaseMessage requestBaseMessage = queryMessage.getBaseMessage();
        BSONDocument query = queryMessage.getDocument();

    	attributeMap.attr(QUERY_COMMAND).set(null);
        if(QUERY_MESSAGE_COMMAND_COLLECTION.equals(queryMessage.getCollection())) {
            QueryCommand queryCommand = QueryCommandGroup.byQueryDocument(query);
            if(null == queryCommand) {
            	noSuchCommand(query, messageReplier);
                return;
            }
            
            if(queryCommand.isAdminOnly() && !QUERY_MESSAGE_ADMIN_DATABASE.equals(queryMessage.getDatabase())) {
            	adminOnlyCommand(queryCommand, messageReplier);
            	return;
            }
            
        	attributeMap.attr(QUERY_COMMAND).set(queryCommand);
            queryCommand.call(
                    requestBaseMessage, 
                    query, 
                    new QueryCommandProcessor.ProcessorCaller(
                            queryMessage.getDatabase(), 
                            queryCommandProcessor, 
                            metaQueryProcessor, 
                            messageReplier
                    )
            );
        } else {
            query(queryMessage, messageReplier);
        }
    }

    @Override
	public boolean handleError(@Nonnull RequestOpCode requestOpCode, @Nonnull MessageReplier messageReplier, @Nonnull Throwable throwable) throws Exception {
    	AttributeMap attributeMap = messageReplier.getAttributeMap();
		if (requestOpCode == RequestOpCode.OP_QUERY) {
			if (attributeMap.attr(QUERY_COMMAND).get() != null) {
				queryCommandProcessor.handleError(attributeMap.attr(QUERY_COMMAND).get(), messageReplier, throwable);
				return true;
			}
		}
		
		return false;
	}

	public void query(@Nonnull QueryMessage queryMessage, @Nonnull MessageReplier messageReplier) throws Exception {
        QueryRequest.Builder requestBuilder = new QueryRequest.Builder(
                queryMessage.getDatabase(),
                messageReplier.getAttributeMap()
        );
        requestBuilder.setCollection(queryMessage.getCollection())
                .setQuery(extractQuery(queryMessage.getDocument()))
                .setProjection(null)
                .setNumberToSkip(queryMessage.getNumberToSkip())
                .setLimit(queryMessage.getNumberToReturn())
                .setAwaitData(queryMessage.isFlagSet(QueryMessage.Flag.AWAIT_DATA))
                .setExhaust(queryMessage.isFlagSet(QueryMessage.Flag.EXHAUST))
                .setNoCursorTimeout(queryMessage.isFlagSet(QueryMessage.Flag.NO_CURSOR_TIMEOUT))
                .setOplogReplay(queryMessage.isFlagSet(QueryMessage.Flag.OPLOG_REPLAY))
                .setPartial(queryMessage.isFlagSet(QueryMessage.Flag.PARTIAL))
                .setSlaveOk(queryMessage.isFlagSet(QueryMessage.Flag.SLAVE_OK))
                .setTailable(queryMessage.isFlagSet(QueryMessage.Flag.TAILABLE_CURSOR));
        
        if (requestBuilder.getLimit() < 0) {
            requestBuilder.setAutoclose(true);
            requestBuilder.setLimit(-requestBuilder.getLimit());
        }
        else if (requestBuilder.getLimit() == 1) {
            requestBuilder.setAutoclose(true);
        }
        
        QueryReply reply;
        if (metaQueryProcessor.isMetaQuery(queryMessage)) {
            reply = metaQueryProcessor.query(requestBuilder.build());
        }
        else {
            reply = query(requestBuilder.build());
        }
        reply.reply(messageReplier);
    }
	
	public abstract void noSuchCommand(@Nonnull BSONDocument query, @Nonnull MessageReplier messageReplier) throws Exception;
	
	public abstract void adminOnlyCommand(@Nonnull QueryCommand queryCommand, @Nonnull MessageReplier messageReplier) throws Exception;

    public abstract QueryReply query(QueryRequest build) throws Exception;
    
    private BSONObject extractQuery(BSONDocument document) {
        BSONObject query = ((MongoBSONDocument) document).getBSONObject();
        for (String key : query.keySet()) {
    		if ("query".equals(key) || "$query".equals(key)) {
    			Object queryObject = query.get(key);
    			if (queryObject != null && queryObject instanceof BSONObject) {
    				return (BSONObject) queryObject;
    			}
    		}
    	}
        return query;
    }
}
