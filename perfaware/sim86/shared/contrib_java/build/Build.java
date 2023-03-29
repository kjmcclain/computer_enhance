import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Build
{
	public static void main(String[] args) throws Exception
	{
		new Build().run();
	}

	private final JavaCompiler javac;
	private final StandardJavaFileManager fileManager;

	public Build()
	{
		javac = ToolProvider.getSystemJavaCompiler();
		fileManager = javac.getStandardFileManager(null, null, null);
	}

	public void run() throws Exception
	{
		var srcDir = Path.of("src");
		var testDir = Path.of("test");
		var outProductionDir = Path.of("out", "production", "contrib_java");
		var outTestDir = Path.of("out", "test", "contrib_java");

		compile("-h", "include", "-d", outProductionDir, srcDir);
		copyResources(srcDir, outProductionDir);

		compile("-cp", outProductionDir, "-d", outTestDir, testDir);
		copyResources(testDir, outTestDir);

		try (
			var out = new JarOutputStream(Files.newOutputStream(Path.of("sim86.jar")))
		)
		{
			try (
				var files = Files.find(outProductionDir, 10, (p, a) -> a.isRegularFile())
			)
			{
				var iter = files.iterator();
				while (iter.hasNext())
				{
					var file = iter.next();
					var ze = new ZipEntry(outProductionDir.relativize(file).toString());
					out.putNextEntry(ze);
					try (var o = Files.newInputStream(file))
					{
						o.transferTo(out);
					}
					out.closeEntry();
				}
			}
		}

		var javaHome = System.getProperty("java.home");
		switch (getOS())
		{
			case MAC ->
			{
				exec("clang",
					"--std=c++17",
					"-dynamiclib",
					"../../sim86_lib.cpp",
					"-o",
					"lib/libsim86.dylib"
				);
				exec("clang",
					"-dynamiclib",
					"lib/libsim86.dylib",
					"-I" + javaHome + "/include",
					"-I" + javaHome + "/include/darwin",
					"-Iinclude",
					"-I..",
					"c/sim86_lib_java.c",
					"-o",
					"lib/libsim86java.dylib"
				);
			}
			case LINUX ->
			{
				copy(Path.of("../sim86_shared_debug.lib"), Path.of("lib/libsim86.so"), REPLACE_EXISTING);
				exec("gcc",
					"lib/libsim86.lib",
					"-I" + javaHome + "/include",
					"-I" + javaHome + "/include/linux",
					"-Iinclude",
					"-I..",
					"c/sim86_lib_java.c",
					"-o",
					"lib/libsim86java.so"
				);
			}
		}
	}

	private void exec(String... cmd) throws Exception
	{
		var process = new ProcessBuilder().command(cmd).inheritIO().start();
		var status = process.waitFor();
		if (status != 0) throw new RuntimeException("Command:\n\t" +
		                                            Arrays.toString(cmd) +
		                                            "\nexited with status " +
		                                            status);
	}

	private void compile(Object... args) throws Exception
	{
		var options = new ArrayList<String>(args.length - 1);
		for (int i = 0; i < args.length - 1; i++)
		{
			options.add(args[i].toString());
		}
		var dir = (Path) args[args.length - 1];
		var task = javac.getTask(null, fileManager, null, options, List.of(), javaFiles(dir));
		if (!task.call()) throw new RuntimeException("Compilation failed: " + Arrays.toString(args));

		// this forces the files to be written so they can be read on future compiles
		fileManager.flush();
		fileManager.close();
	}

	private void copyResources(Path src, Path dest) throws Exception
	{
		try (
			var paths = Files.find(src,
				10,
				(p, a) -> !p.getFileName().toString().endsWith(".java") && a.isRegularFile()
			)
		)
		{
			var iter = paths.iterator();
			while (iter.hasNext())
			{
				var s = iter.next();
				var d = dest.resolve(src.relativize(s));
				Files.createDirectories(d.getParent());
				Files.copy(s, d, REPLACE_EXISTING);
			}
		}
	}

	private Iterable<? extends JavaFileObject> javaFiles(Path dir) throws Exception
	{
		try (
			var paths = Files.find(dir,
				10,
				(p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith(".java")
			)
		)
		{
			return fileManager.getJavaFileObjects(paths.toArray(Path[]::new));
		}
	}

	private OS getOS()
	{
		var name = System.getProperty("os.name").toLowerCase();
		return name.contains("win") ? OS.WINDOWS : name.contains("linux") ? OS.LINUX : OS.MAC;
	}

	private enum OS
	{
		LINUX, WINDOWS, MAC
	}
}
