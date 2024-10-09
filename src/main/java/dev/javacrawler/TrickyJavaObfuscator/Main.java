package dev.javacrawler.TrickyJavaObfuscator;

import java.io.File;
import java.util.Scanner;

public class Main {

    public static void log(String message) {
        System.out.println("[Tricky Java Obfuscator] " + message);
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        log("Enter .jar name or path: ");
        String filePath = scanner.nextLine().trim();

        File file = new File(filePath);

        if (file.exists()) {
            log("Loaded file: " + file.getAbsolutePath());
            scanner.close();
            log("Processing obfuscation...");

            ObfuscationProcessor processorThread = new ObfuscationProcessor(file);
            processorThread.start();

            synchronized (processorThread.flagLock) {
                while (processorThread.getFlag() == null) {
                    try {
                        processorThread.flagLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else {
            log("File not found: " + file.getAbsolutePath());
            scanner.close();
        }

        log("Developed by https://javacrawler.lol/");
    }
}
