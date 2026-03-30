// DIFERENCIA vs Node:
// En Node requireAuth verificaba la cookie de sesión via API.me().
// Aquí hace lo mismo — pero si las credenciales no están en memoria
// (porque el usuario cerró la pestaña), API.me() falla con 401
// y redirige al login. Comportamiento correcto para stateless.

const requireAuth = async () => {
    try {
        const { user } = await API.me();
        const el = document.getElementById('navbar-username');
        if (el) el.textContent = user.username;
        return user;
    } catch {
        window.location.href = '/login.html';
    }
};

const requireGuest = async () => {
    try {
        await API.me();
        window.location.href = '/dashboard.html';
    } catch {
        // sin credenciales válidas en memoria, puede continuar
    }
};