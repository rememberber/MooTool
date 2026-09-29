import { ManagedFavoritesDialog } from '@/features/favorites/ManagedFavoritesDialog'

export function CronFavoritesDialog(props: { open: boolean; currentValue: string; onClose: () => void; onApply: (value: string) => void }) {
  return <ManagedFavoritesDialog kind="cron" {...props} />
}
