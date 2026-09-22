# Containerizing and Deploying a Java Web Application

Workshop del curso TDSE — Taller de virtualización, empaquetado con Docker y despliegue de una aplicación web Java.

## Estado del taller

| Parte | Descripción | Estado |
|---|---|---|
| 1 | Aplicación web Java con Spring Boot | ✅ Completa |
| 2 | Imagen Docker + contenedores aislados | ✅ Completa |
| 3 | Entorno multi-contenedor con Docker Compose | ✅ Completa (con una limitación documentada, ver abajo) |
| 4 | Despliegue en AWS EC2 + análisis de costos | ⏳ Pendiente |

## Stack tecnológico

- Java 21 (Amazon Corretto)
- Maven
- Spring Boot 4.1.1
- Docker Desktop + Docker Compose v2
- Docker Hub (`danielpm1912`)
- MongoDB 8 (contenedor auxiliar, sin persistencia de datos aún)

## Parte 1 — Aplicación web

Proyecto Maven (`pom.xml`) con la dependencia `spring-boot-starter-web` y un controlador REST:

[HelloRestController.java](src/main/java/edu/co/escuelaing/containerizing_and_deploying_a_java_web_app/controller/HelloRestController.java)

```java
@RestController
public class HelloRestController {

    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hello, " + name + "!";
    }
}
```

El puerto se toma de la variable de entorno `PORT` (por defecto `8000` si no se define), definido en [ContainerizingAndDeployingAJavaWebAppApplication.java](src/main/java/edu/co/escuelaing/containerizing_and_deploying_a_java_web_app/ContainerizingAndDeployingAJavaWebAppApplication.java):

```java
application.setDefaultProperties(
        Map.of("server.port", System.getenv().getOrDefault("PORT", "8000")));
```

### Construir y ejecutar localmente

```bash
mvn clean package
java -jar target/*.jar
```

### Evidencia

| Prueba | Captura |
|---|---|
| `GET /greeting` (valor por defecto) | ![Hello World](docs/image.png) |
| `GET /greeting?name=Pedro` | ![Hello Pedro](docs/image%20copy.png) |

## Parte 2 — Imagen Docker y contenedores aislados

### Dockerfile

```dockerfile
FROM amazoncorretto:21

WORKDIR /app

COPY target/*.jar app.jar

ENV PORT=8000

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build de la imagen

```bash
docker build -t danielpm1912/virtualization-lab:1.0 .
docker images
```

### Ejecutar un contenedor

```bash
docker run -d \
  --name virtualization-lab-1 \
  -e PORT=6000 \
  -p 34000:6000 \
  danielpm1912/virtualization-lab:1.0
```

### Aislamiento: múltiples instancias del mismo contenedor

```bash
docker run -d --name virtualization-lab-2 -e PORT=8000 -p 34001:8000 danielpm1912/virtualization-lab:1.0
docker run -d --name virtualization-lab-3 -e PORT=8000 -p 34002:8000 danielpm1912/virtualization-lab:1.0
```

Cada contenedor corre de forma aislada, con su propio proceso y espacio de puertos internos, expuesto en un puerto distinto del host (`34000`, `34001`, `34002`).

### Evidencia

| Paso | Captura |
|---|---|
| `docker build` | ![docker build](docs/image%20copy%202.png) |
| `docker images` | ![docker images](docs/image%20copy%203.png) |
| `docker run` contenedor 1 | ![docker run 1](docs/image%20copy%204.png) |
| `docker ps` | ![docker ps](docs/image%20copy%205.png) |
| Contenedor 1 respondiendo | ![Container 1](docs/image%20copy%206.png) |
| `docker run` contenedores 2 y 3 | ![docker run 2 y 3](docs/image%20copy%207.png) |
| Contenedor 2 respondiendo | ![Container 2](docs/image%20copy%208.png) |
| Contenedor 3 respondiendo | ![Container 3](docs/image%20copy%209.png) |

### Problema encontrado y solucionado: mapeo de puertos

Al recrear los contenedores 2 y 3 se publicó el puerto `9000` del contenedor (`-p 3400x:9000`) mientras la variable `PORT` estaba en `8000`, es decir, Spring Boot escuchaba en el `8000` pero Docker redirigía tráfico al `9000`, donde no había nada escuchando. Resultado: los contenedores estaban `Up` pero no respondían.

**Lección:** el puerto del lado derecho de `-p host:contenedor` debe coincidir siempre con el valor real de `PORT` que recibe la aplicación dentro del contenedor.

```bash
docker run -d --name virtualization-lab-2 -e PORT=8000 -p 34001:8000 danielpm1912/virtualization-lab:1.0
docker run -d --name virtualization-lab-3 -e PORT=8000 -p 34002:8000 danielpm1912/virtualization-lab:1.0
```

## Parte 3 — Docker Compose (web + MongoDB)

[compose.yaml](compose.yaml) define dos servicios en la misma red de Docker: `web` (la aplicación) y `db` (MongoDB 8, sin persistencia de datos usada todavía — el volumen existe solo para practicar la gestión de volúmenes de Compose).

```bash
docker compose up -d --build
docker compose ps
docker compose logs web
docker compose logs db
```

### Evidencia

| Paso | Captura |
|---|---|
| `docker compose up -d --build` | ![compose up](docs/image%20copy%2010.png) |
| `docker compose ps` | ![compose ps](docs/image%20copy%2011.png) |
| `docker compose logs web` | ![compose logs web](docs/image%20copy%2012.png) |
| `docker compose logs db` | ![compose logs db](docs/image%20copy%2013.png) |
| Servicio `web` respondiendo en `:8087` | ![Hello Compose](docs/image%20copy%2014.png) |

### Limitación conocida

El contenedor `db` (imagen oficial `mongo:8`) no arranca en este equipo:

```
MongoDB cannot start: Linux kernel versions 6.19 and newer has a known
incompatibility with this version of MongoDB.
```

Es una incompatibilidad entre `mongo:8` y el kernel de Linux del host (CachyOS, kernel reciente), no un error de configuración de Compose. El servicio `web` arranca igualmente porque `depends_on` solo espera a que el contenedor `db` **inicie**, no a que esté saludable. Como la aplicación aún no usa MongoDB para nada, `/greeting` sigue respondiendo con normalidad.

## Pendiente

- **Parte 4:** desplegar la imagen en una instancia EC2 (Amazon Linux 2023), publicar la imagen en Docker Hub y verificar el servicio desde la IP pública.
- Discusión arquitectónica (costo base de EC2, punto en que el costo fijo deja de ser relevante, cuándo escalar a más instancias, servicios adicionales para producción, comparación con serverless).
- Evidencia requerida: diagrama del modelo de despliegue, estimación de AWS Pricing Calculator, tabla de análisis de costos, supuestos y conclusión.

## Cómo correr todo localmente

```bash
# Build de la app
mvn clean package

# Imagen Docker
docker build -t danielpm1912/virtualization-lab:1.0 .

# Un solo contenedor
docker run -d --name virtualization-lab-1 -e PORT=6000 -p 34000:6000 danielpm1912/virtualization-lab:1.0

# Stack completo (web + mongo) con Compose
docker compose up -d --build
```
