package petablox.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import petablox.project.Boot;
import petablox.project.Messages;

public class ResourceUtil {
	/*
	 * Get an InputStream object to a resource.
	 * The function will look into the directory specified by the property, if set,
	 * otherwise look inside the petablox.jar file
	 */
	public static InputStream getResource(String property,String fileName){
		String environment = System.getProperty(property);
		if(environment == null){
			// Find the file within the JAR file
			try{
		        JarFile jarFile = new JarFile(getPetabloxJarFile());
		        Enumeration e = jarFile.entries();
		        while (e.hasMoreElements()) {
		            JarEntry je = (JarEntry) e.nextElement();
		            String fileName2 = je.getName();
		            if (fileName2.contains(fileName)) {
		                InputStream is = jarFile.getInputStream(je);
		                return is;
		            }
		        }
			}catch(Exception e){}
		}else{
			// Find the file in the given path
			String filePath = environment+File.separator+fileName;
			File f = new File(filePath);
			if(!f.exists()){
				Messages.warn("WARN: ResourceUtil: File %s not found in %s", fileName,environment);
			}try{
				InputStream is = new FileInputStream(f);
				return is;
			}catch(Exception e){
				
			}
		}
		return null;
	}
	
	private static String getPetabloxJarFile() {
        String cname = Boot.class.getName().replace('.', '/') + ".class";
        URL url = Boot.class.getClassLoader().getResource(cname);
        if (!url.getProtocol().equals("jar"))
            Messages.fatal("ERROR: ResourceUtil: Expected Petablox to be loaded from petablox.jar instead of from '%s'.", url.toString());
        String file = url.getFile();
        return file.substring(file.indexOf(':') + 1, file.indexOf('!'));
    }
}
