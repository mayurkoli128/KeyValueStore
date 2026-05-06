package StudentPackage;

import java.util.List;

public class Solution {

    public void solve() {

        KVStore cluster = new KVStore();

        // Create cluster with 3 nodes (replicationFactor is property of Node, default=1)
        System.out.println("========== CREATING CLUSTER ==========");
        cluster.createCluster(List.of("node1", "node2", "node3", "node4"));

        Node entry = cluster.getAnyNode();
        System.out.println("\nEntry point: " + entry.getNodeId());

        // PUT operations - shows logging and replication
        System.out.println("\n========== PUT OPERATIONS (with replication) ==========");
        entry.put("user:1", "Mayur");
        System.out.println();
        entry.put("user:2", "Aman");
        System.out.println();
        entry.put("user:3", "Ravi");

        // GET operations - shows which node serves
        System.out.println("\n========== GET OPERATIONS ==========");
        System.out.println("Result: " + entry.get("user:1"));
        System.out.println();
        System.out.println("Result: " + entry.get("user:2"));
        System.out.println();
        System.out.println("Result: " + entry.get("user:3"));

        // Stop the primary node to demonstrate failover
        System.out.println("\n========== FAULT TOLERANCE TEST ==========");
        System.out.println("Stopping node3 (primary for user:1 and user:3) to test failover...");
        cluster.getNode("node3").stop();

        System.out.println("\n--- Getting data after node3 is down (should use backup node1) ---");
        System.out.println("Result: " + entry.get("user:1"));
        System.out.println();
        System.out.println("Result: " + entry.get("user:2"));
        System.out.println();
        System.out.println("Result: " + entry.get("user:3"));

        // Test WAL persistence
        System.out.println("\n========== WAL PERSISTENCE TEST ==========");
        System.out.println("Stopping node2 (data goes to WAL on disk)...");
        cluster.getNode("node2").stop();
        
        System.out.println("\nRestarting node2 (data recovered from WAL)...");
        cluster.getNode("node2").start();
        
        System.out.println("\n--- GET after node2 restart (data should be recovered) ---");
        System.out.println("Result: " + entry.get("user:1"));

        // Show cluster state
        System.out.println("\n========== CLUSTER STATE ==========");
        cluster.printCluster();
    }
}
