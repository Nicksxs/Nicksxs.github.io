package com.nicksxs.bytecode.demo.bytekit;

import java.lang.instrument.Instrumentation;

public class ByteKitAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[ByteKit agent] installed");
        instrumentation.addTransformer(new ByteKitTransformer(), true);
    }
}
