package com.jpd.web.dto;
import java.util.List;

import com.jpd.web.model.Language;
import com.jpd.web.service.utils.LanguageConverter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandardizedContentDto {
    private long content_id;
    private List<String> lang;
    private String content;
    
    // ========== SIMPLIFIED HELPERS ==========
    
    /**
     * Set single language from code
     */
    public void setLanguageFromCode(String langCode) {
        this.lang = List.of(langCode != null ? langCode : "vi");
    }
    
    /**
     * Set multiple languages from codes
     */
    public void setLanguagesFromCodes(List<String> langCodes) {
        this.lang = (langCodes != null && !langCodes.isEmpty()) 
            ? langCodes 
            : List.of("vi");
    }
    
    /**
     * Set from single Language enum
     */
    public void setLanguageFromEnum(Language language) {
        this.lang = List.of(LanguageConverter.toCode(language));
    }
    
    /**
     * Set from multiple Language enums
     */
    public void setLanguagesFromEnums(List<Language> languages) {
        this.lang = LanguageConverter.toCodes(languages);
    }
    
    /**
     * Get primary language code
     */
    public String getPrimaryLanguage() {
        return (lang != null && !lang.isEmpty()) ? lang.get(0) : "vi";
    }
}