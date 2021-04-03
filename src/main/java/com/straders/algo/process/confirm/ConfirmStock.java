package com.straders.algo.process.confirm;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.MapUtils;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.enumurated.Strategy;
import com.straders.algo.enumurated.Trend;
import com.straders.algo.process.Stocks;
import com.straders.algo.process.order.OrderStock;
import com.straders.algo.process.trend.IndexData;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.keys.ConfirmOrderKey;
import com.straders.service.algobase.db.keys.FultaKey;
import com.straders.service.algobase.db.keys.PlaceOrderKey;
import com.straders.service.algobase.db.keys.RangeBoundKey;
import com.straders.service.algobase.db.model.FultaModel;
import com.straders.service.algobase.db.model.RangeBoundModel;
import com.straders.service.algobase.db.service.AlgoService;

public class ConfirmStock extends Thread {

	protected Stocks se;

	protected String symbol;

	protected URL url;

	protected Integer endTime;

	protected Integer plusTime;

	protected Trend trend;

	protected AlgoService service;

	protected IndexEntity indexEntity;

	protected Strategy strategy;

	public ConfirmStock(URL index, IndexEntity indexEntity, AlgoService service, Trend trend, String symbol,
			Integer confirmTime, Strategy strategy) {
		this.url = index;
		this.indexEntity = indexEntity;
		this.service = service;
		this.trend = trend;
		this.symbol = symbol;
		this.plusTime = confirmTime;
		this.se = new Stocks(index, symbol);
		this.strategy = strategy;
	}

	@Override
	public void run() {
		currentThread().setName(symbol + " : " + plusTime);
		endTime = new Timestamp(new Date().getTime()).toLocalDateTime().plusMinutes(plusTime).getMinute();
		confirmStock();
	}

	@SuppressWarnings("static-access")
	private void confirmStock() {
		try {
			System.out.println("End Time for  " + symbol + " is " + endTime + " : " + strategy.name());
			if (endTime == (new Timestamp(new Date().getTime()).toLocalDateTime().getMinute())) {
				if (se.canSelect(service)) {
					System.out.println("Process Completed for " + symbol);
					processConfirmation();
				} else {
					deleteConfirmation(symbol);
					System.out.println("Done for the day ..!! No order placed after Confirmation");
				}
			} else {
				if (checkAlreadyPlacedOrder() && !checkConfirmProgress()) {
					System.out.println("Order Already Placed for in  " + strategy + " for " + symbol);
				} else {
					System.out.println("Yet to Complete in " + strategy + " for " + symbol);
					Thread.currentThread().sleep(120000);
					confirmStock();
				}
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			Thread.currentThread().interrupt();
		}
	}

	private void processConfirmation() {
		RangeBoundModel rangeBoundModel = new RangeBoundModel();
		Map<String, Object> stockDataMap = new HashMap<>();
		RangeBoundKey key = new RangeBoundKey();
		key.date = se.getToday();
		switch (strategy) {
		case DAMAKKA:
			key.symbol = symbol;
			rangeBoundModel = service.getRangeService().findById(key).get();
			stockDataMap = se.stockData(indexEntity);
			orderDamakkaStock(rangeBoundModel, key, stockDataMap);
			break;
		case BOUNDARY:
			key.symbol = symbol;
			rangeBoundModel = service.getRangeService().findById(key).get();
			stockDataMap = se.stockData(indexEntity);
			orderBoundaryStock(rangeBoundModel, key, stockDataMap);
			break;
		case FULTA:
			setTrend();
			stockDataMap = se.stockData(indexEntity);
			orderFultaStock(stockDataMap);
			break;
		case INDEX:
			key.symbol = url.getName();
			IndexEntity entity = se.getIndexEntity(url);
			rangeBoundModel = service.getRangeService().findById(key).get();
			stockDataMap = entity.getIndexDataMap();
			orderBoundaryStock(rangeBoundModel, key, stockDataMap);
			break;
		case OPEN:
			setTrend();
			stockDataMap = se.stockData(indexEntity);
			orderOpenStock(stockDataMap);
			break;
		case PERCENTAGE:
			setTrend();
			stockDataMap = se.stockData(indexEntity);
			orderPercentageStock(stockDataMap);
			break;
		default:
			break;
		}
	}

	private void orderOpenStock(Map<String, Object> stockDataMap) {
		Double ltp = se.getLTP(stockDataMap);
		if (checkOpenRange(stockDataMap, ltp) && confirmTrend()) {
			System.out.println("Order placed for Open " + symbol);
			new OrderStock(service, symbol, indexEntity, trend, ltp, null, strategy).orderStock();
			deleteConfirmation(symbol);
		} else {
			deleteConfirmation(symbol);
			System.out.println("Open condition not satisfied to order" + symbol);
		}
	}

	private boolean checkOpenRange(Map<String, Object> stockDataMap, Double ltp) {
		Double open = se.getOpenPrice(stockDataMap);
		return indexEntity.getTrend() ? ltp > open : ltp < open;
	}

	private void orderPercentageStock(Map<String, Object> stockDataMap) {
		Double ltp = se.getLTP(stockDataMap);
		if (checkPercentageRange(stockDataMap) && confirmTrend()) {
			System.out.println("Order placed for Percentage " + symbol);
			new OrderStock(service, symbol, indexEntity, trend, ltp, null, strategy).orderStock();
			deleteConfirmation(symbol);
		} else {
			deleteConfirmation(symbol);
			System.out.println("Percentage condition not satisfied to order" + symbol);
		}
	}

	private boolean checkPercentageRange(Map<String, Object> stockDataMap) {
		return indexEntity.getTrend() ? se.getPercentage(stockDataMap) > 3.0 : se.getPercentage(stockDataMap) < -3.0;
	}

	private void setTrend() {
		if (se.isPositiveTrend(trend)) {
			indexEntity.setTrend(true);
		} else if (se.isNegativeTrend(trend)) {
			indexEntity.setTrend(false);
		}
	}

	private void orderFultaStock(Map<String, Object> stockDataMap) {
		Double ltp = se.getLTP(stockDataMap);
		if (checkFultaRange(ltp)) {
			System.out.println("Order placed for Fulta " + symbol);
			new OrderStock(service, symbol, indexEntity, trend, ltp, null, strategy).orderStock();
			deleteConfirmation(symbol);
		} else {
			deleteConfirmation(symbol);
			System.out.println("Fulta condition not satisfied to order" + symbol);
		}
	}

	private boolean checkFultaRange(Double ltp) {
		Boolean canOrder = false;
		FultaKey key = new FultaKey();
		key.date = se.getToday();
		key.symbol = symbol;
		Optional<FultaModel> fultaModel = service.getFultaService().findById(key);
		if (fultaModel.isPresent()) {
			FultaModel stock = fultaModel.get();
			canOrder = indexEntity.getTrend() ? ltp > stock.getClose() && ltp < se.rangeTarget(stock)
					: ltp < stock.getHigh() && ltp > se.rangeTarget(stock);
		}
		return canOrder;
	}

	private void orderDamakkaStock(RangeBoundModel rangeBoundModel, RangeBoundKey key,
			Map<String, Object> stockDataMap) {
		if (MapUtils.isNotEmpty(stockDataMap)) {
			System.out.println("Confimration for " + key.symbol);
			Double ltp = se.getLTP(stockDataMap);
			Boolean placeOrder = damakkaCheck(indexEntity, stockDataMap)
					&& se.filterRangeSelection(indexEntity.getTrend(), trend, rangeBoundModel, ltp);
			if (placeOrder) {
				System.out.println("Order Placeed for Damakka " + symbol);
				new OrderStock(service, symbol, indexEntity, trend, ltp, rangeBoundModel, strategy).orderStock();
				deleteConfirmation(symbol);
			} else {
				deleteConfirmation(symbol);
				System.out.println("Not finalised to Place Order");
			}
		} else {
			System.out.println("Stock Data not available for confirmation");
		}
	}

	private void orderBoundaryStock(RangeBoundModel rangeBoundModel, RangeBoundKey key,
			Map<String, Object> stockDataMap) {
		if (MapUtils.isNotEmpty(stockDataMap)) {
			System.out.println("Confimration for Boundary " + key.symbol);
			Double ltp = se.getLTP(stockDataMap);
			Boolean placeOrder = se.filterRangeSelection(indexEntity.getTrend(), trend, rangeBoundModel, ltp)
					&& confirmTrend();
			if (placeOrder) {
				System.out.println("Order Placeed for Boundary " + symbol);
				new OrderStock(service, symbol, indexEntity, trend, ltp, rangeBoundModel, strategy).orderStock();
				deleteConfirmation(symbol);
			} else {
				deleteConfirmation(symbol);
				System.out.println("Not finalised to Place Order");
			}
		} else {
			System.out.println("Stock Data not available for confirmation");
		}
	}

	private boolean damakkaCheck(IndexEntity indexEntity, Map<String, Object> stockDataMap) {
		Boolean percentageSatisfied = false;
		switch (strategy) {
		case DAMAKKA:
			Double percentage = se.getPercentage(stockDataMap);
			Boolean trendSatisfied = confirmTrend();
			percentageSatisfied = (indexEntity.getTrend() ? percentage > 2 : percentage < -2) && trendSatisfied;
			break;
		default:
			break;
		}
		return percentageSatisfied;
	}

	private boolean checkConfirmProgress() {
		ConfirmOrderKey keys = new ConfirmOrderKey();
		keys.date = se.getToday();
		keys.symbol = symbol;
		return service.getConfirmService().existById(keys);
	}

	private boolean checkAlreadyPlacedOrder() {
		PlaceOrderKey keys = new PlaceOrderKey();
		keys.date = se.getToday();
		keys.symbol = symbol;
		return service.getOrderService().existsById(keys);
	}

	private void deleteConfirmation(String symbol) {
		ConfirmOrderKey confirmKey = new ConfirmOrderKey();
		confirmKey.symbol = symbol;
		confirmKey.date = se.getToday();
		// update the symbol in cache
		service.getConfirmService().delete(confirmKey);
	}

	private Boolean confirmTrend() {
		Boolean sameTrend = new IndexData(service, url).checkIndexTrend(indexEntity);
		System.out.println("Follow's same Trend for " + symbol + " :" + sameTrend);
		return sameTrend;
	}
}
