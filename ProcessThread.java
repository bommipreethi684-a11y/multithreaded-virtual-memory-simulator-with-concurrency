import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a process/thread in the multithreading simulation.
 * Each thread has its own reference string, priority, and state.
 */
public class ProcessThread {
    private static final AtomicInteger idGenerator = new AtomicInteger(1);
    
    private final int threadId;
    private final String threadName;
    private int[] referenceString;
    private int priority;
    private int currentIndex;
    
    // Thread state
    public enum State {
        READY, RUNNING, WAITING, BLOCKED, COMPLETED
    }
    
    private State state;
    
    // Statistics
    private int pageFaults;
    private int pageHits;
    private long waitingTime;
    private long arrivalTime;
    private long completionTime;
    private long lastScheduledTime;
    private int contextSwitches;
    
    // Lock information
    private Set<String> heldLocks;
    private String waitingForLock;
    
    /**
     * Creates a new process thread.
     * 
     * @param referenceString Array of page references
     * @param priority Thread priority (higher = more priority)
     */
    public ProcessThread(int[] referenceString, int priority) {
        this.threadId = idGenerator.getAndIncrement();
        this.threadName = "T" + threadId;
        this.referenceString = referenceString;
        this.priority = priority;
        this.currentIndex = 0;
        this.state = State.READY;
        this.pageFaults = 0;
        this.pageHits = 0;
        this.waitingTime = 0;
        this.arrivalTime = System.currentTimeMillis();
        this.heldLocks = new HashSet<>();
        this.waitingForLock = null;
    }
    
    /**
     * Gets the next page reference for this thread.
     */
    public int getNextPage() {
        if (currentIndex < referenceString.length) {
            return referenceString[currentIndex];
        }
        return -1; // No more pages
    }
    
    /**
     * Advances to the next page reference.
     */
    public void advanceIndex() {
        currentIndex++;
        if (currentIndex >= referenceString.length) {
            state = State.COMPLETED;
            completionTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Checks if thread has completed execution.
     */
    public boolean isCompleted() {
        return currentIndex >= referenceString.length || state == State.COMPLETED;
    }
    
    /**
     * Increments page fault count.
     */
    public void recordPageFault() {
        pageFaults++;
    }
    
    /**
     * Increments page hit count.
     */
    public void recordPageHit() {
        pageHits++;
    }
    
    /**
     * Records context switch.
     */
    public void recordContextSwitch() {
        contextSwitches++;
    }
    
    /**
     * Adds waiting time.
     */
    public void addWaitingTime(long time) {
        waitingTime += time;
    }
    
    /**
     * Acquires a lock.
     */
    public void acquireLock(String lockName) {
        heldLocks.add(lockName);
        waitingForLock = null;
    }
    
    /**
     * Releases a lock.
     */
    public void releaseLock(String lockName) {
        heldLocks.remove(lockName);
    }
    
    /**
     * Sets the lock this thread is waiting for.
     */
    public void setWaitingForLock(String lockName) {
        this.waitingForLock = lockName;
    }
    
    // Getters and setters
    public int getThreadId() { return threadId; }
    public String getThreadName() { return threadName; }
    public int[] getReferenceString() { return referenceString; }
    public void setReferenceString(int[] referenceString) { 
        this.referenceString = referenceString;
        this.currentIndex = 0;
    }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getCurrentIndex() { return currentIndex; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public int getPageFaults() { return pageFaults; }
    public int getPageHits() { return pageHits; }
    public long getWaitingTime() { return waitingTime; }
    public long getTurnaroundTime() { 
        if (completionTime > 0) {
            return completionTime - arrivalTime;
        }
        return 0;
    }
    public int getContextSwitches() { return contextSwitches; }
    public Set<String> getHeldLocks() { return new HashSet<>(heldLocks); }
    public String getWaitingForLock() { return waitingForLock; }
    public long getLastScheduledTime() { return lastScheduledTime; }
    public void setLastScheduledTime(long time) { this.lastScheduledTime = time; }
    
    /**
     * Resets the thread to initial state.
     */
    public void reset() {
        currentIndex = 0;
        state = State.READY;
        pageFaults = 0;
        pageHits = 0;
        waitingTime = 0;
        contextSwitches = 0;
        arrivalTime = System.currentTimeMillis();
        completionTime = 0;
        heldLocks.clear();
        waitingForLock = null;
    }
    
    @Override
    public String toString() {
        return threadName + " [Priority: " + priority + ", State: " + state + "]";
    }
}
