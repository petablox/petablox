package petablox.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
	
	/*
	 * Get an InputStream object to a resource.
	 * The function return an inputstream pointing to <root>/<path>, if set,
	 * otherwise <petablox.jar>/<path>.
	 */
	public static InputStream getResourceByPath(String root,String path){
		String environment;
		if(root == null)
			environment = null;
		else
			environment = System.getProperty(root);
		if(environment == null){
			// Find the file within the JAR file
			try{
				JarFile jarFile = new JarFile(getPetabloxJarFile());
				JarEntry je = jarFile.getJarEntry(path);
				InputStream is = jarFile.getInputStream(je);
				return is;
			}catch(Exception e){}
		}else{
			// Find the file in the given path
			String filePath = environment+File.separator+path;
			File f = new File(filePath);
			if(!f.exists()){
				Messages.warn("WARN: ResourceUtil: File %s not found in %s", path,environment);
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
	
	/**
	 * Extract a file specified by <path> from petablox.jar as a temp file.
	 * @param path
	 * @return
	 */
	public static URI extractFileFromJar(String path)
	{
		final URI  fileURI;

		final ZipFile zipFile;
		try{

			zipFile = new ZipFile(getPetabloxJarFile());

			try
			{
				fileURI = extract(zipFile, path);
			}
			finally
			{
				zipFile.close();
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return (fileURI);
	}

	private static URI extract(final ZipFile zipFile,
			final String path)
					throws IOException
	{
		final File         tempFile;
		final ZipEntry     entry;
		final InputStream  zipStream;
		OutputStream       fileStream;

		String fTokens[] = path.split(File.separator);
		tempFile = File.createTempFile(fTokens[fTokens.length-1], Long.toString(System.currentTimeMillis()));
		tempFile.setExecutable(true);
		tempFile.deleteOnExit();
		entry    = zipFile.getEntry(path);

		if(entry == null)
		{
			throw new FileNotFoundException("cannot find file: " + path + " in archive: " + zipFile.getName());
		}

		zipStream  = zipFile.getInputStream(entry);
		fileStream = null;

		try
		{
			final byte[] buf;
			int          i;

			fileStream = new FileOutputStream(tempFile);
			buf        = new byte[1024];
			i          = 0;

			while((i = zipStream.read(buf)) != -1)
			{
				fileStream.write(buf, 0, i);
			}
		}
		finally
		{
			close(zipStream);
			close(fileStream);
		}

		return (tempFile.toURI());
	}

	private static void close(final Closeable stream)
	{
		if(stream != null)
		{
			try {
				stream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
