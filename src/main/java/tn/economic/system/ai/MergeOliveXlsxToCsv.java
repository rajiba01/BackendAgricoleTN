package tn.economic.system.ai;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MergeOliveXlsxToCsv {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter D_MON_YY = DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH); // 25-Oct-20
    private static final DateTimeFormatter MDY_SLASH = DateTimeFormatter.ofPattern("M/d/yyyy"); // 11/4/2024

    public static void main(String[] args) throws Exception {
        String base = "olive_oil/";

        String ds1 = base + "mg-decembre-2019-open-data.xlsx";
        String ds2 = base + "prix-olives-marche-gremda-ckan-1.xlsx";
        String ds3 = base + "prix-olives-marche-gremda-2020-2021.xlsx";
        String ds4 = base + "prix-olives-marche-gremda-2021-2022.xlsx";
        String ds5 = base + "prix-a-la-production-olives-a-huile.xlsx";

        Path out = Paths.get("olive_oil_merged_filtered.csv");

        List<RowOut> rows = new ArrayList<>();
        rows.addAll(parseWide5Regions(loadResource(ds1), "dataset_1", HeaderMapping.DATASET_1));
        rows.addAll(parseWide5Regions(loadResource(ds2), "dataset_2", HeaderMapping.DATASET_2));
        rows.addAll(parseWide5Regions(loadResource(ds3), "dataset_3", HeaderMapping.DATASET_3));
        rows.addAll(parseWide5Regions(loadResource(ds4), "dataset_4", HeaderMapping.DATASET_4));
        rows.addAll(parseGovernorates(loadResource(ds5), "dataset_5"));

        rows.removeIf(r -> r.pxMin == null && r.pxMax == null);

        rows.sort(Comparator
                .comparing((RowOut r) -> r.date)
                .thenComparing(r -> r.region)
                .thenComparing(r -> r.source));

        writeCsv(out, rows);
        System.out.println("OK -> " + out.toAbsolutePath() + " (rows=" + rows.size() + ")");
    }

    // -------------------- loaders --------------------
    private static InputStream loadResource(String path) {
        InputStream in = MergeOliveXlsxToCsv.class.getClassLoader().getResourceAsStream(path);
        if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
        return in;
    }

    // -------------------- parsing datasets #1-#4 (wide) --------------------
    enum HeaderMapping { DATASET_1, DATASET_2, DATASET_3, DATASET_4 }

    private static List<RowOut> parseWide5Regions(InputStream xlsxStream, String source, HeaderMapping mapping) throws IOException {
        List<RowOut> out = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(xlsxStream)) {
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);

            Sheet sh = wb.getSheetAt(0);
            Iterator<org.apache.poi.ss.usermodel.Row> it = sh.iterator();
            if (!it.hasNext()) return out;

            org.apache.poi.ss.usermodel.Row header = it.next();
            Map<String, Integer> idx = headerIndex(header);

            String[] regions = {"sfax", "centre", "sahel", "nord", "sud"};

            while (it.hasNext()) {
                org.apache.poi.ss.usermodel.Row r = it.next();

                String dateStr = getStr(r, idx, evaluator, formatter, "DATE", "date", "Date");
                if (dateStr == null || dateStr.isBlank()) continue;

                LocalDate date;
                try {
                    date = parseAnyDate(dateStr);
                } catch (Exception ex) {
                    // mg-decembre-2019-open-data.xlsx can contain formulas like "A2+1" or non-date rows
                    continue;
                }

                for (String region : regions) {
                    Double qte = getNum(r, idx, evaluator, formatter, qCol(mapping, region));
                    Double pxMin = getNum(r, idx, evaluator, formatter, minCol(mapping, region));
                    Double pxMax = getNum(r, idx, evaluator, formatter, maxCol(mapping, region));

                    // Outliers from your samples (safe -> NA)
                    if (source.equals("dataset_3") && date.equals(LocalDate.of(2019, 11, 24)) && region.equals("sahel")) {
                        if (pxMax != null && pxMax == 150.0) pxMax = null;
                    }
                    if (source.equals("dataset_1") && date.equals(LocalDate.of(2020, 12, 7)) && region.equals("sahel")) {
                        if (pxMax != null && pxMax == 13002.0) pxMax = null;
                    }

                    // swap if inverted
                    Double[] swapped = swapIfInverted(pxMin, pxMax);
                    pxMin = swapped[0];
                    pxMax = swapped[1];

                    if (qte == null && pxMin == null && pxMax == null) continue;

                    Double pxMoy = (pxMin != null && pxMax != null) ? (pxMin + pxMax) / 2.0 : null;
                    Double spread = (pxMin != null && pxMax != null) ? (pxMax - pxMin) : null;

                    out.add(new RowOut(date, "huile_olive", region, qte, pxMin, pxMax, pxMoy, spread, source));
                }
            }
        }

        return out;
    }

    // IMPORTANT: Dataset #2 has different headers.
    private static String qCol(HeaderMapping m, String region) {
        return switch (m) {
            case DATASET_1, DATASET_3, DATASET_4 -> "Qté_Tonne_" + region;
            case DATASET_2 -> switch (region) {
                case "sfax" -> "Qté-Tonne Sfax";
                case "centre" -> "Qté-Tonne-centre";
                case "sahel" -> "Qté-Tonne-sahel";
                case "nord" -> "Qté-Tonne-nord";
                case "sud" -> "Qté-Tonne-sud";
                default -> throw new IllegalArgumentException("Unknown region: " + region);
            };
        };
    }

    private static String minCol(HeaderMapping m, String region) {
        return switch (m) {
            case DATASET_1, DATASET_3, DATASET_4 -> "Px_min_" + region;
            case DATASET_2 -> "Px-min-" + region;
        };
    }

    private static String maxCol(HeaderMapping m, String region) {
        return switch (m) {
            case DATASET_1, DATASET_3, DATASET_4 -> "Px_max_" + region;
            case DATASET_2 -> "Px-max-" + region;
        };
    }

    // -------------------- parsing dataset #5 (governorates) --------------------
    private static List<RowOut> parseGovernorates(InputStream xlsxStream, String source) throws IOException {
        List<RowOut> out = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(xlsxStream)) {
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);

            Sheet sh = wb.getSheetAt(0);
            Iterator<org.apache.poi.ss.usermodel.Row> it = sh.iterator();
            if (!it.hasNext()) return out;

            org.apache.poi.ss.usermodel.Row header = it.next();
            Map<String, Integer> idx = headerIndex(header);

            while (it.hasNext()) {
                org.apache.poi.ss.usermodel.Row r = it.next();

                String dateStr = getStr(r, idx, evaluator, formatter, "DATE", "date", "Date");
                if (dateStr == null || dateStr.isBlank()) continue;

                LocalDate date;
                try {
                    date = parseAnyDate(dateStr);
                } catch (Exception ex) {
                    continue;
                }

                String gov = getStr(r, idx, evaluator, formatter, "Gouvernorat", "gouvernorat");
                if (gov == null || gov.isBlank()) continue;

                Double pxMin = getNum(r, idx, evaluator, formatter, "Prix_min", "Px_min", "prix_min");
                Double pxMax = getNum(r, idx, evaluator, formatter, "Prix_max", "Px_max", "prix_max");

                Double[] swapped = swapIfInverted(pxMin, pxMax);
                pxMin = swapped[0];
                pxMax = swapped[1];

                Double pxMoy = (pxMin != null && pxMax != null) ? (pxMin + pxMax) / 2.0 : null;
                Double spread = (pxMin != null && pxMax != null) ? (pxMax - pxMin) : null;

                out.add(new RowOut(date, "huile_olive", normalizeRegion(gov), null, pxMin, pxMax, pxMoy, spread, source));
            }
        }

        return out;
    }

    // -------------------- excel helpers --------------------
    private static Map<String, Integer> headerIndex(org.apache.poi.ss.usermodel.Row header) {
        Map<String, Integer> idx = new HashMap<>();
        for (Cell c : header) {
            if (c == null) continue;
            if (c.getCellType() != CellType.STRING) continue;
            String key = c.getStringCellValue().trim();
            if (!key.isEmpty()) idx.put(key, c.getColumnIndex());
        }
        return idx;
    }

    private static String getStr(org.apache.poi.ss.usermodel.Row r,
                                 Map<String, Integer> idx,
                                 FormulaEvaluator evaluator,
                                 DataFormatter formatter,
                                 String... names) {
        Integer col = findCol(idx, names);
        if (col == null) return null;
        Cell c = r.getCell(col);
        if (c == null) return null;

        CellType t = c.getCellType();

        // If it's a formula (=A2+1), evaluate it
        if (t == CellType.FORMULA) {
            CellValue cv = evaluator.evaluate(c);
            if (cv == null) return null;

            return switch (cv.getCellType()) {
                case STRING -> cv.getStringValue();
                case NUMERIC -> {
                    // If Excel cell is formatted as a date, return yyyy-MM-dd
                    if (DateUtil.isCellDateFormatted(c)) {
                        yield c.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    yield Double.toString(cv.getNumberValue());
                }
                case BOOLEAN -> Boolean.toString(cv.getBooleanValue());
                default -> null;
            };
        }

        // If it's a date stored as numeric value
        if (t == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getLocalDateTimeCellValue().toLocalDate().toString();
        }

        // Otherwise: take the "displayed" text in Excel
        String s = formatter.formatCellValue(c, evaluator);
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static Double getNum(org.apache.poi.ss.usermodel.Row r,
                                 Map<String, Integer> idx,
                                 FormulaEvaluator evaluator,
                                 DataFormatter formatter,
                                 String... names) {
        Integer col = findCol(idx, names);
        if (col == null) return null;
        Cell c = r.getCell(col);
        if (c == null) return null;

        String s = formatter.formatCellValue(c, evaluator);
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equals("-")) return null;

        // normalize decimal separator
        s = s.replace(",", ".");
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer findCol(Map<String, Integer> idx, String... names) {
        for (String n : names) {
            Integer i = idx.get(n);
            if (i != null) return i;
        }
        return null;
    }

    // -------------------- cleaning helpers --------------------
    private static Double[] swapIfInverted(Double min, Double max) {
        if (min != null && max != null && min > max) return new Double[]{max, min};
        return new Double[]{min, max};
    }

    private static LocalDate parseAnyDate(String raw) {
        raw = raw.trim();
        if (raw.isEmpty()) throw new IllegalArgumentException("Empty date");

        // 2019-12-01
        if (raw.length() >= 10 && raw.charAt(4) == '-' && raw.charAt(7) == '-') {
            return LocalDate.parse(raw.substring(0, 10), ISO_DATE);
        }

        // 11/4/2024
        if (raw.contains("/")) {
            return LocalDate.parse(raw, MDY_SLASH);
        }

        // 25-Oct-20
        return LocalDate.parse(raw, D_MON_YY);
    }

    private static String normalizeRegion(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        s = s.replace(" ", "_");
        s = s.replace("é", "e").replace("è", "e").replace("â", "a").replace("û", "u");
        return s;
    }

    // -------------------- csv output --------------------
    private static void writeCsv(Path out, List<RowOut> rows) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("date,produit,region,qte_tonne,px_min,px_max,px_moyen,spread,source_dataset\n");
            for (RowOut r : rows) w.write(r.toCsvLine());
        }
    }

    static class RowOut {
        LocalDate date;
        String produit;
        String region;
        Double qteTonne;
        Double pxMin;
        Double pxMax;
        Double pxMoyen;
        Double spread;
        String source;

        RowOut(LocalDate date, String produit, String region,
               Double qteTonne, Double pxMin, Double pxMax, Double pxMoyen,
               Double spread, String source) {
            this.date = date;
            this.produit = produit;
            this.region = region;
            this.qteTonne = qteTonne;
            this.pxMin = pxMin;
            this.pxMax = pxMax;
            this.pxMoyen = pxMoyen;
            this.spread = spread;
            this.source = source;
        }

        String toCsvLine() {
            return String.format(Locale.ROOT,
                    "%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    date,
                    produit,
                    region,
                    qteTonne == null ? "" : fmt(qteTonne),
                    pxMin == null ? "" : fmt(pxMin),
                    pxMax == null ? "" : fmt(pxMax),
                    pxMoyen == null ? "" : fmt(pxMoyen),
                    spread == null ? "" : fmt(spread),
                    source
            );
        }

        static String fmt(double v) {
            if (v == (long) v) return Long.toString((long) v);
            return Double.toString(v);
        }
    }
}