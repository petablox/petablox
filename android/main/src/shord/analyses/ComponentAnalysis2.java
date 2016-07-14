package shord.analyses;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.AbstractJasminClass;

import soot.*;
import soot.jimple.*;
import soot.util.*;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.jimple.internal.VariableBox;
import soot.tagkit.LinkTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.*;
import java.util.*;
import stamp.analyses.ReachingDefsAnalysis;
import stamp.harnessgen.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.NumberedSet;

import shord.program.Program;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import shord.project.analyses.ProgramDom;
import shord.project.ClassicProject;

import stamp.analyses.SootUtils;
import stamp.app.Component;
import stamp.app.Layout;
import stamp.app.App;
import stamp.app.IntentFilter;
import stamp.app.Data;

import chord.project.Chord;

import java.util.jar.*;
import java.io.*;

/**
 * Generating relations related to ICCG.
 * @author Yu Feng (yufeng@cs.stanford.edu)
 * @author Saswat Anand
 */

@Chord(name="comp-java-2",
       produces={"COMP",
                 "CSC", "MC", "ICCMeth",
                 "IntentTgtField", "ActionField", "DataTypeField", 
				 "TgtAction", "TgtDataType"
                },
       namesOfTypes = {"COMP"},
       types = {DomComp.class},
       namesOfSigns = {"CSC", "MC", "ICCMeth",
                       "IntentTgtField", "ActionField", "DataTypeField", 
                       "TgtAction", "TgtDataType"},
       signs = {"COMP0,SC0:COMP0_SC0", "M0,COMP0:M0_COMP0", "M0,Z0:M0_Z0",
                "F0:F0", "F0:F0", "F0:F0", 
                "COMP0,SC0:COMP0_SC0", "COMP0,SC0:COMP0_SC0"
               }
       )
public class ComponentAnalysis2 extends JavaAnalysis
{
    private List<String> iccMeths = Arrays.asList(new String[] {
        "<android.content.ContextWrapper: void startActivity(android.content.Intent)>",
        "<android.content.ContextWrapper: void sendBroadcast(android.content.Intent)>",
        "<android.content.ContextWrapper: boolean bindService(android.content.Intent,android.content.ServiceConnection,int)>",
        "<android.content.ContextWrapper: android.content.ComponentName startService(android.content.Intent)>",
        "<android.content.ContextWrapper: void sendBroadcast(android.content.Intent,java.lang.String)>",
        "<android.content.ContextWrapper: void sendStickyBroadcast(android.content.Intent)>",
        "<android.content.ContextWrapper: void sendOrderedBroadcast(android.content.Intent,java.lang.String)>",
        "<android.content.ContextWrapper: void sendOrderedBroadcast(android.content.Intent,java.lang.String,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
        "<android.content.ContextWrapper: void sendStickyOrderedBroadcast(android.content.Intent,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
        "<android.content.ContextWrapper: void sendBroadcast(android.content.Intent)>",
        "<android.content.ContextWrapper: android.content.ComponentName startService(android.content.Intent)>",
        "<android.content.ContextWrapper: void sendBroadcast(android.content.Intent,java.lang.String)>",
        "<android.content.ContextWrapper: void sendStickyBroadcast(android.content.Intent)>",
        "<android.content.ContextWrapper: void sendOrderedBroadcast(android.content.Intent,java.lang.String)>",
        "<android.content.ContextWrapper: void sendOrderedBroadcast(android.content.Intent,java.lang.String,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
        "<android.content.ContextWrapper: void sendStickyOrderedBroadcast(android.content.Intent,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
		
        //"void startActivities(android.content.Intent[])",
        //"void startIntentSender(android.content.IntentSender,android.content.Intent,int,int,int)",
        "<android.app.Activity: void startActivityForResult(android.content.Intent,int)>",
        "<android.app.Activity: void startActivityForResult(android.content.Intent,int,android.os.Bundle)>",
        "<android.app.Activity: boolean startActivityIfNeeded(android.content.Intent,int)>",
        "<android.app.Activity: boolean startActivityIfNeeded(android.content.Intent,int,android.os.Bundle)>",
        "<android.app.Activity: boolean startNextMatchingActivity(android.content.Intent)>",
        "<android.app.Activity: boolean startNextMatchingActivity(android.content.Intent,android.os.Bundle)>",
        "<android.app.Activity: void startActivity(android.content.Intent)>",
        //"void setResult(int,android.content.Intent)",
        //"<android.app.Activity: startActivityFromChild(android.app.Activity,android.content.Intent,int)>",
        //"<android.app.Activity: void startActivityFromFragment(android.app.Fragment,android.content.Intent,int)>",
        //"<android.app.Activity: void startIntentSenderForResult(android.content.IntentSender,int,android.content.Intent,int,int,int)",
        //"<android.app.Activity: void startIntentSenderFromChild(android.app.Activity,android.content.IntentSender,int,android.content.Intent,int,int,int)",
        "<android.widget.TabHost$TabSpec: android.widget.TabHost$TabSpec setContent(android.content.Intent)>",
	"<android.test.mock.MockContext: void startActivity(android.content.Intent,android.os.Bundle)>",
	"<android.test.mock.MockContext: void sendBroadcast(android.content.Intent)>",
	"<android.test.mock.MockContext: void sendBroadcast(android.content.Intent,java.lang.String)>",
	"<android.test.mock.MockContext: void sendOrderedBroadcast(android.content.Intent,java.lang.String)>",
	"<android.test.mock.MockContext: void sendOrderedBroadcast(android.content.Intent,java.lang.String,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
	"<android.test.mock.MockContext: void sendBroadcastAsUser(android.content.Intent,android.os.UserHandle)>",
	"<android.test.mock.MockContext: void sendBroadcastAsUser(android.content.Intent intent,android.os.UserHandle,java.lang.String)>",
	"<android.test.mock.MockContext: void sendOrderedBroadcastAsUser(android.content.Intent,android.os.UserHandle,java.lang.String,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
	"<android.test.mock.MockContext: void sendStickyBroadcast(android.content.Intent)>",
	"<android.test.mock.MockContext: void sendStickyOrderedBroadcast(android.content.Intent,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
	"<android.test.mock.MockContext: void sendStickyBroadcastAsUser(android.content.Intent,android.os.UserHandle)>",
	"<android.test.mock.MockContext: void sendStickyOrderedBroadcastAsUser(android.content.Intent,android.os.UserHandle,android.content.BroadcastReceiver,android.os.Handler,int,java.lang.String,android.os.Bundle)>",
	"<android.test.mock.MockContext: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)>",
	"<android.test.mock.MockContext: boolean bindService(android.content.Intent,android.content.ServiceConnection,int)>",
	"<android.test.mock.MockContext: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)>",
	"<android.test.mock.MockContext: android.content.ComponentName startService(android.content.Intent)>",
        "<android.test.mock.MockContext: void startActivity(android.content.Intent)>",
		});

	private void populateIntentFields() 
	{
		ProgramRel relIntentTgtField = (ProgramRel) ClassicProject.g().getTrgt("IntentTgtField");
        relIntentTgtField.zero();
		ProgramRel relActionField = (ProgramRel) ClassicProject.g().getTrgt("ActionField");
        relActionField.zero();
		ProgramRel relDataTypeField = (ProgramRel) ClassicProject.g().getTrgt("DataTypeField");
        relDataTypeField.zero();

		SootClass klass = Program.g().scene().getSootClass("android.content.Intent");
		SootField nameField = klass.getFieldByName("name");
		SootField actionField = klass.getFieldByName("action");
		SootField dataTypeField = klass.getFieldByName("type");

		relIntentTgtField.add(nameField);
		relIntentTgtField.save();

		relActionField.add(actionField);
		relActionField.save();

        relDataTypeField.add(dataTypeField);
        relDataTypeField.save();
	}

	private void populateRels()
	{
		DomComp domComp = (DomComp) ClassicProject.g().getTrgt("COMP");

		App app = Program.g().app();

		List<Component> comps = new ArrayList();
		SystemComponents.add(comps);
		comps.addAll(app.components());

		for(Component comp : comps){
			domComp.add(comp.name);
		}

		domComp.save();

        ProgramRel relMC = (ProgramRel) ClassicProject.g().getTrgt("MC");
        relMC.zero();
        ProgramRel relCSC = (ProgramRel) ClassicProject.g().getTrgt("CSC");
        relCSC.zero();
        ProgramRel relTgtAction = (ProgramRel) ClassicProject.g().getTrgt("TgtAction");
        relTgtAction.zero();
        ProgramRel relTgtDataType = (ProgramRel) ClassicProject.g().getTrgt("TgtDataType");
        relTgtDataType.zero();

		for(Component comp : comps){
			
			relCSC.add(comp.name, comp.name);

			SootClass compClass = Scene.v().getSootClass(comp.name);
			if(compClass.declaresMethod("void <init>()")){
				SootMethod init = compClass.getMethod("void <init>()");
				relMC.add(init, comp.name);
			}

			for(Layout layout : comp.layouts){
				for(String cb: layout.callbacks){
					cb = "void " + cb + "(android.view.View)";
					if(compClass.declaresMethod(cb)){
						SootMethod cbMeth = compClass.getMethod(cb);
						relMC.add(cbMeth, comp.name);
					}
				}
			}
			
			for(IntentFilter filter : comp.intentFilters){
				for(String act : filter.actions){
					relTgtAction.add(comp.name, act);
				}

				for(Data dt : filter.data){
					if(dt.mimeType != null)
						relTgtDataType.add(comp.name, dt.mimeType);
				}
			}
		}

		relMC.save();
		relCSC.save();
		relTgtAction.save();
		relTgtDataType.save();
		
		ProgramRel relICCMeth = (ProgramRel) ClassicProject.g().getTrgt("ICCMeth");
        relICCMeth.zero();
		
		relICCMeth.save();
	}
	
	private void populateICCMeth()
	{
        ProgramRel relICCMeth = (ProgramRel) ClassicProject.g().getTrgt("ICCMeth");
        relICCMeth.zero();

		for(String methSig : iccMeths){
			if(!Scene.v().containsMethod(methSig))
				continue;
			SootMethod m = Scene.v().getMethod(methSig);
			relICCMeth.add(m, 1);
		}

		relICCMeth.save();
	}

    public void run()
    {
        populateIntentFields();
		populateRels();
		populateICCMeth();
    }
}
