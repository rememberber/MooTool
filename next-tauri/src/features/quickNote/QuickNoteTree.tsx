import { ChevronDown, ChevronRight, FileText, Folder, FolderOpen } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { QuickNote, QuickNoteFolder } from '../../platform/contracts/localData'

export type NoteTreeAction = 'rename' | 'move' | 'duplicate' | 'export' | 'info' | 'delete' | 'reveal'

export interface NoteTreeNode {
  name: string
  path: string
  kind: 'directory' | 'file'
  note?: QuickNote
  children: NoteTreeNode[]
}

export function buildNoteTree(notes: QuickNote[], folders: QuickNoteFolder[], query: string, tag: string): NoteTreeNode[] {
  const needle = query.trim().toLowerCase()
  const matches = (note: QuickNote) => (!tag || note.tags.includes(tag))
    && (!needle || `${note.title}\n${note.content}\n${note.tags.join(' ')}`.toLowerCase().includes(needle))
  const visible = notes.filter(matches)
  const directoryPaths = new Set<string>(folders.map((folder) => folder.path))
  for (const note of visible) {
    if (!note.folderPath) continue
    const parts = note.folderPath.split('/')
    for (let index = 1; index <= parts.length; index += 1) directoryPaths.add(parts.slice(0, index).join('/'))
  }
  if (needle || tag) {
    for (const path of [...directoryPaths]) {
      const hasNote = visible.some((note) => note.folderPath === path || note.folderPath.startsWith(`${path}/`))
      if (!hasNote) directoryPaths.delete(path)
    }
  }
  const roots: NoteTreeNode[] = []
  const byPath = new Map<string, NoteTreeNode>()
  for (const path of [...directoryPaths].sort((left, right) => left.split('/').length - right.split('/').length || left.localeCompare(right))) {
    const node: NoteTreeNode = { name: path.split('/').at(-1) ?? path, path, kind: 'directory', children: [] }
    byPath.set(path, node)
    const parent = path.includes('/') ? path.slice(0, path.lastIndexOf('/')) : ''
    const parentNode = parent ? byPath.get(parent) : undefined
    if (parentNode) parentNode.children.push(node)
    else roots.push(node)
  }
  for (const note of visible) {
    const file: NoteTreeNode = {
      name: noteLabel(note),
      path: note.relativePath || (note.folderPath ? `${note.folderPath}/${note.id}` : note.id),
      kind: 'file',
      note,
      children: []
    }
    const parent = note.folderPath ? byPath.get(note.folderPath) : undefined
    if (parent) parent.children.push(file)
    else roots.push(file)
  }
  return roots
}

function noteLabel(note: QuickNote): string {
  if (note.title.trim()) return note.title
  const leaf = (note.relativePath || note.id).split('/').pop() ?? note.id
  return leaf.replace(/\.txt$/i, '')
}

const quickNotePathType = 'application/x-mootool-quick-note-path'
const quickNoteKindType = 'application/x-mootool-quick-note-kind'

export function canMoveToDirectory(relativePath: string, targetDirectory: string): boolean {
  const currentDirectory = relativePath.includes('/') ? relativePath.slice(0, relativePath.lastIndexOf('/')) : ''
  return currentDirectory !== targetDirectory
    && relativePath !== targetDirectory
    && !targetDirectory.startsWith(`${relativePath}/`)
}

export function QuickNoteTree({ nodes, activeId, selectedFolder, onOpenNote, onSelectFolder, onMove, onAction, onGit, labels, gitLabel }: {
  nodes: NoteTreeNode[]
  activeId: string
  selectedFolder: string
  onOpenNote(note: QuickNote): void
  onSelectFolder(path: string): void
  onMove(node: NoteTreeNode, targetDirectory: string): void
  onAction(node: NoteTreeNode, action: NoteTreeAction): void
  onGit(): void
  labels: Record<NoteTreeAction, string>
  gitLabel: string
}) {
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(() => new Set())
  const [dropTarget, setDropTarget] = useState<string | null>(null)
  const [contextMenu, setContextMenu] = useState<{ node: NoteTreeNode; left: number; top: number } | null>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const knownDirectories = useRef(new Set<string>())
  const draggedPath = useRef('')
  useEffect(() => {
    const paths = directoryPaths(nodes)
    const fresh = paths.filter((path) => !knownDirectories.current.has(path))
    knownDirectories.current = new Set(paths)
    if (!fresh.length) return
    setExpanded((current) => new Set([...current, ...fresh]))
  }, [nodes])
  useEffect(() => {
    if (!contextMenu) return
    const close = () => setContextMenu(null)
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [contextMenu])
  function moveDropped(targetDirectory: string): void {
    const node = findNode(nodes, draggedPath.current)
    setDropTarget(null)
    draggedPath.current = ''
    if (node && canMoveToDirectory(node.path, targetDirectory)) onMove(node, targetDirectory)
  }
  return (
    <div
      className={dropTarget === '' ? 'quick-note-tree quick-note-tree--drop' : 'quick-note-tree'}
      role="tree"
      onDragOver={(event) => {
        if (!draggedPath.current || !canMoveToDirectory(draggedPath.current, '')) return
        event.preventDefault()
        setDropTarget('')
      }}
      onDrop={(event) => {
        event.preventDefault()
        moveDropped('')
      }}
    >
      {nodes.map((node) => (
        <TreeNode
          key={node.path}
          node={node}
          depth={0}
          activeId={activeId}
          selectedFolder={selectedFolder}
          expanded={expanded}
          dropTarget={dropTarget}
          onToggle={(path) => setExpanded((current) => {
            const next = new Set(current)
            if (next.has(path)) next.delete(path)
            else next.add(path)
            return next
          })}
          onOpenNote={onOpenNote}
          onSelectFolder={onSelectFolder}
          onDragPath={(path) => { draggedPath.current = path }}
          canDrop={(path) => Boolean(draggedPath.current) && canMoveToDirectory(draggedPath.current, path)}
          onDragEnd={() => { draggedPath.current = ''; setDropTarget(null) }}
          onDragHover={setDropTarget}
          onDropNode={moveDropped}
          onContextMenu={(node, left, top) => setContextMenu({ node, left, top })}
        />
      ))}
      {contextMenu && createPortal(
        <div
          ref={menuRef}
          className="quick-note-tree-menu"
          role="menu"
          style={{ left: contextMenu.left, top: contextMenu.top }}
          onPointerDown={(event) => event.stopPropagation()}
        >
          {(['rename', 'move'] as const).map((action) => (
            <button key={action} type="button" role="menuitem" onClick={() => { onAction(contextMenu.node, action); setContextMenu(null) }}>{labels[action]}</button>
          ))}
          {contextMenu.node.kind === 'file' && (
            <>
              {(['duplicate', 'export', 'info'] as const).map((action) => (
                <button key={action} type="button" role="menuitem" onClick={() => { onAction(contextMenu.node, action); setContextMenu(null) }}>{labels[action]}</button>
              ))}
              <div className="quick-note-tree-menu__separator" role="separator" />
            </>
          )}
          {(['delete', 'reveal'] as const).map((action) => (
            <button key={action} type="button" role="menuitem" onClick={() => { onAction(contextMenu.node, action); setContextMenu(null) }}>{labels[action]}</button>
          ))}
          <div className="quick-note-tree-menu__separator" role="separator" />
          <button type="button" role="menuitem" onClick={() => { onGit(); setContextMenu(null) }}>{gitLabel}</button>
        </div>,
        document.body
      )}
    </div>
  )
}

function directoryPaths(nodes: NoteTreeNode[]): string[] {
  return nodes.flatMap((node) => node.kind === 'directory' ? [node.path, ...directoryPaths(node.children)] : [])
}

function findNode(nodes: NoteTreeNode[], path: string): NoteTreeNode | undefined {
  for (const node of nodes) {
    if (node.path === path) return node
    const child = findNode(node.children, path)
    if (child) return child
  }
  return undefined
}

function TreeNode({ node, depth, activeId, selectedFolder, expanded, dropTarget, onToggle, onOpenNote, onSelectFolder, onDragPath, canDrop, onDragEnd, onDragHover, onDropNode, onContextMenu }: {
  node: NoteTreeNode
  depth: number
  activeId: string
  selectedFolder: string
  expanded: ReadonlySet<string>
  dropTarget: string | null
  onToggle(path: string): void
  onOpenNote(note: QuickNote): void
  onSelectFolder(path: string): void
  onDragPath(path: string): void
  canDrop(path: string): boolean
  onDragEnd(): void
  onDragHover(path: string | null): void
  onDropNode(targetDirectory: string): void
  onContextMenu(node: NoteTreeNode, left: number, top: number): void
}) {
  const open = node.kind === 'directory' && expanded.has(node.path)
  const selected = node.kind === 'file' ? node.note?.id === activeId : selectedFolder === node.path
  const dropping = node.kind === 'directory' && dropTarget === node.path
  return (
    <div role="treeitem" aria-expanded={node.kind === 'directory' ? open : undefined} aria-selected={selected}>
      <button
        className={`quick-note-tree__row${selected ? ' quick-note-tree__row--active' : ''}${dropping ? ' quick-note-tree__row--drop' : ''}`}
        style={{ paddingInlineStart: `${8 + depth * 16}px`, color: node.kind === 'file' ? noteColor(node.note?.color) : undefined }}
        type="button"
        draggable
        onDragStart={(event) => {
          event.dataTransfer.effectAllowed = 'move'
          event.dataTransfer.setData(quickNotePathType, node.path)
          event.dataTransfer.setData(quickNoteKindType, node.kind)
          onDragPath(node.path)
        }}
        onDragEnd={onDragEnd}
        onDragOver={node.kind === 'directory' ? (event) => {
          if (!canDrop(node.path)) return
          event.preventDefault()
          event.stopPropagation()
          event.dataTransfer.dropEffect = 'move'
          onDragHover(node.path)
        } : undefined}
        onDrop={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          onDropNode(node.path)
        } : undefined}
        onContextMenu={(event) => {
          event.preventDefault()
          event.stopPropagation()
          const menuHeight = node.kind === 'file' ? 248 : 132
          onContextMenu(
            node,
            Math.max(8, Math.min(event.clientX, window.innerWidth - 188)),
            Math.max(8, Math.min(event.clientY, window.innerHeight - menuHeight))
          )
        }}
        onClick={() => {
          if (node.kind === 'directory') {
            onSelectFolder(node.path)
            onToggle(node.path)
          } else if (node.note) onOpenNote(node.note)
        }}
      >
        {node.kind === 'directory'
          ? open ? <ChevronDown size={14} /> : <ChevronRight size={14} />
          : <span className="quick-note-tree__spacer" />}
        {node.kind === 'directory' ? open ? <FolderOpen size={15} /> : <Folder size={15} /> : <FileText size={15} />}
        <span>{node.name}</span>
      </button>
      {open && node.children.map((child) => (
        <TreeNode
          key={child.path}
          node={child}
          depth={depth + 1}
          activeId={activeId}
          selectedFolder={selectedFolder}
          expanded={expanded}
          dropTarget={dropTarget}
          onToggle={onToggle}
          onOpenNote={onOpenNote}
          onSelectFolder={onSelectFolder}
          onDragPath={onDragPath}
          canDrop={canDrop}
          onDragEnd={onDragEnd}
          onDragHover={onDragHover}
          onDropNode={onDropNode}
          onContextMenu={onContextMenu}
        />
      ))}
    </div>
  )
}

function noteColor(color: QuickNote['color'] | undefined): string {
  const colors: Record<string, string> = {
    coral: '#d97868',
    yellow: '#c99535',
    green: '#4e9275',
    blue: '#4f83cc',
    purple: '#8a72b5',
    red: '#c96761'
  }
  return colors[color ?? ''] ?? 'currentColor'
}
