package com.jpd.web.transform;

import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.model.*;

public class ModuleContentTransform {

    //Hàm chính: chuyển ModuleContent thành StandardizedContentDto
    public static StandardizedContentDto transform(ModuleContent mc) {
        if (mc == null) return null;

        return StandardizedContentDto.builder()
                .lang(extractLanguage(mc))        // nếu ModuleContent có trường language thì lấy
                .content(cleanContent(extractRawContent(mc))) // nội dung chính
                .build();
    }

    //Lấy nội dung thực từ từng loại
    private static String extractRawContent(ModuleContent mc) {
        switch (mc.getTypeOfContent()) {
            case FLASHCARD -> {
                FlashCard f = (FlashCard) mc;
                return f.getWord() + " - " + f.getMeaning();
            }
            case MULTIPLE_CHOICE -> {
                MultipleChoiceQuestion q = (MultipleChoiceQuestion) mc;
                return q.getQuestionText();
            }
            case GAPFILL -> {
                GapFillQuestion g = (GapFillQuestion) mc;
                return g.getQuestionText();
            }
            case LISTEN_CHOICE -> {
                ListeningChoiceQuestion l = (ListeningChoiceQuestion) mc;
                return l.getQuestion(); // hoặc description của audio
            }
            case READING -> {
                Passage p = (Passage) mc;
                return p.getContent();
            }
            case WRITING -> {
                WritingQuestion w = (WritingQuestion) mc;
                return w.getQuestion();
            }
            case VIDEO -> {
                TeachingVideo v = (TeachingVideo) mc;
                return v.getVideoUrl(); // hoặc transcript nếu có
            }
            case PDF -> {
                PdfDocument pdf = (PdfDocument) mc;
                return pdf.getDocUrl(); // hoặc bỏ trống vì pdf chủ yếu là file
            }
            case SPEAKING_PASSAGE -> {
                SpeakingPassageQuestion sp = (SpeakingPassageQuestion) mc;
                return sp.getPassage();
            }
            case SPEAKING_PICTURE -> {
                SpeakingPictureQuestion sp = (SpeakingPictureQuestion) mc;
                return sp.getPictureUrl();
            }

            default -> {
                return "";
            }
        }
    }

    //Lấy ngôn ngữ nếu có (nhiều content không có lang → trả null)
    private static String extractLanguage(ModuleContent mc) {
        try {
            // Nếu class con có getLanguage() thì lấy, không thì trả null
            return (String) mc.getClass().getMethod("getLanguage").invoke(mc);
        } catch (Exception e) {
            return null;
        }
    }

    //Làm sạch nội dung
    private static String cleanContent(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")                 // bỏ tag HTML
                .replaceAll("[^\\p{L}\\p{N}\\s.,?!\\-_]", "") // giữ dấu gạch dưới _
                .replaceAll("\\s+", " ")                     // gom nhiều space
                .trim();
    }

}
