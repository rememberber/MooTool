export type FuncHistoryRecord = {
  id: number
  funcType: string
  summary: string
  inputText: string
  outputText: string
  extraData: string | null
  createTime: string
}

export type SaveFuncHistoryInput = {
  funcType: string
  summary?: string
  inputText: string
  outputText: string
  extraData?: string | null
}

export type HistoryQuery = {
  funcType: string
  keyword?: string
}
