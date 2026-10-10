# Modelo de issue no Linear

Use este modelo ao escrever uma tarefa (no chat do Claude ou direto no Linear). Quanto mais concreto, menos a automação `AI-READY` precisa parar para perguntar.

```markdown
## Contexto
O que acontece hoje e por que é um problema. Quem é afetado.

## Resultado esperado
O que a pessoa consegue fazer ou ver quando estiver pronto. Tela, endpoint ou comportamento.

## Critérios
- Quando <ação>, então <resultado com valor concreto: status HTTP, valor em R$, mensagem exata>
- Se <algo dá errado>, então <resultado>

## Fora de escopo
- O que explicitamente não entra

## Decisões já tomadas
- Referências a docs/decisions.md (Dxx) ou escolhas novas, com a alternativa rejeitada
```

Ao final, adicione a label `AI-READY` para a automação executar, ou deixe sem a label para implementar numa sessão interativa.
