package com.cloudata.keyvalue.freemap;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.List;

import com.cloudata.keyvalue.KeyValueProto.KvAction;
import com.cloudata.keyvalue.btree.EntryListener;
import com.cloudata.keyvalue.btree.Page;
import com.cloudata.keyvalue.btree.PageStore;
import com.cloudata.keyvalue.btree.Transaction;
import com.cloudata.keyvalue.btree.TransactionPage;
import com.cloudata.keyvalue.btree.WriteTransaction;

public class FreeSpaceMap {

    public static final byte PAGE_TYPE = 'F';

    final RangeTree freeRanges;

    private FreeSpaceMap(int start, int end) {
        this.freeRanges = new RangeTree();
        this.freeRanges.add(start, end - start);
    }

    private FreeSpaceMap(SnapshotPage snapshot) {
        this.freeRanges = snapshot.deserialize();
    }

    public void replay(TransactionPage txn) {
        for (int i = 0; i < txn.getFreedCount(); i++) {
            int start = txn.getFreedStart(i);
            int size = txn.getFreedLength(i);

            freeRanges.add(start, size);
        }

        for (int i = 0; i < txn.getAllocatedCount(); i++) {
            int start = txn.getAllocatedStart(i);
            int size = txn.getAllocatedLength(i);

            freeRanges.remove(start, size);
        }
    }

    public static FreeSpaceMap createEmpty(int start, int end) {
        return new FreeSpaceMap(start, end);
    }

    public static FreeSpaceMap createFromSnapshot(SnapshotPage snapshot) {
        return new FreeSpaceMap(snapshot);
    }

    // public void reclaimSpace(TransactionPage t) {
    // int freelistSize = t.getFreelistSize();
    // for (int i = 0; i < freelistSize; i++) {
    // int pageNumber = t.getFreelist(i);
    // reclaimPage(pageNumber);
    // }
    // }
    //
    // @Override
    // public void reclaimPage(int pageNumber) {
    // int offset = pageNumber * ALIGNMENT;
    //
    // PageHeader header = new PageHeader(buffer, offset);
    // header.checkValid();
    //
    // int dataSize = header.getDataSize();
    // int totalSize = PageHeader.HEADER_SIZE + dataSize;
    //
    // int slots = totalSize / ALIGNMENT;
    // int padding = totalSize % ALIGNMENT;
    // if (padding != 0) {
    // slots++;
    // }
    //
    // freemap.add(pageNumber, slots);
    // }

    public class SnapshotPage extends Page {
        protected SnapshotPage(Page parent, int pageNumber, ByteBuffer buffer) {
            super(parent, pageNumber, buffer);
        }

        RangeTree deserialize() {
            return RangeTree.deserialize(buffer);
        }

        @Override
        public int getSerializedSize() {
            int size = freeRanges.getSerializedSize();

            // We need to pad the data, because we'll change the free space map when we allocate space for ourselves!
            size += 64;

            return size;
        }

        @Override
        public void write(ByteBuffer dest) {
            freeRanges.write(dest);

            // Write the padding
            while (dest.remaining() > 0) {
                dest.put((byte) 0);
            }
        }

        @Override
        public boolean walk(Transaction txn, ByteBuffer from, EntryListener listener) {
            throw new IllegalStateException();
        }

        @Override
        public void doAction(Transaction txn, KvAction action, ByteBuffer key, ByteBuffer value) {
            throw new IllegalStateException();
        }

        @Override
        public ByteBuffer getKeyLbound() {
            throw new IllegalStateException();
        }

        @Override
        public boolean isDirty() {
            return true;
        }

        @Override
        public byte getPageType() {
            return PAGE_TYPE;
        }

        @Override
        public void dump(PrintStream os) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Page> split(WriteTransaction txn) {
            throw new IllegalStateException();
        }

        @Override
        public boolean shouldSplit() {
            return false;
        }
    }

    public SpaceMapEntry writeSnapshot(PageStore pageStore) {
        ByteBuffer empty = ByteBuffer.allocate(0);
        SnapshotPage page = new SnapshotPage(null, Integer.MIN_VALUE, empty);
        return pageStore.writePage(page);
    }

    public int allocate(int size) {
        return this.freeRanges.allocate(size);
    }

}