/**
 * DocMCP Server - Frontend JavaScript
 * 包含搜尋、表單處理、通知和微互動功能
 */

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    initializeSearch();
    initializeForms();
    initializeDeleteButtons();
    initializeSyncButtons();
    initializeMicroInteractions();
});

/**
 * 初始化搜尋功能
 */
function initializeSearch() {
    const searchForm = document.getElementById('search-form');
    const searchInput = document.getElementById('search-input');
    const searchResults = document.getElementById('search-results');

    if (searchForm && searchInput) {
        // 防抖搜尋
        let debounceTimer;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                const query = e.target.value.trim();
                if (query.length >= 2) {
                    performSearch(query);
                } else if (searchResults) {
                    searchResults.innerHTML = '';
                }
            }, 300);
        });

        // 表單提交
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const query = searchInput.value.trim();
            if (query) {
                performSearch(query);
            }
        });
    }
}

/**
 * 執行搜尋 API 呼叫
 */
async function performSearch(query) {
    const searchResults = document.getElementById('search-results');
    const searchMode = document.getElementById('search-mode')?.value || 'hybrid';
    const libraryId = document.getElementById('library-filter')?.value || '';

    if (!searchResults) return;

    try {
        // 顯示載入中狀態
        searchResults.innerHTML = `
            <div class="flex items-center justify-center py-8">
                <div class="spinner"></div>
                <span class="ml-3 text-secondary">搜尋中...</span>
            </div>
        `;

        const params = new URLSearchParams({ query, mode: searchMode, limit: 10 });
        if (libraryId) {
            params.append('libraryId', libraryId);
        }

        const response = await fetch(`/api/search?${params}`);
        const data = await response.json();

        if (data.items && data.items.length > 0) {
            searchResults.innerHTML = data.items.map((item, index) => `
                <div class="p-4 border-b transition-all hover:bg-gray-50"
                     style="animation: fadeIn 0.3s ease ${index * 0.05}s both;">
                    <h3 class="font-semibold text-primary">${escapeHtml(item.title)}</h3>
                    <p class="text-sm text-secondary mt-1 font-mono">${escapeHtml(item.path)}</p>
                    <p class="text-sm text-secondary mt-2 line-clamp-2">${escapeHtml(item.content)}</p>
                    <div class="flex items-center gap-4 mt-2">
                        <span class="text-xs text-tertiary">Score: ${item.score.toFixed(2)}</span>
                        ${item.chunkIndex !== null ? `<span class="text-xs text-tertiary">Chunk: ${item.chunkIndex}</span>` : ''}
                    </div>
                </div>
            `).join('');
        } else {
            searchResults.innerHTML = `
                <div class="empty-state">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                    </svg>
                    <p class="font-medium">找不到 "${escapeHtml(query)}" 的結果</p>
                    <p class="text-sm mt-2">試試其他關鍵字或調整搜尋模式</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('搜尋錯誤:', error);
        searchResults.innerHTML = `
            <div class="p-4 text-error text-center">
                <p>搜尋時發生錯誤，請稍後再試</p>
            </div>
        `;
    }
}

/**
 * 初始化表單處理
 */
function initializeForms() {
    const libraryForm = document.getElementById('library-form');
    if (libraryForm) {
        libraryForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = new FormData(libraryForm);
            const isEdit = libraryForm.dataset.mode === 'edit';
            const libraryId = libraryForm.dataset.libraryId;
            const submitBtn = libraryForm.querySelector('button[type="submit"]');

            const data = {
                name: formData.get('name'),
                displayName: formData.get('displayName'),
                description: formData.get('description'),
                sourceType: formData.get('sourceType'),
                sourceUrl: formData.get('sourceUrl'),
                category: formData.get('category'),
                tags: formData.get('tags')?.split(',').map(t => t.trim()).filter(t => t) || []
            };

            try {
                // 禁用按鈕並顯示載入狀態
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner mr-2"></span>處理中...';

                const url = isEdit ? `/api/libraries/${libraryId}` : '/api/libraries';
                const method = isEdit ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method,
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(data)
                });

                if (response.ok) {
                    showSuccess(isEdit ? 'Library 更新成功' : 'Library 建立成功');
                    setTimeout(() => {
                        window.location.href = '/libraries';
                    }, 500);
                } else {
                    const error = await response.json();
                    showError(error.detail || '操作失敗');
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = isEdit ? 'Update Library' : 'Create Library';
                }
            } catch (error) {
                console.error('表單提交錯誤:', error);
                showError('發生錯誤，請稍後再試');
                submitBtn.disabled = false;
                submitBtn.innerHTML = isEdit ? 'Update Library' : 'Create Library';
            }
        });
    }
}

/**
 * 初始化刪除按鈕
 */
function initializeDeleteButtons() {
    document.querySelectorAll('[data-delete-library]').forEach(button => {
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            const libraryId = button.dataset.deleteLibrary;
            const libraryName = button.dataset.libraryName || 'this library';

            if (confirm(`確定要刪除 ${libraryName} 嗎？此操作無法復原。`)) {
                try {
                    button.disabled = true;
                    button.innerHTML = '<span class="spinner mr-2"></span>刪除中...';

                    const response = await fetch(`/api/libraries/${libraryId}`, {
                        method: 'DELETE'
                    });

                    if (response.ok) {
                        showSuccess('Library 已刪除');
                        setTimeout(() => {
                            window.location.href = '/libraries';
                        }, 500);
                    } else {
                        showError('刪除失敗');
                        button.disabled = false;
                        button.innerHTML = 'Delete';
                    }
                } catch (error) {
                    console.error('刪除錯誤:', error);
                    showError('發生錯誤，請稍後再試');
                    button.disabled = false;
                    button.innerHTML = 'Delete';
                }
            }
        });
    });
}

/**
 * 初始化同步按鈕
 */
function initializeSyncButtons() {
    document.querySelectorAll('[data-sync-library]').forEach(button => {
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            const libraryId = button.dataset.syncLibrary;
            const version = button.dataset.version || prompt('請輸入要同步的版本：');

            if (!version) return;

            try {
                button.disabled = true;
                const originalText = button.innerHTML;
                button.innerHTML = '<span class="spinner"></span>';

                const response = await fetch(`/api/libraries/${libraryId}/sync`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ version })
                });

                if (response.ok) {
                    showSuccess('同步已開始');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    const error = await response.json();
                    showError(error.detail || '同步失敗');
                    button.disabled = false;
                    button.innerHTML = originalText;
                }
            } catch (error) {
                console.error('同步錯誤:', error);
                showError('發生錯誤，請稍後再試');
                button.disabled = false;
                button.innerHTML = 'Sync';
            }
        });
    });
}

/**
 * 初始化微互動效果
 */
function initializeMicroInteractions() {
    // 卡片 hover 效果增強（已在 CSS 處理）

    // 按鈕 ripple 效果
    document.querySelectorAll('.btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            const rect = this.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;

            const ripple = document.createElement('span');
            ripple.style.cssText = `
                position: absolute;
                background: rgba(255, 255, 255, 0.3);
                border-radius: 50%;
                transform: scale(0);
                animation: ripple 0.6s linear;
                left: ${x}px;
                top: ${y}px;
                width: 100px;
                height: 100px;
                margin-left: -50px;
                margin-top: -50px;
                pointer-events: none;
            `;

            this.style.position = 'relative';
            this.style.overflow = 'hidden';
            this.appendChild(ripple);

            setTimeout(() => ripple.remove(), 600);
        });
    });

    // 輸入框 focus 增強（已在 CSS 處理）

    // 滾動時淡入效果（使用 CSS class 避免初始不可見）
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const fadeObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
                fadeObserver.unobserve(entry.target);
            }
        });
    }, observerOptions);

    document.querySelectorAll('.glass-card, .stat-card, .action-card').forEach(el => {
        el.classList.add('animate-on-scroll');
        fadeObserver.observe(el);
    });
}

/**
 * 顯示錯誤通知
 */
function showError(message) {
    showNotification(message, 'error');
}

/**
 * 顯示成功通知
 */
function showSuccess(message) {
    showNotification(message, 'success');
}

/**
 * 顯示通知
 */
function showNotification(message, type = 'info') {
    // 移除現有通知
    document.querySelectorAll('.notification').forEach(n => n.remove());

    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="flex items-center gap-3">
            ${type === 'success' ? `
                <svg class="w-5 h-5 text-success" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
            ` : type === 'error' ? `
                <svg class="w-5 h-5 text-error" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
            ` : `
                <svg class="w-5 h-5 text-accent" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
            `}
            <span>${escapeHtml(message)}</span>
        </div>
    `;

    document.body.appendChild(notification);

    // 自動移除
    setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateX(20px)';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

/**
 * HTML 跳脫以防止 XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 格式化日期顯示
 */
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-TW', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * Alpine.js 元件：同步 Modal
 * 用於從 GitHub 選擇要同步的版本
 */
function syncModal() {
    return {
        // 狀態
        open: false,
        loading: false,
        syncing: false,
        error: null,

        // 資料
        libraryId: null,
        docsPath: 'docs',
        releases: [],

        /**
         * 開啟 Modal 並載入 GitHub Releases
         */
        async openModal() {
            // 從按鈕取得 library ID
            const button = document.querySelector('[data-library-id]');
            this.libraryId = button?.dataset.libraryId;

            if (!this.libraryId) {
                console.error('Library ID not found');
                return;
            }

            this.open = true;
            this.loading = true;
            this.error = null;

            try {
                const response = await fetch(`/api/libraries/${this.libraryId}/github-releases?limit=20`);

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                this.docsPath = data.defaultDocsPath || 'docs';
                this.releases = data.releases.map(r => ({
                    ...r,
                    selected: false
                }));
            } catch (error) {
                console.error('載入 GitHub releases 失敗:', error);
                this.error = '無法載入 GitHub Releases，請稍後再試';
            } finally {
                this.loading = false;
            }
        },

        /**
         * 關閉 Modal
         */
        closeModal() {
            if (this.syncing) return;
            this.open = false;
            this.releases = [];
            this.error = null;
        },

        /**
         * 計算已選擇的數量
         */
        get selectedCount() {
            return this.releases.filter(r => r.selected && !r.exists).length;
        },

        /**
         * 檢查是否全選（只考慮可選的項目）
         */
        get allSelectableSelected() {
            const selectableReleases = this.releases.filter(r => !r.exists);
            return selectableReleases.length > 0 &&
                   selectableReleases.every(r => r.selected);
        },

        /**
         * 切換全選
         */
        toggleSelectAll() {
            const shouldSelect = !this.allSelectableSelected;
            this.releases.forEach(r => {
                if (!r.exists) {
                    r.selected = shouldSelect;
                }
            });
        },

        /**
         * 格式化日期顯示
         */
        formatDate(dateString) {
            if (!dateString) return '-';
            const date = new Date(dateString);
            return date.toLocaleDateString('zh-TW', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            });
        },

        /**
         * 開始同步選中的版本
         * 每個版本使用其自身的 docsPath（根據版本號計算）
         */
        async startSync() {
            const selectedReleases = this.releases.filter(r => r.selected && !r.exists);

            if (selectedReleases.length === 0) {
                showError('請至少選擇一個版本');
                return;
            }

            this.syncing = true;

            try {
                const requestBody = {
                    // 每個版本使用各自的 docsPath（從 API 回傳，已根據版本號計算）
                    versions: selectedReleases.map(r => ({
                        tagName: r.tagName,
                        version: r.version,
                        docsPath: r.docsPath
                    })),
                    defaultDocsPath: this.docsPath
                };

                const response = await fetch(`/api/libraries/${this.libraryId}/batch-sync`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(requestBody)
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.detail || '同步失敗');
                }

                const data = await response.json();
                showSuccess(data.message || '同步已開始');

                // 關閉 Modal 並重新載入頁面
                this.closeModal();
                setTimeout(() => {
                    window.location.reload();
                }, 1000);

            } catch (error) {
                console.error('批次同步失敗:', error);
                showError(error.message || '同步失敗，請稍後再試');
            } finally {
                this.syncing = false;
            }
        }
    };
}

// 加入 ripple 動畫的 CSS（動態注入）
const style = document.createElement('style');
style.textContent = `
    @keyframes ripple {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);
