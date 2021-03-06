/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package org.eclipse.californium.examples;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.logging.Level;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;

public class SecureClient_IoTSH {

	static {
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINE);
	}

	private static final String TRUST_STORE_PASSWORD = "rootPass";
	private static final String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
	private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";

	//Input the ip addresses of the two traffic light servers
	private static final String RESET_NORTH_URI = "coaps://172.29.19.69/reset";
	private static final String RESET_WEST_URI = "coaps://172.29.23.252/reset";
	
	private static final String AMBULANCE_NORTH_ForNorth_URI = "coaps://172.29.19.69/ambulance_north";
	private static final String AMBULANCE_NORTH_ForWest_URI = "coaps://172.29.23.252/ambulance_north";
	private static final String AMBULANCE_WEST_ForNorth_URI = "coaps://172.29.19.69/ambulance_west";
	private static final String AMBULANCE_WEST_ForWest_URI = "coaps://172.29.23.252/ambulance_west";

	private DTLSConnector dtlsConnector_n;
	private DTLSConnector dtlsConnector_w;

	public SecureClient_IoTSH() {
		try {
			// load key store
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = getClass().getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());
			in.close();

			// load trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			in = getClass().getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION);
			trustStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
			in.close();

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate("root");

			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
			builder.setPskStore(new StaticPskStore("Client_identity", "secretPSK".getBytes()));
			builder.setIdentity((PrivateKey)keyStore.getKey("client", KEY_STORE_PASSWORD.toCharArray()),
					keyStore.getCertificateChain("client"), true);
			builder.setTrustStore(trustedCertificates);
			dtlsConnector_n = new DTLSConnector(builder.build());
			dtlsConnector_w = new DTLSConnector(builder.build());

		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Could not load the keystore");
			e.printStackTrace();
		}
	}

	public void reset() {

		CoapResponse response = null;
		try {
			URI rst_n_uri = new URI(RESET_NORTH_URI);
			URI rst_w_uri = new URI(RESET_WEST_URI);
			URI amb_nn_uri = new URI(AMBULANCE_NORTH_ForNorth_URI);
			URI amb_nw_uri = new URI(AMBULANCE_NORTH_ForWest_URI);
			URI amb_ww_uri = new URI(AMBULANCE_WEST_ForWest_URI);
			URI amb_wn_uri = new URI(AMBULANCE_WEST_ForNorth_URI);

			CoapClient north_reset_client = new CoapClient(rst_n_uri);
			CoapClient west_reset_client = new CoapClient(rst_w_uri);
			north_reset_client.setEndpoint(new CoapEndpoint(dtlsConnector_n, NetworkConfig.getStandard()));
			west_reset_client.setEndpoint(new CoapEndpoint(dtlsConnector_w, NetworkConfig.getStandard()));
			response = north_reset_client.get();
			response = west_reset_client.get();

		} catch (URISyntaxException e) {
			System.err.println("Invalid URI: " + e.getMessage());
			System.exit(-1);
		}

		if (response != null) {

			System.out.println(response.getCode());
			System.out.println(response.getOptions());
			System.out.println(response.getResponseText());

			System.out.println("\nADVANCED\n");
			System.out.println(Utils.prettyPrint(response));

		} else {
			System.out.println("No response received.");
		}
	}

	public static void main(String[] args) throws InterruptedException {

		SecureClient_IoTSH client = new SecureClient_IoTSH();
		client.reset();

		synchronized (SecureClient_IoTSH.class) {
			SecureClient_IoTSH.class.wait();
		}
	}
}