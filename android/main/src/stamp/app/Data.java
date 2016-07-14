package stamp.app;

public class Data
{
	public String scheme;
	public String host;
	public String port;
	public String path;
	public String pathPattern;
	public String pathPrefix;
	public String mimeType;
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		boolean first = true;
		if(scheme != null){
			first = false;
			builder.append("\"scheme\": \""+scheme+"\"");
		}
		if(host != null){
			if(!first) builder.append(", "); first = false;
			builder.append("\"host\": \""+host+"\"");
		}
		if(port != null){
			if(!first) builder.append(", "); first = false;
			builder.append("\"port\": \""+port+"\"");
		}
		if(path != null){
			if(!first) builder.append(", "); first = false;
			builder.append("\"path\": \""+path+"\"");
		}
		if(pathPattern != null){
			if(!first) builder.append(", "); first = false;
			builder.append("\"pathPattern\": \""+pathPattern+"\"");
		}
		if(pathPrefix != null){
			if(!first) builder.append(", "); first = false;
			builder.append("\"pathPrefix\": \""+pathPrefix+"\"");
		}
		if(mimeType != null){
			if(!first) builder.append(", "); first = false;
			builder.append("\"mimeType\": \""+mimeType+"\"");
		}
		builder.append("}");
		return builder.toString();
	}
}