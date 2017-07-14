package com.liguodong.elasticsearch.plugin.stopword.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.Lucene43StopFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.suggest.analyzing.SuggestStopFilter;
import org.apache.lucene.util.Version;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.elasticsearch.index.analysis.Analysis;
import org.elasticsearch.index.settings.IndexSettingsService;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;


public class DynamicStopTokenFilterFactory extends AbstractTokenFilterFactory {

    public static ESLogger logger = Loggers.getLogger("dynamic-stopword");
    private ScheduledExecutorService pool;
    private volatile ScheduledFuture<?> scheduledFuture;

    private final CharArraySet stopWords;
    private final boolean ignoreCase;
    private final boolean enablePositionIncrements;
    private final boolean removeTrailing;

    @Inject
    public DynamicStopTokenFilterFactory(Index index,
                                         IndexSettingsService indexSettingsService,
                                         Environment env,
                                         @Assisted String name,
                                         @Assisted Settings settings) {
        super(index, indexSettingsService.getSettings(), name, settings);
        //true将所有字母转换成小写，默认为false
        this.ignoreCase = settings.getAsBoolean("ignore_case", false);
        //设置为false,如果他是一个停用词，以便搜索时不忽略最后一个term. 默认为true
        this.removeTrailing = settings.getAsBoolean("remove_trailing", true);

        //设置停止词
        this.stopWords = Analysis.parseStopWords(env, settings, StopAnalyzer.ENGLISH_STOP_WORDS_SET, ignoreCase);

        if (version.onOrAfter(Version.LUCENE_4_4) && settings.get("enable_position_increments") != null) {
            throw new IllegalArgumentException("enable_position_increments is not supported anymore as of Lucene 4.4 as it can create broken token streams."
                    + " Please fix your analysis chain or use an older compatibility version (<= 4.3).");
        }
        //指定查询结果中的位置增量是否打开，默认true
        this.enablePositionIncrements = settings.getAsBoolean("enable_position_increments", true);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        if (removeTrailing) {
            if (version.onOrAfter(Version.LUCENE_4_4)) {
                return new DynamicStopwordFilter(tokenStream, stopWords);
            } else {
                return new Lucene43StopFilter(enablePositionIncrements, tokenStream, stopWords);
            }
        } else {
            return new SuggestStopFilter(tokenStream, stopWords);
        }
    }

    public Set<?> stopWords() {
        return stopWords;
    }

    public boolean ignoreCase() {
        return ignoreCase;
    }

}
