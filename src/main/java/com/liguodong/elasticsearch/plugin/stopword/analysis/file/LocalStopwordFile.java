package com.liguodong.elasticsearch.plugin.stopword.analysis.file;

import org.apache.lucene.analysis.util.CharArraySet;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.env.Environment;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 10:29
 */
public class LocalStopwordFile implements StopwordFile {

    private static ESLogger logger = Loggers.getLogger("dynamic-stopword");

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
            logger.info("start reload local stopwords from {}.", location);
            List<String> list = getReader();
            logger.info("list: {} .",list.toString());
            CharArraySet stopSet = new CharArraySet(list, false);
            stopwords = CharArraySet.unmodifiableSet(stopSet);
        } catch (Exception e) {
            logger.error("reload local stopwords {} error!", e, location);
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
            logger.error("check need reload local stopword {} error!", e, location);
        }
        return false;
    }

    @Override
    public List<String> getReader() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(stopwordFilePath.toString()));
            String line = "";
            List<String> contentString= new ArrayList<>();
            while ((line = br.readLine()) != null) {
                contentString.add(line);
            }
            logger.info("list:{}.",contentString.toString());
            return contentString;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
