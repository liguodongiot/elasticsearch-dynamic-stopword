
`org.elasticsearch.index.analysis.AnalysisModule`

```
http://10.16.70.68:9200/

curl -XDELETE "http://10.16.70.68:9200/question_answer_info?pretty"
curl -XPUT "http://10.16.70.68:9200/question_answer_info?pretty" -d '
{
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
       "analysis": {
           "filter": {
               "my_stop": {
                   "type": "dynamic_stopword",
                   "stopwords_path": "stop_word.dic"
               }
           },
           "analyzer": {
               "my_ana_stop": {
                   "tokenizer": "ik_max_word",
                   "filter": [
                       "my_stop"
                   ]
               }
           }
       }
    },
    "mappings": {
        "correct_answer_info": {
            "dynamic": "false",
            "properties": {
                "id": {
                    "type": "long"
                },
                "ask": {
                    "type": "string",
                    "analyzer": "my_ana_stop",
                    "search_analyzer": "my_ana_stop"
                },
                "answer": {
                    "type": "string",
                    "analyzer": "my_ana_stop",
                    "search_analyzer": "my_ana_stop"
                }
            }
        }
    }
}'

http://10.16.70.68:9200/question_answer_info/_analyze?text=中华人民共和国&tokenizer=ik_max_word

http://10.16.70.68:9200/question_answer_info/_analyze?text=%E5%A4%A9%E6%B0%94%E5%A5%BD%E7%9A%84%E5%91%80&analyzer=my_ana_stop
```

