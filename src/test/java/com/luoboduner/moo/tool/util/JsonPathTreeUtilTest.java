package com.luoboduner.moo.tool.util;

import cn.hutool.json.JSONUtil;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonPathTreeUtilTest {

    @Test
    public void shouldBuildObjectAndArrayPaths() {
        String json = "{\"name\":\"test\",\"arr\":[{\"id\":1}],\"a.b\":2}";
        DefaultMutableTreeNode root = JsonPathTreeUtil.buildNode(JSONUtil.parse(json), "$", "(root)");

        assertEquals("$.name", getPath(root, "name"));
        assertEquals("$.arr[0].id", getPath(root, "id"));
        assertEquals("$['a.b']", getPath(root, "a.b"));
        assertEquals("test", JSONUtil.getByPath(JSONUtil.parse(json), getPath(root, "name")));
        assertEquals(1, JSONUtil.getByPath(JSONUtil.parse(json), getPath(root, "id")));
        assertEquals(2, JSONUtil.getByPath(JSONUtil.parse(json), getPath(root, "a.b")));
    }

    @Test
    public void shouldUseBracketNotationForSpecialKeys() {
        assertTrue(JsonPathTreeUtil.needsBracketNotation("a.b"));
        assertTrue(JsonPathTreeUtil.needsBracketNotation("1key"));
        assertEquals("$['a.b']", JsonPathTreeUtil.appendObjectPath("$", "a.b"));
        assertEquals("$.name", JsonPathTreeUtil.appendObjectPath("$", "name"));
    }

    private String getPath(DefaultMutableTreeNode root, String displaySuffix) {
        JsonPathTreeUtil.JsonPathNode node = findNode(root, displaySuffix);
        return node.getJsonPath();
    }

    private JsonPathTreeUtil.JsonPathNode findNode(TreeNode parent, String displaySuffix) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            JsonPathTreeUtil.JsonPathNode data = (JsonPathTreeUtil.JsonPathNode) child.getUserObject();
            if (data.getDisplayText().startsWith(displaySuffix + ":") || data.getDisplayText().equals(displaySuffix)) {
                return data;
            }
            if (child.getChildCount() > 0) {
                JsonPathTreeUtil.JsonPathNode found = findNode(child, displaySuffix);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
