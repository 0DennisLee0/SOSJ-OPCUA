/*
 * Copyright (c) 2018 Kevin Herron, Udayanto Dwi Atmojo
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

import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;

import org.eclipse.milo.examples.server.RestrictedAccessDelegate;
import org.eclipse.milo.examples.server.ValueLoggingDelegate;
import systemj.common.opcua_milo.method.GetServiceDescription;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRank;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.Namespace;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class SOSJOPCUAServerNamespaceForCD implements Namespace {

    public static final String NAMESPACE_URI = "urn:eclipse:milo:sosj-cd";
    
    private String folderName="";
    
    private Hashtable SOSJSignalNodes = new Hashtable();
    private final static Object SOSJSignalNodesLock = new Object();
    private Hashtable SOSJSignalNodeDirections = new Hashtable();
    private final static Object SOSJSignalNodeDirectionsLock = new Object();

    private static final Object[][] STATIC_SCALAR_NODES = new Object[][]{
    	/*
        {"Boolean", Identifiers.Boolean, new Variant(false)},
        {"Byte", Identifiers.Byte, new Variant(ubyte(0x00))},
        {"SByte", Identifiers.SByte, new Variant((byte) 0x00)},
        {"Int16", Identifiers.Int16, new Variant((short) 16)},
        {"Int32", Identifiers.Int32, new Variant(32)},
        {"Int64", Identifiers.Int64, new Variant(64L)},
        {"UInt16", Identifiers.UInt16, new Variant(ushort(16))},
        {"UInt32", Identifiers.UInt32, new Variant(uint(32))},
        {"UInt64", Identifiers.UInt64, new Variant(ulong(64L))},
        {"Float", Identifiers.Float, new Variant(3.14f)},
        {"Double", Identifiers.Double, new Variant(3.14d)},
        */
        {"SignalName", Identifiers.String, new Variant("signal name")},
        {"SignalName", Identifiers.String, new Variant("string value")}
        /*,
        {"DateTime", Identifiers.DateTime, new Variant(DateTime.now())},
        {"Guid", Identifiers.Guid, new Variant(UUID.randomUUID())},
        {"ByteString", Identifiers.ByteString, new Variant(new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04}))},
        {"XmlElement", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>"))},
        {"LocalizedText", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text"))},
        {"QualifiedName", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg"))},
        {"NodeId", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd"))},

        {"Duration", Identifiers.Duration, new Variant(1.0)},
        {"UtcTime", Identifiers.UtcTime, new Variant(DateTime.now())},
        */
    };

    private static final Object[][] STATIC_ARRAY_NODES = new Object[][]{
        {"BooleanArray", Identifiers.Boolean, false},
        {"ByteArray", Identifiers.Byte, ubyte(0)},
        {"SByteArray", Identifiers.SByte, (byte) 0x00},
        {"Int16Array", Identifiers.Int16, (short) 16},
        {"Int32Array", Identifiers.Int32, 32},
        {"Int64Array", Identifiers.Int64, 64L},
        {"UInt16Array", Identifiers.UInt16, ushort(16)},
        {"UInt32Array", Identifiers.UInt32, uint(32)},
        {"UInt64Array", Identifiers.UInt64, ulong(64L)},
        {"FloatArray", Identifiers.Float, 3.14f},
        {"DoubleArray", Identifiers.Double, 3.14d},
        {"StringArray", Identifiers.String, "string value"},
        {"DateTimeArray", Identifiers.DateTime, new Variant(DateTime.now())},
        {"GuidArray", Identifiers.Guid, new Variant(UUID.randomUUID())},
        {"ByteStringArray", Identifiers.ByteString, new Variant(new ByteString(new byte[]{0x01, 0x02, 0x03, 0x04}))},
        {"XmlElementArray", Identifiers.XmlElement, new Variant(new XmlElement("<a>hello</a>"))},
        {"LocalizedTextArray", Identifiers.LocalizedText, new Variant(LocalizedText.english("localized text"))},
        {"QualifiedNameArray", Identifiers.QualifiedName, new Variant(new QualifiedName(1234, "defg"))},
        {"NodeIdArray", Identifiers.NodeId, new Variant(new NodeId(1234, "abcd"))}
    };


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Random random = new Random();

    private final SubscriptionModel subscriptionModel;

    private final OpcUaServer server;
    private final UShort namespaceIndex;
    
    private JSONObject jsSigsChans;

    
    //Creating the Namespace with name. 
    
    public SOSJOPCUAServerNamespaceForCD(OpcUaServer server, UShort namespaceIndex, String folderName, JSONObject jsSigsChans) {
        this.server = server;
        this.namespaceIndex = namespaceIndex;
        this.folderName = folderName;
        this.jsSigsChans = jsSigsChans;

        subscriptionModel = new SubscriptionModel(server, this);

        try {
            // Create a "HelloWorld" folder and add it to the node manager
            NodeId folderNodeId = new NodeId(namespaceIndex, folderName);

            UaFolderNode folderNode = new UaFolderNode(
                server.getNodeMap(),
                folderNodeId,
                new QualifiedName(namespaceIndex, folderName),
                LocalizedText.english(folderName)
            );

            server.getNodeMap().addNode(folderNode);

            // Make sure our new folder shows up under the server's Objects folder
            server.getUaNamespace().addReference(
                Identifiers.ObjectsFolder,
                Identifiers.Organizes,
                true,
                folderNodeId.expanded(),
                NodeClass.Object
            );

            // Add the rest of the nodes
            //addVariableNodes(folderNode);
            
            //for (int i=0;i<signals.size();i++) {
            
            
            
            try {
            	JSONObject jsSignals = jsSigsChans.getJSONObject("signals");
				JSONObject jsInputSigs = jsSignals.getJSONObject("inputs");
				JSONObject jsOutputSigs = jsSignals.getJSONObject("outputs");
				
				//add signal folder
				
				UaFolderNode SignalsNode = addHierarchyFolderNode(folderNode, "Signals", "Interfaces");
				
				// add signal direction folder
				
				UaFolderNode InSignalsNode = addHierarchyFolderNode(SignalsNode, "Inputs", "Signals");
				
				UaFolderNode OutSignalsNode = addHierarchyFolderNode(SignalsNode, "Outputs", "Signals");
				
				
				//For inputs
				
				   Enumeration inSigKeys = jsInputSigs.keys();
	            
		            while(inSigKeys.hasMoreElements()){
		               
		               String inSigName = inSigKeys.nextElement().toString();
		               
		             
		               addDynamicNodesForSignals(InSignalsNode, inSigName,"Input");
		               
		           }
		            
		            // for outputs
		            
		            Enumeration outSigKeys = jsOutputSigs.keys();
		            
		            while(outSigKeys.hasMoreElements()){
		               
		               String outSigName = outSigKeys.nextElement().toString();
		               
		               JSONObject jsOutSigDet = jsOutputSigs.getJSONObject(outSigName);
		               
		               String className = jsOutSigDet.getString("Class");
		               
		                  if(className.equalsIgnoreCase("systemj.signals.SOA.output.LocalWriteOPCUA") || className.equalsIgnoreCase("systemj.signals.SOA.output.RemoteReadOPCUASender")) {
		            	   
		                  } else {
		                	  
		                	  addDynamicNodesForSignals(OutSignalsNode, outSigName,"Output");
		                	  
		                  }
		               
		               
		               
		           }
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            
           // Enumeration keys = jsSigsChans.keys();
            
           // while(keys.hasMoreElements()){
           ///     
           //     String servInd = keys.nextElement().toString();
           // }
            
            /*
            Set<String> keys = signals.keySet();
            for(String key: keys){
            	String sigDirection = signals.get(key).toString();
                //System.out.println("Value of "+key+" is: "+hm.get(key));
                addDynamicNodesForSignals(folderNode, key,sigDirection);
            }
            */	
            	
            	//addDynamicNodesForSignals(folderNode, signalName);
            //}
            
            
            //addMethodNode(folderNode);
            
            //addGetServiceDescriptionMethodNode(folderNode);
        } catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
    }

    @Override
    public UShort getNamespaceIndex() {
        return namespaceIndex;
    }

    @Override
    public String getNamespaceUri() {
        return NAMESPACE_URI;
    }

    private void addVariableNodes(UaFolderNode rootNode) {
        //addArrayNodes(rootNode);
        //addScalarNodes(rootNode);
        //addAdminReadableNodes(rootNode);
        //addAdminWritableNodes(rootNode);
        //addDynamicNodes(rootNode);
    }

    private void addArrayNodes(UaFolderNode rootNode) {
        UaFolderNode arrayTypesFolder = new UaFolderNode(
            server.getNodeMap(),
            new NodeId(namespaceIndex, folderName+"/ArrayTypes"),
            new QualifiedName(namespaceIndex, "ArrayTypes"),
            LocalizedText.english("ArrayTypes")
        );

        server.getNodeMap().addNode(arrayTypesFolder);
        rootNode.addOrganizes(arrayTypesFolder);

        for (Object[] os : STATIC_ARRAY_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Object value = os[2];
            Object array = Array.newInstance(value.getClass(), 4);
            for (int i = 0; i < 4; i++) {
                Array.set(array, i, value);
            }
            Variant variant = new Variant(array);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/ArrayTypes/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setValueRank(ValueRank.OneDimension.getValue())
                .setArrayDimensions(new UInteger[]{uint(0)})
                .build();

            node.setValue(new DataValue(variant));

            node.setAttributeDelegate(new ValueLoggingDelegate());

            server.getNodeMap().addNode(node);
            arrayTypesFolder.addOrganizes(node);
        }
    }

    private void addScalarNodes(UaFolderNode rootNode) {
        UaFolderNode scalarTypesFolder = new UaFolderNode(
            server.getNodeMap(),
            new NodeId(namespaceIndex, folderName+"/ScalarTypes"),
            new QualifiedName(namespaceIndex, "ScalarTypes"),
            LocalizedText.english("ScalarTypes")
        );

        server.getNodeMap().addNode(scalarTypesFolder);
        rootNode.addOrganizes(scalarTypesFolder);

        for (Object[] os : STATIC_SCALAR_NODES) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            Variant variant = (Variant) os[2];

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/ScalarTypes/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(variant));

            node.setAttributeDelegate(new ValueLoggingDelegate());

            server.getNodeMap().addNode(node);
            scalarTypesFolder.addOrganizes(node);
        }
    }

    private void addAdminReadableNodes(UaFolderNode rootNode) {
        UaFolderNode adminFolder = new UaFolderNode(
            server.getNodeMap(),
            new NodeId(namespaceIndex, folderName+"/OnlyAdminCanRead"),
            new QualifiedName(namespaceIndex, "OnlyAdminCanRead"),
            LocalizedText.english("OnlyAdminCanRead")
        );

        server.getNodeMap().addNode(adminFolder);
        rootNode.addOrganizes(adminFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
            .setNodeId(new NodeId(namespaceIndex, folderName+"/OnlyAdminCanRead/" + name))
            .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
            .setBrowseName(new QualifiedName(namespaceIndex, name))
            .setDisplayName(LocalizedText.english(name))
            .setDataType(Identifiers.String)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();

        node.setValue(new DataValue(new Variant("shh... don't tell the lusers")));

        node.setAttributeDelegate(new RestrictedAccessDelegate(identity -> {
            if ("admin".equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.NONE;
            }
        }));

        server.getNodeMap().addNode(node);
        adminFolder.addOrganizes(node);
    }

    private void addAdminWritableNodes(UaFolderNode rootNode) {
        UaFolderNode adminFolder = new UaFolderNode(
            server.getNodeMap(),
            new NodeId(namespaceIndex, folderName+"/OnlyAdminCanWrite"),
            new QualifiedName(namespaceIndex, "OnlyAdminCanWrite"),
            LocalizedText.english("OnlyAdminCanWrite")
        );

        server.getNodeMap().addNode(adminFolder);
        rootNode.addOrganizes(adminFolder);

        String name = "String";
        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
            .setNodeId(new NodeId(namespaceIndex, folderName+"/OnlyAdminCanWrite/" + name))
            .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
            .setBrowseName(new QualifiedName(namespaceIndex, name))
            .setDisplayName(LocalizedText.english(name))
            .setDataType(Identifiers.String)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();

        node.setValue(new DataValue(new Variant("admin was here")));

        node.setAttributeDelegate(new RestrictedAccessDelegate(identity -> {
            if ("admin".equals(identity)) {
                return AccessLevel.READ_WRITE;
            } else {
                return AccessLevel.READ_ONLY;
            }
        }));

        server.getNodeMap().addNode(node);
        adminFolder.addOrganizes(node);
    }
    
    private UaFolderNode addHierarchyFolderNode(UaFolderNode rootNode, String Name, String precedingName) {
        UaFolderNode dynamicFolder = new UaFolderNode(
            server.getNodeMap(),
            new NodeId(namespaceIndex, rootNode+"/"+precedingName+"/"+Name),
            new QualifiedName(namespaceIndex, Name),
            LocalizedText.english(Name)
        );

        server.getNodeMap().addNode(dynamicFolder);
        rootNode.addOrganizes(dynamicFolder);
        return dynamicFolder;
    }

    private void addDynamicNodesForSignals(UaFolderNode rootNode, String signalName, String signalDirection) {
        UaFolderNode dynamicFolder = new UaFolderNode(
            server.getNodeMap(),
            new NodeId(namespaceIndex, folderName+"/Signals/"+signalDirection+"/"+signalName),
            new QualifiedName(namespaceIndex, signalName),
            LocalizedText.english(signalName)
        );

        server.getNodeMap().addNode(dynamicFolder);
        rootNode.addOrganizes(dynamicFolder);

        
        
        /*// Dynamic Boolean
        {
            String name = "Boolean";
            NodeId typeId = Identifiers.Boolean;
            Variant variant = new Variant(false);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/Dynamic/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(variant));

            AttributeDelegate delegate = AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(random.nextBoolean()));
                    }
                },
                ValueLoggingDelegate::new
            );

            node.setAttributeDelegate(delegate);

            server.getNodeMap().addNode(node);
            dynamicFolder.addOrganizes(node);
        }

        // Dynamic Int32
        {
            String name = "Int32";
            NodeId typeId = Identifiers.Int32;
            Variant variant = new Variant(0);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/Dynamic/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(variant));

            AttributeDelegate delegate = AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(random.nextInt()));
                    }
                },
                ValueLoggingDelegate::new
            );

            node.setAttributeDelegate(delegate);

            server.getNodeMap().addNode(node);
            dynamicFolder.addOrganizes(node);
        }

        // Dynamic Double
        {
            String name = "Double";
            NodeId typeId = Identifiers.Double;
            Variant variant = new Variant(0.0);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/Dynamic/" + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(variant));

            AttributeDelegate delegate = AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(random.nextDouble()));
                    }
                },
                ValueLoggingDelegate::new
            );

            node.setAttributeDelegate(delegate);

            server.getNodeMap().addNode(node);
            dynamicFolder.addOrganizes(node);
        }
        */
        
        //Signal Name, Type String
        {
            String name = signalName;
            NodeId typeId = Identifiers.String;
            Variant variant = new Variant(signalName);

            UaVariableNode nodeSignalName = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/Signals/"+signalDirection+"/"+name+"/Name"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_ONLY)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_ONLY)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english("Name"))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            nodeSignalName.setValue(new DataValue(variant));

            /*
            AttributeDelegate delegate = AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(random.nextBoolean()));
                    }
                },
                ValueLoggingDelegate::new
            );
            
            nodeSignalName.setAttributeDelegate(delegate);
            
            */
            nodeSignalName.setAttributeDelegate(new ValueLoggingDelegate());

            

            server.getNodeMap().addNode(nodeSignalName);
            
            dynamicFolder.addOrganizes(nodeSignalName);
            
            AddNodeObjToStorage(signalName+":Name", nodeSignalName);
            AddSignalDirectionEntry(signalName, signalDirection);
            
        }
        
      //Signal Status, Type Boolean
        {
            //String name = signalName;
            NodeId typeId = Identifiers.Boolean;
            Variant variant = new Variant(false);

            UaVariableNode nodeSignalStatus = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
            	.setNodeId(new NodeId(namespaceIndex, folderName+"/Signals/"+signalDirection+"/"+signalName+"/Status"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, signalName+":Status"))
                .setDisplayName(LocalizedText.english("Status"))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            nodeSignalStatus.setValue(new DataValue(variant));

            /*
            AttributeDelegate delegate = AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(random.nextBoolean()));
                    }
                },
                ValueLoggingDelegate::new
            );
			
			
            nodeSignalStatus.setAttributeDelegate(delegate);
            */
            
            
            nodeSignalStatus.setAttributeDelegate(new ValueLoggingDelegate());

            server.getNodeMap().addNode(nodeSignalStatus);
            
            dynamicFolder.addOrganizes(nodeSignalStatus);
            
            AddNodeObjToStorage(signalName+":Status", nodeSignalStatus);
        }
        
        //Signal Value, Type String
        {
            //String name = "value";
            NodeId typeId = Identifiers.String;
            Variant variant = new Variant("");

            UaVariableNode nodeSignalValue = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, folderName+"/Signals/"+signalDirection+"/"+signalName+"/Value"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, signalName+":Value"))
                .setDisplayName(LocalizedText.english("Value"))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            nodeSignalValue.setValue(new DataValue(variant));

            
            /*
            AttributeDelegate delegate = AttributeDelegateChain.create(
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(random.nextBoolean()));
                    }
                },
                ValueLoggingDelegate::new
            );

            nodeSignalValue.setAttributeDelegate(delegate);
            
            */
            
            nodeSignalValue.setAttributeDelegate(new ValueLoggingDelegate());

            server.getNodeMap().addNode(nodeSignalValue);
            dynamicFolder.addOrganizes(nodeSignalValue);
            
            AddNodeObjToStorage(signalName+":Value", nodeSignalValue);
        }
        
    }
    
    /*
    private void addMethodNode(UaFolderNode folderNode) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
            .setNodeId(new NodeId(namespaceIndex, folderName+"/sqrt(x)"))
            .setBrowseName(new QualifiedName(namespaceIndex, "sqrt(x)"))
            .setDisplayName(new LocalizedText(null, "sqrt(x)"))
            .setDescription(
                LocalizedText.english("Returns the correctly rounded positive square root of a double value."))
            .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                AnnotationBasedInvocationHandler.fromAnnotatedObject(
                    server.getNodeMap(), new SqrtMethod());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
            ));

            methodNode.addReference(new Reference(
                methodNode.getNodeId(),
                Identifiers.HasComponent,
                folderNode.getNodeId().expanded(),
                folderNode.getNodeClass(),
                false
            ));
        } catch (Exception e) {
            logger.error("Error creating sqrt() method.", e);
        }
    }
    */
    
    /*
    private void addGetServiceDescriptionMethodNode(UaFolderNode folderNode) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
            .setNodeId(new NodeId(namespaceIndex, folderName+"/getServiceDescription()"))
            .setBrowseName(new QualifiedName(namespaceIndex, "getServiceDescription()"))
            .setDisplayName(new LocalizedText(null, "getServiceDescription()"))
            .setDescription(
                LocalizedText.english("Returns the service description."))
            .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                AnnotationBasedInvocationHandler.fromAnnotatedObject(
                    server.getNodeMap(), new GetServiceDescription());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
            ));

            methodNode.addReference(new Reference(
                methodNode.getNodeId(),
                Identifiers.HasComponent,
                folderNode.getNodeId().expanded(),
                folderNode.getNodeClass(),
                false
            ));
        } catch (Exception e) {
            logger.error("Error creating getServiceDescription() method.", e);
        }
    }
    */

    @Override
    public CompletableFuture<List<Reference>> browse(AccessContext context, NodeId nodeId) {
        ServerNode node = server.getNodeMap().get(nodeId);

        if (node != null) {
            return CompletableFuture.completedFuture(node.getReferences());
        } else {
            return FutureUtils.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
        }
    }

    @Override
    public void read(
        ReadContext context,
        Double maxAge,
        TimestampsToReturn timestamps,
        List<ReadValueId> readValueIds) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId readValueId : readValueIds) {
            ServerNode node = server.getNodeMap().get(readValueId.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                    new AttributeContext(context),
                    readValueId.getAttributeId(),
                    timestamps,
                    readValueId.getIndexRange()
                );

                results.add(value);
            } else {
                results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            ServerNode node = server.getNodeMap().get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(
                        new AttributeContext(context),
                        writeValue.getAttributeId(),
                        writeValue.getValue(),
                        writeValue.getIndexRange()
                    );

                    results.add(StatusCode.GOOD);

                    logger.info(
                        "Wrote value {} to {} attribute of {}",
                        writeValue.getValue().getValue(),
                        AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
                        node.getNodeId());
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
        Optional<ServerNode> node = server.getNodeMap().getNode(methodId);

        return node.flatMap(n -> {
            if (n instanceof UaMethodNode) {
                return ((UaMethodNode) n).getInvocationHandler();
            } else {
                return Optional.empty();
            }
        });
    }
    
    public void AddNodeObjToStorage(String nodeSignalName, UaVariableNode node) {
    	synchronized(SOSJSignalNodesLock) {
    		SOSJSignalNodes.put(nodeSignalName, node);
    	}
    }
    
    public UaVariableNode GetNodeObjFromStorage(String nodeSignalName) {
    	synchronized(SOSJSignalNodesLock) {
    		return (UaVariableNode) SOSJSignalNodes.get(nodeSignalName);
    	}
    }
    
    public void AddSignalDirectionEntry(String signalName, String direction) {
    	synchronized(SOSJSignalNodeDirectionsLock) {
    		SOSJSignalNodeDirections.put(signalName, direction);
    	}
    }
    
    public String GetSignalDirection(String signalName) {
    	synchronized(SOSJSignalNodeDirectionsLock) {
    		return SOSJSignalNodeDirections.get(signalName).toString();
    	}
    }
    
    public Hashtable GetAllSignalDirection() {
    	synchronized(SOSJSignalNodeDirectionsLock) {
    		return SOSJSignalNodeDirections;
    	}
    }

}
