package com.liguodong.elasticsearch.plugin.stopword.analysis;

import com.liguodong.elasticsearch.plugin.stopword.analysis.file.LocalStopwordFile;
import com.liguodong.elasticsearch.plugin.stopword.analysis.file.RemoteStopwordFile;
import com.liguodong.elasticsearch.plugin.stopword.analysis.file.StopwordFile;
import com.liguodong.elasticsearch.plugin.stopword.analysis.filter.DynamicFilter;
import com.liguodong.elasticsearch.plugin.stopword.analysis.filter.DynamicLucene43StopFilter;
import com.liguodong.elasticsearch.plugin.stopword.analysis.filter.DynamicStopwordFilter;
import com.liguodong.elasticsearch.plugin.stopword.analysis.filter.DynamicSuggestStopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.settings.IndexSettingsService;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class DynamicStopTokenFilterFactory extends AbstractTokenFilterFactory {

    public static ESLogger logger = Loggers.getLogger("dynamic-stopword");
    private ScheduledExecutorService pool;
    private volatile ScheduledFuture<?> scheduledFuture;

    //停止词
    private CharArraySet stopWords;

    private final boolean ignoreCase;
    private final boolean enablePositionIncrements;
    private final boolean removeTrailing;

    //索引名
    private final String indexName;
    //路径 必须要配置的，根据它的值是否是以`http://`开头来判断是本地文件，还是远程文件。
    private final String location;
    //间隔时间 非必须配置的，默认值是60，单位秒，表示间隔多少秒去检查停止词文件是否有更新。
    private final int interval;
    //保存所有设置停用词
    private Map<DynamicFilter, Integer> dynamicStopwordFilters = new WeakHashMap<>();


    @Inject
    public DynamicStopTokenFilterFactory(Index index,
                                         IndexSettingsService indexSettingsService,
                                         Environment env,
                                         @Assisted String name,
                                         @Assisted Settings settings) {
        super(index, indexSettingsService.getSettings(), name, settings);

        //索引名
        this.indexName = index.getName();
        //路径
        this.location = settings.get("stopwords_path");
        logger.info("indexName is [{}],location is [{}]....", indexName, location);

        //时间间隔
        this.interval = settings.getAsInt("interval", 60);
        //true将所有字母转换成小写，默认为false
        this.ignoreCase = settings.getAsBoolean("ignore_case", false);
        //设置为false,如果他是一个停用词，以便搜索时不忽略最后一个term. 默认为true
        this.removeTrailing = settings.getAsBoolean("remove_trailing", true);


        //设置停止词
        //this.stopWords = Analysis.parseStopWords(env, settings, StopAnalyzer.ENGLISH_STOP_WORDS_SET, ignoreCase);

        if (version.onOrAfter(Version.LUCENE_4_4) && settings.get("enable_position_increments") != null) {
            throw new IllegalArgumentException("enable_position_increments is not supported anymore as of Lucene 4.4 as it can create broken token streams."
                    + " Please fix your analysis chain or use an older compatibility version (<= 4.3).");
        }
        //指定查询结果中的位置增量是否打开，默认true
        this.enablePositionIncrements = settings.getAsBoolean("enable_position_increments", true);

        //创建调度线程词
        pool = Executors.newScheduledThreadPool(1);

        if (this.location == null) {
            return;
            //throw new IllegalArgumentException("dynamic requires `stopword_path` to be configured");
        }

        StopwordFile stopwordFile;
        if (location.startsWith("http://")) {
            stopwordFile = new RemoteStopwordFile(env, location);
        } else {
            stopwordFile = new LocalStopwordFile(env, location);
        }

        //停用词
        stopWords = stopwordFile.reloadStopwordSet();
        //每一分钟调用一次
        scheduledFuture = pool.scheduleAtFixedRate(new Monitor(stopwordFile), interval, interval, TimeUnit.SECONDS);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        if (removeTrailing) {
            if (version.onOrAfter(Version.LUCENE_4_4)) {
                DynamicStopwordFilter dynamicStopwordFilter = new DynamicStopwordFilter(tokenStream, stopWords);
                dynamicStopwordFilters.put(dynamicStopwordFilter, 1);
                return dynamicStopwordFilter;
            } else {
                DynamicLucene43StopFilter dynamicLucene43StopFilter = new DynamicLucene43StopFilter(enablePositionIncrements, tokenStream, stopWords);
                dynamicStopwordFilters.put(dynamicLucene43StopFilter, 1);
                return dynamicLucene43StopFilter;
            }
        } else {
            DynamicSuggestStopFilter dynamicSuggestStopFilter = new DynamicSuggestStopFilter(tokenStream, stopWords);
            dynamicStopwordFilters.put(dynamicSuggestStopFilter, 1);
            return dynamicSuggestStopFilter;
        }
    }

    public Set<?> stopWords() {
        return stopWords;
    }

    public boolean ignoreCase() {
        return ignoreCase;
    }

    //停止词监控
    public class Monitor implements Runnable {

        private StopwordFile stopwordFile;

        public Monitor(StopwordFile stopwordFile) {
            this.stopwordFile = stopwordFile;
        }

        @Override
        public void run() {
            if (stopwordFile.isNeedReloadStopwordSet()) {
                stopWords = stopwordFile.reloadStopwordSet();
                for (DynamicFilter dynamicSynonymFilter : dynamicStopwordFilters.keySet()) {
                    dynamicSynonymFilter.update(stopWords);
                    logger.info("{} success reload stopword", indexName);
                }
            }
        }
    }

}
