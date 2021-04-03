package com.straders.algo.strategy.index;

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
public class IndexSchedule extends AlgoController {

//	@Scheduled(cron = "45 26,31,36,41,46,51 9-10 * * MON-FRI")
//	@Async
//	public void rangeBoundCheck() {
//		// new SelectStock(URL.NIFTY_50, algoService(), new Range(2, 9),
//		// Strategy.INDEX).start();
//		// new SelectStock(URL.NIFTY_BANK, algoService(), range(2, 9),
//		// Strategy.INDEX).start();
//	}
//
//	@Scheduled(cron = "45 0/4 10-11 * * MON-FRI")
//	@Async
//	public void regularCheck() {
//		// new SelectStock(URL.NIFTY_50, algoService(), new Range(2, 9),
//		// Strategy.INDEX).start();
//		// new SelectStock(URL.NIFTY_BANK, algoService(), range(2, 9),
//		// Strategy.INDEX).start();
//	}
//
//	@Scheduled(cron = "45 23,28,33,38,43,48,53 12-14 * * MON-FRI")
//	@Async
//	public void midBound() {
//		// new SelectStock(URL.NIFTY_50, algoService(), new Range(2, 6),
//		// Strategy.INDEX).start();
//		// new SelectStock(URL.NIFTY_BANK, algoService(), range(2, 6),
//		// Strategy.INDEX).start();
//	}

}
