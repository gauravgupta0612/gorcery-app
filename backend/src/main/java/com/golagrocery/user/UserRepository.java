package com.golagrocery.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("""
        select distinct u from User u join u.roles r
        where r.name = :role and (:q = '' or lower(u.fullName) like lower(concat('%', :q, '%'))
              or u.phone like concat('%', :q, '%') or lower(coalesce(u.email, '')) like lower(concat('%', :q, '%')))
        """)
    Page<User> searchByRole(@Param("role") String role, @Param("q") String q, Pageable pageable);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :role")
    long countByRole(@Param("role") String role);

    @Query("select count(distinct u) from User u join u.roles r where r.name = :role and u.createdAt >= :since")
    long countByRoleSince(@Param("role") String role, @Param("since") Instant since);
}
