package com.liguodong.elasticsearch.plugin.stopword.analysis.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.analysis.util.CharArraySet;
import org.elasticsearch.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 10:28
 */
public class StopwordHttpImpl implements StopwordHttp{

    // 远程url地址
    private String location;

    //上次更新时间戳
    private String lastTimestamp;

    //结果
    private String result = "";

    // 获取当前客户端对象
    HttpClient httpClient = HttpClientBuilder.create().build();

    public StopwordHttpImpl(String location) {
        this.location = location;
        isNeedReloadStopwordSet();
    }

    @Override
    public CharArraySet reloadStopwordSet() {
        CharArraySet stopwords = null;
        try {
            LOGGER.info("start reload remote stopwords from {}.", location);
            List<String> list = new ArrayList<>();
            if(StringUtils.isNotBlank(result)){
                JSONObject jsonObject = JSONObject.parseObject(result);
                this.lastTimestamp = jsonObject.get("lastTimestamp").toString();

                JSONArray jsonArray =jsonObject.getJSONArray("stopWordList");
                for (int i = 0; i < jsonArray.size(); i++) {
                    list.add(jsonArray.get(i).toString());
                }
                CharArraySet stopSet = new CharArraySet(list, false);
                stopwords = CharArraySet.unmodifiableSet(stopSet);
            }
        } catch (Exception e) {
            LOGGER.error("reload remote stopwords {} error!", e, location);
            throw new IllegalArgumentException("could not reload remote stopwords file to build stopwords", e);
        }
        return stopwords;
    }

    @Override
    public boolean isNeedReloadStopwordSet() {
        //http://localhost:8100/extStopWordDic/updateStopWord?time=1234
        String urlNameString = location+"?lastTimestamp="+lastTimestamp;
        HttpResponse response = null;
        HttpGet request = null;
        try {
            // 根据地址获取请求
            request = new HttpGet(urlNameString);
            // 通过请求对象获取响应对象
            response = httpClient.execute(request);

            // 判断网络连接状态码是否正常
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                result= EntityUtils.toString(response.getEntity(),"utf-8");
                JSONObject jsonObject = JSONObject.parseObject(result);
                String isUpdate = jsonObject.get("isUpdate").toString();
                return Boolean.parseBoolean(isUpdate);
            } else {
                LOGGER.info("remote stopword {} return bad code {}", location,
                        response.getStatusLine().getStatusCode());
            }
        } catch (Exception e) {
            LOGGER.error("check need reload remote stopword {} error!", e, location);
        }
        return false;
    }


}
