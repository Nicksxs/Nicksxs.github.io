package com.nicksxs.bytecode.demo.asm;

import java.io.PrintStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

public class AsmTimingTransformer implements ClassFileTransformer {

    private static final String TARGET_CLASS = "com/nicksxs/bytecode/demo/app/DemoService";
    private static final String TARGET_METHOD = "placeOrder";
    private static final String TARGET_DESCRIPTOR = "(Ljava/lang/String;I)Ljava/lang/String;";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!TARGET_CLASS.equals(className)) {
            return null;
        }

        System.out.println("[ASM agent] transforming " + className);
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!TARGET_METHOD.equals(name) || !TARGET_DESCRIPTOR.equals(descriptor)) {
                    return methodVisitor;
                }
                return new TimingAdvice(methodVisitor, access, name, descriptor);
            }
        };
        reader.accept(visitor, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class TimingAdvice extends AdviceAdapter {

        private static final Type SYSTEM_TYPE = Type.getType(System.class);
        private static final Type PRINT_STREAM_TYPE = Type.getType(PrintStream.class);
        private static final Type STRING_BUILDER_TYPE = Type.getType(StringBuilder.class);
        private static final Type STRING_TYPE = Type.getType(String.class);

        private static final Method NANO_TIME = new Method("nanoTime", Type.LONG_TYPE, new Type[0]);
        private static final Method STRING_BUILDER_INIT = new Method("<init>", Type.VOID_TYPE, new Type[0]);
        private static final Method APPEND_STRING = new Method("append", STRING_BUILDER_TYPE,
                new Type[] { STRING_TYPE });
        private static final Method APPEND_INT = new Method("append", STRING_BUILDER_TYPE,
                new Type[] { Type.INT_TYPE });
        private static final Method APPEND_LONG = new Method("append", STRING_BUILDER_TYPE,
                new Type[] { Type.LONG_TYPE });
        private static final Method TO_STRING = new Method("toString", STRING_TYPE, new Type[0]);
        private static final Method PRINTLN = new Method("println", Type.VOID_TYPE, new Type[] { STRING_TYPE });

        private int startNanosLocal;

        private TimingAdvice(MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
        }

        @Override
        protected void onMethodEnter() {
            invokeStatic(SYSTEM_TYPE, NANO_TIME);
            startNanosLocal = newLocal(Type.LONG_TYPE);
            storeLocal(startNanosLocal);

            getStatic(SYSTEM_TYPE, "out", PRINT_STREAM_TYPE);
            newInstance(STRING_BUILDER_TYPE);
            dup();
            invokeConstructor(STRING_BUILDER_TYPE, STRING_BUILDER_INIT);
            push("[ASM] enter placeOrder user=");
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_STRING);
            loadArg(0);
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_STRING);
            push(", quantity=");
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_STRING);
            loadArg(1);
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_INT);
            invokeVirtual(STRING_BUILDER_TYPE, TO_STRING);
            invokeVirtual(PRINT_STREAM_TYPE, PRINTLN);
        }

        @Override
        protected void onMethodExit(int opcode) {
            invokeStatic(SYSTEM_TYPE, NANO_TIME);
            loadLocal(startNanosLocal);
            math(SUB, Type.LONG_TYPE);
            int elapsedLocal = newLocal(Type.LONG_TYPE);
            storeLocal(elapsedLocal);

            getStatic(SYSTEM_TYPE, "out", PRINT_STREAM_TYPE);
            newInstance(STRING_BUILDER_TYPE);
            dup();
            invokeConstructor(STRING_BUILDER_TYPE, STRING_BUILDER_INIT);
            push(opcode == ATHROW ? "[ASM] throw placeOrder cost=" : "[ASM] return placeOrder cost=");
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_STRING);
            loadLocal(elapsedLocal);
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_LONG);
            push(" ns");
            invokeVirtual(STRING_BUILDER_TYPE, APPEND_STRING);
            invokeVirtual(STRING_BUILDER_TYPE, TO_STRING);
            invokeVirtual(PRINT_STREAM_TYPE, PRINTLN);
        }
    }
}
