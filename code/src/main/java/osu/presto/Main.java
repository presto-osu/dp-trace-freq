package osu.presto;

import com.beust.jcommander.JCommander;
import osu.presto.Tree;

import java.nio.file.Path;
import java.util.*;

public class Main {

    public static Args args;
    public static Path trace_dir;
    public static List<String> trace_list;
    public static int size_v;
    // number of times each trace is selected as opt-in users.
    public static int[] opt_in_users;
    public static int num_users_opt_in;
    public static int num_users_opt_out;
    // The global CCT for opt-in users.
    public static Tree global_tree_optin;
    // The global CCT for opt-out users.
    public static Tree global_tree;
    public static GlobalSketch global_sketch;
    // The maximum possible frequency of a chain by all opt-out users.
    // The largest size_v is 8682. 10*8682*9000 < 2147483647. So, int is enough.
    public static int max_possible_freq;
    public static int tau;
    public static Set<String> estHeavyHitters = new HashSet<>();
    // The sum of local freqs. For call chains, it's 10*size_v. For enter/exit traces, it's 20*size_v.
    public static int K;
    public static int hideHotnessThreshold;

    public static void init(String[] argv) {
        args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);

        trace_dir = Path.of(args.dir, args.app);
        size_v = Util.readSizeV(trace_dir);
        trace_list = Util.readList(trace_dir);

        String analysis = "call chain";
        if (args.enter_exit_trace) {
            analysis = "enter/exit trace";
        }
        System.out.println("*** " + args.app + " v=" + size_v
                + " " + trace_list.size() + " traces " + args.replication + "x replication " + analysis + " ***");

        // number of times each trace is selected as opt-in users.
        opt_in_users = split(trace_list.size(), args.replication);

        max_possible_freq = 10 * size_v * num_users_opt_out;

        if (args.enter_exit_trace) {
            K = 20 * size_v;
        } else {
            K = 10 * size_v;
        }
    }

    public static void main(String[] argv) {
        long start = System.currentTimeMillis();

        init(argv);

        readOptIn();

        readOptOut();

        error();

        global_sketch.checkConflicts(global_tree);

        heavyHittersUsingOptIn();

        errorHH();

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.println("Time: " + timeElapsed / 1000 + "s.");
    }

    // read traces for opt-in users, build global CCT for opt-in, determine tau
    public static void readOptIn() {
        if (args.hot) {
            readOptInHotness();
            return;
        }
        System.out.println("*** Reading traces for " + num_users_opt_in + " opt-in users ***");
        global_tree_optin = new Tree();
        // Map of difficulties.
        Map<String, Integer> diff_map = new HashMap<>();
        for (int i = 0; i < trace_list.size(); i++) {
            if (opt_in_users[i] == 0) continue;

            String fileName = trace_list.get(i);
            List<String> trace = Util.readTrace(trace_dir, fileName);
            Tree t;
            if (args.enter_exit_trace) {
                t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
            } else {
                t = Tree.createCallingContextTree(trace, args.depth_limit);
            }
            global_tree_optin.merge(t, opt_in_users[i]);

            for (Tree.Node n : t.all_nodes) {
                int diff = diff_map.getOrDefault(n.long_id, 0);
                diff = diff < n.prefix_freq ? n.prefix_freq : diff;
                diff_map.put(n.long_id, diff);
            }
        }
        ArrayList<Integer> diff_list = new ArrayList<>(diff_map.values());
        // Sort in ascending order.
        diff_list.sort(null);
        tau = diff_list.get(((int) (diff_list.size() * args.protect / 100.0)) - 1);
        System.out.println("*** num_nodes: " + global_tree_optin.all_nodes.size()
                + ", depth: " + global_tree_optin.depth
                + ", tau(presence " + args.protect + "%): " + tau + " ***");
    }

    public static void readOptInHotness() {
        System.out.println("*** Reading traces for " + num_users_opt_in + " opt-in users ***");
        global_tree_optin = new Tree();
        // first round: find the size of tree
        for (int i = 0; i < trace_list.size(); i++) {
            if (opt_in_users[i] == 0) continue;

            String fileName = trace_list.get(i);
            List<String> trace = Util.readTrace(trace_dir, fileName);
            Tree t;
            if (args.enter_exit_trace) {
                t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
            } else {
                t = Tree.createCallingContextTree(trace, args.depth_limit);
            }
            global_tree_optin.merge(t, opt_in_users[i]);
        }
        hideHotnessThreshold = K / (global_tree_optin.all_nodes.size());
        // hideHotnessThreshold = K / size_v;
        System.out.println("Threshold for hiding the hotness = " + hideHotnessThreshold);
        // Map of difficulties.
        Map<String, Integer> diff_map = new HashMap<>();
        // second round: find the difficulties
        for (int i = 0; i < trace_list.size(); i++) {
            if (opt_in_users[i] == 0) continue;

            String fileName = trace_list.get(i);
            List<String> trace = Util.readTrace(trace_dir, fileName);
            Tree t;
            if (args.enter_exit_trace) {
                t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
            } else {
                t = Tree.createCallingContextTree(trace, args.depth_limit);
            }
            global_tree_optin.merge(t, opt_in_users[i]);

            for (Tree.Node n : t.all_nodes) {
                int diff = n.freq - hideHotnessThreshold;
                if (diff <= 0) {
                    continue;
                }
                int diff_max = diff_map.getOrDefault(n.long_id, 0);
                diff_max = diff_max < diff ? diff : diff_max;
                diff_map.put(n.long_id, diff_max);
            }
        }
        ArrayList<Integer> diff_list = new ArrayList<>(diff_map.values());
        // Sort in ascending order.
        diff_list.sort(null);
        System.out.println("Number of hot nodes: " + diff_list.size());
        int idx = ((int) (diff_list.size() * args.protect / 100.0)) - 1;
        if (idx < 0) {
            idx = 0;
        }
        tau = diff_list.get(idx);
        System.out.println("*** num_nodes: " + global_tree_optin.all_nodes.size()
                + ", depth: " + global_tree_optin.depth
                + ", tau(hotness " + args.protect + "%): " + tau + " ***");
    }

    // Read traces for opt-out users and build sketches.
    public static void readOptOut() {
        global_tree = new Tree();

        Sketch.init(global_tree_optin.all_nodes.size(), tau, args.epsilon, args.rows, args.col_mul);

        System.out.println("*** Building global sketch [t=" + Sketch.t +
                ",m=" + Sketch.m + ",log(m)=" + Sketch.log_m + ",epsilon=" + Sketch.epsilon + "] ***");
        System.out.print("*** Reading traces for " + num_users_opt_out + " opt-out users" + " ...    ");
        System.out.flush();

        // Difficulty map, for getting the "real" tau.
        Map<String, Integer> diff_map = new HashMap<>();
        for (int i = 0; i < trace_list.size(); i++) {
            int users = args.replication - opt_in_users[i];
            if (users == 0) continue;

            String fileName = trace_list.get(i);
            List<String> trace = Util.readTrace(trace_dir, fileName);
            Tree t;
            if (args.enter_exit_trace) {
                t = Tree.createEnterExitTraceTree(trace, args.depth_limit);
            } else {
                t = Tree.createCallingContextTree(trace, args.depth_limit);
            }
            global_tree.merge(t, users);
            for (Tree.Node n: t.all_nodes) {
                int diff_max = diff_map.getOrDefault(n.long_id, 0);
                int diff = args.hot ? (n.freq - hideHotnessThreshold) : n.prefix_freq;
                diff_max = diff_max < diff ? diff : diff_max;
                if (diff_max > 0) {
                    diff_map.put(n.long_id, diff_max);
                }
            }
        }

        int sum_expected = K * num_users_opt_out;
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
        System.out.println("\b\b\b 100% done. [total nodes: " + size_opt_out +
                ", depth: " + global_tree.depth +
                ", size_ratio_in_to_out: " + ((double)size_opt_in)/size_opt_out +
                ", tau_real: " + tau_real + "]");
    }

    // L1 norm normalized by the expected value of max L1 norm. The estimates of freqs are the raw value in sketch,
    // instead of rounded integers in range [0, max].
    public static double error() {
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
        double L1_max = 2.0 * K * num_users_opt_out;
        double re = L1/L1_max;
        System.out.println("RE: " + re);
        return re;
    }

    public static long freqEstimate(String long_id) {
        double est_freq = global_sketch.estimate(long_id);
        if (est_freq < 0.0) est_freq = 0.0d;
        if (est_freq > max_possible_freq) est_freq = max_possible_freq;
        // TODO: Check overflow.
        return (long)est_freq;
    }

    // Choose randomly 10% opt-in users. Returns array of counters of how many times each trace is selected (due to
    // replication).
    public static int[] split(int trace_size, int replication) {
        double opt_in_rate = 0.1;
        int num_users = trace_size * replication;
        num_users_opt_in = (int)(num_users * opt_in_rate);
        num_users_opt_out = num_users - num_users_opt_in;

        // list of index to traces, used for drawing random users
        LinkedList<Integer> all_trace_idx = new LinkedList<>();
        for (int j = 0; j < replication; j++) {
            for (int i = 0; i < trace_size; i++) {
                all_trace_idx.add(i);
            }
        }
        Collections.shuffle(all_trace_idx);
        // counter representing how many times each trace appears in opt-in set
        int[] opt_in_trace_ctr = new int[trace_size];
        for (int i = 0; i < opt_in_trace_ctr.length; i++) {
            opt_in_trace_ctr[i] = 0;

        }
        Random random = new Random();
        for (int i = 0; i < num_users_opt_in; i++) {
            int rand = random.nextInt(all_trace_idx.size());
            int idx = all_trace_idx.remove(rand);
            opt_in_trace_ctr[idx]++;
        }

        return opt_in_trace_ctr;
    }

    public static void heavyHittersUsingOptIn() {
        long threshold = (long) K * num_users_opt_out / global_tree_optin.all_nodes.size();
        for (Tree.Node node : global_tree_optin.all_nodes) {
            long est_freq = freqEstimate(node.long_id);
            if (est_freq >= threshold) {
                estHeavyHitters.add(node.long_id);
            }
        }

        Set<String> realHeavyHitters = new HashSet<>();
        for (Tree.Node node : global_tree.all_nodes) {
            if (node.freq > threshold) {
                realHeavyHitters.add(node.long_id);
            }
        }

        // precision and recall
        Set<String> inter = new HashSet<String>(realHeavyHitters);
        inter.retainAll(estHeavyHitters);
        String recall = Util.rel(inter.size(), realHeavyHitters.size());
        String precision = Util.rel(inter.size(), estHeavyHitters.size());

        System.out.println("Real HH: " + realHeavyHitters.size() + ", estimated HH: " + estHeavyHitters.size() +
                ", recall: " + recall + ", precision: " + precision);
    }

    public static void errorHH() {
        Map<String, Integer> groundFreqMap = new HashMap<>();
        for (Tree.Node n : global_tree.all_nodes) {
            groundFreqMap.put(n.long_id, n.freq);
        }

        double deltaSum = 0;
        double groundSum = 0;
        for (String hh : estHeavyHitters) {
            int ground_freq = groundFreqMap.getOrDefault(hh, 0);
            double est_freq = global_sketch.estimate(hh);
            double delta = (double)ground_freq - est_freq;
            if (delta < 0) delta = -delta;
            deltaSum += delta;
            groundSum += ground_freq;
        }
        double re = deltaSum / (groundSum * 2);
        System.out.println("RE-HH: " + re);
    }

}
