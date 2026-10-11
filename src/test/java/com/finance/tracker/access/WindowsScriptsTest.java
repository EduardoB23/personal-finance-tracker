package com.finance.tracker.access;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// Não há PowerShell neste ambiente: os scripts são provados pelo texto, não pela execução.
// Quem executa é o usuário no PC de casa (fora de escopo, ver .checks/acesso-pela-casa.md).
class WindowsScriptsTest {

    private static final Path INSTALLER = Path.of("scripts/windows/instalar-inicio-automatico.ps1");
    private static final Path REMOVER = Path.of("scripts/windows/remover-inicio-automatico.ps1");

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void installerRegistersLogonTaskWithWorkingDirectory() throws IOException {
        String script = read(INSTALLER);
        assertThat(script).contains("'Personal Finance Tracker'");
        assertThat(script).contains("-AtLogOn");
        assertThat(script).contains("-User");
        assertThat(script).contains("-WorkingDirectory");
        assertThat(script).contains("exit 0");
    }

    @Test
    void installerIsIdempotent() throws IOException {
        assertThat(read(INSTALLER)).contains("Register-ScheduledTask").contains("-Force");
    }

    @Test
    void removerUnregistersTask() throws IOException {
        String script = read(REMOVER);
        assertThat(script).contains("'Personal Finance Tracker'");
        assertThat(script).contains("Unregister-ScheduledTask");
        assertThat(script).contains("exit 0");
    }

    @Test
    void installerFailsWithoutJdkBeforeRegistering() throws IOException {
        String script = read(INSTALLER);
        int jdkCheck = script.indexOf("java.exe");
        int registration = script.indexOf("Register-ScheduledTask");
        assertThat(jdkCheck).isPositive();
        assertThat(registration).isGreaterThan(jdkCheck);
        assertThat(script).contains("JAVA_HOME");
        assertThat(script).contains("exit 1");
    }
}
