#!/usr/bin/python

import argparse
import os
from loadDB import *

def parseargs():
    parser = argparse.ArgumentParser(description='Post-processing for classification, report generation, and application mapping')
    parser.add_argument('-o', type=str, metavar="<OutputDir>", help="Path to directory to store results")


    args = parser.parse_args()
    return (args.o)

def storePolicyResults(path, init):
    conn = createPolicyTable(path)
    insertPolicyTable(conn,init)

if __name__ == "__main__":

    (outputPath) = parseargs()

    # Set output path
    if not outputPath is None:
        outputPath = os.path.abspath(outputPath)
    else:
        outputPath = os.path.abspath(".")

    if not outputPath.endswith("/"):
        outputPath = outputPath + "/"

    initPolicies = [["initPolicy",1,"source","srcParam","sink","sinkParam"],
                    ["Data Loss Prevention",1,"FILE",".*","INTERNET",".*"],
                    ["Data Loss Prevention",1,"CONTACTS",".*","INTERNET",".*"],
                    ["Data Loss Prevention",1,"FILE",".*","SMS",".*"]]
    storePolicyResults(outputPath, initPolicies)
