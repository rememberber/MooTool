// Evaluation only; creates synthetic screenshot fixtures through Electron's canvas.
const { _electron } = require('@playwright/test')
const fs = require('node:fs/promises')
const path = require('node:path')
const os = require('node:os')

const samples = [
  { id: 'light-20', font: 20, text: ['中文识别评估：先预览，再复制，全部在本机完成。', '批量重命名、图片压缩、格式转换和剪贴板工具。', '订单编号 001234，金额 ¥128.50，日期 2026-10-12。'] },
  { id: 'dark-16', font: 16, dark: true, text: ['设置已保存，重启应用后生效。请检查全局快捷键是否冲突。', '当前版本支持 JSON、Base64、URL 编码与中文文本处理。', '复制结果不会自动粘贴到其他应用，识别图片不会上传。'] },
  { id: 'small-12', font: 12, text: ['请先选择文件，再调整参数并预览输出结果。', '字体较小时，中文笔画容易粘连，标点和数字也可能识别错误。', '文件路径 /Users/test/截图.png，版本 v1.3.1，编号 000102。'] },
  { id: 'retina-12x2', font: 12, dpr: 2, text: ['请先选择文件，再调整参数并预览输出结果。', '字体较小时，中文笔画容易粘连，标点和数字也可能识别错误。', '文件路径 /Users/test/截图.png，版本 v1.3.1，编号 000102。'] },
  { id: 'traditional-20', font: 20, text: ['繁體中文辨識：檔案名稱、視窗設定與離線文字擷取。', '請確認輸出內容，保留原圖，再將辨識結果複製到剪貼簿。', '訂單編號 001234，價格 NT$128.50，時間 09:30。'] },
  { id: 'mixed-18', font: 18, text: ['请求失败：HTTP 403，请检查 API Key 和网络连接。', '错误代码 EACCES，配置项 enableOCR=true，超时 3000ms。', '邮箱 test@example.com，地址 https://example.com/api/v1。'] },
  { id: 'blur-low-contrast', font: 18, blur: 0.7, lowContrast: true, text: ['截图中文字模糊时，应先查看预览再确认识别结果。', '保存失败，请重试或复制内容后手动保存，原文件未被修改。', '库存 000108 件，合计 ¥2,048.90，折扣 8.5%。'] },
  { id: 'serif-22', font: 22, family: 'Songti SC', text: ['离线文字识别用于截图、文档片段和日常信息整理。', '模型体积与运行时体积应分别统计，不能混为一谈。', '简体中文、繁體中文和 English 需要分别验证。'] }
]

;(async () => {
  const output = path.resolve(process.argv[2] || 'doc/ocr-evaluation')
  await fs.mkdir(output, { recursive: true })
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'mootool-ocr-fixtures-'))
  const app = await _electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: path.resolve(__dirname, '../..'), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  try {
    const page = await app.firstWindow()
    for (const sample of samples) {
      const dataUrl = await page.evaluate(async sample => {
        await document.fonts.ready
        const dpr = sample.dpr || 1
        const canvas = document.createElement('canvas')
        canvas.width = 1000 * dpr; canvas.height = 240 * dpr
        const context = canvas.getContext('2d')
        context.scale(dpr, dpr)
        context.fillStyle = sample.dark ? '#202124' : '#ffffff'
        context.fillRect(0, 0, 1000, 240)
        context.fillStyle = sample.dark ? '#eeeeee' : sample.lowContrast ? '#999999' : '#222222'
        context.font = `${sample.font}px "${sample.family || 'PingFang SC'}", sans-serif`
        context.textBaseline = 'top'
        sample.text.forEach((line, index) => context.fillText(line, 40, 35 + index * 65))
        if (sample.blur) {
          const blurred = document.createElement('canvas'); blurred.width = canvas.width; blurred.height = canvas.height
          const blurredContext = blurred.getContext('2d'); blurredContext.filter = `blur(${sample.blur}px)`
          blurredContext.drawImage(canvas, 0, 0)
          return blurred.toDataURL('image/png')
        }
        return canvas.toDataURL('image/png')
      }, sample)
      await fs.writeFile(path.join(output, `${sample.id}.png`), Buffer.from(dataUrl.split(',')[1], 'base64'))
      console.log(sample.id)
    }
    await fs.writeFile(path.join(output, 'samples.json'), JSON.stringify(samples.map(sample => ({ ...sample, width: 1000 * (sample.dpr || 1), height: 240 * (sample.dpr || 1), expected: sample.text.join('\n') })), null, 2) + '\n')
  } finally { await app.close(); await fs.rm(directory, { recursive: true, force: true }) }
})().catch(error => { console.error(error); process.exitCode = 1 })
