package com.example.dineX.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class FeedbackDtos {
    private FeedbackDtos() { }

    public record FeedbackRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comments) { }

    public record FeedbackView(Long id, String sessionNumber, String tableNumber, int rating, String comments,
                               LocalDateTime createdAt) { }
}
