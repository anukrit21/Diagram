// Compile: javac Berkeley.java
// Run:     java Berkeley

import java.io.*;                  // For ObjectInput/Output streams
import java.net.*;                 // For Socket programming
import java.util.*;                // For Date, Calendar, List

public class Berkeley {

    // Port number where server will listen
    private static final int PORT = 9876;

    // Number of client threads to simulate
    private static final int NUM_CLIENTS = 3;

    // A thread-safe list to store time differences from clients
    private static final List<Long> timeDiffs = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {

        // Create a server socket to accept client connections
        ServerSocket serverSocket = new ServerSocket(PORT);

        // Thread to act as time server (accepting connections from clients)
        Thread timeServerThread = new Thread(() -> {
            while (true) {
                try (
                    // Accept a connection from a client
                    Socket clientSocket = serverSocket.accept();

                    // Set up object streams to read/write Date objects
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ) {

                    // Read client's time
                    Date clientTime = (Date) in.readObject();

                    // Send current server time to client
                    out.writeObject(new Date());

                    // Calculate time difference: server - client
                    long timeDiff = new Date().getTime() - clientTime.getTime();

                    // Store the time difference in the list
                    timeDiffs.add(timeDiff);

                } catch (Exception e) {
                    e.printStackTrace();  // Print any error
                }
            }
        });

        // Start the server thread
        timeServerThread.start();

        // Start multiple client threads
        for (int i = 0; i < NUM_CLIENTS; i++) {
            Thread clientThread = new Thread(new TimeClient(i));
            clientThread.start();
        }

        // Wait for 5 seconds to let all clients communicate with the server
        Thread.sleep(5000);

        // Calculate the average time difference
        long sumTimeDiff = 0;
        for (Long timeDiff : timeDiffs) {
            sumTimeDiff += timeDiff;
        }

        // Avoid divide-by-zero
        long avgTimeDiff = timeDiffs.size() > 0 ? sumTimeDiff / timeDiffs.size() : 0;

        // Print the average time difference
        System.out.println("Average time difference: " + avgTimeDiff + " ms");

        // Adjust server's clock based on average difference
        adjustClock(avgTimeDiff);
    }

    // Adjust the server's system clock (simulated)
    private static void adjustClock(long avgTimeDiff) {
        Calendar calendar = Calendar.getInstance();   // Get current calendar time
        calendar.setTime(new Date());                 // Set it to current time
        calendar.add(Calendar.MILLISECOND, (int) avgTimeDiff); // Add avg difference
        System.out.println("Adjusted time: " + calendar.getTime()); // Print new time
    }

    // Inner class to simulate a time client
    static class TimeClient implements Runnable {
        private final int clientId;

        public TimeClient(int clientId) {
            this.clientId = clientId;  // Assign client ID
        }

        public void run() {
            while (true) {
                try (
                    // Connect to the server
                    Socket socket = new Socket("localhost", PORT);

                    // Set up object streams for communication
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ) {

                    // Get client's current time
                    Date myTime = new Date();

                    // Send client's time to server
                    out.writeObject(myTime);

                    // Read server's time sent back
                    Date serverTime = (Date) in.readObject();

                    // Calculate time difference: server - client (after sending)
                    long timeDiff = serverTime.getTime() - new Date().getTime();

                    // Add it to the shared list
                    timeDiffs.add(timeDiff);

                    // Sleep for 1 second before retrying
                    Thread.sleep(1000);

                } catch (Exception e) {
                    e.printStackTrace();  // Print any error
                }
            }
        }
    }
}
/*The Berkeley Algorithm is used to synchronize clocks across multiple machines in a network. It works like this:

One machine acts as a master (time server).

It collects the current time from other machines (clients).

It calculates the average time difference.

Then, it adjusts its own clock and sends time correction to clients so everyone is synchronized.

Command:javac *.java 
java filename*/