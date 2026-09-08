class TransferRequest {
  TransferRequest({
    required this.sessionId,
    required this.sourceWindowId,
    required this.targetWindowId,
    required this.revision,
    required this.text,
    required this.selectionStart,
    required this.selectionEnd,
    required this.undoDepth,
  });

  final String sessionId;
  final String sourceWindowId;
  final String targetWindowId;
  final int revision;
  final String text;
  final int selectionStart;
  final int selectionEnd;
  final int undoDepth;
}

class TransferResult {
  TransferResult({required this.ok, this.ownerWindowId, this.error});
  final bool ok;
  final String? ownerWindowId;
  final String? error;
}

/// Single-writer session transfer used before a second Flutter engine is shown.
class SessionCoordinator {
  SessionCoordinator({this.windowId = 'main'});

  String windowId;
  final Map<String, String> _owners = {};
  final Map<String, TransferRequest> _locks = {};

  String ownerOf(String sessionId) => _owners[sessionId] ?? windowId;

  void claim(String sessionId) => _owners[sessionId] = windowId;

  TransferResult beginTransfer(TransferRequest request) {
    if (_locks.containsKey(request.sessionId)) {
      return TransferResult(
          ok: false,
          ownerWindowId: _owners[request.sessionId],
          error: 'session is already transferring');
    }
    if (ownerOf(request.sessionId) != request.sourceWindowId) {
      return TransferResult(
          ok: false,
          ownerWindowId: ownerOf(request.sessionId),
          error: 'source is not the write owner');
    }
    _locks[request.sessionId] = request;
    return TransferResult(ok: true, ownerWindowId: request.sourceWindowId);
  }

  TransferResult ack(String sessionId, String targetWindowId, int revision) {
    final lock = _locks[sessionId];
    if (lock == null)
      return TransferResult(ok: false, error: 'no transfer in progress');
    if (lock.targetWindowId != targetWindowId || lock.revision != revision) {
      return TransferResult(
          ok: false,
          ownerWindowId: lock.sourceWindowId,
          error: 'revision or target mismatch');
    }
    _owners[sessionId] = targetWindowId;
    _locks.remove(sessionId);
    return TransferResult(ok: true, ownerWindowId: targetWindowId);
  }

  TransferResult abort(String sessionId) {
    final lock = _locks.remove(sessionId);
    if (lock == null)
      return TransferResult(ok: false, error: 'no transfer in progress');
    return TransferResult(ok: true, ownerWindowId: lock.sourceWindowId);
  }
}
