package dslab.transfer;

import dslab.protocols.dmtp.Email;
import dslab.util.Config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DMTPListenerThread extends Thread {

    public static final int N_CONSUMERS = 3;
    public static final int N_PRODUCERS = 2;

    private final ServerSocket serverSocket;
    private final Config domainsConfig;
    private final Config transferConfig;
    private final ExecutorService producersExecutorService;
    private final ExecutorService consumersExecutorService;
    private final ArrayBlockingQueue<Email> blockingQueue;

    public DMTPListenerThread(ServerSocket serverSocket, Config config) {
        this.serverSocket = serverSocket;

        // Unlimited size thread pool vs limited size: this is an heuristic. Risk is thread starvation
        this.producersExecutorService = Executors.newFixedThreadPool(N_PRODUCERS);
        this.consumersExecutorService = Executors.newFixedThreadPool(N_CONSUMERS);

        this.domainsConfig = new Config("domains");
        this.transferConfig = config;

        // Alternative is an unbounded LinkedBlockingQueue, but if producers produce more than consumers and the server
        // crashes, all the emails in the queue would be lost. This way, if capacity is reached, I can show an error
        // to the client and notify him that the email couldn't be sent.
        this.blockingQueue = new ArrayBlockingQueue<>(40);

    }

    @Override
    public void run() {

        for (int i = 0; i < N_CONSUMERS; i++)
            consumersExecutorService.submit(new EmailConsumer(domainsConfig, blockingQueue));

        Socket socket = null;

        while (!Thread.currentThread().isInterrupted()) {

            try {
                socket = serverSocket.accept();

                Runnable clientEmailProducer = new EmailProducer(transferConfig, socket, blockingQueue);
                producersExecutorService.submit(clientEmailProducer);

            } catch (SocketException e) {
                // exit loop
                break;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // Close open running services
        producersExecutorService.shutdownNow();
        consumersExecutorService.shutdownNow();

    }

}
