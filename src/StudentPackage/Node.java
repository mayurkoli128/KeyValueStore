package StudentPackage;

import java.util.*;
import java.io.*;

/**
 * Node represents a single node in the distributed key-value store cluster.
 * Supports simple replication, fault tolerance, and WAL for persistence.
 */
public class Node {

    private final String nodeId;
    private final ConsistentHashRing ring;

    // Store - all data (primary + replicated)
    private final Map<String, Object> store = new TreeMap<>();

    // Peer nodes (excluding self) - TreeMap for sorted order
    private Map<String, Node> peerNodes = new TreeMap<>();
    
    // Number of replicas to keep
    private final int replicationFactor;
    
    // WAL file for persistence
    private final String walFile;
    
    private boolean running = false;

    public Node(String nodeId, int replicationFactor) {
        this.nodeId = nodeId;
        this.replicationFactor = replicationFactor;
        this.ring = new ConsistentHashRing();
        this.walFile = "data/" + nodeId + "/wal.log";
        ring.addNode(this);
    }

    public void setPeerNodes(Map<String, Node> allNodes) {
        this.peerNodes = new TreeMap<>();
        
        // Add peers to ring
        for (Map.Entry<String, Node> entry : allNodes.entrySet()) {
            if (!entry.getKey().equals(nodeId)) {
                peerNodes.put(entry.getKey(), entry.getValue());
                ring.addNode(entry.getValue());
            }
        }
    }

    public void start() {
        // Create data directory
        new File("data/" + nodeId).mkdirs();
        
        // Replay WAL to recover data
        replayWAL();
        
        this.running = true;
        System.out.println("Node started: " + nodeId);
    }

    public void stop() {
        this.running = false;
        store.clear();
        System.out.println("Node stopped: " + nodeId);
    }
    
    // ==================== WAL ====================
    
    // Append to WAL before storing
    private void appendToWAL(String key, Object value) {
        try (FileWriter fw = new FileWriter(walFile, true)) {
            fw.write("PUT " + key + " " + value + "\n");
            System.out.println("  [WAL] " + nodeId + " logged: PUT " + key + " " + value);
        } catch (IOException e) {
            System.out.println("  [WAL ERROR] " + e.getMessage());
        }
    }
    
    // Replay WAL on restart
    private void replayWAL() {
        File file = new File(walFile);
        if (!file.exists()) return;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                // Parse: "PUT key value"
                String[] parts = line.split(" ", 3);
                if (parts.length == 3 && parts[0].equals("PUT")) {
                    store.put(parts[1], parts[2]);
                    count++;
                }
            }
            if (count > 0) {
                System.out.println("  [WAL] " + nodeId + " replayed " + count + " entries");
            }
        } catch (IOException e) {
            System.out.println("  [WAL ERROR] " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }

    private String getReplicaNames(List<Node> replicas) {
        List<String> names = new ArrayList<>();
        for (Node n : replicas) {
            names.add(n.getNodeId());
        }
        return names.toString();
    }

    public boolean put(String key, Object value) {
        Node primaryNode = ring.getNode(key);
        // Use hash ring to get replicas
        List<Node> replicas = ring.getNextNNodes(primaryNode, replicationFactor);
        
        System.out.println("[PUT] Entry: " + nodeId + " | Key: " + key + " | Value: " + value);
        System.out.println("  Primary: " + primaryNode.getNodeId() + " | Replicas: " + getReplicaNames(replicas));

        boolean primarySuccess = false;
        int replicaCount = 0;

        if (primaryNode.isRunning()) {
            primaryNode.storeData(key, value);
            primarySuccess = true;
        } else {
            System.out.println("  [WARN] Primary " + primaryNode.getNodeId() + " is DOWN!");
        }

        // Store on replica servers
        for (Node replicaNode : replicas) {
            if (replicaNode.isRunning()) {
                replicaNode.storeData(key, value);
                replicaCount++;
            } else {
                System.out.println("  [FAILOVER] Replica " + replicaNode.getNodeId() + " is DOWN! Trying other replicas...");
            }
        }

        System.out.println("  Result: Primary=" + (primarySuccess ? "OK" : "FAILED") + ", Replicas=" + replicaCount);
        return primarySuccess || replicaCount > 0;
    }

    public Object get(String key) {
        Node primaryNode = ring.getNode(key);
        // Use hash ring to get replicas
        List<Node> replicas = ring.getNextNNodes(primaryNode, replicationFactor);
        
        System.out.println("[GET] Entry: " + nodeId + " | Key: " + key);
        System.out.println("  Primary: " + primaryNode.getNodeId() + " | Replicas: " + getReplicaNames(replicas));

        if (primaryNode.isRunning()) {
            Object value = primaryNode.getData(key);
            return value;
        }

        // Primary is down, try replicas
        System.out.println("  [FAILOVER] Primary " + primaryNode.getNodeId() + " is DOWN! Trying replicas...");
        
        for (Node replicaNode : replicas) {
            if (replicaNode.isRunning()) {
                Object value = replicaNode.getData(key);
                return value;
            } else {
                System.out.println("  [FAILOVER] Replica " + replicaNode.getNodeId() + " is DOWN! Trying other replicas...");
            }
        }

        System.out.println("  [ERROR] No nodes available!");
        return null;
    }

    // Store data directly - WAL first, then memory
    public void storeData(String key, Object value) {
        appendToWAL(key, value);  // 1. Write to WAL (disk)
        store.put(key, value);     // 2. Update memory
        System.out.println("  [STORE] " + nodeId + " stored: " + key + " = " + value);
    }

    // Get data directly
    public Object getData(String key) {
        Object value = store.get(key);
        System.out.println("  [SERVE] " + nodeId + " serving: " + key + " = " + value);
        return value;
    }

    public String getNodeId() {
        return nodeId;
    }

    public Map<String, Object> getStore() {
        return new TreeMap<>(store);
    }
}