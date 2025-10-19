package com.jpd.web.controller.customer;

import com.jpd.web.dto.Request.AiWritingRequest;
import com.jpd.web.dto.Response.AiWritingResponse;
import com.jpd.web.service.AIService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/customer/AIWriting")
public class AiWritingController {
    @Autowired
    private AIService aiService;
    @PostMapping("/grade/{questionId}")
    public ResponseEntity<AiWritingResponse> gradeEssay(@PathVariable("questionId") long questionId,
            @Valid @RequestBody AiWritingRequest aiWritingRequest) {

        AiWritingResponse res =
                aiService.gradeEssay(questionId,aiWritingRequest);
        return ResponseEntity.ok(res);
    }
}
