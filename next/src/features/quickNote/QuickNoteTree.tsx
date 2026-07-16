import { ChevronDown, ChevronRight, FileText, Folder, FolderOpen } from 'lucide-react'
import type { QuickNoteNode } from '@/shared/contracts/quickNote'

type QuickNoteTreeProps = {
  nodes: QuickNoteNode[]
  selectedPath: string
  expanded: ReadonlySet<string>
  onSelect: (node: QuickNoteNode) => void
  onToggle: (relativePath: string) => void
  onMove: (node: Pick<QuickNoteNode, 'relativePath' | 'kind'>, targetDirectory: string) => void
}

const quickNotePathType = 'application/x-mootool-quick-note-path'
const quickNoteKindType = 'application/x-mootool-quick-note-kind'

export function QuickNoteTree({ nodes, selectedPath, expanded, onSelect, onToggle, onMove }: QuickNoteTreeProps) {
  return (
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
        />
      ))}
    </div>
  )
}

type NodeProps = Omit<QuickNoteTreeProps, 'nodes'> & {
  node: QuickNoteNode
  depth: number
}

function QuickNoteTreeNode({ node, depth, selectedPath, expanded, onSelect, onToggle, onMove }: NodeProps) {
  const open = node.kind === 'directory' && expanded.has(node.relativePath)
  const label = node.title || node.name.replace(/\.txt$/i, '')
  return (
    <div role="none">
      <button
        className={selectedPath === node.relativePath ? 'quick-note-tree__row quick-note-tree__row--active' : 'quick-note-tree__row'}
        type="button"
        role="treeitem"
        draggable
        aria-expanded={node.kind === 'directory' ? open : undefined}
        aria-selected={selectedPath === node.relativePath}
        style={{ paddingLeft: 7 + depth * 15 }}
        onClick={() => {
          if (node.kind === 'directory') onToggle(node.relativePath)
          onSelect(node)
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
