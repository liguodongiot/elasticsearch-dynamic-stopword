package com.liguodong.elasticsearch.plugin.stopword.analysis.file;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.util.CharArraySet;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import java.io.Reader;
import java.util.List;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 10:28
 */
public interface StopwordFile {

    ESLogger LOGGER = Loggers.getLogger("dynamic-stopword");

    CharArraySet reloadStopwordSet();

    //判断是否需要重新加载
    boolean isNeedReloadStopwordSet();

    List<String> readFile();

}
