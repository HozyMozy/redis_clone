import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    private static ExecutorService threadPool = Executors.newCachedThreadPool();// Use a thread pool to manage client threads
    private static ConcurrentHashMap<String, String> setStore = new ConcurrentHashMap<>();

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
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        ) {
            List<Object> commands = null;
            String message;
            while (running && (((commands = parserCommand(reader))) != null)) {
                String command = (String) commands.get(0);
                System.out.println("Received command: " + command);
                switch (command.toLowerCase()) {
                    case "ping":
                        writer.write("+PONG\r\n");
                        break;
                    case "echo":
                        writer.write(String.join("\r\n", commands.stream().skip(1).toArray(String[]::new))
                                + "\r\n");
                        break;
                    case "set":
                        writer.write("+OK\r\n");
                        String key = (String) commands.get(2);
                        setStore.put(key, String.join("\r\n", commands.stream().skip(3).toArray(String[]::new))
                                + "\r\n");
                        break;
                    case "get":
                        key = (String) commands.get(2);
                        writer.write(setStore.getOrDefault(key, "$-1\r\n"));
                        break;
                    default:
                        return;

                }
                writer.flush();
            }

            /*
            while (running && !clientSocket.isClosed() && (message = reader.readLine()) != null) {
                System.out.println("Received message from " + clientSocket.getRemoteSocketAddress() + ": " + message);

                // Respond to 'ping' command
                if (message.startsWith("PING")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        writer.write("$" + parts[1].length() + "\r\n" + parts[1] + "\r\n");
                    } else {
                        writer.write("+PONG\r\n");
                    }
                } else {
                    writer.write("+UNKNOWN\r\n");
                }

                writer.flush(); // Ensure the message is sent to the client
            }
             */
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        } finally {
            try {
                System.out.println("Closing connection for " + clientSocket.getRemoteSocketAddress());
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

    public static List<Object> parserCommand(BufferedReader in) {
        List<Object> ret = new ArrayList<>();
        try {
            String line1 = in.readLine();
            if (line1 == null) {
                ret.add("exit");
                return ret;
            }
            if (line1.charAt(0) != '*') {
                throw new RuntimeException("ERR command must be an array ");
            }
            int nEle = Integer.parseInt(line1.substring(1));
            System.out.println("Skipped: " + in.readLine());          // skip len - 2nd line
            ret.add(in.readLine()); // read command - 3rd line
            System.out.println("Received command " + ret.get(0) +
                    ", number of element " + nEle);
            // read data
            String line = null;
            for (int i = 1; i < nEle && (line = in.readLine()) != null; i++) {
                if (line.isEmpty())
                    continue;
                char type = line.charAt(0);
                switch (type) {
                    case '$':
                        System.out.println("parse bulk string: " + line);
                        ret.add(line);
                        ret.add(in.readLine());
                        break;
                    case ':':
                        System.out.println("parse int: " + line);
                        ret.add(String.valueOf(type));
                        ret.add(Integer.parseInt(line.substring(1)));
                        break;
                    default:
                        System.out.println("default: " + line);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Parse failed " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Parse failed " + e.getMessage());
        }
        System.out.println("Command: " +
                String.join(" ", ret.stream().toArray(String[]::new)));
        return ret;
    }
}
