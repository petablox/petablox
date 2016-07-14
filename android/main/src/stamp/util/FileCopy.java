package stamp.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class FileCopy
{
	private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.

	public static void copy(File srcFile, File dstFile) throws IOException
	{
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new BufferedInputStream(new FileInputStream(srcFile), DEFAULT_BUFFER_SIZE);
			output = new BufferedOutputStream(new FileOutputStream(dstFile), DEFAULT_BUFFER_SIZE);
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			for (int length = 0; ((length = input.read(buffer)) > 0);) {
                    output.write(buffer, 0, length);
			}
		} finally {
			if (output != null) try { output.close(); } catch (IOException e) { throw e; }
			if (input != null) try { input.close(); } catch (IOException e) { throw e; }
		}
	}
}