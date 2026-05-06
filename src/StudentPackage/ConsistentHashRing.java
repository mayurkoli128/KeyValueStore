package StudentPackage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * ConsistentHashRing distributes keys across nodes using consistent hashing.
 */
public class ConsistentHashRing {

    // Sorted map of hash → Node
    private final TreeMap<Long, Node> ring = new TreeMap<>();

    /**
     * Computes a hash for a given key using MD5.
     */
    private long getHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash & 0x7fffffffffffffffL;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a node to the hash ring.
     */
    public void addNode(Node node) {
        long hash = getHash(node.getNodeId());
        ring.put(hash, node);
    }

    /**
     * Returns the node responsible for the given key.
     * Uses clockwise lookup on the ring.
     */
    public Node getNode(String key) {
        if (ring.isEmpty()) return null;

        long hash = getHash(key);
        Map.Entry<Long, Node> entry = ring.ceilingEntry(hash);

        // Wrap around if no higher key exists
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * Returns the next N distinct nodes clockwise from the given node on the hash ring.
     * This is the CORRECT way to get replicas in consistent hashing.
     */
    public java.util.List<Node> getNextNNodes(Node startNode, int count) {
        java.util.List<Node> result = new java.util.ArrayList<>();
        if (ring.isEmpty() || count <= 0) return result;

        long startHash = getHash(startNode.getNodeId());
        
        // Get all entries after the start node
        Map.Entry<Long, Node> entry = ring.higherEntry(startHash);
        
        while (result.size() < count && result.size() < ring.size() - 1) {
            // Wrap around if needed
            if (entry == null) {
                entry = ring.firstEntry();
            }
            
            Node node = entry.getValue();
            
            // Don't add the start node itself, and avoid duplicates
            if (!node.getNodeId().equals(startNode.getNodeId()) && !result.contains(node)) {
                result.add(node);
            }
            
            entry = ring.higherEntry(entry.getKey());
        }
        
        return result;
    }
}
