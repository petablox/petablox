package stamp.submitting;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.http.HttpSession; 

import java.util.logging.Logger;

@WebServlet(name = "FileUploadServlet", urlPatterns = {"/fileUpload"})
	@MultipartConfig(location = "/tmp")
public class FileUploadServlet extends HttpServlet 
{
	private static Logger logger = Logger.getLogger(FileUploadServlet.class.getName());
	private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.
	private StampRunner runner;
	
	public FileUploadServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response) throws ServletException, IOException 
	 {
		System.out.println("FileUploadServlet");

		HttpSession session = request.getSession();
		StampRunner runner = (StampRunner) session.getAttribute("runner");
		if(runner == null){
			String stampDir = getServletContext().getInitParameter("stamp.dir");
			runner = new StampRunner(stampDir);
			session.setAttribute("runner", runner);
			runner.start();
		}

		for (Part part : request.getParts()) {
			logger.info(part.getName());
			String fileName = getFileName(part);
			System.out.println("File uploaded:"+fileName);
			
			File apkFile = runner.targetFile(fileName);
			InputStream input = null;
            OutputStream output = null;
            try {
                input = new BufferedInputStream(part.getInputStream(), DEFAULT_BUFFER_SIZE);
                output = new BufferedOutputStream(new FileOutputStream(apkFile), DEFAULT_BUFFER_SIZE);
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                for (int length = 0; ((length = input.read(buffer)) > 0);) {
                    output.write(buffer, 0, length);
                }
            } finally {
                if (output != null) try { output.close(); } catch (IOException logOrIgnore) { /**/ }
                if (input != null) try { input.close(); } catch (IOException logOrIgnore) { /**/ }
            }
			
			runner.addToWorkList(apkFile);
		}
	}

	private String getFileName(Part part) {
		String partHeader = part.getHeader("content-disposition");
		logger.info("Part Header = " + partHeader);
		for (String cd : partHeader.split(";")) {
			if (cd.trim().startsWith("filename")) {
				return cd.substring(cd.indexOf('=') + 1).trim()
					.replace("\"", "");
			}
		}
		return null;
	}

	protected void doPost(HttpServletRequest request,
						  HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}