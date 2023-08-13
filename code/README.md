This is the guide for building and running the artifact, and reproducing the experimental results described in the evaluation of Chapter 4.

## Code Structure
The project is a [Gradle](https://gradle.org/) project.

- `src` contains the source code.
- `run.sh` is a start-up script for running the program.
- `plot_ne.py` is a python script that plots the figures of normalized error.
- `plot_hh.py` is a python script that plots the figures of recall and precision.
- Other files/folders are for Gradle.

## Prerequisites

- Java 11+
- Python 3 with [matplotlib](https://matplotlib.org/) and [numpy](https://numpy.org/) for plotting
- Unix-like OS

## Run
### Clone the repository

```bash
$ git clone https://github.com/presto-osu/dp-trace-freq.git
$ cd dp-trace-freq
```

### Download the dataset
Download and extract [traces.tar.gz](https://github.com/presto-osu/ecoop21/releases/download/dataset/traces.tar.gz) to `dp-trace-freq/`:

```bash
$ wget https://github.com/presto-osu/ecoop21/releases/download/dataset/traces.tar.gz
$ tar -xzvf traces.tar.gz
```

The dataset is in the directory named `traces`. For each app evaluated in the paper, there's a sub-directory in it which contains 1000 low-level traces for 1000 users simulated using monkey. Each low-level trace is a sequence of "Enter" and "Exit" events for methods, where each method is denoted by a unique ID. Our experiments are conducted using these low-level traces. Specifically, the traces referred to by the paper, i.e., call chains and enter/exit traces, are parsed from the low-level traces. Besides the low-level traces, there're also three other files in each sub-directory: `callpairs` contains the calling relationship between methods; `v` contains the number of methods of the app; and `list` contains the name of trace files.


### Build
Before running the experiments, make sure you build the project first using the following commands:

```bash
$ cd code
$ ./gradlew shadowJar
```

### Usage
Instead of running the analyses by invoking the `java` command, we provide a wrapping script `run.sh` for convinience. The instructions for reproducing the results are based on this script. Here's the description about how to use the script.

The fisrt two parameters have to be set at the beginning.

| Parameter | Description |
|------|--------------|
| `APP_LIST` | Required. Has to be set first. The list of app names separated in spaces and in quotes.|
| `NUM_OF_RUNS` | Required. Has to be set second. The number of times to repeat the run.|
| `-dir PATH` | Required. The path to the trace dir.|
| `-rep VALUE` | Optional. # of replication per user. E.g., `-r 10` means 10000 users. Default is 1, i.e., 1000 users.|
| `-protect VALUE` | Required. The precentage of traces to be protected. E.g. `-protect 50` means 50% will be procted.|
| `-hot` | Optional. By default, the presence of traces is hidden. To choose to hide the hotness, use this flag.|
| `-epsilon VALUE` | Optional. Value for the privacy budget. Default is 1.0.|
| `-enter` | Optional. If set, run the program on enter/exit traces. By default, the program runs on call chains.|
| `-depth VALUE` | Optional. The depth limit of traces. Default is 100.|
| `-rows VALUE` | Optional. The number of rows in count sketch. Default is 256.|
| `-col-multiply VALUE` | Optional. To increase the number of columns in count sketch by VALUE times. Default is 1.|


**Example:**
The following command runs the call-chain analysis for `drumpads` and `barometer` on 1000 users for 3 times, using `ε=2.0` and protecting `50%` presence:

```bash
$ bash run.sh 'drumpads barometer' 3 -dir ../traces -protect 50 -epsilon 2.0
```

**Run on all apps**
The file `apps` contains the list of all the apps evaluated in the paper. To run the experiments on all the apps, pass ``"`cat apps`"`` as the first paramter when invoke the script.

**Save the result**
To save the result printed by the experiments to a file, append ` | tee FILE_PATH` to the command.

## Reproducing The Results
To reproduce the results presented in the evaluation section of Chapter 4. Run the following commands.
```bash
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 0.5 | tee output_cc_presence_pr25_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 1.0 | tee output_cc_presence_pr25_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 2.0 | tee output_cc_presence_pr25_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 0.5 | tee output_cc_presence_pr50_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 1.0 | tee output_cc_presence_pr50_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 2.0 | tee output_cc_presence_pr50_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 0.5 | tee output_cc_presence_pr75_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 1.0 | tee output_cc_presence_pr75_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 2.0 | tee output_cc_presence_pr75_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 0.5 -hot | tee output_cc_hotness_pr25_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 1.0 -hot | tee output_cc_hotness_pr25_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 2.0 -hot | tee output_cc_hotness_pr25_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 0.5 -hot | tee output_cc_hotness_pr50_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 1.0 -hot | tee output_cc_hotness_pr50_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 2.0 -hot | tee output_cc_hotness_pr50_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 0.5 -hot | tee output_cc_hotness_pr75_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 1.0 -hot | tee output_cc_hotness_pr75_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 2.0 -hot | tee output_cc_hotness_pr75_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 0.5 -enter -depth 20 | tee output_eet_presence_pr25_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 1.0 -enter -depth 20| tee output_eet_presence_pr25_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 2.0 -enter -depth 20| tee output_eet_presence_pr25_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 0.5 -enter -depth 20| tee output_eet_presence_pr50_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 1.0 -enter -depth 20| tee output_eet_presence_pr50_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 2.0 -enter -depth 20| tee output_eet_presence_pr50_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 0.5 -enter -depth 20| tee output_eet_presence_pr75_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 1.0 -enter -depth 20| tee output_eet_presence_pr75_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 2.0 -enter -depth 20| tee output_eet_presence_pr75_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 0.5 -hot -enter -depth 20| tee output_eet_hotness_pr25_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 1.0 -hot -enter -depth 20| tee output_eet_hotness_pr25_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 25 -epsilon 2.0 -hot -enter -depth 20| tee output_eet_hotness_pr25_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 0.5 -hot -enter -depth 20| tee output_eet_hotness_pr50_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 1.0 -hot -enter -depth 20| tee output_eet_hotness_pr50_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 50 -epsilon 2.0 -hot -enter -depth 20| tee output_eet_hotness_pr50_ep2_0.txt

$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 0.5 -hot -enter -depth 20| tee output_eet_hotness_pr75_ep0_5.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 1.0 -hot -enter -depth 20| tee output_eet_hotness_pr75_ep1_0.txt
$ bash run.sh "`cat apps`" 30 -dir ../traces -protect 75 -epsilon 2.0 -hot -enter -depth 20| tee output_eet_hotness_pr75_ep2_0.txt
```

To plot the figures, run the following:
```bash
$ python3 plot_ne.py
$ python3 plot_hh.py
```
