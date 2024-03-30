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

## MongoDB - Contenedor

Trabajaremos con la base de datos `mongodb`, para eso creamos su servicio con sus configuraciones correspondientes.
Además, crearemos un servicio adicional con el que podremos interactuar con la base de datos de mongo,
ese servicio será `mongo-express`, aunque en mi computadora tengo instalado el `Studio-3T` que es lo mismo que usaría
para poder interactura con la base de datos de mongo.

````yml
services:
  mongodb:
    image: mongo:6-jammy
    container_name: mongo_db
    restart: unless-stopped
    ports:
      - 27017:27017
    volumes:
      - mongo:/data/db
    environment:
      MONGO_INITDB_ROOT_USERNAME: magadiflo
      MONGO_INITDB_ROOT_PASSWORD: magadiflo
  mongo-express:
    image: mongo-express
    container_name: mongo_express
    restart: unless-stopped
    ports:
      - 8081:8081
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: magadiflo
      ME_CONFIG_MONGODB_ADMINPASSWORD: magadiflo
      ME_CONFIG_MONGODB_SERVER: mongodb
volumes:
  mongo:
    name: mongo
````

**IMPORTANTE**

Para poder usar `mongo-express` en el navegador ingresamos mediante la siguiente url: `http://localhost:8081`:

- **username:** admin
- **password:** `pass`

## Configurando application.yml

````yml
server:
  port: 8080
  error:
    include-message: always

spring:
  application:
    name: sb-websocket-one-chat

  data:
    mongodb:
      username: magadiflo
      password: magadiflo
      host: localhost
      port: 27017
      database: db_sb_websocket_one_chat
      authentication-database: admin
````

## Configurando WebSocket

`registry.addEndpoint("/web-socket")`, registre un endpoint `STOMP` sobre `WebSocket` en la ruta de mapeo proporcionada.
Este endpoint será el que usaremos cuando iniciemos la conexión con WebSocket desde la aplicación cliente al Servidor.

`registry.enableSimpleBroker("/user")`, habilite un **intermediario de mensajes simple (simple message broker)** y
configure uno o más prefijos para filtrar destinos dirigidos al **intermediario (broker)** (por ejemplo, destinos con el
prefijo `/topic`). En nuestro caso, el prefijo definido será `/user`.

`registry.setApplicationDestinationPrefixes("/app")`, configure uno o más prefijos para filtrar destinos dirigidos a
métodos anotados en la aplicación. Por ejemplo, los destinos con el prefijo `/app` pueden procesarse mediante métodos
anotados, mientras que otros destinos pueden apuntar al intermediario de mensajes (por ejemplo, `/topic`, `/queue`).
Cuando se procesan mensajes, el prefijo coincidente se elimina del destino para formar la ruta de búsqueda. Esto
significa que las anotaciones no deben contener el prefijo de destino.
A los prefijos que no tengan una barra diagonal se les agregará una automáticamente.

`registry.setUserDestinationPrefix("/user")`, configure el prefijo utilizado para identificar los destinos de los
usuarios. Los destinos de usuario brindan la posibilidad de que un usuario se suscriba a nombres de cola exclusivos de
su sesión, así como de que otros envíen mensajes a esas colas únicas y específicas del usuario.
Por ejemplo, cuando un usuario intenta suscribirse a `/user/queue/position-updates`, el destino puede traducirse
a `/queue/position-updates-useri9oqdfzo`, lo que genera un nombre de cola único que no colisiona con ningún otro usuario
que intente hacer lo mismo. Posteriormente, cuando los mensajes se envían a `/user/{username}/queue/position-updates`,
el destino se traduce a `/queue/position-updates-useri9oqdfzo`.
El prefijo predeterminado utilizado para identificar dichos destinos es `/user/`.

````java

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/web-socket")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper());
        converter.setContentTypeResolver(resolver);

        messageConverters.add(converter);
        return false; // No queremos usar los valores predeterminados sino el que acabamos de configurar
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
````

El método `configureMessageConverters()`, permite configurar los convertidores de mensajes que se utilizarán al extraer
la carga útil de los mensajes en métodos anotados y al enviar mensajes (por ejemplo, a través del "intermediario"
SimpMessagingTemplate).

La lista proporcionada, inicialmente vacía, se puede usar para agregar convertidores de mensajes, mientras que el valor
de retorno booleano se usa para determinar si también se debe agregar el mensaje predeterminado.

````
Parámetros:
messageConverters: los convertidores a configurar (inicialmente una lista vacía)

Devoluciones:
si agregar también el convertidor predeterminado o no
````

## Document User

Será como nuestra entidad, pero como estamos trabajando con MongoDb será nuestro documento que estará mapeado a la
colección `users` en la base de datos de mongoDB.

````java
public enum Status {
    ONLINE,
    OFFLINE
}
````

````java

@Getter
@Setter
@Document(collection = "users")
public class User {
    @Id
    private String nickName;
    private String fullName;
    private Status status;
}
````

## Repositorio

````java
public interface UserRepository extends MongoRepository<User, String> {
    List<User> findAllByStatus(Status status);
}
````

## Servicio

````java
public interface UserService {
    void saveUser(User user);

    void disconnect(User user);

    List<User> findConnectedUsers();
}
````

````java

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public void saveUser(User user) {
        user.setStatus(Status.ONLINE);
        this.userRepository.save(user);
    }

    @Override
    public void disconnect(User user) {
        Optional<User> userDBOptional = this.userRepository.findById(user.getNickName());
        if (userDBOptional.isPresent()) {
            User userDB = userDBOptional.get();
            userDB.setStatus(Status.OFFLINE);
            this.userRepository.save(userDB);
        }
    }

    @Override
    public List<User> findConnectedUsers() {
        return this.userRepository.findAllByStatus(Status.ONLINE);
    }
}
````

## Controlador de usuario

````java

@RequiredArgsConstructor
@Controller
public class UserController {

    private final UserService userService;

    @MessageMapping("/user.addUser")
    @SendTo("/user/topic") // Para informar que un nuevo usuario se ha conectado. Esta cola será creado automáticamente
    public User addUser(@Payload User user) {
        this.userService.saveUser(user);
        return user;
    }

    @MessageMapping("/user.disconnectUser")
    @SendTo("/user/topic") // Notificaremos a la misma cola que algún usuario está desconectado
    public User disconnect(@Payload User user) {
        this.userService.disconnect(user);
        return user;
    }

    @GetMapping(path = "/users")
    public ResponseEntity<List<User>> findConnectedUsers() {
        return ResponseEntity.ok(this.userService.findConnectedUsers());
    }
}
````

## Document ChatRoom

````java
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "chat_rooms")
public class ChatRoom {
    @Id
    private String id;
    private String chatId;
    private String senderId;
    private String recipientId;
}
````