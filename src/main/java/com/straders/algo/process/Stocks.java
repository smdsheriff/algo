package com.straders.algo.process;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.DailyCalculationModel;
import com.straders.service.algobase.db.model.ExitOrderModel;
import com.straders.service.algobase.db.model.PlaceOrderModel;
import com.straders.service.algobase.db.model.TrendModel;
import com.straders.service.algobase.db.service.AlgoService;

public class Stocks extends NSE {

	protected URL index;

	protected String stock;

	public Stocks() {
	}

	public Stocks(URL indexName, String symbol) {
		stock = symbol;
		index = indexName;
	}

	@SuppressWarnings("static-access")
	public Map<String, Object> stockData(IndexEntity indexEntity) {
		try {
			IndexEntity entity = getIndexEntity(index);
			if (checkNonNull(entity)) {
				indexEntity.setIndexDataMap(entity.getIndexDataMap());
				return getStockData(entity.getStockDataList(), stock);
			} else {
				System.out.println("Thread Sleep for No data");
				Thread.currentThread().sleep(10000);
				return stockData(indexEntity);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return Collections.emptyMap();
		}
	}

	public Map<String, Object> getStockData(IndexEntity indexEntity, String stock) {
		return getStockData(indexEntity.getStockDataList(), stock);
	}

	public boolean canSelect(AlgoService service) {
		Boolean activeTrade = true;
		List<DailyCalculationModel> todayReturns = service.getDailyService().getTodayCalc();
		List<PlaceOrderModel> ordersList = service.getOrderService().getTodayOrder(getToday());
		List<ExitOrderModel> exitList = service.getExitService().getTodayOrder(getToday());
		List<String> stockList = exitList.stream().map(ExitOrderModel::getSymbol).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(stockList)) {
			List<String> exitStockList = ordersList.stream()
					.filter(exitStock -> !(stockList.contains(exitStock.getSymbol()))).map(PlaceOrderModel::getSymbol)
					.collect(Collectors.toList());
			System.out.println("Active Trade" + exitStockList.size());
			activeTrade = exitStockList.size() < 4;
		}
		if (CollectionUtils.isEmpty(todayReturns)) {
			return true;
		} else {
			double returns = todayReturns.stream().mapToDouble(value -> value.getAmount()).sum();
			Boolean reversalTrend = isReversal(service);
			System.out.println("Returns & Order List size & Trend Reversal & Active Trade :" + returns + " & "
					+ ordersList.size() + " & " + reversalTrend + " & " + activeTrade);
			return madeReturns(returns) && reversalTrend && activeTrade;
			// return madeReturns(returns) && ordersList.size() < 30 &&
			// reversalTrend && activeTrade;
		}
	}

	private boolean isReversal(AlgoService service) {
		List<TrendModel> trendList = service.getTrendService().getTodayTrend();
		if (CollectionUtils.isEmpty(trendList)) {
			return true;
		} else {
			System.out.println("Today's Trend Size " + trendList.size());
			return trendList.size() == 1 ? true : false;
		}
	}

}
