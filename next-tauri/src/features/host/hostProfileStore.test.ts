import { describe, expect, it } from 'vitest'
import { listStoredHostProfiles, saveStoredHostProfile, sqliteDate } from './hostProfileStore'

describe('host profile records', () => {
  it('assigns integer ids, updates the same name, and searches with SQL LIKE', () => {
    const created = saveStoredHostProfile([], { name: ' Development ', content: '127.0.0.1 api.local' }, new Date('2026-10-21T04:05:06'))
    expect(created).toMatchObject({ id: 1, name: 'Development', createTime: sqliteDate(new Date('2026-10-21T04:05:06')), modifiedTime: sqliteDate(new Date('2026-10-21T04:05:06')) })
    const updated = saveStoredHostProfile([created], { name: 'Development', content: '10.0.0.8 api.local build' }, new Date('2026-10-21T05:00:00'))
    expect(updated.id).toBe(1)
    expect(updated.createTime).toBe(created.createTime)
    const second = saveStoredHostProfile([updated], { name: 'Home', content: '127.0.0.1 localhost' }, new Date('2026-10-21T06:00:00'))
    expect(listStoredHostProfiles([updated, second], 'API', true).map((profile) => profile.name)).toEqual(['Development'])
    expect(listStoredHostProfiles([updated, second], 'home', false).map((profile) => profile.id)).toEqual([2])
    expect(listStoredHostProfiles([updated, second], 'a_i', true).map((profile) => profile.name)).toEqual(['Development'])
    expect(listStoredHostProfiles([updated, second], 'z_z', true)).toEqual([])
    expect(() => saveStoredHostProfile([], { name: '', content: '' })).toThrow('required')
  })
})
