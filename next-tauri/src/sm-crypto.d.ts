declare module 'sm-crypto' {
  export const sm3: (value: string) => string
  export const sm4: {
    encrypt(value: string, key: string, options: { mode: 'ecb'; padding: 'pkcs#7' }): string
    decrypt(value: string, key: string, options: { mode: 'ecb'; padding: 'pkcs#7' }): string
  }
  export const sm2: {
    generateKeyPairHex(): { publicKey: string; privateKey: string }
    doEncrypt(value: string, publicKey: string, cipherMode: 1): string
    doDecrypt(value: string, privateKey: string, cipherMode: 1): string
    doSignature(value: string, privateKey: string, options: { hash: true; der: true; publicKey?: string }): string
    doVerifySignature(value: string, signature: string, publicKey: string, options: { hash: true; der: true }): boolean
  }
}
