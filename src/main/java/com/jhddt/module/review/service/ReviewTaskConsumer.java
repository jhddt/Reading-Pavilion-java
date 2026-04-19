package com.jhddt.module.review.service;

import com.jhddt.module.review.dto.ReviewTaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewTaskConsumer {

    private final ReviewService reviewService;

    @RabbitListener(queues = "${review.mq.queue}")
    public void consumeReviewTask(ReviewTaskMessage message) {
        if (message == null || message.getReviewId() == null || message.getEssayId() == null) {
            log.warn("收到无效批改消息: {}", message);
            return;
        }
        reviewService.processReviewTask(message.getReviewId(), message.getEssayId(), message.getRuleId(), message.getBatchTaskId());
    }
}
