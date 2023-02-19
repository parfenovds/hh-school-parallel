package ru.hh.school.homework.controller;



import ru.hh.school.homework.service.Service;
import ru.hh.school.homework.view.Viewer;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
    private final Service service;
    private final Viewer viewer;

    public Controller(Service service, Viewer viewer) {
        this.service = service;
        this.viewer = viewer;
    }

    public void start() {
        Map<Path, Map<String, Long>> dirToRes = new ConcurrentHashMap<>();
        Map<String, Long> wordToGoogleFreq = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newWorkStealingPool();
        long l = System.currentTimeMillis();
        service.collectStatisticsForWordsInMap(executorService, dirToRes);
        System.out.println(System.currentTimeMillis() - l);
        dirToRes = service.getMapWithRemovedEmptyDirectories(dirToRes);
        long l1 = System.currentTimeMillis();
        service.countPopularWordsWithGoogleFreq(dirToRes, wordToGoogleFreq);
        System.out.println(System.currentTimeMillis() - l1);
        viewer.print(dirToRes);
    }
}
