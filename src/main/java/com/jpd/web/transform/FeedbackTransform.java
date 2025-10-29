package com.jpd.web.transform;

import com.jpd.web.dto.FeedbackSimpleDto;
import com.jpd.web.model.Feedback;

public class FeedbackTransform {

    public static FeedbackSimpleDto tofeedbackDto(Feedback feedback) {
        return FeedbackSimpleDto.builder()
                .feedbackId(feedback.getFeedbackId())
                .content(feedback.getContent())
                .rate(feedback.getRate())
                .createDate(feedback.getUpdateDate())
                .build();
    }
}
