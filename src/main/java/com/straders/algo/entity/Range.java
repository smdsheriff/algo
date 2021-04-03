package com.straders.algo.entity;

import lombok.Data;

@Data
public class Range {

	Integer min;

	Integer max;

	public Range(Integer min, Integer max) {
		super();
		this.min = min;
		this.max = max;
	}
}
