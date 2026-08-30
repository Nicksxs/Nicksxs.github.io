package com.nicksxs.bytecode.demo.bytekit;

import java.util.Arrays;

import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;

public class DemoInterceptor {

    @AtEnter(inline = true)
    public static void atEnter(@Binding.MethodName String methodName, @Binding.Args Object[] args) {
        System.out.println("[ByteKit] enter " + methodName + " args=" + Arrays.toString(args));
    }

    @AtExit(inline = true)
    public static void atExit(@Binding.Return Object returnObject) {
        System.out.println("[ByteKit] return " + returnObject);
    }

    @AtExceptionExit(inline = true, onException = Throwable.class)
    public static void atExceptionExit(@Binding.Throwable Throwable throwable) {
        System.out.println("[ByteKit] throw " + throwable.getClass().getSimpleName()
                + ": " + throwable.getMessage());
    }
}
