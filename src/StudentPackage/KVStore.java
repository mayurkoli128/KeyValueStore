package StudentPackage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KVStore manages a cluster of distributed key-value store nodes.
 * It provides functionality to create, add, and remove nodes from the cluster.
 */
public class KVStore {

    private final Map<String, Node> nodes = new HashMap<>();

    /**
     * Creates a new cluster with the specified node IDs.
     * All nodes are initialized and connected to each other.
     *
     * @param nodeIds List of node identifiers to create
     */
    public void createCluster(List<String> nodeIds) {

        // Step 1: Create all nodes
        for (String nodeId : nodeIds) {
            Node node = new Node(nodeId);
            nodes.put(nodeId, node);
        }

        // Step 2: Inject peer nodes reference
        for (Node node : nodes.values()) {
            node.setPeerNodes(nodes);
            node.start();
        }
    }

    /**
     * Adds a new node to the existing cluster.
     *
     * @param nodeId The identifier for the new node
     */
    public void addNode(String nodeId) {

        System.out.println("Adding node: " + nodeId);

        Node newNode = new Node(nodeId);
        nodes.put(nodeId, newNode);

        // Update peer nodes reference everywhere
        for (Node node : nodes.values()) {
            node.setPeerNodes(nodes);
        }

        newNode.start();
    }

    /**
     * Removes a node from the cluster.
     *
     * @param nodeId The identifier of the node to remove
     */
    public void removeNode(String nodeId) {

        System.out.println("Removing node: " + nodeId);

        nodes.remove(nodeId);

        for (Node node : nodes.values()) {
            node.setPeerNodes(nodes);
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
     * Gets the number of nodes in the cluster.
     *
     * @return The number of nodes
     */
    public int getClusterSize() {
        return nodes.size();
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