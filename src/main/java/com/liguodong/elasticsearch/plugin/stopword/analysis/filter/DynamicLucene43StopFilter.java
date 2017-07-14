package com.liguodong.elasticsearch.plugin.stopword.analysis.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.Lucene43FilteringTokenFilter;

import java.io.IOException;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 14:23
 */
public final class DynamicLucene43StopFilter extends Lucene43FilteringTokenFilter implements DynamicFilter {

    private CharArraySet stopWords;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public DynamicLucene43StopFilter(boolean enablePositionIncrements, TokenStream in, CharArraySet stopWords) {
        super(enablePositionIncrements, in);
        this.stopWords = stopWords;
    }

    /**
     * Returns the next input Token whose term() is not a stop word.
     */
    @Override
    protected boolean accept() throws IOException {
        return !stopWords.contains(termAtt.buffer(), 0, termAtt.length());
    }

    @Override
    public void update(CharArraySet stopWords) {
        this.stopWords = stopWords;
    }
}

