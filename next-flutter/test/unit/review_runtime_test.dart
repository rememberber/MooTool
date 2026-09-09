import 'dart:async';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mootool_next_flutter/app/app_controller.dart';
import 'package:mootool_next_flutter/app/app_paths.dart';
import 'package:mootool_next_flutter/core/desktop/desktop_host.dart';
import 'package:mootool_next_flutter/features/runtime/runtime_service.dart';

void main() {
  test('R13 cancelling runtime must terminate child processes too', () async {
    final root =
        await Directory.systemTemp.createTemp('mootool-runtime-review-');
    final service = RuntimeExecutionService(root);
    final ready = Completer<int>();
    int? childPid;
    var output = '';
    final running = service.run(
        requestId: 'review_child_01',
        runtime: 'python',
        pythonPath: '/usr/bin/python3',
        code: '''import subprocess,sys,time
child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'])
print(child.pid, flush=True)
time.sleep(30)
''',
        onOutput: (stream, chunk) {
          if (stream != 'stdout') return;
          output += chunk;
          if (!ready.isCompleted && output.contains('\n')) {
            ready.complete(int.parse(output.trim()));
          }
        });
    try {
      childPid = await ready.future.timeout(const Duration(seconds: 5));
      await service.cancel('review_child_01');
      await Future<void>.delayed(const Duration(milliseconds: 1600));
      final status = await Process.run('/bin/kill', ['-0', '$childPid']);
      expect(status.exitCode, isNot(0),
          reason: 'spawned child $childPid is still alive after cancellation');
    } finally {
      if (childPid != null) Process.killPid(childPid, ProcessSignal.sigkill);
      await service.cancel('review_child_01');
      await running.timeout(const Duration(seconds: 5));
      if (root.existsSync()) root.deleteSync(recursive: true);
    }
  });

  test('R15 foreign product data must be rejected before any write', () async {
    final root =
        await Directory.systemTemp.createTemp('mootool-isolation-review-');
    final foreign = await Directory('${root.path}/.MooTool').create();
    await File('${foreign.path}/product.json')
        .writeAsString('{"productId":"legacy-java"}');
    final controller =
        AppController(AppPaths(foreign), desktopHost: MemoryDesktopHost());
    try {
      await expectLater(controller.load(), throwsA(isA<StateError>()));
      expect(Directory('${foreign.path}/vaults').existsSync(), isFalse);
    } finally {
      controller.dispose();
      root.deleteSync(recursive: true);
    }
  });
}
