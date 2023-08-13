package osu.presto;

import java.util.*;

/**
 * A version of Calling Context Tree without back edges. The frequency of a leaf is the sum of its real frequency and
 * its descendants pruned (due to depth limit).
 */
public class Tree {
    public Node root = new Node();
    // A tree with root only has depth 0
    public int depth = 0;
    // all nodes excluding the root
    public Set<Node> all_nodes = new HashSet<Node>();

    // build a calling context tree from the trace.
    public static Tree createCallingContextTree(List<String> trace, int depth_limit) {
        Tree tree = new Tree();
        Node curr = tree.root;
        int numEntries = 0;
        // Stack of method_id's.
        Stack<Integer> stack = new Stack<>();
        for (String event: trace) {
            int method_id = Integer.parseInt(event.substring(2));
            if (event.startsWith("E-")) {
                stack.push(method_id);
                numEntries++;
                if (stack.size() <= depth_limit) {
                    // Add node if does not exceed depth limit
                    curr = curr.addOrIncreaseChild(method_id, tree);
                } else {
                    // Node is pruned, add its freq into the leaf node.
                    curr.freq++;
                }
            }
            if (event.startsWith("X-")) {
                if (stack.isEmpty())
                    throw new RuntimeException("Exit on empty stack");
                int top_method_id = stack.pop();
                if (top_method_id != method_id) {
                    throw new RuntimeException("Expected E-" + method_id + " but observed E-" + top_method_id);
                }
                if (stack.size() < depth_limit) {
                    // Retreat the current node to its parent
                    curr = curr.parent;
                }
            }
        }
        // Sanity check. This holds only for depth_limit > 0, because we don't care about the frequency of the
        // artificial root node.
        int sum = tree.getSumFreq();
        if (sum != numEntries) throw new RuntimeException();

        tree.root.cumulate();
        return tree;
    }

    public static Tree createEnterExitTraceTree(List<String> trace, int depth_limit) {
        Tree tree = new Tree();
        Node curr = tree.root;
        int numEvents = 0;
        int validLen = 0;
        Stack<Integer> stack = new Stack<>();
        for (String event: trace) {
            int method_id = Integer.parseInt(event.substring(2));
            if (event.startsWith("X-") && stack.isEmpty())
                throw new RuntimeException("Exit on empty stack");
            if (event.startsWith("E-")) {
                stack.push(method_id);
                if (validLen < depth_limit) {
                    curr = curr.addOrIncreaseChild(method_id, tree);
                    validLen++;
                    numEvents++;
                    // sanity check
                    assert validLen == curr.depth;
                } else {
                    // accumulate freqs to the leaf node
                    curr.freq++;
                    numEvents++;
                }
            }
            if (event.startsWith("X-")) {
                if (stack.isEmpty())
                    throw new RuntimeException("Exit on empty stack");
                int top_method_id = stack.pop();
                if (top_method_id != method_id) {
                    throw new RuntimeException("Expected E-" + method_id + " but observed E-" + top_method_id);
                }
                if (validLen < depth_limit) {
                    curr = curr.addOrIncreaseChild(-method_id, tree);
                    validLen++;
                    numEvents++;
                    // sanity check
                    assert validLen == curr.depth;
                } else {
                    // accumulate the freqs to the leaf node
                    curr.freq++;
                    numEvents++;
                }
                if (stack.size() == 0) {
                    validLen = 0;
                    curr = tree.root;
                }
            }
        }
        // add exit events to match the enter events left in the stack
        while (!stack.isEmpty()) {
            int top_method_id = stack.pop();
            if (validLen < depth_limit) {
                curr = curr.addOrIncreaseChild(-top_method_id, tree);
                validLen++;
                numEvents++;
                // sanity check
                assert validLen == curr.depth;
            } else {
                // accumulate the freqs to the leaf node
                curr.freq++;
                numEvents++;
            }
            if (stack.size() == 0) {
                validLen = 0;
                curr = tree.root;
            }
        }
        // sanity check
        int sum = tree.getSumFreq();
        if (sum != numEvents) throw new RuntimeException();

        // cumulative tree
        tree.root.cumulate();
        return tree;
    }

    public Tree() { }

    // Grow the tree (creating new nodes or updating node freq and prefix_freq) by merging with another tree. The second
    // parameter specifies how many times the other tree is merged, which is efficient for merging the same tree
    // multiple times.
    public void merge(Tree tree_another, int times) {
        // Expected sum of freq.
        int sumExp = this.getSumFreq() + tree_another.getSumFreq() * times;

        List<Node[]> worklist = new LinkedList<>();
        worklist.add(new Node[] {this.root, tree_another.root});
        while (!worklist.isEmpty()) {
            Node[] nodes = worklist.remove(0);
            Node node_mine = nodes[0];
            Node node_another = nodes[1];
            // Add children if not exist.
            for (Node child_another: node_another.children) {
                Node child_mine = node_mine.findChild(child_another.method_id);
                if (child_mine == null) {
                    child_mine = node_mine.addChild(child_another.method_id,
                            this,
                            child_another.freq * times,
                            child_another.prefix_freq * times);
                } else {
                    child_mine.freq += child_another.freq * times;
                    child_mine.prefix_freq += child_another.prefix_freq * times;
                }
                worklist.add(new Node[] {child_mine, child_another});
            }
        }

        // Sanity check
        if (getSumFreq() != sumExp) throw new RuntimeException();
    }

    // Get sum of freqs of all nodes excluding the root.
    public int getSumFreq() {
        int sum = 0;
        for (Node n: this.all_nodes) sum += n.freq;
        return sum;
    }

    public static class Node {
        public int method_id;
        // ids separated by "," excluding the root id "0".
        public String long_id;
        public Node parent;
        public int depth;
        public int freq;
        // Sum of freqs of the current node and its descendants. int is enough because the max possible prefix_freq is
        // the same as the max possible freq.
        public int prefix_freq;
        // Children, no including back edges.
        public List<Node> children = new LinkedList<>();

        public Node(int method_id, Node parent, Tree tree) {
            this.method_id = method_id;
            this.parent = parent;
            this.depth = parent.depth + 1;
            this.freq = 1;
            this.long_id = (this.depth > 1 ? parent.long_id + "," : "") + method_id;
            tree.all_nodes.add(this);
            if (this.depth > tree.depth) {
                tree.depth = this.depth;
            }
        }
        // Only for root node
        public Node() {
            this.depth = 0;
            this.parent = null;
            // The following attributes are never to be used for a root.
            this.method_id = 0;
            this.freq = 0;
            this.long_id = "";
        }

        public Node addChild(int child_method_id, Tree tree) {
            Node ch = new Node(child_method_id, this, tree);
            children.add(ch);
            return ch;
        }

        // Add a child and init its freq and prefix_freq.
        public Node addChild(int child_method_id, Tree tree, int freq, int prefix_freq) {
            Node ch = addChild(child_method_id, tree);
            ch.freq = freq;
            ch.prefix_freq = prefix_freq;
            return ch;
        }

        // Add a child node if not present, otherwise increment the child's freq.
        public Node addOrIncreaseChild(int child_method_id, Tree tree) {
            Node ch = findChild(child_method_id);
            if (ch != null) {
                ch.freq++;
            } else {
                ch = addChild(child_method_id, tree);
            }
            return ch;
        }

        // Find the child, null if not present.
        public Node findChild(int target_method_id) {
            for (Node n: children) {
                if (n.method_id == target_method_id) {
                    return n;
                }
            }
            return null;
        }

        // Calculate prefix-freq for this node and all its descendants.
        public void cumulate() {
            int prefix_freq_children = 0;
            for (Node ch: this.children) {
                ch.cumulate();
                prefix_freq_children += ch.prefix_freq;
            }
            this.prefix_freq = this.freq + prefix_freq_children;
        }
    }
}
