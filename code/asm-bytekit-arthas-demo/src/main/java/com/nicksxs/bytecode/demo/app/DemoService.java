package com.nicksxs.bytecode.demo.app;

public class DemoService {

    public String placeOrder(String user, int quantity) {
        if ("error".equals(user)) {
            throw new IllegalArgumentException("user cannot be error");
        }

        validateQuantity(quantity);
        String sku = queryInventory(user);
        return saveOrder(user, sku, quantity);
    }

    private void validateQuantity(int quantity) {
        sleep(20L);
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    private String queryInventory(String user) {
        sleep(40L);
        return "SKU-" + user.toUpperCase();
    }

    private String saveOrder(String user, String sku, int quantity) {
        sleep(30L);
        return "order created: user=" + user + ", sku=" + sku + ", quantity=" + quantity;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        }
    }
}
