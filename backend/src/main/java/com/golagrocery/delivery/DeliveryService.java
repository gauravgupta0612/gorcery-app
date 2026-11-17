package com.golagrocery.delivery;

import com.golagrocery.audit.AuditService;
import com.golagrocery.common.ApiException;
import com.golagrocery.common.Crypto;
import com.golagrocery.common.PageDto;
import com.golagrocery.order.*;
import com.golagrocery.payment.PaymentRepository;
import com.golagrocery.user.User;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private static final Set<String> ACTIVE = Set.of("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY");
    private final DeliveryPartnerRepository partners;
    private final DeliveryAssignmentRepository assignments;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final OrderNotifier notifier;
    private final AuditService audit;

    public record AssignmentDto(Long assignmentId, String assignmentStatus, Instant assignedAt, Instant pickedUpAt, Instant deliveredAt,
                                Long orderId, String orderNumber, String orderStatus, String paymentMethod, String paymentStatus,
                                BigDecimal amountToCollect, BigDecimal grandTotal, int itemCount,
                                List<OrderDtos.ItemDto> items, OrderDtos.AddressSnapshot address, String deliveryInstructions,
                                String navigationUrl) {}

    public record PartnerProfile(Long id, String name, String phone, String vehicleType, String vehicleNumber, boolean available,
                                 long activeCount, long deliveredCount) {}

    // ---------------------------------------------------------------- admin side
    @Transactional
    public OrderDtos.OrderDetailDto assign(Long orderId, Long partnerId, Long adminId, OrderService orderService) {
        Order o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (o.getStatus() != OrderStatus.PACKED && o.getStatus() != OrderStatus.ASSIGNED) {
            throw ApiException.badRequest("Only packed orders can be assigned to a delivery partner");
        }
        DeliveryPartner p = partners.findById(partnerId).filter(DeliveryPartner::isActive)
            .orElseThrow(() -> ApiException.notFound("Active delivery partner"));
        assignments.findFirstByOrderIdAndStatusNot(orderId, "CANCELLED").ifPresent(a -> {
            if (!"ASSIGNED".equals(a.getStatus())) throw ApiException.conflict("Order already picked up; cannot reassign");
            a.setStatus("CANCELLED");
            assignments.saveAndFlush(a);
        });
        DeliveryAssignment a = new DeliveryAssignment();
        a.setOrderId(orderId);
        a.setDeliveryPartnerId(partnerId);
        a.setStatus("ASSIGNED");
        assignments.save(a);
        boolean firstAssignment = o.getStatus() != OrderStatus.ASSIGNED;
        o.setStatus(OrderStatus.ASSIGNED);
        String name = users.findById(p.getUserId()).map(User::getFullName).orElse("partner");
        o.addHistory(OrderStatus.ASSIGNED, "Assigned to " + name, adminId);
        orders.save(o);
        if (firstAssignment) notifier.statusChanged(o);
        audit.log("ORDER_ASSIGNED", "Order", orderId, "partnerId=" + partnerId);
        return orderService.toDetail(o, false);
    }

    // ---------------------------------------------------------------- partner side
    private DeliveryPartner me(Long userId) {
        DeliveryPartner p = partners.findByUserId(userId).orElseThrow(() -> ApiException.forbidden("Not a delivery partner"));
        if (!p.isActive()) throw ApiException.forbidden("Your delivery partner account is inactive");
        return p;
    }

    @Transactional(readOnly = true)
    public PartnerProfile profile(Long userId) {
        DeliveryPartner p = me(userId);
        User u = users.findById(userId).orElseThrow();
        return new PartnerProfile(p.getId(), u.getFullName(), u.getPhone(), p.getVehicleType(), p.getVehicleNumber(), p.isAvailable(),
            assignments.countByDeliveryPartnerIdAndStatusIn(p.getId(), ACTIVE),
            assignments.countByDeliveryPartnerIdAndStatus(p.getId(), "DELIVERED"));
    }

    @Transactional
    public PartnerProfile setAvailability(Long userId, boolean available) {
        DeliveryPartner p = me(userId);
        p.setAvailable(available);
        partners.save(p);
        return profile(userId);
    }

    @Transactional
    public void updateLocation(Long userId, double lat, double lng) {
        DeliveryPartner p = me(userId);
        p.setCurrentLat(lat);
        p.setCurrentLng(lng);
        partners.save(p);
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> active(Long userId) {
        DeliveryPartner p = me(userId);
        return assignments.findByDeliveryPartnerIdAndStatusInOrderByAssignedAtAsc(p.getId(), ACTIVE).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageDto<AssignmentDto> history(Long userId, int page) {
        DeliveryPartner p = me(userId);
        return PageDto.of(assignments.findByDeliveryPartnerIdAndStatusOrderByDeliveredAtDesc(p.getId(), "DELIVERED",
            PageRequest.of(Math.max(page, 0), 20)), this::toDto);
    }

    @Transactional(readOnly = true)
    public AssignmentDto get(Long userId, Long assignmentId) {
        return toDto(owned(userId, assignmentId));
    }

    @Transactional
    public AssignmentDto pickup(Long userId, Long assignmentId) {
        DeliveryAssignment a = owned(userId, assignmentId);
        if (!"ASSIGNED".equals(a.getStatus())) throw ApiException.badRequest("Order is already " + a.getStatus());
        a.setStatus("PICKED_UP");
        a.setPickedUpAt(Instant.now());
        assignments.save(a);
        Order o = orders.findById(a.getOrderId()).orElseThrow();
        o.addHistory(OrderStatus.ASSIGNED, "Picked up from store by delivery partner", userId);
        orders.save(o);
        audit.log("ORDER_PICKED_UP", "Order", o.getId(), null);
        return toDto(a);
    }

    @Transactional
    public AssignmentDto outForDelivery(Long userId, Long assignmentId) {
        DeliveryAssignment a = owned(userId, assignmentId);
        if (!"PICKED_UP".equals(a.getStatus())) throw ApiException.badRequest("Confirm pickup first");
        a.setStatus("OUT_FOR_DELIVERY");
        assignments.save(a);
        Order o = orders.findById(a.getOrderId()).orElseThrow();
        o.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        o.addHistory(OrderStatus.OUT_FOR_DELIVERY, null, userId);
        orders.save(o);
        notifier.statusChanged(o);
        return toDto(a);
    }

    /** Delivery is confirmed only with the customer's OTP. For COD the collected cash amount must match. */
    @Transactional(noRollbackFor = ApiException.class)
    public AssignmentDto deliver(Long userId, Long assignmentId, String otp, BigDecimal codCollected) {
        DeliveryAssignment a = owned(userId, assignmentId);
        if (!"OUT_FOR_DELIVERY".equals(a.getStatus())) throw ApiException.badRequest("Mark the order out for delivery first");
        Order o = orders.findById(a.getOrderId()).orElseThrow();
        if (!Crypto.constantTimeEquals(o.getDeliveryOtp(), otp == null ? "" : otp.trim())) {
            audit.logAs(userId, "DELIVERY_PARTNER", "DELIVERY_OTP_MISMATCH", "Order", o.getId(), null);
            throw ApiException.badRequest("Incorrect delivery OTP. Ask the customer for the 4-digit OTP shown in their app.");
        }
        if (o.getPaymentMethod() == PaymentMethod.COD) {
            if (codCollected == null || codCollected.compareTo(o.getGrandTotal()) != 0) {
                throw ApiException.badRequest("Collect exactly ₹" + o.getGrandTotal() + " in cash before confirming");
            }
            a.setCodCollected(codCollected);
            payments.findFirstByOrderIdOrderByIdDesc(o.getId()).ifPresent(p -> {
                p.setStatus("COD_COLLECTED");
                payments.save(p);
            });
            o.setPaymentStatus(PaymentStatus.PAID);
        }
        Instant now = Instant.now();
        a.setStatus("DELIVERED");
        a.setDeliveredAt(now);
        assignments.save(a);
        o.setStatus(OrderStatus.DELIVERED);
        o.setDeliveredAt(now);
        o.addHistory(OrderStatus.DELIVERED, "Delivered (OTP verified)", userId);
        orders.save(o);
        notifier.statusChanged(o);
        audit.log("ORDER_DELIVERED", "Order", o.getId(), o.getPaymentMethod() == PaymentMethod.COD ? "COD ₹" + codCollected : null);
        return toDto(a);
    }

    private DeliveryAssignment owned(Long userId, Long assignmentId) {
        DeliveryPartner p = me(userId);
        return assignments.findById(assignmentId).filter(a -> a.getDeliveryPartnerId().equals(p.getId()))
            .orElseThrow(() -> ApiException.notFound("Assignment"));
    }

    private AssignmentDto toDto(DeliveryAssignment a) {
        Order o = orders.findById(a.getOrderId()).orElseThrow();
        String dest = o.getShipLat() != null && o.getShipLng() != null
            ? o.getShipLat() + "," + o.getShipLng()
            : java.net.URLEncoder.encode(o.getShipLine1() + ", " + o.getShipCity() + " " + o.getShipPincode(), java.nio.charset.StandardCharsets.UTF_8);
        BigDecimal toCollect = o.getPaymentMethod() == PaymentMethod.COD && o.getPaymentStatus() != PaymentStatus.PAID
            ? o.getGrandTotal() : BigDecimal.ZERO;
        return new AssignmentDto(a.getId(), a.getStatus(), a.getAssignedAt(), a.getPickedUpAt(), a.getDeliveredAt(), o.getId(),
            o.getOrderNumber(), o.getStatus().name(), o.getPaymentMethod().name(), o.getPaymentStatus().name(), toCollect,
            o.getGrandTotal(), o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
            o.getItems().stream().map(OrderDtos.ItemDto::of).toList(), OrderDtos.AddressSnapshot.of(o), o.getDeliveryInstructions(),
            "https://www.google.com/maps/dir/?api=1&destination=" + dest + "&travelmode=driving");
    }
}
