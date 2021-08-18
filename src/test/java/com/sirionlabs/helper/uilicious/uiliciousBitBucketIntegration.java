package com.sirionlabs.helper.uilicious;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class uiliciousBitBucketIntegration {

    public static void main(String[] args) throws IOException, InterruptedException {


        ProcessBuilder processBuilder = new ProcessBuilder(System.getProperty("user.dir")+"//"+"uilicious-test.bat");

        try {

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        addAndPushFile();
    }

    // example of usage
    private static void initAndAddFile() throws IOException, InterruptedException {
        Path directory = Paths.get(System.getProperty("user.dir")+"//"+"UI-Sanity");
        Files.createDirectories(directory);
        gitInit(directory);
        Files.write(directory.resolve("example.txt"), new byte[0]);
        gitStage(directory);
        gitCommit(directory, "Add example.txt");
    }

    // example of usage
    private static void gitClone() throws IOException, InterruptedException {
        String originUrl = "ssh://git@bit.sirionlabs.office:7999/qaaut/ui2-automation-uilicious.git";
        Path directory = Paths.get(System.getProperty("user.dir")+"//"+"UI-Sanity");
        gitClone(directory, originUrl);
        gitCheckout(directory,"naveen");
    }

    // example of usage
    private static void addAndPushFile() throws IOException, InterruptedException {
        String originUrl = "ssh://git@bit.sirionlabs.office:7999/qaaut/ui2-automation-uilicious.git";
        Path directory = Paths.get(System.getProperty("user.dir")+"//"+"UI-Sanity");
        gitStage(directory);
        gitCommit(directory, "Backup Date " + currentDate());
        gitPush(directory);
    }

    public static void gitCheckout(Path directory, String branch) throws IOException, InterruptedException {
        runCommand(directory, "git", "checkout", branch);
    }

    public static void gitInit(Path directory) throws IOException, InterruptedException {
        runCommand(directory, "git", "init");
    }

    public static void gitStage(Path directory) throws IOException, InterruptedException {
        runCommand(directory, "git", "add", "-A");
    }

    public static void gitCommit(Path directory, String message) throws IOException, InterruptedException {
        runCommand(directory, "git", "commit", "-m", message);
    }

    public static void gitPush(Path directory) throws IOException, InterruptedException {
        runCommand(directory, "git", "push");
    }

    public static void gitClone(Path directory, String originUrl) throws IOException, InterruptedException {
        runCommand(directory.getParent(), "git", "clone", originUrl, directory.getFileName().toString());
    }

    public static String currentDate(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-YYYY hh:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }

    public static void runCommand(Path directory, String... command) throws IOException, InterruptedException {
        Objects.requireNonNull(directory, "directory");
        if (!Files.exists(directory)) {
            throw new RuntimeException("can't run command in non-existing directory '" + directory + "'");
        }
        ProcessBuilder pb = new ProcessBuilder()
                .command(command)
                .directory(directory.toFile());
        Process p = pb.start();
        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");
        outputGobbler.start();
        errorGobbler.start();
        int exit = p.waitFor();
        errorGobbler.join();
        outputGobbler.join();
        if(exit==1){
            System.out.println("nothing to commit, working tree clean");
        }else if (exit != 0) {
            throw new AssertionError(String.format("runCommand returned %d", exit));
        }
    }

    private static class StreamGobbler extends Thread {

        private final InputStream is;
        private final String type;

        private StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(type + "> " + line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
