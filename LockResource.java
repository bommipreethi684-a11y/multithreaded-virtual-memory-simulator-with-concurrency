import java.util.LinkedList;
import java.util.Queue;

/**
 * Represents a synchronization lock resource (Mutex or Semaphore).
 */
public class LockResource {
    private final String lockName;
    private final int maxPermits; // 1 for mutex, N for semaphore
    private int availablePermits;
    private ProcessThread currentHolder; // For mutex
    private Queue<ProcessThread> waitingQueue;
    private int acquisitionCount;
    
    /**
     * Creates a new lock resource.
     * 
     * @param lockName Name of the lock
     * @param maxPermits Maximum permits (1 for Mutex, N for Semaphore)
     */
    public LockResource(String lockName, int maxPermits) {
        this.lockName = lockName;
        this.maxPermits = maxPermits;
        this.availablePermits = maxPermits;
        this.waitingQueue = new LinkedList<>();
        this.acquisitionCount = 0;
    }
    
    /**
     * Attempts to acquire the lock.
     * 
     * @param thread Thread trying to acquire the lock
     * @return true if lock acquired, false if thread must wait
     */
    public synchronized boolean tryAcquire(ProcessThread thread) {
        if (availablePermits > 0) {
            availablePermits--;
            if (maxPermits == 1) { // Mutex
                currentHolder = thread;
            }
            thread.acquireLock(lockName);
            acquisitionCount++;
            return true;
        } else {
            if (!waitingQueue.contains(thread)) {
                waitingQueue.add(thread);
                thread.setWaitingForLock(lockName);
                thread.setState(ProcessThread.State.BLOCKED);
            }
            return false;
        }
    }
    
    /**
     * Releases the lock.
     * 
     * @param thread Thread releasing the lock
     */
    public synchronized void release(ProcessThread thread) {
        if (maxPermits == 1 && currentHolder == thread) {
            currentHolder = null;
        }
        
        availablePermits++;
        thread.releaseLock(lockName);
        
        // Wake up next waiting thread
        if (!waitingQueue.isEmpty()) {
            ProcessThread next = waitingQueue.poll();
            if (next != null && !next.isCompleted()) {
                availablePermits--;
                if (maxPermits == 1) {
                    currentHolder = next;
                }
                next.acquireLock(lockName);
                next.setState(ProcessThread.State.READY);
            }
        }
    }
    
    /**
     * Forces release of the lock (for deadlock resolution).
     */
    public synchronized void forceRelease() {
        availablePermits = maxPermits;
        currentHolder = null;
        
        // Wake all waiting threads
        while (!waitingQueue.isEmpty()) {
            ProcessThread thread = waitingQueue.poll();
            if (thread != null) {
                thread.setWaitingForLock(null);
                thread.setState(ProcessThread.State.READY);
            }
        }
    }
    
    /**
     * Checks if lock is available.
     */
    public synchronized boolean isAvailable() {
        return availablePermits > 0;
    }
    
    /**
     * Gets the current holder (for mutex).
     */
    public synchronized ProcessThread getCurrentHolder() {
        return currentHolder;
    }
    
    /**
     * Gets the number of threads waiting.
     */
    public synchronized int getWaitingCount() {
        return waitingQueue.size();
    }
    
    /**
     * Gets waiting threads.
     */
    public synchronized Queue<ProcessThread> getWaitingQueue() {
        return new LinkedList<>(waitingQueue);
    }
    
    // Getters
    public String getLockName() { return lockName; }
    public int getMaxPermits() { return maxPermits; }
    public int getAvailablePermits() { return availablePermits; }
    public int getAcquisitionCount() { return acquisitionCount; }
    public boolean isMutex() { return maxPermits == 1; }
    
    @Override
    public String toString() {
        String type = isMutex() ? "Mutex" : "Semaphore";
        return lockName + " (" + type + ") - Available: " + availablePermits + "/" + maxPermits;
    }
}
