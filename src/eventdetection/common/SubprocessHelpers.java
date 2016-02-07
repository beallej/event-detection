package eventdetection.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

/**
 * Provides useful methods for working with subprocesses.
 * 
 * @author Joshua Lipstone
 */
public class SubprocessHelpers {
	/**
	 * The path required to run the system's bash executable.
	 */
	public static final String bashPath = getBashPath();
	/**
	 * The path required to run the system's Python 3 executable.
	 */
	public static final String pythonPath = getPythonPath();
	
	private SubprocessHelpers() {/* This is a static class */}
	
	private static final String getBashPath() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command("which", "bash");
		try {
			Process p = pb.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				p.waitFor();
				return reader.readLine().trim();
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return "/bin/bash";
	}
	
	private static final String getPythonPath() {
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(bashPath, "-l", "-c", "[ \"$(python --version 2>&1 | grep 'Python 3')\" != \"\" ] && echo \"$(which python)\" || echo \"$(which python3)\"");
		try {
			Process p = pb.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				p.waitFor();
				return reader.readLine().trim();
			}
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return "python3";
	}
	
	/**
	 * Executes a python script with the given arguments
	 * 
	 * @param scriptPath
	 *            the {@link Path} to the script.
	 * @param args
	 *            the arguments to the script. Arguments that include spaces must be quoted
	 * @return a started {@link Process}
	 * @throws IOException
	 *             if an error occurs while starting the process
	 */
	public static Process executePythonProcess(Path scriptPath, String... args) throws IOException {
		String[] command = {bashPath, "-l", "-c", ""};
		StringBuilder cmd = new StringBuilder(pythonPath.length() * 2);
		cmd.append(pythonPath).append(" ").append(scriptPath.normalize().toString());
		for (String arg : args)
			cmd.append(" ").append(arg);
		command[3] = cmd.toString();
		
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(scriptPath.getParent().toFile());
		return pb.start();
	}
}
