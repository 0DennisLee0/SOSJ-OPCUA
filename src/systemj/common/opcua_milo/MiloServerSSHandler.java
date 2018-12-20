/*
 * 
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

import java.io.File;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;

import org.eclipse.milo.examples.server.KeyStoreLoader;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CryptoRestrictions;
import org.slf4j.LoggerFactory;

import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.TestStackExRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.TestStackExResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.TestStackRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.TestStackResponse;


import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;

//public class MiloServerHandler implements Runnable{

public class MiloServerSSHandler {
    
    private final OpcUaServer server;

    public MiloServerSSHandler() throws Exception {
        CryptoRestrictions.remove();

        KeyStoreLoader loader = new KeyStoreLoader().load();

        DefaultCertificateManager certificateManager = new DefaultCertificateManager(
            loader.getServerKeyPair(),
            loader.getServerCertificate()
        );

        File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");

        LoggerFactory.getLogger(getClass())
            .info("security temp dir: {}", securityTempDir.getAbsolutePath());

        DefaultCertificateValidator certificateValidator = new DefaultCertificateValidator(securityTempDir);

        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(true, authChallenge -> {
            String username = authChallenge.getUsername();
            String password = authChallenge.getPassword();

            boolean userOk = "user".equals(username) && "password1".equals(password);
            boolean adminOk = "admin".equals(username) && "password2".equals(password);

            return userOk || adminOk;
        });

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationUri("urn:eclipse:milo:examples:server")
            .setApplicationName(LocalizedText.english("Eclipse Milo SOSJ OPC-UA Discovery Server"))
            .setBindAddresses(newArrayList("127.0.0.1"))
            .setBindPort(4840)
            .setBuildInfo(
                new BuildInfo(
                    "urn:eclipse:milo:example-server",
                    "eclipse",
                    "eclipse milo example server",
                    OpcUaServer.SDK_VERSION,
                    "", DateTime.now()))
            .setCertificateManager(certificateManager)
            .setCertificateValidator(certificateValidator)
            .setIdentityValidator(identityValidator)
            .setProductUri("urn:eclipse:milo:example-discovery-server")
            .setServerName("registration")
            .setSecurityPolicies(
                EnumSet.of(
                    SecurityPolicy.None,
                    SecurityPolicy.Basic128Rsa15,
                    SecurityPolicy.Basic256,
                    SecurityPolicy.Basic256Sha256))
            .setUserTokenPolicies(
                ImmutableList.of(
                    USER_TOKEN_POLICY_ANONYMOUS,
                    USER_TOKEN_POLICY_USERNAME))
            .setDiscoveryServerEnabled(false)
            .setMulticastEnabled(true)
            .build();

        server = new OpcUaServer(serverConfig);

    }
    
    public MiloServerSSHandler(String name, String addr, int SSPort) throws Exception {
        CryptoRestrictions.remove();

        //KeyStoreLoader loader = new KeyStoreLoader().load();

        DefaultCertificateManager certificateManager = new DefaultCertificateManager(
       //     loader.getServerKeyPair(),
        //    loader.getServerCertificate()
        );

        File securityTempDir = new File(System.getProperty("java.io.tmpdir"), "security");

        LoggerFactory.getLogger(getClass())
            .info("security temp dir: {}", securityTempDir.getAbsolutePath());

        DefaultCertificateValidator certificateValidator = new DefaultCertificateValidator(securityTempDir);

        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(true, authChallenge -> {
            String username = authChallenge.getUsername();
            String password = authChallenge.getPassword();

            boolean userOk = "user".equals(username) && "password1".equals(password);
            boolean adminOk = "admin".equals(username) && "password2".equals(password);

            return userOk || adminOk;
        });

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationUri("urn:eclipse:milo:sosj-cd-server:"+name+":"+addr+""+SSPort)
            .setApplicationName(LocalizedText.english("Eclipse Milo SOSJ CD OPC-UA Server " +name))
            .setBindAddresses(newArrayList(addr))
            .setBindPort(SSPort) //OPCUA bindPort is often 4840, but this is not required unless for discovery server
            .setBuildInfo(
                new BuildInfo(
                    "urn:eclipse:milo:sosj-cd-server:"+name,
                    "eclipse",
                    "eclipse milo cd sosj server " +name,
                    OpcUaServer.SDK_VERSION,
                    "", DateTime.now()))
            .setCertificateManager(certificateManager)
            .setCertificateValidator(certificateValidator)
            .setIdentityValidator(identityValidator)
            .setProductUri("urn:eclipse:milo:sosjcdserver:" +name)
            .setServerName(name)
            .setSecurityPolicies(
                EnumSet.of(
                    SecurityPolicy.None,
                    SecurityPolicy.Basic128Rsa15,
                    SecurityPolicy.Basic256,
                    SecurityPolicy.Basic256Sha256))
            .setUserTokenPolicies(
                ImmutableList.of(
                    USER_TOKEN_POLICY_ANONYMOUS,
                    USER_TOKEN_POLICY_USERNAME))
            .setDiscoveryServerEnabled(true)
            .setMulticastEnabled(true)
            .build();

        server = new OpcUaServer(serverConfig);
        
        server.getNamespaceManager().registerAndAdd(
                SOSJOPCServerNamespace.NAMESPACE_URI,
                idx -> new SOSJOPCServerNamespace(server, idx, name));
        server.getServer().addRequestHandler(TestStackRequest.class, service ->
        {
          TestStackRequest request = service.getRequest();
          ResponseHeader header = service.createResponseHeader();
          service.setResponse(new TestStackResponse(header, request.getInput()));
        });
        server.getServer().addRequestHandler(TestStackExRequest.class, service ->
        {
          TestStackExRequest request = service.getRequest();
          ResponseHeader header = service.createResponseHeader();
          service.setResponse(new TestStackExResponse(header, request.getInput()));
    });

    }

    public OpcUaServer getServer() {
        return server;
    }

    public CompletableFuture<OpcUaServer> startup() {
    	 return server.startup().whenComplete((opcUaServer, throwable) -> server
    	            .registerWithDiscoveryServer("opc.tcp://localhost:4840/discovery", null, null));
    }
    
    public CompletableFuture<OpcUaServer> startup(String DiscServAddr) {
   	 return server.startup().whenComplete((opcUaServer, throwable) -> server
   	            .registerWithDiscoveryServer("opc.tcp://"+DiscServAddr+":4840/discovery", null, null));
    }
    
    public boolean reRegisterServer (String DiscServAddr) {
    	return server.registerWithDiscoveryServer("opc.tcp://"+DiscServAddr+":4840/discovery", null, null);
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        CompletableFuture<OpcUaServer> done = new CompletableFuture<>();
        server.unregisterFromDiscoveryServer()
            .whenComplete((statusCode, throwable) -> server.shutdown().whenComplete((opcUaServer, throwable1) -> {
                if (opcUaServer != null) {
                    done.complete(opcUaServer);
                } else {
                    done.completeExceptionally(throwable1);
                }
            }));
        return done;
    }

	
	/*
	 @Override
	public void run() {
		
		try {
			
			MiloServerHandler server = new MiloServerHandler();

	        server.startup().get();

	        final CompletableFuture<Void> future = new CompletableFuture<>();

	        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdown().thenRun(() -> future.complete(null))));

	        future.get();
			
		} catch (Exception ex){
			
			
			
		}
		
		
		
	}
	*/
}
