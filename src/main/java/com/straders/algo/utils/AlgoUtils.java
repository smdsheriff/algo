package com.straders.algo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.straders.algo.entity.Candle;
import com.straders.algo.entity.IndexEntity;
import com.straders.algo.enumurated.Trend;
import com.straders.service.algobase.db.model.FultaModel;
import com.straders.service.algobase.db.model.RangeBoundModel;

public class AlgoUtils extends HttpUtils {

	public Integer makeNumber(Object data) {
		return Double.valueOf((String) data).intValue();
	}

	protected Object getTimeFromJson(String jsonData) {
		return getMapFromJson(jsonData, "timestamp");
	}

	public List<Map<String, Object>> getStockByPercentage(IndexEntity entity, int minPerncetage, int maxPerncetage) {
		return sortListbyTrend(entity.getStockDataList().stream()
				.sorted((per1, per2) -> makeInteger(per2.get("pChange")).compareTo(makeInteger(per1.get("pChange"))))
				.filter(filter -> entity.getTrend() ? (makeInteger(filter.get("pChange")) >= minPerncetage)
						&& (makeInteger(filter.get("pChange")) <= maxPerncetage)
						: (makeInteger(filter.get("pChange")) <= -minPerncetage)
								&& (makeInteger(filter.get("pChange")) >= -maxPerncetage))
				.collect(Collectors.toList()), entity.getTrend());
	}

	public List<Map<String, Object>> sortListbyTrend(List<Map<String, Object>> valueStocks, Boolean trend) {
		List<Map<String, Object>> subList = new ArrayList<>();
		List<Map<String, Object>> subList2 = new ArrayList<>();
		for (int i = 0; (i < valueStocks.size()); i++) {
			if (makeInteger(valueStocks.get(i).get("pChange")) < 0) {
				subList2.add((Map<String, Object>) valueStocks.get(i));
			} else {
				subList.add((Map<String, Object>) valueStocks.get(i));
			}
		}
		Collections.reverse(subList);
		Collections.reverse(subList2);
		return trend ? subList : subList2;
	}

	public void printStockData(List<Map<String, Object>> dataList) {
		for (int i = 0; (i < dataList.size()); i++) {
			if (makeInteger(dataList.get(i).get("lastPrice")) > 150) {
				System.out.println(dataList.get(i).get("symbol") + " % Change :" + dataList.get(i).get("pChange")
						+ " Last Price :" + dataList.get(i).get("lastPrice"));
			}
		}
	}

	public Double getRangeBound(Candle candle) {
		return makeDouble(candle.getHigh() - candle.getLow());
	}


	public Boolean getTrendData(Map<String, Object> trendDataMap) {
		Integer declines = Integer.parseInt(String.valueOf(trendDataMap.get("declines")));
		Integer advances = Integer.parseInt(String.valueOf(trendDataMap.get("advances")));
		System.out.println("Advances: " + advances + " Declines: " + declines);
		return advances > declines;
	}

	public Double getTarget(Candle candle, Boolean trend) {
		return trend ? candle.getHigh() + getRangeBound(candle) : candle.getLow() - getRangeBound(candle);
	}

	public Double getStockLTP(List<Map<String, Object>> list, String stock) {
		Map<String, Object> stockMap = getStockData(list, stock);
		if (stockMap.isEmpty()) {
			return 0.0;
		}
		return getLTP(stockMap);
	}

	public Double getLTP(Map<String, Object> stockMap) {
		return makeDouble(stockMap.get("lastPrice"));
	}

	public Map<String, Object> getStockData(List<Map<String, Object>> list, String stock) {
		Map<String, Object> stockMap = new HashMap<>();
		list.stream().forEach(action -> {
			if (String.valueOf(action.get("symbol")).equalsIgnoreCase(stock)) {
				stockMap.putAll(action);
			}
		});
		return stockMap;
	}

	public Boolean filterRangeSelection(Boolean trend, Trend trendValue, RangeBoundModel rangeBoundModel, Double ltp) {
		Double high = rangeBoundModel.getHigh();
		Double low = rangeBoundModel.getLow();
		return trend ? (isPositiveTrend(trendValue) && ltp > high) : (isNegativeTrend(trendValue) && ltp < low);
	}

	public boolean isPositiveTrend(Trend trendValue) {
		return trendValue.name().equalsIgnoreCase(Trend.POSITIVE.name());
	}

	public boolean isNegativeTrend(Trend trendValue) {
		return trendValue.name().equalsIgnoreCase(Trend.NEGATIVE.name());
	}

	public Trend getTrend(RangeBoundModel rangeBoundStock, Double ltp) {
		Trend trend = Trend.NEUTRAL;
		boolean upTrend = ltp > rangeBoundStock.getHigh();
		boolean downTrend = ltp < rangeBoundStock.getLow();
		if (upTrend) {
			trend = Trend.POSITIVE;
		} else if (downTrend) {
			trend = Trend.NEGATIVE;
		}
		return trend;
	}

	public double getTargetByPercentage(Double lastPrice, Double percentage) {
		return lastPrice * (percentage / 100);
	}

	public Double makeTarget(Candle candle, Boolean trend, Double value) {
		return trend ? candle.getHigh() + value : candle.getLow() - value;
	}

	public List<Map<String, Object>> stockList(List<Map<String, Object>> stockList, Integer stockRange) {
		return stockList.stream().filter(filter -> getLTP(filter) > stockRange).collect(Collectors.toList());
	}

	public Candle getRangeBoundCandle(RangeBoundModel model) {
		Candle candle = new Candle();
		candle.setHigh(model.getHigh());
		candle.setLow(model.getLow());
		return candle;
	}

	public Double makeStopLoss(Candle candle, Boolean trend, Double value) {
		return trend ? candle.getHigh() - value : candle.getLow() + value;
	}

	public Candle makeCandle(Map<String, Object> data) {
		Candle candle = new Candle();
		candle.setOpen(makeDouble(data.get("open")));
		candle.setHigh(makeDouble(data.get("dayHigh")));
		candle.setLow(makeDouble(data.get("dayLow")));
		candle.setClose(makeDouble(data.get("lastPrice")));
		return candle;
	}

	public Double makeTargetByPercentage(Double ltp, Boolean trend, Double percentage) {
		Double target = getTargetByPercentage(ltp, percentage);
		return trend ? ltp + target : ltp - target;
	}

	public Double makeStopLossByPercentage(Double ltp, Boolean trend, Double percentage) {
		Double target = getTargetByPercentage(ltp, percentage);
		return trend ? ltp - target : ltp + target;
	}

	public Double makeTargetForIndex(IndexEntity entity, Candle rangeCandle) {
		return makeTarget(rangeCandle, entity.getTrend(), getTargetByPercentage(getRangeBound(rangeCandle), 80.0));
	}

	public Boolean madeReturns(double returns) {
		return !(returns > 21000 || returns < -7000);
	}

	public Integer getVolume(Map<String, Object> stockMap) {
		return Integer.valueOf(String.valueOf(stockMap.get("totalTradedVolume")));
	}

	public Double getPercentage(Map<String, Object> stockMap) {
		return Double.valueOf(String.valueOf(stockMap.get("pChange")));
	}

	public String getSymbol(Map<String, Object> stockMap) {
		return String.valueOf(stockMap.get("symbol"));
	}

	public Double getOpenPrice(Map<String, Object> stockMap) {
		return makeDouble(stockMap.get("open"));
	}

	public Double getPreviousClose(Map<String, Object> stockMap) {
		return makeDouble(stockMap.get("previousClose"));
	}

	public Double getDayHigh(Map<String, Object> stockMap) {
		return makeDouble(stockMap.get("dayHigh"));
	}

	public Double getDayLow(Map<String, Object> stockMap) {
		return makeDouble(stockMap.get("dayLow"));
	}

	public void filterStocks(List<Map<String, Object>> stockList) {
		// stockList.removeIf(filter -> !(Boolean) ((Map<String, Object>)
		// filter.get("meta")).get("isFNOSec"));
//		stockList.removeIf(range -> (makeDouble(range.get("lastPrice")) > 7000.0));
		stockList.removeIf(
				range -> (makeDouble(range.get("lastPrice")) < 15.0) || (makeDouble(range.get("lastPrice")) > 6000.0));
	}

	public Double rangeTarget(FultaModel stock) {
		return makeDouble((stock.getHigh() + stock.getLow()) / 2);
	}

}
