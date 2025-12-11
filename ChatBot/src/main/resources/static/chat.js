document.addEventListener('DOMContentLoaded', () => {

    // =========================================================================
    // NUEVAS VARIABLES GLOBALES PARA AUTENTICACIÓN Y CONEXIÓN
    // =========================================================================
    const BACKEND_URL = 'http://localhost:8080'; // <-- AJUSTA EL PUERTO SI ES NECESARIO
    let authToken = localStorage.getItem('authToken'); // Recupera el token al cargar

    // Elementos del Modal de Autenticación (Asegúrate que el HTML tenga estos IDs)
    const authModal = document.getElementById('auth-modal');
    const authForm = document.getElementById('auth-form');
    const authSubmitBtn = document.getElementById('auth-submit-btn');
    const authTitle = document.getElementById('auth-title');
    const authNameInput = document.getElementById('auth-name');
    const authEmailInput = document.getElementById('auth-email');
    const authPasswordInput = document.getElementById('auth-password');
    const authError = document.getElementById('auth-error');
    let isRegistering = false; // Estado inicial: Login

    // Elementos del DOM existentes
    const messageInput = document.querySelector('.input-area input');
    const chatMessagesContainer = document.querySelector('.chat-messages');
    const navItems = document.querySelectorAll('.nav-item');
    const tabButtons = document.querySelectorAll('.tab-btn');
    const breathCircle = document.querySelector('.breath-circle');
    const breathText = document.querySelector('.breath-text');

    // ... (El resto de tus variables de avatares y respuestas por mood se mantienen)
    const momoAvatars = [
        'amor.png',
        // ... (resto de avatares)
        'triste.png'
    ];
    const momoResponsesByMood = { };

    let currentMood = 'general';

    function scrollToBottom() {
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    /**
     * Crear mensaje del usuario
     */
    function createSentMessage(text) {
        const inputArea = document.querySelector('.input-area');

        const messageHTML = `
            <div class="message message-sent">
                <div class="message-content">
                    <p>${escapeHtml(text)}</p>
                </div>
            </div>
        `;

        inputArea.insertAdjacentHTML('beforebegin', messageHTML);
        scrollToBottom();
    }

    /**
     * Crear respuesta de MoMo
     */
    function createReceivedMessage(text) {
        const inputArea = document.querySelector('.input-area');
        const randomAvatar = momoAvatars[Math.floor(Math.random() * momoAvatars.length)];

        const messageHTML = `
            <div class="message message-received">
                <img src="${randomAvatar}" alt="MoMo" class="avatar"
                     onerror="this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22%3E%3Ccircle cx=%2250%22 cy=%2250%22 r=%2240%22 fill=%22%23d4a574%22/%3E%3Ccircle cx=%2235%22 cy=%2245%22 r=%225%22 fill=%22%23333%22/%3E%3Ccircle cx=%2265%22 cy=%2245%22 r=%225%22 fill=%22%23333%22/%3E%3Cpath d=%22M 35 65 Q 50 75 65 65%22 stroke=%22%23333%22 stroke-width=%223%22 fill=%22none%22/%3E%3C/svg%3E'">
                <div class="message-content">
                    <p>${escapeHtml(text)}</p>
                </div>
            </div>
        `;

        inputArea.insertAdjacentHTML('beforebegin', messageHTML);
        scrollToBottom();
    }

    /**
     * Escapar HTML
     */
    function escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, m => map[m]);
    }

    // =========================================================================
    // NUEVA FUNCIÓN: Chequeo de Autenticación y Control del Modal
    // =========================================================================
    function checkAuth() {
        if (authToken && authModal) {
            authModal.style.display = 'none'; // Token existe: ocultar modal
            // Iniciar la conversación con el mensaje de bienvenida
            if (chatMessagesContainer.children.length === 0) {
                 createReceivedMessage("¡Hola! Soy MoMo. Me alegra tenerte aquí. ¿En qué puedo ayudarte hoy? 🌟");
            }
        } else if (authModal) {
            authModal.style.display = 'flex'; // Token NO existe: mostrar modal
        }
    }

    // -------------------------------------------------------------------------
    // EVENTOS DEL MODAL DE AUTENTICACIÓN
    // -------------------------------------------------------------------------

    // Manejar el cambio entre Login y Registro
    const switchToRegisterLink = document.getElementById('switch-to-register');
    if (switchToRegisterLink) {
        switchToRegisterLink.addEventListener('click', (e) => {
            e.preventDefault();
            isRegistering = !isRegistering;
            authTitle.textContent = isRegistering ? 'Crear Cuenta' : 'Iniciar Sesión';
            authSubmitBtn.textContent = isRegistering ? 'Registrarse y Entrar' : 'Entrar';
            authNameInput.style.display = isRegistering ? 'block' : 'none';
            authError.textContent = ''; // Limpiar errores
        });
    }

    // Manejar el envío del formulario (Login/Register)
    if (authForm) {
        authForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            authError.textContent = '';

            const email = authEmailInput.value;
            const password = authPasswordInput.value;
            const name = authNameInput.value;

            // Usamos las rutas de UsuarioController.java (/api/auth/...)
            const endpoint = isRegistering ? '/api/auth/register' : '/api/auth/login';
            const body = isRegistering
                ? { nombre: name, email: email, password: password }
                : { email: email, password: password };

            authSubmitBtn.disabled = true; // Deshabilitar para evitar envíos múltiples

            try {
                const response = await fetch(BACKEND_URL + endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(body)
                });

                const data = await response.json();

                if (response.ok) {
                    // ÉXITO: El token viene en la respuesta del backend
                    authToken = data.token;
                    localStorage.setItem('authToken', authToken);
                    checkAuth(); // Oculta el modal y muestra el chat
                } else {
                    // ERROR: Credenciales inválidas o email ya existe
                    authError.textContent = data.message || "Credenciales inválidas o error en el servidor.";
                }
            } catch (error) {
                console.error("Error de conexión:", error);
                authError.textContent = 'Error de conexión con el servidor. Verifica que el backend esté activo.';
            } finally {
                authSubmitBtn.disabled = false;
            }
        });
    }

    // =========================================================================
    // FUNCIÓN MODIFICADA: Enviar Mensaje a Spring/Gemini
    // =========================================================================
    async function handleMessageSend() { // Añadido 'async'
        const messageText = messageInput.value.trim();

        if (messageText === '') return;

        // 1. Verificar Autenticación antes de enviar
        if (!authToken) {
            createReceivedMessage("Necesitas iniciar sesión para chatear. Por favor, inicia sesión.");
            checkAuth(); // Muestra el modal
            return;
        }

        // 2. Mostrar mensaje del usuario y limpiar input
        createSentMessage(messageText);
        messageInput.value = '';

        // Deshabilitar el input para simular "pensamiento"
        messageInput.disabled = true;

        try {
            // 3. Llamada al Backend con el Token JWT
            const response = await fetch(BACKEND_URL + '/api/chat/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authToken}` // <-- ENVÍO DEL TOKEN JWT
                },
                body: JSON.stringify({ message: messageText }) // El DTO ChatRequest
            });

            if (response.ok) {
                const data = await response.json();
                // El DTO ChatResponse tiene el campo 'response'
                createReceivedMessage(data.response);
            } else if (response.status === 401 || response.status === 403) {
                // Token inválido/expirado/no autorizado
                createReceivedMessage("Tu sesión ha expirado o no tienes permiso. Por favor, inicia sesión de nuevo.");
                // Limpiar el token para forzar el login
                authToken = null;
                localStorage.removeItem('authToken');
                checkAuth(); // Muestra el modal
            } else {
                // Otro error del servidor
                createReceivedMessage("Error del servidor: No pude obtener una respuesta de MoMo.");
            }
        } catch (error) {
            console.error("Error al enviar mensaje:", error);
            createReceivedMessage('Error de conexión. Verifica que el servidor de chat esté activo.');
        } finally {
            messageInput.disabled = false; // Habilitar el input de nuevo
            messageInput.focus();
        }
    }

    // -------------------------------------------------------------------------
    // EVENT LISTENERS ORIGINALES (MODIFICADOS PARA USAR la nueva handleMessageSend)
    // -------------------------------------------------------------------------

    // Event listeners para enviar mensajes
    if (messageInput) {
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleMessageSend(); // Usa la función asíncrona
            }
        });
    }

    const sendButton = document.querySelector('.heart-icon');
    if (sendButton) {
        sendButton.addEventListener('click', handleMessageSend); // Usa la función asíncrona
    }

    // Navegación del sidebar (se mantiene)
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');
        });
    });

    // Pestañas de mood (se mantiene, pero el mensaje automático se elimina ya que el chat está activo)
    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            tabButtons.forEach(tab => tab.classList.remove('active'));
            btn.classList.add('active');
            currentMood = btn.dataset.mood;

            // Mensaje automático al cambiar mood (ELIMINADO para no interferir con la IA)
            // setTimeout(() => { ... }, 500);
        });
    });

    // Reproductor de audio (se mantiene)
    const playButton = document.querySelector('.play-btn');
    if (playButton) {
        playButton.addEventListener('click', function() {
            if (this.textContent === '▶') {
                this.textContent = '⏸';
                console.log('🎵 Reproduciendo sonidos de naturaleza...');
            } else {
                this.textContent = '▶';
                console.log('⏸ Pausado');
            }
        });
    }

    // Ejercicio de respiración (se mantiene)
    if (breathCircle && breathText) {
        let breathPhase = 0;
        const breathPhrases = ['Inhala', 'Sostén', 'Exhala', 'Sostén'];

        setInterval(() => {
            breathText.textContent = breathPhrases[breathPhase];
            breathPhase = (breathPhase + 1) % breathPhrases.length;
        }, 4000);
    }

    // Inicializar y chequear Auth
    scrollToBottom();
    checkAuth(); // <-- MUESTRA EL MODAL SI NO HAY TOKEN, O INICIA EL CHAT SI LO HAY

});