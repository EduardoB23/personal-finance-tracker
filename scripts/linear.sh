#!/usr/bin/env bash
# Utilitário mínimo para a API GraphQL do Linear, usado pela automação AI-READY.
# Requer LINEAR_API_KEY, curl e jq. Não usa IA: zero tokens.
set -euo pipefail

API="https://api.linear.app/graphql"
TEAM_KEY="${LINEAR_TEAM_KEY:-COD}"

gql() {
  local query="$1" variables="${2:-}"
  [ -n "$variables" ] || variables='{}'
  local response
  response=$(jq -n --arg q "$query" --argjson v "$variables" '{query: $q, variables: $v}' |
    curl -sS -X POST "$API" \
      -H "Content-Type: application/json" \
      -H "Authorization: ${LINEAR_API_KEY:?LINEAR_API_KEY não definido}" \
      --data-binary @-)
  if [ "$(jq 'has("errors")' <<<"$response")" = "true" ]; then
    echo "Erro da API do Linear: $(jq -c '.errors' <<<"$response")" >&2
    exit 1
  fi
  echo "$response"
}

label_id() {
  gql 'query($name: String!, $team: String!) {
         issueLabels(filter: { name: { eq: $name }, team: { key: { eq: $team } } }) { nodes { id } }
       }' "$(jq -n --arg name "$1" --arg team "$TEAM_KEY" '{name: $name, team: $team}')" |
    jq -r '.data.issueLabels.nodes[0].id // empty'
}

case "${1:-}" in
  # Próxima issue com AI-READY e não concluída: imprime JSON {id, identifier, title, branchName, url} ou nada
  pick)
    gql 'query($team: String!) {
           issues(first: 1, orderBy: createdAt, filter: {
             team: { key: { eq: $team } },
             labels: { name: { eq: "AI-READY" } },
             state: { type: { nin: ["completed", "canceled"] } }
           }) { nodes { id identifier title branchName url } }
         }' "$(jq -n --arg team "$TEAM_KEY" '{team: $team}')" |
      jq -c '.data.issues.nodes[0] // empty'
    ;;

  # Texto da issue em Markdown, para o prompt do agente
  get)
    gql 'query($id: String!) {
           issue(id: $id) { identifier title description url comments { nodes { body createdAt user { name } } } }
         }' "$(jq -n --arg id "$2" '{id: $id}')" |
      jq -r '.data.issue |
        "# \(.identifier) - \(.title)\n\n\(.url)\n\n\(.description // "")\n\n## Comentários\n\n" +
        ([.comments.nodes | sort_by(.createdAt)[] | "**\(.user.name // "?")** (\(.createdAt)):\n\(.body)\n"] | join("\n"))'
    ;;

  # Troca uma label por outra, mantendo as demais: swap-label <issueId> <de> <para>
  # Remove também qualquer outra label AI-*, porque o grupo AI do Linear aceita só uma por issue
  swap-label)
    issue_id="$2"; from="$3"; to="$4"
    current=$(gql 'query($id: String!) { issue(id: $id) { labels { nodes { id name } } } }' \
      "$(jq -n --arg id "$issue_id" '{id: $id}')")
    to_id=$(label_id "$to")
    ids=$(jq -c --arg from "$from" --arg to "$to_id" \
      '[.data.issue.labels.nodes[] | select(.name != $from and (.name | startswith("AI-") | not)) | .id] + [$to] | unique' <<<"$current")
    gql 'mutation($id: String!, $labels: [String!]) { issueUpdate(id: $id, input: { labelIds: $labels }) { success } }' \
      "$(jq -n --arg id "$issue_id" --argjson labels "$ids" '{id: $id, labels: $labels}')" >/dev/null
    ;;

  # Remove uma label: remove-label <issueId> <nome>
  remove-label)
    current=$(gql 'query($id: String!) { issue(id: $id) { labels { nodes { id name } } } }' \
      "$(jq -n --arg id "$2" '{id: $id}')")
    ids=$(jq -c --arg name "$3" '[.data.issue.labels.nodes[] | select(.name != $name) | .id]' <<<"$current")
    gql 'mutation($id: String!, $labels: [String!]) { issueUpdate(id: $id, input: { labelIds: $labels }) { success } }' \
      "$(jq -n --arg id "$2" --argjson labels "$ids" '{id: $id, labels: $labels}')" >/dev/null
    ;;

  # Muda o status pelo nome: state <issueId> "In Progress"
  state)
    state_id=$(gql 'query($name: String!, $team: String!) {
                      workflowStates(filter: { name: { eq: $name }, team: { key: { eq: $team } } }) { nodes { id } }
                    }' "$(jq -n --arg name "$3" --arg team "$TEAM_KEY" '{name: $name, team: $team}')" |
      jq -r '.data.workflowStates.nodes[0].id')
    gql 'mutation($id: String!, $state: String!) { issueUpdate(id: $id, input: { stateId: $state }) { success } }' \
      "$(jq -n --arg id "$2" --arg state "$state_id" '{id: $id, state: $state}')" >/dev/null
    ;;

  # Comenta na issue com o conteúdo do stdin: comment <issueId> < arquivo
  comment)
    body=$(cat)
    gql 'mutation($id: String!, $body: String!) { commentCreate(input: { issueId: $id, body: $body }) { success } }' \
      "$(jq -n --arg id "$2" --arg body "$body" '{id: $id, body: $body}')" >/dev/null
    ;;

  *)
    echo "uso: $0 pick | get <id> | swap-label <id> <de> <para> | remove-label <id> <nome> | state <id> <status> | comment <id> < arquivo" >&2
    exit 2
    ;;
esac
