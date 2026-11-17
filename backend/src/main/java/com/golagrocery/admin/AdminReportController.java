package com.golagrocery.admin;

import com.golagrocery.catalog.InventoryRepository;
import com.golagrocery.order.OrderRepository;
import com.golagrocery.order.OrderStatus;
import com.golagrocery.store.StoreService;
import com.golagrocery.support.SupportTicketRepository;
import com.golagrocery.user.Role;
import com.golagrocery.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminReportController {
    private static final String COUNTED = "status not in ('CANCELLED','PAYMENT_FAILED','PENDING_PAYMENT')";
    private final JdbcTemplate jdbc;
    private final OrderRepository orders;
    private final UserRepository users;
    private final InventoryRepository inventory;
    private final SupportTicketRepository tickets;

    public record Point(String date, long orders, BigDecimal revenue) {}
    public record Named(String name, long count, BigDecimal amount) {}
    public record Dashboard(long todayOrders, BigDecimal todayRevenue, long pendingOrders, long outForDelivery, long totalCustomers,
                            long newCustomers7d, long lowStockItems, long openTickets, List<Point> last14Days,
                            List<Named> ordersByStatus, List<Named> topProducts30d, List<Named> paymentSplit30d) {}
    public record SalesRow(String date, long orders, BigDecimal itemTotal, BigDecimal discounts, BigDecimal deliveryFees,
                           BigDecimal handlingFees, BigDecimal tax, BigDecimal netRevenue, BigDecimal refunds) {}
    public record SalesReport(LocalDate from, LocalDate to, List<SalesRow> rows, SalesRow totals, List<Named> topProducts,
                              List<Named> categorySales, List<Named> paymentSplit) {}

    private Timestamp startOfDay(LocalDate d) { return Timestamp.from(d.atStartOfDay(StoreService.IST).toInstant()); }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        LocalDate today = LocalDate.now(StoreService.IST);
        Timestamp t0 = startOfDay(today);
        Map<String, Object> todays = jdbc.queryForMap("select count(*) c, coalesce(sum(grand_total),0) r from orders where placed_at >= ? and " + COUNTED, t0);
        long pending = orders.countByStatus(OrderStatus.PLACED) + orders.countByStatus(OrderStatus.CONFIRMED)
            + orders.countByStatus(OrderStatus.PREPARING) + orders.countByStatus(OrderStatus.PACKED) + orders.countByStatus(OrderStatus.ASSIGNED);
        List<Point> series = jdbc.query("""
            select to_char(d::date, 'YYYY-MM-DD') as day_label, count(o.id) c, coalesce(sum(o.grand_total),0) r
            from generate_series((now() at time zone 'Asia/Kolkata')::date - 13, (now() at time zone 'Asia/Kolkata')::date, interval '1 day') d
            left join orders o on (o.placed_at at time zone 'Asia/Kolkata')::date = d::date and o.""" + COUNTED + """
             group by d order by d""", (rs, n) -> new Point(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)));
        List<Named> byStatus = jdbc.query("select status, count(*), coalesce(sum(grand_total),0) from orders group by status order by 2 desc",
            (rs, n) -> new Named(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)));
        Timestamp d30 = Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS));
        return new Dashboard(((Number) todays.get("c")).longValue(), (BigDecimal) todays.get("r"), pending,
            orders.countByStatus(OrderStatus.OUT_FOR_DELIVERY), users.countByRole(Role.CUSTOMER),
            users.countByRoleSince(Role.CUSTOMER, Instant.now().minus(7, ChronoUnit.DAYS)), inventory.countLowStock(),
            tickets.countByStatus("OPEN"), series, byStatus, topProducts(d30, Timestamp.from(Instant.now()), 8), paymentSplit(d30, Timestamp.from(Instant.now())));
    }

    private List<Named> topProducts(Timestamp from, Timestamp to, int limit) {
        return jdbc.query("""
            select i.product_name, sum(i.quantity), sum(i.line_total) from order_items i join orders o on o.id = i.order_id
            where o.placed_at >= ? and o.placed_at < ? and o.""" + COUNTED + """
             group by i.product_name order by 3 desc limit ?""",
            (rs, n) -> new Named(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)), from, to, limit);
    }

    private List<Named> paymentSplit(Timestamp from, Timestamp to) {
        return jdbc.query("select payment_method, count(*), coalesce(sum(grand_total),0) from orders where placed_at >= ? and placed_at < ? and "
            + COUNTED + " group by payment_method order by 3 desc", (rs, n) -> new Named(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)), from, to);
    }

    @GetMapping("/reports/sales")
    @Transactional(readOnly = true)
    public SalesReport sales(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        LocalDate t = to == null ? LocalDate.now(StoreService.IST) : to;
        LocalDate f = from == null ? t.minusDays(29) : from;
        if (f.isAfter(t)) { LocalDate x = f; f = t; t = x; }
        if (ChronoUnit.DAYS.between(f, t) > 366) f = t.minusDays(366);
        Timestamp a = startOfDay(f), b = startOfDay(t.plusDays(1));
        List<SalesRow> rows = jdbc.query("""
            select to_char((o.placed_at at time zone 'Asia/Kolkata')::date, 'YYYY-MM-DD') as day_label, count(*),
                   sum(o.item_total), sum(o.coupon_discount), sum(o.delivery_fee), sum(o.handling_fee), sum(o.tax_total), sum(o.grand_total),
                   coalesce(sum((select sum(r.amount) from refunds r where r.order_id = o.id and r.status in ('PROCESSED','APPROVED'))),0)
            from orders o where o.placed_at >= ? and o.placed_at < ? and o.""" + COUNTED + """
             group by 1 order by 1""",
            (rs, n) -> new SalesRow(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5),
                rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8), rs.getBigDecimal(9)), a, b);
        SalesRow totals = new SalesRow("TOTAL", rows.stream().mapToLong(SalesRow::orders).sum(),
            sum(rows, SalesRow::itemTotal), sum(rows, SalesRow::discounts), sum(rows, SalesRow::deliveryFees),
            sum(rows, SalesRow::handlingFees), sum(rows, SalesRow::tax), sum(rows, SalesRow::netRevenue), sum(rows, SalesRow::refunds));
        List<Named> categories = jdbc.query("""
            select coalesce(pc.name, c.name), sum(i.quantity), sum(i.line_total) from order_items i join orders o on o.id = i.order_id
            join products p on p.id = i.product_id join categories c on c.id = p.category_id left join categories pc on pc.id = c.parent_id
            where o.placed_at >= ? and o.placed_at < ? and o.""" + COUNTED + """
             group by 1 order by 3 desc""", (rs, n) -> new Named(rs.getString(1), rs.getLong(2), rs.getBigDecimal(3)), a, b);
        return new SalesReport(f, t, rows, totals, topProducts(a, b, 20), categories, paymentSplit(a, b));
    }

    private static BigDecimal sum(List<SalesRow> rows, java.util.function.Function<SalesRow, BigDecimal> f) {
        return rows.stream().map(f).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/reports/sales.csv")
    public ResponseEntity<String> salesCsv(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        SalesReport r = sales(from, to);
        StringBuilder sb = new StringBuilder("date,orders,item_total,coupon_discounts,delivery_fees,handling_fees,gst_included,net_revenue,refunds\n");
        for (SalesRow x : r.rows()) row(sb, x);
        row(sb, r.totals());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=gola-sales-" + r.from() + "-to-" + r.to() + ".csv")
            .contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
    }

    private void row(StringBuilder sb, SalesRow x) {
        sb.append(String.join(",", x.date(), String.valueOf(x.orders()), s(x.itemTotal()), s(x.discounts()), s(x.deliveryFees()),
            s(x.handlingFees()), s(x.tax()), s(x.netRevenue()), s(x.refunds()))).append('\n');
    }

    private static String s(BigDecimal v) { return v == null ? "0" : v.toPlainString(); }
}
