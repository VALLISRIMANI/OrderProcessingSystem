package src;

import src.dao.CustomerDAO;
import src.dao.ItemDAO;
import src.dao.OrderDAO;
import src.model.Customer;
import src.model.Item;
import src.model.Order;
import src.model.OrderItem;
import src.util.DBConnection;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OrderProcessingSystem {

    private static final Scanner sc = new Scanner(System.in);
    private static final ItemDAO itemDAO = new ItemDAO();
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final OrderDAO orderDAO = new OrderDAO();

    public static void main(String[] args) {
        System.out.println("\n=== ORDER PROCESSING SYSTEM (JDBC + MySQL) ===\n");

        while (true) {
            System.out.println("\n---------------- MAIN MENU ----------------");
            System.out.println("1. Item Management");
            System.out.println("2. Customer Management");
            System.out.println("3. Order Management");
            System.out.println("4. Reports");
            System.out.println("0. Exit");
            System.out.print("\nEnter your choice: ");

            int choice = getIntInput();
            switch (choice) {
                case 1 -> itemMenu();
                case 2 -> customerMenu();
                case 3 -> orderMenu();
                case 4 -> reportMenu();
                case 0 -> {
                    DBConnection.closeConnection();
                    System.exit(0);
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    // ---------------- ITEM MENU ----------------
    private static void itemMenu() {
        while (true) {
            System.out.println("\n---------------- ITEM MANAGEMENT ----------------");
            System.out.println("1. View Items\n2. Add Item\n3. Search by Name\n4. Price Range\n5. Reorder Items\n0. Back");
            System.out.print("Enter your choice: ");
            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1 -> viewItems(itemDAO.getAllItems());
                    case 2 -> addItem();
                    case 3 -> {
                        System.out.print("Enter name: ");
                        viewItems(itemDAO.searchByName(sc.nextLine().trim()));
                    }
                    case 4 -> {
                        System.out.print("Min: ");
                        double min = getDoubleInput();
                        System.out.print("Max: ");
                        double max = getDoubleInput();
                        viewItems(itemDAO.searchByPriceRange(min, max));
                    }
                    case 5 -> viewItems(itemDAO.getItemsNeedingReorder());
                    case 0 -> { return; }
                    default -> System.out.println("Invalid choice!");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void addItem() throws SQLException {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Price: ");
        double price = getDoubleInput();
        System.out.print("Quantity: ");
        int qty = getIntInput();
        System.out.print("Reorder Level: ");
        int reorderLevel = getIntInput();

        itemDAO.addItem(new Item(name, price, qty, reorderLevel));
        System.out.println("Item added!");
    }

    private static void viewItems(List<Item> items) {
        if (items.isEmpty()) {
            System.out.println("No items found!");
            return;
        }
        System.out.printf("%-5s %-20s %-12s %-8s %-8s%n", "ID", "Name", "Price", "Qty", "Reorder");
        for (Item i : items) {
            System.out.printf("%-5d %-20s %-12.2f %-8d %-8d%n",
                    i.getId(), i.getName(), i.getPrice(), i.getQuantity(), i.getReorderLevel());
        }
    }

    // ---------------- CUSTOMER MENU ----------------
    private static void customerMenu() {
        while (true) {
            System.out.println("\n---------------- CUSTOMER MANAGEMENT ----------------");
            System.out.println("1. Add Customer\n2. View Customers\n0. Back");
            System.out.print("Enter your choice: ");
            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1 -> addCustomer();
                    case 2 -> viewCustomers();
                    case 0 -> { return; }
                    default -> System.out.println("Invalid choice!");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void addCustomer() throws SQLException {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Address: ");
        String address = sc.nextLine().trim();
        System.out.print("Phone: ");
        String phone = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();

        customerDAO.addCustomer(new Customer(name, address, phone, email));
        System.out.println("Customer added!");
    }

    private static void viewCustomers() throws SQLException {
        List<Customer> customers = customerDAO.getAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("No customers found!");
            return;
        }
        System.out.printf("%-5s %-20s %-25s%n", "ID", "Name", "Email");
        for (Customer c : customers) {
            System.out.printf("%-5d %-20s %-25s%n", c.getId(), c.getName(), c.getEmail());
        }
    }

    // ---------------- ORDER MENU ----------------
    private static void orderMenu() {
        System.out.println("\n---------------- ORDER MANAGEMENT ----------------");
        try {
            System.out.print("Enter Customer ID: ");
            int customerId = getIntInput();
            if (customerDAO.getCustomerById(customerId) == null) {
                System.out.println("Customer not found!");
                return;
            }

            List<OrderItem> orderItems = new ArrayList<>();
            while (true) {
                System.out.print("Enter Item ID (0 to stop): ");
                int itemId = getIntInput();
                if (itemId == 0) break;

                Item item = itemDAO.getItemById(itemId);
                if (item == null) {
                    System.out.println("Item not found!");
                    continue;
                }
                System.out.print("Quantity: ");
                int qty = getIntInput();
                orderItems.add(new OrderItem(itemId, qty, item.getPrice()));
            }

            if (orderItems.isEmpty()) {
                System.out.println("Order cancelled - no items selected.");
                return;
            }

            int orderId = orderDAO.placeOrder(customerId, orderItems);
            Order placed = orderDAO.getOrderById(orderId);
            System.out.printf("Order placed! Order ID: %d, Total: %.2f%n", orderId, placed.getTotalAmount());

        } catch (SQLException e) {
            System.out.println("Order failed: " + e.getMessage());
        }
    }

    // ---------------- REPORTS MENU ----------------
    private static void reportMenu() {
        while (true) {
            System.out.println("\n---------------- REPORTS ----------------");
            System.out.println("1. Find Order by ID\n2. Customers Who Purchased\n3. Last Week Orders\n4. Last Month Orders\n5. Highest/Lowest Order\n0. Back");
            System.out.print("Enter your choice: ");
            int choice = getIntInput();
            try {
                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter Order ID: ");
                        Order o = orderDAO.getOrderById(getIntInput());
                        if (o == null) System.out.println("Order not found!");
                        else printOrder(o);
                    }
                    case 2 -> {
                        List<String> names = orderDAO.getCustomersWhoPurchased();
                        names.forEach(System.out::println);
                    }
                    case 3 -> printOrders(orderDAO.getOrdersWithinDays(7));
                    case 4 -> printOrders(orderDAO.getOrdersWithinDays(30));
                    case 5 -> {
                        Order highest = orderDAO.getHighestOrder();
                        Order lowest = orderDAO.getLowestOrder();
                        System.out.printf("Highest -> OrderID: %d, Total: %.2f%n", highest.getOrderId(), highest.getTotalAmount());
                        System.out.printf("Lowest  -> OrderID: %d, Total: %.2f%n", lowest.getOrderId(), lowest.getTotalAmount());
                    }
                    case 0 -> { return; }
                    default -> System.out.println("Invalid choice!");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static void printOrder(Order o) {
        System.out.printf("OrderID: %d | Customer: %s | Date: %s | Total: %.2f%n",
                o.getOrderId(), o.getCustomerName(), o.getOrderDate(), o.getTotalAmount());
        if (o.getOrderItems() != null) {
            for (OrderItem oi : o.getOrderItems()) {
                System.out.printf("   - %s x%d @ %.2f%n", oi.getItemName(), oi.getQuantity(), oi.getPriceAtOrder());
            }
        }
    }

    private static void printOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("No orders found!");
            return;
        }
        for (Order o : orders) {
            System.out.printf("OrderID: %d | Customer: %s | Date: %s | Total: %.2f%n",
                    o.getOrderId(), o.getCustomerName(), o.getOrderDate(), o.getTotalAmount());
        }
    }

    // ---------------- INPUT UTILITIES ----------------
    private static int getIntInput() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input! Enter a number: ");
            }
        }
    }

    private static double getDoubleInput() {
        while (true) {
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input! Enter a decimal number: ");
            }
        }
    }
}