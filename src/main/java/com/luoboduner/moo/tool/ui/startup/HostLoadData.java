package com.luoboduner.moo.tool.ui.startup;

import com.luoboduner.moo.tool.domain.THost;
import com.luoboduner.moo.tool.util.MybatisUtil;
import com.luoboduner.moo.tool.dao.THostMapper;
import lombok.Getter;

import java.util.List;

/**
 * Host 后台加载快照。
 */
@Getter
public final class HostLoadData {

    private final List<THost> filteredHosts;
    private final List<THost> allHosts;

    public HostLoadData(List<THost> filteredHosts, List<THost> allHosts) {
        this.filteredHosts = filteredHosts == null ? List.of() : List.copyOf(filteredHosts);
        this.allHosts = allHosts == null ? List.of() : List.copyOf(allHosts);
    }

    public static HostLoadData loadInitial() {
        EdtGuard.assertNotEdt();
        THostMapper mapper = MybatisUtil.getSqlSession().getMapper(THostMapper.class);
        List<THost> filtered = mapper.selectByFilter("%%");
        List<THost> all = mapper.selectAll();
        return new HostLoadData(filtered, all);
    }
}
