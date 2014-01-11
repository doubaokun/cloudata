package com.cloudata.structured.operation;

import java.util.Map.Entry;
import java.util.Set;

import com.cloudata.btree.Btree;
import com.cloudata.btree.Keyspace;
import com.cloudata.btree.Transaction;
import com.cloudata.btree.WriteTransaction;
import com.cloudata.btree.operation.ComplexOperation;
import com.cloudata.btree.operation.SimpleSetOperation;
import com.cloudata.structured.StructuredProtocol.StructuredAction;
import com.cloudata.structured.StructuredProtocol.StructuredActionResponse;
import com.cloudata.structured.StructuredProtocol.StructuredActionType;
import com.cloudata.structured.StructuredStore;
import com.cloudata.values.Value;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;

public class StructuredSetOperation extends StructuredOperationBase implements
        ComplexOperation<StructuredActionResponse> {

    private final StructuredStore store;

    public StructuredSetOperation(StructuredAction action, StructuredStore store) {
        super(action);
        this.store = store;
    }

    @Override
    public void doAction(Btree btree, Transaction transaction) {
        WriteTransaction txn = (WriteTransaction) transaction;

        // TODO: Get tablespace, check the type there (protobuf vs json etc)

        // Set the value
        Value newValue = Value.fromRawBytes(action.getValue());
        txn.doAction(btree, new SimpleSetOperation(qualifiedKey, newValue));

        // Update the key dictionary
        JsonObject json = newValue.asJsonObject();
        Set<String> keys = Sets.newHashSet();

        for (Entry<String, JsonElement> entry : json.entrySet()) {
            keys.add(entry.getKey());
        }

        Keyspace keyspace = Keyspace.user(action.getKeyspaceId());
        store.getKeys().ensureKeys(txn, keyspace, keys);
    }

    public static StructuredSetOperation build(long storeId, Keyspace keyspace, ByteString key, Value value) {
        StructuredAction.Builder b = StructuredAction.newBuilder();
        b.setAction(StructuredActionType.STRUCTURED_SET);
        b.setStoreId(storeId);
        b.setKeyspaceId(keyspace.getKeyspaceId());
        b.setKey(key);
        b.setValue(ByteString.copyFrom(value.asBytes()));

        // We don't need the store yet..
        StructuredStore store = null;

        return new StructuredSetOperation(b.build(), store);
    }

}
