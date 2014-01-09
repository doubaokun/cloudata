package com.cloudata.btree;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.cloudata.btree.io.BackingFile;
import com.cloudata.btree.io.CipherSpec;
import com.cloudata.btree.io.EncryptedBackingFile;
import com.cloudata.btree.io.NioBackingFile;

public class Database implements Closeable {
    final PageStore pageStore;
    final TransactionTracker transactionTracker;

    private Database(PageStore pageStore) throws IOException {
        this.pageStore = pageStore;

        MasterPage latest = pageStore.findLatestMasterPage();
        this.transactionTracker = new TransactionTracker(this, latest);
    }

    public static Database build(File file, byte[] keyBytes) throws IOException {
        CipherSpec cipherSpec = CipherSpec.AES_128;

        if (!file.exists()) {
            long size = 1024L * 1024L * 64L;
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.setLength(size);
            }

            BackingFile backingFile = new NioBackingFile(file);
            if (keyBytes != null) {
                backingFile = new EncryptedBackingFile(backingFile, cipherSpec, keyBytes);
            }

            CachingPageStore.createNew(backingFile);
            backingFile.close();
        }

        {
            BackingFile backingFile = new NioBackingFile(file);
            if (keyBytes != null) {
                backingFile = new EncryptedBackingFile(backingFile, cipherSpec, keyBytes);
            }

            PageStore pageStore = CachingPageStore.open(backingFile);
            return new Database(pageStore);
        }
    }

    public PageStore getPageStore() {
        return pageStore;
    }

    @Override
    public void close() throws IOException {
        pageStore.close();
        transactionTracker.close();
    }
}
