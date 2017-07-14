package com.liguodong.elasticsearch.plugin.stopword.analysis.file;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.lucene.analysis.util.CharArraySet;
import org.elasticsearch.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 10:30
 */
public class RemoteStopwordFile implements StopwordFile {


    private Environment env;

    /** 远程url地址 */
    private String location;

    /** 上次更改时间 */
    private String lastModified;

    /** 资源属性 */
    private String eTags;

    private CloseableHttpClient httpclient = HttpClients.createDefault();

    public RemoteStopwordFile(Environment env, String location) {
        this.env = env;
        this.location = location;
        isNeedReloadStopwordSet();
    }

    @Override
    public CharArraySet reloadStopwordSet() {
        CharArraySet stopwords = null;
        try {
            LOGGER.info("start reload remote stopwords from {}.", location);
            List<String> list = readFile();
            CharArraySet stopSet = new CharArraySet(list, false);
            stopwords = CharArraySet.unmodifiableSet(stopSet);
        } catch (Exception e) {
            LOGGER.error("reload remote stopwords {} error!", e, location);
            throw new IllegalArgumentException("could not reload remote stopwords file to build stopwords", e);
        }
        return stopwords;
    }

    @Override
    public boolean isNeedReloadStopwordSet() {
        RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000).setSocketTimeout(15 * 1000)
                .build();
        HttpHead head = new HttpHead(location);
        head.setConfig(rc);

        // 设置请求头
        if (lastModified != null) {
            head.setHeader("If-Modified-Since", lastModified);
        }
        if (eTags != null) {
            head.setHeader("If-None-Match", eTags);
        }

        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(head);
            if (response.getStatusLine().getStatusCode() == 200) { // 返回200 才做操作
                if (!response.getLastHeader("Last-Modified").getValue().equalsIgnoreCase(lastModified)
                        || !response.getLastHeader("ETag").getValue().equalsIgnoreCase(eTags)) {

                    lastModified = response.getLastHeader("Last-Modified") == null ? null
                            : response.getLastHeader("Last-Modified")
                            .getValue();
                    eTags = response.getLastHeader("ETag") == null ? null
                            : response.getLastHeader("ETag").getValue();
                    return true;
                }
            } else if (response.getStatusLine().getStatusCode() == 304) {
                return false;
            } else {
                LOGGER.info("remote stopword {} return bad code {}", location,
                        response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            LOGGER.error("check need reload remote stopword {} error!", e, location);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 从远程服务器上下载自定义词条
     */
    @Override
    public List<String> readFile() {

        List<String> list = new ArrayList<>();
        RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000).setSocketTimeout(60 * 1000)
                .build();
        CloseableHttpResponse response = null;
        BufferedReader br = null;
        HttpGet get = new HttpGet(location);
        get.setConfig(rc);
        try {
            response = httpclient.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                String charset = "UTF-8"; // 获取编码，默认为utf-8
                if (response.getEntity().getContentType().getValue()
                        .contains("charset=")) {
                    String contentType = response.getEntity().getContentType()
                            .getValue();
                    charset = contentType.substring(contentType
                            .lastIndexOf("=") + 1);
                }
                br = new BufferedReader(new InputStreamReader(response
                        .getEntity().getContent(), charset));
                String line = null;
                while ((line = br.readLine()) != null) {
                    list.add(line);
                }
                LOGGER.info("list:{}.",list.toString());
            }
        } catch (IOException e) {
            LOGGER.error("get remote stopword reader {} error!", e, location);
            throw new IllegalArgumentException("IOException while reading remote stopword file", e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                LOGGER.error("close io error.");
            }
        }
        return list;
    }
}
