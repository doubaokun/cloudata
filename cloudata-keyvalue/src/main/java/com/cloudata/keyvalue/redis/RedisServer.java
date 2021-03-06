package com.cloudata.keyvalue.redis;

import org.robotninjas.barge.RaftException;

import com.cloudata.btree.Keyspace;
import com.cloudata.keyvalue.KeyValueProtocol.ActionResponse;
import com.cloudata.keyvalue.KeyValueStateMachine;
import com.cloudata.keyvalue.operation.KeyValueOperation;
import com.cloudata.values.Value;
import com.google.protobuf.ByteString;

public class RedisServer {
    final KeyValueStateMachine stateMachine;
    final long storeId;

    public long getStoreId() {
        return storeId;
    }

    public RedisServer(KeyValueStateMachine stateMachine, long storeId) {
        this.stateMachine = stateMachine;
        this.storeId = storeId;
    }

    public ActionResponse doAction(KeyValueOperation operation) throws RedisException {
        try {
            return stateMachine.doActionSync(operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisException("Interrupted during processing", e);
        } catch (RaftException e) {
            throw new RedisException("Error during processing", e);
        }
    }

    public Value get(Keyspace keyspace, ByteString key) {
        return stateMachine.get(storeId, keyspace, key);
    }

}
