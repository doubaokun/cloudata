package com.cloudata.btree;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.google.protobuf.ByteString;

public class ByteBuffers {

    public static int compare(ByteBuffer l, ByteBuffer r) {
        int n = Math.min(l.remaining(), r.remaining());

        int lPos = l.position();
        int rPos = r.position();

        int end = lPos + n;
        while (lPos < end) {
            int comparison = compareUnsigned(l.get(lPos), r.get(rPos));
            if (comparison != 0) {
                return comparison;
            }
            lPos++;
            rPos++;
        }

        return Integer.compare(l.remaining(), r.remaining());
    }

    public static int compare(ByteBuffer l, ByteBuffer r, int length) {
        int n = Math.min(l.remaining(), r.remaining());

        int lPos = l.position();
        int rPos = r.position();

        int end = lPos + n;
        while (lPos < end) {
            int comparison = compareUnsigned(l.get(lPos), r.get(rPos));
            if (comparison != 0) {
                return comparison;
            }
            lPos++;
            rPos++;
        }

        if (length <= n) {
            return 0;
        } else {
            // One (or both) of the byte-strings was shorter than the requested length
            return Integer.compare(l.remaining(), r.remaining());
        }
    }

    public final static int compareUnsigned(byte l, byte r) {
        // Compare as unsigned
        int il = l & 0xff;
        int ir = r & 0xff;

        return Integer.compare(il, ir);
    }

    public static long parseLong(ByteBuffer buff) {
        long v = 0;
        boolean negative = false;
        for (int i = buff.position(); i < buff.limit(); i++) {
            byte b = buff.get(i);

            if (b >= '0' && b <= '9') {
                v = (v * 10) + (b - '0');
                continue;
            } else if (b == '-') {
                if (i == buff.position()) {
                    negative = true;
                    continue;
                }
            }

            throw new IllegalArgumentException("Unexpected character in long: " + ((int) b));
        }

        if (negative) {
            v = -v;
        }
        return v;
    }

    public static ByteBuffer clone(ByteBuffer b) {
        int n = b.remaining();
        ByteBuffer buff = ByteBuffer.allocate(n);
        buff.put(b.duplicate());
        buff.flip();
        return buff;
    }

    public static ByteBuffer asReadOnlyBuffer(ByteString b) {
        if (b == null) {
            return null;
        }
        return b.asReadOnlyByteBuffer();
    }

    public static String toString(Charset charset, ByteBuffer buffer) {
        // TODO: Something more efficient

        buffer = buffer.duplicate();
        byte[] array = new byte[buffer.remaining()];
        buffer.get(array);
        String s = new String(array, charset);

        return s;
    }
}
