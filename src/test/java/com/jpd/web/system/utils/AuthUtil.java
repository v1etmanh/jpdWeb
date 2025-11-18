package com.jpd.web.system.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.Map;

public class AuthUtil {

    /**
     * Bootstrap localStorage for your app's auth layer.
     * Pass -Dauth.kv with entries like: token=abc;isAuthentication=true;role=CREATOR
     * Adjust keys/values to match your app.
     */
    public static void bootstrapLocalStorage(WebDriver driver) {
        String kv = System.getProperty("auth.kv", "").trim();
        if (kv.isEmpty()) return;

        Map<String, String> map = new HashMap<>();
        for (String pair : kv.split(";")) {
            if (pair.isBlank()) continue;
            String[] parts = pair.split("=", 2);
            String k = parts[0].trim();
            String v = parts.length > 1 ? parts[1].trim() : "";
            map.put(k, v);
        }
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.localStorage.clear();");
        map.forEach((k,v) -> js.executeScript(String.format("window.localStorage.setItem('%s', '%s');", esc(k), esc(v))));
    }

    private static String esc(String s) {
        return s.replace("\\","\\\\").replace("'","\\'");
    }
}
