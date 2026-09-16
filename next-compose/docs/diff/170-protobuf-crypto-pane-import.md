# DIFF-170：Protobuf / 加解密分栏与 Electron 迁入

## 背景

Electron Protobuf：`protobuf-json`（0.34:0.66）、`protobuf-wire` / `protobuf-convert`（三列 1:0.28:1）。加解密：`crypto-symmetric`、`crypto-base`（三列）、`crypto-key-pair`（1:1）。compose 此前为固定 `weight` 或 132dp 中栏，迁入未覆盖。

## 行为

- **F07**：JSON / Wire / Convert 三个 Tab 分栏可拖，键分别为 `protobuf-json`、`protobuf-wire`、`protobuf-convert`。
- **F14**：对称/摘要 Base 三列与公私钥两列可拖，键 `crypto-symmetric`、`crypto-base`、`crypto-key-pair`。
- **A03**：`ElectronPaneSizeImport` 增加上述键；抽取 `IoThreePaneRow` / `IoTwoPaneRow`（`ui/components/IoPaneLayouts.kt`）供配置/Protobuf/加解密复用。

## 验证

- `ElectronPaneSizeImportTest.convertsProtobufAndCryptoPaneRatios`
- `./gradlew :composeApp:desktopTest --offline`
