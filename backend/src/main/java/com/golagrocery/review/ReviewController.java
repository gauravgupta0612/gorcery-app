package com.golagrocery.review;

import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.security.CurrentUser;
import com.golagrocery.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewRepository reviews;
    private final UserRepository users;

    public record CreateReview(@NotNull Long productId, @Min(1) @Max(5) int rating, @Size(max = 120) String title,
                               @Size(max = 1000) String comment) {}
    public record ReviewDto(Long id, Long productId, String userName, int rating, String title, String comment, Instant createdAt) {}

    @GetMapping("/catalog/products/{productId}/reviews")
    @Transactional(readOnly = true)
    public PageDto<ReviewDto> list(@PathVariable Long productId, @RequestParam(defaultValue = "0") int page) {
        return PageDto.of(reviews.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId, PageRequest.of(page, 20)), this::toDto);
    }

    @PostMapping("/reviews")
    @Transactional
    public ReviewDto create(@Valid @RequestBody CreateReview req) {
        Long uid = CurrentUser.id();
        if (!reviews.hasPurchased(uid, req.productId())) {
            throw ApiException.forbidden("You can review products from your delivered orders");
        }
        if (reviews.existsByProductIdAndUserId(req.productId(), uid)) throw ApiException.conflict("You have already reviewed this product");
        Review r = new Review();
        r.setProductId(req.productId());
        r.setUserId(uid);
        r.setRating((short) req.rating());
        r.setTitle(req.title());
        r.setComment(req.comment());
        reviews.save(r);
        reviews.flush();
        reviews.refreshProductRating(req.productId());
        return toDto(r);
    }

    private ReviewDto toDto(Review r) {
        String name = users.findById(r.getUserId()).map(u -> {
            String[] parts = u.getFullName().trim().split("\\s+");
            return parts.length > 1 ? parts[0] + " " + parts[parts.length - 1].charAt(0) + "." : parts[0];
        }).orElse("Customer");
        return new ReviewDto(r.getId(), r.getProductId(), name, r.getRating(), r.getTitle(), r.getComment(), r.getCreatedAt());
    }
}
