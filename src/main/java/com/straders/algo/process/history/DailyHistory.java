package com.straders.algo.process.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.HistoryDataModel;
import com.straders.service.algobase.db.service.AlgoService;

public class DailyHistory extends Stocks {

	protected AlgoService service;

	protected URL url;

	public DailyHistory(AlgoService service, URL url) {
		super();
		this.service = service;
		this.url = url;
	}

	@SuppressWarnings({ "static-access", "unused" })
	public void updateHistory() {
		try {
			List<Map<String, Object>> topStockList = new ArrayList<>();
			IndexEntity indexEntity = getIndexEntity(url);
			if (checkNonNull(indexEntity)) {
				updateDailyStockData(indexEntity.getStockDataList());
			} else {
				System.out.println("Thread Sleep for 15 Seconds");
				Thread.currentThread().sleep(15000);
				updateHistory();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private void updateDailyStockData(List<Map<String, Object>> stockDataList) {
		List<HistoryDataModel> historyList = stockDataList.stream().map(stock -> {
			HistoryDataModel historyModel = new HistoryDataModel();
			historyModel.setDate(getToday());
			historyModel.setSymbol(getSymbol(stock));
			historyModel.setOpen(getOpenPrice(stock));
			historyModel.setHigh(getDayHigh(stock));
			historyModel.setLow(getDayLow(stock));
			historyModel.setClose(getLTP(stock));
			historyModel.setPercentage(getPercentage(stock));
			historyModel.setVolume(makeInteger(stock.get("totalTradedVolume")));
			return historyModel;
		}).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(historyList)) {
			service.getHistory().saveAll(historyList);
		}
	}

}
