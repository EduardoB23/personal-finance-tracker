package com.finance.tracker.backup;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Planilha de leitura (D23): uma aba por entidade, colunas com os nomes do backup JSON.
// Não é formato de importação: o H2 continua sendo a fonte da verdade.
final class BackupXlsx {

    private BackupXlsx() {
    }

    static byte[] write(BackupExport export) throws IOException {
        Map<String, List<?>> sheets = new LinkedHashMap<>();
        sheets.put("Perfis", export.profiles());
        sheets.put("Categorias", export.categories());
        sheets.put("Contas", export.accounts());
        sheets.put("Cartoes", export.cards());
        sheets.put("Faturas", export.invoices());
        sheets.put("Pagamentos", export.invoicePayments());
        sheets.put("Orcamentos", export.budgets());
        sheets.put("Metas", export.goals());
        sheets.put("Lancamentos", export.transactions());
        sheets.put("Excecoes", export.recurrenceExceptions());

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheets.forEach((name, rows) -> writeSheet(workbook.createSheet(name), rows));
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void writeSheet(Sheet sheet, List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        RecordComponent[] columns = rows.get(0).getClass().getRecordComponents();
        Row header = sheet.createRow(0);
        for (int column = 0; column < columns.length; column++) {
            header.createCell(column).setCellValue(columns[column].getName());
        }
        for (int index = 0; index < rows.size(); index++) {
            Row row = sheet.createRow(index + 1);
            for (int column = 0; column < columns.length; column++) {
                setValue(row.createCell(column), value(columns[column], rows.get(index)));
            }
        }
    }

    private static Object value(RecordComponent component, Object record) {
        try {
            return component.getAccessor().invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Coluna da planilha não pôde ser lida", e);
        }
    }

    private static void setValue(Cell cell, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean flag) {
            cell.setCellValue(flag);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
