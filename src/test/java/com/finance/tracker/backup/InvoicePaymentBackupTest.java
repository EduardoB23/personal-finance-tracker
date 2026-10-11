package com.finance.tracker.backup;

import com.finance.tracker.IntegrationTest;
import com.finance.tracker.account.BankAccount;
import com.finance.tracker.account.CreditCard;
import com.finance.tracker.account.InvoicePaymentRepository;
import com.finance.tracker.profile.Profile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePaymentBackupTest extends IntegrationTest {

    @Autowired private BackupController backupController;
    @Autowired private InvoicePaymentRepository paymentRepository;

    @Test
    void jsonRoundTripKeepsPayments() throws Exception {
        roundTrip("json", "finance-backup.json", "application/json");
    }

    @Test
    void csvRoundTripKeepsPayments() throws Exception {
        roundTrip("csv", "finance-backup.csv", "text/csv");
    }

    @Test
    void backupWithoutPaymentsSectionStillImports() throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        entityManager.flush();
        ResponseEntity<ByteArrayResource> response = backupController.exportBackup("json");
        ObjectNode backup = (ObjectNode) objectMapper.readTree(response.getBody().getByteArray());
        backup.remove("invoicePayments");

        backupController.importBackup(new MockMultipartFile("file", "finance-backup.json", "application/json",
                objectMapper.writeValueAsBytes(backup)));

        assertEquals(0, paymentRepository.count());
        assertTrue(invoiceRepository.count() > 0);
    }

    private void roundTrip(String format, String filename, String contentType) throws Exception {
        Profile maria = profile("Maria");
        BankAccount conjunta = account("Conjunta", "5000");
        CreditCard nubank = card("Nubank", "3000");
        cardExpense(maria, nubank, "1000.00", LocalDate.of(2026, 1, 10), 1);
        payInvoice(nubank, "2026-02", conjunta, maria, "300.00", LocalDate.of(2026, 2, 5));
        entityManager.flush();
        entityManager.clear();

        String before = paymentSnapshot();
        String exported = new String(backupController.exportBackup(format).getBody().getByteArray(), StandardCharsets.UTF_8);
        backupController.importBackup(new MockMultipartFile("file", filename, contentType,
                exported.getBytes(StandardCharsets.UTF_8)));
        entityManager.flush();
        entityManager.clear();

        assertEquals(before, paymentSnapshot());
        assertEquals(0, new BigDecimal("4700").compareTo(balanceOfNamed("Conjunta")));
    }

    // Valor, conta, perfil, data e restante de cada pagamento, para comparar antes e depois
    private String paymentSnapshot() {
        return paymentRepository.findAll().stream()
                .map(p -> p.getAmount() + "|" + p.getAccount().getName() + "|" + p.getProfile().getName() + "|"
                        + p.getPaidOn() + "|" + p.getInvoice().getAmount())
                .sorted()
                .toList()
                .toString();
    }

    private BigDecimal balanceOfNamed(String name) {
        return bankAccountRepository.findAll().stream()
                .filter(account -> account.getName().equals(name))
                .findFirst().orElseThrow().getBalance();
    }
}
