package chord.analyses.escape.metaback;

import chord.analyses.alloc.DomH;
import chord.analyses.heapacc.DomE;
import chord.analyses.method.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;

@Chord(name = "thresc-xml2html")
public class EscXML2HTML extends JavaAnalysis {
	public void run() {
		DomH domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		DomE domE = (DomE) ClassicProject.g().getTrgt("E");
		ClassicProject.g().runTask(domE);

        domH.saveToXMLFile();
		domM.saveToXMLFile();
        domE.saveToXMLFile();

        OutDirUtils.copyResourceByName("web/style.css");
        OutDirUtils.copyResourceByName("chord/analyses/method/Mlist.dtd");
        OutDirUtils.copyResourceByName("chord/analyses/method/M.xsl");
        OutDirUtils.copyResourceByName("chord/analyses/method/Mlist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/heapacc/Elist.dtd");
		OutDirUtils.copyResourceByName("chord/analyses/heapacc/E.xsl");
        OutDirUtils.copyResourceByName("chord/analyses/alloc/Hlist.dtd");
        OutDirUtils.copyResourceByName("chord/analyses/alloc/H.xsl");
        OutDirUtils.copyResourceByName("chord/analyses/escape/metaback/web/results.dtd");
        OutDirUtils.copyResourceByName("chord/analyses/escape/metaback/web/results.xml");
        OutDirUtils.copyResourceByName("chord/analyses/escape/metaback/web/results.xsl");

        OutDirUtils.runSaxon("results.xml", "results.xsl");

        Program.g().HTMLizeJavaSrcFiles();
	}
}