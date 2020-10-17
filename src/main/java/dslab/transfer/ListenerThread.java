package dslab.transfer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ListenerThread extends Thread {

    private ServerSocket serverSocket;

    public ListenerThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        Socket socket = null;

        while (true) {

            try {
                socket = serverSocket.accept();
                // Todo thread manager
                new ClientHandlerThread(socket).start();

            } catch (SocketException e) {
                // when the socket is closed, the I/O methods of the Socket will throw a SocketException
                // almost all SocketException cases indicate that the socket was closed
                // Todo close all workers here - or on thread shutdown method
                break;
            } catch (IOException e) {
                // you should properly handle all other exceptions
                throw new UncheckedIOException(e);
            }

        }
    }
}
