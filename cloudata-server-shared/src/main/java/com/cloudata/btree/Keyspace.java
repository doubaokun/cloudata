package com.cloudata.btree;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

public class Keyspace {

    private static final Logger log = LoggerFactory.getLogger(Keyspace.class);

    public static final Keyspace ZERO = Keyspace.user(0);
    private static final int SYSTEM_START = 65536;

    final int keyspaceId;
    final ByteString keyspaceIdPrefix;

    private Keyspace(int keyspaceId) {
        Preconditions.checkArgument(keyspaceId >= 0);
        this.keyspaceId = keyspaceId;

        if (keyspaceId < 128) {
            assert 1 == CodedOutputStream.computeInt32SizeNoTag(keyspaceId);
            byte[] data = new byte[1];
            data[0] = (byte) keyspaceId;
            this.keyspaceIdPrefix = ByteString.copyFrom(data);
        } else {
            try {
                byte[] data = new byte[CodedOutputStream.computeInt32SizeNoTag(keyspaceId)];
                CodedOutputStream c = CodedOutputStream.newInstance(data);
                c.writeInt32NoTag(keyspaceId);
                c.flush();

                this.keyspaceIdPrefix = ByteString.copyFrom(data);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    public int getKeyspaceId() {
        return keyspaceId;
    }

    public ByteString mapToKey(ByteString key) {
        return keyspaceIdPrefix.concat(key);
    }

    public static Keyspace user(int keyspaceId) {
        if (keyspaceId > SYSTEM_START) {
            throw new IllegalArgumentException();
        }
        return new Keyspace(keyspaceId);
    }

    public ByteString mapToKey(byte[] key) {
        return mapToKey(ByteString.copyFrom(key));
    }

    public static Keyspace system(int i) {
        return new Keyspace(SYSTEM_START + i);
    }

    public boolean contains(ByteBuffer buffer) {
        if (buffer.remaining() < keyspaceIdPrefix.size()) {
            return false;
        }
        for (int i = 0; i < keyspaceIdPrefix.size(); i++) {
            if (buffer.get(buffer.position() + i) != keyspaceIdPrefix.byteAt(i)) {
                // log.debug("Mismatch: {} vs {}", Hex.forDebug(buffer), Hex.forDebug(keyspaceIdPrefix));
                return false;
            }
        }
        return true;
    }

    public boolean keyIsAfter(ByteBuffer key) {
        for (int i = 0; i < keyspaceIdPrefix.size(); i++) {
            byte keyByte = key.get(key.position() + i);
            byte prefixByte = keyspaceIdPrefix.byteAt(i);
            if (keyByte != prefixByte) {
                int compare = ByteBuffers.compareUnsigned(keyByte, prefixByte);
                if (compare >= 0) {
                    assert compare != 0;
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public ByteBuffer getSuffix(ByteBuffer buffer) {
        assert contains(buffer);
        ByteBuffer dup = buffer.duplicate();
        dup.position(dup.position() + keyspaceIdPrefix.size());
        return dup;
    }

    public ByteString mapToKey(ByteBuffer key) {
        return mapToKey(ByteString.copyFrom(key));
    }

}
