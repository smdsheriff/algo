package com.straders.algo.process.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.straders.algo.entity.Candle;
import com.straders.algo.entity.IndexEntity;
import com.straders.algo.entity.Range;
import com.straders.algo.enumurated.Strategy;
import com.straders.algo.enumurated.Trend;
import com.straders.algo.process.Stocks;
import com.straders.algo.process.confirm.ConfirmStock;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.keys.ConfirmOrderKey;
import com.straders.service.algobase.db.keys.PlaceOrderKey;
import com.straders.service.algobase.db.model.ConfirmOrderModel;
import com.straders.service.algobase.db.model.FultaModel;
import com.straders.service.algobase.db.model.HistoryDataModel;
import com.straders.service.algobase.db.model.RangeBoundModel;
import com.straders.service.algobase.db.model.TrendModel;
import com.straders.service.algobase.db.service.AlgoService;

public class SelectStock extends Thread {

	protected Stocks se = new Stocks();

	protected URL index;

	protected AlgoService service;

	protected Range range;

	protected Strategy strategy;

	public SelectStock(URL indexName, AlgoService service, Range range, Strategy strategy) {
		this.index = indexName;
		this.service = service;
		this.range = range;
		this.strategy = strategy;
	}

	@Override
	public void run() {
		selectStock();
	}

	@SuppressWarnings("static-access")
	private void selectStock() {
		try {
			List<Map<String, Object>> topStockList = new ArrayList<>();
			IndexEntity indexEntity = se.getIndexEntity(index);
			if (se.checkNonNull(indexEntity)) {
				switch (strategy) {
				case BOUNDARY:
					topStockList.addAll(indexEntity.getStockDataList());
					processBoundaryStocks(indexEntity, topStockList);
					break;
				case FULTA:
					topStockList.addAll(indexEntity.getStockDataList());
					processFultaStocks(indexEntity, topStockList);
					break;
				case INDEX:
					topStockList.add(indexEntity.getIndexDataMap());
					processBoundaryStocks(indexEntity, topStockList);
					break;
				case OPEN:
					topStockList.addAll(indexEntity.getStockDataList());
					processOpenStocks(indexEntity, topStockList);
					break;
				case PERCENTAGE:
					topStockList.addAll(getFilteredTopStocks(indexEntity));
					processPercentageStocks(indexEntity, topStockList);
					break;
				case DAMAKKA:
					topStockList.addAll(getFilteredTopStocks(indexEntity));
					processDamakaStocks(indexEntity, topStockList);
					break;
				default:
					break;
				}
			} else {
				System.out.println("Thread Sleep for 15 Seconds");
				Thread.currentThread().sleep(15000);
				selectStock();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			Thread.currentThread().interrupt();
		}
	}

	private void processPercentageStocks(IndexEntity indexEntity, List<Map<String, Object>> topStockList) {
		if (se.canSelect(service)) {
			for (int i = 0; i < topStockList.size(); i++) {
				RangeBoundModel rangeBoundModel = new RangeBoundModel();
				Map<String, Object> stockMap = topStockList.get(i);
				String symbol = se.getSymbol(stockMap);
				Candle candle = se.makeCandle(stockMap);
				rangeBoundModel.setSymbol(symbol);
				rangeBoundModel.setHigh(candle.getHigh());
				rangeBoundModel.setLow(candle.getLow());
				Double open = se.getOpenPrice(stockMap);
				Double perviousClose = se.getPreviousClose(stockMap);
				if (open >= perviousClose) {
					if (checkAlreadyExecuted(symbol)) {
						System.out.println("Confirm Order for Quick Percentage Stock " + symbol);
						proceedConfirmation(indexEntity, addSymbol(symbol, Trend.POSITIVE));
					}
				} else if (open <= perviousClose) {
					if (checkAlreadyExecuted(symbol)) {
						System.out.println("Confirm Order for Quick Percentage Stock " + symbol);
						proceedConfirmation(indexEntity, addSymbol(symbol, Trend.NEGATIVE));
					}
				}
			}
		} else {
			System.out.println("Today's Trade completed Successfully");
		}
	}

	private void processOpenStocks(IndexEntity indexEntity, List<Map<String, Object>> stockList) {
		if (se.canSelect(service)) {
			List<TrendModel> todaysTrend = service.getTrendService().getTodayTrend();
			String trend = todaysTrend.stream().findAny().get().getTrend();
			boolean positiveTrend = se.isPositiveTrend(Trend.valueOf(trend));
			System.out.println("Trend for Open Strategy" + positiveTrend);
			for (int i = 0; i < stockList.size(); i++) {
				Map<String, Object> stockMap = stockList.get(i);
				String symbol = se.getSymbol(stockMap);
				Candle candle = se.makeCandle(stockMap);
				Double ltp = se.getLTP(stockMap);
				Double open = se.getOpenPrice(stockMap);
				Double maxRange = open + ((open * 0.005) / 100);
				Double minRange = open - ((open * 0.005) / 100);
				if ((open == candle.getHigh() || (maxRange >= candle.getHigh() && minRange <= candle.getHigh()))
						&& ltp < open && !positiveTrend) {
					if (checkAlreadyExecuted(symbol)) {
						System.out.println("Confirm Order for Quick Open Stock " + symbol);
						indexEntity.setTrend(false);
						proceedConfirmation(indexEntity, addSymbol(symbol, Trend.NEGATIVE));
					}
				} else if ((open == candle.getLow() || (maxRange >= candle.getLow() && minRange <= candle.getLow()))
						&& ltp > open && positiveTrend) {
					if (checkAlreadyExecuted(symbol)) {
						System.out.println("Confirm Order for Quick Open Stock " + symbol);
						indexEntity.setTrend(true);
						proceedConfirmation(indexEntity, addSymbol(symbol, Trend.POSITIVE));
					}
				}
			}
		} else {
			System.out.println("Today's Trade completed Successfully");
		}
	}

	private void processBoundaryStocks(IndexEntity indexEntity, List<Map<String, Object>> topStockList) {
		if (se.canSelect(service)) {
			List<RangeBoundModel> rangeBoundList = service.getRangeService().getTodayRange(se.getToday());
			if (CollectionUtils.isNotEmpty(rangeBoundList)) {
				List<Map<String, Object>> stockList = processBoundarySelection(indexEntity, rangeBoundList,
						topStockList);
				proceedConfirmation(indexEntity, stockList);
			} else {
				System.out.println("Boundary Data not yet recovered");
			}
		} else {
			System.out.println("Today's Trade completed Successfully");
		}
	}

	private void processDamakaStocks(IndexEntity indexEntity, List<Map<String, Object>> topStockList) {
		if (se.canSelect(service)) {
			List<RangeBoundModel> rangeBoundList = service.getRangeService().getTodayRange(se.getToday());
			if (CollectionUtils.isNotEmpty(rangeBoundList)) {
				List<Map<String, Object>> stockList = processDamakaSelection(indexEntity, rangeBoundList, topStockList);
				proceedConfirmation(indexEntity, stockList);
			} else {
				System.out.println("Damaka Data not yet recovered");
			}
		} else {
			System.out.println("Today's Trade completed Successfully");
		}
	}

	private List<Map<String, Object>> processDamakaSelection(IndexEntity indexEntity,
			List<RangeBoundModel> rangeBoundList, List<Map<String, Object>> topStockList) {
		List<Map<String, Object>> stockList = new ArrayList<>();
		for (int i = 0; i < rangeBoundList.size(); i++) {
			RangeBoundModel rangeBoundStock = rangeBoundList.get(i);
			String symbol = String.valueOf(rangeBoundStock.getSymbol());
			Double ltp = se.getStockLTP(topStockList, symbol);
			// Boolean rangeCondition = ((rangeBoundStock.getRange() / ltp) *
			// 100) < 2.0;
			Trend trend = se.getTrend(rangeBoundStock, ltp);
			if (ltp != 0) {
				Boolean canProceed = se.filterRangeSelection(indexEntity.getTrend(), trend, rangeBoundList.get(i), ltp);
				if (canProceed && checkAlreadyExecuted(symbol)) {
					System.out.println("+++ Damaka Condition satisfied for " + symbol + "+++");
					Map<String, Object> stockMap = new HashMap<>();
					stockMap.put("symbol", symbol);
					stockMap.put("trend", trend);
					stockList.add(stockMap);
				} else {
					// System.out.println("Range Bound Condition not satisfied
					// for " + symbol);
				}
			}
		}
		return stockList;
	}

	private List<Map<String, Object>> processBoundarySelection(IndexEntity indexEntity,
			List<RangeBoundModel> rangeBoundList, List<Map<String, Object>> topStockList) {
		List<Map<String, Object>> stockList = new ArrayList<>();
		for (int i = 0; i < rangeBoundList.size(); i++) {
			RangeBoundModel rangeBoundStock = rangeBoundList.get(i);
			String symbol = String.valueOf(rangeBoundStock.getSymbol());
			Double ltp = se.getStockLTP(topStockList, symbol);
			Trend trend = se.getTrend(rangeBoundStock, ltp);
			if (ltp != 0) {
				Boolean canProceed = se.filterRangeSelection(indexEntity.getTrend(), trend, rangeBoundList.get(i), ltp);
				Boolean range = ((rangeBoundStock.getRange() / ltp) * 100) < 0.85;
				if (canProceed && range && checkAlreadyExecuted(symbol)) {
					System.out.println("+++ Boundary Condition satisfied for " + symbol + "+++");
					Map<String, Object> stockMap = new HashMap<>();
					stockMap.put("symbol", symbol);
					stockMap.put("trend", trend);
					stockList.add(stockMap);
				} else {
					// System.out.println("Range Bound Condition not satisfied
					// for " + symbol);
				}
			}
		}
		return stockList;
	}

	private boolean checkAlreadyExecuted(String symbol) {
		return !checkConfirmedExist(symbol) && !checkAlreadyPlacedOrder(symbol);
	}

	private void proceedConfirmation(IndexEntity indexEntity, List<Map<String, Object>> stockList) {
		if (CollectionUtils.isNotEmpty(stockList)) {
			service.getConfirmService().saveAll(makeConfirmModel(stockList));
			for (int i = 0; i < stockList.size(); i++) {
				String symbol = String.valueOf(se.getSymbol(stockList.get(i)));
				Trend trend = (Trend) stockList.get(i).get("trend");
				new ConfirmStock(index, indexEntity, service, trend, symbol, makeTimeInterval(), strategy).start();
			}
		}
	}

	private boolean checkConfirmedExist(String symbol) {
		ConfirmOrderKey confirmkey = new ConfirmOrderKey();
		confirmkey.symbol = symbol;
		confirmkey.date = se.getToday();
		return service.getConfirmService().existById(confirmkey);
	}

	private List<Map<String, Object>> getFilteredTopStocks(IndexEntity entity) {
		List<Map<String, Object>> getByPercentageList = se.getStockByPercentage(entity, range.getMin(), range.getMax());
		return getByPercentageList;
	}

	private boolean checkAlreadyPlacedOrder(String symbol) {
		PlaceOrderKey keys = new PlaceOrderKey();
		keys.date = se.getToday();
		keys.symbol = symbol;
		return service.getOrderService().existsById(keys);
	}

	public List<ConfirmOrderModel> makeConfirmModel(List<Map<String, Object>> stockList) {
		return stockList.stream().map(action -> {
			ConfirmOrderModel confirmModel = new ConfirmOrderModel();
			confirmModel.setDate(se.getToday());
			confirmModel.setSymbol(se.getSymbol(action));
			confirmModel.setTime(se.getTime());
			confirmModel.setStrategy(strategy.name());
			return confirmModel;
		}).collect(Collectors.toList());
	}

	public Integer makeTimeInterval() {
		Integer interval = 8;
		switch (strategy) {
		case DAMAKKA:
			interval = 8;
			break;
		case BOUNDARY:
			interval = 8;
			break;
		case FULTA:
			interval = 6;
			break;
		case INDEX:
			interval = 4;
			break;
		case OPEN:
			interval = 4;
			break;
		case PERCENTAGE:
			interval = 6;
			break;
		default:
			break;
		}
		return interval;
	}

	private void processFultaStocks(IndexEntity indexEntity, List<Map<String, Object>> stockList) {
		if (se.canSelect(service)) {
			List<FultaModel> fultaList = service.getFultaService().getToday(se.getToday());
			if (CollectionUtils.isNotEmpty(fultaList)) {
				fultaList.stream().forEach(stock -> {
					String symbol = stock.getSymbol();
					Map<String, Object> stockMap = se.getStockData(stockList, symbol);
					Double ltp = se.getLTP(stockMap);
					Double open = se.getOpenPrice(stockMap);
					Double high = se.getDayHigh(stockMap);
					Double low = se.getDayLow(stockMap);
					if (ltp < stock.getHigh() && open != low && ltp > se.rangeTarget(stock)) {
						if (checkAlreadyExecuted(symbol)) {
							Optional<HistoryDataModel> previousDayData = service.getHistory().getPreviousData(symbol);
							if (previousDayData.isPresent()) {
								Double previousDataHigh = previousDayData.get().getHigh();
								if (open >= previousDataHigh) {
									indexEntity.setTrend(false);
									System.out.println("Confirm Order for Fulta Stock " + symbol);
									proceedConfirmation(indexEntity, addSymbol(symbol, Trend.NEGATIVE));
								}
							}
						}
					} else if (ltp > stock.getClose() && open != high && ltp < se.rangeTarget(stock)) {
						if (checkAlreadyExecuted(symbol)) {
							Optional<HistoryDataModel> previousDayData = service.getHistory().getPreviousData(symbol);
							if (previousDayData.isPresent()) {
								Double previousDataLow = previousDayData.get().getLow();
								if (open <= previousDataLow) {
									indexEntity.setTrend(true);
									System.out.println("Confirm Order for Fulta Stock " + symbol);
									proceedConfirmation(indexEntity, addSymbol(symbol, Trend.POSITIVE));
								}
							}
						}
					}
				});
			} else {
				System.out.println("Fulta Data is not available");
			}
		} else {
			System.out.println("Done for the day");
		}
	}

	private List<Map<String, Object>> addSymbol(String symbol, Trend trend) {
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> map = new HashMap<>();
		map.put("symbol", symbol);
		map.put("trend", trend);
		list.add(map);
		return list;
	}

}
