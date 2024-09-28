import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class
 */
public class Main {
    private static ServerSocket serverSocket;
    private static volatile boolean running = true;
    private static final int PORT = 6379;
    private static final int BUFFER_SIZE = 1024;
    private static ExecutorService threadPool = Executors.newCachedThreadPool(); // Use a thread pool to manage client threads

    /**
     * Uses blocking threads to manage concurrent clients
     *
     * @param args arguments given by cmd
     */
    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            // Keep the server running
            while (running) {
                Socket clientSocket = serverSocket.accept(); // Blocking call
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());

                // Handle the client connection in a new thread
                threadPool.submit(() -> handleClient(clientSocket));
            }

        } catch (IOException e) {
            System.out.println("IOException: " + e);
        } finally {
            stopServer();
        }
    }

    /**
     * Handles the communication with the client.
     *
     * @param clientSocket the connected client socket
     */
    private static void handleClient(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            while (running && !clientSocket.isClosed()) {
                // Read data from the client
                int bytesRead = inputStream.read(buffer.array());

                if (bytesRead == -1) {
                    System.out.println("Connection closed by client: " + clientSocket.getRemoteSocketAddress());
                    break;
                }

                String message = new String(buffer.array(), 0, bytesRead).trim();
                System.out.println("Received message from " + clientSocket.getRemoteSocketAddress() + ": " + message);

                // Respond to 'ping' command
                if ("ping".equalsIgnoreCase(message)) {
                    outputStream.write("pong\r\n".getBytes());
                    outputStream.flush();
                } else {
                    outputStream.write((message + "\r\n").getBytes());
                    outputStream.flush();
                }

                buffer.clear(); // Clear the buffer for the next read
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e);
            }
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

        // Gracefully shutdown the thread pool
        threadPool.shutdown();
    }
}
