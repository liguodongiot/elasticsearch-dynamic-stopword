package com.liguodong.elasticsearch.plugin;

import com.liguodong.elasticsearch.plugin.stopword.analysis.DynamicStopTokenFilterFactory;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.plugins.Plugin;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/13 20:42
 */
public class DynamicStopwordPlugin extends Plugin{


    @Override
    public String name() {
        return "analysis-dynamic-stopword";
    }

    @Override
    public String description() {
        return "Analysis-plugin for stopword";
    }

    public void onModule(AnalysisModule module) {
        module.addTokenFilter("dynamic_stopword", DynamicStopTokenFilterFactory.class);
    }
}
