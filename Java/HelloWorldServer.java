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
 *    Kai Hudalla (Bosch Software Innovations GmbH) - add endpoints for all IP addresses
 ******************************************************************************/
package org.eclipse.californium.examples;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;

//gpios
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;


public class HelloWorldServer extends CoapServer {

	private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    /*
     * Application entry point.
     */
    public static void main(String[] args) {
        
        try {
			
            // create server
            HelloWorldServer server = new HelloWorldServer();
            // add endpoints on all IP addresses
            server.addEndpoints();
            server.start();

        } catch (SocketException e) {
            System.err.println("Failed to initialize server: " + e.getMessage());
        }
    }

    /*
     * Add individual endpoints listening on default CoAP port on all IPv4 addresses of all network interfaces.
     */
    private void addEndpoints() {
    	for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
    		// only binds to IPv4 addresses and localhost
			if (addr instanceof Inet4Address || addr.isLoopbackAddress()) {
				InetSocketAddress bindToAddress = new InetSocketAddress(addr, COAP_PORT);
				addEndpoint(new CoapEndpoint(bindToAddress));
			}
		}
    }

    /*
     * Constructor for a new Hello-World server. Here, the resources
     * of the server are initialized.
     */
    public HelloWorldServer() throws SocketException {
        
        // provide an instance of a Hello-World resource
        add(new redResource());
	add(new greenResource());
	add(new blueResource());

	
    }

    /*
     * Definition of the Red Resource
     */
    class redResource extends CoapResource {
        
        public redResource() {
            
            // set resource identifier
            super("red");
            
            // set display name
            getAttributes().setTitle("Red Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            
            // respond to the request
                exchange.respond("LED is red.");
		final GpioController gpio = GpioFactory.getInstance();
		final GpioPinDigitalOutput redPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "RED", PinState.LOW);
        	redPin.setShutdownOptions(true, PinState.LOW);
		redPin.toggle();
		
		try
		{
			Thread.sleep(3000);
			redPin.low();
			gpio.shutdown();
			gpio.unprovisionPin(redPin);
		}
		catch(InterruptedException e){
			redPin.low();
			gpio.shutdown();
			gpio.unprovisionPin(redPin);
		}
	}
    }

    /*
     * Definition of the Green Resource
     */
    class greenResource extends CoapResource {
        
        public greenResource() {
            
            // set resource identifier
            super("green");
            
            // set display name
            getAttributes().setTitle("Green Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            
            // respond to the request
                exchange.respond("LED is green.");
		final GpioController gpio = GpioFactory.getInstance();
		final GpioPinDigitalOutput greenPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "GREEN", PinState.LOW);
		greenPin.setShutdownOptions(true, PinState.LOW);
		greenPin.toggle();
		try
		{
			Thread.sleep(3000);
			greenPin.low();
			gpio.shutdown();
			gpio.unprovisionPin(greenPin);
		}
		catch(InterruptedException e){
			greenPin.low();
			gpio.shutdown();
			gpio.unprovisionPin(greenPin);
		}
	}
    }

     /*
     * Definition of the Blue Resource
     */
    class blueResource extends CoapResource {
        
        public blueResource() {
            
            // set resource identifier
            super("blue");
            
            // set display name
            getAttributes().setTitle("Blue Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            
            // respond to the request
                exchange.respond("LED is blue.");
		final GpioController gpio = GpioFactory.getInstance();
		final GpioPinDigitalOutput bluePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "BLUE", PinState.LOW);
		bluePin.setShutdownOptions(true, PinState.LOW);	
        	bluePin.toggle();
		try
		{
			Thread.sleep(3000);
			bluePin.low();
			gpio.shutdown();
			gpio.unprovisionPin(bluePin);
		}
		catch(InterruptedException e){
			bluePin.low();
			gpio.shutdown();
			gpio.unprovisionPin(bluePin);
		}
	}
    }
}
