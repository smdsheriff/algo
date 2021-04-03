package com.straders.algo.url;

public enum URL {

	NIFTY_50("http://www.nseindia.com/api/equity-stockIndices?index=NIFTY%2050", "NIFTY 50"),

	NIFTY_100("http://www.nseindia.com/api/equity-stockIndices?index=NIFTY%20100", "NIFTY 100"),

	NIFTY_200("http://www.nseindia.com/api/equity-stockIndices?index=NIFTY%20200", "NIFTY 200"),

	NIFTY_BANK("http://www.nseindia.com/api/equity-stockIndices?index=NIFTY%20BANK", "NIFTY BANK"),

	SYMBOL("http://www.nseindia.com/api/quote-equity?symbol=", "SYMBOL");

	private String url;

	private String name;

	URL(String mapping, String name) {
		this.url = mapping;
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

}
