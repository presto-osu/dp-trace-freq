package osu.presto;

import java.util.Arrays;

public class GlobalSketch extends Sketch {
    public GlobalSketch() {
        init();
    }

    public void addContributionOf(Sketch s) {
        for (int i = 0; i < t; i++)
            for (int j = 0; j < m; j++) {
                table_dp[i][j] += s.table_dp[i][j];
            }
    }

    public GlobalSketch(Tree tree, int num_users) {
        init();

        for (Tree.Node n : tree.all_nodes) {
            if (n == tree.root) throw new RuntimeException();
            for (int j = 0; j < t; j++) {
                int[] res = sha256(n.long_id, j);
                table_dp[j][res[0]] += res[1] * n.freq;
            }
        }

        // add laplace noise
        for (int i = 0; i < t; i++) {
            for (int j = 0; j < m; j++) {
                // TODO: double overflow?
                double laplace_all_d = 0.0;
                for (int k = 0; k < num_users; k++) {
                    double laplace_d = laplace.sample();
                    // This is unlikely to happen.
                    if (Double.isInfinite(laplace_d)) {
                        throw new RuntimeException("Infinite laplace!");
                    }
                    laplace_all_d += laplace_d;
                }
                table_dp[i][j] += laplace_all_d;
            }
        }
    }

    // The return value could be +/-Infinite.
    public double estimate(String long_id) {
        double[] vals = new double[t];
        for (int j = 0; j < t; j++) {
            int[] res = sha256(long_id, j);
            vals[j] = res[1] * table_dp[j][res[0]];
        }
        Arrays.sort(vals);
        if (t % 2 == 0) {
            double value = ((double)vals[t / 2 - 1] + (double)vals[t / 2]) / 2.0;
            return value;
        }
        return vals[(t - 1) / 2];
    }

    public void checkConflicts(Tree tree) {
        boolean[][] table_hashed = new boolean[t][m];
        for (int i = 0; i < t; i++)
            for (int j = 0; j < m; j++)
                table_hashed[i][j] = false;

        int conflicts = 0;
        for (Tree.Node n : tree.all_nodes) {
            if (n == tree.root) throw new RuntimeException();
            for (int j = 0; j < t; j++) {
                int[] res = sha256(n.long_id, j);
                if (table_hashed[j][res[0]] == true) {
                    conflicts++;
                } else {
                    table_hashed[j][res[0]] = true;
                }
            }
        }
        System.out.println("Average conflicts per row: " + conflicts / t);
    }
}
