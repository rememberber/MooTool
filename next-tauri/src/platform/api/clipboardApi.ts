import {
  readImage as readNativeImage,
  writeImage as writeNativeImage,
  writeText as writeNativeText
} from '@tauri-apps/plugin-clipboard-manager'

export interface ClipboardApi {
  writeText(value: string): Promise<void>
  readImageFile(): Promise<File | null>
  writeImageDataUrl(value: string): Promise<void>
}

const browserApi: ClipboardApi = {
  writeText: async (value) => {
    if (!navigator.clipboard?.writeText) throw new Error('当前浏览器未开放剪贴板写入')
    await navigator.clipboard.writeText(value)
  },
  readImageFile: async () => {
    if (!navigator.clipboard?.read) return null
    for (const item of await navigator.clipboard.read()) {
      const type = item.types.find((value) => value.startsWith('image/'))
      if (type) return new File([await item.getType(type)], 'clipboard.png', { type })
    }
    return null
  },
  writeImageDataUrl: async (value) => {
    if (!navigator.clipboard?.write || typeof ClipboardItem === 'undefined') throw new Error('当前浏览器未开放图片剪贴板写入')
    const blob = await fetch(value).then((response) => response.blob())
    await navigator.clipboard.write([new ClipboardItem({ [blob.type]: blob })])
  }
}

export const clipboardApi: ClipboardApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      writeText: writeNativeText,
      readImageFile: async () => {
        const image = await readNativeImage()
        const [{ width, height }, rgba] = await Promise.all([image.size(), image.rgba()])
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const context = canvas.getContext('2d')
        if (!context) throw new Error('无法创建剪贴板图片画布')
        context.putImageData(new ImageData(new Uint8ClampedArray(rgba), width, height), 0, 0)
        const blob = await new Promise<Blob>((resolve, reject) => canvas.toBlob((value) => value ? resolve(value) : reject(new Error('无法编码剪贴板图片')), 'image/png'))
        return new File([blob], 'clipboard.png', { type: 'image/png' })
      },
      writeImageDataUrl: async (value) => {
        const image = await loadHtmlImage(value)
        const canvas = document.createElement('canvas')
        canvas.width = image.naturalWidth
        canvas.height = image.naturalHeight
        const context = canvas.getContext('2d')
        if (!context) throw new Error('无法创建图片剪贴板画布')
        context.drawImage(image, 0, 0)
        const pixels = context.getImageData(0, 0, canvas.width, canvas.height)
        const { Image } = await import('@tauri-apps/api/image')
        await writeNativeImage(await Image.new(Uint8Array.from(pixels.data), canvas.width, canvas.height))
      }
    }
  : browserApi

function loadHtmlImage(source: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('无法解码剪贴板图片'))
    image.src = source
  })
}
