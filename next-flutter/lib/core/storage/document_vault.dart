class VaultException implements Exception {
  VaultException(this.message);
  final String message;
  @override
  String toString() => message;
}

enum VaultSort { name, modified, created }

class VaultFolder {
  VaultFolder(
      {required this.id,
      required this.toolId,
      required this.name,
      this.parentId,
      DateTime? created,
      DateTime? modified})
      : created = created ?? DateTime.now(),
        modified = modified ?? DateTime.now();

  final String id;
  final String toolId;
  String? parentId;
  String name;
  DateTime created;
  DateTime modified;

  Map<String, Object?> toJson() => {
        'id': id,
        'toolId': toolId,
        'parentId': parentId,
        'name': name,
        'created': created.toIso8601String(),
        'modified': modified.toIso8601String(),
      };

  factory VaultFolder.fromJson(Map<String, Object?> json) => VaultFolder(
        id: json['id'] as String,
        toolId: json['toolId'] as String,
        name: json['name'] as String,
        parentId: json['parentId'] as String?,
        created: DateTime.tryParse(json['created'] as String? ?? '') ??
            DateTime.now(),
        modified: DateTime.tryParse(json['modified'] as String? ?? '') ??
            DateTime.now(),
      );
}

class VaultDocument {
  VaultDocument({
    required this.id,
    required this.toolId,
    required this.title,
    this.content = '',
    this.parentId,
    this.query = '',
    this.output = '',
    Map<String, Object?>? metadata,
    DateTime? created,
    DateTime? modified,
  })  : created = created ?? DateTime.now(),
        modified = modified ?? DateTime.now(),
        metadata = metadata ?? {};

  final String id;
  final String toolId;
  String? parentId;
  String title;
  String content;
  String query;
  String output;
  final Map<String, Object?> metadata;
  DateTime created;
  DateTime modified;

  Map<String, Object?> toJson() => {
        'id': id,
        'toolId': toolId,
        'parentId': parentId,
        'title': title,
        'content': content,
        'query': query,
        'output': output,
        'metadata': metadata,
        'created': created.toIso8601String(),
        'modified': modified.toIso8601String(),
      };

  factory VaultDocument.fromJson(Map<String, Object?> json) => VaultDocument(
        id: json['id'] as String,
        toolId: json['toolId'] as String,
        title: json['title'] as String,
        content: json['content'] as String? ?? '',
        parentId: json['parentId'] as String?,
        query: json['query'] as String? ?? '',
        output: json['output'] as String? ?? '',
        metadata: Map<String, Object?>.from(json['metadata'] as Map? ?? {}),
        created: DateTime.tryParse(json['created'] as String? ?? '') ??
            DateTime.now(),
        modified: DateTime.tryParse(json['modified'] as String? ?? '') ??
            DateTime.now(),
      );
}

class VaultPreferences {
  VaultPreferences({
    this.selectedEntryId,
    Set<String>? expanded,
    this.query = '',
    this.includeContent = true,
    this.sort = VaultSort.name,
    this.treeVisible = true,
  }) : expanded = expanded ?? {};

  String? selectedEntryId;
  Set<String> expanded;
  String query;
  bool includeContent;
  VaultSort sort;
  bool treeVisible;

  Map<String, Object?> toJson() => {
        'selectedEntryId': selectedEntryId,
        'expanded': expanded.toList(),
        'query': query,
        'includeContent': includeContent,
        'sort': sort.name,
        'treeVisible': treeVisible,
      };

  factory VaultPreferences.fromJson(Map<String, Object?> json) =>
      VaultPreferences(
        selectedEntryId: json['selectedEntryId'] as String?,
        expanded: {...?json['expanded'] as List?},
        query: json['query'] as String? ?? '',
        includeContent: json['includeContent'] as bool? ?? true,
        sort: VaultSort.values.firstWhere((value) => value.name == json['sort'],
            orElse: () => VaultSort.name),
        treeVisible: json['treeVisible'] as bool? ?? true,
      );
}

class VaultNode {
  VaultNode(
      {required this.id,
      required this.title,
      required this.path,
      required this.isFolder,
      required this.modified,
      required this.created,
      required this.children});
  final String id;
  final String title;
  final String path;
  final bool isFolder;
  final DateTime modified;
  final DateTime created;
  final List<VaultNode> children;
}

class DocumentVault {
  DocumentVault({List<VaultDocument>? documents, List<VaultFolder>? folders})
      : documents = documents ?? [],
        folders = folders ?? [];

  final List<VaultDocument> documents;
  final List<VaultFolder> folders;

  static const supportedTools = ['json', 'quickNote'];

  void validate() {
    final ids = [
      ...documents.map((item) => item.id),
      ...folders.map((item) => item.id)
    ];
    if (ids.toSet().length != ids.length) throw VaultException('文档库包含重复标识。');
    final byId = {for (final folder in folders) folder.id: folder};
    for (final folder in folders) {
      if (!supportedTools.contains(folder.toolId))
        throw VaultException('文件夹所属工具无效。');
      validName(folder.name);
      var visited = {folder.id};
      var parent = folder.parentId;
      while (parent != null) {
        final ancestor = byId[parent];
        if (ancestor == null ||
            ancestor.toolId != folder.toolId ||
            !visited.add(parent) ||
            visited.length > 64) {
          throw VaultException('文件夹层级无效、包含循环或超过 64 层。');
        }
        parent = ancestor.parentId;
      }
    }
    for (final file in documents) {
      if (!supportedTools.contains(file.toolId))
        throw VaultException('文档所属工具无效。');
      if (file.parentId != null && byId[file.parentId]?.toolId != file.toolId) {
        throw VaultException('文档的父文件夹不存在或属于其他工具。');
      }
    }
  }

  String? parentOf(String id) =>
      folders
          .cast<VaultFolder?>()
          .firstWhere((item) => item!.id == id, orElse: () => null)
          ?.parentId ??
      documents
          .cast<VaultDocument?>()
          .firstWhere((item) => item!.id == id, orElse: () => null)
          ?.parentId;

  String? toolOf(String id) =>
      folders
          .cast<VaultFolder?>()
          .firstWhere((item) => item!.id == id, orElse: () => null)
          ?.toolId ??
      documents
          .cast<VaultDocument?>()
          .firstWhere((item) => item!.id == id, orElse: () => null)
          ?.toolId;

  String? nameOf(String id) =>
      folders
          .cast<VaultFolder?>()
          .firstWhere((item) => item!.id == id, orElse: () => null)
          ?.name ??
      documents
          .cast<VaultDocument?>()
          .firstWhere((item) => item!.id == id, orElse: () => null)
          ?.title;

  List<String> ancestorsOf(String id) {
    final result = <String>[];
    var current = parentOf(id);
    while (current != null && !result.contains(current) && result.length < 64) {
      result.add(current);
      current = parentOf(current);
    }
    return result.reversed.toList();
  }

  String pathOf(String id) =>
      [...ancestorsOf(id), id].map(nameOf).whereType<String>().join('/');

  Set<String> descendantsOf(String id) {
    final ids = {id};
    final pending = [id];
    while (pending.isNotEmpty) {
      final parent = pending.removeLast();
      for (final folder in folders.where((item) => item.parentId == parent)) {
        if (ids.add(folder.id)) pending.add(folder.id);
      }
      ids.addAll(documents
          .where((item) => item.parentId == parent)
          .map((item) => item.id));
    }
    return ids;
  }

  static String validName(String value) {
    final name = value.trim();
    if (name.isEmpty ||
        name == '.' ||
        name == '..' ||
        name.length > 240 ||
        name.contains(RegExp(r'[/\\:\n\r]'))) {
      throw VaultException('名称不能为空，不能包含路径分隔符、控制字符或超过 240 字节。');
    }
    return name;
  }

  void _checkParent(String? parent, String toolId) {
    if (!supportedTools.contains(toolId)) throw VaultException('此工具不支持文档库。');
    if (parent != null &&
        !folders.any((item) => item.id == parent && item.toolId == toolId)) {
      throw VaultException('目标文件夹不存在或属于其他工具。');
    }
  }

  bool _available(String name, String toolId, String? parent,
      {String? excluding}) {
    String key(String text) => text.toLowerCase();
    return !folders.any((item) =>
            item.id != excluding &&
            item.toolId == toolId &&
            item.parentId == parent &&
            key(item.name) == key(name)) &&
        !documents.any((item) =>
            item.id != excluding &&
            item.toolId == toolId &&
            item.parentId == parent &&
            key(item.title) == key(name));
  }

  String uniqueName(String proposed, String toolId, String? parent) {
    if (_available(proposed, toolId, parent)) return proposed;
    final dot = proposed.lastIndexOf('.');
    final stem = dot > 0 ? proposed.substring(0, dot) : proposed;
    final ext = dot > 0 ? proposed.substring(dot) : '';
    var index = 2;
    while (true) {
      final name = '$stem ($index)$ext';
      if (_available(name, toolId, parent)) return name;
      index++;
    }
  }

  String createFolder(
      {required String toolId,
      required String name,
      String? parent,
      String? id}) {
    _checkParent(parent, toolId);
    final valid = validName(name);
    if (!_available(valid, toolId, parent))
      throw VaultException('目标位置已有同名文件或文件夹。');
    if (parent != null && ancestorsOf(parent).length >= 63)
      throw VaultException('文件夹不能超过 64 层。');
    final folder = VaultFolder(
        id: id ?? _id(), toolId: toolId, name: valid, parentId: parent);
    folders.add(folder);
    return folder.id;
  }

  String createDocument(
      {required String toolId,
      required String name,
      String content = '',
      String? parent,
      String? id}) {
    _checkParent(parent, toolId);
    final valid = validName(name);
    if (!_available(valid, toolId, parent))
      throw VaultException('目标位置已有同名文件或文件夹。');
    final file = VaultDocument(
        id: id ?? _id(),
        toolId: toolId,
        title: valid,
        content: content,
        parentId: parent);
    documents.add(file);
    return file.id;
  }

  void rename(String id, String value) {
    final toolId = toolOf(id);
    if (toolId == null) throw VaultException('所选项目已不存在。');
    final name = validName(value);
    if (!_available(name, toolId, parentOf(id), excluding: id))
      throw VaultException('目标位置已有同名文件或文件夹。');
    for (final folder in folders.where((item) => item.id == id)) {
      folder.name = name;
      folder.modified = DateTime.now();
    }
    for (final file in documents.where((item) => item.id == id)) {
      file.title = name;
      file.modified = DateTime.now();
    }
  }

  void move(String id, String? parent) {
    final toolId = toolOf(id);
    final name = nameOf(id);
    if (toolId == null || name == null) throw VaultException('所选项目已不存在。');
    _checkParent(parent, toolId);
    if (parent != null && descendantsOf(id).contains(parent))
      throw VaultException('不能将文件夹移动到自身内部。');
    if (!_available(name, toolId, parent, excluding: id))
      throw VaultException('目标位置已有同名文件或文件夹。');
    for (final folder in folders.where((item) => item.id == id)) {
      folder.parentId = parent;
      folder.modified = DateTime.now();
    }
    for (final file in documents.where((item) => item.id == id)) {
      file.parentId = parent;
      file.modified = DateTime.now();
    }
  }

  String duplicate(String id) {
    final file = documents
        .cast<VaultDocument?>()
        .firstWhere((item) => item!.id == id, orElse: () => null);
    if (file == null) throw VaultException('只能复制文档。');
    final copyId = createDocument(
        toolId: file.toolId,
        name: uniqueName(file.title, file.toolId, file.parentId),
        content: file.content,
        parent: file.parentId);
    documents
        .firstWhere((item) => item.id == copyId)
        .metadata
        .addAll(file.metadata);
    return copyId;
  }

  Set<String> delete(String id) {
    final removed = descendantsOf(id);
    documents.removeWhere((item) => removed.contains(item.id));
    folders.removeWhere((item) => removed.contains(item.id));
    return removed;
  }

  List<VaultNode> tree(String toolId,
      {VaultSort sort = VaultSort.name,
      String query = '',
      bool includeContent = false}) {
    List<VaultNode> build(String? parent, String prefix) {
      final folderNodes = folders
          .where((item) => item.toolId == toolId && item.parentId == parent)
          .map((folder) {
        final children = build(
            folder.id, prefix.isEmpty ? folder.name : '$prefix/${folder.name}');
        return VaultNode(
            id: folder.id,
            title: folder.name,
            path: prefix.isEmpty ? folder.name : '$prefix/${folder.name}',
            isFolder: true,
            modified: folder.modified,
            created: folder.created,
            children: children);
      });
      final fileNodes = documents
          .where((item) => item.toolId == toolId && item.parentId == parent)
          .map((file) {
        return VaultNode(
            id: file.id,
            title: file.title,
            path: prefix.isEmpty ? file.title : '$prefix/${file.title}',
            isFolder: false,
            modified: file.modified,
            created: file.created,
            children: const []);
      });
      final nodes = [...folderNodes, ...fileNodes];
      nodes.sort((a, b) {
        if (a.isFolder != b.isFolder) return a.isFolder ? -1 : 1;
        return switch (sort) {
          VaultSort.modified => b.modified.compareTo(a.modified),
          VaultSort.created => b.created.compareTo(a.created),
          VaultSort.name =>
            a.title.toLowerCase().compareTo(b.title.toLowerCase()),
        };
      });
      if (query.trim().isEmpty) return nodes;
      final needle = query.toLowerCase();
      return [
        for (final node in nodes)
          if (node.isFolder)
            VaultNode(
              id: node.id,
              title: node.title,
              path: node.path,
              isFolder: true,
              modified: node.modified,
              created: node.created,
              children: node.children,
            )
          else if (node.title.toLowerCase().contains(needle) ||
              (includeContent &&
                  documents.any((file) =>
                      file.id == node.id &&
                      file.content.toLowerCase().contains(needle))))
            node
      ]
          .where((node) =>
              !node.isFolder ||
              node.children.isNotEmpty ||
              node.title.toLowerCase().contains(needle))
          .toList();
    }

    return build(null, '');
  }

  Map<String, Object?> toJson() => {
        'documents': [for (final item in documents) item.toJson()],
        'folders': [for (final item in folders) item.toJson()],
      };

  factory DocumentVault.fromJson(Map<String, Object?> json) => DocumentVault(
        documents: [
          for (final item in json['documents'] as List? ?? const [])
            if (item is Map)
              VaultDocument.fromJson(Map<String, Object?>.from(item)),
        ],
        folders: [
          for (final item in json['folders'] as List? ?? const [])
            if (item is Map)
              VaultFolder.fromJson(Map<String, Object?>.from(item)),
        ],
      );

  static String _id() =>
      'doc-${DateTime.now().microsecondsSinceEpoch}-${documentsSeed++}';
}

int documentsSeed = 0;
