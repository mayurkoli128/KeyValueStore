package StudentPackage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * ConsistentHashRing implements a consistent hashing algorithm for distributing
 * keys across multiple nodes in a distributed system.
 */
public class ConsistentHashRing {

    // Sorted map: hash → node
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int replicas;

    /**
     * Creates a new ConsistentHashRing with the specified number of virtual nodes (replicas)
     * per physical node.
     *
     * @param replicas Number of virtual nodes per physical node
     */
    public ConsistentHashRing(int replicas) {
        this.replicas = replicas;
    }

    /**
     * Computes the hash value for a given key using MD5.
     *
     * @param key The key to hash
     * @return A positive long hash value
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
     * Adds a node to the hash ring with the configured number of virtual nodes.
     *
     * @param node The node identifier to add
     */
    public void addNode(String node) {
        for (int i = 0; i < replicas; i++) {
            long hash = getHash(node + "#" + i);
            ring.put(hash, node);
        }
    }

    /**
     * Removes a node and all its virtual nodes from the hash ring.
     *
     * @param node The node identifier to remove
     */
    public void removeNode(String node) {
        for (int i = 0; i < replicas; i++) {
            long hash = getHash(node + "#" + i);
            ring.remove(hash);
        }
    }

    /**
     * Gets the node responsible for the given key using consistent hashing.
     * Finds the first node clockwise from the key's hash position.
     *
     * @param key The key to look up
     * @return The node responsible for the key, or null if the ring is empty
     */
    public String getNode(String key) {
        if (ring.isEmpty()) return null;

        long hash = getHash(key);

        // Find first node clockwise
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);

        // Wrap around
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }
}