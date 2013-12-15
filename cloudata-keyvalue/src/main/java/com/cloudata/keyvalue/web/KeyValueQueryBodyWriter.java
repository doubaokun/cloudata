package com.cloudata.keyvalue.web;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudata.keyvalue.btree.EntryListener;
import com.google.common.base.Throwables;

@Provider
@Singleton
@Produces({ "application/octet-stream", "*/*" })
public final class KeyValueQueryBodyWriter implements MessageBodyWriter<KeyValueQuery> {

    private static final Logger log = LoggerFactory.getLogger(KeyValueQueryBodyWriter.class);

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return KeyValueQuery.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(KeyValueQuery query, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(KeyValueQuery query, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        final DataOutputStream dos = new DataOutputStream(entityStream);

        final byte[] buffer = new byte[8192];

        EntryListener entryListener = new EntryListener() {
            @Override
            public boolean found(ByteBuffer key, ByteBuffer value) {
                try {
                    dos.writeInt(key.remaining());
                    copy(key, dos, buffer);
                    dos.writeInt(value.remaining());
                    copy(value, dos, buffer);
                    return true;
                } catch (IOException e) {
                    log.warn("Error writing output", e);
                    throw Throwables.propagate(e);
                }
            }
        };

        try (KeyValueQuery.KeyValueResultset cursor = query.execute()) {
            cursor.walk(entryListener);
        }

        dos.writeInt(-1);

        dos.flush();
    }

    static void copy(ByteBuffer src, OutputStream os, byte[] buffer) throws IOException {
        while (true) {
            int n = src.remaining();
            if (n == 0) {
                break;
            }

            n = Math.min(buffer.length, n);

            src.get(buffer, 0, n);

            os.write(buffer, 0, n);
        }
    }

}