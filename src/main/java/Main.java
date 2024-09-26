import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main class
 */
public class Main {
    private static ServerSocket serverSocket;
    private static volatile boolean running = true;

    /**
     * Main method
     *
     * @param args arguments given by cmd
     */
    public static void main(String[] args) {
        startServer(6379);
    }

    public static void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            while (running) {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e);
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

    public static void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            String content;
            while ((content = reader.readLine()) != null) {
                System.out.println(content);
                if ("ping".equals(content)) {
                    writer.write("pong\r\n");
                } else {
                    writer.write(content);
                }
                writer.flush();
            }
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }
}
