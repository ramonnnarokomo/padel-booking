# PadelBook — reservas de pistas de pádel

App para reservar pistas de pádel: parrilla de horarios por pista, reserva con precio en vivo y gestión de "mis reservas". El backend aplica las reglas de un club real: horario, franjas de 30 minutos, hora punta, antelación máxima, límite de reservas por persona y política de cancelación.

- **Backend:** Java 17 + Spring Boot 3.5 (Web, Data JPA, Validation) + H2 + springdoc-openapi (`backend/`)
- **Frontend:** Angular 21 con componentes standalone, signals y CSS propio (`frontend/`)

## Arrancar en local

Requisitos: JDK 17+, Maven y Node.js 20.19+, 22.12+ o 24+.

```bash
# Backend: http://localhost:8080 (Swagger UI en /swagger-ui.html)
cd backend
mvn spring-boot:run

# Frontend (otra terminal): http://localhost:4200
cd frontend
npm install
npm start
```

La primera vez se crea una base de datos H2 en `backend/data/` con 4 pistas y algunas reservas de ejemplo. En desarrollo, Angular redirige `/api` al backend mediante `proxy.conf.json`.

## Tests

```bash
cd backend && mvn test                       # reglas de negocio + tests de la API con MockMvc
cd frontend && npm run test:ci               # Vitest
```

## Reglas de negocio

- El club abre de **09:00 a 23:00**. Las reservas empiezan en punto o y media y duran **60, 90 o 120 minutos**.
- Se puede reservar con **14 días de antelación** como máximo y nunca en el pasado.
- **Hora punta:** de lunes a viernes a partir de las 18:00 el precio sube un **25 %**.
- Cada email puede tener como máximo **2 reservas activas**.
- Solo se puede cancelar hasta **24 horas antes**.
- Dos reservas de la misma pista no pueden solaparse. Si llegan a la vez dos reservas de la misma pista, la segunda espera gracias a un bloqueo pesimista sobre la pista (`SELECT … FOR UPDATE`), así que no se cuelan reservas duplicadas.

Las reglas viven en una clase Java pura (`BookingRules`) sin dependencias de Spring, lo que permite testearlas sin levantar el contexto. La hora actual se inyecta con un `Clock`, de modo que los tests usan una fecha fija.

## API

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/courts` | Pistas con su precio por hora |
| GET | `/api/schedule?date=2026-10-05` | Franjas ocupadas de cada pista ese día |
| GET | `/api/quote?courtId=&start=&durationMinutes=` | Precio de una reserva (y si es hora punta) |
| POST | `/api/bookings` | Crear reserva |
| GET | `/api/bookings?email=` | Próximas reservas de un email |
| GET | `/api/bookings/{id}?email=` | Detalle de una reserva (también si está cancelada) |
| DELETE | `/api/bookings/{id}?email=` | Cancelar reserva |

Los errores siguen el estándar **ProblemDetail (RFC 9457)**: `400` si los datos no son válidos (con un mapa `errors` por campo), `404` si no existe, `409` si la franja ya está ocupada y `422` si se incumple una regla del club.

## Estructura

```
backend/src/main/java/com/ramonnnarokomo/padel
├── domain/       Entidades JPA y reglas del club (BookingRules)
├── repository/   Repositorios Spring Data
├── service/      Casos de uso: reservar, cancelar, horario, presupuesto
├── web/          Controladores REST y manejo de errores
├── dto/          Records de entrada y salida
└── config/       Clock, CORS, OpenAPI y datos de ejemplo
frontend/src/app
├── booking/      Pantalla "Reservar": parrilla y panel de reserva
├── my-bookings/  Pantalla "Mis reservas"
├── core/         Modelos de la API, servicio HTTP, errores y toasts
└── shared/       Formato de precios y fechas
```
