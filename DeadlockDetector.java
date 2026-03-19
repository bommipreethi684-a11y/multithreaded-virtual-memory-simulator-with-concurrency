import java.util.*;

/**
 * Detects deadlocks using resource allocation graph cycle detection.
 */
public class DeadlockDetector {
    
    /**
     * Detects if there is a deadlock among the given threads and locks.
     * 
     * @param threads List of threads
     * @param locks List of lock resources
     * @return List of threads involved in deadlock, empty if no deadlock
     */
    public static List<ProcessThread> detectDeadlock(List<ProcessThread> threads, List<LockResource> locks) {
        // Build resource allocation graph
        Map<ProcessThread, Set<String>> threadToLocks = new HashMap<>();
        Map<String, ProcessThread> lockToThread = new HashMap<>();
        
        // Build the graph
        for (ProcessThread thread : threads) {
            if (thread.getState() == ProcessThread.State.BLOCKED) {
                threadToLocks.put(thread, thread.getHeldLocks());
                
                String waitingFor = thread.getWaitingForLock();
                if (waitingFor != null) {
                    // Find who holds this lock
                    for (ProcessThread t : threads) {
                        if (t.getHeldLocks().contains(waitingFor)) {
                            lockToThread.put(waitingFor, t);
                            break;
                        }
                    }
                }
            }
        }
        
        // Detect cycle using DFS
        Set<ProcessThread> visited = new HashSet<>();
        Set<ProcessThread> recStack = new HashSet<>();
        List<ProcessThread> deadlockedThreads = new ArrayList<>();
        
        for (ProcessThread thread : threads) {
            if (thread.getState() == ProcessThread.State.BLOCKED) {
                if (hasCycle(thread, threadToLocks, lockToThread, visited, recStack, deadlockedThreads)) {
                    return deadlockedThreads;
                }
            }
        }
        
        return new ArrayList<>(); // No deadlock
    }
    
    /**
     * DFS-based cycle detection.
     */
    private static boolean hasCycle(ProcessThread thread,
                                    Map<ProcessThread, Set<String>> threadToLocks,
                                    Map<String, ProcessThread> lockToThread,
                                    Set<ProcessThread> visited,
                                    Set<ProcessThread> recStack,
                                    List<ProcessThread> cycle) {
        if (recStack.contains(thread)) {
            cycle.add(thread);
            return true; // Cycle found
        }
        
        if (visited.contains(thread)) {
            return false;
        }
        
        visited.add(thread);
        recStack.add(thread);
        
        // Check what lock this thread is waiting for
        String waitingFor = thread.getWaitingForLock();
        if (waitingFor != null && lockToThread.containsKey(waitingFor)) {
            ProcessThread holder = lockToThread.get(waitingFor);
            if (hasCycle(holder, threadToLocks, lockToThread, visited, recStack, cycle)) {
                cycle.add(thread);
                return true;
            }
        }
        
        recStack.remove(thread);
        return false;
    }
    
    /**
     * Creates a visual representation of the resource allocation graph.
     */
    public static String visualizeGraph(List<ProcessThread> threads, List<LockResource> locks) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resource Allocation Graph:\n");
        sb.append("========================\n\n");
        
        for (ProcessThread thread : threads) {
            sb.append(thread.getThreadName()).append(" (")
              .append(thread.getState()).append(")\n");
            
            // Held locks
            if (!thread.getHeldLocks().isEmpty()) {
                sb.append("  Holds: ").append(thread.getHeldLocks()).append("\n");
            }
            
            // Waiting for lock
            if (thread.getWaitingForLock() != null) {
                sb.append("  Waiting for: ").append(thread.getWaitingForLock()).append("\n");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
