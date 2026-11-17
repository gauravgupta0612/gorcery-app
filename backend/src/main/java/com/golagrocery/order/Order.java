package com.golagrocery.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "CustomerOrder")
@Table(name = "orders")
@Getter @Setter
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderNumber;
    private Long userId;
    @Enumerated(EnumType.STRING) private OrderStatus status;
    @Enumerated(EnumType.STRING) private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus;

    private String shipName;
    private String shipPhone;
    private String shipLine1;
    private String shipLine2;
    private String shipLandmark;
    private String shipCity;
    private String shipState;
    private String shipPincode;
    private Double shipLat;
    private Double shipLng;

    private BigDecimal itemTotal;
    private BigDecimal mrpTotal;
    private String couponCode;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    private BigDecimal handlingFee = BigDecimal.ZERO;
    private BigDecimal taxTotal = BigDecimal.ZERO;
    private BigDecimal grandTotal;
    private String deliveryInstructions;
    private String cancelReason;
    private String deliveryOtp;
    private Instant placedAt = Instant.now();
    private Instant deliveredAt;
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    public void addHistory(OrderStatus s, String note, Long by) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(this);
        h.setStatus(s.name());
        h.setNote(note);
        h.setChangedBy(by);
        history.add(h);
    }

    @PreUpdate
    void touch() { updatedAt = Instant.now(); }
}
