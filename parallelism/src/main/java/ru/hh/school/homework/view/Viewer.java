package ru.hh.school.homework.view;

import java.nio.file.Path;
import java.util.Map;
public class Viewer {
    public void print(Map<Path, Map<String, Long>> dirToRes) {
        for (Map.Entry<Path, Map<String, Long>> pathToMap : dirToRes.entrySet()) {
            for (Map.Entry<String, Long> wordToFrequency : pathToMap.getValue().entrySet()) {
                System.out.printf("%-30s - %-10s - %d%n",
                        pathToMap.getKey(),
                        wordToFrequency.getKey(),
                        wordToFrequency.getValue());
            }
        }
    }
}
