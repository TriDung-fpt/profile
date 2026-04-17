// ============================================================================
// STAFF-LOGIC.JS - EV RENTAL SYSTEM
// ============================================================================

// ============================================================================
// API CONFIGURATION
// ============================================================================
const API_BASE = 'http://localhost:8080/api';
const API_AUTH = `${API_BASE}/auth`;
const API_PROFILE = `${API_BASE}/profile`;
const API_STATIONS = `${API_BASE}/stations`;
const API_MODELS = `${API_BASE}/models`;
const API_VEHICLES = `${API_BASE}/vehicles`;
const API_BOOKINGS = `${API_BASE}/bookings`;
const API_CONTRACTS = `${API_BASE}/contracts`;
const API_INVOICES = `${API_BASE}/invoices`;
const API_VERIFICATION = `${API_BASE}/verification`;
const API_STAFF = `${API_BASE}/staff`;

// ============================================================================
// GLOBAL STATE
// ============================================================================
let currentUser = null;
let currentRole = null;
let selectedStation = null;
let selectedStartTime = null;
let selectedEndTime = null;
let availableModels = [];
let currentBookingId = null;

// ============================================================================
// INITIALIZATION
// ============================================================================
document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
    setupEventListeners();
    checkAuthStatus();
});

/**
 * Initialize application
 */
function initializeApp() {
    console.log('🚀 Khởi tạo EV Rental System...');
}

/**
 * Setup all event listeners
 */
function setupEventListeners() {
    // Auth forms
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    if (loginForm) loginForm.addEventListener('submit', handleLogin);
    if (registerForm) registerForm.addEventListener('submit', handleRegister);

    // Logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);

    // Profile update
    const updateProfileForm = document.getElementById('update-profile-form');
    if (updateProfileForm) updateProfileForm.addEventListener('submit', handleUpdateProfile);

    // Verification upload
    const verificationForm = document.getElementById('verification-upload-form');
    if (verificationForm) verificationForm.addEventListener('submit', handleVerificationUpload);

    // Model search form (Renter)
    const modelSearchForm = document.getElementById('model-search-form');
    if (modelSearchForm) modelSearchForm.addEventListener('submit', handleModelSearch);

    // Booking form
    const bookingForm = document.getElementById('booking-form');
    if (bookingForm) bookingForm.addEventListener('submit', handleCreateBooking);

    // Booking filter form (Staff)
    const bookingFilterForm = document.getElementById('booking-filter-form');
    if (bookingFilterForm) bookingFilterForm.addEventListener('submit', handleBookingFilter);

    // Staff quick access buttons
    const viewAllBookingsBtn = document.getElementById('view-all-bookings-btn');
    if (viewAllBookingsBtn) viewAllBookingsBtn.addEventListener('click', () => openModal('booking-list-modal'));

    const viewAllContractsBtn = document.getElementById('view-all-contracts-btn');
    if (viewAllContractsBtn) viewAllContractsBtn.addEventListener('click', () => openModal('contract-list-modal'));

    const viewAllInvoicesBtn = document.getElementById('view-all-invoices-btn');
    if (viewAllInvoicesBtn) viewAllInvoicesBtn.addEventListener('click', () => openModal('invoice-list-modal'));

    // Modal close buttons
    const closeModalBtns = document.querySelectorAll('.close-modal');
    closeModalBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            const modal = e.target.closest('.modal');
            if (modal) closeModal(modal.id);
        });
    });

    // Close modal on outside click
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) {
            closeModal(e.target.id);
        }
    });
}

// ============================================================================
// AUTHENTICATION
// ============================================================================

/**
 * Check authentication status on page load
 */
async function checkAuthStatus() {
    const token = localStorage.getItem('token');
    if (!token) {
        showView('auth-view');
        return;
    }

    try {
        const response = await fetch(`${API_PROFILE}/role`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Token không hợp lệ');

        const data = await response.json();
        currentUser = data;
        currentRole = data.role;

        updateUserInfo(data);
        
        // Route to appropriate view based on role
        if (currentRole === 'STATION_STAFF') {
            showView('staff-view');
            await initStaffDashboard();
        } else if (currentRole === 'EV_RENTER') {
            showView('renter-view');
            await initRenterView();
        } else {
            showToast('Bạn không có quyền truy cập', 'warning');
            handleLogout();
        }

    } catch (error) {
        console.error('Auth check failed:', error);
        localStorage.removeItem('token');
        showView('auth-view');
    }
}

/**
 * Handle login
 */
async function handleLogin(e) {
    e.preventDefault();
    const form = e.target;
    const identifier = form['login-identifier'].value.trim();
    const password = form['login-password'].value;

    try {
        const response = await fetch(`${API_AUTH}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ identifier, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Đăng nhập thất bại');
        }

        const data = await response.json();
        localStorage.setItem('token', data.token);
        
        showToast('Đăng nhập thành công!', 'success');
        form.reset();
        
        // Reload to check auth
        await checkAuthStatus();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Handle register
 */
async function handleRegister(e) {
    e.preventDefault();
    const form = e.target;

    const fullName = form['reg-fullName'].value.trim();
    const email = form['reg-email'].value.trim();
    const phone = form['reg-phone'].value.trim();
    const password = form['reg-password'].value;
    const confirmPassword = form['reg-confirmPassword'].value;
    const agreedToTerms = form['reg-agreedToTerms'].checked;

    // Validation
    if (password !== confirmPassword) {
        showToast('Mật khẩu xác nhận không khớp', 'warning');
        return;
    }

    if (!agreedToTerms) {
        showToast('Vui lòng đồng ý với điều khoản dịch vụ', 'warning');
        return;
    }

    try {
        const response = await fetch(`${API_AUTH}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, phone, password, agreedToTerms })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Đăng ký thất bại');
        }

        showToast('Đăng ký thành công! Vui lòng đăng nhập.', 'success');
        form.reset();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Handle logout
 */
function handleLogout() {
    localStorage.removeItem('token');
    currentUser = null;
    currentRole = null;
    showView('auth-view');
    showToast('Đã đăng xuất', 'info');
}

/**
 * Update user info in header
 */
function updateUserInfo(user) {
    const userInfoEl = document.getElementById('user-info');
    const logoutBtn = document.getElementById('logout-btn');

    if (userInfoEl && user) {
        userInfoEl.textContent = `${user.fullName || user.email} (${user.role})`;
        userInfoEl.style.display = 'block';
    }

    if (logoutBtn) {
        logoutBtn.style.display = 'block';
    }
}

// ============================================================================
// VIEW MANAGEMENT
// ============================================================================

/**
 * Show specific view and hide others
 */
function showView(viewId) {
    const views = document.querySelectorAll('.view');
    views.forEach(view => {
        if (view.id === viewId) {
            view.classList.add('active');
        } else {
            view.classList.remove('active');
        }
    });
}

// ============================================================================
// RENTER FUNCTIONS
// ============================================================================

/**
 * Initialize renter view
 */
async function initRenterView() {
    await loadRenterProfile();
    await loadVerificationStatus();
    await loadMyBookings();
    await loadStationsForFilter();
}

/**
 * Load renter profile
 */
async function loadRenterProfile() {
    const token = localStorage.getItem('token');
    const profileDetails = document.getElementById('profile-details');

    try {
        const response = await fetch(`${API_PROFILE}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải thông tin hồ sơ');

        const profile = await response.json();
        
        profileDetails.innerHTML = `
            <p><strong><i class="fas fa-user"></i> Họ và tên:</strong> ${profile.fullName || 'N/A'}</p>
            <p><strong><i class="fas fa-envelope"></i> Email:</strong> ${profile.email || 'N/A'}</p>
            <p><strong><i class="fas fa-phone"></i> Số điện thoại:</strong> ${profile.phone || 'N/A'}</p>
            <p><strong><i class="fas fa-id-card"></i> CCCD:</strong> ${profile.cccd || 'Chưa cập nhật'}</p>
            <p><strong><i class="fas fa-id-card-alt"></i> GPLX:</strong> ${profile.gplx || 'Chưa cập nhật'}</p>
        `;

        // Populate update form
        document.getElementById('update-fullName').value = profile.fullName || '';
        document.getElementById('update-phone').value = profile.phone || '';
        document.getElementById('update-cccd').value = profile.cccd || '';
        document.getElementById('update-gplx').value = profile.gplx || '';

    } catch (error) {
        profileDetails.innerHTML = `<p class="text-danger">❌ ${error.message}</p>`;
    }
}

/**
 * Handle profile update
 */
async function handleUpdateProfile(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;

    const data = {
        fullName: form['update-fullName'].value.trim(),
        phone: form['update-phone'].value.trim(),
        cccd: form['update-cccd'].value.trim(),
        gplx: form['update-gplx'].value.trim()
    };

    try {
        const response = await fetch(`${API_PROFILE}/update`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Cập nhật thất bại');
        }

        showToast('Cập nhật thông tin thành công!', 'success');
        await loadRenterProfile();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Load verification status
 */
async function loadVerificationStatus() {
    const token = localStorage.getItem('token');
    const statusDiv = document.getElementById('verification-status');

    try {
        const response = await fetch(`${API_VERIFICATION}/status`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải trạng thái xác thực');

        const data = await response.json();
        
        let statusBadge = '';
        let statusColor = 'secondary';
        
        switch (data.verificationStatus) {
            case 'VERIFIED':
                statusColor = 'success';
                break;
            case 'PENDING':
                statusColor = 'warning';
                break;
            case 'REJECTED':
                statusColor = 'danger';
                break;
            default:
                statusColor = 'secondary';
        }

        statusDiv.innerHTML = `
            <p><strong>Trạng thái:</strong> <span class="badge" style="background-color: var(--${statusColor}-color); color: white; padding: 6px 12px; border-radius: 4px;">${data.verificationStatus || 'NOT_VERIFIED'}</span></p>
            ${data.rejectionReason ? `<p><strong>Lý do từ chối:</strong> ${data.rejectionReason}</p>` : ''}
            <p style="color: var(--secondary-color); margin-top: 1rem;"><i class="fas fa-info-circle"></i> Bạn cần xác thực CCCD và GPLX để có thể đặt xe.</p>
        `;

    } catch (error) {
        statusDiv.innerHTML = `<p class="text-danger">❌ ${error.message}</p>`;
    }
}

/**
 * Handle verification document upload
 */
async function handleVerificationUpload(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;

    const formData = new FormData();
    formData.append('cccd', form['verify-cccd'].value.trim());
    formData.append('gplx', form['verify-gplx'].value.trim());
    formData.append('cccdFile1', form['cccdFile1'].files[0]);
    formData.append('cccdFile2', form['cccdFile2'].files[0]);
    formData.append('gplxFile1', form['gplxFile1'].files[0]);
    formData.append('gplxFile2', form['gplxFile2'].files[0]);
    formData.append('selfieFile', form['selfieFile'].files[0]);

    try {
        const response = await fetch(`${API_VERIFICATION}/upload`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            },
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Gửi yêu cầu xác thực thất bại');
        }

        showToast('Gửi yêu cầu xác thực thành công! Vui lòng chờ phê duyệt.', 'success');
        form.reset();
        await loadVerificationStatus();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Load my bookings (renter)
 */
async function loadMyBookings() {
    const token = localStorage.getItem('token');
    const listDiv = document.getElementById('my-bookings-list');

    try {
        const response = await fetch(`${API_BOOKINGS}/my-bookings`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải lịch sử đặt xe');

        const bookings = await response.json();
        
        if (bookings.length === 0) {
            listDiv.innerHTML = '<p class="empty-state"><i class="fas fa-inbox"></i><br>Bạn chưa có booking nào</p>';
            return;
        }

        let tableHTML = `
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Xe</th>
                        <th>Thời gian</th>
                        <th>Trạng thái</th>
                        <th>Tổng tiền</th>
                        <th>Hành động</th>
                    </tr>
                </thead>
                <tbody>
        `;

        bookings.forEach(booking => {
            const statusColor = getBookingStatusColor(booking.status);
            tableHTML += `
                <tr>
                    <td>${booking.id}</td>
                    <td>${booking.vehicle?.model?.modelName || 'N/A'}<br><small>${booking.vehicle?.licensePlate || ''}</small></td>
                    <td><small>${formatDateTime(booking.startTime)} - ${formatDateTime(booking.endTime)}</small></td>
                    <td><span class="badge" style="background-color: var(--${statusColor}-color); color: white;">${booking.status}</span></td>
                    <td>${formatCurrency(booking.totalPrice || 0)}</td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="viewBookingDetail(${booking.id})">
                            <i class="fas fa-eye"></i>
                        </button>
                        ${booking.status === 'PENDING' ? `<button class="btn btn-sm btn-danger" onclick="cancelBooking(${booking.id})"><i class="fas fa-times"></i></button>` : ''}
                    </td>
                </tr>
            `;
        });

        tableHTML += '</tbody></table>';
        listDiv.innerHTML = tableHTML;

    } catch (error) {
        listDiv.innerHTML = `<p class="text-danger">❌ ${error.message}</p>`;
    }
}

/**
 * Load stations for filter dropdown
 */
async function loadStationsForFilter() {
    const stationSelect = document.getElementById('filter-station');
    if (!stationSelect) return;

    try {
        const response = await fetch(`${API_STATIONS}`);
        if (!response.ok) throw new Error('Không thể tải danh sách trạm');

        const stations = await response.json();
        
        stationSelect.innerHTML = '<option value="">-- Chọn trạm --</option>';
        stations.forEach(station => {
            const option = document.createElement('option');
            option.value = station.id || station.stationId;
            option.textContent = `${station.name} - ${station.address}`;
            stationSelect.appendChild(option);
        });

    } catch (error) {
        console.error('Error loading stations:', error);
        showToast('Không thể tải danh sách trạm', 'error');
    }
}

/**
 * Handle model search with filters
 * This implements the new flow: Select station -> Show all available models -> Filter by price, seats, etc.
 */
async function handleModelSearch(e) {
    e.preventDefault();
    const form = e.target;

    const stationId = form['filter-station'].value;
    const startTime = form['filter-start-time'].value;
    const endTime = form['filter-end-time'].value;
    const minPrice = form['filter-min-price'].value;
    const maxPrice = form['filter-max-price'].value;
    const seatCount = form['filter-seat-count'].value;

    // Validation
    if (!stationId || !startTime || !endTime) {
        showToast('Vui lòng chọn trạm và thời gian thuê xe', 'warning');
        return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
        showToast('Thời gian trả xe phải sau thời gian nhận xe', 'warning');
        return;
    }

    // Save selected filters to global state
    selectedStation = stationId;
    selectedStartTime = startTime;
    selectedEndTime = endTime;

    // Build query parameters
    const params = new URLSearchParams({
        startTime,
        endTime
    });

    if (minPrice) params.append('minPrice', minPrice);
    if (maxPrice) params.append('maxPrice', maxPrice);
    if (seatCount) params.append('seatCount', seatCount);

    const modelListDiv = document.getElementById('model-list');
    modelListDiv.innerHTML = '<p class="text-center"><div class="loading-spinner"></div> Đang tìm kiếm...</p>';

    try {
        // Call correct API endpoint: /api/stations/{stationId}/models/search
        const response = await fetch(`${API_STATIONS}/${stationId}/models/search?${params.toString()}`);

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Không thể tìm kiếm xe');
        }

        const models = await response.json();
        availableModels = models;

        if (models.length === 0) {
            modelListDiv.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-car-side"></i>
                    <p>Không tìm thấy xe phù hợp với điều kiện tìm kiếm</p>
                </div>
            `;
            return;
        }

        // Display models
        let modelsHTML = '';
        models.forEach(model => {
            modelsHTML += `
                <div class="model-card" style="background: white; border-radius: var(--border-radius-sm); padding: 1.5rem; box-shadow: var(--box-shadow); cursor: pointer; transition: all 0.3s ease;" onclick="viewModelDetail(${model.id})">
                    ${model.imageUrl ? `<img src="${model.imageUrl}" alt="${model.modelName}" style="width: 100%; height: 200px; object-fit: cover; border-radius: var(--border-radius-sm); margin-bottom: 1rem;">` : ''}
                    <h3 style="font-size: 1.25rem; font-weight: 700; margin-bottom: 0.5rem;">${model.modelName}</h3>
                    <div style="display: flex; gap: 1rem; flex-wrap: wrap; margin-bottom: 1rem; color: var(--secondary-color); font-size: 0.9rem;">
                        <span><i class="fas fa-users"></i> ${model.seatCount} chỗ</span>
                        <span><i class="fas fa-battery-full"></i> ${model.batteryCapacity || 'N/A'} kWh</span>
                        <span><i class="fas fa-road"></i> ${model.range || 'N/A'} km</span>
                    </div>
                    <p style="color: var(--primary-color); font-size: 1.5rem; font-weight: 700;">
                        ${formatCurrency(model.pricePerHour)}<span style="font-size: 0.9rem; font-weight: 500;">/giờ</span>
                    </p>
                    <p style="margin-top: 0.5rem; color: var(--success-color); font-weight: 600;">
                        <i class="fas fa-check-circle"></i> ${model.availableCount || 0} xe khả dụng
                    </p>
                    <button class="btn btn-primary btn-sm" style="width: 100%; margin-top: 1rem;" onclick="event.stopPropagation(); viewModelDetail(${model.id})">
                        <i class="fas fa-eye"></i> Xem chi tiết & Chọn xe
                    </button>
                </div>
            `;
        });

        modelListDiv.innerHTML = modelsHTML;

    } catch (error) {
        modelListDiv.innerHTML = `<p class="text-danger">❌ ${error.message}</p>`;
        showToast(error.message, 'error');
    }
}

/**
 * View model detail and show available vehicles
 */
async function viewModelDetail(modelId) {
    if (!selectedStation || !selectedStartTime || !selectedEndTime) {
        showToast('Vui lòng chọn trạm và thời gian trước', 'warning');
        return;
    }

    const token = localStorage.getItem('token');
    const modal = document.getElementById('model-detail-modal');
    const modalBody = document.getElementById('model-detail-body');

    if (!modal || !modalBody) {
        console.error('Model detail modal not found');
        return;
    }

    modalBody.innerHTML = '<p class="text-center"><div class="loading-spinner"></div> Đang tải...</p>';
    openModal('model-detail-modal');

    try {
        // Fetch model details
        const modelResponse = await fetch(`${API_MODELS}/${modelId}`);
        if (!modelResponse.ok) throw new Error('Không thể tải thông tin model');
        
        const model = await modelResponse.json();

        // Fetch available vehicles for this model in the selected time range
        const params = new URLSearchParams({
            modelId,
            stationId: selectedStation,
            startTime: selectedStartTime,
            endTime: selectedEndTime
        });

        const vehiclesResponse = await fetch(`${API_VEHICLES}/available?${params.toString()}`, {
            headers: token ? { 'Authorization': `Bearer ${token}` } : {}
        });

        if (!vehiclesResponse.ok) throw new Error('Không thể tải danh sách xe khả dụng');
        
        const vehicles = await vehiclesResponse.json();

        // Build modal content
        let modalHTML = `
            <div class="detail-modal-grid">
                <div>
                    ${model.imageUrl ? `<img src="${model.imageUrl}" alt="${model.modelName}" class="detail-modal-img">` : '<div style="background: var(--light-color); height: 300px; display: flex; align-items: center; justify-content: center; border-radius: var(--border-radius-sm);"><i class="fas fa-car" style="font-size: 4rem; color: var(--secondary-color);"></i></div>'}
                </div>
                <div>
                    <h2 style="font-size: 1.75rem; font-weight: 700; margin-bottom: 1rem;">${model.modelName}</h2>
                    <p style="color: var(--secondary-color); margin-bottom: 1.5rem;">${model.description || 'Không có mô tả'}</p>
                    
                    <h3 style="font-size: 1.25rem; font-weight: 600; margin-bottom: 1rem;">Thông số kỹ thuật</h3>
                    <ul class="spec-list">
                        <li><span><i class="fas fa-users"></i> Số chỗ ngồi</span> <strong>${model.seatCount}</strong></li>
                        <li><span><i class="fas fa-battery-full"></i> Dung lượng pin</span> <strong>${model.batteryCapacity || 'N/A'} kWh</strong></li>
                        <li><span><i class="fas fa-road"></i> Quãng đường</span> <strong>${model.range || 'N/A'} km</strong></li>
                        <li><span><i class="fas fa-tachometer-alt"></i> Tốc độ tối đa</span> <strong>${model.maxSpeed || 'N/A'} km/h</strong></li>
                        <li><span><i class="fas fa-dollar-sign"></i> Giá thuê</span> <strong style="color: var(--primary-color); font-size: 1.25rem;">${formatCurrency(model.pricePerHour)}/giờ</strong></li>
                    </ul>
                </div>
            </div>
            
            <div class="available-vehicles-section">
                <h3 style="font-size: 1.5rem; font-weight: 700; margin-bottom: 1.5rem;">
                    <i class="fas fa-car-side"></i> Xe khả dụng (${vehicles.length})
                </h3>
        `;

        if (vehicles.length === 0) {
            modalHTML += `
                <div class="empty-state">
                    <i class="fas fa-info-circle"></i>
                    <p>Không có xe nào khả dụng trong khung thời gian này</p>
                </div>
            `;
        } else {
            modalHTML += '<div class="grid-container">';
            vehicles.forEach(vehicle => {
                const statusColor = vehicle.status === 'AVAILABLE' ? 'success' : 'secondary';
                modalHTML += `
                    <div class="vehicle-card" style="background: var(--light-color); border-radius: var(--border-radius-sm); padding: 1.5rem; border: 2px solid var(--border-color);">
                        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 1rem;">
                            <h4 style="font-size: 1.1rem; font-weight: 700;">${vehicle.licensePlate}</h4>
                            <span class="badge" style="background-color: var(--${statusColor}-color); color: white;">${vehicle.status}</span>
                        </div>
                        <p style="color: var(--secondary-color); font-size: 0.9rem; margin-bottom: 1rem;">
                            <i class="fas fa-palette"></i> ${vehicle.color || 'N/A'}<br>
                            <i class="fas fa-calendar"></i> Năm: ${vehicle.year || 'N/A'}<br>
                            <i class="fas fa-tachometer-alt"></i> Số km: ${vehicle.mileage ? formatNumber(vehicle.mileage) : 'N/A'} km
                        </p>
                        ${vehicle.status === 'AVAILABLE' && token ? `
                            <button class="btn btn-success btn-sm" style="width: 100%;" onclick="selectVehicleForBooking(${vehicle.id}, '${vehicle.licensePlate}', ${model.id}, '${model.modelName}')">
                                <i class="fas fa-check-circle"></i> Chọn xe này
                            </button>
                        ` : vehicle.status !== 'AVAILABLE' ? '<p style="color: var(--danger-color); text-align: center; margin: 0;"><small>Xe không khả dụng</small></p>' : '<p style="color: var(--warning-color); text-align: center; margin: 0;"><small>Vui lòng đăng nhập để đặt xe</small></p>'}
                    </div>
                `;
            });
            modalHTML += '</div>';
        }

        modalHTML += '</div>';
        modalBody.innerHTML = modalHTML;

    } catch (error) {
        modalBody.innerHTML = `<p class="text-danger">❌ ${error.message}</p>`;
        showToast(error.message, 'error');
    }
}

/**
 * Select vehicle for booking
 */
function selectVehicleForBooking(vehicleId, licensePlate, modelId, modelName) {
    closeModal('model-detail-modal');
    
    // Find the model data
    const model = availableModels.find(m => m.id === modelId);
    
    // Populate booking form in the booking-confirm-modal
    document.getElementById('booking-vehicle-id').value = vehicleId;
    document.getElementById('booking-station-id').value = selectedStation;
    document.getElementById('booking-start-time').value = selectedStartTime;
    document.getElementById('booking-end-time').value = selectedEndTime;
    
    // Update summary
    document.getElementById('booking-summary-vehicle').textContent = modelName;
    document.getElementById('booking-summary-plate').textContent = licensePlate;
    document.getElementById('booking-summary-start').textContent = formatDateTime(selectedStartTime);
    document.getElementById('booking-summary-end').textContent = formatDateTime(selectedEndTime);
    
    // Calculate and show estimated price
    calculateBookingPrice(vehicleId, selectedStartTime, selectedEndTime);
    
    openModal('booking-confirm-modal');
}

/**
 * Calculate booking price
 */
async function calculateBookingPrice(vehicleId, startTime, endTime) {
    try {
        const params = new URLSearchParams({ vehicleId, startTime, endTime });
        const response = await fetch(`${API_BOOKINGS}/calculate-price?${params.toString()}`);
        
        if (response.ok) {
            const data = await response.json();
            document.getElementById('booking-summary-price').textContent = formatCurrency(data.totalPrice || 0);
        } else {
            document.getElementById('booking-summary-price').textContent = 'Liên hệ để biết giá';
        }
    } catch (error) {
        console.error('Error calculating price:', error);
        document.getElementById('booking-summary-price').textContent = 'Liên hệ để biết giá';
    }
}

/**
 * Handle create booking from form submission
 */
async function handleCreateBooking(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    if (!token) {
        showToast('Vui lòng đăng nhập để đặt xe', 'warning');
        return;
    }

    const form = e.target;
    const data = {
        vehicleId: parseInt(form['booking-vehicle-id'].value),
        startTime: form['booking-start-time'].value,
        endTime: form['booking-end-time'].value,
        pickupLocation: selectedStation, // Use station as pickup location
        notes: ''
    };

    try {
        const response = await fetch(`${API_BOOKINGS}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Đặt xe thất bại');
        }

        const booking = await response.json();
        showToast('Đặt xe thành công! Mã booking: ' + booking.id, 'success');
        
        closeModal('booking-confirm-modal');
        form.reset();
        
        // Reload my bookings if in renter view
        if (currentRole === 'EV_RENTER') {
            await loadMyBookings();
        }

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * View booking detail
 */
async function viewBookingDetail(bookingId) {
    // Implementation for viewing booking details
    showToast('Xem chi tiết booking: ' + bookingId, 'info');
}

/**
 * Cancel booking
 */
async function cancelBooking(bookingId) {
    if (!confirm('Bạn có chắc muốn hủy booking này?')) return;

    const token = localStorage.getItem('token');
    try {
        const response = await fetch(`${API_BOOKINGS}/${bookingId}/cancel`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Hủy booking thất bại');
        }

        showToast('Hủy booking thành công', 'success');
        await loadMyBookings();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

// ============================================================================
// STAFF FUNCTIONS
// ============================================================================

/**
 * Initialize staff dashboard
 */
async function initStaffDashboard() {
    await loadStaffStationInfo();
    await loadDashboardStats();
}

/**
 * Load staff station info
 */
async function loadStaffStationInfo() {
    const token = localStorage.getItem('token');
    const stationNameEl = document.getElementById('dashboard-station-name');

    try {
        const response = await fetch(`${API_STAFF}/my-station`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải thông tin trạm');

        const station = await response.json();
        stationNameEl.textContent = `Trạm: ${station.name} - ${station.address}`;

    } catch (error) {
        stationNameEl.textContent = 'Không thể tải thông tin trạm';
    }
}

/**
 * Load dashboard statistics
 */
async function loadDashboardStats() {
    const token = localStorage.getItem('token');
    const statsContainer = document.getElementById('dashboard-stats-container');

    try {
        const response = await fetch(`${API_STAFF}/dashboard/stats`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải thống kê');

        const stats = await response.json();
        
        statsContainer.innerHTML = `
            <div class="stat-card" style="border-left-color: var(--primary-color);">
                <div class="stat-value" style="color: var(--primary-color);">${stats.totalBookings || 0}</div>
                <div class="stat-label">Tổng Bookings</div>
            </div>
            <div class="stat-card" style="border-left-color: var(--warning-color);">
                <div class="stat-value" style="color: var(--warning-color);">${stats.pendingBookings || 0}</div>
                <div class="stat-label">Chờ xử lý</div>
            </div>
            <div class="stat-card" style="border-left-color: var(--success-color);">
                <div class="stat-value" style="color: var(--success-color);">${stats.activeRentals || 0}</div>
                <div class="stat-label">Đang cho thuê</div>
            </div>
            <div class="stat-card" style="border-left-color: var(--info-color);">
                <div class="stat-value" style="color: var(--info-color);">${stats.availableVehicles || 0}</div>
                <div class="stat-label">Xe khả dụng</div>
            </div>
            <div class="stat-card" style="border-left-color: var(--danger-color);">
                <div class="stat-value" style="color: var(--danger-color);">${formatCurrency(stats.totalRevenue || 0)}</div>
                <div class="stat-label">Doanh thu tháng</div>
            </div>
        `;

    } catch (error) {
        statsContainer.innerHTML = `<p class="text-danger">❌ ${error.message}</p>`;
    }
}

/**
 * Load all bookings for staff
 */
async function loadAllBookings(filters = {}) {
    const token = localStorage.getItem('token');
    const tableBody = document.getElementById('booking-list-table').querySelector('tbody');

    tableBody.innerHTML = '<tr><td colspan="6" class="text-center"><div class="loading-spinner"></div> Đang tải...</td></tr>';

    try {
        const params = new URLSearchParams(filters);
        const response = await fetch(`${API_STAFF}/bookings?${params.toString()}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải danh sách bookings');

        const bookings = await response.json();
        
        if (bookings.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Không tìm thấy booking nào</td></tr>';
            return;
        }

        tableBody.innerHTML = '';
        bookings.forEach(booking => {
            const statusColor = getBookingStatusColor(booking.status);
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${booking.id}</td>
                <td>${booking.user?.fullName || 'N/A'}<br><small>${booking.user?.phone || ''}</small></td>
                <td>${booking.vehicle?.licensePlate || 'N/A'}<br><small>${booking.vehicle?.model?.modelName || ''}</small></td>
                <td><span class="badge" style="background-color: var(--${statusColor}-color); color: white;">${booking.status}</span></td>
                <td><small>${formatDateTime(booking.createdAt)}</small></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="viewStaffBookingDetail(${booking.id})">
                        <i class="fas fa-eye"></i>
                    </button>
                    ${booking.status === 'PENDING' ? `
                        <button class="btn btn-sm btn-success" onclick="confirmBooking(${booking.id})" title="Xác nhận">
                            <i class="fas fa-check"></i>
                        </button>
                    ` : ''}
                    ${booking.status === 'CONFIRMED' ? `
                        <button class="btn btn-sm btn-info" onclick="checkInBooking(${booking.id})" title="Check-in">
                            <i class="fas fa-sign-in-alt"></i>
                        </button>
                    ` : ''}
                    ${booking.status === 'RENTING' ? `
                        <button class="btn btn-sm btn-warning" onclick="checkOutBooking(${booking.id})" title="Check-out">
                            <i class="fas fa-sign-out-alt"></i>
                        </button>
                    ` : ''}
                </td>
            `;
            tableBody.appendChild(row);
        });

    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">❌ ${error.message}</td></tr>`;
    }
}

/**
 * Handle booking filter
 */
async function handleBookingFilter(e) {
    e.preventDefault();
    const form = e.target;

    const filters = {};
    const keyword = form['booking-search-keyword'].value.trim();
    const status = form['booking-filter-status'].value;
    const date = form['booking-filter-date'].value;

    if (keyword) filters.keyword = keyword;
    if (status) filters.status = status;
    if (date) filters.date = date;

    await loadAllBookings(filters);
}

/**
 * View staff booking detail
 */
async function viewStaffBookingDetail(bookingId) {
    const token = localStorage.getItem('token');
    
    try {
        const response = await fetch(`${API_STAFF}/bookings/${bookingId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải chi tiết booking');

        const booking = await response.json();
        
        // Show detail in modal
        const modalBody = document.getElementById('verification-modal-body');
        const modalTitle = document.getElementById('verification-modal-title');
        const modalActions = document.getElementById('verification-modal-actions');

        modalTitle.textContent = `Chi tiết Booking #${booking.id}`;
        
        modalBody.innerHTML = `
            <div style="display: grid; gap: 1rem;">
                <p><strong>Khách hàng:</strong> ${booking.user?.fullName || 'N/A'}</p>
                <p><strong>Email:</strong> ${booking.user?.email || 'N/A'}</p>
                <p><strong>SĐT:</strong> ${booking.user?.phone || 'N/A'}</p>
                <p><strong>Xe:</strong> ${booking.vehicle?.model?.modelName || 'N/A'} - ${booking.vehicle?.licensePlate || 'N/A'}</p>
                <p><strong>Thời gian:</strong> ${formatDateTime(booking.startTime)} - ${formatDateTime(booking.endTime)}</p>
                <p><strong>Trạng thái:</strong> <span class="badge" style="background-color: var(--${getBookingStatusColor(booking.status)}-color); color: white;">${booking.status}</span></p>
                <p><strong>Tổng tiền:</strong> ${formatCurrency(booking.totalPrice || 0)}</p>
                <p><strong>Địa điểm nhận:</strong> ${booking.pickupLocation || 'N/A'}</p>
                <p><strong>Ghi chú:</strong> ${booking.notes || 'Không có'}</p>
            </div>
        `;

        modalActions.innerHTML = '';
        openModal('verification-detail-modal');

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Confirm booking
 */
async function confirmBooking(bookingId) {
    if (!confirm('Xác nhận booking này?')) return;

    const token = localStorage.getItem('token');
    try {
        const response = await fetch(`${API_STAFF}/bookings/${bookingId}/confirm`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Xác nhận thất bại');
        }

        showToast('Xác nhận booking thành công!', 'success');
        await loadAllBookings();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Check-in booking
 */
async function checkInBooking(bookingId) {
    if (!confirm('Thực hiện check-in cho booking này?')) return;

    const token = localStorage.getItem('token');
    try {
        const response = await fetch(`${API_STAFF}/bookings/${bookingId}/check-in`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Check-in thất bại');
        }

        const result = await response.json();
        showToast('Check-in thành công!', 'success');
        
        // Show contract link if available
        if (result.contractUrl) {
            document.getElementById('contract-link').href = result.contractUrl;
            openModal('contract-link-modal');
        }
        
        await loadAllBookings();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Check-out booking
 */
async function checkOutBooking(bookingId) {
    if (!confirm('Thực hiện check-out cho booking này?')) return;

    const token = localStorage.getItem('token');
    try {
        const response = await fetch(`${API_STAFF}/bookings/${bookingId}/check-out`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Check-out thất bại');
        }

        showToast('Check-out thành công!', 'success');
        await loadAllBookings();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Load all contracts
 */
async function loadAllContracts() {
    const token = localStorage.getItem('token');
    const tableBody = document.getElementById('contract-list-table').querySelector('tbody');

    tableBody.innerHTML = '<tr><td colspan="6" class="text-center"><div class="loading-spinner"></div> Đang tải...</td></tr>';

    try {
        const response = await fetch(`${API_STAFF}/contracts`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải danh sách hợp đồng');

        const contracts = await response.json();
        
        if (contracts.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Không có hợp đồng nào</td></tr>';
            return;
        }

        tableBody.innerHTML = '';
        contracts.forEach(contract => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${contract.id}</td>
                <td>${contract.booking?.id || 'N/A'}</td>
                <td>${contract.booking?.user?.fullName || 'N/A'}</td>
                <td>${contract.staff?.fullName || 'N/A'}</td>
                <td><small>${formatDateTime(contract.signedAt)}</small></td>
                <td>
                    <a href="${contract.contractUrl}" target="_blank" class="btn btn-sm btn-primary">
                        <i class="fas fa-file-pdf"></i> Xem
                    </a>
                </td>
            `;
            tableBody.appendChild(row);
        });

    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">❌ ${error.message}</td></tr>`;
    }
}

/**
 * Load all invoices
 */
async function loadAllInvoices() {
    const token = localStorage.getItem('token');
    const tableBody = document.getElementById('invoice-list-table').querySelector('tbody');

    tableBody.innerHTML = '<tr><td colspan="5" class="text-center"><div class="loading-spinner"></div> Đang tải...</td></tr>';

    try {
        const response = await fetch(`${API_STAFF}/invoices`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải danh sách hóa đơn');

        const invoices = await response.json();
        
        if (invoices.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Không có hóa đơn nào</td></tr>';
            return;
        }

        tableBody.innerHTML = '';
        invoices.forEach(invoice => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${invoice.booking?.id || 'N/A'}</td>
                <td>${invoice.booking?.user?.fullName || 'N/A'}</td>
                <td>${formatCurrency(invoice.totalAmount || 0)}</td>
                <td><small>${formatDateTime(invoice.createdAt)}</small></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="viewInvoiceDetail(${invoice.id})">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });

    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger">❌ ${error.message}</td></tr>`;
    }
}

/**
 * View invoice detail
 */
async function viewInvoiceDetail(invoiceId) {
    showToast('Xem chi tiết hóa đơn: ' + invoiceId, 'info');
}

// ============================================================================
// MODAL MANAGEMENT
// ============================================================================

/**
 * Open modal
 */
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'block';
        
        // Load data when modal opens
        if (modalId === 'booking-list-modal') {
            loadAllBookings();
        } else if (modalId === 'contract-list-modal') {
            loadAllContracts();
        } else if (modalId === 'invoice-list-modal') {
            loadAllInvoices();
        }
    }
}

/**
 * Close modal
 */
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'none';
    }
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Show toast notification
 */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) {
        console.log(`Toast (${type}):`, message);
        return;
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    const icons = {
        success: 'fa-check-circle',
        error: 'fa-times-circle',
        warning: 'fa-exclamation-triangle',
        info: 'fa-info-circle'
    };

    toast.innerHTML = `
        <i class="fas ${icons[type] || icons.info}"></i>
        <span>${message}</span>
        <button class="toast-close-btn" onclick="this.parentElement.remove()">×</button>
    `;

    container.appendChild(toast);

    // Auto remove after 5 seconds
    setTimeout(() => {
        if (toast.parentElement) {
            toast.remove();
        }
    }, 5000);
}

/**
 * Format currency
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

/**
 * Format number
 */
function formatNumber(num) {
    return new Intl.NumberFormat('vi-VN').format(num);
}

/**
 * Format date time
 */
function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Get booking status color
 */
function getBookingStatusColor(status) {
    const colors = {
        'PENDING': 'warning',
        'CONFIRMED': 'info',
        'RENTING': 'primary',
        'COMPLETED': 'success',
        'CANCELLED': 'danger'
    };
    return colors[status] || 'secondary';
}

// ============================================================================
// EXPORT FOR TESTING (Optional)
// ============================================================================
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        formatCurrency,
        formatDateTime,
        getBookingStatusColor
    };
}

// ============================================================================
// STAFF SPECIFIC FUNCTIONS - PENALTY FEES & CHECKOUT
// ============================================================================

/**
 * Load penalty fees for staff
 */
async function loadPenaltyFees() {
    const token = localStorage.getItem('token');
    const feeListDiv = document.getElementById('penalty-fee-list');

    if (!feeListDiv) return;

    try {
        const response = await fetch(`${API_STAFF}/penalty-fees`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải danh sách phí phạt');

        const fees = await response.json();

        if (fees.length === 0) {
            feeListDiv.innerHTML = '<p>Không có phí phạt nào</p>';
            return;
        }

        let feesHTML = '';
        fees.forEach(fee => {
            feesHTML += `
                <div class="form-check" style="padding: 0.75rem; background: var(--light-color); border-radius: 6px; margin-bottom: 0.5rem;">
                    <input type="checkbox" id="fee-${fee.id}" value="${fee.id}" 
                           data-name="${fee.name}" data-amount="${fee.amount}" 
                           onchange="updateSelectedFees()">
                    <label for="fee-${fee.id}" style="cursor: pointer; margin: 0;">
                        <strong>${fee.name}</strong> - ${formatCurrency(fee.amount)}
                        ${fee.description ? `<br><small style="color: var(--secondary-color);">${fee.description}</small>` : ''}
                    </label>
                </div>
            `;
        });

        feeListDiv.innerHTML = feesHTML;

    } catch (error) {
        console.error('Error loading penalty fees:', error);
        feeListDiv.innerHTML = `<p style="color: var(--danger-color);">❌ ${error.message}</p>`;
    }
}

/**
 * Update selected fees summary
 */
function updateSelectedFees() {
    const checkboxes = document.querySelectorAll('#penalty-fee-list input[type="checkbox"]:checked');
    const summaryDiv = document.getElementById('selected-fees-summary');
    const totalAmountSpan = document.getElementById('total-penalty-amount');

    if (!summaryDiv || !totalAmountSpan) return;

    if (checkboxes.length === 0) {
        summaryDiv.innerHTML = '<p>Chưa chọn phụ phí.</p>';
        totalAmountSpan.textContent = '0';
        return;
    }

    let total = 0;
    let summaryHTML = '<ul style="margin: 0; padding-left: 1.5rem;">';

    checkboxes.forEach(cb => {
        const name = cb.dataset.name;
        const amount = parseFloat(cb.dataset.amount);
        total += amount;
        summaryHTML += `<li><strong>${name}:</strong> ${formatCurrency(amount)}</li>`;
    });

    summaryHTML += '</ul>';
    summaryDiv.innerHTML = summaryHTML;
    totalAmountSpan.textContent = formatCurrency(total);
}

/**
 * Handle confirm deposit form
 */
async function handleConfirmDeposit(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;
    const bookingId = form['confirm-deposit-booking-id'].value;

    try {
        const response = await fetch(`${API_STAFF}/bookings/${bookingId}/confirm-deposit`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Xác nhận cọc thất bại');
        }

        const result = await response.json();
        showToast(result.message || 'Xác nhận cọc thành công!', 'success');
        form.reset();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Handle initiate check-in form
 */
async function handleInitiateCheckIn(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;
    const bookingId = form['initiate-checkin-booking-id'].value;

    try {
        const response = await fetch(`${API_STAFF}/rentals/initiate-check-in/${bookingId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Không thể tính cọc');
        }

        const result = await response.json();

        // Show QR code or payment info
        if (result.qrCode) {
            document.getElementById('qr-code-image').src = result.qrCode;
            document.getElementById('qr-code-title').textContent = 'Thanh toán cọc thuê xe';
            document.getElementById('qr-code-message').textContent = `Vui lòng thanh toán ${formatCurrency(result.depositAmount)} để hoàn tất check-in`;

            // Show payment info
            const paymentInfo = document.getElementById('payment-info-section');
            if (paymentInfo) {
                paymentInfo.style.display = 'block';
                document.getElementById('deposit-amount-span').textContent = formatCurrency(result.depositAmount);
                document.getElementById('transfer-booking-id-span').textContent = bookingId;
            }

            openModal('qr-code-modal');
        } else {
            showToast(result.message || 'Tính cọc thành công!', 'success');
        }

        form.reset();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Handle calculate bill form
 */
async function handleCalculateBill(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;
    const bookingId = form['bill-booking-id'].value;

    // Prepare form data
    const formData = new FormData();

    // Get selected penalty fees
    const selectedFees = [];
    const checkboxes = document.querySelectorAll('#penalty-fee-list input[type="checkbox"]:checked');
    checkboxes.forEach(cb => {
        selectedFees.push({
            feeId: parseInt(cb.value),
            quantity: 1
        });
    });

    // Add custom fee if provided
    const customFeeName = form['custom-fee-name']?.value.trim();
    const customFeeAmount = form['custom-fee-amount']?.value;
    const customFeeDesc = form['custom-fee-desc']?.value.trim();

    if (customFeeName && customFeeAmount) {
        formData.append('customFeeName', customFeeName);
        formData.append('customFeeAmount', customFeeAmount);
        if (customFeeDesc) formData.append('customFeeDescription', customFeeDesc);
    }

    // Add custom fee photos
    const customFeePhotos = form['custom-fee-photo']?.files;
    if (customFeePhotos && customFeePhotos.length > 0) {
        for (let i = 0; i < customFeePhotos.length; i++) {
            formData.append('customFeePhotos', customFeePhotos[i]);
        }
    }

    // Add selected fees as JSON
    if (selectedFees.length > 0) {
        formData.append('selectedFeesJson', JSON.stringify(selectedFees));
    }

    try {
        const response = await fetch(`${API_STAFF}/bookings/${bookingId}/calculate-bill`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Tính bill thất bại');
        }

        const bill = await response.json();

        // Display bill result
        const resultDiv = document.getElementById('bill-result');
        if (resultDiv) {
            resultDiv.innerHTML = `
                <div style="background: linear-gradient(135deg, var(--success-color) 0%, #059669 100%); color: white; padding: 2rem; border-radius: var(--border-radius); box-shadow: var(--box-shadow-lg);">
                    <h3 style="margin-bottom: 1.5rem; font-size: 1.5rem;"><i class="fas fa-file-invoice-dollar"></i> Hóa đơn cuối cùng</h3>
                    <div style="background: rgba(255,255,255,0.1); padding: 1.5rem; border-radius: var(--border-radius-sm); margin-bottom: 1rem;">
                        <p style="margin: 0.5rem 0;"><strong>Phí thuê xe:</strong> ${formatCurrency(bill.rentalFee || 0)}</p>
                        <p style="margin: 0.5rem 0;"><strong>Phụ phí:</strong> ${formatCurrency(bill.penaltyFees || 0)}</p>
                        <p style="margin: 0.5rem 0;"><strong>Cọc đã trả:</strong> ${formatCurrency(bill.depositPaid || 0)}</p>
                        <hr style="margin: 1rem 0; opacity: 0.3;">
                        <p style="margin: 0.5rem 0; font-size: 1.5rem; font-weight: 800;"><strong>Tổng cộng:</strong> ${formatCurrency(bill.totalAmount || 0)}</p>
                        <p style="margin: 0.5rem 0; font-size: 1.25rem;"><strong>Khách cần trả:</strong> ${formatCurrency(bill.amountDue || 0)}</p>
                    </div>
                    ${bill.qrCode ? `
                        <div style="text-align: center; margin-top: 1.5rem;">
                            <img src="${bill.qrCode}" alt="QR Code" style="max-width: 300px; border-radius: var(--border-radius-sm); background: white; padding: 1rem;">
                            <p style="margin-top: 1rem;">Quét mã QR để thanh toán</p>
                        </div>
                    ` : ''}
                </div>
            `;

            // Scroll to result
            resultDiv.scrollIntoView({ behavior: 'smooth' });
        }

        showToast('Tạo hóa đơn thành công!', 'success');

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Load pending verifications
 */
async function loadPendingVerifications() {
    const token = localStorage.getItem('token');
    const listDiv = document.getElementById('pending-verifications-list');

    if (!listDiv) return;

    listDiv.innerHTML = '<p class="empty-state"><div class="loading-spinner"></div><br>Đang tải...</p>';

    try {
        const response = await fetch(`${API_STAFF}/verifications/pending`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải danh sách chờ duyệt');

        const users = await response.json();

        if (users.length === 0) {
            listDiv.innerHTML = '<p class="empty-state"><i class="fas fa-check-circle"></i><br>Không có yêu cầu chờ duyệt</p>';
            return;
        }

        let usersHTML = '';
        users.forEach(user => {
            usersHTML += `
                <div style="background: white; padding: 1.5rem; border-radius: var(--border-radius-sm); border: 2px solid var(--border-color);">
                    <h4 style="margin-bottom: 1rem;">${user.fullName}</h4>
                    <p><strong>Email:</strong> ${user.email}</p>
                    <p><strong>SĐT:</strong> ${user.phone || 'N/A'}</p>
                    <p><strong>CCCD:</strong> ${user.cccd || 'N/A'}</p>
                    <p><strong>GPLX:</strong> ${user.gplx || 'N/A'}</p>
                    <button class="btn btn-primary btn-sm" onclick="viewVerificationDetail(${user.id})" style="margin-top: 1rem; width: 100%;">
                        <i class="fas fa-eye"></i> Xem chi tiết
                    </button>
                </div>
            `;
        });

        listDiv.innerHTML = usersHTML;

    } catch (error) {
        listDiv.innerHTML = `<p style="color: var(--danger-color);">❌ ${error.message}</p>`;
    }
}

/**
 * View verification detail
 */
async function viewVerificationDetail(userId) {
    const token = localStorage.getItem('token');

    try {
        const response = await fetch(`${API_VERIFICATION}/${userId}/details`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) throw new Error('Không thể tải chi tiết xác thực');

        const data = await response.json();

        const modalBody = document.getElementById('verification-modal-body');
        const modalTitle = document.getElementById('verification-modal-title');
        const modalActions = document.getElementById('verification-modal-actions');

        modalTitle.textContent = `Xác thực: ${data.user?.fullName || 'N/A'}`;

        modalBody.innerHTML = `
            <div>
                <h3>Thông tin cá nhân</h3>
                <p><strong>CCCD:</strong> ${data.user?.cccd || 'N/A'}</p>
                <p><strong>GPLX:</strong> ${data.user?.gplx || 'N/A'}</p>
            </div>
            <div>
                <h3>Hình ảnh xác thực</h3>
                <div class="verification-image-grid">
                    ${data.cccdFront ? `<div class="verification-image-item"><img src="${data.cccdFront}" alt="CCCD Mặt trước"><span>CCCD Mặt trước</span></div>` : ''}
                    ${data.cccdBack ? `<div class="verification-image-item"><img src="${data.cccdBack}" alt="CCCD Mặt sau"><span>CCCD Mặt sau</span></div>` : ''}
                    ${data.gplxFront ? `<div class="verification-image-item"><img src="${data.gplxFront}" alt="GPLX Mặt trước"><span>GPLX Mặt trước</span></div>` : ''}
                    ${data.gplxBack ? `<div class="verification-image-item"><img src="${data.gplxBack}" alt="GPLX Mặt sau"><span>GPLX Mặt sau</span></div>` : ''}
                    ${data.selfie ? `<div class="verification-image-item"><img src="${data.selfie}" alt="Selfie"><span>Selfie</span></div>` : ''}
                </div>
            </div>
        `;

        modalActions.innerHTML = `
            <button class="btn btn-success" onclick="approveVerification(${userId})">
                <i class="fas fa-check"></i> Phê duyệt
            </button>
            <button class="btn btn-danger" onclick="rejectVerification(${userId})">
                <i class="fas fa-times"></i> Từ chối
            </button>
        `;

        openModal('verification-detail-modal');

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Approve verification
 */
async function approveVerification(userId) {
    const token = localStorage.getItem('token');

    try {
        const response = await fetch(`${API_STAFF}/verifications/${userId}/process`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ approve: true })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Phê duyệt thất bại');
        }

        const result = await response.json();
        showToast(result.message || 'Phê duyệt thành công!', 'success');
        closeModal('verification-detail-modal');
        await loadPendingVerifications();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Reject verification
 */
async function rejectVerification(userId) {
    const reason = prompt('Nhập lý do từ chối:');
    if (!reason) return;

    const token = localStorage.getItem('token');

    try {
        const response = await fetch(`${API_STAFF}/verifications/${userId}/process`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ approve: false, rejectionReason: reason })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Từ chối thất bại');
        }

        const result = await response.json();
        showToast(result.message || 'Từ chối thành công!', 'success');
        closeModal('verification-detail-modal');
        await loadPendingVerifications();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Handle update vehicle form
 */
async function handleUpdateVehicle(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;

    const vehicleId = form['update-vehicle-id'].value;
    const data = {};

    if (form['update-battery-level'].value) {
        data.batteryLevel = parseInt(form['update-battery-level'].value);
    }

    if (form['update-condition'].value) {
        data.condition = form['update-condition'].value;
    }

    try {
        const response = await fetch(`${API_STAFF}/vehicles/${vehicleId}/details`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Cập nhật thất bại');
        }

        const result = await response.json();
        showToast(result.message || 'Cập nhật xe thành công!', 'success');
        form.reset();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Handle report damage form
 */
async function handleReportDamage(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const form = e.target;

    const formData = new FormData();
    formData.append('vehicleId', form['damage-vehicle-id'].value);
    formData.append('description', form['damage-description'].value);

    const photos = form['damage-photo'].files;
    if (photos && photos.length > 0) {
        for (let i = 0; i < photos.length; i++) {
            formData.append('photos', photos[i]);
        }
    }

    try {
        const response = await fetch(`${API_STAFF}/vehicles/report-damage`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Báo cáo thất bại');
        }

        const result = await response.json();
        showToast(result.message || 'Báo cáo hư hỏng thành công!', 'success');
        form.reset();

    } catch (error) {
        showToast(error.message, 'error');
    }
}

/**
 * Load staff vehicles
 */
async function loadStaffVehicles() {
    const token = localStorage.getItem('token');
    const listDiv = document.getElementById('staff-vehicle-list');
    const containerDiv = document.getElementById('staff-vehicle-list-container');

    if (!listDiv || !containerDiv) return;

    // Toggle visibility
    if (containerDiv.style.display === 'none') {
        containerDiv.style.display = 'block';
        listDiv.innerHTML = '<p class="empty-state"><div class="loading-spinner"></div><br>Đang tải...</p>';

        try {
            const response = await fetch(`${API_STAFF}/my-station/vehicles`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!response.ok) throw new Error('Không thể tải danh sách xe');

            const vehicles = await response.json();

            if (vehicles.length === 0) {
                listDiv.innerHTML = '<p class="empty-state"><i class="fas fa-car"></i><br>Không có xe nào</p>';
                return;
            }

            let vehiclesHTML = '';
            vehicles.forEach(vehicle => {
                vehiclesHTML += `
                    <div class="vehicle-card">
                        <h3>${vehicle.licensePlate}</h3>
                        <p><strong>Model:</strong> ${vehicle.model?.modelName || 'N/A'}</p>
                        <p><strong>Màu:</strong> ${vehicle.color || 'N/A'}</p>
                        <p><strong>Năm:</strong> ${vehicle.year || 'N/A'}</p>
                        <p><strong>Pin:</strong> ${vehicle.batteryLevel || 'N/A'}%</p>
                        <p><strong>Trạng thái:</strong> <span class="status-badge status-${vehicle.status}">${vehicle.status}</span></p>
                    </div>
                `;
            });

            listDiv.innerHTML = vehiclesHTML;

        } catch (error) {
            listDiv.innerHTML = `<p style="color: var(--danger-color);">❌ ${error.message}</p>`;
        }
    } else {
        containerDiv.style.display = 'none';
    }
}

// ============================================================================
// ADDITIONAL EVENT LISTENERS FOR STAFF FORMS
// ============================================================================

// Setup staff-specific event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Confirm deposit form
    const confirmDepositForm = document.getElementById('confirm-deposit-form');
    if (confirmDepositForm) {
        confirmDepositForm.addEventListener('submit', handleConfirmDeposit);
    }

    // Initiate check-in form
    const initiateCheckinForm = document.getElementById('initiate-checkin-form');
    if (initiateCheckinForm) {
        initiateCheckinForm.addEventListener('submit', handleInitiateCheckIn);
    }

    // Calculate bill form
    const calculateBillForm = document.getElementById('calculate-bill-form');
    if (calculateBillForm) {
        calculateBillForm.addEventListener('submit', handleCalculateBill);
    }

    // Update vehicle form
    const updateVehicleForm = document.getElementById('update-vehicle-form');
    if (updateVehicleForm) {
        updateVehicleForm.addEventListener('submit', handleUpdateVehicle);
    }

    // Report damage form
    const reportDamageForm = document.getElementById('report-damage-form');
    if (reportDamageForm) {
        reportDamageForm.addEventListener('submit', handleReportDamage);
    }

    // Load pending verifications button
    const loadVerificationsBtn = document.getElementById('load-pending-verifications-btn');
    if (loadVerificationsBtn) {
        loadVerificationsBtn.addEventListener('click', loadPendingVerifications);
    }

    // Load staff vehicles button
    const loadVehiclesBtn = document.getElementById('load-staff-vehicles-btn');
    if (loadVehiclesBtn) {
        loadVehiclesBtn.addEventListener('click', loadStaffVehicles);
    }

    // Load penalty fees when staff view is active
    if (currentRole === 'STATION_STAFF') {
        loadPenaltyFees();
    }
});

