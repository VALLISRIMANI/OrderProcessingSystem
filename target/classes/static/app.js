/* ==========================================================
   ORDER PROCESSING SYSTEM - ROBUST JAVASCRIPT (CRIMSON ACCENT)
   ========================================================== */

const API_BASE = 'http://localhost:9090/api';

// Cached application data
let allItems = [];
let allCustomers = [];
let allOrders = [];

// ==========================================================
// INITIALIZATION
// ==========================================================

document.addEventListener('DOMContentLoaded', () => {
    setupSidebarNavigation();
    setupFormListeners();
    loadInitialData();
});

// Setup sidebar Tab switching
function setupSidebarNavigation() {
    const navButtons = document.querySelectorAll('.nav-menu .nav-btn');
    const sections = document.querySelectorAll('main .section');
    const pageTitle = document.getElementById('pageTitle');
    const pageSubtitle = document.getElementById('pageSubtitle');

    const headersInfo = {
        items: { title: "Item Management", subtitle: "Register, filter, and inspect store items" },
        customers: { title: "Customer Registry", subtitle: "Manage and register customer contact channels" },
        orders: { title: "Order Sales System", subtitle: "Build client orders and inspect purchase records" },
        reports: { title: "Reports & Analytics", subtitle: "Inspect financial summaries, active accounts, and warning lines" }
    };

    navButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const targetSection = btn.dataset.section;

            // Update active states
            navButtons.forEach(b => b.classList.remove('active'));
            sections.forEach(s => s.classList.remove('active'));

            btn.classList.add('active');
            const sectionEl = document.getElementById(targetSection);
            if (sectionEl) sectionEl.classList.add('active');

            // Update page titles
            if (headersInfo[targetSection]) {
                pageTitle.textContent = headersInfo[targetSection].title;
                pageSubtitle.textContent = headersInfo[targetSection].subtitle;
            }

            // Custom section re-queries
            if (targetSection === 'items') getAllItems();
            if (targetSection === 'customers') getAllCustomers();
            if (targetSection === 'orders') {
                getAllItems();
                getAllCustomers();
                getAllOrders();
            }
            if (targetSection === 'reports') {
                getAllOrders().then(() => {
                    updateOverviewStats();
                });
            }
        });
    });
}

// Setup Event Listeners on Forms
function setupFormListeners() {
    document.getElementById('itemForm').addEventListener('submit', handleAddItem);
    document.getElementById('customerForm').addEventListener('submit', handleAddCustomer);
    document.getElementById('orderForm').addEventListener('submit', handlePlaceOrder);
}

// Initial Sync from API database
function loadInitialData() {
    getAllItems();
    getAllCustomers();
    getAllOrders();
}

// ==========================================================
// API CLIENT IMPLEMENTATIONS & DOM RENDERING
// ==========================================================

// --- ITEMS SECTION ---

async function handleAddItem(e) {
    e.preventDefault();
    const item = {
        name: document.getElementById('itemName').value.trim(),
        price: parseFloat(document.getElementById('itemPrice').value),
        quantity: parseInt(document.getElementById('itemQty').value),
        reorderLevel: parseInt(document.getElementById('itemReorder').value)
    };

    try {
        const response = await fetch(`${API_BASE}/items`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(item)
        });

        if (response.ok) {
            showToast('Item registered successfully!', 'success');
            e.target.reset();
            await getAllItems();
        } else {
            const err = await response.json();
            showToast(err.error || 'Failed to register item.', 'error');
        }
    } catch (error) {
        showToast(`Server unreachable: ${error.message}`, 'error');
    }
}

async function getAllItems() {
    try {
        const response = await fetch(`${API_BASE}/items`);
        if (!response.ok) throw new Error('API query failed');
        allItems = await response.json();
        renderItemsTable(allItems);
        updateOrderItemDropdowns();
    } catch (error) {
        showToast(`Error syncing items: ${error.message}`, 'error');
    }
}

async function searchItemByName() {
    const query = document.getElementById('searchName').value.trim();
    if (!query) {
        getAllItems();
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/items/search?name=${encodeURIComponent(query)}`);
        if (!response.ok) throw new Error('Query error');
        const items = await response.json();
        renderItemsTable(items);
    } catch (error) {
        showToast(`Failed search filtering: ${error.message}`, 'error');
    }
}

async function searchByPriceRange() {
    const minVal = parseFloat(document.getElementById('minPrice').value);
    const maxVal = parseFloat(document.getElementById('maxPrice').value);

    if (isNaN(minVal) || isNaN(maxVal)) {
        showToast('Please enter both min and max boundary prices', 'error');
        return;
    }

    try {
        // FIXED ENDPOINT AND PARAMS IN ALIGNMENT WITH SPRING CONTROLLER: /api/items/price-range?min=X&max=Y
        const response = await fetch(`${API_BASE}/items/price-range?min=${minVal}&max=${maxVal}`);
        if (!response.ok) throw new Error('Pricing range API error');
        const items = await response.json();
        renderItemsTable(items);
    } catch (error) {
        showToast(`Filter error: ${error.message}`, 'error');
    }
}

async function getReorderItems() {
    try {
        const response = await fetch(`${API_BASE}/items/reorder`);
        if (!response.ok) throw new Error('Reorder API query failed');
        const items = await response.json();
        if (items.length === 0) {
            showToast('Zero items need stock orders currently.', 'info');
        } else {
            showToast(`Found ${items.length} items needing reorders`, 'info');
        }
        renderItemsTable(items);
    } catch (error) {
        showToast(`Reorder query failed: ${error.message}`, 'error');
    }
}

function renderItemsTable(items) {
    const tbody = document.getElementById('itemsBody');
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-msg">No items in database</td></tr>';
        return;
    }

    tbody.innerHTML = items.map(item => {
        const isLowStock = item.quantity < item.reorderLevel;
        const statusBadge = isLowStock 
            ? `<span class="badge badge-warning">Reorder</span>` 
            : `<span class="badge badge-success">OK</span>`;

        return `
            <tr class="${isLowStock ? 'warning-row' : ''}">
                <td><strong>#${item.id}</strong></td>
                <td>${item.name}</td>
                <td>$${item.price.toFixed(2)}</td>
                <td>${item.quantity}</td>
                <td>${item.reorderLevel}</td>
                <td>${statusBadge}</td>
            </tr>
        `;
    }).join('');
}

// --- CUSTOMERS SECTION ---

async function handleAddCustomer(e) {
    e.preventDefault();
    const customer = {
        name: document.getElementById('custName').value.trim(),
        address: document.getElementById('custAddress').value.trim(),
        phone: document.getElementById('custPhone').value.trim(),
        email: document.getElementById('custEmail').value.trim()
    };

    try {
        const response = await fetch(`${API_BASE}/customers`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(customer)
        });

        if (response.ok) {
            showToast('Customer registered!', 'success');
            e.target.reset();
            await getAllCustomers();
        } else {
            const err = await response.json();
            showToast(err.error || 'Registration failed.', 'error');
        }
    } catch (error) {
        showToast(`Server query failed: ${error.message}`, 'error');
    }
}

async function getAllCustomers() {
    try {
        const response = await fetch(`${API_BASE}/customers`);
        if (!response.ok) throw new Error('API fetch failed');
        allCustomers = await response.json();
        renderCustomersTable(allCustomers);
        updateOrderCustomerDropdown();
    } catch (error) {
        showToast(`Failed loading customers: ${error.message}`, 'error');
    }
}

// Customer searching executed client-side since API does not support search mapping
function searchCustomerByName() {
    const val = document.getElementById('custSearchName').value.trim().toLowerCase();
    if (!val) {
        renderCustomersTable(allCustomers);
        return;
    }
    const filtered = allCustomers.filter(c => c.name.toLowerCase().includes(val));
    renderCustomersTable(filtered);
    showToast(`Found ${filtered.length} matching customers (client-side filter)`, 'info');
}

function renderCustomersTable(customers) {
    const tbody = document.getElementById('customersBody');
    if (!tbody) return;

    if (!customers || customers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty-msg">No customers registered</td></tr>';
        return;
    }

    tbody.innerHTML = customers.map(cust => `
        <tr>
            <td><strong>#${cust.id}</strong></td>
            <td>${cust.name}</td>
            <td>${cust.address || '-'}</td>
            <td>${cust.phone || '-'}</td>
            <td>${cust.email || '-'}</td>
        </tr>
    `).join('');
}

// --- ORDER SALES MANAGEMENT ---

function updateOrderCustomerDropdown() {
    const ddl = document.getElementById('orderCustomerId');
    if (!ddl) return;
    
    const currVal = ddl.value;
    ddl.innerHTML = '<option value="">-- Choose Customer --</option>' +
        allCustomers.map(c => `<option value="${c.id}">${c.name} (ID: #${c.id})</option>`).join('');
    ddl.value = currVal;
}

function updateOrderItemDropdowns() {
    const selects = document.querySelectorAll('.itemSelect');
    selects.forEach(sel => {
        const currVal = sel.value;
        sel.innerHTML = '<option value="">-- Select Item --</option>' + 
            allItems.map(it => `<option value="${it.id}" ${it.quantity <= 0 ? 'disabled' : ''}>${it.name} - $${it.price.toFixed(2)} (${it.quantity} stock)</option>`).join('');
        sel.value = currVal;
    });
}

function addOrderItem() {
    const container = document.getElementById('orderItemsContainer');
    if (!container) return;

    const row = document.createElement('div');
    row.className = 'order-item-row';
    row.innerHTML = `
        <div class="item-select-col">
            <select class="itemSelect" required>
                <option value="">-- Select Item --</option>
                ${allItems.map(it => `<option value="${it.id}" ${it.quantity <= 0 ? 'disabled' : ''}>${it.name} - $${it.price.toFixed(2)} (${it.quantity} stock)</option>`).join('')}
            </select>
        </div>
        <div class="item-qty-col">
            <input type="number" class="itemQtyInput" placeholder="Qty" min="1" required>
        </div>
        <div class="item-remove-col">
            <button type="button" class="btn-icon-btn btn-danger-text" onclick="removeOrderItem(this)">🗑️</button>
        </div>
    `;
    container.appendChild(row);
}

function removeOrderItem(btn) {
    const rows = document.querySelectorAll('.order-item-row');
    if (rows.length <= 1) {
        showToast('Orders must include at least one item specification row.', 'error');
        return;
    }
    btn.closest('.order-item-row').remove();
}

async function handlePlaceOrder(e) {
    e.preventDefault();

    const customerId = parseInt(document.getElementById('orderCustomerId').value);
    const rows = document.querySelectorAll('.order-item-row');

    if (!customerId) {
        showToast('Please select a customer first.', 'error');
        return;
    }

    const items = [];
    let validationPassed = true;

    rows.forEach(r => {
        const itemId = parseInt(r.querySelector('.itemSelect').value);
        const qty = parseInt(r.querySelector('.itemQtyInput').value);

        if (!itemId || isNaN(qty) || qty <= 0) {
            validationPassed = false;
            return;
        }

        // Validate stock level client side first
        const matchedItem = allItems.find(it => it.id === itemId);
        if (matchedItem && matchedItem.quantity < qty) {
            showToast(`Error: ${matchedItem.name} has only ${matchedItem.quantity} units available.`, 'error');
            validationPassed = false;
            return;
        }

        items.push({ itemId, quantity: qty });
    });

    if (!validationPassed) return;

    if (items.length === 0) {
        showToast('Please build at least one item purchase row.', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ customerId, items })
        });

        const data = await response.json();

        if (response.ok) {
            showToast(`Order #${data.orderId} placed successfully!`, 'success');
            e.target.reset();
            
            // Restores builder layout to single row
            document.getElementById('orderItemsContainer').innerHTML = `
                <div class="order-item-row">
                    <div class="item-select-col">
                        <select class="itemSelect" required>
                            <option value="">-- Select Item --</option>
                        </select>
                    </div>
                    <div class="item-qty-col">
                        <input type="number" class="itemQtyInput" placeholder="Quantity" min="1" required>
                    </div>
                    <div class="item-remove-col">
                        <button type="button" class="btn-icon-btn btn-danger-text" onclick="removeOrderItem(this)">🗑️</button>
                    </div>
                </div>
            `;
            
            await getAllItems(); // Sync items/stock levels
            await getAllOrders(); // Sync orders list
        } else {
            showToast(data.error || 'Failed to place order.', 'error');
        }
    } catch (error) {
        showToast(`Server error: ${error.message}`, 'error');
    }
}

async function getAllOrders() {
    try {
        // FIXED ENDPOINT MISMATCH: backend has no standard GET /api/orders.
        // It has /api/orders/report/recent?days=36500 which selects all orders in database.
        const response = await fetch(`${API_BASE}/orders/report/recent?days=36500`);
        if (!response.ok) throw new Error('API query failed');
        allOrders = await response.json();
        renderOrdersTable(allOrders);
        updateOverviewStats();
    } catch (error) {
        showToast(`Failed syncing order history logs: ${error.message}`, 'error');
    }
}

async function getOrderById() {
    const idVal = parseInt(document.getElementById('orderSearchId').value);
    if (!idVal) {
        getAllOrders();
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/orders/${idVal}`);
        if (!response.ok) {
            if (response.status === 404) {
                showToast(`Order #${idVal} not found in database`, 'error');
            } else {
                throw new Error('API fetch error');
            }
            return;
        }
        const order = await response.json();
        renderOrdersTable([order]);
        showToast(`Found order details matching #${idVal}`, 'success');
    } catch (error) {
        showToast(`Failed loading order details: ${error.message}`, 'error');
    }
}

function renderOrdersTable(orders) {
    const tbody = document.getElementById('ordersBody');
    if (!tbody) return;

    if (!orders || orders.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty-msg">No orders recorded</td></tr>';
        return;
    }

    // FIXED FIELD ACCNAME MISMATCHES: orderId (not id) and orderDate (not date)
    tbody.innerHTML = orders.map(ord => {
        const formattedDate = ord.orderDate ? formatRawDate(ord.orderDate) : '-';
        return `
            <tr>
                <td><strong>#${ord.orderId}</strong></td>
                <td>${ord.customerName || `Customer #${ord.customerId}`}</td>
                <td>${formattedDate}</td>
                <td><strong>$${ord.totalAmount.toFixed(2)}</strong></td>
                <td class="text-right">
                    <button class="btn btn-secondary btn-sm" onclick="viewOrderDetails(${ord.orderId})">View Specification Sheet</button>
                </td>
            </tr>
        `;
    }).join('');
}

// --- ORDER DETAILS OVERLAY ---

async function viewOrderDetails(orderId) {
    try {
        const response = await fetch(`${API_BASE}/orders/${orderId}`);
        if (!response.ok) throw new Error('Fetch details failed');

        const ord = await response.json();
        const modal = document.getElementById('orderModal');
        const detailsContainer = document.getElementById('modalDetails');
        if (!modal || !detailsContainer) return;

        const formattedDate = ord.orderDate ? formatRawDate(ord.orderDate) : '-';

        // Build item specifications list rows
        let itemsHtml = `
            <div class="table-wrapper" style="margin-top: 16px;">
                <table style="min-width: 100%;">
                    <thead>
                        <tr>
                            <th>Item ID</th>
                            <th>Name</th>
                            <th>Unit price</th>
                            <th>Quantity</th>
                            <th class="text-right">Total Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        if (ord.orderItems && ord.orderItems.length > 0) {
            itemsHtml += ord.orderItems.map(oi => {
                const sub = (oi.priceAtOrder || 0) * (oi.quantity || 0);
                return `
                    <tr>
                        <td>#${oi.itemId}</td>
                        <td>${oi.itemName || `Item #${oi.itemId}`}</td>
                        <td>$${oi.priceAtOrder.toFixed(2)}</td>
                        <td>${oi.quantity}</td>
                        <td class="text-right"><strong>$${sub.toFixed(2)}</strong></td>
                    </tr>
                `;
            }).join('');
        } else {
            itemsHtml += `<tr><td colspan="5" class="empty-msg">No specifications row data items loaded.</td></tr>`;
        }

        itemsHtml += `
                    </tbody>
                </table>
            </div>
        `;

        detailsContainer.innerHTML = `
            <div class="modal-spec-grid">
                <div class="spec-item">
                    <span class="spec-lbl">Order identifier</span>
                    <span class="spec-val">#${ord.orderId}</span>
                </div>
                <div class="spec-item">
                    <span class="spec-lbl">Order date</span>
                    <span class="spec-val">${formattedDate}</span>
                </div>
                <div class="spec-item">
                    <span class="spec-lbl">Customer account</span>
                    <span class="spec-val">${ord.customerName || `ID: #${ord.customerId}`}</span>
                </div>
                <div class="spec-item">
                    <span class="spec-lbl">Total invoice value</span>
                    <span class="spec-val" style="color: var(--primary); font-weight: 700;">$${ord.totalAmount.toFixed(2)}</span>
                </div>
            </div>
            <h4 style="font-size: 13px; font-weight: 600; text-transform: uppercase; color: var(--text-muted); margin-top: 24px; padding-bottom: 8px; border-bottom: 2px solid var(--primary);">Items Specifications Breakdown</h4>
            ${itemsHtml}
        `;

        modal.style.display = 'flex';
    } catch (error) {
        showToast(`Failed loading specifications modal sheet: ${error.message}`, 'error');
    }
}

function closeOrderModal() {
    const modal = document.getElementById('orderModal');
    if (modal) modal.style.display = 'none';
}

// --- REPORTS SECTION ---

// Client-side computations based on database orders cache
function updateOverviewStats() {
    const totalCount = allOrders.length;
    const totalRev = allOrders.reduce((sum, ord) => sum + ord.totalAmount, 0);
    const avgVal = totalCount > 0 ? totalRev / totalCount : 0.00;

    const countEl = document.getElementById('totalSalesResult');
    const revEl = document.getElementById('totalRevenueResult');
    const avgEl = document.getElementById('avgOrderResult');

    if (countEl) countEl.textContent = totalCount;
    if (revEl) revEl.textContent = `$${totalRev.toFixed(2)}`;
    if (avgEl) avgEl.textContent = `$${avgVal.toFixed(2)}`;
}

// Query orders placed within relative timeframe
async function getRecentOrdersReport(days) {
    try {
        const response = await fetch(`${API_BASE}/orders/report/recent?days=${days}`);
        if (!response.ok) throw new Error('API reports endpoint error');
        const orders = await response.json();
        
        renderReportTable(
            `Recent Orders - Past ${days} Days`,
            orders,
            ['orderId', 'customerName', 'orderDate', 'totalAmount'],
            ['Order ID', 'Customer Account', 'Order Date', 'Total Invoice Amount']
        );
        showToast(`Fetched ${orders.length} orders from the last ${days} days`, 'success');
    } catch (error) {
        showToast(`Failed report query: ${error.message}`, 'error');
    }
}

// Query highest and lowest orders
async function getExtremeOrdersReport() {
    try {
        const response = await fetch(`${API_BASE}/orders/report/extreme`);
        if (!response.ok) throw new Error('API reports extreme endpoint error');
        const data = await response.json();
        
        // Structure data for table display
        const rows = [];
        if (data.highest) {
            rows.push({ type: '🍒 Highest Value Order', ...data.highest });
        }
        if (data.lowest) {
            rows.push({ type: '📉 Lowest Value Order', ...data.lowest });
        }

        renderReportTable(
            'Maximum & Minimum Sales Orders Value Metric',
            rows,
            ['type', 'orderId', 'customerName', 'orderDate', 'totalAmount'],
            ['Comparison Type', 'Order ID', 'Customer Name', 'Order Date', 'Total amount']
        );
        showToast('Extreme sales orders metrics loaded.', 'success');
    } catch (error) {
        showToast(`Failed loading metrics: ${error.message}`, 'error');
    }
}

// Query customers list with active orders
async function getCustomersWhoPurchasedReport() {
    try {
        const response = await fetch(`${API_BASE}/orders/report/customers-purchased`);
        if (!response.ok) throw new Error('API query failed');
        const list = await response.json(); // List of strings (names)
        
        const rows = list.map(name => ({ name }));

        renderReportTable(
            'Customers Directory (Active Account Shoppers)',
            rows,
            ['name'],
            ['Customer Name']
        );
        showToast('Active buyers list synced.', 'success');
    } catch (error) {
        showToast(`Failed query active shopper reports: ${error.message}`, 'error');
    }
}

// Get low stock alert items
async function getReorderItemsReport() {
    try {
        const response = await fetch(`${API_BASE}/items/reorder`);
        if (!response.ok) throw new Error('Stock API query failed');
        const items = await response.json();
        
        renderReportTable(
            'Critical Stock Alerts (Reorder Levels Triggered)',
            items,
            ['id', 'name', 'price', 'quantity', 'reorderLevel'],
            ['Item ID', 'Product Specification Name', 'Unit price', 'Current Qty in Stock', 'Required Alert Stock Level']
        );
        showToast(`Low stock warning reports updated (Count: ${items.length})`, 'success');
    } catch (error) {
        showToast(`Failed stock check reports: ${error.message}`, 'error');
    }
}

// Client-side structured Top Customers Report from orders cached log database
function getTopCustomersReport() {
    if (!allOrders || allOrders.length === 0) {
        showToast('Order database is empty. Cannot rank active profiles. Please place an order.', 'error');
        return;
    }

    const dict = {};
    allOrders.forEach(o => {
        const name = o.customerName || `Customer #${o.customerId}`;
        if (!dict[name]) {
            dict[name] = { name: name, totalSpent: 0, orderCount: 0 };
        }
        dict[name].totalSpent += o.totalAmount;
        dict[name].orderCount += 1;
    });

    const ranked = Object.values(dict)
        .sort((a, b) => b.totalSpent - a.totalSpent)
        .slice(0, 5);

    renderReportTable(
        'Top 5 Customers Dashboard (Accumulated Total Expenditure)',
        ranked,
        ['name', 'orderCount', 'totalSpent'],
        ['Customer Account name', 'Orders count', 'Accumulated Spending ($)']
    );
    showToast('Top customer expenditure list compiled.', 'success');
}

// Custom table builder and renderer for analytical widgets outputs
function renderReportTable(title, data, keys, headers) {
    const container = document.getElementById('reportTableContainer');
    const titleEl = document.getElementById('reportTableTitle');
    const thead = document.getElementById('reportTableHead');
    const tbody = document.getElementById('reportTableBody');

    if (!container || !titleEl || !thead || !tbody) return;

    titleEl.textContent = title;
    
    // Build Headers
    thead.innerHTML = '<tr>' + headers.map(h => `<th>${h}</th>`).join('') + '</tr>';
    
    // Build Rows
    if (!data || data.length === 0) {
        tbody.innerHTML = `<tr><td colspan="${headers.length}" class="empty-msg">No data matches found.</td></tr>`;
    } else {
        tbody.innerHTML = data.map(item => {
            return '<tr>' + keys.map(k => {
                let cellVal = item[k];
                
                // Formats
                if (k === 'totalAmount' || k === 'price' || k === 'totalSpent') {
                    cellVal = `$${parseFloat(cellVal || 0).toFixed(2)}`;
                }
                if (k === 'orderDate' && cellVal) {
                    cellVal = formatRawDate(cellVal);
                }
                if (k === 'id' || k === 'orderId') {
                    cellVal = `<strong>#${cellVal}</strong>`;
                }

                return `<td>${cellVal}</td>`;
            }).join('') + '</tr>';
        }).join('');
    }

    container.style.display = 'block';
    
    // Scroll container into view smoothly
    container.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function closeReportTable() {
    const container = document.getElementById('reportTableContainer');
    if (container) container.style.display = 'none';
}

// ==========================================================
// UTILITY / HELPERS FUNCTIONS
// ==========================================================

// Parse LocalDate array or ISO Dates from Jackson serializer
function formatRawDate(rawDate) {
    if (!rawDate) return '-';
    // If Spring Boot returns a LocalDate array: [year, month, day]
    if (Array.isArray(rawDate)) {
        const year = rawDate[0];
        const month = String(rawDate[1]).padStart(2, '0');
        const day = String(rawDate[2]).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }
    // Else return standard ISO string or parsed date
    try {
        const d = new Date(rawDate);
        if (isNaN(d.getTime())) return String(rawDate);
        return d.toISOString().split('T')[0];
    } catch (e) {
        return String(rawDate);
    }
}

// Toast alerts message displays
let toastTimeout = null;
function showToast(msg, type = 'info') {
    const alertEl = document.getElementById('alert');
    if (!alertEl) return;

    alertEl.textContent = msg;
    // Resets CSS
    alertEl.className = 'toast-alert';
    
    // Trigger double layout computation so animations replay correctly
    void alertEl.offsetWidth;
    
    alertEl.classList.add('show', type);

    if (toastTimeout) clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        alertEl.classList.remove('show');
    }, 4500);
}

// ==========================================================
// EXPOSE GLOBAL SCOPE BINDS FOR ONCLICK INLINE HANDLERS
// ==========================================================

window.searchItemByName = searchItemByName;
window.searchByPriceRange = searchByPriceRange;
window.getReorderItems = getReorderItems;
window.searchCustomerByName = searchCustomerByName;
window.getAllCustomers = getAllCustomers;
window.addOrderItem = addOrderItem;
window.removeOrderItem = removeOrderItem;
window.getAllOrders = getAllOrders;
window.getOrderById = getOrderById;
window.viewOrderDetails = viewOrderDetails;
window.closeOrderModal = closeOrderModal;
window.getRecentOrdersReport = getRecentOrdersReport;
window.getExtremeOrdersReport = getExtremeOrdersReport;
window.getCustomersWhoPurchasedReport = getCustomersWhoPurchasedReport;
window.getReorderItemsReport = getReorderItemsReport;
window.getTopCustomersReport = getTopCustomersReport;
window.closeReportTable = closeReportTable;
