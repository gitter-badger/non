import java.io.*;

public class Gradle {
    private static final String windowsFile = "gradlew.bat";
    private static final String unixFile = "gradlew";
    private Runner.OutputListener listener;

    private File workingDir;

    public Gradle(String working, Runner.OutputListener listener) {
        this(new File(working), listener);
    }
    
    public Gradle(File working, Runner.OutputListener listener) {
        this.listener = listener;
        workingDir = working;
        new File(workingDir, unixFile).setExecutable(true);
    }

    public void execute (String parameters) throws Exception {
        String targetFile = (System.getProperty("os.name").contains("Windows") ? windowsFile : unixFile);
        String exec = (workingDir.getAbsolutePath() + "/" + targetFile).replace("\\", "/");

        String[] params = parameters.split(" ");
        String[] commands = new String[params.length + 1];
        commands[0] = exec;

        for (int i = 0; i < params.length; i++) {
            commands[i + 1] = params[i];
        }

        startProcess(commands, workingDir);
    }

    private void startProcess (String[] commands, File directory) throws Exception {
        final Process process = new ProcessBuilder(commands).redirectErrorStream(true).directory(directory).start();

        Thread t = new Thread(new Runnable() {
            public void run () {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);
                boolean failed = false;
                try {
                    String c = null;
                    while ((c = reader.readLine()) != null) {
                        c = c.trim();
                        
                        if (c.equals("") ||
                            c.startsWith("BUILD") ||
                            c.startsWith("Total time:") ||
                            !Main.DEBUG && c.startsWith("Configuration on demand is an incubating feature.") ||
                            !Main.DEBUG && c.startsWith("Note:") ||
                            !Main.DEBUG && c.endsWith("warning") ||
                            !Main.DEBUG && c.endsWith("error"))
                            continue;
                        
                        if (!failed) {
                            if (!Main.DEBUG) {
                                if (c.startsWith(":android:compileJava")) {
                                    listener.print(":android:compileJava\n");
                                    continue;
                                } else if (c.startsWith(":desktop:compileJava")) {
                                    listener.print(":desktop:compileJava\n");
                                    continue;
                                } else if (c.startsWith(":ios:compileJava")) {
                                    listener.print(":ios:compileJava\n");
                                    continue;
                                }
                                
                                if (c.endsWith("FAILED")) {
                                    failed = true;
                                    continue;
                                }
                            }
                            
                            listener.print(c + "\n");
                        }
                    }
                } catch (IOException e) { }
            }
        });

        t.setDaemon(true);
        t.start();
        process.waitFor();
        t.interrupt();

        if (process.exitValue() != 0) throw new Exception("Task finished with non-zero result");
    }
}