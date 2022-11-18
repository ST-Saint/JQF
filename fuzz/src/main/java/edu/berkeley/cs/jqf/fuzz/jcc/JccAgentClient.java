package edu.berkeley.cs.jqf.fuzz.jcc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;

public class JccAgentClient
        implements Runnable, ISessionInfoVisitor, IExecutionDataVisitor {

    static Logger logger = LogManager.getLogger(JccAgentClient.class);

    private final Socket socket;
    private String sessionId;
    private SessionInfo sesInfo;

    public CountDownLatch okCMD = new CountDownLatch(1);

    private final RemoteControlReader reader;
    private final RemoteControlWriter writer;

    private final ExecutionDataWriter fileWriter;

    private final int maxn = 10240;

    private boolean registered = false;

    private byte[] buffer;

    private JccGuidance guidance;

    String serverAddress = "localhost";
    int serverPort = 6300;

    JccAgentClient(JccGuidance guidance) throws IOException {
        this.guidance = guidance;
        System.out.printf("Connect to %s:%d\n", serverAddress, serverPort);
        this.socket = new Socket(serverAddress, serverPort);
        System.out.printf("Connected\n");

        this.socket.setSendBufferSize(128 * 1024);
        this.socket.setReceiveBufferSize(128 * 1024);

        // Just send a valid header:
        writer = new RemoteControlWriter(socket.getOutputStream());
        reader = new RemoteControlReader(socket.getInputStream());
        reader.setSessionInfoVisitor(this);
        reader.setExecutionDataVisitor(this);
        buffer = new byte[maxn];
        fileWriter = new ExecutionDataWriter(new FileOutputStream("./jacoco.exec"));
    }

    @Override
    public void run() {
        try {
            while (reader.read()) {
                okCMD.countDown();
            }

            logger.debug("connection closed");
            socket.close();
            synchronized (fileWriter) {
                fileWriter.flush();
            }
        } catch (final IOException e) {
            e.printStackTrace();
            // client.agentHandler.remove(sessionId);
        }
    }

    public void visitSessionInfo(final SessionInfo info) {
        // System.out.println("visit session");
        // synchronized (fileWriter) {
        //         fileWriter.visitSessionInfo(info);
        // }
    }

    public void visitClassExecution(final ExecutionData data) {
        // System.out.println("visit execution");
        this.guidance.putData(data);
        // logger.info(sessionId + " get data");
        // logger.info(data.getName());
        // if (executor.agentStore.containsKey(sessionId)) {
        //     ExecutionDataStore store = executor.agentStore.get(sessionId);

        //     ExecutionData preData = store.get(data.getId());
        //     if (preData != null) {
        //	// FIXME take the maxinum value when merging data
        //	data.merge(preData, false);
        //     }
        //     store.put(data);
        //     executor.agentStore.put(sessionId, store);
        // } else {
        //     ExecutionDataStore store = new ExecutionDataStore();
        //     store.put(data);
        //     executor.agentStore.put(sessionId, store);
        // }
        // synchronized (fileWriter) {
        //         fileWriter.visitClassExecution(data);
        // }
    }

    public void collect() {
        // FIXME frequently collect null
        // System.out.println("handler collect...");
        Long ts0 = System.currentTimeMillis(), ts1;
        try {
            writer.visitDumpCommand(true, true);
        } catch (IOException e) {
            System.err.println("failed to send dump command");
            return;
        }
        ts0 = System.currentTimeMillis();
        okCMD = new CountDownLatch(1);
        // synchronized (okCMD) {
        try {
            okCMD.await(1000, TimeUnit.MILLISECONDS);
            ts1 = System.currentTimeMillis();
            System.out.println("wait " + (ts1 - ts0));
        } catch (InterruptedException e) {
            throw new GuidanceException(e);
        }
        // }
    }
}
