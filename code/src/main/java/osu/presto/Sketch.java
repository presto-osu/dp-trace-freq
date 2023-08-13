package osu.presto;

import org.apache.commons.math3.distribution.LaplaceDistribution;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public abstract class Sketch {
    // row size
    public static int t;
    // column size
    public static int m; // must be a power of 2
    public static int log_m;
    public static double epsilon;
    private static MessageDigest digest;
    protected static LaplaceDistribution laplace;

    protected double[][] table_dp;

    protected void init() {
        table_dp = new double[t][m];
        // start with zero in all table cells
        for (int i = 0; i < t; i++)
            for (int j = 0; j < m; j++)
                table_dp[i][j] = 0;
    }

    public static void init(int numTracesOptIn, int tau, double epsilon_para, int rows, int col_mul) {
        t = rows;
        m = (int) Math.round(Math.pow(2, Math.ceil(Math.log(numTracesOptIn) / Math.log(2))));
        m *= col_mul;
        log_m = (int) Math.round((Math.log(m) / Math.log(2)));
        epsilon = epsilon_para;
        double scale = 2 * tau / epsilon;
        laplace = new LaplaceDistribution(0, scale);

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 died");
        }
    }

    protected int[] sha256(String long_id, int row) {
        byte[] encoded_hash;
        String s = long_id + "|" + row;
        try {
            encoded_hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 died for " + s);
        }
        boolean[] all_bits = new boolean[256];
        int pos = 0;
        for (int i = 0; i < 32; i++) {
            int x = encoded_hash[i] + 128; // value from 0 to 255
            for (int j = 0; j < 8; j++) {
                if (x % 2 == 0)
                    all_bits[pos + 7 - j] = false;
                else
                    all_bits[pos + 7 - j] = true;
                x /= 2;
            }
            pos += 8;
            if (pos > log_m + 1) break;
        }
        // take the first log(m) bits and make an int. this is res[0]. take
        // the next bit and make it the sign. this is res[1]
        int[] res = new int[2];
        res[0] = 0;
        for (int i = 0; i < log_m; i++) {
            if (all_bits[i]) res[0] = 2 * res[0] + 1;
            else res[0] = 2 * res[0];
        }

        if (all_bits[log_m]) res[1] = 1;
        else res[1] = -1;
        return res;
    }
}