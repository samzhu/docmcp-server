/**
 * DocMCP Server - Frontend JavaScript
 */

// Initialize on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    initializeSearch();
    initializeForms();
    initializeDeleteButtons();
    initializeSyncButtons();
});

/**
 * Initialize search functionality
 */
function initializeSearch() {
    const searchForm = document.getElementById('search-form');
    const searchInput = document.getElementById('search-input');
    const searchResults = document.getElementById('search-results');

    if (searchForm && searchInput) {
        // Debounced search
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

        // Form submission
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
 * Perform search API call
 */
async function performSearch(query) {
    const searchResults = document.getElementById('search-results');
    const searchMode = document.getElementById('search-mode')?.value || 'hybrid';
    const libraryId = document.getElementById('library-filter')?.value || '';

    if (!searchResults) return;

    try {
        searchResults.innerHTML = '<div class="text-center py-4"><div class="spinner"></div></div>';

        const params = new URLSearchParams({ query, mode: searchMode, limit: 10 });
        if (libraryId) {
            params.append('libraryId', libraryId);
        }

        const response = await fetch(`/api/search?${params}`);
        const data = await response.json();

        if (data.items && data.items.length > 0) {
            searchResults.innerHTML = data.items.map(item => `
                <div class="p-4 border-b border-gray-200 hover:bg-gray-50 transition-colors">
                    <h3 class="font-semibold text-gray-900">${escapeHtml(item.title)}</h3>
                    <p class="text-sm text-gray-500 mt-1">${escapeHtml(item.path)}</p>
                    <p class="text-sm text-gray-600 mt-2 line-clamp-2">${escapeHtml(item.content)}</p>
                    <div class="flex items-center gap-4 mt-2">
                        <span class="text-xs text-gray-400">Score: ${item.score.toFixed(2)}</span>
                        ${item.chunkIndex !== null ? `<span class="text-xs text-gray-400">Chunk: ${item.chunkIndex}</span>` : ''}
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
                    <p>No results found for "${escapeHtml(query)}"</p>
                </div>
            `;
        }
    } catch (error) {
        console.error('Search error:', error);
        searchResults.innerHTML = `
            <div class="p-4 text-red-600">
                An error occurred while searching. Please try again.
            </div>
        `;
    }
}

/**
 * Initialize form handling
 */
function initializeForms() {
    // Library create/edit form
    const libraryForm = document.getElementById('library-form');
    if (libraryForm) {
        libraryForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = new FormData(libraryForm);
            const isEdit = libraryForm.dataset.mode === 'edit';
            const libraryId = libraryForm.dataset.libraryId;

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
                    window.location.href = '/libraries';
                } else {
                    const error = await response.json();
                    showError(error.detail || 'An error occurred');
                }
            } catch (error) {
                console.error('Form submission error:', error);
                showError('An error occurred while saving');
            }
        });
    }
}

/**
 * Initialize delete buttons
 */
function initializeDeleteButtons() {
    document.querySelectorAll('[data-delete-library]').forEach(button => {
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            const libraryId = button.dataset.deleteLibrary;
            const libraryName = button.dataset.libraryName || 'this library';

            if (confirm(`Are you sure you want to delete ${libraryName}? This action cannot be undone.`)) {
                try {
                    const response = await fetch(`/api/libraries/${libraryId}`, {
                        method: 'DELETE'
                    });

                    if (response.ok) {
                        window.location.reload();
                    } else {
                        showError('Failed to delete library');
                    }
                } catch (error) {
                    console.error('Delete error:', error);
                    showError('An error occurred while deleting');
                }
            }
        });
    });
}

/**
 * Initialize sync buttons
 */
function initializeSyncButtons() {
    document.querySelectorAll('[data-sync-library]').forEach(button => {
        button.addEventListener('click', async (e) => {
            e.preventDefault();
            const libraryId = button.dataset.syncLibrary;
            const version = button.dataset.version || prompt('Enter version to sync:');

            if (!version) return;

            try {
                button.disabled = true;
                button.innerHTML = '<span class="spinner"></span> Syncing...';

                const response = await fetch(`/api/libraries/${libraryId}/sync`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ version })
                });

                if (response.ok) {
                    const data = await response.json();
                    showSuccess('Sync started successfully');
                    // Optionally redirect to sync status page
                    // window.location.href = `/api/sync/${data.id}`;
                } else {
                    const error = await response.json();
                    showError(error.detail || 'Failed to start sync');
                }
            } catch (error) {
                console.error('Sync error:', error);
                showError('An error occurred while starting sync');
            } finally {
                button.disabled = false;
                button.innerHTML = 'Sync';
            }
        });
    });
}

/**
 * Show error notification
 */
function showError(message) {
    showNotification(message, 'error');
}

/**
 * Show success notification
 */
function showSuccess(message) {
    showNotification(message, 'success');
}

/**
 * Show notification
 */
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `fixed top-4 right-4 p-4 rounded-lg shadow-lg transition-all transform ${
        type === 'error' ? 'bg-red-100 text-red-800' :
        type === 'success' ? 'bg-green-100 text-green-800' :
        'bg-blue-100 text-blue-800'
    }`;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.opacity = '0';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Format date for display
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
