import { lstat, readdir, unlink } from 'node:fs/promises'
import { join } from 'node:path'

const DAY = 24 * 60 * 60 * 1000
export const UPDATE_CLEANUP_INTERVAL = 6 * 60 * 60 * 1000

/** Only remove recognized updater files; never follow directories or symlinks. */
export async function cleanupUpdateFiles(
  directory: string,
  isProtected: (fileName: string) => boolean = () => false,
  now = Date.now()
): Promise<void> {
  let entries
  try {
    entries = await readdir(directory, { withFileTypes: true })
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return
    throw error
  }
  for (const entry of entries) {
    if (!entry.isFile()) continue
    const installer = /^MooTool-Next-Electron-.+-mac-(arm64|x64)\.dmg$/.test(entry.name)
    const partial = /^\.MooTool-Next-Electron-.+-mac-(arm64|x64)\.dmg\.\d+\.download$/.test(entry.name)
    if (!installer && !partial) continue
    try {
      const path = join(directory, entry.name)
      const stat = await lstat(path)
      if (!stat.isFile() || now - stat.mtimeMs < (partial ? DAY : 7 * DAY)) continue
      // Read live state after filesystem awaits, so a newly started download is protected.
      if (isProtected(entry.name)) continue
      await unlink(path)
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
        console.warn('Could not clean update file:', entry.name, error)
      }
    }
  }
}
