package com.jpd.web.helper;

import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

public final class TestDataReader {
    private TestDataReader() {}

    // Đọc CSV từ classpath, trả về List<Map<String, String>>
    public static List<Map<String, String>> read(String classpathCsv) throws Exception {
        InputStream is = TestDataReader.class.getResourceAsStream(classpathCsv);
        if (is == null) {
            throw new IllegalArgumentException("CSV not found on classpath: " + classpathCsv);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return Collections.emptyList();
            String[] headers = split(headerLine);
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
                String[] cells = split(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String key = headers[i].trim();
                    String val = i < cells.length ? cells[i].trim() : "";
                    row.put(key, val);
                }
                rows.add(row);
            }
            return rows;
        }
    }

    // Đọc CSV, trả về Stream<Map<String, String>> (tuỳ trường hợp dùng)
    public static Stream<Map<String, String>> readAsStream(String classpathCsv) throws Exception {
        return read(classpathCsv).stream();
    }

    // Đọc CSV dùng Spring classpath (trường hợp file trong resources/testdata)
    public static List<Map<String, String>> readTestDataFromCSV(String fileName) throws IOException {
        List<Map<String, String>> testData = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("testdata/" + fileName).getInputStream()))) {
            String headerLine = br.readLine();
            if (headerLine == null) return testData;
            String[] headers = headerLine.split(",");
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length && i < values.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                testData.add(row);
            }
        }
        return testData;
    }

    // Split CSV đời thường, hỗ trợ quoted fields
    private static String[] split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (c == ',' && !inQuotes) { out.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(c);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    // Helpers cho các kiểu dữ liệu
    public static boolean parseBoolean(String value) {
        return value != null && value.equalsIgnoreCase("true");
    }

    public static Long parseLong(String value) {
        return (value == null || value.isBlank()) ? null : Long.parseLong(value.trim());
    }

    public static Integer parseInteger(String value) {
        return (value == null || value.isBlank()) ? null : Integer.parseInt(value.trim());
    }

    public static Double parseDouble(String value) {
        return (value == null || value.isBlank()) ? null : Double.parseDouble(value.trim());
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        return (value == null || value.isBlank()) ? null : Enum.valueOf(enumClass, value.trim());
    }

    public static LocalDateTime parseDateTime(String value) {
        return (value == null || value.isBlank()) ? null : LocalDateTime.parse(value);
    }

    // ======================================================================
    // NEW: aliases & helpers hỗ trợ thư mục "test-data/" và đường dẫn không có dấu '/'
    // ======================================================================

    /**
     * Alias tiện dụng: tự thêm dấu "/" đầu vào và ưu tiên thư mục "test-data/".
     * Ví dụ hợp lệ:
     *  - "test-data/log_action_test_cases.csv"
     *  - "/test-data/log_action_test_cases.csv"
     *  - "log_action_test_cases.csv" (nếu file nằm trực tiếp ở /test-data/)
     */
    public static List<Map<String, String>> readCsv(String path) throws Exception {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("CSV path is blank");
        }

        // Nếu chỉ truyền tên file -> mặc định ở test-data/
        if (!path.contains("/")) {
            path = "test-data/" + path;
        }

        String normalized = normalizeForClasspath(path);

        // Thử cách 1: getResourceAsStream (classpath)
        InputStream is = TestDataReader.class.getResourceAsStream(normalized);
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return readFromBufferedReader(br);
            }
        }

        // Thử cách 2: Spring ClassPathResource
        try {
            ClassPathResource cpr = new ClassPathResource(stripLeadingSlash(normalized));
            if (cpr.exists()) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(cpr.getInputStream(), StandardCharsets.UTF_8))) {
                    return readFromBufferedReader(br);
                }
            }
        } catch (IOException ignore) {}

        throw new IllegalArgumentException("CSV not found on classpath: " + path);
    }

    // NEW: Stream alias
    public static Stream<Map<String, String>> readCsvAsStream(String path) throws Exception {
        return readCsv(path).stream();
    }

    // NEW: đọc trong thư mục "test-data/" bằng tên file (sugar cho readCsv)
    public static List<Map<String, String>> readFromTestData(String fileName) throws Exception {
        return readCsv("test-data/" + fileName);
    }

    // NEW: boolean mở rộng: true/false, 1/0, yes/no, y/n
    public static boolean parseFlexibleBoolean(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }

    // -------------------- private helpers (NEW) ----------------------------
    private static String normalizeForClasspath(String p) {
        String path = p.trim();
        // Nếu không có tiền tố, thêm "/"
        if (!path.startsWith("/")) path = "/" + path;
        // Hỗ trợ cả "testdata" và "test-data"
        path = path.replaceFirst("^/testdata/", "/test-data/");
        return path;
    }

    private static String stripLeadingSlash(String p) {
        return p.startsWith("/") ? p.substring(1) : p;
    }

    // ======================================================================
// FLEXIBLE CSV LOADER: thử nhiều vị trí phổ biến trên classpath
// ======================================================================

    /**
     * Đọc CSV với đường dẫn linh hoạt.
     * - Chỉ tên file -> thử /test-data/<file>, /testdata/<file>, /<file>
     * - Có chứa '/' -> thử cả biến thể /test-data/, /testdata/ tương ứng
     * - Hỗ trợ cả getResourceAsStream và Spring ClassPathResource
     */
    public static List<Map<String, String>> readCsvFlexible(String pathOrFile) throws Exception {
        if (pathOrFile == null || pathOrFile.isBlank()) {
            throw new IllegalArgumentException("CSV path is blank");
        }

        String p = pathOrFile.trim().replace('\\', '/');

        // Tập các ứng viên để thử
        List<String> candidates = new ArrayList<>();

        // Nếu chỉ là tên file
        if (!p.contains("/")) {
            candidates.add("/test-data/" + p);
            candidates.add("/testdata/" + p);
            candidates.add("/" + p);
        } else {
            // Có thư mục: thêm biến thể test-data vs testdata và bản gốc
            String withLeading = p.startsWith("/") ? p : "/" + p;
            candidates.add(withLeading);
            candidates.add(withLeading.replaceFirst("^/testdata/", "/test-data/"));
            candidates.add(withLeading.replaceFirst("^/test-data/", "/testdata/"));
        }

        // Thử lần lượt cho đến khi tìm được
        for (String cand : candidates) {
            InputStream is = openClasspathStream(cand);
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                    return readFromBufferedReader(br);
                }
            }
        }

        throw new IllegalArgumentException("CSV not found on classpath. Tried: " + String.join(", ", candidates));
    }

    // Helper mở stream cho một ứng viên
    private static InputStream openClasspathStream(String absPathWithSlash) {
        // 1) Class.getResourceAsStream
        InputStream is = TestDataReader.class.getResourceAsStream(absPathWithSlash);
        if (is != null) return is;

        // 2) ClassLoader.getResourceAsStream
        String noSlash = absPathWithSlash.startsWith("/") ? absPathWithSlash.substring(1) : absPathWithSlash;
        is = TestDataReader.class.getClassLoader().getResourceAsStream(noSlash);
        if (is != null) return is;

        // 3) Spring ClassPathResource
        try {
            org.springframework.core.io.ClassPathResource cpr = new org.springframework.core.io.ClassPathResource(noSlash);
            if (cpr.exists()) return cpr.getInputStream();
        } catch (Exception ignore) {}

        return null;
    }


    private static List<Map<String, String>> readFromBufferedReader(BufferedReader br) throws IOException {
        String headerLine = br.readLine();
        if (headerLine == null) return Collections.emptyList();
        String[] headers = split(headerLine);
        List<Map<String, String>> rows = new ArrayList<>();
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            String[] cells = split(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String key = headers[i].trim();
                String val = i < cells.length ? cells[i].trim() : "";
                row.put(key, val);
            }
            rows.add(row);
        }
        return rows;
    }
}
