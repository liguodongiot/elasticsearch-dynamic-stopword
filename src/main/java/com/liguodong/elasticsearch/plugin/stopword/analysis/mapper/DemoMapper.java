package com.liguodong.elasticsearch.plugin.stopword.analysis.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Describe:
 * author: guodong.li
 * datetime: 2017/7/18 20:53
 */
public interface DemoMapper {

    List<Map<String,Object>> getUser(@Param("id") Integer id);

}
