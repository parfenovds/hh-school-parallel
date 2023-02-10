package ru.hh.school.homework;

import ru.hh.school.homework.controller.Controller;
import ru.hh.school.homework.service.Service;
import ru.hh.school.homework.view.Viewer;

import java.io.IOException;

public class Launcher {
    public static void main(String[] args) throws IOException {
        Service service = new Service();
        Viewer viewer = new Viewer();
        Controller controller = new Controller(service, viewer);
        controller.start();
    }
}
