package com.straders.algo;

import java.sql.Date;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("unused")
public class Test {

	public static void main(String[] args) {
		try {
			
			System.out.println(new Time(System.currentTimeMillis()));
		} catch (Exception exception) {
			exception.printStackTrace();
		}

	}

}
