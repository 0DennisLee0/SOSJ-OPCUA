/*
 * Copyright (c) 2016 Kevin Herron, Udayanto Dwi Atmojo
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.html.
 */

package systemj.common.opcua_milo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import org.json.me.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systemj.common.SJServiceRegistry;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class InvokeGetServiceDescription implements ClientExample {

	public void execute(String Addr, int port) throws Exception {
    //public static void main(String[] args) throws Exception {
    	InvokeGetServiceDescription example = new InvokeGetServiceDescription();

        new ClientRequestServiceDescriptionRunner(example, Addr, port).run();
    //}
	}
	
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future, String name) throws Exception {
        // synchronous connect
        client.connect().get();

        // call the sqrt(x) function
        InvokeGetServiceDescriptionMethod(client, name).exceptionally(ex -> {
            logger.error("error invoking GetServiceDescription", ex);
            return "{}";
        }).thenAccept(res -> {
            logger.info("GetServiceDescription={}", res);
            try {
            	JSONObject jsRes = new JSONObject(new JSONTokener(res));
				SJServiceRegistry.AppendNodeServicesToCurrentRegistry(jsRes);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            future.complete(client);
        });
    }

    private CompletableFuture<String> InvokeGetServiceDescriptionMethod(OpcUaClient client, String folderName) {
        NodeId objectId = NodeId.parse("ns=2;s=" +folderName);
        NodeId methodId = NodeId.parse("ns=2;s=" +folderName+"/getServiceDescription()");

        CallMethodRequest request = new CallMethodRequest(
            objectId, methodId, new Variant[]{new Variant("")});

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                String value = (String) l(result.getOutputArguments()).get(0).getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
    
    /*
    private CompletableFuture<Double> sqrt(OpcUaClient client, Double input) {
        NodeId objectId = NodeId.parse("ns=2;s=HelloWorld");
        NodeId methodId = NodeId.parse("ns=2;s=HelloWorld/sqrt(x)");

        CallMethodRequest request = new CallMethodRequest(
            objectId, methodId, new Variant[]{new Variant(input)});

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                Double value = (Double) l(result.getOutputArguments()).get(0).getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                CompletableFuture<Double> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }
    */

}
