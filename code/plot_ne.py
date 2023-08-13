#!/usr/bin/env python3

import numpy as np
import matplotlib.pyplot as plt
from statistics import mean
import sys
import collections

def main():
    global ne_pr25, ne_pr50, ne_pr75, apps
    apps = ['barometer', 'bible', 'dpm', 'drumpads', 'equibase', 'localtv', 'loctracker', 'mitula', 'moonphases',
            'parking', 'parrot', 'post', 'quicknews', 'speedlogic', 'vidanta']

    ne_pr25 = read_data_for_all_epsilons('output_cc_presence_pr25')
    ne_pr50 = read_data_for_all_epsilons('output_cc_presence_pr50')
    ne_pr75 = read_data_for_all_epsilons('output_cc_presence_pr75')
    plot('fig4_2.pdf')

    ne_pr25 = read_data_for_all_epsilons('output_cc_hotness_pr25')
    ne_pr50 = read_data_for_all_epsilons('output_cc_hotness_pr50')
    ne_pr75 = read_data_for_all_epsilons('output_cc_hotness_pr75')
    plot('fig4_4.pdf')

    ne_pr25 = read_data_for_all_epsilons('output_eet_presence_pr25')
    ne_pr50 = read_data_for_all_epsilons('output_eet_presence_pr50')
    ne_pr75 = read_data_for_all_epsilons('output_eet_presence_pr75')
    plot('fig4_3.pdf')

    ne_pr25 = read_data_for_all_epsilons('output_eet_hotness_pr25')
    ne_pr50 = read_data_for_all_epsilons('output_eet_hotness_pr50')
    ne_pr75 = read_data_for_all_epsilons('output_eet_hotness_pr75')
    plot('fig4_5.pdf')


def read_data_for_all_epsilons(file_prefix):
    data = {}
    ne_0_5 = read_data(file_prefix + '_ep0_5.txt')
    ne_1_0 = read_data(file_prefix + '_ep1_0.txt')
    ne_2_0 = read_data(file_prefix + '_ep2_0.txt')

    for app in apps:
        data[app] = [ne_0_5[app], ne_1_0[app], ne_2_0[app]]

    return data

def read_data(file_name):
    data = {}
    runs = 30
    with open(file_name, 'r') as f:
        for line in f.readlines():
            if 'replication' in line:
                app = line.split(' ')[1]
            elif line.startswith('RE:') and (',' not in line):
                assert 'app' in locals()
                ne = float(line.split(' ')[1])
                if app not in data:
                    data[app] = []
                data[app].append(ne)
                del app
    for app in apps:
        assert len(data[app]) == runs, app
        data[app] = mean(data[app])
    return data

def plot(fig_file_name):
    fig, axs = plt.subplots(3, 5)
    for i in range(3):
        for j in range(5):
            if i*5+j >= len(apps):
                continue
            app = apps[i * 5 + j]
            axs[i,j].set_title(app)
            axs[i,j].plot(ne_pr25[app], 'o-', label='x = 25', mfc='none')
            axs[i,j].plot(ne_pr50[app], 'v-', label='x = 50', mfc='none')
            axs[i,j].plot(ne_pr75[app], 's-', label='x = 75', mfc='none')

            # axs[i,j].set_ylim(None, ylim)
            if i == 2:
                axs[i,j].set_xlabel(u'log\u2082\u03B5', size='large')
            axs[i,j].set_xticks([0,1,2])
            axs[i,j].set_xticklabels([-1, 0, 1])
            if j == 0:
                axs[i,j].set_ylabel('NE', size='large')

    handles, labels = axs[0,0].get_legend_handles_labels()
    fig.legend(handles, labels, loc='upper center', ncol=3)

    fig.tight_layout(pad=0.1)
    # plt.show()
    plt.savefig(fig_file_name)

if __name__ == '__main__':
    main()
