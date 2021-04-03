package com.straders.algo.strategy.boundary;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.straders.algo.entity.Candle;
import com.straders.algo.entity.IndexEntity;
import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.RangeBoundModel;
import com.straders.service.algobase.db.service.AlgoService;

public class Boundary extends Thread {

	protected Stocks se = new Stocks();

	protected Integer timeFrame;

	protected URL url;

	protected Boolean stockBound;

	protected AlgoService service;

	public Boundary() {
	}

	public Boundary(URL index, AlgoService service, Integer timeBound, Boolean isStock) {
		this.timeFrame = timeBound;
		this.url = index;
		this.service = service;
		this.stockBound = isStock;
	}

	@Override
	public void run() {
		currentThread().setName("Range Bound " + url.name() + " : " + stockBound);
		rangeBound();
	}

	@SuppressWarnings("static-access")
	private void rangeBound() {
		try {
			IndexEntity entity = se.getIndexEntity(url);
			if (se.checkNonNull(entity)) {
				if (isExactTime(entity.getTime())) {
					System.out.println("Got Data for Exact Time for " + url.name());
					if (stockBound) {
						processStock(entity, entity.getStockDataList());
					} else {
						Map<String, Object> stock = entity.getIndexDataMap();
						RangeBoundModel model = makeRangeBoundModel(entity, stock);
						service.getRangeService().save(model);
					}
				} else {
					System.out.println("Thread Sleep for 15 Seconds");
					Thread.currentThread().sleep(15000);
					rangeBound();
				}
			} else {
				System.out.println("Thread Sleep for 15 Seconds");
				Thread.currentThread().sleep(15000);
				rangeBound();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			currentThread().interrupt();
		}
	}

	private RangeBoundModel makeRangeBoundModel(IndexEntity entity, Map<String, Object> stock) {
		RangeBoundModel model = new RangeBoundModel();
		Candle stockCandle = se.makeCandle(stock);
		model.setDate(se.getToday());
		model.setSymbol(se.getSymbol(stock));
		model.setHigh(stockCandle.getHigh());
		model.setLow(stockCandle.getLow());
		model.setRange(se.getRangeBound(stockCandle));
		model.setVolume(se.getVolume(stock));
		return model;
	}

	private void processStock(IndexEntity entity, List<Map<String, Object>> stockList) {
		List<RangeBoundModel> modelList = stockList.stream().map(stock -> {
			return makeRangeBoundModel(entity, stock);
		}).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(modelList)) {
			service.getRangeService().saveAll(modelList);
		}
	}

	private boolean isExactTime(Object time) {
		String[] timeValue = String.valueOf(time).split(":");
		if (timeFrame == 5) {
			return timeValue[1].contains("20") || timeValue[1].contains("21") || timeValue[1].contains("19");
		} else if (timeFrame == 15) {
			return timeValue[1].contains("30") || timeValue[1].contains("31") || timeValue[1].contains("29");
		} else {
			return true;
		}
	}

}
