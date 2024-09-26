import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Main class test
 */
public class MainTest {
    private Thread serverThread;
    @BeforeEach
    void setUp() throws Exception {
        serverThread = new Thread(() -> Main.startServer(6379));
        serverThread.start();

        Thread.sleep(500);
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

        writer.write("ping\r\n");
        writer.flush();

        String response = reader.readLine();
        assertEquals("pong", response);

        clientSocket.close();
    }
}
