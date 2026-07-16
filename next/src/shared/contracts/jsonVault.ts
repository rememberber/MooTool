export type JsonVaultNode = {
  name: string
  relativePath: string
  kind: 'directory' | 'file'
  children?: JsonVaultNode[]
  modifiedAt?: string
}

export type JsonVaultFile = {
  relativePath: string
  content: string
  modifiedAt: string
}

export type SaveJsonVaultFileInput = {
  relativePath: string
  content: string
}

export type JsonVaultListInput = {
  hideIgnored?: boolean
  sort?: 'name' | 'modified'
}

export type RenameJsonVaultEntryInput = {
  relativePath: string
  name: string
}

export type MoveJsonVaultEntryInput = {
  relativePath: string
  targetDirectory: string
}
