export type VaultKind = 'json' | 'quickNote'

export type VaultWorkspace<File> = {
  rootDirectory: string
  file: File
}

export type RememberVaultFileInput = {
  kind: VaultKind
  rootDirectory: string
  relativePath: string
}
