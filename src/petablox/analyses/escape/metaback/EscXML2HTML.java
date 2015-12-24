package petablox.analyses.escape.metaback;

import petablox.analyses.alloc.DomH;
import petablox.analyses.heapacc.DomE;
import petablox.analyses.method.DomM;
import petablox.program.Program;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.OutDirUtils;
import petablox.project.analyses.JavaAnalysis;

@Petablox(name = "thresc-xml2html")
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