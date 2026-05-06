package StudentPackage;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * KVStore manages a cluster of distributed key-value store nodes.
 * It provides functionality to create, add, and remove nodes from the cluster.
 */
public class KVStore {

    // TreeMap for sorted node order
    private final Map<String, Node> nodes = new TreeMap<>();

    /**
     * Creates a new cluster with the specified node IDs.
     *
     * @param nodeIds List of node identifiers to create
     */
    public void createCluster(List<String> nodeIds) {

        int replicationFactor = 2;

        // Step 1: Create all nodes
        for (String nodeId : nodeIds) {
            Node node = new Node(nodeId, replicationFactor);
            nodes.put(nodeId, node);
        }

        // Step 2: Inject peer nodes reference
        for (Node node : nodes.values()) {
            node.setPeerNodes(nodes);
            node.start();
        }
    }

    /**
     * Gets any available node from the cluster for client operations.
     *
     * @return A node from the cluster, or null if the cluster is empty
     */
    public Node getAnyNode() {
        if (nodes.isEmpty()) return null;
        return nodes.values().iterator().next();
    }

    /**
     * Prints the current state of the cluster showing all node IDs.
     */
    public void printCluster() {
        System.out.println("Current nodes: " + nodes.keySet());
    }

    /**
     * Gets a specific node by its ID.
     *
     * @param nodeId The node identifier
     * @return The node, or null if not found
     */
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }
}