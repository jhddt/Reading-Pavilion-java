package com.jhddt.module.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewTaskMessage implements Serializable {

    private Long reviewId;
    private Long essayId;
    private Long ruleId;
    private Long batchTaskId;
}
