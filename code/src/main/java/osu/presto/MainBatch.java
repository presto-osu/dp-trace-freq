package osu.presto;

import java.util.*;

public class MainBatch extends Main{
    // number of times each user is selected in each batch
    static int[][] batches;
    static int num_batches = 3;

    public static void main(String[] argv) {
        long start = System.currentTimeMillis();

        init(argv);

        readOptIn();

        deployInBatches();

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("Time: " + timeElapsed / 1000 + "s.");
    }

    // Deploy in three batches, gradually increasing tau.
    public static void deployInBatches() {
        // divide opt-out users into three batches.
        divide();

        for (int batch = 0; batch < num_batches; batch++) {
            System.out.println("*** Running on batch " + batch + " ... ***");
            global_tree = new Tree();
            Sketch.init(global_tree_optin.all_nodes.size(), tau, args.epsilon, args.rows, args.col_mul);
            System.out.println("*** Global sketch [t=" + Sketch.t + ",m=" + Sketch.m + ",log(m)=" + Sketch.log_m +
                    ",epsilon=" + Sketch.epsilon + ", tau=" + tau + "] ***");
            System.out.print("*** Reading traces for " + num_users_opt_out/num_batches + " opt-out users" + " ...    ");
            System.out.flush();
            // Difficulty map, for getting the "real" tau.
            Map<String, Integer> diff_map = new HashMap<>();
            int num_users_bigger_tau = 0;
            int max_local_tau = 0;
            int num_users_batch = 0;
            for (int i = 0; i < trace_list.size(); i++) {
                int users = batches[batch][i];
                if (users == 0) continue;
                num_users_batch += users;

                String fileName = trace_list.get(i);
                List<String> trace = Util.readTrace(trace_dir, fileName);
                Tree t;
                if (args.enter_exit_trace) {
                    t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
                } else {
                    t = Tree.createCallingContextTree(trace, args.depth_limit);
                }
                global_tree.merge(t, users);
                int tau_local = getLocalTau(t);
                if (tau_local > tau) {
                    num_users_bigger_tau += users;
                    if (tau_local > max_local_tau) {
                        max_local_tau = tau_local;
                    }
                }

                for (Tree.Node n: t.all_nodes) {
                    int diff_max = diff_map.getOrDefault(n.long_id, 0);
                    int diff = args.hot ? (n.freq - hideHotnessThreshold) : n.prefix_freq;
                    diff_max = diff_max < diff ? diff : diff_max;
                    if (diff_max > 0) {
                        diff_map.put(n.long_id, diff_max);
                    }
                }
            }
            if (num_users_batch != num_users_opt_out / num_batches) {
                throw new RuntimeException();
            }
            int sum_expected = K * num_users_batch;
            global_sketch = new GlobalSketch(global_tree, num_users_opt_out);
            // Sanity check.
            if (global_tree.getSumFreq() != sum_expected) {
                throw new RuntimeException(global_tree.getSumFreq() + " != " + sum_expected);
            }

            ArrayList<Integer> diff_list = new ArrayList<>(diff_map.values());
            diff_list.sort(null);
            int tau_real = diff_list.get(((int)(diff_list.size() * args.protect/100.0)) - 1);

            int size_opt_in = global_tree_optin.all_nodes.size();
            int size_opt_out = global_tree.all_nodes.size();
            double ratio_users_bigger_tau = ((double)num_users_bigger_tau) / num_users_batch;
            System.out.println("\b\b\b 100% done. [total nodes: " + size_opt_out +
                    ", depth: " + global_tree.depth +
                    ", size_ratio_in_to_out: " + ((double)size_opt_in)/size_opt_out +
                    ", tau_real: " + tau_real +
                    ", bigger_tau_users: " + num_users_bigger_tau + "(" + ratio_users_bigger_tau + ")" + "]");
            error(num_users_batch);
            double tau_inc = 0;
            if (tau < max_local_tau) {
                tau_inc = ((double)(max_local_tau - tau)) / tau;
                tau = max_local_tau;
            }
            System.out.println("Increasing tau to " + tau + " (" + tau_inc + ")");
        }
    }

    // L1 norm normalized by the expected value of max L1 norm. The estimates of freqs are the raw value in sketch,
    // instead of rounded integers in range [0, max].
    public static double error(int num_users) {
        double L1 = 0;
        for (Tree.Node n: global_tree.all_nodes) {
            int ground_freq = n.freq;
            double est_freq = global_sketch.estimate(n.long_id);
            // if (est_freq < 0) est_freq = 0;
            // if (est_freq > max_possible_freq) est_freq = max_possible_freq;
            double delta = (double)ground_freq - est_freq;
            if (delta < 0) delta = -delta;
            L1 += delta;
        }
        double L1_max = 2.0 * K * num_users;
        double re = L1/L1_max;
        System.out.println("RE: " + re);
        return re;
    }

    // Divide the users into batches.
    private static void divide() {
        // list of index to traces, used for drawing random users
        LinkedList<Integer> all_opt_out_users = new LinkedList<>();
        for (int i = 0; i < trace_list.size(); i++) {
            int num_users = args.replication - opt_in_users[i];
            for (int j = 0; j < num_users; j++) {
                all_opt_out_users.add(i);
            }
        }
        if (all_opt_out_users.size() != num_users_opt_out) {
            throw new RuntimeException();
        }
        Collections.shuffle(all_opt_out_users);
        // counter representing how many times each trace appears in each batch
        batches = new int[num_batches][trace_list.size()];
        for (int i = 0; i < trace_list.size(); i++) {
            for (int j = 0; j < num_batches; j++)
                batches[j][i] = 0;
        }
        Random random = new Random();
        for (int i = 0; i < num_batches; i++) {
            for (int j = 0; j < num_users_opt_out / num_batches; j++) {
                int rand = random.nextInt(all_opt_out_users.size());
                int user = all_opt_out_users.remove(rand);
                batches[i][user]++;
            }
        }
    }
}
