package src.dao;

import src.model.Order;
import src.model.OrderItem;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderDAO {

    private final DataSource dataSource;

    public OrderDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int placeOrder(int customerId, List<OrderItem> orderItems) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double total = orderItems.stream()
                    .mapToDouble(oi -> oi.getPriceAtOrder() * oi.getQuantity()).sum();

                int generatedOrderId;
                String insertOrderSql = "INSERT INTO orders (customer_id, order_date, total_amount) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setDate(2, Date.valueOf(LocalDate.now()));
                    ps.setDouble(3, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        generatedOrderId = keys.getInt(1);
                    }
                }

                String insertItemSql = "INSERT INTO order_items (order_id, item_id, quantity, price_at_order) VALUES (?, ?, ?, ?)";
                String updateStockSql = "UPDATE items SET quantity = quantity - ? WHERE item_id = ? AND quantity >= ?";

                try (PreparedStatement itemPs = conn.prepareStatement(insertItemSql);
                     PreparedStatement stockPs = conn.prepareStatement(updateStockSql)) {
                    for (OrderItem oi : orderItems) {
                        itemPs.setInt(1, generatedOrderId);
                        itemPs.setInt(2, oi.getItemId());
                        itemPs.setInt(3, oi.getQuantity());
                        itemPs.setDouble(4, oi.getPriceAtOrder());
                        itemPs.addBatch();

                        stockPs.setInt(1, oi.getQuantity());
                        stockPs.setInt(2, oi.getItemId());
                        stockPs.setInt(3, oi.getQuantity());
                        stockPs.addBatch();
                    }
                    itemPs.executeBatch();
                    int[] stockResults = stockPs.executeBatch();
                    for (int result : stockResults) {
                        if (result == 0) throw new SQLException("Insufficient stock for one or more items.");
                    }
                }

                conn.commit();
                return generatedOrderId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Order getOrderById(int orderId) throws SQLException {
        String sql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, " +
                     "o.order_date, o.total_amount FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.customer_id WHERE o.order_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrderRow(rs);
                    order.setOrderItems(getOrderItems(conn, orderId));
                    return order;
                }
            }
        }
        return null;
    }

    private List<OrderItem> getOrderItems(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.*, i.name AS item_name FROM order_items oi " +
                     "JOIN items i ON oi.item_id = i.item_id WHERE oi.order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem oi = new OrderItem();
                    oi.setOrderItemId(rs.getInt("order_item_id"));
                    oi.setOrderId(rs.getInt("order_id"));
                    oi.setItemId(rs.getInt("item_id"));
                    oi.setItemName(rs.getString("item_name"));
                    oi.setQuantity(rs.getInt("quantity"));
                    oi.setPriceAtOrder(rs.getDouble("price_at_order"));
                    items.add(oi);
                }
            }
        }
        return items;
    }

    public List<String> getCustomersWhoPurchased() throws SQLException {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT c.name FROM customers c " +
                     "JOIN orders o ON c.customer_id = o.customer_id ORDER BY c.name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString("name"));
        }
        return names;
    }

    public List<Order> getOrdersWithinDays(int days) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, " +
                     "o.order_date, o.total_amount FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.customer_id " +
                     "WHERE o.order_date >= CURDATE() - INTERVAL ? DAY ORDER BY o.order_date DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) orders.add(mapOrderRow(rs));
            }
        }
        return orders;
    }

    public Order getHighestOrder() throws SQLException { return getExtremeOrder("MAX"); }
    public Order getLowestOrder() throws SQLException { return getExtremeOrder("MIN"); }

    private Order getExtremeOrder(String func) throws SQLException {
        String sql = "SELECT o.order_id, o.customer_id, c.name AS customer_name, " +
                     "o.order_date, o.total_amount FROM orders o " +
                     "JOIN customers c ON o.customer_id = c.customer_id " +
                     "WHERE o.total_amount = (SELECT " + func + "(total_amount) FROM orders) LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return mapOrderRow(rs);
        }
        return null;
    }

    private Order mapOrderRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setCustomerId(rs.getInt("customer_id"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setOrderDate(rs.getDate("order_date").toLocalDate());
        order.setTotalAmount(rs.getDouble("total_amount"));
        return order;
    }
}