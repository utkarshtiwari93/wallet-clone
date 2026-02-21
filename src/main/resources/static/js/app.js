// ===== API BASE URL =====
const API_BASE = '/api'; ;

// ===== AUTH HELPERS =====
const Auth = {
    getToken() {
        return localStorage.getItem('token');
    },

    setToken(token) {
        localStorage.setItem('token', token);
    },

    getUserData() {
        const data = localStorage.getItem('userData');
        return data ? JSON.parse(data) : null;
    },

    setUserData(data) {
        localStorage.setItem('userData', JSON.stringify(data));
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('userData');
        window.location.href = 'index.html';
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    requireAuth() {
        if (!this.isAuthenticated()) {
            window.location.href = 'index.html';
        }
    }
};

// ===== API HELPER =====
async function apiCall(endpoint, options = {}) {
    const token = Auth.getToken();

    const config = {
        headers: {
            'Content-Type': 'application/json',
            ...(token && { 'Authorization': `Bearer ${token}` })
        },
        ...options
    };

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, config);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Something went wrong');
        }

        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// ===== ALERT HELPER =====
function showAlert(message, type = 'success') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.innerHTML = `
        <span>⚠️</span>
        <span>${message}</span>
    `;

    const container = document.querySelector('.container') || document.body;
    container.insertBefore(alertDiv, container.firstChild);

    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

// ===== LOADING STATE =====
function setLoading(button, isLoading) {
    if (isLoading) {
        button.disabled = true;
        button.dataset.originalText = button.textContent;
        button.innerHTML = '<span class="loader"></span>';
    } else {
        button.disabled = false;
        button.textContent = button.dataset.originalText;
    }
}

// ===== LOGIN =====
async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const submitBtn = event.target.querySelector('button[type="submit"]');

    setLoading(submitBtn, true);

    try {
        const response = await apiCall('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        Auth.setToken(response.token);
        Auth.setUserData({
            email: response.email,
            userId: response.userId,
            name: response.name
        });

        showAlert('Login successful!', 'success');
        setTimeout(() => {
            window.location.href = 'pages/dashboard.html';
        }, 1000);
    } catch (error) {
        showAlert(error.message || 'Login failed. Please check your credentials.', 'error');
        setLoading(submitBtn, false);
    }
}

// ===== REGISTER =====
async function handleRegister(event) {
    event.preventDefault();

    const name = document.getElementById('name').value;
    const email = document.getElementById('email').value;
    const phone = document.getElementById('phone').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const submitBtn = event.target.querySelector('button[type="submit"]');

    if (password !== confirmPassword) {
        showAlert('Passwords do not match', 'error');
        return;
    }

    setLoading(submitBtn, true);

    try {
        const response = await apiCall('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ name, email, phone, password })
        });

        Auth.setToken(response.token);
        Auth.setUserData({
            email: response.email,
            userId: response.userId,
            name: response.name
        });

        showAlert('Registration successful!', 'success');
        setTimeout(() => {
            window.location.href = 'pages/dashboard.html';
        }, 1000);
    } catch (error) {
        showAlert(error.message || 'Registration failed', 'error');
        setLoading(submitBtn, false);
    }
}

// ===== LOAD DASHBOARD =====
async function loadDashboard() {
    Auth.requireAuth();

    const userData = Auth.getUserData();
    document.getElementById('userName').textContent = userData.name;

    try {
        const balance = await apiCall('/wallet/balance');
        document.getElementById('walletBalance').textContent =
            `₹${parseFloat(balance.balance).toFixed(2)}`;
    } catch (error) {
        showAlert('Failed to load balance', 'error');
    }
}

// ===== LOAD TRANSACTION HISTORY =====
async function loadTransactionHistory(page = 0) {
    Auth.requireAuth();

    try {
        const response = await apiCall(`/wallet/transactions?page=${page}&size=10`);
        const transactionList = document.getElementById('transactionList');
        const pagination = document.getElementById('pagination');

        if (response.content.length === 0 && page === 0) {
            transactionList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <h3>No transactions yet</h3>
                    <p>Your transaction history will appear here</p>
                </div>
            `;
            return;
        }

        transactionList.innerHTML = response.content.map(txn => `
            <div class="transaction-item">
                <div class="transaction-details">
                    <div class="transaction-icon ${txn.direction.toLowerCase()}">
                        ${txn.direction === 'SENT' ? '↑' : '↓'}
                    </div>
                    <div class="transaction-info">
                        <h3>${txn.description}</h3>
                        <p>${txn.counterpartyName || 'External'} • ${new Date(txn.createdAt).toLocaleDateString()}</p>
                    </div>
                </div>
                <div class="transaction-amount">
                    <div class="amount ${txn.direction.toLowerCase()}">
                        ${txn.direction === 'SENT' ? '-' : '+'}₹${parseFloat(txn.amount).toFixed(2)}
                    </div>
                    <div class="date">${new Date(txn.createdAt).toLocaleTimeString()}</div>
                </div>
            </div>
        `).join('');

        // Pagination
        pagination.innerHTML = `
            <button onclick="loadTransactionHistory(${page - 1})" ${page === 0 ? 'disabled' : ''}>
                Previous
            </button>
            <button class="active">${page + 1}</button>
            <button onclick="loadTransactionHistory(${page + 1})" ${response.last ? 'disabled' : ''}>
                Next
            </button>
        `;
    } catch (error) {
        showAlert('Failed to load transactions', 'error');
    }
}

// ===== TRANSFER MONEY =====
async function handleTransfer(event) {
    event.preventDefault();

    const recipientPhone = document.getElementById('recipientPhone').value;
    const amount = document.getElementById('amount').value;
    const note = document.getElementById('note').value;
    const submitBtn = event.target.querySelector('button[type="submit"]');

    setLoading(submitBtn, true);

    try {
        const response = await apiCall('/wallet/transfer', {
            method: 'POST',
            body: JSON.stringify({ recipientPhone, amount: parseFloat(amount), note })
        });

        showAlert(`Successfully sent ₹${amount} to ${response.recipientName}`, 'success');

        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 2000);
    } catch (error) {
        showAlert(error.message || 'Transfer failed', 'error');
        setLoading(submitBtn, false);
    }
}

// ===== ADD MONEY (RAZORPAY) =====
async function handleAddMoney(event) {
    event.preventDefault();

    const amount = document.getElementById('amount').value;
    const submitBtn = event.target.querySelector('button[type="submit"]');

    setLoading(submitBtn, true);

    try {
        // Create Razorpay order
        const orderResponse = await apiCall('/payment/create-order', {
            method: 'POST',
            body: JSON.stringify({ amount: parseFloat(amount) })
        });

        // Initialize Razorpay checkout
        const options = {
            key: orderResponse.keyId,
            amount: parseFloat(amount) * 100,
            currency: 'INR',
            name: 'PayFlow Wallet',
            description: 'Add money to wallet',
            order_id: orderResponse.razorpayOrderId,
            handler: function (response) {
                showAlert('Payment successful! Your wallet will be credited shortly.', 'success');
                setTimeout(() => {
                    window.location.href = 'dashboard.html';
                }, 2000);
            },
            prefill: {
                email: Auth.getUserData().email
            },
            theme: {
                color: '#e91e63'
            }
        };

        const rzp = new Razorpay(options);
        rzp.on('payment.failed', function (response) {
            showAlert('Payment failed. Please try again.', 'error');
            setLoading(submitBtn, false);
        });

        rzp.open();
        setLoading(submitBtn, false);
    } catch (error) {
        showAlert(error.message || 'Failed to create payment order', 'error');
        setLoading(submitBtn, false);
    }
}

// ===== TOGGLE PASSWORD VISIBILITY =====
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const icon = input.nextElementSibling;

    if (input.type === 'password') {
        input.type = 'text';
        icon.textContent = '🙈';
    } else {
        input.type = 'password';
        icon.textContent = '👁️';
    }
}

// ===== FORMAT PHONE NUMBER =====
function formatPhoneNumber(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length > 10) {
        value = value.slice(0, 10);
    }
    input.value = value;
}