import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RicartAgrawala {
    private static final int BASE_PORT = 12345;
    private static final String REQUEST = "REQUEST";
    private static final String REPLY = "REPLY";
    private static final String RELEASED = "RELEASED";
    private static final String WANTED = "WANTED";
    private static final String HELD = "HELD";

    private final int pid;
    private final List<Integer> allPids;
    private final AtomicInteger clock = new AtomicInteger(0);
    private String state = RELEASED;
    private final PriorityQueue<Request> requestQueue = new PriorityQueue<>();
    private final Set<Integer> repliesReceived = ConcurrentHashMap.newKeySet();
    private Request currentRequest = null;
    private ServerSocket serverSocket;
    private final Map<Integer, Socket> clientSockets = new ConcurrentHashMap<>();
    private final Map<Integer, PrintWriter> writers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final CountDownLatch connectionLatch;
    private final Scanner scanner = new Scanner(System.in);
    private final Object csMonitor = new Object();
    private boolean inCriticalSection = false;

    public RicartAgrawala(int pid, List<Integer> allPids) {
        this.pid = pid;
        this.allPids = new ArrayList<>(allPids);
        Collections.sort(this.allPids);
        this.connectionLatch = new CountDownLatch(allPids.size() - 1);
    }

    public void start() {
        try {
            System.out.println("[" + pid + "] Starting process...");
            
            // Start server first
            serverSocket = new ServerSocket(BASE_PORT + pid);
            System.out.println("[" + pid + "] Listening on port " + (BASE_PORT + pid));
            new Thread(this::listenForMessages).start();

            // Connect to other processes with retries
            connectToAllProcesses();

            // Wait for all connections to establish
            if (!connectionLatch.await(15, TimeUnit.SECONDS)) {
                System.err.println("[" + pid + "] Failed to establish all connections");
                System.err.println("[" + pid + "] Connected to: " + clientSockets.keySet());
                return;
            }

            System.out.println("[" + pid + "] Successfully connected to all other processes");
            
            // Main control loop
            while (true) {
                System.out.println("\n[Process " + pid + "] Menu:");
                System.out.println("1. Request Critical Section");
                System.out.println("2. Exit Critical Section");
                System.out.println("3. Exit Program");
                System.out.print("Enter choice:\n");
                
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                
                switch (choice) {
                    case 1:
                        requestCriticalSection();
                        break;
                    case 2:
                        releaseCriticalSection();
                        break;
                    case 3:
                        cleanup();
                        System.exit(0);
                        break;
                    default:
                        System.out.println("[" + pid + "] Invalid choice");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void connectToAllProcesses() {
        List<Integer> processesToConnect = new ArrayList<>(allPids);
        processesToConnect.remove(Integer.valueOf(pid));
        
        for (int otherPid : processesToConnect) {
            executor.submit(() -> connectWithRetry(otherPid));
        }
    }

    private void connectWithRetry(int otherPid) {
        int maxAttempts = 20;
        int attempt = 0;
        int retryDelay = 500;
        
        while (attempt < maxAttempts) {
            try {
                Socket socket = new Socket("localhost", BASE_PORT + otherPid);
                synchronized (this) {
                    clientSockets.put(otherPid, socket);
                    writers.put(otherPid, new PrintWriter(socket.getOutputStream(), true));
                }
                System.out.println("[" + pid + "] Connected to process " + otherPid);
                connectionLatch.countDown();
                return;
            } catch (IOException e) {
                attempt++;
                try {
                    Thread.sleep(retryDelay);
                    retryDelay = Math.min(retryDelay + 200, 2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        System.err.println("[" + pid + "] Failed to connect to process " + otherPid);
    }

    private void listenForMessages() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {
            String message;
            while ((message = reader.readLine()) != null) {
                processMessage(message);
            }
        } catch (IOException e) {
            System.err.println("[" + pid + "] Error handling client: " + e.getMessage());
        }
    }

    private synchronized void processMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length < 3) return;
        
        String type = parts[0];
        int senderPid = Integer.parseInt(parts[1]);
        int timestamp = Integer.parseInt(parts[2]);

        clock.updateAndGet(curr -> Math.max(curr, timestamp) + 1);

        if (type.equals(REQUEST)) {
            System.out.println("[" + pid + "] Received REQUEST from " + 
                             senderPid + " with ts " + timestamp);
            handleRequest(senderPid, timestamp);
        } else if (type.equals(REPLY)) {
            System.out.println("[" + pid + "] Received REPLY from " + 
                             senderPid + " with ts " + timestamp);
            repliesReceived.add(senderPid);
            notifyAll(); // Notify waiting requestCriticalSection()
        }
    }
    private synchronized void requestCriticalSection() {
        if (state == HELD) {
            System.err.println("[" + pid + "] Already in critical section!");
            return;
        }
        
        System.out.println("[" + pid + "] Transitioning to WANTED state");
        state = WANTED;
        int newClock = clock.incrementAndGet();
        currentRequest = new Request(newClock, pid);
        repliesReceived.clear();

        System.out.println("[" + pid + "] Requesting CS with timestamp " + newClock);

        // Send request to all connected processes
        List<Integer> connectedPids = new ArrayList<>(clientSockets.keySet());
        if (connectedPids.isEmpty()) {
            System.err.println("[" + pid + "] No connections established - cannot request CS");
            state = RELEASED;
            return;
        }

        for (int otherPid : connectedPids) {
            sendMessage(otherPid, REQUEST, newClock);
        }

        // Wait for replies with timeout
        long startTime = System.currentTimeMillis();
        while (repliesReceived.size() < connectedPids.size()) {
            if (System.currentTimeMillis() - startTime > 50000) {
                System.err.println("[" + pid + "] Aborting due to timeout of 50 seconds");
                state = RELEASED;
                currentRequest = null;
                return;
            }
            try {
                wait(100); // Wait with timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                state = RELEASED;
                currentRequest = null;
                return;
            }
        }

        // Verify we have all replies before entering
        if (repliesReceived.size() == connectedPids.size()) {
            state = HELD;
            enterCriticalSection();
        } else {
            System.err.println("[" + pid + "] Missing replies from " + 
                             (connectedPids.size() - repliesReceived.size()) + " processes");
            state = RELEASED;
            currentRequest = null;
        }
    }

    private synchronized void handleRequest(int senderPid, int timestamp) {
        // Update clock first
        clock.updateAndGet(curr -> Math.max(curr, timestamp) + 1);

        if (state == RELEASED) {
            System.out.println("[" + pid + "] Immediately replying to " + senderPid + " (RELEASED state)");
            sendMessage(senderPid, REPLY, clock.get());
            return;
        }
        
        // Decision to defer based on timestamp and process ID
        boolean shouldDefer = (state == HELD) || 
                           (state == WANTED && 
                           (currentRequest.timestamp < timestamp || 
                           (currentRequest.timestamp == timestamp && pid < senderPid)));

        if (shouldDefer) {
            System.out.println("[" + pid + "] Deferring request from " + senderPid + 
                             " (my ts: " + currentRequest.timestamp + ")");
            requestQueue.add(new Request(timestamp, senderPid));
        } else {
            System.out.println("[" + pid + "] Immediately replying to " + senderPid);
            sendMessage(senderPid, REPLY, clock.get());
        }
    }

    private void enterCriticalSection() {
        synchronized (csMonitor) {
            inCriticalSection = true;
            System.out.println("\n========== [" + pid + "] ENTERED CRITICAL SECTION ==========");
            
            // Simulate work in critical section
            try {
                Thread.sleep(20000); // Simulated work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            releaseCriticalSection();
        }
    }
    private synchronized void releaseCriticalSection() {
        if (state != HELD) {
            System.err.println("[" + pid + "] Cannot release CS - not in HELD state");
            return;
        }
        
        System.out.println("[" + pid + "] Releasing critical section");
        System.out.println("========== [" + pid + "] EXITED CRITICAL SECTION ==========\n");
        
        state = RELEASED;
        inCriticalSection = false;
        currentRequest = null;

        // Process all deferred requests
        while (!requestQueue.isEmpty()) {
            Request deferredRequest = requestQueue.poll();
            System.out.println("[" + pid + "] Sending deferred reply to " + deferredRequest.pid);
            sendMessage(deferredRequest.pid, REPLY, clock.incrementAndGet());
        }
        
        notifyAll(); // Notify any waiting threads
    }
    private synchronized void sendMessage(int receiverPid, String type, int timestamp) {
        PrintWriter writer = writers.get(receiverPid);
        if (writer != null) {
            writer.println(type + ":" + pid + ":" + timestamp);
            System.out.println("[" + pid + "] Sent " + type + " to " + 
                            receiverPid + " with ts " + timestamp);
        } else {
            System.err.println("[" + pid + "] No active connection to process " + receiverPid);
        }
    }

    private void cleanup() {
        try {
            System.out.println("[" + pid + "] Cleaning up resources...");
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (Socket socket : clientSockets.values()) {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
            for (PrintWriter writer : writers.values()) {
                writer.close();
            }
            scanner.close();
        } catch (IOException | InterruptedException e) {
            System.err.println("[" + pid + "] Cleanup error: " + e.getMessage());
        }
    }

    private static class Request implements Comparable<Request> {
        final int timestamp;
        final int pid;

        Request(int timestamp, int pid) {
            this.timestamp = timestamp;
            this.pid = pid;
        }

        @Override
        public int compareTo(Request other) {
            if (this.timestamp != other.timestamp) {
                return Integer.compare(this.timestamp, other.timestamp);
            }
            return Integer.compare(this.pid, other.pid);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java RicartAgrawala <pid> <all_pids_comma_separated>");
            System.exit(1);
        }

        int pid = Integer.parseInt(args[0]);
        List<Integer> allPids = new ArrayList<>();
        for (String s : args[1].split(",")) {
            allPids.add(Integer.parseInt(s));
        }

        if (!allPids.contains(pid)) {
            System.err.println("Error: pid must be included in all_pids list");
            System.exit(1);
        }

        RicartAgrawala process = new RicartAgrawala(pid, allPids);
        process.start();
    }

}

