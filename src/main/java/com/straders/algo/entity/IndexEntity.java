package com.straders.algo.entity;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class IndexEntity {

	Map<String, Object> indexDataMap;

	List<Map<String, Object>> stockDataList;

	Object time;

	Boolean trend;
}
