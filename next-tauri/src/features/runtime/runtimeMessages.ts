import { defineMessages } from '../../app/localizedMessages'

export const runtimeMessages = defineMessages({
  'zh-CN': {
    'title': '代码运行', 'session.label': '会话', 'session.running': '运行中', 'session.exit': '退出 {code}',
    'session.available': '可用', 'session.missing': '未检测', 'notice.detecting': '正在检测本机运行时…',
    'notice.detected': '检测到 {count} 个可用运行时', 'error.runtimeMissing': '{runtime} 未在 PATH 中找到',
    'error.unclosedArguments': '运行参数存在未闭合引号', 'error.argumentLimit': '运行参数超出限制',
    'notice.timeout': '运行超时，进程已终止', 'notice.cancelled': '运行已取消', 'notice.finished': '运行结束，退出码 {code}',
    'notice.stopping': '正在终止运行进程…', 'operation.run': '运行代码', 'field.arguments': '参数', 'field.timeout': '超时',
    'field.workingDirectory': '工作目录', 'placeholder.workingDirectory': '留空使用隔离临时目录', 'action.chooseDirectory': '选择工作目录', 'action.detect': '检测',
    'action.stop': '停止', 'action.run': '运行', 'action.format': '格式化', 'notice.formatted': '源码已格式化', 'status.notInstalled': '未安装或不在 PATH', 'pane.source': '源码 · {runtime}',
    'action.restore': '恢复示例', 'aria.source': '运行源码', 'pane.output': '输出', 'output.running': '进程正在运行…',
    'output.empty': '运行输出将在这里实时显示', 'footer.capabilities': '白名单运行时 · 不经过 shell · {seconds} 秒超时 · 支持终止进程组',
    'report.error': '运行状态上报失败：{error}', 'host.loading': '正在加载代码运行工具…'
  },
  'en-US': {
    'title': 'Code Runner', 'session.label': 'Session', 'session.running': 'Running', 'session.exit': 'Exit {code}',
    'session.available': 'Available', 'session.missing': 'Not detected', 'notice.detecting': 'Detecting local runtimes…',
    'notice.detected': 'Detected {count} available runtimes', 'error.runtimeMissing': '{runtime} was not found in PATH',
    'error.unclosedArguments': 'Runtime arguments contain an unclosed quote', 'error.argumentLimit': 'Runtime arguments exceed the limit',
    'notice.timeout': 'Execution timed out and the process was terminated', 'notice.cancelled': 'Execution cancelled', 'notice.finished': 'Execution finished with exit code {code}',
    'notice.stopping': 'Stopping the runtime process…', 'operation.run': 'Run code', 'field.arguments': 'Arguments', 'field.timeout': 'Timeout',
    'field.workingDirectory': 'Working directory', 'placeholder.workingDirectory': 'Leave empty to use an isolated temporary directory', 'action.chooseDirectory': 'Choose working directory', 'action.detect': 'Detect',
    'action.stop': 'Stop', 'action.run': 'Run', 'action.format': 'Format', 'notice.formatted': 'Source formatted', 'status.notInstalled': 'Not installed or not in PATH', 'pane.source': 'Source · {runtime}',
    'action.restore': 'Restore sample', 'aria.source': 'Runtime source', 'pane.output': 'Output', 'output.running': 'Process is running…',
    'output.empty': 'Runtime output will stream here', 'footer.capabilities': 'Allowlisted runtimes · No shell · {seconds}-second timeout · Process-group termination',
    'report.error': 'Runtime status reporting failed: {error}', 'host.loading': 'Loading Code Runner…'
  },
  'ja-JP': {
    'title': 'コード実行', 'session.label': 'セッション', 'session.running': '実行中', 'session.exit': '終了 {code}',
    'session.available': '利用可能', 'session.missing': '未検出', 'notice.detecting': 'ローカルランタイムを検出中…',
    'notice.detected': '利用可能なランタイムを {count} 個検出しました', 'error.runtimeMissing': '{runtime} が PATH に見つかりません',
    'error.unclosedArguments': '実行引数に閉じられていない引用符があります', 'error.argumentLimit': '実行引数が上限を超えています',
    'notice.timeout': 'タイムアウトしたためプロセスを終了しました', 'notice.cancelled': '実行をキャンセルしました', 'notice.finished': '実行終了、終了コード {code}',
    'notice.stopping': '実行プロセスを停止中…', 'operation.run': 'コードを実行', 'field.arguments': '引数', 'field.timeout': 'タイムアウト',
    'field.workingDirectory': '作業ディレクトリ', 'placeholder.workingDirectory': '空の場合は隔離された一時ディレクトリを使用', 'action.chooseDirectory': '作業ディレクトリを選択', 'action.detect': '検出',
    'action.stop': '停止', 'action.run': '実行', 'action.format': '整形', 'notice.formatted': 'ソースを整形しました', 'status.notInstalled': '未インストールまたは PATH にありません', 'pane.source': 'ソース · {runtime}',
    'action.restore': 'サンプルを復元', 'aria.source': '実行ソース', 'pane.output': '出力', 'output.running': 'プロセスを実行中…',
    'output.empty': '実行出力はここへリアルタイム表示されます', 'footer.capabilities': '許可済みランタイム · シェル不使用 · {seconds} 秒タイムアウト · プロセスグループ停止',
    'report.error': '実行状態の報告に失敗しました：{error}', 'host.loading': 'コード実行ツールを読み込み中…'
  }
})
