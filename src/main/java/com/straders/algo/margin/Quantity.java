package com.straders.algo.margin;

import com.straders.algo.process.Stocks;
import com.straders.algo.url.URL;

public class Quantity extends Stocks {

	private Integer exposure = 8;

	private Integer valuePerStock = 5000;

	private Integer maxValuePerStock = 10000;

	private Integer maxNiftyQunatity = 150;

	private Integer maxBankNiftyQuantity = 50;

	public Integer getQuantity(Double ltp) {
		return Math.round(makeInteger(((valuePerStock / ltp) * exposure)));
	}

	public Boolean withinRange(Integer quantity, Double ltp) {
		return ((quantity * ltp) / exposure) < maxValuePerStock;
	}

	public Integer withIndexRange(String symbol, Integer oldQuantity) {
		if (symbol.equalsIgnoreCase(URL.NIFTY_50.getName())) {
			return oldQuantity < maxNiftyQunatity ? oldQuantity + 75 : oldQuantity;
		} else {
			return oldQuantity < maxBankNiftyQuantity ? oldQuantity + 25 : oldQuantity;
		}

	}

}
