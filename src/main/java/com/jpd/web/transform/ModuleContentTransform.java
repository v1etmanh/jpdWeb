package com.jpd.web.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.jpd.web.dto.*;
import com.jpd.web.model.*;
import com.jpd.web.service.utils.LanguageConverter;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ModuleContentTransform {

    private static final int MAX_CONTENT_LENGTH = 8000;
    
    /**
     * Transform ModuleContent to StandardizedContentDto
     * ✅ Using centralized LanguageConverter
     */
    public static StandardizedContentDto transform(
            ModuleContent mc, 
            Language primary, 
            Language secondary
    ) {
        if (mc == null) {
            return null;
        }

        try {
            long contentId = generateContentId(mc);
            String rawContent = extractRawContent(mc);
            String cleanedContent = cleanContent(rawContent);
            String finalContent = truncateContent(cleanedContent, MAX_CONTENT_LENGTH);
            
            // ✅ Use utility instead of duplicated code
            List<String> languages = LanguageConverter.toCodes(primary, secondary);

            return StandardizedContentDto.builder()
                    .content_id(contentId)
                    .lang(languages)
                    .content(finalContent)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error transforming ModuleContent mcId={}, type={}", 
                mc.getMcId(), mc.getTypeOfContent(), e);
            
            return StandardizedContentDto.builder()
                    .content_id(generateContentId(mc))
                    .lang(List.of("vi")) // Fallback
                    .content("Error extracting content: " + e.getMessage())
                    .build();
        }
    }
    
    // Rest of the methods remain the same...
    private static long generateContentId(ModuleContent mc) {
        if (mc.getMcId() != null && mc.getMcId() > 0) {
            return mc.getMcId();
        }
        return -Math.abs(System.nanoTime() + mc.hashCode());
    }

    public static String extractRawContent(ModuleContent mc) {
        if (mc == null || mc.getTypeOfContent() == null) {
            return "";
        }

        try {
            switch (mc.getTypeOfContent()) {
                case FLASHCARD -> {
                    FlashCard f = (FlashCard) mc;
                    return String.format("Flashcard: %s - %s", 
                        safeGet(f.getWord()), 
                        safeGet(f.getMeaning()));
                }
                
                case GAPFILL -> {
                    GapFillQuestion gfq = (GapFillQuestion) mc;
                    String answers = gfq.getAnswers() != null 
                        ? gfq.getAnswers().stream()
                            .map(a -> safeGet(a.getAnswer()))
                            .collect(Collectors.joining("; "))
                        : "";
                    return String.format("Gap Fill: %s [Answers: %s]", 
                        safeGet(gfq.getQuestionText()), 
                        answers);
                }
                
                case LISTEN_CHOICE -> {
                    ListeningChoiceQuestion lcq = (ListeningChoiceQuestion) mc;
                    String options = lcq.getOptions() != null
                        ? lcq.getOptions().stream()
                            .map(o -> String.format("%s%s", 
                                safeGet(o.getOptionText()), 
                                o.isCorrect() ? " (✓)" : ""))
                            .collect(Collectors.joining("; "))
                        : "";
                    return String.format("Listening: %s [Options: %s]", 
                        safeGet(lcq.getQuestion()), 
                        options);
                }
                
                case SPEAKING_PASSAGE -> {
                    SpeakingPassageQuestion spq = (SpeakingPassageQuestion) mc;
                    return String.format("Speaking Passage: %s\n%s", 
                        safeGet(spq.getTitle()), 
                        safeGet(spq.getPassage()));
                }
                
                case SPEAKING_PICTURE -> {
                    SpeakingPictureQuestion spq = (SpeakingPictureQuestion) mc;
                    String qa = spq.getSpeakingPictureListQuestions() != null
                        ? spq.getSpeakingPictureListQuestions().stream()
                            .map(sq -> String.format("Q: %s | A: %s", 
                                safeGet(sq.getQuestion()), 
                                safeGet(sq.getAnswer())))
                            .collect(Collectors.joining("; "))
                        : "";
                    return String.format("Speaking Picture [URL: %s] Q&A: %s", 
                        safeGet(spq.getPictureUrl()), 
                        qa);
                }
                
                case VIDEO -> {
                    TeachingVideo tv = (TeachingVideo) mc;
                    return String.format("Video: %s [URL: %s]", 
                        safeGet(tv.getTitleVideo()), 
                        safeGet(tv.getVideoUrl()));
                }
                
                case WRITING -> {
                    WritingQuestion wq = (WritingQuestion) mc;
                    return String.format("Writing Question: %s\nRequirements: %s", 
                        safeGet(wq.getQuestion()), 
                        safeGet(wq.getRequirements()));
                }
                
                case MULTIPLE_CHOICE -> {
                    MultipleChoiceQuestion mcq = (MultipleChoiceQuestion) mc;
                    String options = mcq.getOptions() != null
                        ? mcq.getOptions().stream()
                            .map(o -> String.format("%s%s", 
                                safeGet(o.getOptionText()), 
                                o.isCorrect() ? " (✓)" : ""))
                            .collect(Collectors.joining("; "))
                        : "";
                    return String.format("Multiple Choice: %s [Options: %s]", 
                        safeGet(mcq.getQuestionText()), 
                        options);
                }
                
                case READING -> {
                    if (mc instanceof Passage) {
                        Passage passage = (Passage) mc;
                        return String.format("Reading Passage: %s\n%s", 
                            safeGet(passage.getTitle()), 
                            safeGet(passage.getContent()));
                    }
                    return "Reading content";
                }
                
                default -> {
                    log.warn("Unsupported content type: {}", mc.getTypeOfContent());
                    return String.format("Content type: %s", mc.getTypeOfContent().name());
                }
            }
        } catch (ClassCastException e) {
            log.error("Type mismatch for content type {}", mc.getTypeOfContent(), e);
            return "Error: Invalid content type mapping";
        }
    }

    private static String cleanContent(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        return text
            .replaceAll("<[^>]*>", " ")
            .replaceAll("https?://\\S+", "[URL]")
            .replaceAll("[^\\p{L}\\p{N}\\s.,?!\\-_:;()'\"\\n]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    private static String truncateContent(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private static String safeGet(String value) {
        return value != null ? value : "";
    }
}