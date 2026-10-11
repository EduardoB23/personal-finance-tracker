package com.finance.tracker.backup;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.profile.Profile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BackupXlsxTest extends IntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired private BackupController backupController;

    @Test
    void exportHasOneSheetPerEntityWithNumericAmounts() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "1500.50");
        cardExpense(maria, card("Nubank", "3000"), "120.00", LocalDate.of(2026, 1, 10), 1);
        accountExpense(maria, conjunta, "42.50", LocalDate.of(2026, 1, 5));
        entityManager.flush();

        byte[] content = backupController.exportBackup("xlsx").getBody().getByteArray();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertEquals("Perfis", workbook.getSheetName(0));
            assertNotNull(workbook.getSheet("Lancamentos"));
            assertNotNull(workbook.getSheet("Pagamentos"));

            Sheet accounts = workbook.getSheet("Contas");
            assertEquals("name", accounts.getRow(0).getCell(1).getStringCellValue());
            Row conjuntaRow = findRow(accounts, "Conjunta");
            // saldo já descontado da despesa de 42,50
            assertEquals(1458.00, conjuntaRow.getCell(3).getNumericCellValue(), 0.001);

            Sheet transactions = workbook.getSheet("Lancamentos");
            assertTrue(transactions.getPhysicalNumberOfRows() >= 3, "cabeçalho mais 2 lançamentos");
        }
    }

    @Test
    void xlsxIsRejectedForImport() {
        MockMultipartFile file = new MockMultipartFile("file", "finance-backup.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> backupController.importBackup(file));

        assertEquals("Planilha não é aceita para importação; use o JSON ou o CSV", error.getMessage());
    }

    @Test
    void unknownFormatMentionsXlsx() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> backupController.exportBackup("pdf"));
        assertTrue(error.getMessage().contains("XLSX"));
    }

    private static Row findRow(Sheet sheet, String value) {
        for (Row row : sheet) {
            if (row.getCell(1) != null && value.equals(row.getCell(1).getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("linha não encontrada: " + value);
    }
}
