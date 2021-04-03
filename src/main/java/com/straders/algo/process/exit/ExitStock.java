package com.straders.algo.process.exit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.straders.algo.entity.IndexEntity;
import com.straders.algo.enumurated.ExitType;
import com.straders.algo.enumurated.OrderType;
import com.straders.algo.enumurated.ProcessType;
import com.straders.algo.enumurated.Strategy;
import com.straders.algo.enumurated.Trend;
import com.straders.algo.margin.Quantity;
import com.straders.algo.process.Stocks;
import com.straders.algo.process.trend.IndexTrend;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.keys.ExitOrderKey;
import com.straders.service.algobase.db.keys.PlaceOrderKey;
import com.straders.service.algobase.db.model.ExitOrderModel;
import com.straders.service.algobase.db.model.PlaceOrderModel;
import com.straders.service.algobase.db.model.TrailOrderModel;
import com.straders.service.algobase.db.model.TrendModel;
import com.straders.service.algobase.db.service.AlgoService;

public class ExitStock extends Thread {

	protected Stocks se = new Stocks();

	protected AlgoService service;

	protected ExitType exitType;

	protected URL url = URL.NIFTY_200;

	public ExitStock(AlgoService algoService, ExitType exitType) {
		this.service = algoService;
		this.exitType = exitType;
	}

	@Override
	public void run() {
		currentThread().setName("Exit Check " + se.getCurrentTime());
		exitCheck();
	}

	private void exitCheck() {
		try {
			switch (exitType) {
			case ALL:
				exitAll();
				break;
			case STOCK:
				exitStocks();
				break;
			case TRAIL:
				trailStocks();
				break;
			default:
				break;

			}
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			Thread.currentThread().interrupt();
		}
	}

	@SuppressWarnings("static-access")
	private void exitAll() throws InterruptedException {
		IndexEntity niftyIndex = se.getIndexEntity(url);
		if (se.checkNonNull(niftyIndex)) {
			Set<Map<String, Object>> stockSet = new HashSet<>();
			stockSet.addAll(niftyIndex.getStockDataList());
			List<PlaceOrderModel> orderList = service.getOrderService().getTodayOrder(se.getToday());
			List<String> exitStockList = getActiveOrderList(orderList);
			List<ExitOrderModel> exitAllList = exitAllStocks(niftyIndex, exitStockList,
					stockSet.stream().collect(Collectors.toList()), orderList);
			exit(exitAllList);
			service.getConfirmService().deleteAll();
		} else {
			System.out.println("No Data Retrieved from Index Data for Exit All");
			System.out.println("Thread Sleep for 15 Seconds for Exit All");
			Thread.currentThread().sleep(15000);
			exitAll();
		}

	}

	@SuppressWarnings("static-access")
	private void exitStocks() throws InterruptedException {
		List<PlaceOrderModel> orderList = service.getOrderService().getTodayOrder(se.getToday());
		if (CollectionUtils.isNotEmpty(orderList)) {
			IndexEntity niftyIndex = se.getIndexEntity(url);
			if (se.checkNonNull(niftyIndex)) {
				if (isReversal(niftyIndex, false)) {
					System.out.println("Trend Reversed So Exited all Stocks");
					exitAll();
				} else {
					List<ExitOrderModel> exitList = processExit(orderList, niftyIndex);
					exit(exitList);
				}
			} else {
				System.out.println("No Data Retrieved from Index Data for Order Check");
				System.out.println("Thread Sleep for 15 Seconds for Exit");
				Thread.currentThread().sleep(15000);
				exitStocks();
			}
		} else {
			System.out.println("No Ordered Data");
		}

	}

	private void trailStocks() throws InterruptedException {
		IndexEntity niftyIndex = se.getIndexEntity(url);
		if (se.checkNonNull(niftyIndex)) {
			Set<Map<String, Object>> stockSet = new HashSet<>();
			stockSet.addAll(niftyIndex.getStockDataList());
			List<PlaceOrderModel> orderList = service.getOrderService().getTodayOrder(se.getToday());
			if (CollectionUtils.isNotEmpty(orderList)) {
				List<PlaceOrderModel> trailList = orderList.stream().filter(filter -> (filter.getTrail()))
						.collect(Collectors.toList());
				if (CollectionUtils.isNotEmpty(trailList)) {
					List<String> activeList = getActiveOrderList(trailList);
					List<PlaceOrderModel> activeListModel = orderList.stream()
							.filter(orderStock -> activeList.contains(orderStock.getSymbol()))
							.collect(Collectors.toList());
					if (CollectionUtils.isNotEmpty(activeListModel)) {
						List<ExitOrderModel> exitList = processExit(activeListModel, niftyIndex);
						exit(exitList);
					} else {
						System.out.println("No Data to trail orders");
					}
				}
			}
		} else {
			System.out.println("No Data Retrieved from Trail Order Check");
			System.out.println("Thread Sleep for 15 Seconds for Exit");
			Thread.currentThread().sleep(15000);
			trailStocks();
		}
	}

	private List<ExitOrderModel> processExit(List<PlaceOrderModel> orderList, IndexEntity niftyIndex) {
		List<ExitOrderModel> exitList = new ArrayList<>();
		for (int i = 0; i < orderList.size(); i++) {
			PlaceOrderModel orderedStock = orderList.get(i);
			Boolean exitOrder = false;
			Boolean target = false;
			Boolean loss = false;
			Boolean trailTarget = false;
			String symbol = String.valueOf(orderedStock.getSymbol());
			Map<String, Object> stockData = checkIndexData(symbol) ? niftyIndex.getIndexDataMap()
					: se.getStockData(niftyIndex.getStockDataList(), symbol);
			String orderType = orderedStock.getOrderType();
			Double targetPrice = orderedStock.getTargetPrice();
			Double stopLossPrice = orderedStock.getStoplossPrice();
			Double ltp = getLastTradePrice(niftyIndex, symbol) == 0.0 ? orderedStock.getStrikePrice()
					: getLastTradePrice(niftyIndex, symbol);
			Double dayHigh = se.getDayHigh(stockData);
			Double dayLow = se.getDayLow(stockData);
			if (checkAlreadyExitedOrder(symbol)) {
				// System.out.println("Exited already for " + symbol);
			} else {
				if (orderType.equalsIgnoreCase(OrderType.BUY.name())) {
					trailTarget = ltp >= targetPrice;
					target = (trailTarget) || (targetPrice <= dayHigh);
					loss = (ltp <= stopLossPrice);
				} else {
					trailTarget = ltp <= targetPrice;
					target = (trailTarget) || (targetPrice >= dayLow);
					loss = (ltp >= stopLossPrice);
				}
				System.out.println("Target Hit for " + symbol + " is " + target);
				System.out.println("Stoploss Hit for " + symbol + " is " + loss);
				exitOrder = target || loss;
				if (exitOrder) {
					if (trailTarget) {
						if (trailExecution(orderType, symbol, ltp, dayHigh, dayLow)) {
							System.out.println("Trail Order Executed for " + symbol);
						} else {
							Double exitPrice = target ? targetPrice : stopLossPrice;
							System.out.println("Condition Not Satisfied Trail Order so Exit Order " + ltp);
							exitList.add(exitOrder(symbol, orderType, exitPrice, target, loss, orderedStock));
						}
					} else {
						Double exitPrice = target ? targetPrice : stopLossPrice;
						System.out.println("Condition Satisfied for Exit Order " + ltp);
						exitList.add(exitOrder(symbol, orderType, exitPrice, target, loss, orderedStock));
					}
				}
			}
		}
		return exitList;
	}

	private Boolean trailExecution(String orderType, String symbol, Double ltp, Double dayHigh, Double dayLow) {
		System.out.println("Trail Target and StopLoss for " + symbol);
		Double trailPrice;
		Double trailStopLossPrice;
		Boolean trailOrder = false;
		if (orderType.equalsIgnoreCase(OrderType.BUY.name())) {
			trailPrice = ltp + (ltp * getTrailPercentage(symbol, ltp));
			trailStopLossPrice = se.makeStopLossByPercentage(ltp, true, 0.2);
			trailOrder = trailPrice > dayHigh;
		} else {
			trailPrice = ltp - (ltp * getTrailPercentage(symbol, ltp));
			trailStopLossPrice = se.makeStopLossByPercentage(ltp, false, 0.2);
			trailOrder = trailPrice < dayLow;
		}
		if (trailOrder) {
			trailOrder(orderType, symbol, ltp, trailPrice, trailStopLossPrice);
		}
		return trailOrder;
	}

	private Double getLastTradePrice(IndexEntity niftyIndex, String symbol) {
		if (checkIndexData(symbol)) {
			IndexEntity indexData = symbol.equalsIgnoreCase(URL.NIFTY_50.getName()) ? se.getIndexEntity(URL.NIFTY_50)
					: se.getIndexEntity(URL.NIFTY_BANK);
			if (se.checkNonNull(indexData)) {
				return se.getLTP(indexData.getIndexDataMap());
			} else {
				return 0.0;
			}
		} else {
			return se.getStockLTP(niftyIndex.getStockDataList(), symbol);
		}
	}

	private void exit(List<ExitOrderModel> exitList) {
		if (CollectionUtils.isNotEmpty(exitList)) {
			service.getExitService().saveAll(exitList);
		} else {
			System.out.println("No Exit order List");
		}
	}

	private ExitOrderModel exitOrder(String symbol, String orderType, Double ltp, Boolean target, Boolean loss,
			PlaceOrderModel orderedStock) {
		System.out.println("Order Exited for " + symbol + se.getCurrentTime());
		ExitOrderModel exitModel = new ExitOrderModel();
		exitModel.setDate(se.getToday());
		exitModel.setTime(se.getTime());
		exitModel.setSymbol(symbol);
		exitModel.setExitPrice(se.makeDouble(ltp));
		exitModel.setStrategy(orderedStock.getStrategy());
		exitModel.setOrderType(
				orderType.equalsIgnoreCase(OrderType.BUY.name()) ? OrderType.SELL.name() : OrderType.BUY.name());
		if (target) {
			exitModel.setType(ProcessType.TARGET.name());
		} else if (loss) {
			exitModel.setType(ProcessType.STOPLOSS.name());
		}
		return exitModel;
	}

	private boolean checkAlreadyExitedOrder(String symbol) {
		ExitOrderKey keys = new ExitOrderKey();
		keys.date = se.getToday();
		keys.symbol = symbol;
		return service.getExitService().existsById(keys);
	}

	private List<ExitOrderModel> exitAllStocks(IndexEntity niftyIndex, List<String> exitStockList,
			List<Map<String, Object>> stockList, List<PlaceOrderModel> orderList) {
		System.out.println("Exit All Order Stocks  " + exitStockList);
		return exitStockList.stream().map(stock -> {
			ExitOrderModel exitModel = new ExitOrderModel();
			PlaceOrderModel orderModel = orderList.stream().filter(filter -> filter.getSymbol().equalsIgnoreCase(stock))
					.findFirst().get();
			String orderType = orderModel.getOrderType();
			Map<String, Object> stockData = checkIndexData(stock) ? niftyIndex.getIndexDataMap()
					: se.getStockData(niftyIndex.getStockDataList(), stock);
			Strategy strategy = Strategy.valueOf(orderModel.getStrategy());
			Double ltp = Strategy.INDEX.name().equalsIgnoreCase(strategy.name()) ? se.getLTP(stockData)
					: se.getStockLTP(stockList, stock);
			Double targetPrice = orderModel.getTargetPrice();
			Double stopLossPrice = orderModel.getStoplossPrice();
			Double strikePrice = orderModel.getStrikePrice();
			Double dayHigh = se.getDayHigh(stockData);
			Double dayLow = se.getDayLow(stockData);
			Boolean target;
			Boolean loss;
			if (orderType.equalsIgnoreCase(OrderType.BUY.name())) {
				target = (ltp >= targetPrice) || (targetPrice <= dayHigh) || (ltp > strikePrice);
				loss = (ltp <= stopLossPrice) || (ltp < strikePrice);
			} else {
				target = (ltp <= targetPrice) || (targetPrice >= dayLow) || (ltp < strikePrice);
				loss = (ltp >= stopLossPrice) || (ltp > strikePrice);
			}
			exitModel.setDate(se.getToday());
			exitModel.setTime(se.getTime());
			exitModel.setSymbol(stock);
			exitModel.setExitPrice(ltp);
			exitModel.setOrderType(orderType);
			exitModel.setStrategy(orderModel.getStrategy());
			if (loss) {
				if (se.checkNonNull(orderModel.getTrail()) && orderModel.getTrail()) {
					exitModel.setType(ProcessType.TARGET.name());
				} else {
					exitModel.setType(ProcessType.STOPLOSS.name());
				}
			} else if (target) {
				exitModel.setType(ProcessType.TARGET.name());
			} else {
				exitModel.setType(ProcessType.STOPLOSS.name());
			}
			return exitModel;
		}).collect(Collectors.toList());

	}

	private boolean checkIndexData(String stock) {
		return stock.equalsIgnoreCase(URL.NIFTY_50.getName()) || stock.equalsIgnoreCase(URL.NIFTY_BANK.getName());
	}

	private boolean isReversal(IndexEntity indexEntity, Boolean isFinal) throws InterruptedException {
		List<TrendModel> trendList = service.getTrendService().getTodayTrend();
		if (CollectionUtils.isEmpty(trendList)) {
			return false;
		} else {
			if (trendList.size() == 1) {
				TrendModel trendModel = trendList.stream().findFirst().get();
				Boolean isSameTrend = trendModel.getTrend()
						.equalsIgnoreCase(indexEntity.getTrend() ? Trend.POSITIVE.name() : Trend.NEGATIVE.name());
				if (isSameTrend) {
					// follows same trend
				} else {
					Map<String, Object> indexData = indexEntity.getIndexDataMap();
					Double dayLow = trendModel.getLow();
					Double dayHigh = trendModel.getHigh();
					Double ltp = se.getLTP(indexData);
					Boolean isReversed = false;
					if (se.isPositiveTrend(Trend.valueOf(trendModel.getTrend()))) {
						isReversed = ltp < dayLow;
					} else if (se.isNegativeTrend(Trend.valueOf(trendModel.getTrend()))) {
						isReversed = ltp > dayHigh;
					}
					if (isReversed) {
						if (isFinal) {
							System.out.println("Trend Reversed Today+++++++++++++++++++++++");
							new IndexTrend(service, url).updateTrend(indexEntity);
						} else {
							List<PlaceOrderModel> orderList = service.getOrderService().getTodayOrder(se.getToday());
							List<ExitOrderModel> exitList = processExit(orderList, indexEntity);
							exit(exitList);
							System.out.println(
									"+++++++++++++++++++++++Trend Reversal Confirmation for 6 minutes++++++++++++++++++++++++++++++++");
							Thread.currentThread().sleep(360000);
							confirmLatestTrend();
							return trendList.size() > 1 ? true : false;
						}
					}
				}
			}
			System.out.println("Today's Trend Size " + trendList.size());
			return trendList.size() > 1 ? true : false;
		}
	}

	private void confirmLatestTrend() throws InterruptedException {
		IndexEntity niftyIndex = se.getIndexEntity(url);
		if (se.checkNonNull(niftyIndex)) {
			isReversal(niftyIndex, true);
		} else {
			System.out.println("Reversal Check+++++++++++");
			System.out.println("Thread Sleep for 15 Seconds for Exit");
			Thread.currentThread().sleep(15000);
			confirmLatestTrend();
		}

	}

	private void trailOrder(String orderType, String symbol, Double ltp, Double trailPrice, Double trailStopLossPrice) {
		PlaceOrderKey orderKey = new PlaceOrderKey();
		orderKey.date = se.getToday();
		orderKey.symbol = symbol;
		updateOrder(orderKey, ltp, trailPrice, trailStopLossPrice);
	}

	private double getTrailPercentage(String symbol, Double ltp) {
		if (symbol.equalsIgnoreCase(URL.NIFTY_50.getName())) {
			return 5 / ltp;
		} else if (symbol.equalsIgnoreCase(URL.NIFTY_BANK.getName())) {
			return 20 / ltp;
		} else {
			switch (exitType) {
			case STOCK:
				return 0.002;
			case TRAIL:
				return 0.001;
			default:
				return 0.001;
			}
		}
	}

	private List<String> getActiveOrderList(List<PlaceOrderModel> orderList) {
		List<ExitOrderModel> exitList = service.getExitService().getTodayOrder(se.getToday());
		List<String> stockList = exitList.stream().map(ExitOrderModel::getSymbol).collect(Collectors.toList());
		return orderList.stream().filter(exitStock -> !(stockList.contains(exitStock.getSymbol())))
				.map(PlaceOrderModel::getSymbol).collect(Collectors.toList());
	}

	public void updateOrder(PlaceOrderKey keys, Double ltp, Double targetPrice, Double stopLossPrice) {
		Optional<PlaceOrderModel> orderModelOptional = service.getOrderService().getOrder(keys);
		if (orderModelOptional.isPresent()) {
			PlaceOrderModel orderModel = orderModelOptional.get();
			saveTrailData(orderModel, ltp, targetPrice, stopLossPrice);
			Double averagePrice = (orderModel.getStrikePrice() + ltp) / 2;
			orderModel.setStrikePrice(averagePrice);
			orderModel.setTargetPrice(se.makeDouble(targetPrice));
			orderModel.setStoplossPrice(se.makeDouble(stopLossPrice));
			orderModel.setTrail(true);
			orderModel.setQuantity(increaseQuantity(orderModel.getSymbol(), orderModel.getQuantity(), averagePrice));
			service.getOrderService().update(orderModel);
		}

	}

	private void saveTrailData(PlaceOrderModel orderModel, Double ltp, Double targetPrice, Double stopLossPrice) {
		TrailOrderModel trailModel = new TrailOrderModel();
		trailModel.setSymbol(orderModel.getSymbol());
		trailModel.setDate(se.getToday());
		trailModel.setTime(se.getTime());
		trailModel.setStrategy(orderModel.getStrategy());
		trailModel.setPreviousPrice(se.makeDouble(orderModel.getStrikePrice()));
		trailModel.setPreviousTarget(se.makeDouble(orderModel.getTargetPrice()));
		trailModel.setPreviousStoploss(se.makeDouble(orderModel.getStoplossPrice()));
		trailModel.setCurrentPrice(se.makeDouble(ltp));
		trailModel.setCurrentTarget(se.makeDouble(targetPrice));
		trailModel.setCurrentStoploss(se.makeDouble(stopLossPrice));
		service.getTrailService().save(trailModel);
	}

	private Integer increaseQuantity(String symbol, Integer oldQuantity, Double ltp) {
		Quantity quantity = new Quantity();
		if (symbol.equalsIgnoreCase(URL.NIFTY_50.getName()) || symbol.equalsIgnoreCase(URL.NIFTY_BANK.getName())) {
			return quantity.withIndexRange(symbol, oldQuantity);
		} else {
			return quantity.withinRange(oldQuantity, ltp) ? quantity.getQuantity(ltp) + oldQuantity : oldQuantity;
		}
	}

}
