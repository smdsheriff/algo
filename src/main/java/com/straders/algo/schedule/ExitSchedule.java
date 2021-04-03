package com.straders.algo.schedule;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.straders.algo.enumurated.ExitType;
import com.straders.algo.process.exit.ExitStock;
import com.straders.service.algobase.controller.AlgoController;

@EnableScheduling
@EnableAsync
@Controller
public class ExitSchedule extends AlgoController {

	@Scheduled(cron = "30 21,26,33,38,43,48,53,58 9-10 * * MON-FRI")
	@Async
	public void exitOrder() {
		new ExitStock(algoService(), ExitType.STOCK).start();
	}

	@Scheduled(cron = "30 0/8 10-14 * * MON-FRI")
	@Async
	public void exitOrderAnalyse() {
		new ExitStock(algoService(), ExitType.STOCK).start();
	}

	@Scheduled(cron = "30 4,12,20,28,36,44,52 10-14 * * MON-FRI")
	@Async
	public void trailOrder() {
		new ExitStock(algoService(), ExitType.TRAIL).start();
	}

	@Scheduled(cron = "30 8 15 * * MON-FRI")
	@Async
	public void exitOrderFinal() {
		new ExitStock(algoService(), ExitType.STOCK).start();
	}

	@Scheduled(cron = "30 4 15 * * MON-FRI")
	@Async
	public void trailOrderFinal() {
		new ExitStock(algoService(), ExitType.TRAIL).start();
	}

	@Scheduled(cron = "15 10 15 * * MON-FRI")
	@Async
	public void exitAll() {
		new ExitStock(algoService(), ExitType.ALL).start();
	}

}
