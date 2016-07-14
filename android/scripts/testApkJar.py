#!/usr/bin/python

import xml.etree.ElementTree as Et
import subprocess
import argparse
import signal
import sys
import os

verbosity = 2

def printv(strarg):
    if verbosity > 1:
        print strarg

def parseargs():
    parser = argparse.ArgumentParser(description='Classify flows and compare apk versus src results')
    parser.add_argument('-c', type=str, metavar="<SrcClassFile>", help="Path to src classification file")
    parser.add_argument('-k', type=str, metavar="<SinkClassFile>", help="Path to sink classification file")
    parser.add_argument('-d', type=str, metavar="<Dir>", help="Path to directory containing apks and code")
    parser.add_argument('-r', type=str, metavar="<Results>", help="Path to results directory for classification")
    parser.add_argument('-o', type=str, metavar="<OutputDir>", help="Path to output classification results")

    args = parser.parse_args()
    return (args.d, args.r, args.c, args.k, args.o)


def getAppName(root):
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


"""Find apks and source dirs"""
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
                
                appName = getAppName(root)

                if not root.endswith('/'):
                    root = root + '/'
                apkDirs.append(root + f)

                if appName.strip() == '':
                    continue

                printv(appName + " APK: " + root + f)

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

            printv(appName + " manifest: " + root)

            if appName in apps.keys():
                apps[appName] += (root,)
            else:
                apps[appName] = (root,)
            
    return (apps, manifestDirs, apkDirs)


"""Kill long running stamp job"""
def handle_alarm(signum, frame):
    # if triggered, kill long running job
    print "Signal " + signum + " " + frame


"""Run STAMP on APKs"""
def processAPK(pathtoapk):
    # max_time = 30
    # signal.signal(signal.SIGALRM, handle_alarm)

    # Ignore non-apks
    if not pathtoapk.endswith('.apk'):
        print "Not an apk: " + pathtoapk[-1:-3]
        return False

    # Execute stamp on an apk using dex2jar
    # exec_proc_dex2jar = subprocess.call(["stamp -Dstamp.d2j=dex2jar" + " analyze " +  pathtoapk], 
    #                                     stderr=subprocess.STDOUT,
    #                                     shell=True)

    # Execute stamp on an apk using dexpler
    exec_proc_dexpler = subprocess.call(["stamp" + " analyze " +  pathtoapk], 
                                        stderr=subprocess.STDOUT,
                                        shell=True)
    return True


"""Runs STAMP on source"""
def processSrc(pathtosrc):
    # max_time = 30
    # signal.signal(signal.SIGALRM, handle_alarm)

    # Execute stamp on src
    exec_proc = subprocess.call(["stamp" + " analyze " +  pathtosrc], 
                                stderr=subprocess.STDOUT,
                                shell=True)
    return True

"""Extract flows from reports"""
def getFlows(reportpath):
    #printv("[Comparing Results]")

    flows = []
    src = ""
    sink = ""
    
    for root, subFolders, files in os.walk(os.path.abspath(reportpath)):

        if "SrcSinkFlow.xml" in files:
            tree = Et.parse(root + '/' + 'SrcSinkFlow.xml')
            root = tree.getroot()

            for tpl in root.iter('tuple'):
                first = True
                for val in tpl.iter('value'):
                    for lbl in val.iter('label'):

                        if first is True: 
                            src = lbl.text.strip()
                            first = False
                        else:
                            sink = lbl.text.strip()

                flows.append((src,sink))
    return flows


"""Classify sinks"""
def sinkClass(sinkDict, sink):
    return sinkDict.get(sink.lower(), "Error:NoClass")


"""Classify sources"""
def srcClass(srcDict,src):
    return srcDict.get(src.lower(), "Error:NoClass")


"""Classify flows into one level of hierarchy"""
def classifyFlows(flows, srcClassDict, sinkClassDict):

    flowClass = []

    for f in flows:
        src = f[0]
        sink = f[1]

        srcC = srcClass(srcClassDict, src)
        sinkC = sinkClass(sinkClassDict, sink)

        flowClass.append((src,srcC,sink,sinkC))

    return flowClass


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


"""Dump internal dictionaries to XML files.  For development use only"""
def dumpFlowClassXML(xmlf):

    sinkDict = {
        '!internet': "OffDevice",
        '!sendtextmessage': "OffDevice",
        '!sendmultiparttextmessage':"OffDevice",
        '!senddatamessage': "OffDevice",
        '!socket': "OffDevice",
        '!socketaddr': "OffDevice",
        '!webview': "OffDevice",
        '!browser_history.modify': "OnDevice",
        '!browser_searches.modify': "OnDevice",
        '!file': "OnDevice",
        '!intent': "OnDevice",
        '!log': "OnDevice"}


    root = Et.Element('root')

    for k in sinkDict.keys():
        sink = Et.SubElement(root, "sink")
        sink.text = k
        sink.set('class', sinkDict[k])
    tree = Et.ElementTree(root)
    tree.write("sinkClass.xml")

    srcDict = {
        '$getdeviceid': ('Phone Info'),
        '$getnetworkcountryiso':('Phone Info'),
        '$getsimserialnumber':('Phone Info'),
        '$getsubscriberid':('Phone Info'),
        '$getline1number':('Phone Info'),
        '$getvoicemailnumber':('Phone Info'),
        '$cmda_system_id':('Phone Info'),
        '$cdma_network_id':('Phone Info'),
        '$calendar':('Personal Data'),
        '$browser':('Personal Data'),
        '$accounts':('Personal Data'),
        '$contacts':('Personal Data'),
        '$audio':('Personal Data'),
        '$camera.picture':('Personal Data'),
        '$media':('Personal Data'),
        '$getlatitude':('Location'),
        '$getlongitude':('Location'),
        '$fine_Location':('Location'),
        '$cdma_Location':('Location'),
        '$externalstorage':('System Data','high'),
        '$filesystem':('System Data','high'),
        '$systemproperties.get':('System Data','med'),
        '$process.exitvalue':('System Data','low'),
        '$process.errorstream':('System Data','med'),
        '$process.inputstream':('System Data','med'),
        '$wifimanager.getdhcpinfo':('System Data','low'),
        '$fileinputstream':('System Data','high'),
        '$datainputstream':('System Data','high'),
        '$getdevicesoftwareversion':('System Data','low'),
        '$context.getstring':('System Data','low'),
        '$getextras':('Ignore'),
        '$content_provider':('Ignore')}

    root = Et.Element('root')

    for k in srcDict.keys():
        src = Et.SubElement(root, "src")
        src.text = k
        if isinstance(srcDict[k], tuple):
            src.set('class', srcDict[k][0])
            src.set('priority', srcDict[k][1])
        else:
            src.set('class', srcDict[k])

    tree = Et.ElementTree(root)
    tree.write("srcClass.xml")
    


if __name__ == "__main__":

    (rootdir,resultsdir,srcClassFile,sinkClassFile,outputPath) = parseargs()

    srcDict = {}
    sinkDict = {}

    if not outputPath is None:
        outputPath = os.path.abspath(outputPath)
    elif not resultsdir is None:
        outputPath = os.path.abspath(resultsdir)
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
    if not rootdir is None:
        (apps, manifestDirs, apkDirs) = findAPKSrc(rootdir)

        for k in apps.keys():
            print k + ":"
            if len(apps[k]) >= 2:
                for k in apps[k]:
                    if k.endswith('.apk'):
                        print "Processing APK: " + k
                        processAPK(k)
                    else:
                        print "Processing SRC: " + k
                        processSrc(k)
            else:
                print "Fail"

    # Process results and classify flows
    if not resultsdir is None:
        flows = getFlows(resultsdir)
        flowClass = classifyFlows(flows, srcDict, sinkDict)
        writeFlowClassXML(flowClass,outputPath)

