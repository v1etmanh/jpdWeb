package com.jpd.web.transform;

import com.jpd.web.dto.Response.FeedbackDtoResponse;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Feedback;

public class FeedbackTransForm {
    public static FeedbackDtoResponse transformFeedbackDtoResponse(Enrollment enrollment) {
        if (enrollment == null) return null;
        FeedbackDtoResponse.FeedbackDtoResponseBuilder builder = FeedbackDtoResponse.builder();
        builder.user(enrollment.getCustomer().getUsername());
        builder.createAt(enrollment.getFeedback().getCreateAt());
        builder.context(enrollment.getFeedback().getContent());
        builder.rating(enrollment.getFeedback().getRate());
        return builder.build();
    }
}
