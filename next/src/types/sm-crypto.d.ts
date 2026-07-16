declare module 'sm-crypto' {
  export const sm2: {
    generateKeyPairHex(): { publicKey: string; privateKey: string }
    doEncrypt(message: string | number[], publicKey: string, cipherMode?: number): string
    doDecrypt(cipherText: string, privateKey: string, cipherMode?: number, options?: { output?: 'string' | 'array' }): string | number[]
    doSignature(message: string, privateKey: string, options?: { hash?: boolean; publicKey?: string; der?: boolean }): string
    doVerifySignature(message: string, signature: string, publicKey: string, options?: { hash?: boolean; der?: boolean }): boolean
  }
  export function sm3(input: string | number[]): string
  export const sm4: {
    encrypt(input: string | number[], key: string | number[], options?: { mode?: 'cbc' | 'ecb'; iv?: string | number[]; padding?: 'pkcs#7' | 'pkcs#5' | 'none'; output?: 'string' | 'array' }): string | number[]
    decrypt(input: string | number[], key: string | number[], options?: { mode?: 'cbc' | 'ecb'; iv?: string | number[]; padding?: 'pkcs#7' | 'pkcs#5' | 'none'; output?: 'string' | 'array' }): string | number[]
  }
}
