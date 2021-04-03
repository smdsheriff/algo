package com.straders.algo.process;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.url.URL;
import com.straders.algo.utils.AlgoUtils;

public class NSE extends AlgoUtils {

	private static NSE instance = null;

	public static NSE instance() {
		return instance == null ? new NSE() : instance;
	}

	public IndexEntity getIndexEntity(URL url) {
		return makeIndexEntity(url);
	}

	@SuppressWarnings("unchecked")
	private IndexEntity processIndexData(String apiData) {
		IndexEntity niftyEntity = new IndexEntity();
		Map<String, Object> trendData = (Map<String, Object>) getMapFromJson(apiData, "advance");
		List<Map<String, Object>> valueMap = (List<Map<String, Object>>) getMapFromJson(apiData, "data");
		Map<Object, List<Map<String, Object>>> groupByPriority = valueMap.stream()
				.collect(Collectors.groupingBy(jsonData -> String.valueOf(jsonData.get("priority"))));
		List<Map<String, Object>> stockList = groupByPriority.get("0");
		niftyEntity.setIndexDataMap(getDataMap(groupByPriority.get("1")));
		niftyEntity.setTrend(getTrendData(trendData));
		filterStocks(stockList);
		niftyEntity.setStockDataList(stockList);
		niftyEntity.setTime(getTimeFromJson(apiData));
		System.out.println("Time : " + niftyEntity.getTime());
		System.out.println("Trend : " + (niftyEntity.getTrend() ? "Positive" : "Negative"));
		return niftyEntity;
	}

	private IndexEntity makeIndexEntity(URL url) {
		String apiData = getMethod(url.getUrl());
		if (StringUtils.isNotEmpty(apiData) && checkNonNull(apiData)) {
			return processIndexData(apiData);
		} else {
			System.out.println("No data found for " + url);
			return null;
		}
	}

}
