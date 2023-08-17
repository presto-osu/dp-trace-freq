package osu.presto;

import java.util.List;

public class MainConfigRows extends Main {
    public static int num_test_users = 0;
    public static Tree global_tree_test_users;

    public static void main(String[] argv) {
        long start = System.currentTimeMillis();

        init(argv);

        readOptIn();

        configRows();

        readOptOut();

        error();

        heavyHittersUsingOptIn();

        errorHH();

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("Time: " + timeElapsed / 1000 + "s.");
    }

    // find the configuration of sketch size that achieves the target error with minimum rows
    public static void configRowsBinarySearch() {
        // build the global tree for test users (9x rep)
        global_tree_test_users = new Tree();
        int rep = 1;
        num_test_users = 0;
        for (int i = 0; i < trace_list.size(); i++) {
            if (opt_in_users[i] == 0) continue;
            int users = opt_in_users[i] * rep;
            num_test_users += users;

            String fileName = trace_list.get(i);
            List<String> trace = Util.readTrace(trace_dir, fileName);
            Tree t;
            if (args.enter_exit_trace) {
                t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
            } else {
                t = Tree.createCallingContextTree(trace, args.depth_limit);
            }
            global_tree_test_users.merge(t, users);
        }
        if (num_test_users != num_users_opt_in * rep) {
            throw new RuntimeException();
        }

        double targetError = 0.1;
        int runs = 3;
        int smallestRow = 9;
        // binary search
        int low = 0, high = 8;
        while (low <= high) {
            int mid = (low + high) / 2;
            double error = 0;
            for (int i = 0; i < runs; i++) {
                error += runOnTestUsers(1 << mid, 1 << (8 - mid));
            }
            error /= runs;
            if (error > targetError) {
                low = mid + 1;
            } else {
                if (mid < smallestRow) {
                    smallestRow = mid;
                }
                high = mid - 1;
            }
        }
        if (smallestRow != 9) {
            args.rows = 1 << smallestRow;
            args.col_mul = 1 << (8 - smallestRow);
            System.out.println("Final sketch config: rows=" + args.rows + ", cols_mul=" + args.col_mul);
        } else {
            throw new RuntimeException("No row found for target error: " + targetError);
        }
    }

    // no binary search
    public static void configRows() {
        long start = System.currentTimeMillis();
        // build the global tree for test users (9x rep)
        global_tree_test_users = new Tree();
        int rep = 1;
        num_test_users = 0;
        for (int i = 0; i < trace_list.size(); i++) {
            if (opt_in_users[i] == 0) continue;
            int users = opt_in_users[i] * rep;
            num_test_users += users;

            String fileName = trace_list.get(i);
            List<String> trace = Util.readTrace(trace_dir, fileName);
            Tree t;
            if (args.enter_exit_trace) {
                t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
            } else {
                t = Tree.createCallingContextTree(trace, args.depth_limit);
            }
            global_tree_test_users.merge(t, users);
        }
        if (num_test_users != num_users_opt_in * rep) {
            throw new RuntimeException();
        }
        double targetError = 0.1;
        int runs = 5;
        for (int i = 0; i < 9; i++) {
            double error = 0;
            for (int j = 0; j < runs; j++) {
                error += runOnTestUsers(1 << i, 1 << (8 - i));
            }
            error /= runs;
            System.out.println( "[" + (1 << i) + ", " + (1 << (8-i)) + "] " + "average error: " + error);
            if (error <= targetError) {
                args.rows = 1 << i;
                args.col_mul = 1 << (8 - i);
                long finish = System.currentTimeMillis();
                long timeElapsed = finish - start;
                System.out.println("Time Characterization: " + timeElapsed / 1000 + "s.");
                System.out.println("Final sketch config: rows=" + args.rows + ", cols_mul=" + args.col_mul);
                return;
            }
        }
        throw new RuntimeException("No row found for target error: " + targetError);
    }

    public static double runOnTestUsers(int rows, int cols_mul) {
        System.out.print("*** Running on " + num_test_users + " test users, #rows " + rows + ", #cols_mul "
                + cols_mul + " ... ");
        Sketch.init(global_tree_optin.all_nodes.size(), tau, args.epsilon, rows, cols_mul);
        GlobalSketch sketch = new GlobalSketch(global_tree_test_users, num_test_users);
        // compute the error
        double L1 = 0;
        for (Tree.Node n: global_tree_test_users.all_nodes) {
            int ground_freq = n.freq;
            double est_freq = sketch.estimate(n.long_id);
            double delta = (double)ground_freq - est_freq;
            if (delta < 0) delta = -delta;
            L1 += delta;
        }
        double L1_max = 2.0 * K * num_test_users;
        double re = L1/L1_max;
        System.out.println("error: " + re);
        return re;
    }
}
