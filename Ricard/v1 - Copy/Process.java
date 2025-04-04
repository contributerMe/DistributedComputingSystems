
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Process {
    private int id;
    private LogicalClock clock;
    private ProcessState state;
    private List<Process> otherProcesses;
    private Queue<Integer> deferredReplies;
    private int outstandingReplies;
    private int requestTimestamp; // Store the timestamp of our request
    
    public Process(int id) {
        this.id = id;
        this.clock = new LogicalClock();
        this.state = ProcessState.RELEASED;
        this.otherProcesses = new LinkedList<>();
        this.deferredReplies = new LinkedList<>();
        this.outstandingReplies = 0;
        this.requestTimestamp = 0;
    }
    
    public void addProcess(Process process) {
        if (process.getId() != this.id) {
            otherProcesses.add(process);
        }
    }
    
    public int getId() {
        return id;
    }
    
    public synchronized void requestCriticalSection() {
        state = ProcessState.WANTED;
        requestTimestamp = clock.increment();
        outstandingReplies = otherProcesses.size();
        
        System.out.println("Process " + id + " requesting critical section with timestamp " + requestTimestamp);
        
        if (outstandingReplies == 0) {
            enterCriticalSection();
            return;
        }
        
        // Send request to all other processes
        for (Process process : otherProcesses) {
            process.receiveRequest(new Message(Message.Type.REQUEST, requestTimestamp, id));
        }
    }
    
    public synchronized void receiveRequest(Message message) {
        // Update our logical clock
        clock.update(message.getTimestamp());
        
        // Case 1: I'm in the critical section - always defer
        if (state == ProcessState.HELD) {
            deferredReplies.add(message.getSiteId());
            System.out.println("Process " + id + " deferred reply to process " + message.getSiteId() + " (in CS)");
            return;
        }
        
        // Case 2: I want to enter the critical section - compare priorities
        if (state == ProcessState.WANTED) {
            // Compare timestamps first
            if (requestTimestamp < message.getTimestamp()) {
                // My timestamp is lower, I have higher priority - defer reply
                deferredReplies.add(message.getSiteId());
                System.out.println("Process " + id + " deferred reply to process " + message.getSiteId() + " (lower timestamp)");
                return;
            }
            
            // If timestamps are equal, use process IDs as tiebreaker
            if (requestTimestamp == message.getTimestamp() && id < message.getSiteId()) {
                // My ID is lower, I have higher priority - defer reply
                deferredReplies.add(message.getSiteId());
                System.out.println("Process " + id + " deferred reply to process " + message.getSiteId() + " (equal timestamp, lower ID)");
                return;
            }
        }
        
        // In all other cases, send reply immediately
        sendReply(message.getSiteId());
    }
    
    private void sendReply(int processId) {
        System.out.println("Process " + id + " sending reply to process " + processId);
        for (Process process : otherProcesses) {
            if (process.getId() == processId) {
                process.receiveReply(new Message(Message.Type.REPLY, clock.getValue(), id));
                break;
            }
        }
    }
    
    public synchronized void receiveReply(Message message) {
        clock.update(message.getTimestamp());
        outstandingReplies--;
        
        System.out.println("Process " + id + " received reply from process " + message.getSiteId() + 
                          ", outstanding replies: " + outstandingReplies);
        
        if (outstandingReplies == 0 && state == ProcessState.WANTED) {
            enterCriticalSection();
        }
    }
    
    private void enterCriticalSection() {
        state = ProcessState.HELD;
        System.out.println("Process " + id + " entered critical section");
        
        // Simulate some work in the critical section
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        exitCriticalSection();
    }
    
    private void exitCriticalSection() {
        state = ProcessState.RELEASED;
        System.out.println("Process " + id + " exited critical section");
        
        // Send all deferred replies
        while (!deferredReplies.isEmpty()) {
            int deferredId = deferredReplies.poll();
            sendReply(deferredId);
        }
    }
}
