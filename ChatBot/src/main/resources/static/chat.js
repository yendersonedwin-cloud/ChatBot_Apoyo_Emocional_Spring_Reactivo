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

    const chatMessagesContainer = document.getElementById('chat-messages-container'); // *MODIFICADO* - Usamos el ID
    const inputArea = document.getElementById('input-area'); // *NUEVO* - Necesario para ocultar
    const resourcesContainer = document.getElementById('resources-container'); // *NUEVO*
    const profileContainer = document.getElementById('profile-container'); // *NUEVO*
    const navItems = document.querySelectorAll('.nav-item'); // *NUEVO* - Todos los botones de navegación
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
        // Definir la imagen/ícono y la estructura
        const isSent = type === 'sent';
        const className = isSent ? 'message-sent' : 'message-received';

        // MODIFICACIÓN CLAVE AQUÍ: Lógica para diferenciar el avatar
        const avatarContent = isSent
            ? '<i class="fas fa-user-circle avatar-icon"></i>' // Ícono para mensajes ENVIADOS por el usuario
            : `<img src="momo.png" alt="Avatar" class="avatar">`; // Imagen para mensajes RECIBIDOS de Momo

        const textHtml = `<div class="message-bubble"><p>${escapeHtml(text)}</p></div>`;

        // Estructura: [Avatar (Recibido) | Burbuja] O [Burbuja | Avatar (Enviado)]
        const html = `<div class="message ${className}">
            ${isSent ? textHtml + avatarContent : avatarContent + textHtml}
        </div>`;

        chatMessagesContainer.insertAdjacentHTML('beforeend', html);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    function receivedMessage(text) {
        // Esconde el 'welcome-avatar' (el mono grande) al recibir el primer mensaje
        const welcomeAvatar = document.getElementById('welcome-avatar');
        if (welcomeAvatar) welcomeAvatar.style.display = 'none';

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
       const welcomeAvatar = document.getElementById('welcome-avatar');

       // Muestra el avatar grande al cargar el historial, por defecto
       if (welcomeAvatar) welcomeAvatar.style.display = 'flex';

       try {
           const res = await fetch(`${BACKEND_URL}/api/chat/history`, {
               method: 'GET',
               headers: {
                   'Authorization': `Bearer ${authToken}`
               }
           });

           if (res.status === 401) return logout();

           const history = await res.json();

           // Esconde el avatar grande si SÍ hay historial que mostrar
           if (history.length > 0) {
               if (welcomeAvatar) welcomeAvatar.style.display = 'none';
           }

           history.reverse().forEach(interaction => {
               addMessage(interaction.mensajeUsuario, 'sent');
               addMessage(interaction.respuestaChatbot, 'received');
           });

           if (history.length === 0) {
                receivedMessage('¡Hola! Soy MoMo. Estoy aquí para escucharte y ayudarte a sentirte mejor. ¿Cómo te sientes hoy?');
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

       // Esconde el avatar grande al enviar el primer mensaje
       const welcomeAvatar = document.getElementById('welcome-avatar');
       if (welcomeAvatar) welcomeAvatar.style.display = 'none';

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
   // NUEVA FUNCIONALIDAD: CERRAR SESIÓN DESDE EL MENÚ INFERIOR
   // =========================================================================

   // Agrega este bloque de código al final de tu archivo chat.js
   document.getElementById('logout-nav-item').addEventListener('click', () => {
       // Pide confirmación antes de cerrar
       if (confirm('¿Estás seguro de que quieres cerrar la sesión?')) {
           logout();
       }
   });
   // =========================================================================
   // NUEVA FUNCIONALIDAD: NAVEGACIÓN INFERIOR (RECURSOS/PERFIL)
   // =========================================================================

       // Función para mostrar la sección correcta y actualizar el estado activo
       function showSection(sectionName) {
           // Oculta todos los contenedores principales
           chatMessagesContainer.classList.add('hidden');
           resourcesContainer.classList.add('hidden');
           profileContainer.classList.add('hidden');

           // El área de input solo va con el chat
           inputArea.classList.add('hidden');

           // Quita el estado activo de todos los botones de navegación
           navItems.forEach(item => item.classList.remove('active'));

           // Muestra la sección deseada y activa el botón
           if (sectionName === 'inicio') {
               chatMessagesContainer.classList.remove('hidden');
               inputArea.classList.remove('hidden');
               document.querySelector('[data-section="inicio"]').classList.add('active');

           } else if (sectionName === 'recursos') {
               resourcesContainer.classList.remove('hidden');
               document.querySelector('[data-section="recursos"]').classList.add('active');

           } else if (sectionName === 'perfil') {
               profileContainer.classList.remove('hidden');
               document.querySelector('[data-section="perfil"]').classList.add('active');

               // Actualiza la información del perfil si el usuario está logueado
               const storedEmail = localStorage.getItem('userEmail'); // Asume que guardas el email
               const storedName = localStorage.getItem('userName'); // Asume que guardas el nombre

               document.getElementById('profile-email').textContent = storedEmail || 'No logueado';
               document.getElementById('profile-name').textContent = storedName || 'Invitado';
           }
       }

       // -------------------------------------------------------------------------
       // Event Listeners para la barra de navegación (bottom-nav)
       // -------------------------------------------------------------------------

       navItems.forEach(item => {
           item.addEventListener('click', () => {
               const section = item.getAttribute('data-section');
               if (section && section !== 'cerrar') {
                   showSection(section);
               }
               // La lógica de 'cerrar' ya está en el listener 'logout-nav-item'
           });
       });
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