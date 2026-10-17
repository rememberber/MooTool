import { parentPort, workerData } from 'node:worker_threads'
import { join } from 'node:path'
import { inspectPdf, mergePdfs, splitPdfs, type PdfProcessingOptions } from './pdfService'
import type { PdfWorkerTask } from './pdfTaskService'

const { task, directory } = workerData as { task: PdfWorkerTask; directory: string }
const staged: { path: string; stagedPath: string }[] = []
const options: PdfProcessingOptions = {
  outputPath: path => {
    const stagedPath = join(directory, `${staged.length}.pdf`)
    staged.push({ path, stagedPath })
    return stagedPath
  },
  onProgress: progress => parentPort?.postMessage({ type: 'progress', progress })
}
async function run() {
  if (task.kind === 'inspect') {
    const files = []
    for (const path of task.paths) {
      files.push(await inspectPdf(path))
      options.onProgress?.({ phase: 'reading', completed: files.length, total: task.paths.length, name: files.at(-1)!.name })
    }
    return files
  }
  return task.kind === 'merge' ? mergePdfs(task.sources, task.outputPath, options) : splitPdfs(task.tasks, options)
}
void run().then(result => parentPort?.postMessage({ type: 'done', result, staged }))
  .catch(error => parentPort?.postMessage({ type: 'error', error: error instanceof Error ? error.message : String(error) }))
