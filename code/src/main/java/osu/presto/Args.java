package osu.presto;

import com.beust.jcommander.Parameter;

public class Args {
    @Parameter(names = "-depth", description = "Depth limit")
    public int depth_limit  = 100;

    @Parameter(names = "-app", description = "App name", required = true)
    public String app;

    @Parameter(names = "-dir", description = "The directory containing the trace directory",
            required = true)
    public String dir;

    @Parameter(names = "-rep", description = "Replication of users")
    public int replication = 1;

    @Parameter(names = "-protect", description = "Percentage of traces to protect.", required = true)
    public int protect;

    @Parameter(names = "-epsilon", description = "Value (double) of epsilon.")
    public double epsilon = 1;

    @Parameter(names = "-rows", description = "Number of rows in sketch.")
    public int rows = 256;

    @Parameter(names = "-col-multiply", description = "Multiply columns by the supplied number.")
    public int col_mul = 1;

    @Parameter(names = "-progress", description = "Print the progress of builder sketches.")
    public boolean print_progress = false;

    @Parameter(names = "-enter", description = "Enter/exit trace analysis. Default is call chain analysis.")
    public boolean enter_exit_trace = false;

    @Parameter(names = "-hot", description = "Hide the hotness. Default is hiding the presence.")
    public boolean hot = false;
}
