import java.util.*;

/**
 * Implements thread scheduling algorithms: FCFS, Round-Robin, and Priority.
 */
public class ThreadScheduler {
    
    public enum SchedulingAlgorithm {
        FCFS,           // First-Come-First-Served
        ROUND_ROBIN,    // Round-Robin with time quantum
        PRIORITY        // Priority-based
    }
    
    private SchedulingAlgorithm algorithm;
    private int timeQuantum; // For Round-Robin
    private Queue<ProcessThread> readyQueue;
    private ProcessThread currentThread;
    private int currentQuantumUsed;
    private int totalContextSwitches;
    
    /**
     * Creates a new thread scheduler.
     * 
     * @param algorithm Scheduling algorithm to use
     * @param timeQuantum Time quantum for Round-Robin (in page accesses)
     */
    public ThreadScheduler(SchedulingAlgorithm algorithm, int timeQuantum) {
        this.algorithm = algorithm;
        this.timeQuantum = timeQuantum;
        this.readyQueue = new LinkedList<>();
        this.currentThread = null;
        this.currentQuantumUsed = 0;
        this.totalContextSwitches = 0;
    }
    
    /**
     * Adds a thread to the ready queue.
     */
    public void addThread(ProcessThread thread) {
        if (thread.getState() == ProcessThread.State.READY && !readyQueue.contains(thread)) {
            readyQueue.add(thread);
        }
    }
    
    /**
     * Schedules and returns the next thread to execute.
     * 
     * @return Next thread to run, or null if no threads are ready
     */
    public ProcessThread scheduleNext() {
        // Handle Round-Robin quantum expiration
        if (algorithm == SchedulingAlgorithm.ROUND_ROBIN && currentThread != null) {
            if (currentQuantumUsed >= timeQuantum && !currentThread.isCompleted()) {
                // Quantum expired, move to back of queue
                if (currentThread.getState() == ProcessThread.State.RUNNING) {
                    currentThread.setState(ProcessThread.State.READY);
                    readyQueue.add(currentThread);
                }
                performContextSwitch();
                currentThread = null;
                currentQuantumUsed = 0;
            }
        }
        
        // If current thread is still valid, continue with it
        if (currentThread != null && 
            currentThread.getState() == ProcessThread.State.RUNNING && 
            !currentThread.isCompleted()) {
            return currentThread;
        }
        
        // Select next thread based on algorithm
        ProcessThread nextThread = null;
        
        switch (algorithm) {
            case FCFS:
                nextThread = readyQueue.poll();
                break;
                
            case ROUND_ROBIN:
                nextThread = readyQueue.poll();
                break;
                
            case PRIORITY:
                nextThread = selectHighestPriority();
                break;
        }
        
        if (nextThread != null) {
            if (currentThread != null && currentThread != nextThread) {
                performContextSwitch();
            }
            currentThread = nextThread;
            currentThread.setState(ProcessThread.State.RUNNING);
            currentThread.setLastScheduledTime(System.currentTimeMillis());
            currentQuantumUsed = 0;
        }
        
        return currentThread;
    }
    
    /**
     * Selects the thread with highest priority from ready queue.
     */
    private ProcessThread selectHighestPriority() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        ProcessThread highest = null;
        for (ProcessThread thread : readyQueue) {
            if (highest == null || thread.getPriority() > highest.getPriority()) {
                highest = thread;
            }
        }
        
        if (highest != null) {
            readyQueue.remove(highest);
        }
        
        return highest;
    }
    
    /**
     * Increments the quantum usage.
     */
    public void incrementQuantum() {
        currentQuantumUsed++;
    }
    
    /**
     * Performs a context switch.
     */
    private void performContextSwitch() {
        totalContextSwitches++;
        if (currentThread != null) {
            currentThread.recordContextSwitch();
        }
    }
    
    /**
     * Removes a thread from the ready queue.
     */
    public void removeThread(ProcessThread thread) {
        readyQueue.remove(thread);
        if (currentThread == thread) {
            currentThread = null;
            currentQuantumUsed = 0;
        }
    }
    
    /**
     * Checks if there are threads ready to execute.
     */
    public boolean hasReadyThreads() {
        return !readyQueue.isEmpty() || (currentThread != null && !currentThread.isCompleted());
    }
    
    /**
     * Gets all threads currently in ready queue.
     */
    public List<ProcessThread> getReadyThreads() {
        return new ArrayList<>(readyQueue);
    }
    
    /**
     * Resets the scheduler.
     */
    public void reset() {
        readyQueue.clear();
        currentThread = null;
        currentQuantumUsed = 0;
        totalContextSwitches = 0;
    }
    
    // Getters and setters
    public SchedulingAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(SchedulingAlgorithm algorithm) { this.algorithm = algorithm; }
    public int getTimeQuantum() { return timeQuantum; }
    public void setTimeQuantum(int timeQuantum) { this.timeQuantum = timeQuantum; }
    public ProcessThread getCurrentThread() { return currentThread; }
    public int getCurrentQuantumUsed() { return currentQuantumUsed; }
    public int getTotalContextSwitches() { return totalContextSwitches; }
    public int getReadyQueueSize() { return readyQueue.size(); }
}
