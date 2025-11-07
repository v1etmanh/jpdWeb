package com.jpd.web.transform;

import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleContentTransformTest {

    @Test
    void testTransformFlashCard() {
        FlashCard flashCard = new FlashCard();
        flashCard.setTypeOfContent(TypeOfContent.FLASHCARD);
        flashCard.setWord("<b>Hello!</b>");
        flashCard.setMeaning("W@orld!!");

        StandardizedContentDto dto = ModuleContentTransform.transform(flashCard);

        assertEquals("Hello! - World!!", dto.getContent());
        assertNull(dto.getLang());
    }

    @Test
    void testTransformMultipleChoice() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion();
        q.setTypeOfContent(TypeOfContent.MULTIPLE_CHOICE);
        q.setQuestionText("What is <b>Java?</b>");

        StandardizedContentDto dto = ModuleContentTransform.transform(q);

        assertEquals("What is Java?", dto.getContent());
    }

    @Test
    void testTransformGapFill() {
        GapFillQuestion g = new GapFillQuestion();
        g.setTypeOfContent(TypeOfContent.GAPFILL);
        g.setQuestionText("I ___ coffee every morning!");

        StandardizedContentDto dto = ModuleContentTransform.transform(g);

        assertEquals("I ___ coffee every morning!", dto.getContent());
    }

    @Test
    void testTransformListeningChoice() {
        ListeningChoiceQuestion l = new ListeningChoiceQuestion();
        l.setTypeOfContent(TypeOfContent.LISTEN_CHOICE);
        l.setQuestion("Listen to this <audio>sound</audio>...");

        StandardizedContentDto dto = ModuleContentTransform.transform(l);

        assertEquals("Listen to this sound...", dto.getContent());
    }

    @Test
    void testTransformReading() {
        Passage p = new Passage();
        p.setTypeOfContent(TypeOfContent.READING);
        p.setContent("<p>This is <b>reading</b> text.</p>");

        StandardizedContentDto dto = ModuleContentTransform.transform(p);

        assertEquals("This is reading text.", dto.getContent());
    }

    @Test
    void testTransformWriting() {
        WritingQuestion w = new WritingQuestion();
        w.setTypeOfContent(TypeOfContent.WRITING);
        w.setQuestion("Write about your <i>hometown</i>.");

        StandardizedContentDto dto = ModuleContentTransform.transform(w);

        assertEquals("Write about your hometown.", dto.getContent());
    }

    @Test
    void testTransformVideo() {
        TeachingVideo v = new TeachingVideo();
        v.setTypeOfContent(TypeOfContent.VIDEO);
        v.setVideoUrl("https://youtube.com/video123");

        StandardizedContentDto dto = ModuleContentTransform.transform(v);

        assertEquals("httpsyoutube.comvideo123", dto.getContent()); // đã bỏ ký tự đặc biệt
    }

    @Test
    void testTransformPdf() {
        PdfDocument pdf = new PdfDocument();
        pdf.setTypeOfContent(TypeOfContent.PDF);
        pdf.setDocUrl("lesson1.pdf");

        StandardizedContentDto dto = ModuleContentTransform.transform(pdf);

        assertEquals("lesson1.pdf", dto.getContent());
    }

    @Test
    void testTransformSpeakingPassage() {
        SpeakingPassageQuestion sp = new SpeakingPassageQuestion();
        sp.setTypeOfContent(TypeOfContent.SPEAKING_PASSAGE);
        sp.setPassage("<p>Speak clearly!</p>");

        StandardizedContentDto dto = ModuleContentTransform.transform(sp);

        assertEquals("Speak clearly!", dto.getContent());
    }

    @Test
    void testTransformSpeakingPicture() {
        SpeakingPictureQuestion sp = new SpeakingPictureQuestion();
        sp.setTypeOfContent(TypeOfContent.SPEAKING_PICTURE);
        sp.setPictureUrl("https://example.com/pic1.png");

        StandardizedContentDto dto = ModuleContentTransform.transform(sp);

        assertEquals("httpsexample.compic1.png", dto.getContent());
    }

    @Test
    void testTransformNullInput() {
        assertNull(ModuleContentTransform.transform(null));
    }
}
