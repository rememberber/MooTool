// @vitest-environment node
import { mkdtemp, mkdir, readdir, rm, symlink, utimes, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { expect, it } from 'vitest'
import { cleanupUpdateFiles } from './updateCleanup'

it('expires installers and abandoned downloads while preserving active files and unrelated entries', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-cleanup-'))
  const now = Date.now()
  const day = 86400000
  const name = (version: string) => `MooTool-Next-Electron-${version}-mac-arm64.dmg`
  const old = name('1.0.0')
  const ready = name('1.1.0')
  const recent = name('1.2.0')
  const partial = `.${old}.123.download`
  const freshPartial = `.${recent}.123.download`
  try {
    for (const [file, age] of [[old, 8], [ready, 8], [recent, 6], [partial, 2], [freshPartial, 0], ['personal.dmg', 9]] as const) {
      const path = join(directory, file)
      await writeFile(path, 'data')
      await utimes(path, new Date(now - age * day), new Date(now - age * day))
    }
    await mkdir(join(directory, name('directory')))
    await symlink(join(directory, 'personal.dmg'), join(directory, name('link')))
    await cleanupUpdateFiles(directory, () => true, now)
    expect(await readdir(directory)).toContain(old)
    await cleanupUpdateFiles(directory, (file) => file === ready, now)
    expect((await readdir(directory)).sort()).toEqual([ready, recent, freshPartial, 'personal.dmg', name('directory'), name('link')].sort())
    await cleanupUpdateFiles(join(directory, 'missing'))
  } finally {
    await rm(directory, { recursive: true, force: true })
  }
})
