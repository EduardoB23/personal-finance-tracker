package com.finance.tracker.backup;

// Backup bem formado que referencia algo inexistente; a mensagem chega ao usuário como está
public class InvalidBackupException extends IllegalArgumentException {

    public InvalidBackupException(String detail) {
        super("Backup inválido: " + detail);
    }
}
