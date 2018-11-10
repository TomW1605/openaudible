package org.openaudible.convert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openaudible.util.Platform;
import org.openaudible.util.SimpleProcess;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FFMPEG {
	private static final Log LOG = LogFactory.getLog(FFMPEG.class);
	
	// Return ffmpeg meta data file for media file specified in path
	static String getMetaData(String path) throws IOException, InterruptedException {
		if (!new File(path).exists())
			throw new IOException("File not found:" + path);
		ArrayList<String> args = new ArrayList<>();
		args.add(getExecutable());
		args.add("-i");
		args.add(path);
		args.add("-f");
		args.add("ffmetadata");
		args.add("-");
		SimpleProcess ffmpeg = new SimpleProcess(args);
		SimpleProcess.Results results = ffmpeg.getResults();
		System.err.println(results.getErrorString());
		System.err.println(results.getOutputString());
		return results.getOutputString();
	}
	
	static String getExecutable() {
		String s = "bin" + File.separatorChar + Platform.getPlatform().name() + "_ffmpeg";
		File f = new File(s);
		if (!f.exists()) {
			LOG.info("Unable to find executable:" + f.getAbsolutePath());
			
		}
		if (f.exists()) return f.getAbsolutePath();
		return "ffmpeg";        // hope it is in system path.
	}
	
	// Sets (modifed) ffmpeg meta data file for media file specified in path
	// Returns the File Path of the newly created media file.
	static File setMetaData(String path, String metaDataPath, String newFilePath) throws IOException, InterruptedException {
		if (!new File(path).exists()) throw new IOException("File not found:" + path);
		if (!new File(metaDataPath).exists()) throw new IOException("File not found:" + metaDataPath);
		
		
		ArrayList<String> args = new ArrayList<>();
		
		args.add(getExecutable());
		args.add("-i");
		args.add(path);
		args.add("-i");
		args.add(metaDataPath);
		args.add("-map_metadata");
		args.add("1");
		args.add("-codec");
		args.add("copy");
		args.add(newFilePath);
		
		SimpleProcess ffmpeg = new SimpleProcess(args);
		ffmpeg.run();
		File t = new File(newFilePath);
		assert (t.exists());
		return t;
	}
	
	public static String getVersion() throws IOException, InterruptedException {
		ArrayList<String> args = new ArrayList<>();
		args.add(getExecutable());
		args.add("-version");
		
		SimpleProcess ffmpeg = new SimpleProcess(args);
		SimpleProcess.Results results = ffmpeg.getResults();
		String r = results.getOutputString();
		int ch = r.indexOf("\r");
		if (ch > 0)
			r = r.substring(0, ch - 1);
		
		if (r.trim().isEmpty()) {
			String err = results.getErrorString();
			if (!err.isEmpty())
				throw new IOException(err);
			throw new IOException("unknown ffmpeg error:" + getExecutable());
		}
		
		return r;
	}
	
	
}
