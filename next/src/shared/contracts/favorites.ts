export const favoriteKinds = ['regex', 'cron', 'color'] as const

export type FavoriteKind = (typeof favoriteKinds)[number]

export type FavoriteRecord = {
  id: number
  kind: FavoriteKind
  name: string
  value: string
  description: string
  createTime: string
}

export type SaveFavoriteInput = {
  kind: FavoriteKind
  name: string
  value: string
  description?: string
}
