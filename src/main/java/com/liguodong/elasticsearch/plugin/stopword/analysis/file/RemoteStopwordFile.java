package com.liguodong.elasticsearch.plugin.stopword.analysis.file;

import com.liguodong.elasticsearch.plugin.stopword.analysis.file.StopwordFile;
import org.apache.lucene.analysis.util.CharArraySet;

import java.io.Reader;
import java.util.List;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 10:30
 */
public class RemoteStopwordFile implements StopwordFile {
    
    @Override
    public CharArraySet reloadStopwordSet() {
        return null;
    }

    @Override
    public boolean isNeedReloadStopwordSet() {
        return false;
    }

    @Override
    public List<String> getReader() {
        return null;
    }
}
