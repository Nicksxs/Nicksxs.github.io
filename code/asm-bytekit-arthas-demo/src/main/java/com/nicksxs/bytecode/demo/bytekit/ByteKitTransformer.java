package com.nicksxs.bytecode.demo.bytekit;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;

public class ByteKitTransformer implements ClassFileTransformer {

    private static final String TARGET_CLASS = "com/nicksxs/bytecode/demo/app/DemoService";
    private static final String TARGET_METHOD = "placeOrder";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!TARGET_CLASS.equals(className)) {
            return null;
        }

        try {
            System.out.println("[ByteKit agent] transforming " + className);
            DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
            List<InterceptorProcessor> interceptors = parser.parse(DemoInterceptor.class);

            ClassNode classNode = new ClassNode();
            ClassReader reader = AsmUtils.toClassNode(classfileBuffer, classNode);
            for (MethodNode methodNode : classNode.methods) {
                if (!TARGET_METHOD.equals(methodNode.name)) {
                    continue;
                }
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                for (InterceptorProcessor interceptor : interceptors) {
                    interceptor.process(methodProcessor);
                }
            }
            return AsmUtils.toBytes(classNode, loader, reader);
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);
            return null;
        }
    }
}
