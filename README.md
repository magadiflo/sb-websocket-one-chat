# [Tutorial de WebSocket con Spring Boot | Cree una aplicación de chat uno a uno](https://www.youtube.com/watch?v=7T-HnTE6v64)

Tutorial tomado del canal de **Bouali Ali**

- [Tutorial previo donde se construye un chat grupal](https://github.com/magadiflo/spring_boot_websocket_chat.git)

---

## Visión general - Comunicación uno a uno

En el siguiente diagrama se muestra una comunicación de uno a uno, donde `John` publica o envía un mensaje a `Marianne`
y `Marianne` **se subscribe a su propia cola (queue)** en donde va a recibir los mensajes.

En el otro sentido sería algo similar, `Marianne` envía mensaje a `John` y `John` **se subscribe a su propia cola** en
donde va a recibir los mensajes.

![01.comunicacion-uno-a-uno.png](./assets/01.comunicacion-uno-a-uno.png)

## Flujo del chat de uno a uno

En el siguiente diagrama vemos la comunicación que se establece entre distintos usuarios con el usuario `John`.

Veamos la comunicación entre `Hunter` y `John`. Esa comunicación genera un `chatId`: `hunter_john`, en ese chatId
existe dos flujos, la primera cuando el `senderId`: `John` (quien envía el mensaje) y el `recipientId`: `Hunter` (quien
recibe el mensaje). En el siguiente flujo se intercambian los papeles, ahora `senderId`: `Hunter` y
el `recipientId`: `John`.

Finalmente, se tiene un `ChatMessage` donde se tienen los siguientes atributos: **chatId, senderId, recipientId, content
y timestamp.**

![02.flujo-chat.png](./assets/02.flujo-chat.png)

## Diagrama de la relación entre los documentos

En este proyecto usaremos MongoDb como base de datos no relacional, por lo tanto, sabemos bien que en esta base de datos
no existen relaciones, pero la imagen que se muestra a continuación es una manera de ver cómo es que se van a
vincular los documentos.

![03.estructura-documentos.png](./assets/03.estructura-documentos.png)

---

## Dependencias

````xml
<!--Spring Boot 3.2.4-->
<!--Java 21-->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
````
