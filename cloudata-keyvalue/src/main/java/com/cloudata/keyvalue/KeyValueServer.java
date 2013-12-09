package com.cloudata.keyvalue;

import java.io.File;
import java.util.List;

import org.robotninjas.barge.RaftService;
import org.robotninjas.barge.Replica;

import com.cloudata.keyvalue.web.WebModule;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.guice.spi.container.GuiceComponentProviderFactory;

public class KeyValueServer {

    final File baseDir;
    final int httpPort;
    private final Replica local;
    private final List<Replica> peers;
    private RaftService raft;
    private SelectorThread selector;

    public KeyValueServer(File baseDir, Replica local, List<Replica> peers, int httpPort) {
        super();
        this.baseDir = baseDir;
        this.local = local;
        this.peers = peers;
        this.httpPort = httpPort;
    }

    public synchronized void start() throws Exception {
        if (raft != null || selector != null) {
            throw new IllegalStateException();
        }

        File logDir = new File(baseDir, "logs");
        File stateDir = new File(baseDir, "state");

        logDir.mkdirs();
        stateDir.mkdirs();

        KeyValueStateMachine stateMachine = new KeyValueStateMachine();

        this.raft = RaftService.newBuilder().local(local).members(peers).logDir(logDir).timeout(300)
                .build(stateMachine);

        stateMachine.init(raft, stateDir);

        raft.startAsync().awaitRunning();

        final String baseUri = getHttpUrl();

        Injector injector = Guice.createInjector(new KeyValueModule(stateMachine), new WebModule());

        ResourceConfig rc = new PackagesResourceConfig(WebModule.class.getPackage().getName());
        IoCComponentProviderFactory ioc = new GuiceComponentProviderFactory(rc, injector);

        this.selector = GrizzlyServerFactory.create(baseUri, rc, ioc);
    }

    public String getHttpUrl() {
        return "http://localhost:" + httpPort + "/";
    }

    public static void main(String... args) throws Exception {
        final int port = Integer.parseInt(args[0]);

        Replica local = Replica.fromString("localhost:" + (10000 + port));
        List<Replica> members = Lists.newArrayList(Replica.fromString("localhost:10001"),
                Replica.fromString("localhost:10002"), Replica.fromString("localhost:10003"));
        members.remove(local);

        File baseDir = new File(args[0]);
        int httpPort = (9990 + port);
        final KeyValueServer server = new KeyValueServer(baseDir, local, members, httpPort);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });
    }

    public synchronized void stop() {
        if (selector != null) {
            selector.stopEndpoint();
            selector = null;
        }

        if (raft != null) {
            raft.stopAsync().awaitTerminated();
            raft = null;
        }
    }

}
