# dynamic stopword Elasticsearch（动态同义词插件）

动态同义词插件增加了一个token过滤器定时（默认60s）重新加载停止词文件（本地文件或远程文件）。

### 安装

1. `mvn package`

2. copy and unzip `target/releases/elasticsearch-dynamic-stopword-{version}.zip to your-es-root/plugins/dynamic-stopword`

### 例子

**本地文件**（相对于es的config目录）
```
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
```

**远程文件**
```
curl -XPUT "http://10.16.70.68:9200/question_answer_info?pretty" -d '
{
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
       "analysis": {
           "filter": {
               "my_stop": {
                   "type": "dynamic_stopword",
                   "stopwords_path": "http://localhost:9090/my_dict.dic"
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
    }
}'
```
**说明**
* `synonyms_path` 是必须要配置的，根据它的值是否是以 http:// 开头来判断是本地文件，还是远程文件。
* `interval` 非必须配置的，默认值是60，单位秒，表示间隔多少秒去检查停止词文件是否有更新。
* `ignore_case` 非必须配置的， 默认值是false。

**测试**
```
http://10.16.70.68:9200/question_answer_info/_analyze?text=太阳出来了&analyzer=my_ana_stop
```


### 热更新停止词说明
* 对于本地文件：主要通过文件的修改时间戳(Modify time)来判断是否要重新加载。
* 对于远程文件：stopwords_path 是指一个url。 这个http请求需要返回两个头部，一个是 Last-Modified，一个是 ETag，只要有一个发生变化，该插件就会去获取新的停止词来更新相应的停止词。

>注意： 不管是本地文件，还是远程文件，编码都要求是UTF-8的文本文件
