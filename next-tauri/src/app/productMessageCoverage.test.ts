import { describe, expect, it } from 'vitest'
import { calculatorMessages } from '../features/calculator/calculatorMessages'
import { colorMessages } from '../features/color/colorMessages'
import { configMessages } from '../features/config/configMessages'
import { cronMessages } from '../features/cron/cronMessages'
import { cryptoMessages } from '../features/crypto/cryptoMessages'
import { encodeMessages } from '../features/encode/encodeMessages'
import { hostMessages } from '../features/host/hostMessages'
import { httpMessages } from '../features/http/httpMessages'
import { imageMessages } from '../features/image/imageMessages'
import { jsonMessages } from '../features/json/jsonMessages'
import { messageBoardMessages } from '../features/messageBoard/messageBoardMessages'
import { networkMessages } from '../features/network/networkMessages'
import { pdfMessages } from '../features/pdf/pdfMessages'
import { protobufMessages } from '../features/protobuf/protobufMessages'
import { qrcodeMessages } from '../features/qrcode/qrcodeMessages'
import { quickNoteMessages } from '../features/quickNote/quickNoteMessages'
import { reformatMessages } from '../features/reformat/reformatMessages'
import { regexMessages } from '../features/regex/regexMessages'
import { runtimeMessages } from '../features/runtime/runtimeMessages'
import { systemMessages } from '../features/system/systemMessages'
import { textDiffMessages } from '../features/textDiff/textDiffMessages'
import { timestampMessages } from '../features/timestamp/timestampMessages'
import { translationMessages } from '../features/translation/translationMessages'
import { uaMessages } from '../features/ua/uaMessages'
import { variablesMessages } from '../features/variables/variablesMessages'
import { validateMessageCatalog, type LocalizedCatalog } from './localizedMessages'
import { productToolCatalog, type ToolId } from './toolCatalog'

const productMessages: Partial<Record<ToolId, LocalizedCatalog>> = {
  'quick-note': quickNoteMessages,
  'text-diff': textDiffMessages,
  reformat: reformatMessages,
  json: jsonMessages,
  config: configMessages,
  runtime: runtimeMessages,
  protobuf: protobufMessages,
  variables: variablesMessages,
  http: httpMessages,
  host: hostMessages,
  network: networkMessages,
  ua: uaMessages,
  encode: encodeMessages,
  crypto: cryptoMessages,
  regex: regexMessages,
  cron: cronMessages,
  qrcode: qrcodeMessages,
  timestamp: timestampMessages,
  'message-board': messageBoardMessages,
  translation: translationMessages,
  calculator: calculatorMessages,
  color: colorMessages,
  image: imageMessages,
  pdf: pdfMessages,
  system: systemMessages
}

describe('product message coverage', () => {
  it('covers every formal tool with an aligned three-locale catalog', () => {
    expect(productToolCatalog).toHaveLength(25)
    expect(Object.keys(productMessages).sort()).toEqual(productToolCatalog.map(({ id }) => id).sort())
    for (const tool of productToolCatalog) {
      expect(validateMessageCatalog(productMessages[tool.id]!)).toEqual([])
    }
  })
})
