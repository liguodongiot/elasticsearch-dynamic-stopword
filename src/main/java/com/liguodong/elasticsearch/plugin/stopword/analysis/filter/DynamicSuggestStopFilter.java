package com.liguodong.elasticsearch.plugin.stopword.analysis.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;

import java.io.IOException;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/14 14:27
 */

public final class DynamicSuggestStopFilter extends TokenFilter implements DynamicFilter{

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    private CharArraySet stopWords;

    private State endState;

    /** Sole constructor. */
    public DynamicSuggestStopFilter(TokenStream input, CharArraySet stopWords) {
        super(input);
        this.stopWords = stopWords;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        endState = null;
    }

    @Override
    public void end() throws IOException {
        if (endState == null) {
            super.end();
        } else {
            // NOTE: we already called .end() from our .next() when
            // the stream was complete, so we do not call
            // super.end() here
            restoreState(endState);
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (endState != null) {
            return false;
        }

        if (!input.incrementToken()) {
            return false;
        }

        int skippedPositions = 0;
        while (true) {
            if (stopWords.contains(termAtt.buffer(), 0, termAtt.length())) {
                int posInc = posIncAtt.getPositionIncrement();
                int endOffset = offsetAtt.endOffset();
                // This token may be a stopword, if it's not end:
                State sav = captureState();
                if (input.incrementToken()) {
                    // It was a stopword; skip it
                    skippedPositions += posInc;
                } else {
                    clearAttributes();
                    input.end();
                    endState = captureState();
                    int finalEndOffset = offsetAtt.endOffset();
                    assert finalEndOffset >= endOffset;
                    if (finalEndOffset > endOffset) {
                        // OK there was a token separator after the
                        // stopword, so it was a stopword
                        return false;
                    } else {
                        // No token separator after final token that
                        // looked like a stop-word; don't filter it:
                        restoreState(sav);
                        posIncAtt.setPositionIncrement(skippedPositions + posIncAtt.getPositionIncrement());
                        keywordAtt.setKeyword(true);
                        return true;
                    }
                }
            } else {
                // Not a stopword; return the current token:
                posIncAtt.setPositionIncrement(skippedPositions + posIncAtt.getPositionIncrement());
                return true;
            }
        }
    }

    @Override
    public void update(CharArraySet stopWords) {
        this.stopWords = stopWords;
    }
}
