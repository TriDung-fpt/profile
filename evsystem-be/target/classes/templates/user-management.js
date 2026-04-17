// user-management.js

// API_BASE_USER: ĐÃ SỬA LỖI TYPO - Phải là /admin/station
const API_BASE_USER = 'http://localhost:8080/admin/station'; 

// Khai báo biến Modal
let userDetailModalInstance;
let roleModalInstance; // Modal Phân quyền

document.addEventListener('DOMContentLoaded', () => {
    // Khởi tạo User Detail Modal
    const userDetailModalElement = document.getElementById('userDetailModal');
    if (window.bootstrap && userDetailModalElement) {
        userDetailModalInstance = new bootstrap.Modal(userDetailModalElement);
    }
    
    // Khởi tạo Role Modal
    const roleModalElement = document.getElementById('roleModal');
    if (window.bootstrap && roleModalElement) {
        roleModalInstance = new bootstrap.Modal(roleModalElement);
    }
});

/**
 * Hàm hỗ trợ: Chuyển đổi hiển thị mật khẩu trong Modal chi tiết User.
 */
function toggleUserDetailPassword() {
    const field = document.getElementById('modalUserPassword');
    const icon = document.getElementById('passwordToggleIcon');

    if (field.type === 'password') {
        field.type = 'text';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    } else {
        field.type = 'password';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    }
}

// --- LOGIC PHÂN QUYỀN (ROLE) ---

/**
 * Hiển thị Modal để chọn Role mới cho User.
 */
function openRoleModal(userId, currentRole) {
    if (!roleModalInstance) {
        console.error('Lỗi: Role Modal chưa được khởi tạo.');
        return;
    }

    // Gán dữ liệu vào Modal
    document.getElementById('roleUserIdDisplay').textContent = userId; // Hiển thị ID user
    document.getElementById('currentRoleDisplay').textContent = currentRole;
    document.getElementById('roleUserId').value = userId;
    
    // Tạm thời hardcode danh sách Role và Role ID (PHẢI KHỚP VỚI DB)
    const roles = [
        { id: 1, name: 'EV_RENTER' },
        { id: 2, name: 'STATION_STAFF' },
        { id: 3, name: 'ADMIN' },
    ];

    const roleSelect = document.getElementById('newRoleSelect');
    roleSelect.innerHTML = ''; 
    
    roles.forEach(role => {
        const option = document.createElement('option');
        option.value = role.id;
        option.textContent = role.name;
        if (role.name === currentRole) {
            option.selected = true;
        }
        roleSelect.appendChild(option);
    });

    roleModalInstance.show();
}

/**
 * Xử lý việc gửi yêu cầu cập nhật Role lên Backend.
 */
async function updateUserRole() {
    const userId = document.getElementById('roleUserId').value;
    const newRoleId = document.getElementById('newRoleSelect').value;
    const newRoleName = document.getElementById('newRoleSelect').options[document.getElementById('newRoleSelect').selectedIndex].text;
    const token = localStorage.getItem('token');
    
    if (!token) {
        roleModalInstance.hide();
        return showMessage('Vui lòng đăng nhập lại.', 'danger');
    }

    if (!confirm(`Xác nhận đổi Role User ID ${userId} thành ${newRoleName}?`)) {
        return;
    }
    
    const showMessageAdmin = window.showMessage || function(msg, type) { console.log(msg); };

    try {
        // GỌI API PUT /admin/station/users/{id}/role
        const response = await fetch(`${API_BASE_USER}/users/${userId}/role`, { 
            method: 'PUT',
            headers: { 
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            // GỬI roleId DƯỚI DẠNG JSON BODY
            body: JSON.stringify({ roleId: newRoleId }) 
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            // Cập nhật thông báo lỗi từ backend
            throw new Error(errorData.message || `Cập nhật Role thất bại.`);
        }
        
        // Đóng Modal và hiển thị thông báo
        roleModalInstance.hide();
        showMessageAdmin(`✅ Cập nhật Role User ID ${userId} thành ${newRoleName} thành công!`, 'success');
        
        // Tải lại danh sách
        fetchUsers(); 
        
    } catch (error) {
        console.error('Lỗi cập nhật Role:', error);
        roleModalInstance.hide();
        showMessageAdmin(`❌ Lỗi cập nhật Role: ${error.message}`, 'danger');
    }
}


// --- LOGIC HIỂN THỊ VÀ THAO TÁC USER ---

/**
 * Hiển thị danh sách người dùng trong tab 'Người Dùng'.
 */
async function fetchUsers() {
    const token = localStorage.getItem('token');
    const tableBody = document.getElementById('usersTableBody');
    tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted"><div class="spinner-border spinner-border-sm me-2" role="status"></div> Đang tải danh sách người dùng...</td></tr>';

    if (!token) {
        tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger fw-bold">❌ Lỗi: Không có token xác thực.</td></tr>';
        return;
    }

    try {
        // GỌI API: GET /admin/station/users
        const response = await fetch(`${API_BASE_USER}/users`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const errorMessage = errorData.message || 'Không thể tải dữ liệu.';
            
            if (response.status === 403) {
                 tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger fw-bold">❌ Lỗi 403: Bạn không có quyền truy cập quản lý người dùng.</td></tr>';
            } else {
                 tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger fw-bold">❌ Lỗi ${response.status}: ${errorMessage}</td></tr>`;
            }
            return;
        }

        const users = await response.json();
        
        tableBody.innerHTML = '';
        if (users.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Không có người dùng nào trong hệ thống.</td></tr>';
            return;
        }

        users.forEach(user => {
            const userId = user.userId || user.id;
            const displayName = user.fullName || user.username || user.email || 'N/A';
            const displayRole = (user.role && user.role.roleName) ? user.role.roleName : (user.role || 'USER'); 
            const userEmail = user.email || 'N/A';
            const userStatus = user.status || 'UNKNOWN';

            if (!userId) return;

            const row = document.createElement('tr');
            row.setAttribute('onclick', `displayUserDetailModal(${userId})`);
            row.style.cursor = 'pointer'; 

            row.innerHTML = `
                <td class="small">${userId}</td> 
                <td class="small fw-bold">${displayName}</td>
                <td class="small">${userEmail}</td>
                <td class="small">
                    <span class="badge ${displayRole.toUpperCase().includes('ADMIN') ? 'bg-danger' : 'bg-secondary'}">${displayRole}</span>
                    
                    <button onclick="event.stopPropagation(); openRoleModal(${userId}, '${displayRole}')" 
                            class="btn btn-sm btn-info text-white ms-2" title="Chỉnh sửa Role">
                        <i class="fas fa-user-tag"></i>
                    </button>
                    
                </td>
                <td>
                    <button onclick="event.stopPropagation(); handleUserStatusChange(${userId}, '${userStatus}')" 
                            class="btn btn-sm ${userStatus === 'ACTIVE' ? 'btn-outline-danger' : 'btn-outline-success'}">
                        <i class="fas fa-${userStatus === 'ACTIVE' ? 'lock' : 'unlock'}"></i> 
                        ${userStatus === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });

    } catch (error) {
        console.error('Lỗi khi tải danh sách người dùng:', error);
        tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-danger fw-bold">❌ Lỗi kết nối mạng hoặc lỗi server.</td></tr>';
    }
}


/**
 * Tải và hiển thị chi tiết User trong Modal.
 */
async function displayUserDetailModal(userId) {
    if (!userDetailModalInstance) {
        console.error('Lỗi: Modal chưa được khởi tạo.');
        return;
    }

    const token = localStorage.getItem('token');
    const modalBody = document.getElementById('userDetailBody');
    const modalTitle = document.getElementById('userDetailModalLabel');
    const actionButton = document.getElementById('userActionButton');

    modalTitle.textContent = `Chi tiết User ID: ${userId}`;
    modalBody.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-danger" role="status"></div><p class="mt-2">Đang tải thông tin chi tiết...</p></div>';
    actionButton.style.display = 'none';

    try {
        // GỌI API: GET /admin/station/users/{id}
        const response = await fetch(`${API_BASE_USER}/users/${userId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            throw new Error(`Lỗi ${response.status}: Không tìm thấy User hoặc lỗi server.`);
        }

        const user = await response.json();
        
        const userStatus = user.status || 'UNKNOWN';
        const userRole = (user.role && user.role.roleName) || user.role || 'USER';
        const safeName = user.fullName || user.username || user.email || 'N/A';
        
        // Mật khẩu thực (từ Backend), sẽ được hiển thị ẩn
        const actualPassword = user.password || 'N/A';
        
        let detailHtml = `
            <table class="table table-sm table-bordered">
                <tr><th>ID</th><td>${user.userId || user.id}</td></tr>
                <tr><th>Họ và tên</th><td>${safeName}</td></tr>
                <tr><th>Email</th><td>${user.email || 'N/A'}</td></tr>
                <tr><th>Số điện thoại</th><td>${user.phone || 'N/A'}</td></tr>
                <tr><th>Trạng thái</th><td><span class="badge bg-${userStatus === 'ACTIVE' ? 'success' : 'danger'}">${userStatus}</span></td></tr>
                <tr><th>Vai trò</th><td><span class="badge bg-${userRole.includes('ADMIN') ? 'danger' : 'secondary'}">${userRole}</span></td></tr>
                
                <tr class="table-info">
                    <th>Password</th>
                    <td>
                        <div class="input-group input-group-sm">
                            <input type="password" 
                                   id="modalUserPassword" 
                                   class="form-control form-control-plaintext" 
                                   value="${actualPassword}" 
                                   readonly 
                                   style="border: none; background: transparent;">
                            <button class="btn btn-sm btn-outline-secondary" type="button" onclick="toggleUserDetailPassword()">
                                <i id="passwordToggleIcon" class="fas fa-eye-slash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
                </table>
            
            <h6 class="mt-4 mb-2">Thông tin Xác minh</h6>
            <table class="table table-sm table-striped">
                <tr><th>Trạng thái Xác minh</th><td>${user.verificationStatus || 'N/A'}</td></tr>
                <tr><th>Lý do từ chối</th><td>${user.rejectionReason || 'Không có'}</td></tr>
                <tr><th>Đường dẫn CCCD (1)</th><td>${user.cccdPath1 || 'N/A'}</td></tr>
                <tr><th>Đường dẫn GPLX (1)</th><td>${user.gplxPath1 || 'N/A'}</td></tr>
            </table>
        `;
        
        modalBody.innerHTML = detailHtml;

        // --- Cập nhật nút thao tác ---
        actionButton.textContent = userStatus === 'ACTIVE' ? 'Khóa Tài Khoản' : 'Mở Khóa Tài Khoản';
        actionButton.className = `btn btn-lg w-100 mt-3 btn-${userStatus === 'ACTIVE' ? 'danger' : 'success'}`;
        actionButton.setAttribute('onclick', `handleUserStatusChange(${userId}, '${userStatus}', true)`);
        actionButton.style.display = 'block';

        userDetailModalInstance.show();

    } catch (error) {
        console.error('Lỗi khi tải chi tiết user:', error);
        modalBody.innerHTML = `<div class="alert alert-danger">❌ ${error.message}</div>`;
        userDetailModalInstance.show();
    }
}


/**
 * Xử lý sự kiện Khóa/Mở khóa tài khoản (Từ danh sách hoặc từ Modal)
 * @param {number} userId - ID người dùng
 * @param {string} currentStatus - Trạng thái hiện tại ('ACTIVE'/'INACTIVE')
 */
async function handleUserStatusChange(userId, currentStatus) {
    const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const token = localStorage.getItem('token');
    
    if (!token) return showMessage('Vui lòng đăng nhập lại.', 'danger');

    if (!confirm(`Bạn có chắc chắn muốn ${newStatus === 'ACTIVE' ? 'mở khóa' : 'khóa'} tài khoản ID ${userId} không?`)) {
        return;
    }

    // Tạo tham chiếu đến hàm showMessage trong admin.html
    const showMessageAdmin = window.showMessage || function(msg, type) { console.log(msg); };

    try {
        // GỌI API: PATCH /admin/station/users/{userId}/status?status={newStatus}
        const response = await fetch(`${API_BASE_USER}/users/${userId}/status?status=${newStatus}`, { 
            method: 'PATCH',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `Cập nhật trạng thái thất bại.`);
        }

        showMessageAdmin(`✅ Cập nhật trạng thái User ID ${userId} thành ${newStatus} thành công!`, 'success');
        
        // Đóng Modal chi tiết nếu đang mở
        if (userDetailModalInstance) userDetailModalInstance.hide();
        // Tải lại danh sách
        fetchUsers(); 
        
    } catch (error) {
        console.error('Lỗi cập nhật trạng thái:', error);
        showMessageAdmin(`❌ Lỗi: ${error.message}`, 'danger');
    }
}

/**
 * Hàm Khởi tạo Quản lý Người Dùng. Được gọi khi chuyển sang tab 'user'.
 */
function initUserManagement() {
    console.log('Khởi tạo Tab Quản lý Người Dùng...');
    fetchUsers();
}