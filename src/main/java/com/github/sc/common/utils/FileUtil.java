package com.github.sc.common.utils;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;

public class FileUtil {

	public static void zip(String path, String dest) {
		File srcdir = new File(path);

		Project prj = new Project();
		Zip zip = new Zip();
		zip.setProject(prj);
		zip.setDestFile(new File(dest));
		FileSet fileSet = new FileSet();
		fileSet.setProject(prj);
		fileSet.setDir(srcdir);
        zip.addFileset(fileSet);
		zip.execute();
	}

	public static void down(HttpServletResponse response, InputStream in, String fileName)
			throws Exception {
		fileName = URLEncoder.encode(fileName, "UTF-8");
		response.reset();
		response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		response.setContentType("application/octet-stream;charset=UTF-8");
		IOUtils.copy(in, response.getOutputStream());
		in.close();
	}
	
	
}
