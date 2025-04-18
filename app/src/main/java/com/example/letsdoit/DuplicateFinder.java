package com.example.letsdoit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DuplicateFinder {

    // Function to scan a directory and find duplicate files using multithreading
    public static List<List<File>> findDuplicates(File directory) throws InterruptedException, IOException, NoSuchAlgorithmException, ExecutionException {
        Map<String, List<File>> fileMap = new ConcurrentHashMap<>(); // Thread-safe map for storing files
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> tasks = new ArrayList<>();

        // Get all files in the directory
        File[] files = directory.listFiles();
        if (files == null) {
            return new ArrayList<>();  // Return empty list if directory is empty or inaccessible
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively scan subdirectories
                tasks.add(executorService.submit(() -> {
                    findDuplicates(file).forEach(duplicates ->
                            duplicates.forEach(duplicateFile -> {
                                try {
                                    String fileHash = getFileHash(duplicateFile);
                                    fileMap.computeIfAbsent(fileHash, k -> new ArrayList<>()).add(duplicateFile);
                                } catch (IOException | NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                            })
                    );
                    return null;
                }));
            } else {
                // Submit tasks to calculate the hash and update the map
                tasks.add(executorService.submit(() -> {
                    String fileHash = getFileHash(file);
                    fileMap.computeIfAbsent(fileHash, k -> new ArrayList<>()).add(file);
                    return null;
                }));
            }
        }

        // Wait for all tasks to complete
        for (Future<Void> task : tasks) {
            task.get();
        }

        executorService.shutdown();

        // Collect duplicate files from the map
        List<List<File>> duplicates = new ArrayList<>();
        for (List<File> fileList : fileMap.values()) {
            if (fileList.size() > 1) {
                duplicates.add(fileList);  // Add groups of duplicates
            }
        }

        return duplicates;
    }

    // Function to calculate the MD5 hash of a file
    private static String getFileHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
        }

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));  // Convert byte to hex string
        }

        return sb.toString();
    }
}
