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
}