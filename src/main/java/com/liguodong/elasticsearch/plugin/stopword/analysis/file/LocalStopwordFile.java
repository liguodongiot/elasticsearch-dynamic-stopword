package com.liguodong.elasticsearch.plugin.stopword.analysis.file;

import org.apache.lucene.analysis.util.CharArraySet;
import org.elasticsearch.env.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 10:29
 */
public class LocalStopwordFile implements StopwordFile {

    private Environment env;

    /** 本地文件路径 相对于config目录 */
    private String location;

    private Path stopwordFilePath;

    /** 上次更改时间 */
    private long lastModified;

    public LocalStopwordFile(Environment env, String location) {
        this.env = env;
        this.location = location;
        this.stopwordFilePath = env.configFile().resolve(location);
        isNeedReloadStopwordSet();
    }

    @Override
    public CharArraySet reloadStopwordSet() {
        CharArraySet stopwords = null;
        try {
            LOGGER.info("start reload local stopwords from {}.", location);
            List<String> list = readFile();
            CharArraySet stopSet = new CharArraySet(list, false);
            stopwords = CharArraySet.unmodifiableSet(stopSet);
        } catch (Exception e) {
            LOGGER.error("reload local stopwords {} error!", e, location);
            throw new IllegalArgumentException(
                    "could not reload local stopwords file to build stopwords", e);
        }
        return stopwords;
    }

    //判断是否需要重新加载
    @Override
    public boolean isNeedReloadStopwordSet() {
        try {
            File stopwordFile = stopwordFilePath.toFile();
            if (stopwordFile.exists() && lastModified < stopwordFile.lastModified()) {
                lastModified = stopwordFile.lastModified();
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("check need reload local stopword {} error!", e, location);
        }
        return false;
    }

    @Override
    public List<String> readFile() {
        BufferedReader br = null;
        List<String> stopWordList = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(stopwordFilePath.toString()));
            String line = null;
            while ((line = br.readLine()) != null) {
                stopWordList.add(line);
            }
            LOGGER.info("list:{}.",stopWordList.toString());
        } catch (Exception e) {
            LOGGER.error("reload local stopword file [{}] error!", e, location);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                LOGGER.error("close io error.");
            }
        }
        return stopWordList;
    }
}
