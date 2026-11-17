package com.golagrocery.user;

import com.golagrocery.common.ApiException;
import com.golagrocery.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressRepository repo;

    public record AddressRequest(
        @NotBlank @Pattern(regexp = "HOME|WORK|OTHER") String label,
        @NotBlank @Size(max = 120) String contactName,
        @NotBlank @Pattern(regexp = AuthDtos.PHONE_REGEX, message = "Enter a valid 10-digit mobile number") String contactPhone,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @Size(max = 120) String landmark,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 80) String state,
        @NotBlank @Pattern(regexp = "^[1-9]\\d{5}$", message = "Enter a valid 6-digit pincode") String pincode,
        @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @com.fasterxml.jackson.annotation.JsonProperty("isDefault") boolean isDefault) {}

    public record AddressDto(Long id, String label, String contactName, String contactPhone, String line1, String line2,
                             String landmark, String city, String state, String pincode, Double latitude, Double longitude,
                             @com.fasterxml.jackson.annotation.JsonProperty("isDefault") boolean isDefault) {
        static AddressDto of(Address a) {
            return new AddressDto(a.getId(), a.getLabel(), a.getContactName(), a.getContactPhone(), a.getLine1(), a.getLine2(),
                a.getLandmark(), a.getCity(), a.getState(), a.getPincode(), a.getLatitude(), a.getLongitude(), a.isDefault());
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AddressDto> list() {
        return repo.findByUserIdOrderByIsDefaultDescIdDesc(CurrentUser.id()).stream().map(AddressDto::of).toList();
    }

    @PostMapping
    @Transactional
    public AddressDto create(@Valid @RequestBody AddressRequest req) {
        Long uid = CurrentUser.id();
        if (repo.countByUserId(uid) >= 20) throw ApiException.badRequest("You can save up to 20 addresses");
        Address a = new Address();
        a.setUserId(uid);
        boolean makeDefault = req.isDefault() || repo.countByUserId(uid) == 0;
        apply(a, req, makeDefault, uid);
        return AddressDto.of(repo.save(a));
    }

    @PutMapping("/{id}")
    @Transactional
    public AddressDto update(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
        Long uid = CurrentUser.id();
        Address a = repo.findByIdAndUserId(id, uid).orElseThrow(() -> ApiException.notFound("Address"));
        apply(a, req, req.isDefault() || a.isDefault(), uid);
        return AddressDto.of(repo.save(a));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Address a = repo.findByIdAndUserId(id, CurrentUser.id()).orElseThrow(() -> ApiException.notFound("Address"));
        repo.delete(a);
        return ResponseEntity.noContent().build();
    }

    private void apply(Address a, AddressRequest r, boolean makeDefault, Long uid) {
        if (makeDefault) repo.clearDefault(uid);
        a.setLabel(r.label());
        a.setContactName(r.contactName().trim());
        a.setContactPhone(r.contactPhone());
        a.setLine1(r.line1().trim());
        a.setLine2(r.line2());
        a.setLandmark(r.landmark());
        a.setCity(r.city().trim());
        a.setState(r.state().trim());
        a.setPincode(r.pincode());
        a.setLatitude(r.latitude());
        a.setLongitude(r.longitude());
        a.setDefault(makeDefault);
    }
}
