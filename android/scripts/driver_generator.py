from xml.dom.minidom import parse
import sys

##########################
#      Global Vars       #
##########################

intentFilterId = 0
callbackId		= 0
package = ""
tabs = "      "

ACTIVITY = 0
RECEIVER = 1
SERVICE = 2


############################
#     CLASS DEFINITONS     #
############################

class Node:
	def __init__(self, kind, name, exported):
		self.intentFilters = []
		self.kind = kind
		self.name = name
		self.exported = exported

	def addFilter(self, intentFilter):
		self.intentFilters.append(intentFilter)

class IntentFilter:
	def __init__(self):
		self.actions = []
		self.categories = []
		self.mimeType = ""

	def addAction(self, action):
		self.actions.append(action)

	def addCategory(self, category):
		self.categories.append(category)


############################
#   Processing Functions   #
############################

def handleIntentFilter(intentFilter):
	new_intentFilter = IntentFilter()

	# Intent-Filters contain <action>, <category>, and <data> tags
	for node in intentFilter.childNodes:

		# Only deal with Element Nodes. For some reason the parser produces Text Nodes.
		if(node.nodeType != node.ELEMENT_NODE):
			continue

		if(node.tagName == "action"):
			new_intentFilter.addAction(node.getAttribute("android:name"))
		if(node.tagName == "category"):
			new_intentFilter.addCategory(node.getAttribute("android:name"))
		if(node.tagName == "data"):
			new_intentFilter.mimeType = node.getAttribute("android:mimeType")

	return new_intentFilter


def handleNode(node, kind):
	new_node = Node(kind, processName(node.getAttribute("android:name")),
		node.getAttribute("android:exported"))

	for intentFilter in node.getElementsByTagName("intent-filter"):
		new_node.addFilter( handleIntentFilter(intentFilter) )

	return new_node


############################
#      Text Functions      #
############################

def driverPreamble():
	return """\
package edu.stanford.stamp;

import java.util.ArrayList;
import java.util.Random;

import edu.stanford.stamp.harness.drivers.ApplicationDriver;
import edu.stanford.stamp.harness.eventtriggers.CallbackTrigger;
import edu.stanford.stamp.harness.filters.EventFilter;
import edu.stanford.stamp.harness.misc.CallbackData;
import edu.stanford.stamp.harness.runnables.Callback;

public class Driver{
   public static void main(String[] args){
      final ApplicationDriver instance = ApplicationDriver.getInstance();
      ArrayList<CallbackTrigger> entries = new ArrayList<CallbackTrigger>();\n\n"""

def driverPostamble():
	return """\
   }
}\n"""

def intentFilterPreamble():
	return """\
      ArrayList<String> actionList%s = new ArrayList<String>();
      ArrayList<String> categoryList%s = new ArrayList<String>();
      CallbackData data%s = null;\n""" % (intentFilterId, intentFilterId, intentFilterId)

def activityPreamble(activityName):
	return """\
      Callback callback%s = new Callback("", false, Callback.ACTIVITY){
         public void execute(CallbackTrigger t){
            instance.launchActivity(new %s());
         }
      };\n\n""" % (callbackId, activityName)

def receiverPreamble(receiverName):
	return """\
      Callback callback%s = new Callback("", false, Callback.RECEIVER){
         public void execute(CallbackTrigger t){
            new %s().onReceive( instance.getRunningActivity(), t.toIntent())
         }
      };\n\n""" %(callbackId, receiverName)


def nodeText(node):
	global callbackId
	result = ""

	if(node.kind == ACTIVITY):
		result += activityText(node)
	if(node.kind == SERVICE):
		result += serviceText(node)
	if(node.kind == RECEIVER):
		result += receiverText(node)

	if(node.exported):
		result += tabs + "entries.add( callback%s.getMatchingTrigger() );\n" % callbackId

	callbackId += 1
	return result


def activityText(activity):
	# SAMPLE ACTIVITY OUTPUT
	# <activity android:name="ContactManager" android:label="@string/app_name">
	#
	# Callback callback0 = new Callback(null, false){
	#    public void execute(CallbackTrigger t){
	#       instance.createActivity(new ContactManager());
	#    }
  # };
	# // Add intent filters
	# instance.registerCallback(callback0);

	result = activityPreamble(activity.name)
	for intentFilter in activity.intentFilters:
		result += intentFilterText(intentFilter, activity.kind)
	result += tabs + "instance.registerCallback(callback%s);\n\n" % (callbackId)

	return result


def receiverText(receiver):
	# SAMPLE RECEIVER OUTPUT
	# <receiver android:name="MyReceiver">

	# Callback callback0 = new Callback(null, false){
	#    public void execute(CallbackTrigger t){
	#       MyReceiver.new().onReceive( instance.getRunningActivity(), t.toIntent())
	#    }
	# };
	# // Add intent filters
	# instance.registerCallback(callback0);

	result = receiverPreamble(receiver.name)
	for intentFilter in receiver.intentFilters:
		result += intentFilterText(intentFilter, receiver.kind)
	result += tabs + "instance.registerCallback(callback%s);\n" % (callbackId)

	return result

def intentFilterText(intentFilter, kind):
	# SAMPLE INTENT-FILTER OUTPUT
	# <intent-filter>
	#    <action android:name="android.intent.action.MAIN"/>
	#    <category android:name="android.intent.category.LAUNCHER"/>
	# </intent-filter>
	#
	# 	ArrayList<String> actionList0 = new ArrayList<String>();
	#	String category0 = "";
	#	CallbackData data0 = null;
	#	actionList0.add("android.intent.action.MAIN");
	#	category0 = "android.intent.category.LAUNCHER";
	#	EventFilter filter0 = new EventFilter(actionList0, category0, data0);
	#	callback0.setFilter(filter0);

	global intentFilterId

	result = intentFilterPreamble()
	for action in intentFilter.actions:
		result += tabs + "actionList%s.add(\"%s\");\n" % (intentFilterId, action)
	for category in intentFilter.categories:
		result += tabs + "categoryList%s.add(\"%s\");\n" % (intentFilterId, category)
	if(intentFilter.mimeType != ""):
		result += tabs + "data%s = new CallbackData(\"%s\");\n" % (intentFilterId, intentFilter.mimeType)

	result += tabs + "EventFilter filter%s = new EventFilter(actionList%s, categoryList%s, data%s);\n" % (
		intentFilterId, intentFilterId, intentFilterId, intentFilterId)

	result += tabs + "callback%s.addFilter(filter%s);\n\n" % (callbackId, intentFilterId)

	intentFilterId += 1
	return result


def appDrivingText():
	return """\
      while(true){
         int option = new Random().nextInt(5);

         if(option == 0){
            instance.broadcastUITrigger();
         }
         if(option == 1){
            instance.broadcastCallbackTrigger();
         }
         if(option == 2){
            instance.stopRunningActivity();
         }
         if(option == 3){
            instance.navigateToActivity();
         }
         if(option == 4){
            instance.killActivity();
         }
      }\n
"""

############################
#           MISC           #
############################

def processName(name):
	if(name[0] == "."): #
		return package + name
	# XXX: Temporary fix, to make ContactManager work
	# return name
	return package + '.' + name

def processKind(kind):
	if(kind == ACTIVITY):
		return "Callback.ACTIVITY"
	if(kind == RECEIVER):
		return "Callback.RECEIVER"
	if(kind == SERVICE):
		return "Callback.SERVICE"

############################
#           MAIN           #
############################

if len(sys.argv) < 3:
	print "Please supply a path to the manifest file and a filename for output"
	sys.exit()


outfile = open(sys.argv[2], 'w')
dom = parse(open(sys.argv[1], 'r'))
package = dom.getElementsByTagName('manifest')[0].getAttribute("package")

nodes = []
for activity in dom.getElementsByTagName('activity'):
	nodes.append( handleNode(activity, ACTIVITY) )
for receiver in dom.getElementsByTagName('receiver'):
	nodes.append( handleNode(receiver, RECEIVER) )

outfile.write(driverPreamble())

# This sets up the driver environment
for node in nodes:
	outfile.write(nodeText(node))

# This picks an entry point to the application
outfile.write("""
      entries.add(new CallbackTrigger(\"android.intent.action.MAIN\",
         \"android.intent.category.LAUNCHER\", null, Callback.ACTIVITY));\n""")

outfile.write(
	tabs + "instance.broadcastCallbackTrigger( entries.get(new Random().nextInt( entries.size()-1)));\n\n")

# This drives the application
outfile.write(appDrivingText())

outfile.write(driverPostamble())

