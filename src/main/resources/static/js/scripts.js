'use strict';

const usernamePage = document.querySelector('#username-page');
const chatPage = document.querySelector('#chat-page');
const usernameForm = document.querySelector('#usernameForm');
const messageForm = document.querySelector('#messageForm');
const messageInput = document.querySelector('#message');
const connectingElement = document.querySelector('.connecting');
const chatArea = document.querySelector('#chat-messages');
const logout = document.querySelector('#logout');

let stompClient = null;
let nickname = null;
let fullname = null;
let selectedUserId = null;

/**
 * * Lo primero que debemos hacer cuando trabajamos con WebSockets y Stomp
 * * es conectar a nuestro usuario al WebSocket de nuestro sitio web.
 */
function connect(event) {
    nickname = document.querySelector('#nickname').value.trim();
    fullname = document.querySelector('#fullname').value.trim();

    if (nickname && fullname) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');
        /**
         * * Conectándonos al WebSocket
         * * **************************
         * * /web-socket, es el endpoint que definimos en WebSocketConfig del backend.
         * * 
         * * NOTA: Si nuestro frontend estuviera desplegado en otro servidor, y el backend
         * * estuviera corriendo en el puerto, por ejemplo 3000, deberíamos colocar
         * * la url de conexión TCP completa del backend. 
         * * Ejemplo: '//localhost:3000/web-socket' // Conexión TCP, no es conexión HTTP
         * *
         * * NOTA: Como el frontEnd está corriendo en el mismo servidor del backend, entonces
         * * usamos directamente el /web-socket
        */
        const socket = new SockJS('/web-socket');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}

function onConnected() {
    /**
     * * Subscribiéndonos a distintos eventos
     * * ************************************
     * * El usuario deberá subscribirse a su propia cola, para que cada vez que reciba un mensaje
     * * sea notificado.
     */
    stompClient.subscribe(`/user/${nickname}/queue/messages`, onMessageReceived);

    // También se va a subscribir a la siguiente cola, donde el servidor publicará mensajes cuando un usuario
    // se agregue o se desconecte
    stompClient.subscribe(`/user/public`, onMessageReceived);

    /**
     * * Aquí se envía el payload al destino /app/user.addUser, donde el UserController.addUser() lo recibirá
     */
    const payload = JSON.stringify({ nickName: nickname, fullName: fullname, status: 'ONLINE' });
    stompClient.send('/app/user.addUser', {}, payload);

    document.querySelector('#connected-user-fullname').textContent = fullname;
    //* Buscar y mostrar los usuarios conectados
    findAndDisplayConnectedUsers().then();
}

async function findAndDisplayConnectedUsers() {
    const connectedUserResponse = await fetch('/users');
    let connectedUsers = await connectedUserResponse.json();
    connectedUsers = connectedUsers.filter(user => user.nickName !== nickname);

    const ulElementConnectedUsers = document.getElementById('connectedUsers');
    ulElementConnectedUsers.innerHTML = '';

    connectedUsers.forEach(user => {
        appendUserElement(user, ulElementConnectedUsers);
        if (connectedUsers.indexOf(user) < connectedUsers.length - 1) {
            //* Agrega un separador
            const separator = document.createElement('li');
            separator.classList.add('separator');
            ulElementConnectedUsers.appendChild(separator);
        }
    });

}

function appendUserElement(user, connectedUsersList) {
    const listItem = document.createElement('li');
    listItem.classList.add('user-item');
    listItem.id = user.nickName;

    const userImage = document.createElement('img');
    userImage.src = '../img/user_icon.png';
    userImage.alt = user.fullName;

    const usernameSpan = document.createElement('span');
    usernameSpan.textContent = user.fullName;

    const receivedMsgs = document.createElement('span');
    receivedMsgs.textContent = '0';
    receivedMsgs.classList.add('nbr-msg', 'hidden');

    listItem.appendChild(userImage);
    listItem.appendChild(usernameSpan);
    listItem.appendChild(receivedMsgs);

    listItem.addEventListener('click', userItemClick);

    connectedUsersList.appendChild(listItem);
}

function userItemClick(event) {
    document.querySelectorAll('.user-item').forEach(item => {
        item.classList.remove('active');
    });
    messageForm.classList.remove('hidden');

    const clickedUser = event.currentTarget;
    clickedUser.classList.add('active');

    selectedUserId = clickedUser.getAttribute('id');
    fetchAndDisplayUserChat().then();

    const nbrMsg = clickedUser.querySelector('.nbr-msg');
    nbrMsg.classList.add('hidden');
    nbrMsg.textContent = '0';
}

async function fetchAndDisplayUserChat() {
    const userChatResponse = await fetch(`/messages/${nickname}/${selectedUserId}`);
    const userChat = await userChatResponse.json();
    chatArea.innerHTML = '';
    userChat.forEach(chat => {
        displayMessage(chat.senderId, chat.content);
    });
    chatArea.scrollTop = chatArea.scrollHeight;
}

function displayMessage(senderId, content) {
    console.log('Mostrando mensaje!');
    console.log({ senderId, content });
    const messageContainer = document.createElement('div');
    messageContainer.classList.add('message');
    if (senderId === nickname) {
        messageContainer.classList.add('sender');
    } else {
        messageContainer.classList.add('receiver');
    }
    const message = document.createElement('p');
    message.textContent = content;
    messageContainer.appendChild(message);
    chatArea.appendChild(messageContainer);
}

function onError() {
    connectingElement.textContent = 'No se pudo conectar al servidor WebSocket. ¡Actualice esta página para volver a intentarlo!';
    connectingElement.style.color = 'red';
}

async function onMessageReceived(payload) {
    await findAndDisplayConnectedUsers();
    console.log('Message received', payload);

    const message = JSON.parse(payload.body);
    if (selectedUserId && selectedUserId === message.senderId) {
        displayMessage(message.senderId, message.content);
        chatArea.scrollTop = chatArea.scrollHeight;
    }

    if (selectedUserId) {
        document.querySelector(`#${selectedUserId}`).classList.add('active');
    } else {
        messageForm.classList.add('hidden');
    }

    const notifiedUser = document.querySelector(`#${message.senderId}`);
    if (notifiedUser && !notifiedUser.classList.contains('active')) {
        const nbrMsg = notifiedUser.querySelector('.nbr-msg');
        nbrMsg.classList.remove('hidden');
        nbrMsg.textContent = '';
    }
}

function sendMessage(event) {
    const messageContent = messageInput.value.trim();
    if (messageContent && stompClient) {
        const chatMessage = {
            senderId: nickname,
            recipientId: selectedUserId,
            content: messageContent,
            timestamp: new Date()
        };
        stompClient.send("/app/chat", {}, JSON.stringify(chatMessage));
        displayMessage(nickname, messageContent);
        messageInput.value = '';
    }
    chatArea.scrollTop = chatArea.scrollHeight;
    event.preventDefault();
}

function onLogout() {
    const payload = JSON.stringify({ nickName: nickname, fullName: fullname, status: 'OFFLINE' });
    stompClient.send('/app/user.disconnectUser', {}, payload);
    window.location.reload();
}

/**
 * * Cuando estableces el tercer parámetro en true, como en tu ejemplo:
 * * ******************************************************************
 * * Estás indicando que deseas que el evento se capture durante la fase de captura. 
 * * La fase de captura es la primera fase del flujo de eventos en el DOM, en la que el 
 * * evento se maneja desde el elemento raíz hasta el elemento objetivo. Esto significa que, 
 * * en este caso, cuando se envíe el formulario (submit), se capturará el evento en el 
 * * camino hacia el elemento específico al que está asociado el evento (usernameForm), 
 * * independientemente de en qué elemento exacto ocurra el evento de envío del formulario.
 * *
 * * Por lo tanto, cuando establecemos en true, el evento lo maneja desde el elemento raíz hacia el objetivo.
 * *
 * * Si pasas false o simplemente omites este parámetro, el evento se manejará durante la fase de burbujeo, que 
 * * es la fase en la que el evento se propaga desde el elemento objetivo hasta el elemento raíz.
 */
usernameForm.addEventListener('submit', connect, true);
messageForm.addEventListener('submit', sendMessage, true);
logout.addEventListener('click', onLogout, true);
//* Si el usuario actualiza la página, le haremos el logout
window.onbeforeunload = () => onLogout();