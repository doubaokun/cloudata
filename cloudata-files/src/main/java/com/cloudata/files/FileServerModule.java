package com.cloudata.files;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.cloudata.clients.keyvalue.KeyValueStore;
import com.cloudata.clients.keyvalue.redis.RedisKeyValueStore;
import com.cloudata.files.blobs.BlobCache;
import com.cloudata.files.blobs.BlobStore;
import com.cloudata.files.blobs.LocalBlobStore;
import com.cloudata.files.locks.InMemoryLockService;
import com.cloudata.files.locks.LockService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;

public class FileServerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LockService.class).to(InMemoryLockService.class);

        InetSocketAddress redisSocketAddress = new InetSocketAddress("localhost", 6379);
        KeyValueStore keyValueStore = new RedisKeyValueStore(redisSocketAddress);
        bind(KeyValueStore.class).toInstance(keyValueStore);

        File basePath = new File("/tmp/blobs/store");
        File cacheDir = new File("/tmp/blobs/cache");
        BlobCache cache = new BlobCache(cacheDir);
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        LocalBlobStore blobStore = new LocalBlobStore(executor, cache, basePath);
        bind(BlobStore.class).toInstance(blobStore);
    }

}
