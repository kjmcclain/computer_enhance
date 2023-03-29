import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.*;
import java.util.stream.*;
import java.util.zip.*;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.function.Predicate.*;

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
			try (var files = findFiles(outProductionDir))
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
				exec("gcc",
					"--std=c++17",
					"-shared",
					"-fPIC",
					"../../sim86_lib.cpp",
					"-o",
					"lib/libsim86.so"
				);
				// since ../shared_lib.h uses the c23 feature N3030
				// we can't use gcc, which doesn't implement it
				// clang doesn't officially support it either,
				// but apparently it does compile
				exec("clang",
					"-shared",
					"lib/libsim86.so",
					"-fPIC",
					"-I" + javaHome + "/include",
					"-I" + javaHome + "/include/linux",
					"-Iinclude",
					"-I..",
					"c/sim86_lib_java.c",
					"-o",
					"lib/libsim86java.so"
				);
			}
			case WINDOWS ->
			{
				throw new RuntimeException("Windows unsupported");
			}
		}
	}

	private void exec(String... cmd) throws Exception
	{
		var process = new ProcessBuilder().command(cmd).inheritIO().start();
		var status = process.waitFor();
		if (status != 0)
		{
			throw new RuntimeException("Command:\n\t" +
				Arrays.toString(cmd) +
				"\nexited with status " +
				status);
		}
	}

	private void compile(Object... args) throws Exception
	{
		var options = new ArrayList<String>(args.length - 1);
		for (int i = 0; i < args.length - 1; i++)
		{
			options.add(args[i].toString());
		}
		var dir = (Path) args[args.length - 1];
		var
			task =
			javac.getTask(null,
				fileManager,
				null,
				options,
				List.of(),
				javaFiles(dir)
			);
		if (!task.call())
		{
			throw new RuntimeException("Compilation failed: " +
				Arrays.toString(args));
		}

		// this forces the files to be written so they can be read on future compiles
		fileManager.flush();
		fileManager.close();
	}

	private void copyResources(Path src, Path dest) throws Exception
	{
		try (var paths = findFiles(src).filter(not(this::isJavaFile)))
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

	private Iterable<? extends JavaFileObject> javaFiles(Path dir)
		throws Exception
	{
		try (var paths = findFiles(dir).filter(this::isJavaFile))
		{
			return fileManager.getJavaFileObjects(paths.toArray(Path[]::new));
		}
	}

	private Stream<Path> findFiles(Path p) throws Exception
	{
		return Files.find(p, 10, (x, a) -> a.isRegularFile());
	}

	private boolean isJavaFile(Path p)
	{
		return p.getFileName().toString().endsWith(".java");
	}

	private OS getOS()
	{
		var name = System.getProperty("os.name").toLowerCase();
		return name.contains("win")
			? OS.WINDOWS
			: name.contains("linux") ? OS.LINUX : OS.MAC;
	}

	private enum OS
	{
		LINUX, WINDOWS, MAC
	}
}
