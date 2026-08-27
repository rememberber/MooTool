export const favoriteKinds = ['regex', 'cron', 'color'] as const

export type FavoriteKind = (typeof favoriteKinds)[number]

export type FavoriteRecord = {
  id: number
  kind: FavoriteKind
  folderId: number
  name: string
  value: string
  description: string
  createTime: string
}

export type FavoriteFolderRecord = {
  id: number
  kind: FavoriteKind
  title: string
  createTime: string
}

export type SaveFavoriteInput = {
  kind: FavoriteKind
  folderId?: number
  name: string
  value: string
  description?: string
}

export type SaveFavoriteFolderInput = {
  kind: FavoriteKind
  title: string
}

export type RenameFavoriteFolderInput = {
  id: number
  title: string
}
