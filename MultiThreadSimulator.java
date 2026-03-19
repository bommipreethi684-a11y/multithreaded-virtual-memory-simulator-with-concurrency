import java.util.*;

/**
 * Core simulation engine for multithreaded demand paging.
 */
public class MultiThreadSimulator {
    private List<ProcessThread> threads;
    private ThreadScheduler scheduler;
    private List<LockResource> locks;
    
    // Memory frames (shared across all threads)
    private List<FrameEntry> frames;
    private int frameCount;
    private Queue<FrameEntry> fifoQueue;
    
    // Page replacement algorithm
    private String pageReplacementAlgorithm;
    
    // Synchronization settings
    private boolean useSynchronization;
    private String synchronizationType; // "NONE", "MUTEX", "SEMAPHORE"
    
    // Simulation state
    private boolean isRunning;
    private boolean isPaused;
    private int totalSteps;
    private int currentStep;
    
    // Timeline tracking
    private List<TimelineEvent> timeline;
    
    // Deadlock detection
    private boolean deadlockDetected;
    private List<ProcessThread> deadlockedThreads;
    
    /**
     * Represents a frame entry with thread ownership.
     */
    public static class FrameEntry {
        public int pageNumber;
        public ProcessThread owner;
        
        public FrameEntry(int pageNumber, ProcessThread owner) {
            this.pageNumber = pageNumber;
            this.owner = owner;
        }
    }
    
    /**
     * Represents a timeline event.
     */
    public static class TimelineEvent {
        public int step;
        public ProcessThread thread;
        public String event; // "PAGE_ACCESS", "PAGE_FAULT", "PAGE_HIT", "CONTEXT_SWITCH", "LOCK_ACQUIRE", "LOCK_RELEASE", "BLOCKED"
        public String details;
        
        public TimelineEvent(int step, ProcessThread thread, String event, String details) {
            this.step = step;
            this.thread = thread;
            this.event = event;
            this.details = details;
        }
    }
    
    /**
     * Creates a new multithreaded simulator.
     */
    public MultiThreadSimulator(int frameCount, String pageReplacementAlgorithm) {
        this.frameCount = frameCount;
        this.pageReplacementAlgorithm = pageReplacementAlgorithm;
        this.threads = new ArrayList<>();
        this.frames = new ArrayList<>();
        this.fifoQueue = new LinkedList<>();
        this.locks = new ArrayList<>();
        this.timeline = new ArrayList<>();
        this.isRunning = false;
        this.isPaused = false;
        this.currentStep = 0;
        this.useSynchronization = false;
        this.synchronizationType = "NONE";
        this.deadlockDetected = false;
    }
    
    /**
     * Initializes the simulation.
     */
    public void initialize(List<ProcessThread> threads, ThreadScheduler scheduler, 
                          boolean useSynchronization, String syncType, int semaphorePermits) {
        this.threads = new ArrayList<>(threads);
        this.scheduler = scheduler;
        this.useSynchronization = useSynchronization;
        this.synchronizationType = syncType;
        
        // Reset all threads
        for (ProcessThread thread : this.threads) {
            thread.reset();
            scheduler.addThread(thread);
        }
        
        // Initialize locks if synchronization is enabled
        locks.clear();
        if (useSynchronization) {
            if ("MUTEX".equals(syncType)) {
                locks.add(new LockResource("Mutex-A", 1));
                locks.add(new LockResource("Mutex-B", 1));
            } else if ("SEMAPHORE".equals(syncType)) {
                locks.add(new LockResource("Semaphore-1", semaphorePermits));
            }
        }
        
        // Clear frames
        frames.clear();
        fifoQueue.clear();
        
        // Clear timeline
        timeline.clear();
        
        // Calculate total steps
        totalSteps = threads.stream().mapToInt(t -> t.getReferenceString().length).sum();
        currentStep = 0;
        
        isRunning = true;
        isPaused = false;
        deadlockDetected = false;
        deadlockedThreads = null;
    }
    
    /**
     * Executes one simulation step.
     * 
     * @return true if simulation continues, false if completed
     */
    public boolean executeStep() {
        if (!isRunning || isPaused) {
            return false;
        }
        
        // Check for deadlock
        if (useSynchronization) {
            List<ProcessThread> deadlock = DeadlockDetector.detectDeadlock(threads, locks);
            if (!deadlock.isEmpty()) {
                deadlockDetected = true;
                deadlockedThreads = deadlock;
                return false; // Stop simulation
            }
        }
        
        // Schedule next thread
        ProcessThread thread = scheduler.scheduleNext();
        
        if (thread == null) {
            // No threads ready, check if all completed
            boolean allCompleted = threads.stream().allMatch(ProcessThread::isCompleted);
            if (allCompleted) {
                isRunning = false;
                return false;
            }
            return true; // Some threads blocked, continue
        }
        
        // Try to acquire lock if synchronization is enabled
        if (useSynchronization && !locks.isEmpty()) {
            boolean lockAcquired = false;
            for (LockResource lock : locks) {
                if (!thread.getHeldLocks().contains(lock.getLockName())) {
                    lockAcquired = lock.tryAcquire(thread);
                    if (lockAcquired) {
                        addTimelineEvent(thread, "LOCK_ACQUIRE", "Acquired " + lock.getLockName());
                    } else {
                        addTimelineEvent(thread, "BLOCKED", "Waiting for " + lock.getLockName());
                        return true; // Thread blocked, continue with next
                    }
                    break;
                }
            }
        }
        
        // Get next page reference
        int page = thread.getNextPage();
        if (page == -1) {
            // Thread completed
            thread.setState(ProcessThread.State.COMPLETED);
            
            // Release all held locks
            if (useSynchronization) {
                for (String lockName : thread.getHeldLocks()) {
                    for (LockResource lock : locks) {
                        if (lock.getLockName().equals(lockName)) {
                            lock.release(thread);
                            addTimelineEvent(thread, "LOCK_RELEASE", "Released " + lockName);
                        }
                    }
                }
            }
            
            scheduler.removeThread(thread);
            return true;
        }
        
        // Check if page is in memory
        boolean hit = frames.stream().anyMatch(f -> f.pageNumber == page);
        
        if (hit) {
            // Page hit
            thread.recordPageHit();
            addTimelineEvent(thread, "PAGE_HIT", "Page " + page + " found in memory");
            
            // Update for LRU/MRU
            if ("LRU".equals(pageReplacementAlgorithm) || "MRU".equals(pageReplacementAlgorithm)) {
                frames.removeIf(f -> f.pageNumber == page && f.owner == thread);
                frames.add(new FrameEntry(page, thread));
            }
        } else {
            // Page fault
            thread.recordPageFault();
            addTimelineEvent(thread, "PAGE_FAULT", "Page " + page + " not in memory");
            
            if (frames.size() < frameCount) {
                // Empty frame available
                FrameEntry entry = new FrameEntry(page, thread);
                frames.add(entry);
                fifoQueue.add(entry);
            } else {
                // Need to evict a page
                FrameEntry evicted = selectPageToEvict(thread, page);
                frames.remove(evicted);
                
                FrameEntry newEntry = new FrameEntry(page, thread);
                frames.add(newEntry);
                fifoQueue.add(newEntry);
                
                addTimelineEvent(thread, "PAGE_EVICT", "Evicted page " + evicted.pageNumber + " from " + evicted.owner.getThreadName());
            }
        }
        
        // Advance thread
        thread.advanceIndex();
        scheduler.incrementQuantum();
        currentStep++;
        
        // Release lock after page access (if using synchronization)
        if (useSynchronization && !locks.isEmpty() && Math.random() > 0.5) {
            for (String lockName : thread.getHeldLocks()) {
                for (LockResource lock : locks) {
                    if (lock.getLockName().equals(lockName)) {
                        lock.release(thread);
                        addTimelineEvent(thread, "LOCK_RELEASE", "Released " + lockName);
                        break;
                    }
                }
                break; // Release one lock at a time
            }
        }
        
        return true;
    }
    
    /**
     * Selects a page to evict based on the replacement algorithm.
     */
    private FrameEntry selectPageToEvict(ProcessThread currentThread, int newPage) {
        switch (pageReplacementAlgorithm) {
            case "FIFO":
                return fifoQueue.poll();
                
            case "LRU":
                return frames.get(0); // Least recently used is at the front
                
            case "MRU":
                return frames.get(frames.size() - 1); // Most recently used is at the end
                
            case "OPT":
                return findOptimalEviction(currentThread, newPage);
                
            default:
                return frames.get(0);
        }
    }
    
    /**
     * Finds optimal page to evict (used farthest in future).
     */
    private FrameEntry findOptimalEviction(ProcessThread currentThread, int newPage) {
        int farthestUse = -1;
        FrameEntry toEvict = frames.get(0);
        
        for (FrameEntry entry : frames) {
            int nextUse = Integer.MAX_VALUE;
            
            // Check when this page will be used next by its owner
            ProcessThread owner = entry.owner;
            int[] refString = owner.getReferenceString();
            
            for (int i = owner.getCurrentIndex(); i < refString.length; i++) {
                if (refString[i] == entry.pageNumber) {
                    nextUse = i;
                    break;
                }
            }
            
            if (nextUse > farthestUse) {
                farthestUse = nextUse;
                toEvict = entry;
            }
        }
        
        return toEvict;
    }
    
    /**
     * Adds a timeline event.
     */
    private void addTimelineEvent(ProcessThread thread, String event, String details) {
        timeline.add(new TimelineEvent(currentStep, thread, event, details));
    }
    
    // Getters
    public List<ProcessThread> getThreads() { return threads; }
    public List<FrameEntry> getFrames() { return new ArrayList<>(frames); }
    public List<LockResource> getLocks() { return locks; }
    public List<TimelineEvent> getTimeline() { return timeline; }
    public boolean isRunning() { return isRunning; }
    public boolean isPaused() { return isPaused; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public int getCurrentStep() { return currentStep; }
    public int getTotalSteps() { return totalSteps; }
    public boolean isDeadlockDetected() { return deadlockDetected; }
    public List<ProcessThread> getDeadlockedThreads() { return deadlockedThreads; }
    public ThreadScheduler getScheduler() { return scheduler; }
}
