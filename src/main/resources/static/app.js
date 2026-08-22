/**
 * Enterprise Library Management System (Aura LMS) - Single Page Application JS
 */

const API_BASE = '/api/v1';

// Default Demo Credentials
const DEMO_USERS = {
    admin: { email: 'admin@library.com', password: 'Admin@123', name: 'Eleanor Vance', role: 'ADMIN' },
    librarian: { email: 'librarian@library.com', password: 'Librarian@123', name: 'Arthur Pendleton', role: 'LIBRARIAN' },
    member: { email: 'member@library.com', password: 'Member@123', name: 'Sophia Rodriguez', role: 'MEMBER' }
};

let state = {
    token: localStorage.getItem('lms_jwt') || null,
    currentUser: null,
    currentTab: 'catalog',
    catalog: {
        page: 0,
        size: 8,
        category: '',
        search: '',
        availableOnly: false,
        sort: 'id,desc'
    },
    policy: {
        maxBorrowLimit: 5,
        loanDurationDays: 14,
        dailyFineRate: 0.50,
        fineThreshold: 10.00,
        holdWindowHours: 48
    }
};

// Initialize Application
document.addEventListener('DOMContentLoaded', async () => {
    setupNavigation();
    loadTheme();
    await fetchPolicies();
    
    // If no active session, auto-login as Admin for instant full demo exploration
    if (!state.token) {
        await switchDemoUser('admin');
    } else {
        await loadCurrentUserProfile();
    }

    loadCatalogBooks();
});

// Setup Tab Navigation
function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabName = item.getAttribute('data-tab');
            switchTab(tabName);
        });
    });
}

function switchTab(tabName) {
    state.currentTab = tabName;
    
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    const activeNav = document.getElementById(`nav-${tabName}`);
    if (activeNav) activeNav.classList.add('active');

    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    const activeTabEl = document.getElementById(`tab-${tabName}`);
    if (activeTabEl) activeTabEl.classList.add('active');

    // Update Header Titles
    const titleMap = {
        'catalog': { title: 'Catalog Explorer', sub: 'Discover, search, and manage books across all collections' },
        'circulation': { title: 'Circulation Desk', sub: 'Issue physical books, process returns, and manage active loans' },
        'my-portal': { title: 'My Library Portal', sub: 'Track your borrowed books, due dates, and active waitlists' },
        'reservations': { title: 'Waitlists & Holds', sub: 'Automated FIFO queue and 48-hour pickup reservations' },
        'fines': { title: 'Fines & Settlements', sub: 'View overdue fines, settle balances online, and review payment history' },
        'admin': { title: 'Admin Analytics & Governance', sub: 'System metrics, circulation policies, and member privileges' }
    };

    if (titleMap[tabName]) {
        document.getElementById('page-title').textContent = titleMap[tabName].title;
        document.getElementById('page-subtitle').textContent = titleMap[tabName].sub;
    }

    // Refresh Tab Data
    if (tabName === 'catalog') loadCatalogBooks();
    if (tabName === 'circulation') { loadCirculationLoans(); loadBorrowerSelect(); }
    if (tabName === 'my-portal') loadMyPortalData();
    if (tabName === 'reservations') loadReservations();
    if (tabName === 'fines') loadFines();
    if (tabName === 'admin') loadAdminDashboard();
}

// API Helper with JWT Header
async function apiFetch(endpoint, options = {}) {
    const headers = options.headers || {};
    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }
    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers
        });

        if (response.status === 401) {
            showToast('Session expired or unauthorized. Please re-login.', 'error');
            return null;
        }

        if (response.status === 204) return true;

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || (data.validationErrors ? data.validationErrors.join(', ') : 'Request failed'));
        }
        return data;
    } catch (err) {
        console.error('API Error:', err);
        showToast(err.message, 'error');
        throw err;
    }
}

// Fetch Policies
async function fetchPolicies() {
    try {
        const policy = await apiFetch('/admin/policies');
        if (policy) {
            state.policy = policy;
            document.getElementById('policy-summary-text').textContent = 
                `Max ${policy.maxBorrowLimit} Loans • ${policy.loanDurationDays} Days • $${Number(policy.dailyFineRate).toFixed(2)}/Day`;
            
            // Populate form if exists
            const maxLim = document.getElementById('policy-max-limit');
            if (maxLim) {
                document.getElementById('policy-max-limit').value = policy.maxBorrowLimit;
                document.getElementById('policy-loan-days').value = policy.loanDurationDays;
                document.getElementById('policy-fine-rate').value = policy.dailyFineRate;
                document.getElementById('policy-threshold').value = policy.fineThreshold;
                document.getElementById('policy-hold-hours').value = policy.holdWindowHours;
            }
        }
    } catch (e) {
        console.warn('Could not load policy initially');
    }
}

// Switch Demo User
async function switchDemoUser(roleKey) {
    const demo = DEMO_USERS[roleKey];
    if (!demo) return;

    try {
        const res = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email: demo.email, password: demo.password })
        });

        if (res && res.token) {
            state.token = res.token;
            localStorage.setItem('lms_jwt', res.token);
            state.currentUser = res;

            updateUserUI();
            
            // Update quick role buttons
            document.querySelectorAll('.role-btn').forEach(b => b.classList.remove('active'));
            const activeBtn = document.getElementById(`quick-role-${roleKey}`);
            if (activeBtn) activeBtn.classList.add('active');

            showToast(`Logged in as ${res.fullName} (${res.role})`, 'success');
            
            // Refresh current tab
            switchTab(state.currentTab);
        }
    } catch (err) {
        showToast('Login failed: ' + err.message, 'error');
    }
}

async function loadCurrentUserProfile() {
    try {
        const profile = await apiFetch('/auth/me');
        if (profile) {
            state.currentUser = profile;
            updateUserUI();
        }
    } catch (err) {
        state.token = null;
        localStorage.removeItem('lms_jwt');
    }
}

function updateUserUI() {
    if (!state.currentUser) return;
    
    const user = state.currentUser;
    const initials = (user.firstName ? user.firstName[0] : '') + (user.lastName ? user.lastName[0] : '');
    
    document.getElementById('userAvatar').textContent = initials || 'U';
    document.getElementById('userName').textContent = user.fullName || `${user.firstName} ${user.lastName}`;
    document.getElementById('userRoleBadge').textContent = user.role;

    // Role-based visibility
    const isStaff = user.role === 'LIBRARIAN' || user.role === 'ADMIN';
    const isAdmin = user.role === 'ADMIN';
    
    const addBtn = document.getElementById('btn-add-book');
    if (addBtn) addBtn.style.display = isStaff ? 'inline-flex' : 'none';

    const adminTab = document.getElementById('nav-admin');
    if (adminTab) adminTab.style.display = isStaff ? 'flex' : 'none';
}

// -------------------------------------------------------------
// Catalog Explorer Logic
// -------------------------------------------------------------
let searchTimeout;
function handleSearchInput() {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        state.catalog.search = document.getElementById('catalog-search').value;
        state.catalog.page = 0;
        loadCatalogBooks();
    }, 300);
}

function clearCatalogSearch() {
    document.getElementById('catalog-search').value = '';
    state.catalog.search = '';
    state.catalog.page = 0;
    loadCatalogBooks();
}

function selectCategory(cat) {
    state.catalog.category = cat;
    state.catalog.page = 0;
    document.querySelectorAll('.category-chip').forEach(c => {
        c.classList.toggle('active', c.getAttribute('data-category') === cat);
    });
    loadCatalogBooks();
}

async function loadCatalogBooks() {
    const grid = document.getElementById('booksGrid');
    grid.innerHTML = '<div class="loading-state"><div class="spinner"></div><p>Loading library catalog...</p></div>';

    const sortVal = document.getElementById('filter-sort').value;
    const availOnly = document.getElementById('filter-available-only').checked;

    let url = `/books?page=${state.catalog.page}&size=${state.catalog.size}&sort=${sortVal}`;
    if (state.catalog.search) url += `&search=${encodeURIComponent(state.catalog.search)}`;
    if (state.catalog.category) url += `&category=${state.catalog.category}`;
    if (availOnly) url += `&availableOnly=true`;

    try {
        const data = await apiFetch(url);
        if (!data || !data.content || data.content.length === 0) {
            grid.innerHTML = `
                <div class="text-center py-6" style="grid-column: 1/-1;">
                    <i class="fa-solid fa-book-open" style="font-size: 3rem; color: var(--text-muted); margin-bottom: 1rem;"></i>
                    <h3>No matching books found</h3>
                    <p class="text-muted">Try adjusting your search query or category filters.</p>
                </div>`;
            document.getElementById('catalogPagination').innerHTML = '';
            return;
        }

        grid.innerHTML = data.content.map(book => {
            const isAvailable = book.availableCopies > 0;
            const availBadgeClass = isAvailable ? 'avail-green' : 'avail-red';
            const availText = isAvailable ? `${book.availableCopies} of ${book.totalCopies} Available` : 'All Copies Borrowed';
            const coverUrl = book.coverImageUrl || 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=60';

            return `
                <div class="book-card">
                    <div class="book-card-cover-wrapper">
                        <img src="${coverUrl}" alt="${book.title}" class="book-card-cover" onerror="this.src='https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=500&auto=format&fit=crop&q=60'">
                        <span class="category-badge-overlay">${formatCategoryName(book.category)}</span>
                        <span class="availability-badge-overlay ${availBadgeClass}">${availText}</span>
                    </div>
                    <div class="book-card-body">
                        <h4 class="book-card-title" title="${book.title}">${book.title}</h4>
                        <p class="book-card-author">by ${book.author}</p>
                        <p class="book-card-isbn">ISBN: ${book.isbn}</p>
                        <div class="book-card-footer">
                            <button class="secondary-btn btn-sm" onclick="openBookDetailModal(${book.id})">
                                <i class="fa-solid fa-circle-info"></i> Copies
                            </button>
                            ${isAvailable ? `
                                <button class="primary-btn btn-sm" onclick="quickBorrowBook(${book.id})">
                                    <i class="fa-solid fa-book-bookmark"></i> Borrow
                                </button>
                            ` : `
                                <button class="primary-btn btn-sm" style="background: var(--accent-amber);" onclick="quickReserveBook(${book.id})">
                                    <i class="fa-solid fa-clock-rotate-left"></i> Hold (Waitlist)
                                </button>
                            `}
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        renderPagination(data, 'catalogPagination', (newPage) => {
            state.catalog.page = newPage;
            loadCatalogBooks();
        });

    } catch (e) {
        grid.innerHTML = `<div class="text-center text-rose py-6" style="grid-column: 1/-1;">Error loading catalog books.</div>`;
    }
}

function formatCategoryName(cat) {
    if (!cat) return 'GENERAL';
    return cat.replace(/_/g, ' ');
}

function renderPagination(data, containerId, onPageChange) {
    const container = document.getElementById(containerId);
    if (!container || data.totalPages <= 1) {
        if (container) container.innerHTML = '';
        return;
    }

    let html = '<div class="pagination-buttons" style="display: flex; gap: 0.5rem; justify-content: center; margin-top: 1.5rem;">';
    for (let i = 0; i < data.totalPages; i++) {
        const activeClass = i === data.number ? 'primary-btn' : 'secondary-btn';
        html += `<button class="${activeClass} btn-sm" onclick="(${onPageChange.toString()})(${i})">${i + 1}</button>`;
    }
    html += '</div>';
    container.innerHTML = html;
}

// -------------------------------------------------------------
// Book Details & Barcodes Modal
// -------------------------------------------------------------
async function openBookDetailModal(bookId) {
    try {
        const book = await apiFetch(`/books/${bookId}`);
        if (!book) return;

        document.getElementById('modalBookTitle').textContent = book.title;
        const modalBody = document.getElementById('modalBookBody');

        const copiesHtml = (book.copies && book.copies.length > 0) ? book.copies.map(c => `
            <tr>
                <td><code>${c.barcode}</code></td>
                <td><span class="badge ${c.status === 'AVAILABLE' ? 'badge-success' : (c.status === 'BORROWED' ? 'badge-warning' : 'badge-danger')}">${c.status}</span></td>
                <td>${c.conditionNotes || 'Normal'}</td>
                <td>${c.acquiredDate || 'N/A'}</td>
                <td>
                    ${c.status === 'AVAILABLE' ? `
                        <button class="primary-btn btn-sm" onclick="copyBarcodeToCirculation('${c.barcode}')">
                            <i class="fa-solid fa-barcode"></i> Select
                        </button>
                    ` : '<span class="text-muted text-sm">Checked out</span>'}
                </td>
            </tr>
        `).join('') : '<tr><td colspan="5" class="text-center">No copies recorded.</td></tr>';

        modalBody.innerHTML = `
            <div style="display: flex; gap: 1.5rem; flex-wrap: wrap; margin-bottom: 1.5rem;">
                <img src="${book.coverImageUrl || ''}" style="width: 110px; height: 150px; object-fit: cover; border-radius: 8px;" onerror="this.style.display='none'">
                <div style="flex: 1;">
                    <h4>${book.title}</h4>
                    <p class="text-muted">Author: <strong>${book.author}</strong> | Publisher: <strong>${book.publisher || 'N/A'} (${book.publicationYear || ''})</strong></p>
                    <p class="text-muted">Category: <strong>${formatCategoryName(book.category)}</strong> | ISBN: <code>${book.isbn}</code></p>
                    <p class="mt-2" style="font-size: 0.875rem;">${book.description || 'No description provided.'}</p>
                </div>
            </div>

            <h4 class="mb-2"><i class="fa-solid fa-layer-group"></i> Physical Copies & Barcodes (${book.copies ? book.copies.length : 0})</h4>
            <div class="table-responsive">
                <table class="styled-table">
                    <thead>
                        <tr>
                            <th>Barcode</th>
                            <th>Status</th>
                            <th>Condition</th>
                            <th>Acquired</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${copiesHtml}
                    </tbody>
                </table>
            </div>
        `;

        openModal('bookModal');
    } catch (e) {
        showToast('Could not load book details', 'error');
    }
}

function copyBarcodeToCirculation(barcode) {
    closeModal('bookModal');
    switchTab('circulation');
    const issueInput = document.getElementById('issue-barcode');
    if (issueInput) {
        issueInput.value = barcode;
        issueInput.focus();
    }
    showToast(`Barcode ${barcode} copied to Issue form!`, 'info');
}

// -------------------------------------------------------------
// Quick Borrow & Reserve
// -------------------------------------------------------------
async function quickBorrowBook(bookId) {
    try {
        const book = await apiFetch(`/books/${bookId}`);
        if (!book || !book.copies) return;
        const availableCopy = book.copies.find(c => c.status === 'AVAILABLE');
        if (!availableCopy) {
            showToast('No copies available for immediate borrow. Placing hold instead...', 'warning');
            quickReserveBook(bookId);
            return;
        }

        const res = await apiFetch('/circulation/borrow', {
            method: 'POST',
            body: JSON.stringify({ barcode: availableCopy.barcode })
        });

        if (res) {
            showToast(`Success! "${book.title}" borrowed. Due on ${res.dueDate}.`, 'success');
            loadCatalogBooks();
            if (state.currentUser) loadCurrentUserProfile();
        }
    } catch (e) {
        // Error toast shown by apiFetch
    }
}

async function quickReserveBook(bookId) {
    try {
        const res = await apiFetch('/reservations', {
            method: 'POST',
            body: JSON.stringify({ bookId })
        });

        if (res) {
            showToast(`Hold placed for "${res.bookTitle}"! Position in FIFO queue: #${res.queuePosition || 1}`, 'success');
            loadCatalogBooks();
            if (state.currentUser) loadCurrentUserProfile();
        }
    } catch (e) {
        // Error toast shown by apiFetch
    }
}

// -------------------------------------------------------------
// Circulation Desk Logic
// -------------------------------------------------------------
async function loadBorrowerSelect() {
    const select = document.getElementById('issue-user-select');
    if (!select) return;

    try {
        const usersData = await apiFetch('/admin/users?size=50');
        if (usersData && usersData.content) {
            select.innerHTML = '<option value="">Select Member / Patron...</option>' +
                usersData.content.map(u => `<option value="${u.id}" ${state.currentUser && state.currentUser.id === u.id ? 'selected' : ''}>${u.fullName} (${u.email}) [${u.role}]</option>`).join('');
        }
    } catch (e) {
        // If not admin, just use current user
        if (state.currentUser) {
            select.innerHTML = `<option value="${state.currentUser.id}" selected>${state.currentUser.fullName} (You)</option>`;
        }
    }
}

async function handleIssueBook(e) {
    e.preventDefault();
    const barcode = document.getElementById('issue-barcode').value.trim();
    const userId = document.getElementById('issue-user-select').value;

    try {
        const res = await apiFetch('/circulation/borrow', {
            method: 'POST',
            body: JSON.stringify({
                barcode,
                userId: userId ? Number(userId) : null
            })
        });

        if (res) {
            showToast(`Issued "${res.bookTitle}" (Barcode: ${res.barcode}) to ${res.userFullName}. Due: ${res.dueDate}`, 'success');
            document.getElementById('issue-barcode').value = '';
            loadCirculationLoans();
            loadCatalogBooks();
        }
    } catch (err) {
        // Error handled in apiFetch
    }
}

async function handleReturnBook(e) {
    e.preventDefault();
    const barcode = document.getElementById('return-barcode').value.trim();
    const conditionNotes = document.getElementById('return-condition').value.trim();

    try {
        const res = await apiFetch('/circulation/return', {
            method: 'POST',
            body: JSON.stringify({ barcode, conditionNotes })
        });

        if (res) {
            let msg = `Returned "${res.bookTitle}" (Barcode: ${res.barcode}).`;
            if (res.currentFine && res.currentFine > 0) {
                msg += ` Overdue fine calculated: $${Number(res.currentFine).toFixed(2)}`;
            }
            showToast(msg, 'success');
            document.getElementById('return-barcode').value = '';
            document.getElementById('return-condition').value = '';
            loadCirculationLoans();
            loadCatalogBooks();
        }
    } catch (err) {
        // Error handled in apiFetch
    }
}

let circulationFilter = 'active';
function filterCirculationLoans(filter) {
    circulationFilter = filter;
    document.getElementById('tab-loans-active').classList.toggle('active', filter === 'active');
    document.getElementById('tab-loans-overdue').classList.toggle('active', filter === 'overdue');
    loadCirculationLoans();
}

async function loadCirculationLoans() {
    const tbody = document.getElementById('circulationLoansTableBody');
    if (!tbody) return;

    const endpoint = circulationFilter === 'overdue' ? '/circulation/overdue' : '/circulation/active';

    try {
        const data = await apiFetch(endpoint);
        if (!data || !data.content || data.content.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center py-6 text-muted">No ${circulationFilter} loans recorded.</td></tr>`;
            return;
        }

        const countBadge = document.getElementById('nav-active-loans-count');
        if (countBadge && circulationFilter === 'active') {
            countBadge.textContent = data.totalElements || data.content.length;
        }

        tbody.innerHTML = data.content.map(loan => {
            const isOverdue = loan.overdue || (loan.daysOverdue > 0);
            const statusBadge = isOverdue ?
                `<span class="badge badge-danger">OVERDUE (${loan.daysOverdue}d)</span>` :
                `<span class="badge badge-success">ACTIVE</span>`;

            const fineText = loan.currentFine > 0 ? `<strong class="text-rose">$${Number(loan.currentFine).toFixed(2)}</strong>` : '$0.00';

            return `
                <tr>
                    <td>#${loan.id}</td>
                    <td><strong>${loan.bookTitle}</strong></td>
                    <td><code>${loan.barcode}</code></td>
                    <td>${loan.userFullName}<br><span class="text-muted" style="font-size: 0.75rem;">${loan.userEmail}</span></td>
                    <td>${loan.borrowDate}</td>
                    <td><strong class="${isOverdue ? 'text-rose' : ''}">${loan.dueDate}</strong></td>
                    <td>${statusBadge}</td>
                    <td>${fineText}</td>
                    <td>
                        <div style="display: flex; gap: 0.35rem;">
                            <button class="secondary-btn btn-sm" onclick="renewLoanRecord(${loan.id})" title="Renew 14 Days">
                                <i class="fa-solid fa-arrows-rotate"></i> Renew
                            </button>
                            <button class="primary-btn btn-sm" onclick="quickReturnBarcode('${loan.barcode}')" title="Return Book">
                                <i class="fa-solid fa-rotate-left"></i> Return
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-6 text-rose">Failed to load loans.</td></tr>`;
    }
}

async function renewLoanRecord(recordId) {
    try {
        const res = await apiFetch(`/circulation/renew/${recordId}`, { method: 'POST' });
        if (res) {
            showToast(res.message, 'success');
            loadCirculationLoans();
            if (state.currentTab === 'my-portal') loadMyPortalData();
        }
    } catch (e) {}
}

function quickReturnBarcode(barcode) {
    document.getElementById('return-barcode').value = barcode;
    handleReturnBook({ preventDefault: () => {} });
}

// -------------------------------------------------------------
// Member Portal Logic
// -------------------------------------------------------------
async function loadMyPortalData() {
    if (!state.currentUser) return;
    
    await loadCurrentUserProfile();
    const user = state.currentUser;

    const initials = (user.firstName ? user.firstName[0] : '') + (user.lastName ? user.lastName[0] : '');
    document.getElementById('portalAvatar').textContent = initials;
    document.getElementById('portalUserName').textContent = user.fullName || `${user.firstName} ${user.lastName}`;
    document.getElementById('portalUserEmail').textContent = user.email;
    document.getElementById('portalUserStatus').textContent = user.status;
    document.getElementById('portalUserRole').textContent = user.role;

    document.getElementById('portalActiveLoansCount').textContent = user.activeLoansCount || 0;
    document.getElementById('portalActiveHoldsCount').textContent = user.activeReservationsCount || 0;
    document.getElementById('portalUnpaidFinesAmount').textContent = `$${Number(user.unpaidFinesTotal || 0).toFixed(2)}`;

    // Load Loans
    const grid = document.getElementById('myLoansGrid');
    try {
        const loansData = await apiFetch('/circulation/my-loans');
        if (!loansData || !loansData.content || loansData.content.length === 0) {
            grid.innerHTML = '<div class="text-center py-6 text-muted" style="grid-column: 1/-1;">You have no active or historical loans.</div>';
            document.getElementById('myLoansBadge').textContent = '0 items';
            return;
        }

        document.getElementById('myLoansBadge').textContent = `${loansData.content.filter(l => l.status === 'ACTIVE' || l.status === 'OVERDUE').length} active`;

        grid.innerHTML = loansData.content.map(loan => {
            const isOverdue = loan.overdue || (loan.daysOverdue > 0);
            return `
                <div class="my-loan-card ${isOverdue ? 'overdue-border' : ''}">
                    <div style="display: flex; gap: 0.75rem;">
                        <img src="${loan.coverImageUrl || ''}" style="width: 50px; height: 70px; object-fit: cover; border-radius: 4px;" onerror="this.src='https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=500&auto=format&fit=crop&q=60'">
                        <div>
                            <h4 style="font-size: 0.95rem; line-height: 1.3;">${loan.bookTitle}</h4>
                            <p class="text-muted" style="font-size: 0.8rem;">Barcode: <code>${loan.barcode}</code></p>
                            <div class="mt-2">
                                <span class="badge ${loan.status === 'ACTIVE' ? 'badge-success' : (loan.status === 'OVERDUE' ? 'badge-danger' : 'badge-primary')}">${loan.status}</span>
                            </div>
                        </div>
                    </div>
                    <div style="font-size: 0.825rem; border-top: 1px solid var(--border-color); padding-top: 0.5rem;">
                        <div>Borrowed: <strong>${loan.borrowDate}</strong></div>
                        <div>Due Date: <strong class="${isOverdue ? 'text-rose' : ''}">${loan.dueDate}</strong></div>
                        ${loan.currentFine > 0 ? `<div class="text-rose">Fine Accrued: <strong>$${Number(loan.currentFine).toFixed(2)}</strong></div>` : ''}
                    </div>
                    ${loan.status === 'ACTIVE' || loan.status === 'OVERDUE' ? `
                        <button class="secondary-btn btn-sm" onclick="renewLoanRecord(${loan.id})">
                            <i class="fa-solid fa-arrows-rotate"></i> Extend Due Date
                        </button>
                    ` : ''}
                </div>
            `;
        }).join('');
    } catch (e) {
        grid.innerHTML = '<div class="text-center py-6 text-rose" style="grid-column: 1/-1;">Error loading loans.</div>';
    }
}

// -------------------------------------------------------------
// Waitlists & Holds Logic
// -------------------------------------------------------------
async function loadReservations() {
    const tbody = document.getElementById('reservationsTableBody');
    if (!tbody) return;

    try {
        const isStaff = state.currentUser && (state.currentUser.role === 'LIBRARIAN' || state.currentUser.role === 'ADMIN');
        const endpoint = isStaff ? '/reservations/all' : '/reservations/my-reservations';

        const data = await apiFetch(endpoint);
        if (!data || !data.content || data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-6 text-muted">No active reservations found.</td></tr>';
            return;
        }

        tbody.innerHTML = data.content.map(r => {
            let statusBadge = `<span class="badge badge-primary">${r.status}</span>`;
            if (r.status === 'READY_FOR_PICKUP') {
                statusBadge = `<span class="badge badge-success"><i class="fa-solid fa-bell"></i> READY (Hold 48h)</span>`;
            } else if (r.status === 'EXPIRED' || r.status === 'CANCELLED') {
                statusBadge = `<span class="badge badge-danger">${r.status}</span>`;
            }

            const queuePos = r.queuePosition ? `<strong>#${r.queuePosition}</strong> in queue` : '-';
            const expiry = r.expiryDate ? new Date(r.expiryDate).toLocaleString() : '-';

            return `
                <tr>
                    <td>#${r.id}</td>
                    <td><strong>${r.bookTitle}</strong></td>
                    <td>${r.userFullName}<br><span class="text-muted" style="font-size: 0.75rem;">${r.userEmail}</span></td>
                    <td>${new Date(r.reservationDate).toLocaleDateString()}</td>
                    <td>${queuePos}</td>
                    <td>${statusBadge}</td>
                    <td><code>${r.heldCopyBarcode || '-'}</code></td>
                    <td>${expiry}</td>
                    <td>
                        ${r.status === 'PENDING' || r.status === 'READY_FOR_PICKUP' ? `
                            <button class="secondary-btn btn-sm" onclick="cancelReservationHold(${r.id})" style="color: var(--accent-rose);">
                                <i class="fa-solid fa-xmark"></i> Cancel
                            </button>
                        ` : '-'}
                    </td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-6 text-rose">Error loading waitlist holds.</td></tr>';
    }
}

async function cancelReservationHold(resId) {
    if (!confirm('Are you sure you want to cancel this hold reservation?')) return;
    try {
        const res = await apiFetch(`/reservations/${resId}/cancel`, { method: 'POST' });
        if (res) {
            showToast('Reservation cancelled successfully', 'info');
            loadReservations();
        }
    } catch (e) {}
}

// -------------------------------------------------------------
// Fines & Payment Logic
// -------------------------------------------------------------
async function loadFines() {
    const tbody = document.getElementById('finesTableBody');
    if (!tbody) return;

    try {
        const isStaff = state.currentUser && (state.currentUser.role === 'LIBRARIAN' || state.currentUser.role === 'ADMIN');
        const endpoint = isStaff ? '/fines/all' : '/fines/my-fines';

        const data = await apiFetch(endpoint);
        if (!data || !data.content || data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-6 text-muted">No fines recorded. Your balance is clear!</td></tr>';
            return;
        }

        tbody.innerHTML = data.content.map(fine => {
            const isUnpaid = fine.status === 'UNPAID';
            const statusBadge = isUnpaid ?
                `<span class="badge badge-danger">UNPAID</span>` :
                (fine.status === 'PAID' ? `<span class="badge badge-success">PAID</span>` : `<span class="badge badge-warning">WAIVED</span>`);

            return `
                <tr>
                    <td>#${fine.id}</td>
                    <td>${fine.userFullName}</td>
                    <td><strong>${fine.bookTitle}</strong></td>
                    <td><code>${fine.barcode}</code></td>
                    <td><strong class="text-rose">$${Number(fine.amount).toFixed(2)}</strong></td>
                    <td>${fine.reason}</td>
                    <td>${statusBadge}</td>
                    <td>${fine.paidAt ? new Date(fine.paidAt).toLocaleDateString() + ' (' + (fine.paymentMethod || 'Online') + ')' : '-'}</td>
                    <td>
                        <div style="display: flex; gap: 0.35rem;">
                            ${isUnpaid ? `
                                <button class="primary-btn btn-sm" onclick="openPayFineModal(${fine.id}, ${fine.amount})">
                                    <i class="fa-solid fa-credit-card"></i> Pay Now
                                </button>
                                ${isStaff ? `
                                    <button class="secondary-btn btn-sm" onclick="waiveFineRecord(${fine.id})">
                                        <i class="fa-solid fa-handshake"></i> Waive
                                    </button>
                                ` : ''}
                            ` : '<span class="text-muted text-sm">Settled</span>'}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-6 text-rose">Error loading fines.</td></tr>';
    }
}

function openPayFineModal(fineId, amount) {
    document.getElementById('payFineId').value = fineId;
    document.getElementById('payFineDetailsText').textContent = `Total amount to pay: $${Number(amount).toFixed(2)}`;
    openModal('payFineModal');
}

async function handlePayFineSubmit(e) {
    e.preventDefault();
    const fineId = document.getElementById('payFineId').value;
    const method = document.getElementById('payFineMethod').value;
    const notes = document.getElementById('payFineNotes').value;

    try {
        const res = await apiFetch(`/fines/${fineId}/pay`, {
            method: 'POST',
            body: JSON.stringify({ paymentMethod: method, notes })
        });

        if (res) {
            closeModal('payFineModal');
            showToast(`Fine #${fineId} settled successfully via ${method}!`, 'success');
            loadFines();
            if (state.currentUser) loadCurrentUserProfile();
        }
    } catch (e) {}
}

async function waiveFineRecord(fineId) {
    const reason = prompt('Please enter the reason for waiving this fine (e.g. "Library Amnesty Week" or "Staff Discretion"):', 'Staff discretion');
    if (!reason) return;

    try {
        const res = await apiFetch(`/fines/${fineId}/waive`, {
            method: 'POST',
            body: JSON.stringify({ reason })
        });
        if (res) {
            showToast(`Fine #${fineId} waived successfully.`, 'info');
            loadFines();
        }
    } catch (e) {}
}

// -------------------------------------------------------------
// Admin & Analytics Dashboard Logic
// -------------------------------------------------------------
async function loadAdminDashboard() {
    try {
        const stats = await apiFetch('/admin/analytics/dashboard');
        if (stats) {
            document.getElementById('stat-total-books').textContent = stats.totalBooks;
            document.getElementById('stat-total-copies').textContent = stats.totalCopies;
            document.getElementById('stat-available-copies').textContent = `${stats.availableCopies} Available`;
            document.getElementById('stat-active-loans').textContent = stats.activeLoans;
            document.getElementById('stat-overdue-loans').textContent = stats.overdueLoans;
            document.getElementById('stat-revenue').textContent = `$${Number(stats.totalCollectedRevenue || 0).toFixed(2)}`;
            document.getElementById('stat-total-members').textContent = stats.totalMembers;

            // Top Books Leaderboard
            const topList = document.getElementById('topBooksList');
            if (stats.topBorrowedBooks && stats.topBorrowedBooks.length > 0) {
                topList.innerHTML = stats.topBorrowedBooks.map((b, i) => `
                    <div class="top-book-item">
                        <span class="top-book-rank">#${i + 1}</span>
                        <img src="${b.coverImageUrl || ''}" class="top-book-cover" onerror="this.src='https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=60'">
                        <div style="flex: 1;">
                            <h4 style="font-size: 0.9rem;">${b.title}</h4>
                            <p class="text-muted" style="font-size: 0.75rem;">by ${b.author}</p>
                        </div>
                        <span class="badge badge-primary">${b.borrowCount} Loans</span>
                    </div>
                `).join('');
            } else {
                topList.innerHTML = '<div class="text-center py-4 text-muted">No circulation metrics accumulated yet.</div>';
            }
        }

        // Load Policies form
        fetchPolicies();

        // Load Users Table
        loadUserManagementTable();

    } catch (e) {
        showToast('Error loading analytics dashboard', 'error');
    }
}

async function loadUserManagementTable() {
    const tbody = document.getElementById('userManagementTableBody');
    if (!tbody) return;

    try {
        const usersData = await apiFetch('/admin/users?size=25');
        if (!usersData || !usersData.content) return;

        tbody.innerHTML = usersData.content.map(u => `
            <tr>
                <td>#${u.id}</td>
                <td><strong>${u.fullName}</strong></td>
                <td>${u.email}</td>
                <td><span class="badge badge-primary">${u.role}</span></td>
                <td><span class="badge ${u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}">${u.status}</span></td>
                <td>${u.activeLoansCount || 0}</td>
                <td>${u.unpaidFinesTotal > 0 ? `<span class="text-rose font-bold">$${Number(u.unpaidFinesTotal).toFixed(2)}</span>` : '$0.00'}</td>
                <td>
                    <button class="secondary-btn btn-sm" onclick="toggleUserStatus(${u.id}, '${u.status}')">
                        ${u.status === 'ACTIVE' ? '<i class="fa-solid fa-ban text-rose"></i> Suspend' : '<i class="fa-solid fa-check text-emerald"></i> Activate'}
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (e) {}
}

async function toggleUserStatus(userId, currentStatus) {
    const newStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
        await apiFetch(`/admin/users/${userId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ status: newStatus })
        });
        showToast(`User status updated to ${newStatus}`, 'success');
        loadUserManagementTable();
    } catch (e) {}
}

async function handleSavePolicies(e) {
    e.preventDefault();
    const maxBorrowLimit = Number(document.getElementById('policy-max-limit').value);
    const loanDurationDays = Number(document.getElementById('policy-loan-days').value);
    const dailyFineRate = Number(document.getElementById('policy-fine-rate').value);
    const fineThreshold = Number(document.getElementById('policy-threshold').value);
    const holdWindowHours = Number(document.getElementById('policy-hold-hours').value);

    try {
        const res = await apiFetch('/admin/policies', {
            method: 'PUT',
            body: JSON.stringify({
                maxBorrowLimit,
                loanDurationDays,
                dailyFineRate,
                fineThreshold,
                holdWindowHours
            })
        });

        if (res) {
            showToast('Library circulation policies successfully updated!', 'success');
            fetchPolicies();
        }
    } catch (e) {}
}

async function triggerScheduledJobs() {
    try {
        const res = await apiFetch('/admin/scheduler/trigger', { method: 'POST' });
        if (res) {
            showToast('Overnight circulation jobs executed! (Overdue updated, holds processed)', 'success');
            loadAdminDashboard();
        }
    } catch (e) {}
}

// -------------------------------------------------------------
// Add Book Modal Logic
// -------------------------------------------------------------
function openAddBookModal() {
    openModal('addBookModal');
}

async function handleCreateBook(e) {
    e.preventDefault();
    const title = document.getElementById('add-title').value.trim();
    const author = document.getElementById('add-author').value.trim();
    const isbn = document.getElementById('add-isbn').value.trim();
    const category = document.getElementById('add-category').value;
    const initialCopiesCount = Number(document.getElementById('add-copies').value);
    const publisher = document.getElementById('add-publisher').value.trim();
    const publicationYear = Number(document.getElementById('add-year').value);
    const coverImageUrl = document.getElementById('add-cover').value.trim();
    const description = document.getElementById('add-description').value.trim();

    try {
        const res = await apiFetch('/books', {
            method: 'POST',
            body: JSON.stringify({
                title, author, isbn, category, initialCopiesCount,
                publisher, publicationYear, coverImageUrl, description
            })
        });

        if (res) {
            closeModal('addBookModal');
            document.getElementById('createBookForm').reset();
            showToast(`Added "${res.title}" with ${res.totalCopies} barcode copies!`, 'success');
            loadCatalogBooks();
        }
    } catch (e) {}
}

// -------------------------------------------------------------
// Modal & Theme Helpers
// -------------------------------------------------------------
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

function toggleAuthModal() {
    openModal('authModal');
}

async function handleManualLogin(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;

    try {
        const res = await apiFetch('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });

        if (res && res.token) {
            state.token = res.token;
            localStorage.setItem('lms_jwt', res.token);
            state.currentUser = res;
            updateUserUI();
            closeModal('authModal');
            showToast(`Welcome back, ${res.fullName}!`, 'success');
            switchTab(state.currentTab);
        }
    } catch (e) {}
}

function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('lms_theme', newTheme);
    updateThemeIcon(newTheme);
}

function loadTheme() {
    const saved = localStorage.getItem('lms_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', saved);
    updateThemeIcon(saved);
}

function updateThemeIcon(theme) {
    const icon = document.getElementById('themeIcon');
    if (icon) {
        icon.className = theme === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    }
}

function toggleSidebar() {
    const sb = document.getElementById('sidebar');
    if (sb) sb.classList.toggle('open');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? 'fa-circle-check' : (type === 'error' ? 'fa-circle-exclamation' : 'fa-circle-info');
    toast.innerHTML = `<i class="fa-solid ${icon}"></i><span>${message}</span>`;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
