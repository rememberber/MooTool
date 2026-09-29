import { parentPort, workerData } from 'node:worker_threads'
import { vectorizePng } from './imageVectorizationService'
try { parentPort?.postMessage({ svg: vectorizePng(workerData.png, workerData.options) }) }
catch (error) { parentPort?.postMessage({ error: error instanceof Error ? error.message : String(error) }) }
