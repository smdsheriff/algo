package com.straders.algo.strategy.fulta;

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
public class FultaSchedule extends AlgoController {

	@Scheduled(cron = "30 18,23,34,39,44,48 9-10 * * MON-FRI")
	@Async
	public void fultaShort() {
		new SelectStock(URL.NIFTY_200, algoService(), new Range(5, 18), Strategy.FULTA).start();
		System.out.println("Fulta Trade");
	}

	@Scheduled(cron = "45 10 16 * * MON-FRI")
	@Async
	public void fultaSetup() {
		new Fulta(URL.NIFTY_200, algoService(), new Range(5, 18), true).start();
		System.out.println("Fulta Setup");
	}

}
