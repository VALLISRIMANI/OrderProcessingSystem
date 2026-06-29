package src.model;

public class Item {
    private int id;
    private String name;
    private double price;
    private int quantity;
    private int reorderLevel;

    public Item() {
    }

    public Item(int id, String name, double price, int quantity, int reorderLevel) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    // Constructor without id, used before insert
    public Item(String name, double price, int quantity, int reorderLevel) {
        this(0, name, price, quantity, reorderLevel);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}