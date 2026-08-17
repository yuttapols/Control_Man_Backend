# Project instructions

- Treat `env.dev` as the canonical local environment and deployment-variable reference for this workspace.
- Before running locally, testing database connectivity, or preparing a deployment, read all variables from `env.dev` and use them without asking the user to repeat values that are already present.
- Map the variables from `env.dev` to the deployment platform's secret/environment-variable settings when deploying.
- Never print, quote, commit, or copy secret values from `env.dev` into tracked files, logs, chat responses, or deployment manifests.
- Keep `env.dev` gitignored. Ask the user only when a required variable is absent, invalid, or the target deployment requires a materially different value.
