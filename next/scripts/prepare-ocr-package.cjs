module.exports = async context => {
  if (context.electronPlatformName === 'darwin') return
  const { prepareOcrResources } = await import('./prepare-ocr-resources.mjs')
  const arch = { 0: 'ia32', 1: 'x64', 2: 'armv7l', 3: 'arm64' }[context.arch]
  await prepareOcrResources(context.electronPlatformName, arch)
}
