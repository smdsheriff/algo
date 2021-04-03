package com.straders.algo.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;

import com.straders.service.algobase.utils.DataUtils;

public class HttpUtils extends DataUtils {

	public String getMethod(String url) {
		String cookie = StringUtils.EMPTY;
		HttpClient httpClient = HttpClientBuilder.create().build();
		try {
			HttpGet getRequest = new HttpGet(url);
			getRequest.addHeader("accept-encoding", "gzip, deflate, br");
			getRequest.addHeader("accept-language", "en-US,en;q=0.9");
			getRequest.addHeader("accept", "*/*");
			getRequest.addHeader("Cookie", cookie);
			getRequest.addHeader("user-agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36");
			HttpResponse response = httpClient.execute(getRequest);
			int reponseCode = response.getStatusLine().getStatusCode();
			if (reponseCode == 404 || reponseCode == 401) {
				System.out.println("Data not Retrieved : " + String.valueOf(reponseCode));
				Header[] headers = response.getAllHeaders();
				for (int i = 0; i < headers.length; i++) {
					if (headers[i].getName().contains("Set-Cookie")) {
						String[] cookieValues = headers[i].getValue().split(";");
						cookie = cookieValues[0];
					}
				}
				return StringUtils.EMPTY;
			} else if (!((reponseCode >= 200) && (reponseCode <= 299))) {
				return String.valueOf(response.getStatusLine().getStatusCode());
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
			String temp = StringUtils.EMPTY;
			StringBuilder reponseText = new StringBuilder();
			while ((temp = br.readLine()) != null) {
				reponseText = reponseText.append(temp);
			}
			return String.valueOf(reponseText);
		} catch (Exception exception) {
			exception.printStackTrace();
			return StringUtils.EMPTY;
		} finally {
			HttpClientUtils.closeQuietly(httpClient);
		}
	}
}
