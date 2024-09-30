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

        writer.write("*1\r\n$4\r\nPING\r\n");
        writer.write("*1\r\n$4\r\nPING\r\n");
        writer.flush();

        String response = reader.readLine();
        String response2 = reader.readLine();
        assertEquals("+PONG", response);
        assertEquals("+PONG", response2);

        clientSocket.close();
    }

    @Test
    void testPingMessageResponse() throws Exception {
        Socket clientSocket = new Socket("localhost", 6379);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        writer.write("*2\r\n$4\r\nECHO\r\n$11\r\nhello world\r\n");
        writer.flush();

        String response, response2;
        response = reader.readLine();
        response2 = reader.readLine();
        assertEquals("$11", response);
        assertEquals("hello world", response2);
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
                    writer.write("*1\r\n$4\r\nPING\r\n");
                    writer.flush();
                    return reader.readLine();
                }
            });
            results.add(result);
        }

        for (Future<String> result : results) {
            assertEquals("+PONG", result.get());
        }

        executor.shutdown();

    }

    @Test
    void testSetGet() throws Exception {
        Socket clientSocket = new Socket("localhost", 6379);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer.write("*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n");
        writer.flush();
        String response = reader.readLine();
        assertEquals("+OK", response);
        writer.write("*2\r\n$3\r\nGET\r\n$3\r\nfoo\r\n");
        writer.flush();
        reader.readLine();
        response = reader.readLine();
        assertEquals("bar", response);
    }

}
