package com.octopus;

import com.octopus.decorators.WebDriverDecorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        try {
            final ArrayList<String> options = new ArrayList<String>() {{
                add("--glue");
                add("com.octopus.decoratorbase");
            }};

            Collections.addAll(options, args);

            io.cucumber.core.cli.Main.run(
                    options.toArray(new String[0]),
                    Thread.currentThread().getContextClassLoader());
        } finally {
            WebDriverDecorator.staticStopScreenRecording();
        }
    }
}
