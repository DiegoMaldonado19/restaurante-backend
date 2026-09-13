package com.cunoc.restaurant.billing.dto;

import com.cunoc.restaurant.billing.model.ServiceRating;
import java.time.LocalDateTime;

public record ServiceRatingView(
    Long serviceRatingId,
    Long invoiceId,
    Long waiterId,
    int score,
    String commentText,
    LocalDateTime createdAt
)
{
    public static ServiceRatingView from(ServiceRating entity)
    {
        return new ServiceRatingView(
            entity.getServiceRatingId(),
            entity.getInvoiceId(),
            entity.getWaiterId(),
            entity.getScore(),
            entity.getCommentText(),
            entity.getCreatedAt()
        );
    }
}
