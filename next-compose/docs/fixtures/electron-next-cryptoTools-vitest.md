# Electron `cryptoTools.test.ts` → Compose 对照

| 场景 | Electron `cryptoTools.test.ts` | Compose |
| --- | --- | --- |
| symmetric-round-trip | `round-trips AES/DES/SM4 symmetric encryption` | `CryptoEngineTest.matchesElectronSymmetricAndDigestFixtures` |
| digests | `computes standard and national digests` | 同上（MD5/SHA-256/SM3 `abc`） |
| base-round-trip | `round-trips Base64/Base32 text` | `CryptoEngineTest.roundTripsBaseEncodingsAndFileDigest` |
| random | `generates constrained random values` | `CryptoEngineTest.generatesConstrainedRandomValuesAndRejectsBadInput` |
| rsa-sm2 | RSA/SM2 encrypt/sign tests | `CryptoEngineTest.consumesElectronRsaAndSm2Samples` / `generatesRsaAndSm2RoundTrips` |
| key-bytes-ui | （Compose UI；Electron 无 vitest） | `CryptoEngineTest.symmetricKeyUtf8LengthMatchesNormalizationRules` + 对称 Tab `crypto.keyBytes`（[DIFF-524](../diff/524-http-crypto-pdf-tray-slice.md)） |
