#!/usr/bin/env python
import sys
import os
import subprocess
import random
import time
import csv
import argparse

def parseargs():
    parser = argparse.ArgumentParser(description="Automatically run apps and determine their success/failure and time to complete")
    parser.add_argument("appdir", type=str, help="the directory of apps to select from. all apps should be at the top level and the directory should only contain apps")
    parser.add_argument("-o", "--output", type=str, help="output file; will be csv and will be opened in append mode", default="random_samples.csv")
    parser.add_argument("-s", "--stamp_loc", type=str, help="path to stamp script from here", default="../stamp")
    parser.add_argument("-i", "--iterations", type=int, help="number of iterations to run", default=1)
    args = parser.parse_args()
    return (args.appdir, args.output, args.stamp_loc, args.iterations)
    

if __name__ == "__main__":
        (appdir, output, stamp_loc, iterations) = parseargs()

	apps = os.listdir(appdir)
	num_apps = len(apps)
	print("There are "+str(num_apps)+" apps in the directory...randomly sampling...")

	already_sampled = []
	timings = {}
	w = csv.writer(open(output, "a"))

	for i in range(0,int(iterations)):
		j = random.randint(1, num_apps)
		while already_sampled.count(j) > 0:
			j = random.randint(1, num_apps)
			
		command = []
		command.append(stamp_loc)
		command.append('analyze')
		command.append(appdir+apps[j])
		print(' '.join(command))
		start_time = time.time()
		exit_code = subprocess.call(command)
		elapsed_time = time.time() - start_time
		timings[apps[j]] = elapsed_time
				
		already_sampled.append(j)
		w.writerow([apps[j], elapsed_time, exit_code])

	

