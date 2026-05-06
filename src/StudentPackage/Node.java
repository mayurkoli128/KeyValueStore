package StudentPackage;

import java.util.*;

/**
 * Node represents a single node in the distributed key-value store cluster.
 * Supports simple replication and fault tolerance.
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
    
    private boolean running = false;

    public Node(String nodeId, int replicationFactor) {
        this.nodeId = nodeId;
        this.replicationFactor = replicationFactor;
        this.ring = new ConsistentHashRing();
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
        this.running = true;
        System.out.println("Node started: " + nodeId);
    }

    public void stop() {
        this.running = false;
        System.out.println("Node stopped: " + nodeId);
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Dynamically calculates the next N replica nodes from a given node using TreeMap.
     * This can be called even if the primary node is down.
     */
    private List<Node> getReplicaNodes(String primaryNodeId, int count) {
        List<Node> replicas = new ArrayList<>();

        // Build sorted map of all nodes (including self)
        TreeMap<String, Node> allNodes = new TreeMap<>(peerNodes);
        allNodes.put(nodeId, this);

        String currentId = primaryNodeId;
        for (int i = 0; i < count && replicas.size() < allNodes.size() - 1; i++) {
            String nextId = allNodes.higherKey(currentId);

            // Wrap around to first node if at the end
            if (nextId == null) {
                nextId = allNodes.firstKey();
            }

            // Don't add the primary itself as replica
            if (!nextId.equals(primaryNodeId)) {
                replicas.add(allNodes.get(nextId));
            }
            currentId = nextId;
        }
        
        return replicas;
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
        // Calculate replicas dynamically - works even if primary is down!
        List<Node> replicas = getReplicaNodes(primaryNode.getNodeId(), replicationFactor);
        
        System.out.println("[PUT] Entry: " + nodeId + " | Key: " + key + " | Value: " + value);
        System.out.println("  Primary: " + primaryNode.getNodeId() + " | Replicas: " + getReplicaNames(replicas));

        boolean primarySuccess = false;
        int replicaCount = 0;

        // Store on primary
        if (primaryNode == this) {
            store.put(key, value);
            System.out.println("  [STORE] " + nodeId + " stored (self): " + key + " = " + value);
            primarySuccess = true;
        } else if (primaryNode.isRunning()) {
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
        // Calculate replicas dynamically - works even if primary is down!
        List<Node> replicas = getReplicaNodes(primaryNode.getNodeId(), replicationFactor);
        
        System.out.println("[GET] Entry: " + nodeId + " | Key: " + key);
        System.out.println("  Primary: " + primaryNode.getNodeId() + " | Replicas: " + getReplicaNames(replicas));

        // Try primary first
        if (primaryNode == this) {
            Object value = store.get(key);
            System.out.println("  [SERVE] " + nodeId + " serving (self): " + key + " = " + value);
            return value;
        } else if (primaryNode.isRunning()) {
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

    // Store data directly
    public void storeData(String key, Object value) {
        store.put(key, value);
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