package ru.hh.school.homework.service;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import ru.hh.school.homework.util.Constants;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

public class Service {

    public void collectStatisticsForWordsInMap(ExecutorService executorService, Map<Path, Map<String, Long>> dirToRes) {
        try {
            initWordFrequencyCount(executorService, dirToRes);
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void initWordFrequencyCount(ExecutorService executorService, Map<Path, Map<String, Long>> dirToFiles) {
        try {
            Files.walkFileTree(Paths.get(Constants.TARGET_DIR), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    putDirectoriesToMapAsKey(dir, dirToFiles);
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    initStatisticsCollecting(file, executorService, dirToFiles);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void putDirectoriesToMapAsKey(Path dir, Map<Path, Map<String, Long>> dirToFiles) {
        dirToFiles.put(dir, new ConcurrentHashMap<>());
    }

    public void initStatisticsCollecting(Path file, ExecutorService executorService, Map<Path, Map<String, Long>> dirToFiles) {
        String fileName = file.getFileName().toString();
        if (FilenameUtils.getExtension(fileName).equals("java")) {
            CompletableFuture<Map<String, Long>> mapCompletableFuture =
                    CompletableFuture.supplyAsync(() -> initStatisticsByWordCollecting(file), executorService);
            mapCompletableFuture.thenAcceptAsync(m -> initMergingStatistics(file, dirToFiles, m), executorService);
        }
    }

    private static Map<String, Long> initStatisticsByWordCollecting(Path file) {
        try {
            return Files.lines(file)
                    .flatMap(line -> Stream.of(line.split("[^a-zA-Z0-9]")))
                    .filter(word -> word.length() > 3)
                    .collect(groupingBy(identity(), counting()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initMergingStatistics(Path file, Map<Path, Map<String, Long>> dirToFiles, Map<String, Long> m) {
        System.out.println(Thread.currentThread().getName());
        Map<String, Long> stringLongMap = dirToFiles.get(file.getParent());
        m.forEach((key, value) -> {
            if (stringLongMap.computeIfPresent(key, (k, v) -> v + value) == null) {
                stringLongMap.put(key, value);
            }
        });
    }

    public Map<Path, Map<String, Long>> getMapWithRemovedEmptyDirectories(Map<Path, Map<String, Long>> dirToRes) {
        return dirToRes.entrySet()
                .parallelStream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void countPopularWordsWithGoogleFreq(Map<Path, Map<String, Long>> dirToRes) {
        dirToRes.replaceAll((k, v) -> v.entrySet()
                .parallelStream()
                .sorted(comparingByValue(reverseOrder()))
                .limit(10)
                .collect(toMap(Map.Entry::getKey, e -> getGoogleWordFrequency(e.getKey())))
        );
    }

    public Long getGoogleWordFrequency(String query) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .setHeader("User-Agent", Constants.USER_AGENT)
                .uri(URI.create(Constants.URI_TO_SEARCHING_ENGINE + query))
                .GET()
                .build();
        try {
            CompletableFuture<HttpResponse<String>> httpResponseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            String s = StringUtils.substringBetween(httpResponseCompletableFuture.get().body(), "<div id=\"result-stats\">", "<nobr>");
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
