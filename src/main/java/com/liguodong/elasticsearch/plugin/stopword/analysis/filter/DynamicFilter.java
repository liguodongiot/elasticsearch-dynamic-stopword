package com.liguodong.elasticsearch.plugin.stopword.analysis.filter;

import org.apache.lucene.analysis.util.CharArraySet;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 14:29
 */
public interface DynamicFilter {

    //更新停止词
    void update(CharArraySet charArraySet);

}
