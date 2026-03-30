1. # 🔐 Password Manager - Java + MySQL

   Gestor de contraseñas seguro desarrollado con Java Servlets, MySQL y Bootstrap.

   ## 🚀 Funcionalidades

   - ✅ Registro y autenticación de usuarios
   - ✅ CRUD completo de contraseñas (Crear, Leer, Actualizar, Eliminar)
   - ✅ Búsqueda y filtrado por categorías
   - ✅ Revelar contraseñas guardadas
   - ✅ Interfaz responsive (Bootstrap 5)
   - ✅ Base de datos MySQL con persistencia

   ## 🛠️ Tecnologías

   | Capa            | Tecnología                       |
   |-----------------|----------------------------------|
   | Backend         | Java 17 + Servlets (Jakarta EE)  |
   | Frontend        | HTML5 + JavaScript + Bootstrap 5 |
   | Base de datos   | MySQL 8.0                        |
   | Servidor        | Apache Tomcat 10.1               |
   | Contenerización | Docker + Docker Compose          |

   ## 📦 Instalación y Ejecución

   ### Requisitos previos
   - Docker Desktop instalado
   - XAMPP con MySQL corriendo

   ### Pasos
   ```powershell
   # 1. Clonar el repositorio
   git clone <tu-repositorio>
   cd password-manager-java

   # 2. Iniciar MySQL en XAMPP

   # 3. Construir y ejecutar con Docker
   docker-compose -f docker-compose.local.yml up --build

   # 4. Acceder a la aplicación
   http://localhost:8080

2. Iniciar MySQL en XAMPP:
   -Abre XAMPP Control Panel
   -Click en "Start" en MySQL
   -Espera a que se ponga en verde

3. Crear la base de datos (si no existe):
   -- Accede a phpMyAdmin: http://localhost/phpmyadmin
   -- Ejecuta el script SQL de creación (si tienes uno)

4. Construir y ejecutar con Docker:
   docker-compose -f docker-compose.local.yml up --build

5. Acceder a la aplicación:
   Abre tu navegador en: http://localhost:8080
   Regístrate o inicia sesión

6. Detener la aplicación
   docker-compose -f docker-compose.local.yml down

7. 📁 Estructura del Proyecto
    password-manager-java/
   │
   ├── src/
   │   ├── main/
   │   │   ├── java/com/passwordmanager/
   │   │   │   ├── dao/              # Acceso a datos (DAO)
   │   │   │   │   ├── UserDAO.java
   │   │   │   │   └── PasswordDAO.java
   │   │   │   ├── model/            # Modelos de datos
   │   │   │   │   ├── User.java
   │   │   │   │   └── Password.java
   │   │   │   ├── servlet/          # Servlets (API REST)
   │   │   │   │   ├── AuthServlet.java
   │   │   │   │   └── PasswordServlet.java
   │   │   │   ├── util/             # Utilidades
   │   │   │   │   ├── JsonUtil.java
   │   │   │   │   └── AuthUtil.java
   │   │   │   └── db/               # Conexión a BD
   │   │   │       └── DatabasePool.java
   │   │   │
   │   │   └── webapp/               # Frontend
   │   │       ├── index.html
   │   │       ├── login.html
   │   │       ├── register.html
   │   │       ├── dashboard.html
   │   │       ├── css/
   │   │       │   └── styles.css
   │   │       └── js/
   │   │           ├── api.js
   │   │           └── auth.guard.js
   │   │
   │   └── test/                     # Pruebas unitarias
   │
   ├── docker-compose.local.yml      # Configuración Docker
   ├── Dockerfile                    # Imagen Docker
   ├── pom.xml                       # Configuración Maven
   └── README.md                     # Este archivo

8. 🔒 Seguridad
   Implementado
   ✅ Autenticación con Basic Auth (Base64)
   ✅ Validación de datos en frontend y backend
   ✅ Sesiones protegidas por endpoint
   ✅ Filtrado de datos sensibles

9. Mejoras para Producción
   🔐 Implementar hash BCrypt para contraseñas de usuarios
   🔐 Encriptación AES para contraseñas guardadas
   🔐 HTTPS para tráfico seguro
   🔐 Validación de CSRF tokens
   🔐 Rate limiting en endpoints de autenticación

10. 🧪 Uso
   1. Registro de Usuario
   Ve a http://localhost:8080/register.html
   Completa el formulario con:
   Nombre de usuario
   Correo electrónico
   Contraseña (mínimo 8 caracteres)
   Confirmar contraseña
   Click en "Registrarse"

   2. Iniciar Sesión
   Ve a http://localhost:8080/login.html
   Ingresa tu email y contraseña
   Click en "Iniciar sesión"

   3. Crear Credencial
   En el dashboard, click en "+ Nueva credencial"
   Completa:
   Nombre del sitio (obligatorio)
   URL del sitio
   Usuario/Email
   Contraseña (obligatoria)
   Categoría
   Notas
   Click en "Guardar"

   4. Buscar/Filtrar
   Usa el buscador para encontrar por nombre de sitio
   Usa el filtro para ver por categoría

   5. Revelar Contraseña
   Click en el ícono del ojo 👁️
   La contraseña se mostrará en un modal
   Click para copiar al portapapeles

   6. Editar/Eliminar
   Click en el lápiz ✏️ para editar
   Click en la papelera 🗑️ para eliminar

11. 🐛 Solución de Problemas
   1. Error: "Connection refused"
   Verifica que MySQL esté corriendo en XAMPP
   Verifica que el puerto 3306 esté disponible

   2. Error: "Puerto 8080 ocupado"
   # Detén otros servicios que usen el puerto 8080
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F

   3. Docker no inicia
   # Limpia caché de Docker
   docker builder prune -af

   # Reconstruye
   docker-compose -f docker-compose.local.yml up --build

12. Endpoints API REST
   | Método   | Endpoint                    | Descripción               |
   |----------|-----------------------------|---------------------------|
   | POST     | /api/auth/register          | Registrar nuevo usuario   |
   | POST     | /api/auth/login             | Iniciar sesión            |
   | GET      | /api/auth/me                | Obtener usuario actual    |
   | GET      | /api/passwords              | Listar contraseñas        |
   | GET      | /api/passwords/{id}         | Obtener contraseña por ID |
   | GET      | /api/passwords/{id}/reveal  | Revelar contraseña        |
   | POST     | /api/passwords              | Crear contraseña          |
   | PUT      | /api/passwords/{id}         | Actualizar contraseña     |
   | DELET    | /api/passwords/{id}         | Eliminar contraseña       |

👨‍ Autora
Karen Zapata Castañeda
Curso: Lenguaje de programación JavaScript
Profesor: Yeison Stiven Betancur Rojas
Institución: [Nombre de la Universidad/Instituto]
Fecha de entrega: 29 de Marzo 2026
Email: zapatacastanedakaren@gmail.com

📄 Licencia
Este proyecto es desarrollado con fines académicos y educativos.

© 2026 Password Manager - Proyecto Académico