package osu.presto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;

public class Util {
    public static int readSizeV(Path trace_dir) {
        String size_path = trace_dir.resolve("v").toString();
        try (BufferedReader br = new BufferedReader(new FileReader(size_path))) {
            String st = br.readLine();
            return Integer.parseInt(st);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LinkedList<String> readList(Path trace_dir) {
        String list_path = trace_dir.resolve("list").toString();
        try (BufferedReader br = new BufferedReader(new FileReader(list_path))) {
            LinkedList<String> ret = new LinkedList<>();
            String st;
            while ((st = br.readLine()) != null) {
                ret.add(st);
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LinkedList<String> readTrace(Path trace_dir, String file_name) {
        String trace_path = trace_dir.resolve(file_name).toString();
        try (BufferedReader br = new BufferedReader(new FileReader(trace_path))) {
            LinkedList<String> res = new LinkedList<>();
            String st;
            while ((st = br.readLine()) != null) {
                if (st.startsWith("***")); else res.add(st);
            }
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, Set<Integer>> readCallPairs(Path trace_dir) {
        String file_path = trace_dir.resolve("callpairs").toString();
        try (BufferedReader br = new BufferedReader(new FileReader(file_path))) {
        Map<Integer, Set<Integer>> ret = new HashMap<>();
        String st;
            while ((st = br.readLine()) != null) {
                int x = st.indexOf(",");
                String s1 = st.substring(0,x);
                String s2 = st.substring(x+1);
                int m1 = Integer.parseInt(s1);
                int m2 = Integer.parseInt(s2);
                Set<Integer> callees = ret.get(m1);
                if (callees == null) {
                    callees = new HashSet<Integer>();
                    ret.put(m1,callees);
                }
                callees.add(m2);
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String rel(double x, double y) {
        DecimalFormat df = new DecimalFormat("0.000");
        return df.format(((double)x)/y);
    }

    // Log a warning if conversion from double to float lose too much precision.
    public static boolean checkFloatOverflow(float valueF, double valueD, double error) {
        double loss = Math.abs(valueD - (double)valueF);
        if (loss > error) {
            System.out.println("Warning: precision loss from double to float: (float)" + valueF + ", (double)" + valueD
                    + ", loss: " + loss);
            return false;
        }
        return true;
    }
}
