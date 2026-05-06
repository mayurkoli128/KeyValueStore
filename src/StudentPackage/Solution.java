package StudentPackage;

import java.util.List;

/**
 * Solution class containing the main demonstration of the distributed KV store.
 */
public class Solution {

    /**
     * Demonstrates the functionality of the distributed key-value store.
     * Creates a cluster, performs put/get operations, and adds a new node.
     */
    public void solve() {

        KVStore cluster = new KVStore();

        // Create cluster
        cluster.createCluster(List.of("node1", "node2", "node3"));

        Node entry = cluster.getAnyNode();

        System.out.println("\n---- NODE HEALTH CHECK ----");
        System.out.println("Node " + entry.getNodeId() + " isRunning: " + entry.isRunning());

        System.out.println("\n---- PUT ----");
        entry.put("user:1", "Mayur");
        entry.put("user:2", "Aman");
        entry.put("user:3", "Ravi");

        System.out.println("\n---- GET ----");
        System.out.println(entry.get("user:1"));
        System.out.println(entry.get("user:2"));
        System.out.println(entry.get("user:3"));

        // Add new node
        System.out.println("\n---- ADD NODE ----");
        cluster.addNode("node4");

        cluster.printCluster();

        // Demonstrate stop and running check
        System.out.println("\n---- STOP NODE ----");
        entry.stop();
        System.out.println("Node " + entry.getNodeId() + " isRunning: " + entry.isRunning());
    }
}