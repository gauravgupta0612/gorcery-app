package com.golagrocery.admin;

import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.PageDto;
import com.golagrocery.delivery.DeliveryAssignmentRepository;
import com.golagrocery.delivery.DeliveryPartner;
import com.golagrocery.delivery.DeliveryPartnerRepository;
import com.golagrocery.order.OrderDtos.OrderSummaryDto;
import com.golagrocery.order.OrderRepository;
import com.golagrocery.user.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPeopleController {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final DeliveryPartnerRepository partners;
    private final DeliveryAssignmentRepository assignments;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final JdbcTemplate jdbc;

    public record CustomerRow(Long id, String fullName, String phone, String email, boolean enabled, boolean verified, Instant createdAt,
                              long orderCount, BigDecimal totalSpent) {}
    public record CustomerDetail(CustomerRow customer, List<AddressController.AddressDto> addresses, List<OrderSummaryDto> recentOrders) {}

    public record PartnerUpsert(@NotBlank @Size(max = 120) String fullName,
                                @NotBlank @Pattern(regexp = AuthDtos.PHONE_REGEX, message = "Enter a valid 10-digit mobile number") String phone,
                                @Email String email, @Size(min = 8, max = 72) String password,
                                @NotBlank @Pattern(regexp = "BIKE|SCOOTER|BICYCLE|CAR|VAN") String vehicleType,
                                @Size(max = 20) String vehicleNumber, @Size(max = 30) String licenseNumber, boolean active) {}
    public record PartnerRow(Long id, Long userId, String fullName, String phone, String email, String vehicleType, String vehicleNumber,
                             String licenseNumber, boolean available, boolean active, long activeOrders, long delivered) {}

    // ---------------- customers
    @GetMapping("/customers")
    @Transactional(readOnly = true)
    public PageDto<CustomerRow> customers(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return PageDto.of(users.searchByRole(Role.CUSTOMER, q == null ? "" : q.trim(),
            PageRequest.of(page, Math.min(size, 100), org.springframework.data.domain.Sort.by("createdAt").descending())), this::row);
    }

    private CustomerRow row(User u) {
        Map<String, Object> s = jdbc.queryForMap("select count(*) c, coalesce(sum(grand_total),0) t from orders where user_id = ? and status = 'DELIVERED'", u.getId());
        Long all = jdbc.queryForObject("select count(*) from orders where user_id = ?", Long.class, u.getId());
        return new CustomerRow(u.getId(), u.getFullName(), u.getPhone(), u.getEmail(), u.isEnabled(), u.isPhoneVerified(), u.getCreatedAt(),
            all == null ? 0 : all, (BigDecimal) s.get("t"));
    }

    @GetMapping("/customers/{id}")
    @Transactional(readOnly = true)
    public CustomerDetail customer(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        return new CustomerDetail(row(u),
            addresses.findByUserIdOrderByIsDefaultDescIdDesc(id).stream().map(a -> new AddressController.AddressDto(a.getId(), a.getLabel(),
                a.getContactName(), a.getContactPhone(), a.getLine1(), a.getLine2(), a.getLandmark(), a.getCity(), a.getState(),
                a.getPincode(), a.getLatitude(), a.getLongitude(), a.isDefault())).toList(),
            orders.findByUserIdOrderByPlacedAtDesc(id, PageRequest.of(0, 20)).map(OrderSummaryDto::of).getContent());
    }

    @PatchMapping("/customers/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CustomerRow setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        u.setEnabled(enabled);
        users.save(u);
        if (!enabled) refreshTokens.revokeAllForUser(id);
        audit.log(enabled ? "CUSTOMER_ENABLED" : "CUSTOMER_DISABLED", "User", id, null);
        return row(u);
    }

    // ---------------- delivery partners
    @GetMapping("/delivery-partners")
    @Transactional(readOnly = true)
    public List<PartnerRow> partners() {
        return partners.findAllByOrderByIdDesc().stream().map(this::partnerRow).toList();
    }

    private PartnerRow partnerRow(DeliveryPartner p) {
        User u = users.findById(p.getUserId()).orElseThrow();
        return new PartnerRow(p.getId(), u.getId(), u.getFullName(), u.getPhone(), u.getEmail(), p.getVehicleType(), p.getVehicleNumber(),
            p.getLicenseNumber(), p.isAvailable(), p.isActive(),
            assignments.countByDeliveryPartnerIdAndStatusIn(p.getId(), Set.of("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY")),
            assignments.countByDeliveryPartnerIdAndStatus(p.getId(), "DELIVERED"));
    }

    @PostMapping("/delivery-partners")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PartnerRow createPartner(@Valid @RequestBody PartnerUpsert req) {
        if (!StringUtils.hasText(req.password())) throw ApiException.badRequest("Password is required (min 8 characters)");
        User u = users.findByPhone(req.phone()).orElse(null);
        if (u != null && partners.findByUserId(u.getId()).isPresent()) throw ApiException.conflict("This phone is already a delivery partner");
        if (u == null) {
            u = new User();
            u.setPhone(req.phone());
            u.setPhoneVerified(true);
        }
        u.setFullName(req.fullName().trim());
        if (StringUtils.hasText(req.email())) u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.getRoles().add(roles.findByName(Role.DELIVERY_PARTNER).orElseThrow());
        users.save(u);
        DeliveryPartner p = new DeliveryPartner();
        p.setUserId(u.getId());
        applyPartner(p, req);
        partners.save(p);
        audit.log("DELIVERY_PARTNER_CREATED", "DeliveryPartner", p.getId(), u.getFullName());
        return partnerRow(p);
    }

    @PutMapping("/delivery-partners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public PartnerRow updatePartner(@PathVariable Long id, @Valid @RequestBody PartnerUpsert req) {
        DeliveryPartner p = partners.findById(id).orElseThrow(() -> ApiException.notFound("Delivery partner"));
        User u = users.findById(p.getUserId()).orElseThrow();
        if (!u.getPhone().equals(req.phone()) && users.findByPhone(req.phone()).isPresent()) throw ApiException.conflict("Phone already in use");
        u.setFullName(req.fullName().trim());
        u.setPhone(req.phone());
        u.setEmail(StringUtils.hasText(req.email()) ? req.email().trim().toLowerCase() : null);
        if (StringUtils.hasText(req.password())) {
            u.setPasswordHash(encoder.encode(req.password()));
            refreshTokens.revokeAllForUser(u.getId());
        }
        u.setEnabled(req.active());
        users.save(u);
        applyPartner(p, req);
        partners.save(p);
        audit.log("DELIVERY_PARTNER_UPDATED", "DeliveryPartner", id, null);
        return partnerRow(p);
    }

    private void applyPartner(DeliveryPartner p, PartnerUpsert r) {
        p.setVehicleType(r.vehicleType());
        p.setVehicleNumber(r.vehicleNumber());
        p.setLicenseNumber(r.licenseNumber());
        p.setActive(r.active());
    }
}
