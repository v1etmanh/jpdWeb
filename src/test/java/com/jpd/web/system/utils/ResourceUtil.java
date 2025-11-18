package com.jpd.web.system.utils;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public class ResourceUtil {

    public static String pathInResources(String filename) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(filename);
        if (url == null) throw new RuntimeException("Resource not found: " + filename);
        return Paths.get(url.getPath()).toFile().getAbsolutePath();
    }
}
