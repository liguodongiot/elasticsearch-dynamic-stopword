# dynamic stopword Elasticsearch（动态同义词插件）

动态同义词插件增加了一个token过滤器定时（默认60s）重新加载数据库数据。

### 安装

1. `mvn package`

2. copy and unzip `target/releases/elasticsearch-dynamic-stopword-{version}.zip to your-es-root/plugins/dynamic-stopword`

### 例子
**数据库表** 
```
CREATE TABLE `stop_word_dic` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `stop_word` varchar(255) DEFAULT '' COMMENT '停止词',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0：未删除，有效数据  1：删除',
  PRIMARY KEY (`id`),
  KEY `idx_stop_word` (`stop_word`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1599 DEFAULT CHARSET=utf8 COMMENT='停止词';
```

**ES索引库创建**
```
curl -XPUT "http://10.16.161.6:9200/question_answer_info?pretty" -d '
{
    "settings": {
        "number_of_shards": 1,
        "number_of_replicas": 0,
       "analysis": {
           "filter": {
               "my_stop": {
                   "type": "dynamic_stopword",
                   "stopword_url": "http://localhost:8100/extStopWordDic/updateStopWord"
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
* `stopword_url` 是必须要配置的，它的值是以http://开头。
* `interval` 非必须配置的，默认值是60，单位秒，表示间隔多少秒去检查停止词表是否有更新。
* `ignore_case` 非必须配置的， 默认值是false。

**测试**
```
http://10.16.161.6:9200/question_answer_info/_analyze?text=太阳出来了&analyzer=my_ana_stop

INSERT into stop_word_dic(stop_word,create_time,update_time) VALUES("天气",NOW(),NOW());
UPDATE top_word_dic SET update_time=NOW(),is_deleted=1 where stop_word='天气';
```

