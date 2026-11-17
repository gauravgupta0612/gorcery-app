package com.golagrocery.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    Page<Review> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Modifying
    @Query(value = """
        update products p set avg_rating = coalesce((select round(avg(rating)::numeric, 2) from reviews r where r.product_id = p.id and r.approved), 0),
               rating_count = (select count(*) from reviews r where r.product_id = p.id and r.approved)
        where p.id = :productId
        """, nativeQuery = true)
    void refreshProductRating(Long productId);

    @Query(value = """
        select exists(select 1 from orders o join order_items i on i.order_id = o.id
                      where o.user_id = :userId and i.product_id = :productId and o.status = 'DELIVERED')
        """, nativeQuery = true)
    boolean hasPurchased(Long userId, Long productId);
}
