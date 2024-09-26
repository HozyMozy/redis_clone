import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Main class test
 */
public class MainTest {
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        serverThread = new Thread(() -> Main.main(new String[]{}));
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        Main.stopServer();
        serverThread.join();
    }

    @Test
    void testPingResponse() throws Exception {
        Socket clientSocket = new Socket("localhost", 6379);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        writer.write("ping\n");
        writer.flush();

        String response = reader.readLine();
        assertEquals("pong", response);

        clientSocket.close();
    }

    @Test
    void testEchoResponse() throws Exception {
        System.out.println("Testing echo response");
        Socket clientSocket = new Socket("localhost", 6379);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        writer.write("echo\n");
        writer.flush();

        System.out.println("Sending echo ping");
        String response;
        response = reader.readLine();
        System.out.println(response);
        assertEquals("echo", response);
        clientSocket.close();
    }

    @Test
    void testConcurrentClientHandling() throws Exception {
        int numberOfClients = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < numberOfClients; i++) {
            Future<String> result = executor.submit(() -> {
                try (Socket clientSocket = new Socket("localhost", 6379);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                BufferedReader reader = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()))) {
                    writer.write("ping\r\n");
                    writer.flush();
                    return reader.readLine();
                }
            });
            results.add(result);
        }

        for (Future<String> result : results) {
            assertEquals("pong", result.get());
        }

        executor.shutdown();

    }
}
