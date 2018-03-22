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
import java.net.InetAddress;
import java.net.Inet6Address;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.logging.Level;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class SecureNorthTrafficLight extends CoapServer{

	final static GpioController gpio = GpioFactory.getInstance();
	final static GpioPinDigitalOutput redPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "RED", PinState.LOW);
	final static GpioPinDigitalOutput greenPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "GREEN", PinState.LOW);
	final static GpioPinDigitalOutput yellowPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "YELLOW", PinState.LOW);
	static boolean isNewResourceCalled = false;

	static {
		CaliforniumLogger.initialize();
		CaliforniumLogger.setLevel(Level.CONFIG);
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINER);
	}

	// allows configuration via Californium.properties
	public static final int DTLS_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT);

	private static final String TRUST_STORE_PASSWORD = "rootPass";
	private final static String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
	private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";

	public static void main(String[] args) {

		CoapServer server = new CoapServer();
		server.add(new CoapResource("reset") {
			@Override
			public void handleGET(CoapExchange exchange) {
				exchange.respond(ResponseCode.CONTENT, "Regular Traffic.");
				isNewResourceCalled = false;
				while(true)
				{
					//green, wait 3 sec
					greenPin.high();
					for(int i=0; i<=3000; i=i+25){
						try{
							Thread.sleep(25);
						}
						catch(InterruptedException e){
							greenPin.low();
						}
						if(isNewResourceCalled){
							greenPin.low();
							break;
						}
					}
					greenPin.low();
					//yellow, wait 2 sec
					//red, wait 5 sec
				}
			}
		});
		server.add(new CoapResource("ambulance_north") {
			@Override
			public void handleGET(CoapExchange exchange) {
				exchange.respond(ResponseCode.CONTENT, "Ambulance Coming from the South.");
				isNewResourceCalled = true;
				
			}
		});
		// ETSI Plugtest environment
		// server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("::1", DTLS_PORT)), NetworkConfig.getStandard()));
		// server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("127.0.0.1", DTLS_PORT)), NetworkConfig.getStandard()));
		//byte[] b = "2607:b400:26:0:d70:79e7:7054:ccal".getBytes();
		//InetAddress ip = InetAddress.getByAddress(b);
		//server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("172.29.64.100", DTLS_PORT)), NetworkConfig.getStandard()));
		// server.addEndpoint(new CoAPEndpoint(new DTLSConnector(new InetSocketAddress("10.200.1.2", DTLS_PORT)), NetworkConfig.getStandard()));

		try {
			// Pre-shared secrets
			InMemoryPskStore pskStore = new InMemoryPskStore();
			pskStore.setKey("password", "sesame".getBytes()); // from ETSI Plugtest test spec

			// load the trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			InputStream inTrust = SecureServer.class.getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION);
			trustStore.load(inTrust, TRUST_STORE_PASSWORD.toCharArray());

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate("root");

			// load the key store
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = SecureServer.class.getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());

			DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(new InetSocketAddress(DTLS_PORT));
			config.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8,
					CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
			config.setPskStore(pskStore);
			config.setIdentity((PrivateKey)keyStore.getKey("server", KEY_STORE_PASSWORD.toCharArray()),
					keyStore.getCertificateChain("server"), true);
			config.setTrustStore(trustedCertificates);

			DTLSConnector connector = new DTLSConnector(config.build());

			server.addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));
			server.start();

		} catch (GeneralSecurityException | IOException e) {
			System.err.println("Could not load the keystore");
			e.printStackTrace();
		}

		// add special interceptor for message traces
		for (Endpoint ep : server.getEndpoints()) {
			ep.addInterceptor(new MessageTracer());
		}

		System.out.println("Secure CoAP server powered by Scandium (Sc) is listening on port " + DTLS_PORT);
	}
}