package StudentPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node represents a single node in the distributed key-value store cluster.
 * Each node maintains its own local store and can route requests to other nodes
 * using consistent hashing.
 */
public class Node {

    private final String nodeId;
    private final List<String> peerNodes;
    private final ConsistentHashRing ring;

    private final Map<String, Object> store = new HashMap<>();

    private Map<String, Node> cluster;

    /**
     * Creates a new Node with the specified ID and list of peer nodes.
     *
     * @param nodeId    Unique identifier for this node
     * @param peerNodes List of peer node IDs in the cluster
     */
    public Node(String nodeId, List<String> peerNodes) {
        this.nodeId = nodeId;
        this.peerNodes = new ArrayList<>(peerNodes);

        this.ring = new ConsistentHashRing(100);

        ring.addNode(nodeId);
        for (String peer : peerNodes) {
            ring.addNode(peer);
        }
    }

    /**
     * Sets the cluster reference for inter-node communication.
     *
     * @param cluster Map of node IDs to Node instances
     */
    public void setCluster(Map<String, Node> cluster) {
        this.cluster = cluster;
    }

    /**
     * Starts the node and prints a startup message.
     */
    public void start() {
        System.out.println("Node started: " + nodeId);
    }

    /**
     * Sends a message to another node in the cluster.
     *
     * @param targetNode The target node ID
     * @param message    The message to send
     * @return The response from the target node, or null if the node doesn't exist
     */
    public Map<String, Object> sendToNode(String targetNode, Map<String, Object> message) {
        Node node = cluster.get(targetNode);
        if (node == null) return null;

        return node.onMessage(this.nodeId, message);
    }

    /**
     * Handles incoming messages from other nodes.
     *
     * @param fromNode The sender node ID
     * @param message  The received message
     * @return Response map based on the action performed
     */
    public Map<String, Object> onMessage(String fromNode, Map<String, Object> message) {

        String action = (String) message.get("action");

        if ("put".equals(action)) {
            store.put((String) message.get("key"), message.get("value"));
            return Map.of("status", "ok");
        }

        if ("get".equals(action)) {
            return Map.of("value", store.get(message.get("key")));
        }

        return Map.of();
    }

    /**
     * Stores a key-value pair in the appropriate node determined by consistent hashing.
     *
     * @param key   The key to store
     * @param value The value to store
     * @return true if the operation was successful, false otherwise
     */
    public boolean put(String key, Object value) {
        String targetNode = ring.getNode(key);

        if (targetNode.equals(nodeId)) {
            store.put(key, value);
            return true;
        }

        Map<String, Object> response = sendToNode(
                targetNode,
                Map.of("action", "put", "key", key, "value", value)
        );

        return response != null;
    }

    /**
     * Retrieves a value for the given key from the appropriate node.
     *
     * @param key The key to look up
     * @return The value associated with the key, or null if not found
     */
    public Object get(String key) {
        String targetNode = ring.getNode(key);

        if (targetNode.equals(nodeId)) {
            return store.get(key);
        }

        Map<String, Object> response = sendToNode(
                targetNode,
                Map.of("action", "get", "key", key)
        );

        return response != null ? response.get("value") : null;
    }

    /**
     * Gets the unique identifier for this node.
     *
     * @return The node ID
     */
    public String getNodeId() {
        return nodeId;
    }
}