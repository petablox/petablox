package stamp.summaryreport;

import stamp.app.Component;
import stamp.app.Data;
import stamp.app.IntentFilter;


public class ExportReport
{
	Main main;

	ExportReport(Main main)
	{
		this.main = main;
	}
	
	public void generate()
	{
		main.startPanel("Exported Components");
		main.println("<div class=\"list-group\">");
		for(Component comp : main.app.components()){
			if(!comp.exported)
				continue;
			if(comp.type == Component.Type.activity ||
			   comp.type == Component.Type.service){

				boolean skip = true;
				if(comp.type == Component.Type.activity){
					for(IntentFilter ifilter : comp.intentFilters){
						if(!ifilter.actions.contains("android.intent.action.MAIN"))
							skip = false;
					}
				}
				if(skip)
					continue;
				main.startPanel(comp.name, "default");

				for(IntentFilter ifilter : comp.intentFilters){
					main.println("<ul>");
					for(String act : ifilter.actions){
						if(!act.equals("android.intent.action.MAIN"))
							main.println(String.format("<li><b>Action:</b> %s</li>", act));
					}
					for(Data dt : ifilter.data)
						main.println(String.format("<li><b>Data:</b> %s</li>", dt.toString()));
					main.println("</ul>");
				}
				main.endPanel();
			}
		}
		main.println("</div>");
		main.endPanel();
	}
}