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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import org.json.me.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import systemj.common.SJServiceRegistry;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class InvokeGetServiceDescription implements ClientTemplateSS {

	String GSR_Addr_To_Write = "";
	int GSR_Port_To_Write = 4842;
	
	public void SetGSRAddr(String GSR_Addr) {
		this.GSR_Addr_To_Write = GSR_Addr;
	}
	
	public void SetGSRPort(int GSR_Port) {
		this.GSR_Port_To_Write = GSR_Port;
	}
	
	public void execute(String Addr, int port, String name) throws Exception {
    //public static void main(String[] args) throws Exception {
    	InvokeGetServiceDescription example = new InvokeGetServiceDescription();

        new ClientRequestServiceDescriptionRunner(example, Addr, port, name).run();
    //}
	}
	
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future, String name) throws Exception {
        // synchronous connect
        client.connect().get();
        
        
        // write GSR addr
        
        List<NodeId> nodeIdsGSRAddr = ImmutableList.of(new NodeId(2, name+"/GSR/GSR_ADDR"));
        
		Variant vStatus = new Variant(GSR_Addr_To_Write);
		
		 // don't write status or timestamps
        DataValue dvStatus = new DataValue(vStatus, null, null);

        // write asynchronously....
        CompletableFuture<List<StatusCode>> fGSRAddr =
            client.writeValues(nodeIdsGSRAddr, ImmutableList.of(dvStatus));

        // ...but block for the results so we write in order
        List<StatusCode> statusCodesGSRAddr = fGSRAddr.get();
        StatusCode statusGSRAddr = statusCodesGSRAddr.get(0);

        if (statusGSRAddr.isGood()) {
            logger.info("Wrote '{}' to nodeId={}", vStatus, nodeIdsGSRAddr.get(0));
        }
        
        //write GSR port
        
        List<NodeId> nodeIdsGSRPort = ImmutableList.of(new NodeId(2, name+"/GSR/GSR_PORT"));
        
		Variant vPort = new Variant(Integer.toString(GSR_Port_To_Write));
		
		 // don't write status or timestamps
        DataValue dvPort = new DataValue(vPort, null, null);

        // write asynchronously....
        CompletableFuture<List<StatusCode>> fGSRPort =
            client.writeValues(nodeIdsGSRPort, ImmutableList.of(dvPort));

        // ...but block for the results so we write in order
        List<StatusCode> statusCodesGSRPort = fGSRPort.get();
        StatusCode statusGSRPort = statusCodesGSRPort.get(0);

        if (statusGSRAddr.isGood()) {
            logger.info("Wrote '{}' to nodeId={}", vPort, nodeIdsGSRPort.get(0));
        }
        
        
        // call the GetServiceDescription() function
        InvokeGetServiceDescriptionMethod(client, name).exceptionally(ex -> {
            logger.error("error invoking GetServiceDescription", ex);
            return "{}";
        }).thenAccept(res -> {
            logger.info("GetServiceDescription={}", res);
            try {
            	JSONObject jsRes = new JSONObject(new JSONTokener(res));
				SJServiceRegistry.AddServicesToGSR(jsRes);
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
