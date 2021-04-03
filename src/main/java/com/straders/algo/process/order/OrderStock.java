package com.straders.algo.process.order;

import com.straders.algo.entity.Candle;
import com.straders.algo.entity.IndexEntity;
import com.straders.algo.enumurated.OrderType;
import com.straders.algo.enumurated.Strategy;
import com.straders.algo.enumurated.Trend;
import com.straders.algo.margin.Quantity;
import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.PlaceOrderModel;
import com.straders.service.algobase.db.model.RangeBoundModel;
import com.straders.service.algobase.db.service.AlgoService;

public class OrderStock extends Stocks {

	protected AlgoService service;

	protected IndexEntity indexEntity;

	protected Trend trend;

	protected Double ltp;

	protected RangeBoundModel rangeBoundModel;

	protected Quantity quantity = new Quantity();

	private Strategy strategy;

	private String symbol;

	protected Double target;

	protected Double stopLoss;

	protected Double dayHigh;

	protected Double dayLow;

	public OrderStock(AlgoService algoService, String symbol, IndexEntity indexEntity, Trend trend, Double ltp,
			RangeBoundModel rangeBoundModel, Strategy strategy) {
		this.service = algoService;
		this.symbol = symbol;
		this.indexEntity = indexEntity;
		this.trend = trend;
		this.ltp = ltp;
		this.rangeBoundModel = rangeBoundModel;
		this.strategy = strategy;
	}

	public void orderStock() {
		if (canOrder()) {
			PlaceOrderModel model = makePlaceOrderModel(indexEntity, trend, ltp);
			service.getOrderService().save(model);
		} else {
			System.out.println("Condition not satisfied to Place Order " + symbol);
		}
	}

	private boolean canOrder() {
		Boolean canOrder = false;
		switch (strategy) {
		case BOUNDARY:
			Candle rangeBoundaryCandle = getRangeBoundCandle(rangeBoundModel);
			Double rangePercentage = (rangeBoundModel.getRange() / ltp) * 100;
			dayHigh = getDayHigh(getStockData(indexEntity.getStockDataList(), symbol));
			dayLow = getDayLow(getStockData(indexEntity.getStockDataList(), symbol));
			target = makeTargetByPercentage(ltp, indexEntity.getTrend(), rangePercentage);
			stopLoss = trend.name().equalsIgnoreCase(Trend.POSITIVE.name())
					? (rangeBoundaryCandle.getLow() + (rangeBoundaryCandle.getLow() * 0.003))
					: (rangeBoundaryCandle.getHigh() - (rangeBoundaryCandle.getHigh() * 0.003));
			canOrder = indexEntity.getTrend() ? ((ltp < target) && (ltp > stopLoss) && (target > dayHigh))
					&& (target <= (rangeBoundaryCandle.getHigh() + (rangeBoundaryCandle.getHigh() * rangePercentage)))
					: ((ltp > target) && (ltp < stopLoss) && (target < dayLow)
							&& (target >= (rangeBoundaryCandle.getLow()
									- (rangeBoundaryCandle.getLow() * rangePercentage))));
			break;
		case DAMAKKA:
			dayHigh = getDayHigh(getStockData(indexEntity.getStockDataList(), symbol));
			dayLow = getDayLow(getStockData(indexEntity.getStockDataList(), symbol));
			target = makeTargetByPercentage(ltp, indexEntity.getTrend(), 0.5);
			stopLoss = makeStopLossByPercentage(ltp, indexEntity.getTrend(), 0.5);
			canOrder = indexEntity.getTrend() ? ((ltp < target) && (ltp > stopLoss) && (target > dayHigh))
					: ((ltp > target) && (ltp < stopLoss) && (target < dayLow));
			break;
		case FULTA:
			dayHigh = getDayHigh(getStockData(indexEntity.getStockDataList(), symbol));
			dayLow = getDayLow(getStockData(indexEntity.getStockDataList(), symbol));
			target = makeTargetByPercentage(ltp, indexEntity.getTrend(), 0.4);
			stopLoss = makeStopLossByPercentage(ltp, indexEntity.getTrend(), 0.4);
			canOrder = indexEntity.getTrend() ? ((ltp < target) && (ltp > stopLoss) && (target > dayHigh))
					: ((ltp > target) && (ltp < stopLoss) && (target < dayLow));
			break;
		case INDEX:
			Candle rangeCandle = getRangeBoundCandle(rangeBoundModel);
			target = makeTargetForIndex(indexEntity, rangeCandle);
			stopLoss = makeStopLoss(rangeCandle, indexEntity.getTrend(),
					getTargetByPercentage(getRangeBound(rangeCandle), 80.0));
			canOrder = indexEntity.getTrend() ? ((ltp < target) && (ltp > stopLoss))
					: ((ltp > target) && (ltp < stopLoss));
			break;
		case OPEN:
			dayHigh = getDayHigh(getStockData(indexEntity.getStockDataList(), symbol));
			dayLow = getDayLow(getStockData(indexEntity.getStockDataList(), symbol));
			target = makeTargetByPercentage(ltp, indexEntity.getTrend(), 0.5);
			stopLoss = trend.name().equalsIgnoreCase(Trend.POSITIVE.name()) ? (dayLow + (dayLow * 0.005))
					: (dayHigh - (dayHigh * 0.005));
			canOrder = indexEntity.getTrend() ? ((ltp < target) && (ltp > stopLoss) && (target > dayHigh))
					: ((ltp > target) && (ltp < stopLoss) && (target < dayLow));
			break;
		case PERCENTAGE:
			dayHigh = getDayHigh(getStockData(indexEntity.getStockDataList(), symbol));
			dayLow = getDayLow(getStockData(indexEntity.getStockDataList(), symbol));
			target = makeTargetByPercentage(ltp, indexEntity.getTrend(), 0.4);
			stopLoss = makeStopLossByPercentage(ltp, indexEntity.getTrend(), 0.2);
			canOrder = indexEntity.getTrend() ? ((ltp < target) && (ltp > stopLoss) && (target > dayHigh))
					: ((ltp > target) && (ltp < stopLoss) && (target < dayLow));
			break;
		default:
			break;
		}
		return canOrder && checkOrderRecordTime();
	}

	private boolean checkOrderRecordTime() {
		Integer orderSize = service.getOrderService().checkOrderTime(getToday(), getTime());
		System.out.println("Current Time Ordered Data size " + orderSize);
		return orderSize < 2;
	}

	private PlaceOrderModel makePlaceOrderModel(IndexEntity entity, Trend trend, Double ltp) {
		PlaceOrderModel model = new PlaceOrderModel();
		model.setDate(getToday());
		model.setSymbol(symbol);
		model.setTime(getTime());
		model.setOrderType(isPositiveTrend(trend) ? OrderType.BUY.name() : OrderType.SELL.name());
		model.setStrikePrice(ltp);
		model.setTargetPrice(makeDouble(target));
		model.setStoplossPrice(makeDouble(stopLoss));
		model.setStrategy(strategy.name());
		model.setQuantity(makeQuantity(ltp));
		model.setTrail(false);
		return model;
	}

	private Integer makeQuantity(Double ltp) {
		if (symbol.equalsIgnoreCase(URL.NIFTY_50.getName())) {
			return 75;
		} else if (symbol.equalsIgnoreCase(URL.NIFTY_BANK.getName())) {
			return 25;
		} else {
			return quantity.getQuantity(ltp);
		}
	}

}
