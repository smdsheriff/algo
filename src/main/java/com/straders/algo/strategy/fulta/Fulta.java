package com.straders.algo.strategy.fulta;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.straders.algo.entity.Candle;
import com.straders.algo.entity.IndexEntity;
import com.straders.algo.entity.Range;
import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;
import com.straders.service.algobase.db.model.FultaModel;
import com.straders.service.algobase.db.service.AlgoService;

public class Fulta extends Thread {

	protected Stocks se = new Stocks();

	protected URL index;

	protected AlgoService service;

	protected Range range;

	protected Boolean isSetup;

	public Fulta(URL nifty200, AlgoService algoService, Range range, Boolean isSetup) {
		this.index = nifty200;
		this.service = algoService;
		this.range = range;
		this.isSetup = isSetup;
	}

	@Override
	public void run() {
		currentThread().setName(index.name() + " : Fulta");
		fulta();
	}

	@SuppressWarnings("static-access")
	private void fulta() {
		try {
			IndexEntity indexEntity = se.getIndexEntity(index);
			if (se.checkNonNull(indexEntity)) {
				processFultaStocks(indexEntity);
			} else {
				System.out.println("Thread Sleep for 15 Seconds for Fulta");
				Thread.currentThread().sleep(15000);
				fulta();
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			Thread.currentThread().interrupt();
		}

	}

	public void processFultaStocks(IndexEntity indexEntity) {
		if (isSetup) {
			fultaSetup(indexEntity);
		}
	}

	private void fultaSetup(IndexEntity indexEntity) {
		List<Map<String, Object>> filteredStockList = new ArrayList<>();
		indexEntity.setTrend(true);
		filteredStockList.addAll(se.getStockByPercentage(indexEntity, range.getMin(), range.getMax()));
		indexEntity.setTrend(false);
		filteredStockList.addAll(se.getStockByPercentage(indexEntity, range.getMin(), range.getMax()));
		List<FultaModel> fultaList = filteredStockList.stream().map(stock -> {
			return makeFultaModel(stock);
		}).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(fultaList)) {
			System.out.println("Save Data for Fulta");
			service.getFultaService().saveAll(fultaList);
		}
	}

	private FultaModel makeFultaModel(Map<String, Object> stock) {
		FultaModel model = new FultaModel();
		Candle stockCandle = se.makeCandle(stock);
		LocalDate loaclDate = LocalDate.now();
		if (DayOfWeek.from(loaclDate).name().equalsIgnoreCase(DayOfWeek.FRIDAY.name())) {
			model.setDate(new Date(Instant.now().plus(Duration.ofDays(3)).toEpochMilli()));
		} else {
			model.setDate(se.getTomorrow());
		}
		model.setSymbol(se.getSymbol(stock));
		model.setOpen(se.makeDouble(se.getOpenPrice(stock)));
		model.setClose(se.makeDouble(se.getLTP(stock)));
		model.setHigh(se.makeDouble(stockCandle.getHigh()));
		model.setLow(se.makeDouble(stockCandle.getLow()));
		model.setPercentage(se.getPercentage(stock));
		model.setVolume(se.getVolume(stock));
		return model;
	}

}
