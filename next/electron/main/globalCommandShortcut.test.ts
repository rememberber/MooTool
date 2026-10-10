import { describe, expect, it, vi } from 'vitest'
import { GlobalCommandShortcut } from './globalCommandShortcut'

function fixture() {
  const registered = new Map<string, () => void>()
  const api = {
    register: vi.fn((key: string, callback: () => void) => { registered.set(key, callback); return true }),
    unregister: vi.fn((key: string) => { registered.delete(key) }),
    isRegistered: (key: string) => registered.has(key)
  }
  const open = vi.fn()
  return { controller: new GlobalCommandShortcut(api, open, 'darwin'), api, open, registered }
}

describe('global command shortcut lifecycle', () => {
  it('stays disabled by default and does not register invalid/conflicting shortcuts', () => {
    const { controller, api } = fixture()
    expect(controller.apply(false, 'CommandOrControl+Shift+Space').state).toBe('disabled')
    expect(controller.apply(true, 'Space').state).toBe('invalid')
    expect(controller.apply(true, 'Cmd+K').state).toBe('conflict')
    expect(api.register).not.toHaveBeenCalled()
  })

  it('invokes the palette, avoids re-registering unchanged settings and releases only owned shortcuts', () => {
    const { controller, api, open, registered } = fixture()
    registered.set('Control+Alt+F12', () => undefined)
    expect(controller.apply(true, 'CommandOrControl+Shift+Space')).toEqual({ state: 'registered', accelerator: 'Command+Shift+Space' })
    registered.get('Command+Shift+Space')!()
    expect(open).toHaveBeenCalledOnce()
    controller.apply(true, 'Shift+Cmd+Space')
    expect(api.register).toHaveBeenCalledOnce()
    controller.apply(true, 'Control+Alt+Space')
    expect(registered.has('Command+Shift+Space')).toBe(false)
    expect(registered.has('Control+Alt+Space')).toBe(true)
    controller.dispose()
    expect([...registered.keys()]).toEqual(['Control+Alt+F12'])
  })

  it('reports failures without keeping a stale shortcut, and allows retry', () => {
    const { controller, api, registered } = fixture()
    controller.apply(true, 'Cmd+Shift+Space')
    api.register.mockReturnValueOnce(false)
    expect(controller.apply(true, 'Ctrl+Alt+Space').state).toBe('unavailable')
    expect(registered.size).toBe(0)
    expect(controller.apply(true, 'Ctrl+Alt+Space').state).toBe('registered')
    controller.apply(false, 'Ctrl+Alt+Space')
    expect(registered.size).toBe(0)
    api.register.mockImplementationOnce(() => { throw new Error('unsupported') })
    expect(controller.apply(true, 'Ctrl+Alt+Space').state).toBe('unavailable')
  })
})
