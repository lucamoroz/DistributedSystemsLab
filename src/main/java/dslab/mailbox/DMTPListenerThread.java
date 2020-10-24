package dslab.mailbox;

import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DMTPListenerThread extends Thread {

    private static final int N_LISTENERS = 10;

    private final ServerSocket serverSocket;
    private final Config config;
    private final Config userConfig;
    private final String domain;

    ExecutorService listenersExecutorService;

    public DMTPListenerThread(Config config, ServerSocket serverSocket) {
        this.config = config;
        this.serverSocket = serverSocket;

        this.userConfig = new Config(config.getString("users.config"));
        this.domain = config.getString("domain");

        this.listenersExecutorService = Executors.newFixedThreadPool(N_LISTENERS);
    }

    @Override
    public void run() {
        Socket socket = null;

        while (!Thread.currentThread().isInterrupted()) {

            try {
                socket = serverSocket.accept();

                Runnable emailReceiver = new EmailReceiver(userConfig, domain, socket);
                listenersExecutorService.submit(emailReceiver);

            } catch (SocketException e) {
                // exit loop
                break;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        }

        listenersExecutorService.shutdownNow();
    }
}
