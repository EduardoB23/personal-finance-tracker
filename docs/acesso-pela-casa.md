# Acesso pela casa

O servidor roda no PC de casa, em um único `.jar` que serve a API e o app na porta 8080. Os celulares
acessam pelo navegador, na mesma rede Wi‑Fi. Cada aparelho informa o PIN familiar uma vez e fica liberado por 90 dias.

## 1. Gerar o `.jar` com o app

Na raiz do projeto, com JDK 21 e Node instalados:

```powershell
cd frontend
npm ci
npm run build
cd ..
mvn package
```

O `.jar` sai em `target\personal-finance-tracker.jar`, com o app dentro de `static/`.

## 2. Definir o PIN familiar

Gere o hash do PIN escolhido:

```powershell
java -jar target\personal-finance-tracker.jar --gerar-hash-pin=SEU_PIN
```

O comando imprime um hash que começa com `$2a$`. Grave-o na variável de ambiente do seu usuário,
com aspas simples (o hash tem `$`):

```powershell
setx FINANCE_PIN_HASH '$2a$10$...'
```

Abra um terminal novo para que a variável esteja disponível. Sem `FINANCE_PIN_HASH`, o servidor não sobe e
o log mostra a orientação para gerar o hash.

Obs.: o PIN digitado na linha de comando fica no histórico do terminal. Apague a linha depois, se preferir.

## 3. Iniciar automaticamente com o Windows

Com o `.jar` gerado e a variável definida:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\instalar-inicio-automatico.ps1
```

A tarefa `Personal Finance Tracker` sobe o servidor ao fazer logon. Rodar o script de novo substitui a tarefa.

Para remover:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\windows\remover-inicio-automatico.ps1
```

## 4. Acessar pelo celular

No celular, abra `http://<IP do PC>:8080` na mesma rede Wi‑Fi e informe o PIN. O IP do PC aparece em `ipconfig`.

## Limite de tentativas

Cinco PINs errados seguidos vindos do mesmo aparelho bloqueiam o acesso desse aparelho por 15 minutos. O bloqueio
fica na memória do servidor: reiniciar o servidor o zera.
