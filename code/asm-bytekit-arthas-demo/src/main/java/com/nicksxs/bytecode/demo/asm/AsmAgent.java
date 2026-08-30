package com.nicksxs.bytecode.demo.asm;

import java.lang.instrument.Instrumentation;

public class AsmAgent {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[ASM agent] installed");
        instrumentation.addTransformer(new AsmTimingTransformer(), true);
    }
}
