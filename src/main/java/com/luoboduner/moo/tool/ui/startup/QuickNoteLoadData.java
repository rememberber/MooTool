package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.domain.TQuickNote;
import com.luoboduner.moo.tool.util.QuickNoteVaultUtil;
import lombok.Getter;

import java.util.List;

/**
 * Quick Note 后台加载的数据快照。
 */
@Getter
public final class QuickNoteLoadData {

    private final List<TQuickNote> notes;
    private final List<String> folders;

    public QuickNoteLoadData(List<TQuickNote> notes, List<String> folders) {
        this.notes = notes == null ? List.of() : List.copyOf(notes);
        this.folders = folders == null ? List.of() : List.copyOf(folders);
    }

    public static QuickNoteLoadData loadInitial() throws Exception {
        EdtGuard.assertNotEdt();
        QuickNoteVaultUtil.ensureVaultReady();
        List<TQuickNote> notes = QuickNoteVaultUtil.listByFilter("", false);
        List<String> folders = QuickNoteVaultUtil.listFolders();
        return new QuickNoteLoadData(notes, folders);
    }
}
