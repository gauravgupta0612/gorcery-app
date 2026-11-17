package com.golagrocery.delivery;

import com.golagrocery.common.PageDto;
import com.golagrocery.delivery.DeliveryService.AssignmentDto;
import com.golagrocery.delivery.DeliveryService.PartnerProfile;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService delivery;

    public record Availability(boolean available) {}
    public record Location(@DecimalMin("-90") @DecimalMax("90") double lat, @DecimalMin("-180") @DecimalMax("180") double lng) {}
    public record DeliverRequest(@NotBlank @Pattern(regexp = "\\d{4}") String otp, @PositiveOrZero BigDecimal codCollected) {}

    @GetMapping("/me")
    public PartnerProfile me() { return delivery.profile(CurrentUser.id()); }

    @PutMapping("/me/availability")
    public PartnerProfile availability(@RequestBody Availability req) { return delivery.setAvailability(CurrentUser.id(), req.available()); }

    @PutMapping("/me/location")
    public void location(@Valid @RequestBody Location req) { delivery.updateLocation(CurrentUser.id(), req.lat(), req.lng()); }

    @GetMapping("/assignments")
    public List<AssignmentDto> active() { return delivery.active(CurrentUser.id()); }

    @GetMapping("/assignments/history")
    public PageDto<AssignmentDto> history(@RequestParam(defaultValue = "0") int page) { return delivery.history(CurrentUser.id(), page); }

    @GetMapping("/assignments/{id}")
    public AssignmentDto get(@PathVariable Long id) { return delivery.get(CurrentUser.id(), id); }

    @PostMapping("/assignments/{id}/pickup")
    public AssignmentDto pickup(@PathVariable Long id) { return delivery.pickup(CurrentUser.id(), id); }

    @PostMapping("/assignments/{id}/out-for-delivery")
    public AssignmentDto out(@PathVariable Long id) { return delivery.outForDelivery(CurrentUser.id(), id); }

    @PostMapping("/assignments/{id}/deliver")
    public AssignmentDto deliver(@PathVariable Long id, @Valid @RequestBody DeliverRequest req) {
        return delivery.deliver(CurrentUser.id(), id, req.otp(), req.codCollected());
    }
}
