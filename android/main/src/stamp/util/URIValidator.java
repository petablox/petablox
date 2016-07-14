package stamp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class URIValidator
{ 
    private Pattern pattern;
 
    private static final String URI_PATTERN = "[a-zA-Z]*://.+";
 
    public URIValidator(){
		pattern = Pattern.compile(URI_PATTERN);
    }
 
    public boolean validate(final String ip){  
		return pattern.matcher(ip).matches();        
    }
}