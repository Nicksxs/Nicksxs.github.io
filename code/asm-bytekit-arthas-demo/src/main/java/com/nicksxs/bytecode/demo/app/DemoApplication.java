package com.nicksxs.bytecode.demo.app;

import java.lang.management.ManagementFactory;

public class DemoApplication {

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        long pauseMillis = args.length > 1 ? Long.parseLong(args[1]) : 500L;

        System.out.println("demo pid=" + currentPid());
        System.out.println("iterations=" + (iterations == 0 ? "infinite" : iterations));

        DemoService service = new DemoService();
        int index = 1;
        while (iterations == 0 || index <= iterations) {
            String user = index % 4 == 0 ? "error" : "user-" + index;
            try {
                String result = service.placeOrder(user, index);
                System.out.println("[APP] " + result);
            } catch (Exception e) {
                System.out.println("[APP] failed: " + e.getMessage());
            }
            index++;
            Thread.sleep(pauseMillis);
        }
    }

    private static String currentPid() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int separator = runtimeName.indexOf('@');
        return separator > 0 ? runtimeName.substring(0, separator) : runtimeName;
    }
}
