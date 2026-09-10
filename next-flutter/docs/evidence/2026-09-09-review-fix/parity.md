# 对照

已修（单测覆盖，不是 Electron 全量对齐）：

- R02/R06/R10 保存队列与 Vault 先同步再落盘
- R03 恢复后重建内存
- R04/R05 笔记导入与 JSON 切换撤销边界
- R07 凭据离开 `settings.json` / 备份
- R08 SimplePdf 括号往返；非该格式仍拒绝
- R09 正则 `allMatches`
- R12 查找选区同步到 TextField
- R13 取消时杀进程树
- R14 主文件损坏回退 `.prev`；POSIX 不再先删主文件
- R15 写入前校验 `.MooTool` / 外产品 `product.json`
- R16 导航/工具页 Material；底栏不再挤成竖排
- R17 主题逐项字号后再缩放
- R01 用 `ServicesBinding` 建通道（**未**用 Release 安装包验收）
- R11 分离按钮不再假装成功（**仍无**第二 engine）

仍未完成：真多窗口、真 PDF 页复制、SM4/RSA/SM2、QR 识别、Bing、Cron `L`/`#`、行号同步滚动、计算器分区 UI、Electron 并排截图。
