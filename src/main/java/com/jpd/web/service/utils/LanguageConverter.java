package com.jpd.web.service.utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.jpd.web.model.Language;

/**
 * Centralized utility for Language enum ↔ language code conversion
 * Eliminates code duplication across the application
 */
public class LanguageConverter {
    
    /**
     * Convert single Language enum to code
     * VIETNAMESE → "vi"
     * ENGLISH → "en"
     */
    public static String toCode(Language language) {
        if (language == null) {
            return "vi"; // Default
        }
        
        return switch (language) {
            case VIETNAMESE -> "vi";
            case ENGLISH -> "en";
            case CHINESE -> "zh";
            case JAPANESE -> "ja";
            case KOREAN -> "ko";
            case FRENCH -> "fr";
            case GERMAN -> "de";
            case SPANISH -> "es";
            case ITALIAN -> "it";
            case RUSSIAN -> "ru";
        };
    }
    
    /**
     * Convert multiple Language enums to codes
     * [VIETNAMESE, ENGLISH] → ["vi", "en"]
     */
    public static List<String> toCodes(Language... languages) {
        if (languages == null || languages.length == 0) {
            return List.of("vi");
        }
        
        return Arrays.stream(languages)
            .filter(lang -> lang != null)
            .map(LanguageConverter::toCode)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Convert List of Language enums to codes
     */
    public static List<String> toCodes(List<Language> languages) {
        if (languages == null || languages.isEmpty()) {
            return List.of("vi");
        }
        
        return languages.stream()
            .filter(lang -> lang != null)
            .map(LanguageConverter::toCode)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Convert two languages to codes (handles null for second language)
     * Used by ModuleContentTransform
     */
    public static List<String> toCodes(Language primary, Language secondary) {
        List<String> codes = new ArrayList<>();
        
        if (primary != null) {
            codes.add(toCode(primary));
        }
        
        if (secondary != null && secondary != primary) {
            String secondCode = toCode(secondary);
            if (!codes.contains(secondCode)) {
                codes.add(secondCode);
            }
        }
        
        return codes.isEmpty() ? List.of("vi") : codes;
    }
    
    /**
     * Convert language code to Language enum
     * "vi" → VIETNAMESE
     * "en" → ENGLISH
     */
    public static Language fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return Language.VIETNAMESE;
        }
        
        return switch (code.toLowerCase().trim()) {
            case "vi", "vie", "vietnamese" -> Language.VIETNAMESE;
            case "en", "eng", "english" -> Language.ENGLISH;
            case "zh", "chi", "chinese" -> Language.CHINESE;
            case "ja", "jpn", "japanese" -> Language.JAPANESE;
            case "ko", "kor", "korean" -> Language.KOREAN;
            case "fr", "fra", "french" -> Language.FRENCH;
            case "de", "deu", "german" -> Language.GERMAN;
            case "es", "spa", "spanish" -> Language.SPANISH;
            case "it", "ita", "italian" -> Language.ITALIAN;
            case "ru", "rus", "russian" -> Language.RUSSIAN;
            default -> Language.VIETNAMESE; // Fallback
        };
    }
    
    /**
     * Convert array of codes to Language enums
     */
    public static List<Language> fromCodes(String... codes) {
        if (codes == null || codes.length == 0) {
            return List.of(Language.VIETNAMESE);
        }
        
        return Arrays.stream(codes)
            .filter(code -> code != null && !code.isEmpty())
            .map(LanguageConverter::fromCode)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Convert List of codes to Language enums
     */
    public static List<Language> fromCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of(Language.VIETNAMESE);
        }
        
        return codes.stream()
            .filter(code -> code != null && !code.isEmpty())
            .map(LanguageConverter::fromCode)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Validate if code is supported
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        
        String normalized = code.toLowerCase().trim();
        return List.of("vi", "en", "zh", "ja", "ko", "fr", "de", "es", "it", "ru")
            .contains(normalized);
    }
    
    /**
     * Get all supported language codes
     */
    public static List<String> getAllCodes() {
        return List.of("vi", "en", "zh", "ja", "ko", "fr", "de", "es", "it", "ru");
    }
}