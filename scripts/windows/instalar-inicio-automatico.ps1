# Registra o início automático do Personal Finance Tracker ao fazer logon no Windows (D4).
# Rodar uma vez no PC de casa, depois de gerar o jar e definir FINANCE_PIN_HASH (ver docs/acesso-pela-casa.md).
$ErrorActionPreference = 'Stop'

$taskName = 'Personal Finance Tracker'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$jar = Join-Path $projectRoot 'target\personal-finance-tracker.jar'

if (-not $env:JAVA_HOME) {
    Write-Error 'JAVA_HOME não definido. Instale o JDK 21 e defina JAVA_HOME.'
    exit 1
}
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path $java)) {
    Write-Error "JDK 21 não encontrado em $java."
    exit 1
}
if (-not (Test-Path $jar)) {
    Write-Error "Jar não encontrado em $jar. Rode mvn package antes."
    exit 1
}

# O diretório de trabalho é a raiz do projeto: o H2 usa ./data/finance, relativo a ele.
$action = New-ScheduledTaskAction -Execute $java -Argument "-jar `"$jar`"" -WorkingDirectory $projectRoot
$trigger = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
$settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero)

# -Force substitui a tarefa se ela já existir, então rodar de novo não duplica nada
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Force | Out-Null
Write-Host "Tarefa '$taskName' registrada: o servidor sobe ao fazer logon."
exit 0
