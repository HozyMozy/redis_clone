import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.net.ServerSocket;

/**
 * Main class
 */
public class Main {
    private static ServerSocket serverSocket;
    private static volatile boolean running = true;
    private static final int PORT = 6379;
    private static final int BUFFER_SIZE = 1024;

    /**
     * Uses selector to manage concurrent clients, blocking is set to false, event loop handles input
     *
     * @param args arguments given by cmd
     */
    public static void main(String[] args) {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isAcceptable()) {
                        handleAccept(serverSocketChannel, selector);
                    }

                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }


    private static void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from " + socketChannel.getRemoteAddress());
    }

    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            socketChannel.close();
            System.out.println("Connection closed by client");
            return;
        }

        String message = new String(buffer.array()).trim();
        System.out.println("Received message: " + message);
        if ("ping".equalsIgnoreCase(message)) {
            ByteBuffer writeBuffer = ByteBuffer.wrap("pong".getBytes());
            socketChannel.write(writeBuffer);
        } else {
            ByteBuffer readBuffer = ByteBuffer.wrap((message + "\r\n").getBytes());
            socketChannel.read(readBuffer);
        }
    }

    public static void stopServer() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("IOException: " + e);
            }
        }
    }

}
