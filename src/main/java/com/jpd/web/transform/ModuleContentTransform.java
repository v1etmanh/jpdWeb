package com.jpd.web.transform;

import com.jpd.web.dto.StandardizedAnswerDto;
import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.model.*;

import java.util.ArrayList;
import java.util.List;

public class ModuleContentTransform {

    // Chuyển ModuleContent thành StandardizedContentDto (câu hỏi + đáp án)
    public static StandardizedContentDto transform(ModuleContent mc) {
        if (mc == null) return null;
        return StandardizedContentDto.builder()
                .lang(extractLanguage(mc))
                .content(cleanContent(extractRawContent(mc)))
                .build();
    }

    // Lấy nội dung thực từ từng loại câu hỏi / media
    public static String extractRawContent(ModuleContent mc) {
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
                return l.getQuestion();
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
                return normalizeUrl(v.getVideoUrl());
            }
            case PDF -> {
                PdfDocument pdf = (PdfDocument) mc;
                return normalizeUrl(pdf.getDocUrl());
            }
            case SPEAKING_PASSAGE -> {
                SpeakingPassageQuestion sp = (SpeakingPassageQuestion) mc;
                return sp.getPassage();
            }
            case SPEAKING_PICTURE -> {
                SpeakingPictureQuestion sp = (SpeakingPictureQuestion) mc;
                return normalizeUrl(sp.getPictureUrl());
            }
            default -> {
                return "";
            }
        }
    }

    // Lấy danh sách đáp án nếu có
    public static List<StandardizedAnswerDto> extractAnswers(ModuleContent mc) {
        List<StandardizedAnswerDto> answers = new ArrayList<>();
        switch (mc.getTypeOfContent()) {
            case MULTIPLE_CHOICE -> {
                MultipleChoiceQuestion q = (MultipleChoiceQuestion) mc;
                if (q.getOptions() != null) {
                    for (var opt : q.getOptions()) {
                        answers.add(StandardizedAnswerDto.builder()
                                .answerText(cleanContent(opt.getOptionText()))
                                .correct(opt.isCorrect())
                                .build());
                    }
                }
            }
            case LISTEN_CHOICE -> {
                ListeningChoiceQuestion l = (ListeningChoiceQuestion) mc;
                if (l.getOptions() != null) {
                    for (var opt : l.getOptions()) {
                        answers.add(StandardizedAnswerDto.builder()
                                .answerText(cleanContent(opt.getOptionText()))
                                .correct(opt.isCorrect())
                                .build());
                    }
                }
            }
            case GAPFILL -> {
                GapFillQuestion g = (GapFillQuestion) mc;
                if (g.getAnswers() != null) {
                    for (var a : g.getAnswers()) {
                        answers.add(StandardizedAnswerDto.builder()
                                .answerText(cleanContent(a.getAnswer()))
                                .correct(true)
                                .build());
                    }
                }
            }
            case READING -> {
                Passage p = (Passage) mc;
                if (p.getReadingQuestion() != null) {
                    for (var rq : p.getReadingQuestion()) {
                        if (rq.getReadingQuestionOptions() != null) {
                            for (var opt : rq.getReadingQuestionOptions()) {
                                answers.add(StandardizedAnswerDto.builder()
                                        .answerText(cleanContent(opt.getOptionText()))
                                        .correct(opt.isCorrect())
                                        .build());
                            }
                        }
                    }
                }
            }
            default -> {
                // Các loại khác không có đáp án
            }
        }
        return answers;
    }

    // Lấy ngôn ngữ (nếu model có), nếu không thì suy luận từ nội dung
    public static String extractLanguage(ModuleContent mc) {
        try {
            var method = mc.getClass().getMethod("getLanguage");
            Object lang = method.invoke(mc);
            return lang != null ? lang.toString().toLowerCase() : inferLang(mc);
        } catch (Exception e) {
            return inferLang(mc);
        }
    }

    // Suy luận ngôn ngữ nếu model không có field language
    private static String inferLang(ModuleContent mc) {
        String text = mc.toString().toLowerCase();
        if (text.matches(".*[ぁ-んァ-ン一-龯].*")) return "JAPANESE"; // có ký tự tiếng Nhật
        if (text.matches(".*[a-z].*")) return "ENGLISH"; // có ký tự Latin
        return "VIETNAM"; // fallback
    }

    // Chuẩn hóa nội dung (loại bỏ ký tự HTML, ký tự đặc biệt)
    public static String cleanContent(String text) {
        if (text == null) return "";
        return text
                .replaceAll("<[^>]*>", "")                // bỏ tag HTML
                .replaceAll("[^\\p{L}\\p{N}\\s.,?!\\-_]", "") // chỉ giữ ký tự hợp lệ
                .replaceAll("\\s+", " ")                  // gom nhiều khoảng trắng
                .trim();
    }

    // Chuẩn hóa URL (bỏ https://, /, :, giữ . hợp lý)
    private static String normalizeUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("https?://", "")
                .replaceAll("/", ".")
                .replaceAll("[^a-zA-Z0-9.]", "")
                .replaceAll("\\.{2,}", ".")
                .trim();
    }
}
