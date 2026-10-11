# Remove o início automático do Personal Finance Tracker criado por instalar-inicio-automatico.ps1.
$ErrorActionPreference = 'Stop'

$taskName = 'Personal Finance Tracker'
if (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
    Write-Host "Tarefa '$taskName' removida."
} else {
    Write-Warning "Tarefa '$taskName' não existe; nada a remover."
}
exit 0
