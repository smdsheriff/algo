package com.straders.algo.process.trend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.enumurated.Trend;
import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.TrendModel;
import com.straders.service.algobase.db.service.AlgoService;

public class IndexTrend extends Stocks {

	protected AlgoService service;

	protected URL url;

	public IndexTrend(AlgoService service, URL url) {
		super();
		this.service = service;
		this.url = url;
	}

	public IndexTrend() {
	}

	@SuppressWarnings({ "static-access", "unused" })
	public void updateTrend() {
		try {
			List<Map<String, Object>> topStockList = new ArrayList<>();
			IndexEntity indexEntity = getIndexEntity(url);
			if (checkNonNull(indexEntity)) {
				updateTrend(indexEntity);
			} else {
				System.out.println("Thread Sleep for 15 Seconds");
				Thread.currentThread().sleep(15000);
				updateTrend();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public void updateTrend(IndexEntity indexEntity) {
		Trend trend = indexEntity.getTrend() ? Trend.POSITIVE : Trend.NEGATIVE;
		TrendModel model = new TrendModel();
		model.setDate(getToday());
		model.setTrend(trend.name());
		model.setHigh(getDayHigh(indexEntity.getIndexDataMap()));
		model.setLow(getDayLow(indexEntity.getIndexDataMap()));
		service.getTrendService().save(model);
	}

}
