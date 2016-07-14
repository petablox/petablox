#!/usr/bin/python

import xml.etree.ElementTree as Et
#import networkx as nx
import subprocess
import argparse
#import numpy
import sys
import os
from loadDB import *

verbosity = 2

def printv(strarg, vint):
    if verbosity > vint:
        print strarg

def parseargs():
    parser = argparse.ArgumentParser(description='Post-processing for classification, report generation, and application mapping')
    parser.add_argument('-c', type=str, metavar="<SrcClassFile>", help="Path to src classification file")
    parser.add_argument('-k', type=str, metavar="<SinkClassFile>", help="Path to sink classification file")
    parser.add_argument('-a', type=str, metavar="<APK Dir>", help="Directory to analyze apks")
    parser.add_argument('-m', type=str, metavar="<Results>", help="Dir of flow results for mapping")
    parser.add_argument('-r', type=str, metavar="<Results>", help="Dir of flow results for report")
    parser.add_argument('-o', type=str, metavar="<OutputDir>", help="Path to directory to store results")


    args = parser.parse_args()
    return (args.a, args.m, args.c, args.k, args.o, args.r)


def getAppName(root):
    print root
    pathList = root.split('/')

    try:
        pathList.remove('Build')
    except ValueError:
        pass 
    try:
        pathList.remove('bin')
    except ValueError:
        pass 

    appName = pathList[-1]
    
    return appName


"""Find apks"""
def findAPKSrc(rootdir):
    print "[Processing apps]"

    manifestDirs = []
    apkDirs = [] 
    apps = {}
    appName = ""

    for root, subFolders, files in os.walk(os.path.abspath(rootdir)):

        # Look for apks
        for f in files:
            if f.lower().endswith('.apk'):

                appName = f

                if not root.endswith('/'):
                    root = root + '/'
                apkDirs.append(root)

                if appName.strip() == '':
                    continue

                printv(appName + " APK: " + root + f,1)

                if appName in apps.keys():
                    apps[appName] += (root + f,)
                else:
                    apps[appName] = (root,)

        # Look for android manifests
        if "AndroidManifest.xml" in files:
            appName = getAppName(root)

            pathList = root.split('/')
            if 'bin' in pathList:
                continue 

            if not root.endswith('/'):
                root = root + '/'
            manifestDirs.append(root)

            printv(appName + " manifest: " + root, 1)

            if appName in apps.keys():
                apps[appName] += (root,)
            else:
                apps[appName] = (root,)
            
    return (apps, manifestDirs, apkDirs)


"""Run STAMP on APKs"""
def processAPK(pathtoapk):
    # max_time = 30
    # signal.signal(signal.SIGALRM, handle_alarm)

    # Ignore non-apks
    if not pathtoapk.endswith('.apk'):
        print "Not an apk: " + pathtoapk[-1:-3]
        return False

    # Execute stamp on an apk using dexpler
    exec_proc_dexpler = subprocess.call(["stamp" + " analyze " +  pathtoapk], 
                                        stderr=subprocess.STDOUT,
                                        shell=True)
    return True


"""Runs STAMP on source"""
def processSrc(pathtosrc):

    # Execute stamp on src
    exec_proc = subprocess.call(["stamp" + " analyze " +  pathtosrc], 
                                stderr=subprocess.STDOUT,
                                shell=True)
    return True


"""Extract flows from reports"""
def getFlows(reportpath):
    printv("Extracting Flow Data", 1)

    appDict = {}
    flows = []
    src = ""
    sink = ""
    
    for root, subFolders, files in os.walk(os.path.abspath(reportpath)):
        flows = []
        if "SrcSinkFlow.xml" in files:
            tree = Et.parse(root + '/' + 'SrcSinkFlow.xml')
            rootXML = tree.getroot()

            for tpl in rootXML.iter('tuple'):
                storeFlow = False

                if 'source' in tpl.attrib:
                    src = tpl.attrib['source']

                if 'sink' in tpl.attrib:
                    sink = tpl.attrib['sink']
                    storeFlow = True

                if storeFlow:
                    flows.append((src,sink))
                    printv((src,sink),4)

            appDict[root] = flows

    flowHash = {}
    for v in appDict.keys():
        appName = v.split('/')[-2].split('_')[-1].replace('.apk',"").lower()

        s = set()
        for f in appDict[v]:
            pair = str(f).strip('(').replace("'","").strip(')').replace("'","").split(',')
            s.add((pair[0].strip(' ').lower(),pair[1].strip(' ').lower()))

        flowHash[appName] = s

    for k in flowHash.keys():
        print k
        for v in flowHash[k]:
            print v
    return flowHash


"""Classify sinks"""
def sinkClass(sinkDict, sink):
    return sinkDict.get(sink.lower(), "NoClass")


"""Classify sources"""
def srcClass(srcDict,src):
    return srcDict.get(src.lower(), "NoClass")


"""Classify contexts"""
def classifyContext(ctx, contextDict):
    return contextDict.get(ctx.lower(), "App")


"""Classify flows into one level of hierarchy"""
def classifyFlows(app, flows, srcClassDict, sinkClassDict):

    flowClass = []
    modifier = {}

    # Classify flows
    for f in flows:
        src = f[0]
        sink = f[1]

        flowC = ""
        srcClasswoPriority = ""
        srcPriority = "low"
        srcDesc = ""

        srcC = srcClass(srcClassDict, src)
        if isinstance(srcC, tuple):
            srcClasswoPriority = str(srcC[0])
            srcPriority = str(srcC[1])
        else:
            srcClasswoPriority = str(srcC)

        sinkC = sinkClass(sinkClassDict, sink)

        if sinkC == "offdevice":
            flowC = "privacy"
        elif srcClasswoPriority == "untrusted" and sinkC == "sensitiveAPI":
            flowC = "integrity"
        elif sinkC =="cipher":
            modifier[src] = "encrypted"
            continue 
        else:
            flowC = "other"

        flowClass.append((app,src.replace("$","").title(),srcClasswoPriority,sink.replace("!","").title(),sinkC,flowC))

    # Set modifiers

    flowClassModifiers = []
    for f in flowClass:
        if f[1] in modifier.keys():
            if modifier[f[1]] == "encrypted":
                flowClassModifiers.append(f + ("encrypted",))
        else:
            flowClassModifiers.append(f + ("unencrypted",))

    return flowClassModifiers


"""Output flows in XML format"""
def writeFlowClassXML(flowClass, outpath):
    root = Et.Element('root')
    for f in flowClass:
        t = Et.SubElement(root, 'tuple')

        src = Et.SubElement(t, "source")
        src.text = f[0]

        if isinstance(f[1], tuple):
            src.set('class',f[1][0])
            src.set('priority',f[1][1])
        else:
            src.set('class',f[1])

        sink = Et.SubElement(t, "sink")
        sink.text = f[2]
        sink.set('class',f[3])

    tree = Et.ElementTree(root)
    tree.write(outpath + "classifiedFlows.xml")
    return True


"""Read source classification data from xml file"""
def readSrcClassXML(xmlf):
    srcClassDict = {}

    tree = Et.parse(xmlf)
    root = tree.getroot()
    
    for src in root.iter('src'):
        if not src.get('priority') is None:
            srcClassDict[src.text.lower()] = (src.get('class').lower(),src.get('priority').lower())
        else:
            srcClassDict[src.text.lower()] = (src.get('class').lower())
        
    return srcClassDict


"""Read sink classification data from xml file"""
def readSinkClassXML(xmlf):
    sinkClassDict = {}

    tree = Et.parse(xmlf)
    root = tree.getroot()
    
    for sink in root.iter('sink'):
        sinkClassDict[sink.text.lower()] = (sink.get('class').lower())
        
    return sinkClassDict


# """Build graph representation of relationships between apps"""
# def buildAppMap(flowHash):

#     numApps = len(flowHash)
#     print "Analyzed results from " + str(numApps) + " apps"

#     icount = 0;
#     jcount = 0;
#     G=nx.Graph()

#     # Add nodes
#     for i in flowHash.keys():
#         G.add_node(i)

#     # Add edges
#     for i in flowHash.keys():
#         for j in flowHash.keys():
#             if not i == j:
#                 printv(str(i) + " (" + str(len(flowHash[i])) + " flows) vs. " + str(j) + " (" + str(len(flowHash[j])) + " flows)", 1)
                
#                 if flowHash[i].intersection(flowHash[j]):
#                     appDiff = flowHash[i].symmetric_difference(flowHash[j])
#                     inter = flowHash[i].intersection(flowHash[j])
#                     printv("Sym-diff: " + str(len(appDiff)) + " Intersection: " + str(len(inter)), 2)
#                     if len(inter) > 0:
#                         G.add_edge(str(i),str(j),weight=len(inter))
#                 elif flowHash[i].isdisjoint(flowHash[j]):
#                     printv("--disjoint\n" ,2)
#                 else:
#                     printv("--error", 2)

#             icount+= 1;
#             jcount+= 1;

#     nx.write_gexf(G,"test.gexf")


""" Store source sink flow pairs into DFA results table"""
def storeDFAResults(dbPath,flowData):
    conn = createDFATable(dbPath)
    insertDFATable(conn,flowData)
    #selectDFATable(conn)


if __name__ == "__main__":

    (apk_dir,resultsDir,srcClassFile,sinkClassFile,outputPath,reportAPKs) = parseargs()

    srcDict = {}
    sinkDict = {}

    # Set output path
    if not outputPath is None:
        outputPath = os.path.abspath(outputPath)
    else:
        outputPath = os.path.abspath(".")

    if not outputPath.endswith("/"):
        outputPath = outputPath + "/"

    # Read src flow class data from XML file
    if not srcClassFile is None:
        srcDict = readSrcClassXML(srcClassFile)

    # Read sink flow class data from XML file
    if not sinkClassFile is None:
        sinkDict = readSinkClassXML(sinkClassFile)

    # Run analysis on apks and source
    if not apk_dir is None:
        (apps, manifestDirs, apkDirs) = findAPKSrc(apk_dir)

        for k in apps.keys():
            if k.endswith('.apk'):
                print "Processing APK: " + k
                processAPK(apps[k][0] + k)

    flowHash = {}
    # # Map relationships between apps
    # if not resultsDir is None:
    #     flowHash = getFlows(resultsDir)
    #     buildAppMap(flowHash)

    # Classify results and store in a database
    if not reportAPKs is None:
        flowHash = getFlows(reportAPKs)
        for app in flowHash.keys():
            flowClass = classifyFlows(app, flowHash[app], srcDict, sinkDict)
            storeDFAResults(outputPath,flowClass)

    
            
