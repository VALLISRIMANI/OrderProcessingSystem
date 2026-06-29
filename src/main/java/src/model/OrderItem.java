package src.model;


public class OrderItem {
    private int orderItemId;
    private int orderId;
    private int itemId;
    private String itemName;   // populated via JOIN for display purposes
    private int quantity;
    private double priceAtOrder;

    public OrderItem() {
    }

    public OrderItem(int itemId, int quantity, double priceAtOrder) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceAtOrder = priceAtOrder;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceAtOrder() {
        return priceAtOrder;
    }

    public void setPriceAtOrder(double priceAtOrder) {
        this.priceAtOrder = priceAtOrder;
    }
}