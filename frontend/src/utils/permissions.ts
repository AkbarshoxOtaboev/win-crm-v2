const ACTIONS = ['VIEW', 'CREATE', 'EDIT', 'DELETE'] as const

/** Split USER_VIEW / CLIENT_GROUP_VIEW into resource + action. */
export function splitPermissionName(name: string): { resource: string; action: string } | null {
  for (const action of ACTIONS) {
    const suffix = `_${action}`
    if (name.endsWith(suffix)) {
      return { resource: name.slice(0, -suffix.length), action }
    }
  }
  return null
}

type TranslateFn = (key: string) => string

/** SUPER_ADMIN -> "Bosh administrator"; unknown (custom) roles keep their own name. */
export function roleLabel(name: string, t: TranslateFn): string {
  const key = `users.roleNames.${name}`
  const label = t(key)
  return label === key ? name : label
}

export function permissionLabel(name: string, t: TranslateFn): string {
  const parts = splitPermissionName(name)
  if (!parts) return name
  const resourceKey = `permissions.resources.${parts.resource}`
  const actionKey = `permissions.actions.${parts.action}`
  const resource = t(resourceKey)
  const action = t(actionKey)
  const resourceLabel = resource === resourceKey ? parts.resource : resource
  const actionLabel = action === actionKey ? parts.action : action
  return `${resourceLabel} — ${actionLabel}`
}
