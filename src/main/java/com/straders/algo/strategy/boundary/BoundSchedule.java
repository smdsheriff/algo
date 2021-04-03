package com.straders.algo.strategy.boundary;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.straders.algo.url.URL;
import com.straders.service.algobase.controller.AlgoController;

@EnableScheduling
@EnableAsync
@Controller
public class BoundSchedule extends AlgoController {

	@Scheduled(cron = "15 20 9 * * MON-FRI")
	@Async
	public void fiveMinuteSetup() {
		new Boundary(URL.NIFTY_50, algoService(), 5, false).start();
		new Boundary(URL.NIFTY_BANK, algoService(), 5, false).start();
	}

	@Scheduled(cron = "15 40 9 * * MON-FRI")
	@Async
	public void fifteenMinuteSetup() {
		new Boundary(URL.NIFTY_200, algoService(), 30, true).start();
	}

	// @Scheduled(cron = "45 31,36,41,46,51 9-10 * * MON-FRI")
	// @Async
	// public void rangeBoundCheck() {
	// new SelectStock(URL.NIFTY_200, algoService(), null,
	// Strategy.BOUNDARY).start();
	// }
	//
	// @Scheduled(cron = "45 * 18,19 * * MON-FRI")
	// @Async
	// public void regularCheck() throws InterruptedException {
	// Stocks se = new Stocks();
	// IndexEntity niftyIndex = se.getIndexEntity(URL.NIFTY_200);
	// if (se.checkNonNull(niftyIndex)) {
	// System.out.println("Data Retreived");
	// } else {
	// System.out.println("Thread Sleep for 10 Seconds for Exit");
	// Thread.currentThread().sleep(10000);
	// regularCheck();
	// }
	// }

	// @Scheduled(cron = "45 23,28,33,38,43,48,53 12-14 * * MON-FRI")
	// @Async
	// public void midBound() {
	// new SelectStock(URL.NIFTY_200, algoService(), null,
	// Strategy.BOUNDARY).start();
	// }

}
