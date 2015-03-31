package chord.analyses.typestate.metaback;

import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "typestate-xml2html")
public class TSXML2HTML extends JavaAnalysis {
	public void run() {
		DomH domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);
		DomI domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);
		
        domH.saveToXMLFile();
		domM.saveToXMLFile();
        domV.saveToXMLFile();
        domI.saveToXMLFile();

        OutDirUtils.copyResourceByName("web/style.css");
        OutDirUtils.copyResourceByName("chord/analyses/method/Mlist.dtd");
        OutDirUtils.copyResourceByName("chord/analyses/method/M.xsl");
        OutDirUtils.copyResourceByName("chord/analyses/method/Mlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/var/Vlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/var/V.xsl");
		OutDirUtils.copyResourceByName("chord/analyses/invk/Ilist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/invk/I.xsl");
        OutDirUtils.copyResourceByName("chord/analyses/alloc/Hlist.dtd");
        OutDirUtils.copyResourceByName("chord/analyses/alloc/H.xsl");
        OutDirUtils.copyResourceByName("chord/analyses/typestate/metaback/web/results.dtd");
        OutDirUtils.copyResourceByName("chord/analyses/typestate/metaback/web/results.xml");
        OutDirUtils.copyResourceByName("chord/analyses/typestate/metaback/web/results.xsl");

        OutDirUtils.runSaxon("results.xml", "results.xsl");

        Program.g().HTMLizeJavaSrcFiles();
	}
}