package src.dao;

import src.model.Item;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ItemDAO {

    private final DataSource dataSource;

    public ItemDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addItem(Item item) throws SQLException {
        String sql = "INSERT INTO items (name, price, quantity, reorder_level) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setDouble(2, item.getPrice());
            ps.setInt(3, item.getQuantity());
            ps.setInt(4, item.getReorderLevel());
            ps.executeUpdate();
        }
    }

    public List<Item> getAllItems() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items ORDER BY item_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        }
        return items;
    }

    public Item getItemById(int itemId) throws SQLException {
        String sql = "SELECT * FROM items WHERE item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Item> searchByName(String name) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE name LIKE ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> searchByPriceRange(double min, double max) throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE price BETWEEN ? AND ? ORDER BY price";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, min);
            ps.setDouble(2, max);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> getItemsNeedingReorder() throws SQLException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE quantity < reorder_level";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(mapRow(rs));
        }
        return items;
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        return new Item(
            rs.getInt("item_id"), rs.getString("name"),
            rs.getDouble("price"), rs.getInt("quantity"),
            rs.getInt("reorder_level")
        );
    }
}