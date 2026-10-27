package com.example.dineX.feedback;

import com.example.dineX.feedback.FeedbackDtos.FeedbackView;
import com.example.dineX.security.AuthUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public List<FeedbackView> list(@AuthenticationPrincipal AuthUser user) {
        return feedbackService.list(user.hotelId());
    }
}
