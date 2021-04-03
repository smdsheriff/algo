package com.straders.algo.strategy.quickbite;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.straders.algo.entity.Range;
import com.straders.algo.enumurated.Strategy;
import com.straders.algo.process.select.SelectStock;
import com.straders.algo.url.URL;
import com.straders.service.algobase.controller.AlgoController;

@EnableScheduling
@EnableAsync
@Controller
public class QuickSchedule extends AlgoController {

	@Scheduled(cron = "15 38,44,49,56 9 * * MON-FRI")
	@Async
	public void quickBiteByPercentage() {
		new SelectStock(URL.NIFTY_200, algoService(), new Range(2, 9), Strategy.PERCENTAGE).start();
	}

	@Scheduled(cron = "15 0/10 10-13 * * MON-FRI")
	@Async
	public void quickBite() {
		new SelectStock(URL.NIFTY_200, algoService(), new Range(2, 9), Strategy.PERCENTAGE).start();
	}

	@Scheduled(cron = "15 46 9 * * MON-FRI")
	@Async
	public void quickBiteByOpen() {
		// new SelectStock(URL.NIFTY_200, algoService(), null,
		// Strategy.OPEN).start();
	}

	@Scheduled(cron = "15 23 12 * * MON-FRI")
	@Async
	public void quickBiteByOpenMidDay() {
		// new SelectStock(URL.NIFTY_200, algoService(), null,
		// Strategy.OPEN).start();
	}

}
