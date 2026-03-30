const API = (() => {
    const BASE = '/api';
    
    const getCredentials = () => {
        try {
            const stored = sessionStorage.getItem('pm_credentials');
            return stored ? JSON.parse(stored) : null;
        } catch { return null; }
    };
    
    const setCredentials = (creds) => {
        sessionStorage.setItem('pm_credentials', JSON.stringify(creds));
    };
    
    const clearCredentials = () => {
        sessionStorage.removeItem('pm_credentials');
    };
    
    const request = async (method, path, body = null) => {
        const creds = getCredentials();
        const headers = { 'Content-Type': 'application/json' };
        if (creds?.email && creds?.password) {
            headers['Authorization'] = 'Basic ' + btoa(creds.email + ':' + creds.password);
        }
        const opts = { method, headers };
        if (body) opts.body = JSON.stringify(body);
        const res = await fetch(`${BASE}${path}`, opts);
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Error');
        return data;
    };
    
    return {
        register: (body) => request('POST', '/auth/register', body),
        login: async (body) => {
            const data = await request('POST', '/auth/login', body);
            setCredentials({ email: body.email, password: body.password });
            return data;
        },
        logout: () => { clearCredentials(); return Promise.resolve({ status: 'success' }); },
        me: () => request('GET', '/auth/me'),
        getAll: (params = '') => request('GET', `/passwords${params}`),
        getOne: (id) => request('GET', `/passwords/${id}`),
        create: (body) => request('POST', '/passwords', body),
        update: (id, body) => request('PUT', `/passwords/${id}`, body),
        remove: (id) => request('DELETE', `/passwords/${id}`),
        reveal: (id) => request('GET', `/passwords/${id}/reveal`),
    };
})();