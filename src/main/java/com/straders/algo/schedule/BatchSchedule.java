package com.straders.algo.schedule;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.straders.algo.process.history.DailyHistory;
import com.straders.algo.process.trend.IndexData;
import com.straders.algo.process.trend.IndexTrend;
import com.straders.algo.url.URL;
import com.straders.service.algobase.controller.AlgoController;

@EnableScheduling
@EnableAsync
@Controller
public class BatchSchedule extends AlgoController {

	@Scheduled(cron = "0 20 15 * * MON-FRI")
	@Async
	public void deleteTransaction() {
		algoService().getRangeService().deleteAll();
		algoService().getConfirmService().deleteAll();
	}

	@Scheduled(cron = "15 8 16 * * MON-FRI")
	@Async
	public void dailyHistory() {
		new DailyHistory(algoService(), URL.NIFTY_200).updateHistory();
	}

	@Scheduled(cron = "0 42 9 * * MON-FRI")
	@Async
	public void trend() {
		new IndexTrend(algoService(), URL.NIFTY_200).updateTrend();
	}

	@Scheduled(cron = "0 30,45 9 * * MON-FRI")
	@Async
	public void morningIndexData() {
		new IndexData(algoService(), URL.NIFTY_200).indexData();
	}

	@Scheduled(cron = "0 0,15,30,45 10-14 * * MON-FRI")
	@Async
	public void regularIndexData() {
		new IndexData(algoService(), URL.NIFTY_200).indexData();
	}

	@Scheduled(cron = "0 0,15,30 15 * * MON-FRI")
	@Async
	public void endIndexData() {
		new IndexData(algoService(), URL.NIFTY_200).indexData();
	}

	// @Scheduled(cron = "55 33 14 * * *")
	// @Async
	// public void test() {
	// System.out.println("Startteddddd");
	// PlaceOrderModel model = new PlaceOrderModel();
	// model.setDate(new Date(Instant.now().toEpochMilli()));
	// model.setTime(new Time(System.currentTimeMillis()));
	// model.setSymbol("SAIL");
	// model.setOrderType("SELL");
	// model.setStrikePrice(Double.valueOf("70"));
	// model.setStoplossPrice(Double.valueOf("74"));
	// model.setTargetPrice(Double.valueOf("68"));
	// model.setStrategy("DAMAKKA");
	// algoService().getOrderService().save(model);
	// System.out.println("Completed Place");
	// }

	//
	// @Scheduled(cron = "0/20 * * * * *")
	// @Async
	// public void checkTime() {
	// Integer size = algoService().getOrderService().checkOrderTime(new
	// Date(Instant.now().toEpochMilli()),
	// new Time(System.currentTimeMillis()));
	// System.out.println("Current Size" + size);
	// }
	//
	// @Scheduled(cron = "40 03 14 * * *")
	// @Async
	// public void texitst() {
	// System.out.println("Startteddddd Exit");
	// ExitOrderModel model = new ExitOrderModel();
	// model.setExitPrice(Double.valueOf("209.56"));
	// model.setDate(new Date(Instant.now().toEpochMilli()));
	// model.setTime(new Time(System.currentTimeMillis()));
	// model.setOrderType("BUY");
	// model.setType("TARGET");
	// model.setSymbol("ZEEL");
	// model.setStrategy("DAMAKKA");
	// Publisher publish = new Publisher("exitOrder", "exitOrder", "exitOrder");
	// publish.publish("exitOrder", model);
	// System.out.println("Completed Exit");
	// }

	// @Scheduled(cron = "10 57 13 * * *")
	// @Async
	// public void testTrail() {
	// System.out.println("Startteddddd Trail");
	// TrailOrderModel trailModel = new TrailOrderModel();
	// trailModel.setSymbol("ZEEL");
	// trailModel.setDate(new Date(Instant.now().toEpochMilli()));
	// trailModel.setTime(new Time(System.currentTimeMillis()));
	// trailModel.setStrategy("DAMAKKA");
	// trailModel.setPreviousPrice(222.67);
	// trailModel.setPreviousTarget(221.0);
	// trailModel.setPreviousStoploss(223.7);
	// trailModel.setCurrentPrice(223.4);
	// trailModel.setCurrentTarget(217.75);
	// trailModel.setCurrentStoploss(222.22);
	// Publisher publish = new Publisher("trailOrder", "trailOrder",
	// "trailOrder");
	// publish.publish("trailOrder", trailModel);
	// System.out.println("Completed Trail");
	// }

}
