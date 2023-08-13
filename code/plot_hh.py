#!/usr/bin/env python3

import numpy as np
import matplotlib.pyplot as plt
import sys
from statistics import mean
import collections

def main():
	global apps, runs
	runs = 30
	apps = ['barometer', 'bible', 'dpm', 'drumpads', 'equibase', 'localtv', 'loctracker', 'mitula', 'moonphases',
			'parking', 'parrot', 'post', 'quicknews', 'speedlogic', 'vidanta']

	recall, precision = read_all_data('output_cc_presence_')
	plot(recall, precision, 'fig4_6_a.pdf')

	recall, precision = read_all_data('output_cc_hotness_')
	plot(recall, precision, 'fig4_6_c.pdf')

	recall, precision = read_all_data('output_eet_presence_')
	plot(recall, precision, 'fig4_6_b.pdf')

	recall, precision = read_all_data('output_eet_hotness_')
	plot(recall, precision, 'fig4_6_d.pdf')


def read_all_data(file_name_prefix):
	recall = {}
	precision = {}
	for app in apps:
		recall[app] = []
		precision[app] = []
	for i in {'25', '50', '75'}:
		for j in {'0_5', '1_0', '2_0'}:
			file_name = file_name_prefix + 'pr' + i + '_ep' + j + '.txt'
			data = read_data(file_name)
			for app in apps:
				recall[app].append(data[app]['recall'])
				precision[app].append(data[app]['precision'])

	for app in apps:
		assert len(recall[app]) == 9
		assert len(precision[app]) == 9

	recall = np.array([mean(recall[app]) for app in apps])
	precision = np.array([mean(precision[app]) for app in apps])
	return recall, precision

def read_data(file_n):
	data={}
	with open(file_n, 'r') as f:
		for line in f.readlines():
			if 'replication' in line:
				app = line.split(' ')[1]
			elif 'recall' in line:
				assert 'app' in locals()
				recall = float(line.split(',')[2].split(':')[1])
				precision = float(line.split(',')[3].split(':')[1])
				if app not in data:
					data[app] = {}
					data[app]['recall'] = []
					data[app]['precision'] = []
				data[app]['recall'].append(recall)
				data[app]['precision'].append(precision)
				del app

	data_mean = {}
	for app in data.keys():
		assert len(data[app]['recall']) == runs, app
		assert len(data[app]['precision']) == runs, app
		data_mean[app] = {}
		data_mean[app]['recall'] = mean(data[app]['recall'])
		data_mean[app]['precision'] = mean(data[app]['precision'])
	return data_mean


def plot(recall, precision, fig_file_name):
	x = np.arange(len(apps))  # the label locations
	width = 0.4  # the width of the bars

	plt.rcParams.update({'font.size': 13})

	plt.bar(x - width/2, recall, width, label='recall')
	plt.bar(x + width/2, precision, width, label='precision')
	plt.axhline(1.0, c='gray', lw=1, zorder=1)
	plt.axhline(0.5, c='gray', lw=1, zorder=1)
	plt.yticks([0, 0.5, 1.0])
	plt.ylim(0, 1)
	plt.xticks(x, apps, rotation=75)

	plt.legend(bbox_to_anchor=(0.5, 1.0), loc='lower center', ncol=2)

	plt.suptitle(fig_file_name)
	plt.tight_layout()
	# plt.show()
	plt.savefig(fig_file_name)
	plt.clf()


if __name__ == '__main__':
	main()
