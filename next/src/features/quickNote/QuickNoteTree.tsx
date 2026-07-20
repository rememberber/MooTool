import { ChevronDown, ChevronRight, FileText, Folder, FolderOpen } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { QuickNoteNode } from '@/shared/contracts/quickNote'
import { useToolActivity } from '@/shared/components/ToolActivity'

type QuickNoteTreeProps = {
  nodes: QuickNoteNode[]
  selectedPath: string
  expanded: ReadonlySet<string>
  onSelect: (node: QuickNoteNode) => void
  onToggle: (relativePath: string) => void
  onMove: (node: Pick<QuickNoteNode, 'relativePath' | 'kind'>, targetDirectory: string) => void
  onRenameRequest: (node: QuickNoteNode) => void
  onMoveRequest: (node: QuickNoteNode) => void
  renameLabel: string
  moveLabel: string
}

const quickNotePathType = 'application/x-mootool-quick-note-path'
const quickNoteKindType = 'application/x-mootool-quick-note-kind'

export function QuickNoteTree({ nodes, selectedPath, expanded, onSelect, onToggle, onMove, onRenameRequest, onMoveRequest, renameLabel, moveLabel }: QuickNoteTreeProps) {
  const toolActive = useToolActivity()
  const menuRef = useRef<HTMLDivElement>(null)
  const [contextMenu, setContextMenu] = useState<{ node: QuickNoteNode; left: number; top: number } | null>(null)

  useEffect(() => {
    if (!contextMenu || !toolActive) return
    const focusFrame = window.requestAnimationFrame(() => menuRef.current?.querySelector('button')?.focus())
    const close = () => setContextMenu(null)
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', closeOnEscape)
    window.addEventListener('blur', close)
    return () => {
      window.cancelAnimationFrame(focusFrame)
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', closeOnEscape)
      window.removeEventListener('blur', close)
    }
  }, [contextMenu, toolActive])

  return (
    <>
      <div
        className="quick-note-tree"
        role="tree"
        tabIndex={0}
        onDragOver={(event) => event.preventDefault()}
        onDrop={(event) => {
          event.preventDefault()
          const draggedNode = readDraggedNode(event.dataTransfer)
          if (draggedNode && canMoveToDirectory(draggedNode.relativePath, '')) onMove(draggedNode, '')
        }}
      >
        {nodes.map((node) => (
          <QuickNoteTreeNode
            key={node.relativePath}
            node={node}
            depth={0}
            selectedPath={selectedPath}
            expanded={expanded}
            onSelect={onSelect}
            onToggle={onToggle}
            onMove={onMove}
            onOpenContextMenu={(menuNode, left, top) => {
              onSelect(menuNode)
              setContextMenu({ node: menuNode, left, top })
            }}
          />
        ))}
      </div>
      {contextMenu && toolActive && createPortal(
        <div
          ref={menuRef}
          className="quick-note-tree-menu"
          role="menu"
          style={{ left: contextMenu.left, top: contextMenu.top }}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <button type="button" role="menuitem" onClick={() => { onRenameRequest(contextMenu.node); setContextMenu(null) }}>{renameLabel}</button>
          <button type="button" role="menuitem" onClick={() => { onMoveRequest(contextMenu.node); setContextMenu(null) }}>{moveLabel}</button>
        </div>,
        document.body
      )}
    </>
  )
}

type NodeProps = Pick<QuickNoteTreeProps, 'selectedPath' | 'expanded' | 'onSelect' | 'onToggle' | 'onMove'> & {
  node: QuickNoteNode
  depth: number
  onOpenContextMenu: (node: QuickNoteNode, left: number, top: number) => void
}

function QuickNoteTreeNode({ node, depth, selectedPath, expanded, onSelect, onToggle, onMove, onOpenContextMenu }: NodeProps) {
  const open = node.kind === 'directory' && expanded.has(node.relativePath)
  const label = node.title || node.name.replace(/\.txt$/i, '')
  return (
    <div role="none">
      <button
        className={selectedPath === node.relativePath ? 'quick-note-tree__row quick-note-tree__row--active' : 'quick-note-tree__row'}
        type="button"
        role="treeitem"
        draggable
        data-path={node.relativePath}
        aria-expanded={node.kind === 'directory' ? open : undefined}
        aria-selected={selectedPath === node.relativePath}
        style={{ paddingLeft: 7 + depth * 15 }}
        onClick={() => {
          if (node.kind === 'directory') onToggle(node.relativePath)
          onSelect(node)
        }}
        onContextMenu={(event) => {
          event.preventDefault()
          onOpenContextMenu(node, Math.min(event.clientX, window.innerWidth - 164), Math.min(event.clientY, window.innerHeight - 78))
        }}
        onDragStart={(event) => {
          event.dataTransfer.effectAllowed = 'move'
          event.dataTransfer.setData(quickNotePathType, node.relativePath)
          event.dataTransfer.setData(quickNoteKindType, node.kind)
        }}
        onDragOver={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          event.dataTransfer.dropEffect = 'move'
        } : undefined}
        onDrop={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          const draggedNode = readDraggedNode(event.dataTransfer)
          if (draggedNode && canMoveToDirectory(draggedNode.relativePath, node.relativePath)) onMove(draggedNode, node.relativePath)
        } : undefined}
      >
        <span className="quick-note-tree__chevron">
          {node.kind === 'directory' ? open ? <ChevronDown size={12} /> : <ChevronRight size={12} /> : null}
        </span>
        {node.kind === 'directory'
          ? open ? <FolderOpen size={14} /> : <Folder size={14} />
          : <FileText size={14} style={{ color: noteColor(node.color) }} />}
        <span>{label}</span>
      </button>
      {node.kind === 'directory' && open && node.children?.map((child) => (
        <QuickNoteTreeNode
          key={child.relativePath}
          node={child}
          depth={depth + 1}
          selectedPath={selectedPath}
          expanded={expanded}
          onSelect={onSelect}
          onToggle={onToggle}
          onMove={onMove}
          onOpenContextMenu={onOpenContextMenu}
        />
      ))}
    </div>
  )
}

function readDraggedNode(dataTransfer: DataTransfer): Pick<QuickNoteNode, 'relativePath' | 'kind'> | null {
  const relativePath = dataTransfer.getData(quickNotePathType)
  const kind = dataTransfer.getData(quickNoteKindType)
  if (!relativePath || (kind !== 'file' && kind !== 'directory')) return null
  return { relativePath, kind }
}

export function canMoveToDirectory(relativePath: string, targetDirectory: string): boolean {
  const currentDirectory = relativePath.includes('/') ? relativePath.slice(0, relativePath.lastIndexOf('/')) : ''
  return currentDirectory !== targetDirectory
    && relativePath !== targetDirectory
    && !targetDirectory.startsWith(`${relativePath}/`)
}

function noteColor(color: string | undefined): string {
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
