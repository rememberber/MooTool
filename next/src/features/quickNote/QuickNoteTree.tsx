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

export function QuickNoteTree({ nodes, selectedPath, expanded, onSelect, onToggle, onMove }: QuickNoteTreeProps) {
  return (
    <div
      className="quick-note-tree"
      role="tree"
      onDragOver={(event) => event.preventDefault()}
      onDrop={(event) => {
        if (event.target !== event.currentTarget) return
        const relativePath = event.dataTransfer.getData('application/x-mootool-quick-note-path')
        const kind = event.dataTransfer.getData('application/x-mootool-quick-note-kind') as QuickNoteNode['kind']
        if (relativePath) onMove({ relativePath, kind }, '')
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
          event.dataTransfer.setData('application/x-mootool-quick-note-path', node.relativePath)
          event.dataTransfer.setData('application/x-mootool-quick-note-kind', node.kind)
        }}
        onDragOver={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          event.dataTransfer.dropEffect = 'move'
        } : undefined}
        onDrop={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          const relativePath = event.dataTransfer.getData('application/x-mootool-quick-note-path')
          const kind = event.dataTransfer.getData('application/x-mootool-quick-note-kind') as QuickNoteNode['kind']
          if (relativePath && relativePath !== node.relativePath) onMove({ relativePath, kind }, node.relativePath)
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
