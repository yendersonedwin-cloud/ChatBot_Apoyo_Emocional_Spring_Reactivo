document.addEventListener('DOMContentLoaded', () => {

    // =========================================================================
    // CONFIGURACIÓN Y ESTADO
    // =========================================================================
    const BACKEND_URL = 'http://localhost:8080'; // Asegúrate de que este puerto sea correcto
    const TOKEN_KEY = 'jwtToken';
    let authToken = localStorage.getItem(TOKEN_KEY);
    let isRegistering = false;

    // =========================================================================
    // ELEMENTOS UI
    // =========================================================================
    const authModal = document.getElementById('auth-modal');
    const authForm = document.getElementById('auth-form');
    const authTitle = document.getElementById('auth-title');
    const authSubmitBtn = document.getElementById('auth-submit-btn');
    const authNameInput = document.getElementById('auth-name');
    const authEmailInput = document.getElementById('auth-email');
    const authPasswordInput = document.getElementById('auth-password');
    const authError = document.getElementById('auth-error');
    const switchContainer = document.getElementById('switch-container');

    const chatMessagesContainer = document.querySelector('.chat-messages');
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');
    const initialMessage = document.getElementById('initial-message');

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    function showAuthModal() {
        authModal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function hideAuthModal() {
        authModal.style.display = 'none';
        document.body.style.overflow = '';
    }

    function logout() {
        localStorage.removeItem(TOKEN_KEY);
        authToken = null;
        messageInput.disabled = true;
        setRegisterMode(false);
        showAuthModal();
        alert('Sesión expirada o no autorizada. Por favor, inicia sesión.');
    }

    function escapeHtml(text = '') {
        return text.replace(/[&<>"']/g, m => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;',
            '"': '&quot;', "'": '&#039;'
        })[m]);
    }

    function addMessage(text, type) {
        const className = type === 'sent' ? 'message-sent' : 'message-received';
        const html = `<div class="message ${className}"><p>${escapeHtml(text)}</p></div>`;
        chatMessagesContainer.insertAdjacentHTML('beforeend', html);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    function receivedMessage(text) {
        if (initialMessage) initialMessage.remove();
        addMessage(text, 'received');
    }

    // =========================================================================
    // AUTH LÓGICA (Conexión al Backend)
    // =========================================================================

    function setRegisterMode(on) {
        isRegistering = on;
        authError.textContent = '';
        authTitle.textContent = on ? 'Crear cuenta' : 'Iniciar sesión';
        authSubmitBtn.textContent = on ? 'Registrarse' : 'Entrar';
        authNameInput.style.display = on ? 'block' : 'none';

        switchContainer.innerHTML = on
            ? `¿Ya tienes cuenta? <button id="switch-auth" type="button">Inicia sesión</button>`
            : `¿No tienes cuenta? <button id="switch-auth" type="button">Regístrate</button>`;
    }

    authForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        authError.textContent = '';
        authSubmitBtn.disabled = true;

        const email = authEmailInput.value.trim();
        const password = authPasswordInput.value.trim();
        const nombre = authNameInput.value.trim();

        if (!email || !password || (isRegistering && !nombre)) {
            authError.textContent = 'Completa todos los campos.';
            authSubmitBtn.disabled = false;
            return;
        }

        const endpoint = isRegistering ? '/api/auth/register' : '/api/auth/login';
        const payload = isRegistering
            ? { nombre, email, password }
            : { email, password };

        try {
            const res = await fetch(`${BACKEND_URL}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await res.json().catch(() => ({}));

            if (!res.ok) {
                authError.textContent = data.message || 'Credenciales inválidas o error de servidor.';
                return;
            }

            if (data.token) {
                // Éxito en LOGIN
                authToken = data.token;
                localStorage.setItem(TOKEN_KEY, authToken);
                hideAuthModal();
                messageInput.disabled = false;
                await loadChatHistory();
            } else {
                // Éxito en REGISTER
                authError.textContent = 'Registro exitoso. Ahora inicia sesión.';
                setRegisterMode(false);
            }

        } catch (err) {
            authError.textContent = 'Error de conexión con el servidor.';
        } finally {
            authSubmitBtn.disabled = false;
        }
    });

    document.addEventListener('click', (e) => {
        if (e.target.id === 'switch-auth') {
            setRegisterMode(!isRegistering);
            authNameInput.value = '';
            authEmailInput.value = '';
            authPasswordInput.value = '';
        }
    });

    // =========================================================================
    // CHAT LÓGICA (Conexión al Backend)
    // =========================================================================

    async function loadChatHistory() {
        if (!authToken) return;

        chatMessagesContainer.innerHTML = '';

        try {
            const res = await fetch(`${BACKEND_URL}/api/chat/history`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${authToken}`
                }
            });

            if (res.status === 401) return logout();

            const history = await res.json();

            history.reverse().forEach(interaction => {
                addMessage(interaction.mensajeUsuario, 'sent');
                addMessage(interaction.respuestaChatbot, 'received');
            });

            if (history.length === 0) {
                 receivedMessage('¡Bienvenida! 🌸 ¿Cómo te sientes hoy?');
            }

        } catch (error) {
            receivedMessage('Error al cargar el historial.');
        }
    }


    async function sendMessage() {
        const text = messageInput.value.trim();
        if (!text) return;

        if (!authToken) {
            showAuthModal();
            return;
        }

        addMessage(text, 'sent');
        messageInput.value = '';
        messageInput.disabled = true;

        try {
            const res = await fetch(`${BACKEND_URL}/api/chat/message`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authToken}`
                },
                body: JSON.stringify({ message: text })
            });

            if (res.status === 401) return logout();

            const data = await res.json();
            receivedMessage(data.response || 'Error de respuesta de la IA.');
        } catch {
            receivedMessage('Error de conexión con el servicio de chat.');
        } finally {
            messageInput.disabled = false;
            messageInput.focus();
        }
    }

    sendButton.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', e => e.key === 'Enter' && sendMessage());

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================

    setRegisterMode(false);

    if (!authToken) {
        showAuthModal();
    } else {
        messageInput.disabled = false;
        loadChatHistory();
    }
});