package com.jpd.web.transform;

import com.jpd.web.dto.StandardizedAnswerDto;
import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleContentTransformTest {

    @Test
    void testMultipleChoiceQuestion() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion();
        q.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
        q.setQuestionText("What is the capital of France?");

        // Tạo options thủ công (không dùng builder)
        List<MultipleChoiceOption> mcOptions = new ArrayList<>();
        MultipleChoiceOption o1 = new MultipleChoiceOption();
        o1.setOptionText("Paris");
        o1.setCorrect(true);
        mcOptions.add(o1);

        MultipleChoiceOption o2 = new MultipleChoiceOption();
        o2.setOptionText("London");
        o2.setCorrect(false);
        mcOptions.add(o2);

        MultipleChoiceOption o3 = new MultipleChoiceOption();
        o3.setOptionText("Berlin");
        o3.setCorrect(false);
        mcOptions.add(o3);

        MultipleChoiceOption o4 = new MultipleChoiceOption();
        o3.setOptionText("Hanoi");
        o3.setCorrect(false);
        mcOptions.add(o4);

        q.setOptions(mcOptions);

        StandardizedContentDto questionDto = ModuleContentTransform.transform(q);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(q);

        assertEquals("What is the capital of France?", questionDto.getContent());
        assertEquals(4, answers.size());
        assertTrue(answers.stream().anyMatch(a -> a.getAnswerText().equals("Paris") && a.isCorrect()));
        assertTrue(answers.stream().anyMatch(a -> a.getAnswerText().equals("London") && !a.isCorrect()));
    }

    @Test
    void testListeningChoiceQuestion() {
        ListeningChoiceQuestion q = new ListeningChoiceQuestion();
        q.setTypeOfContent(TypeOfContent.LISTEN_CHOICE);
        q.setQuestion("Which word did you hear?");

        List<ListeningChoiceOption> lOpts = new ArrayList<>();
        ListeningChoiceOption lo1 = new ListeningChoiceOption();
        lo1.setOptionText("Cat");
        lo1.setCorrect(false);
        lOpts.add(lo1);

        ListeningChoiceOption lo2 = new ListeningChoiceOption();
        lo2.setOptionText("Dog");
        lo2.setCorrect(true);
        lOpts.add(lo2);

        q.setOptions(lOpts);

        StandardizedContentDto questionDto = ModuleContentTransform.transform(q);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(q);

        assertEquals("Which word did you hear?", questionDto.getContent());
        assertEquals(2, answers.size());
        assertTrue(answers.stream().anyMatch(a -> a.getAnswerText().equals("Dog") && a.isCorrect()));
    }

    @Test
    void testGapFillQuestion() {
        GapFillQuestion g = new GapFillQuestion();
        g.setTypeOfContent(TypeOfContent.GAPFILL);
        g.setQuestionText("I ___ coffee every morning!");

        List<GapFillAnswer> gapAnswers = new ArrayList<>();
        GapFillAnswer a1 = new GapFillAnswer();
        a1.setAnswer("drink");
        gapAnswers.add(a1);

        GapFillAnswer a2 = new GapFillAnswer();
        a2.setAnswer("have");
        gapAnswers.add(a2);

        g.setAnswers(gapAnswers);

        StandardizedContentDto questionDto = ModuleContentTransform.transform(g);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(g);

        assertEquals("I ___ coffee every morning!", questionDto.getContent());
        assertEquals(2, answers.size());
        assertTrue(answers.stream().anyMatch(a -> a.getAnswerText().equals("drink") && a.isCorrect()));
        assertTrue(answers.stream().anyMatch(a -> a.getAnswerText().equals("have") && a.isCorrect()));
    }

    @Test
    void testFlashCard() {
        FlashCard f = new FlashCard();
        f.setTypeOfContent(TypeOfContent.FLASHCARD);
        f.setWord("Apple");
        f.setMeaning("A fruit");
        StandardizedContentDto dto = ModuleContentTransform.transform(f);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(f);

        assertEquals("ENGLISH", dto.getLang());
        assertEquals("Apple - A fruit", dto.getContent());
        assertTrue(answers.isEmpty());
    }

    @Test
    void testWritingQuestion() {
        WritingQuestion w = new WritingQuestion();
        w.setTypeOfContent(TypeOfContent.WRITING);
        w.setQuestion("Describe your hometown.");

        StandardizedContentDto dto = ModuleContentTransform.transform(w);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(w);

        assertEquals("Describe your hometown.", dto.getContent());
        assertTrue(answers.isEmpty());
    }

    @Test
    void testVideoContent() {
        TeachingVideo v = new TeachingVideo();
        v.setTypeOfContent(TypeOfContent.VIDEO);
        v.setVideoUrl("https://example.com/video.mp4");

        StandardizedContentDto dto = ModuleContentTransform.transform(v);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(v);

        assertEquals("example.com.video.mp4", dto.getContent());
        assertTrue(answers.isEmpty());
    }

    @Test
    void testPdfDocument() {
        PdfDocument pdf = new PdfDocument();
        pdf.setTypeOfContent(TypeOfContent.PDF);
        pdf.setDocUrl("https://example.com/doc.pdf");

        StandardizedContentDto dto = ModuleContentTransform.transform(pdf);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(pdf);

        assertEquals("example.com.doc.pdf", dto.getContent());
        assertTrue(answers.isEmpty());
    }

    @Test
    void testSpeakingPassage() {
        SpeakingPassageQuestion sp = new SpeakingPassageQuestion();
        sp.setTypeOfContent(TypeOfContent.SPEAKING_PASSAGE);
        sp.setPassage("Talk about your family.");

        StandardizedContentDto dto = ModuleContentTransform.transform(sp);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(sp);

        assertEquals("Talk about your family.", dto.getContent());
        assertTrue(answers.isEmpty());
    }

    @Test
    void testSpeakingPicture() {
        SpeakingPictureQuestion sp = new SpeakingPictureQuestion();
        sp.setTypeOfContent(TypeOfContent.SPEAKING_PICTURE);
        sp.setPictureUrl("https://example.com/pic.jpg");

        StandardizedContentDto dto = ModuleContentTransform.transform(sp);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(sp);

        assertEquals("example.com.pic.jpg", dto.getContent());
        assertTrue(answers.isEmpty());
    }

    @Test
    void testReadingPassageWithQuestions() {
        Passage passage = new Passage();
        passage.setTypeOfContent(TypeOfContent.READING);
        passage.setContent("Reading passage about Tokyo.");

        // Câu hỏi phụ bên trong passage
        ReadingQuestion rq = new ReadingQuestion();
        List<ReadingQuestionOptions> opts = new ArrayList<>();
        ReadingQuestionOptions o1 = new ReadingQuestionOptions();
        o1.setOptionText("Tokyo");
        o1.setCorrect(true);
        opts.add(o1);

        ReadingQuestionOptions o2 = new ReadingQuestionOptions();
        o2.setOptionText("Osaka");
        o2.setCorrect(false);
        opts.add(o2);

        rq.setReadingQuestionOptions(opts);
        passage.setReadingQuestion(List.of(rq));

        StandardizedContentDto dto = ModuleContentTransform.transform(passage);
        List<StandardizedAnswerDto> answers = ModuleContentTransform.extractAnswers(passage);

        assertEquals("Reading passage about Tokyo.", dto.getContent());
        assertEquals(2, answers.size());
        assertTrue(answers.stream().anyMatch(a -> a.getAnswerText().equals("Tokyo") && a.isCorrect()));
    }
}
