package com.straders.algo.process.trend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.enumurated.Trend;
import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.IndexDataModel;
import com.straders.service.algobase.db.model.StockDataModel;
import com.straders.service.algobase.db.model.TrendModel;
import com.straders.service.algobase.db.service.AlgoService;

public class IndexData extends Stocks {

	protected AlgoService service;

	protected URL url;

	public IndexData(AlgoService service, URL url) {
		super();
		this.service = service;
		this.url = url;
	}

	public IndexData() {
	}

	@SuppressWarnings({ "static-access", "unused" })
	public void indexData() {
		try {
			List<Map<String, Object>> topStockList = new ArrayList<>();
			IndexEntity indexEntity = getIndexEntity(url);
			if (checkNonNull(indexEntity)) {
				updateIndexData(indexEntity);
			} else {
				System.out.println("Thread Sleep for 45 Seconds");
				Thread.currentThread().sleep(45000);
				indexData();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private void updateIndexData(IndexEntity indexEntity) {
		Map<String, Object> indexDataMap = indexEntity.getIndexDataMap();
		IndexDataModel indexData = new IndexDataModel();
		indexData.setDate(getToday());
		indexData.setIndex(url.getName());
		indexData.setTime(getTime());
		indexData.setPrice(getLTP(indexDataMap));
		service.getIndexService().save(indexData);
//		saveStockData(indexEntity);
	}

	public Boolean checkIndexTrend(IndexEntity indexEntity) {
		Double ltp = getLTP(indexEntity.getIndexDataMap());
		List<IndexDataModel> indexDataList = service.getIndexService().getLatestData(getToday());
		System.out.println("Data List" + indexDataList);
		if (CollectionUtils.isEmpty(indexDataList) || indexDataList.size() < 2) {
			return true;
		} else if (indexDataList.size() == 2) {
			double indexPricePresent = indexDataList.get(0).getPrice();
			double indexPricePast = indexDataList.get(1).getPrice();
			Trend trend = indexPricePast > indexPricePresent ? Trend.NEGATIVE : Trend.POSITIVE;
			List<TrendModel> todaysTrend = service.getTrendService().getTodayTrend();
			if (CollectionUtils.isEmpty(todaysTrend)) {
				return true;
			} else if (todaysTrend.size() == 1) {
				Trend todayTrend = Trend.valueOf(todaysTrend.stream().findFirst().get().getTrend());
				switch (todayTrend) {
				case NEGATIVE:
					return ltp < indexPricePresent && isNegativeTrend(trend);
				case POSITIVE:
					return ltp > indexPricePresent && isPositiveTrend(trend);
				default:
					break;
				}
				return false;
			} else {
				System.out.println("Trend Reversal may occured");
				return false;
			}
		}
		System.out.println("Default Case +++");
		return true;
	}

	private void saveStockData(IndexEntity indexEntity) {
		List<StockDataModel> stockList = makeStockDataList(indexEntity.getStockDataList());
		if (!CollectionUtils.isEmpty(stockList)) {
			service.getStockService().saveAll(stockList);
		}
	}

	private List<StockDataModel> makeStockDataList(List<Map<String, Object>> stockDataList) {
		return stockDataList.stream().map(action -> {
			StockDataModel stockModel = new StockDataModel();
			stockModel.setDate(getToday());
			stockModel.setTime(getTime());
			stockModel.setSymbol(getSymbol(action));
			stockModel.setPrice(getLTP(action));
			stockModel.setVolume(makeDouble(getVolume(action)));
			return stockModel;
		}).collect(Collectors.toList());
	}

}
