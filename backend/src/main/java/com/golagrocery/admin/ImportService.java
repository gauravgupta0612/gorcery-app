package com.golagrocery.admin;

import com.golagrocery.admin.AdminCatalogDtos.*;
import com.golagrocery.audit.AuditService;
import com.golagrocery.catalog.*;
import com.golagrocery.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Bulk product import from CSV or Excel (.xlsx). One row = one variant; rows sharing the same product_name
 * (and category) become variants of one product. Existing SKUs are updated (price, stock, status).
 */
@Service
@RequiredArgsConstructor
public class ImportService {
    public static final List<String> COLUMNS = List.of("product_name", "category", "subcategory", "brand", "description",
        "sku", "barcode", "variant_label", "quantity_value", "unit", "mrp", "selling_price", "gst_rate", "hsn_code", "stock",
        "low_stock_threshold", "image_url", "active");

    private final AuditService audit;
    private final ImportRowImporter rowImporter;

    public String template() {
        return String.join(",", COLUMNS) + "\n" +
            "Organic Toor Dal,\"Atta, Rice & Dal\",Dals & Pulses,Gola Select,Unpolished toor dal,GG-AR-9001,8901234567893,1 kg,1,KG,199,169,0,0713,50,10,,true\n" +
            "Organic Toor Dal,\"Atta, Rice & Dal\",Dals & Pulses,Gola Select,Unpolished toor dal,GG-AR-9002,,500 g,500,G,105,89,0,0713,40,10,,true\n";
    }

    public ImportResult importFile(MultipartFile file, Long actorId) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose a CSV or XLSX file");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<Map<String, String>> rows;
        try (InputStream in = file.getInputStream()) {
            rows = name.endsWith(".xlsx") ? readXlsx(in) : readCsv(in);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("Could not read the file. Use the CSV template or an .xlsx with the same columns.");
        }
        if (rows.size() > 5000) throw ApiException.badRequest("Maximum 5000 rows per import");

        int created = 0, updated = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> r = rows.get(i);
            if (r.values().stream().allMatch(v -> v == null || v.isBlank())) continue;
            String key = (r.getOrDefault("product_name", "") + "|" + r.getOrDefault("category", "") + "|" + r.getOrDefault("subcategory", "")).toLowerCase(Locale.ROOT);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }
        for (var g : groups.values()) {
            try {
                int[] res = rowImporter.importGroup(g.stream().map(rows::get).toList(), actorId);
                created += res[0];
                updated += res[1];
            } catch (ApiException | IllegalArgumentException e) {
                skipped += g.size();
                errors.add("Row(s) " + g.stream().map(i -> String.valueOf(i + 2)).reduce((a, b) -> a + "," + b).orElse("") + ": " + e.getMessage());
            }
        }
        audit.log("PRODUCTS_IMPORTED", "Product", null, "created=" + created + " updated=" + updated + " skipped=" + skipped);
        return new ImportResult(created, updated, skipped, errors.size() > 100 ? errors.subList(0, 100) : errors);
    }

    private List<Map<String, String>> readCsv(InputStream in) throws Exception {
        CSVParser p = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build()
            .parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        checkHeaders(p.getHeaderNames());
        List<Map<String, String>> out = new ArrayList<>();
        for (CSVRecord rec : p) {
            Map<String, String> m = new HashMap<>();
            for (String h : p.getHeaderNames()) m.put(h.trim().toLowerCase(Locale.ROOT), rec.isMapped(h) ? rec.get(h) : null);
            out.add(m);
        }
        return out;
    }

    private List<Map<String, String>> readXlsx(InputStream in) throws Exception {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return List.of();
            List<String> headers = new ArrayList<>();
            for (Cell c : it.next()) headers.add(fmt.formatCellValue(c).trim().toLowerCase(Locale.ROOT));
            checkHeaders(headers);
            List<Map<String, String>> out = new ArrayList<>();
            while (it.hasNext()) {
                Row row = it.next();
                Map<String, String> m = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell c = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    m.put(headers.get(i), c == null ? null : fmt.formatCellValue(c).trim());
                }
                out.add(m);
            }
            return out;
        }
    }

    private void checkHeaders(Collection<String> headers) {
        Set<String> h = new HashSet<>();
        headers.forEach(x -> h.add(x.trim().toLowerCase(Locale.ROOT)));
        List<String> missing = List.of("product_name", "category", "sku", "variant_label", "quantity_value", "unit", "mrp", "selling_price")
            .stream().filter(c -> !h.contains(c)).toList();
        if (!missing.isEmpty()) throw ApiException.badRequest("Missing required columns: " + String.join(", ", missing));
    }
}
