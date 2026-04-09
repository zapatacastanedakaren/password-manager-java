/**
 * API Client - Password Manager
 * Versión GLOBAL (sin exports/imports)
 */

// ✅ Credenciales - se cargan al iniciar
let _credentials = null;

// ✅ Cargar desde sessionStorage INMEDIATAMENTE
(function initCredentials() {
    try {
        const stored = sessionStorage.getItem('pm_credentials');
        if (stored) {
            _credentials = JSON.parse(stored);
            console.log('✅ [API] Credenciales cargadas:', _credentials.email);
        }
    } catch (e) {
        console.error('❌ [API] Error cargando credenciales:', e);
    }
})();

// ✅ Función base para requests
const request = async (method, path, body = null) => {
    const headers = { 'Content-Type': 'application/json' };
    
    if (_credentials && _credentials.email && _credentials.password) {
        const auth = btoa(_credentials.email + ':' + _credentials.password);
        headers['Authorization'] = 'Basic ' + auth;
    }
    
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);
    
    const res = await fetch('/api' + path, opts);
    const data = await res.json();
    
    if (!res.ok) {
        throw new Error(data.message || 'Error en la solicitud');
    }
    
    return data;
};

// ✅ API GLOBAL
const API = {
    _saveCredentials: function(email, password) {
        _credentials = { email: email, password: password };
        sessionStorage.setItem('pm_credentials', JSON.stringify(_credentials));
    },
    
    _clearCredentials: function() {
        _credentials = null;
        sessionStorage.removeItem('pm_credentials');
    },
    
    getCredentials: function() {
        return _credentials;
    },
    
    register: async function(username, email, password, confirmPassword) {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: username, email: email, password: password, confirmPassword: confirmPassword })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Error al registrar');
        return data;
    },
    
    login: async function(email, password) {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, password: password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Error al iniciar sesión');
        
        // ✅ Guardar credenciales para requests futuros
        API._saveCredentials(email, password);
        return data;
    },
    
    logout: function() {
        API._clearCredentials();
    },
    
    getMe: async function() {
        return await request('GET', '/auth/me');
    },
    
    getAll: async function() {
        return await request('GET', '/passwords');
    },
    
    getOne: async function(id) {
        return await request('GET', '/passwords/' + id);
    },
    
    reveal: async function(id) {
        if (!_credentials || !_credentials.email || !_credentials.password) {
            throw new Error('No autenticado');
        }
        const auth = btoa(_credentials.email + ':' + _credentials.password);
        const res = await fetch('/api/passwords/' + id + '/reveal', {
            headers: {
                'Authorization': 'Basic ' + auth,
                'Content-Type': 'application/json'
            }
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || 'Error al revelar');
        return { password: json.password || 'No disponible' };
    },
    
    create: async function(body) {
        return await request('POST', '/passwords', body);
    },
    
    update: async function(id, body) {
        return await request('PUT', '/passwords/' + id, body);
    },
    
    remove: async function(id) {
        return await request('DELETE', '/passwords/' + id);
    }
};

// ✅ requireAuth GLOBAL
const requireAuth = async function() {
    if (!_credentials || !_credentials.email) {
        window.location.href = 'login.html';
        return null;
    }
    try {
        const data = await API.getMe();
        const user = data.user || data;
        const el = document.getElementById('navbar-username');
        if (el) el.textContent = user.username || user.email;
        return user;
    } catch (err) {
        API.logout();
        window.location.href = 'login.html';
        return null;
    }
};

// ✅ requireGuest GLOBAL
const requireGuest = async function() {
    if (_credentials && _credentials.email) {
        try {
            await API.getMe();
            window.location.href = 'dashboard.html';
            return false;
        } catch {
            API.logout();
        }
    }
    return true;
};