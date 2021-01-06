package dslab.mailbox;

import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DMAPListenerThread extends Thread {

    private static final int N_LISTENERS = 10;

    private final ServerSocket serverSocket;
    private final Config config;
    private final Config userConfig;
    private final String domain;

    private String componentId;

    ExecutorService executorService;

    public DMAPListenerThread(Config config, ServerSocket serverSocket, String componentId) {
        this.config = config;
        this.componentId = componentId;
        this.serverSocket = serverSocket;

        this.userConfig = new Config(config.getString("users.config"));
        this.domain = config.getString("domain");

        this.executorService = Executors.newFixedThreadPool(N_LISTENERS);
    }

    @Override
    public void run() {
        Socket socket = null;

        while (!Thread.currentThread().isInterrupted()) {

            try {
                socket = serverSocket.accept();
                Runnable clientHandler = new ClientHandler(userConfig, socket, componentId);
                executorService.submit(clientHandler);

            } catch (SocketException e) {
                // exit loop
                break;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        }

        executorService.shutdownNow();
    }
}
