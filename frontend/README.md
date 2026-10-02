# PadelBook · frontend

Frontend en Angular 22 de una app para reservar pistas de pádel. Consume la API REST del
backend Spring Boot de este mismo repositorio (`../backend`).

- **Reservar**: parrilla de horarios por pista (franjas de 30 min, 09:00–23:00), navegación
  por días (hoy + 14), panel de reserva con duración (60/90/120 min), precio en vivo y aviso de
  hora punta.
- **Mis reservas**: búsqueda por email, listado de próximas reservas y cancelación (hasta 24 h
  antes).

Componentes standalone, signals (`signal`, `computed`, `linkedSignal`, `input`, `output`),
`rxResource` para cargar datos, Signal Forms para los formularios y CSS propio sin librerías.

## Requisitos

- Node.js 22.22.3+ o 24.15+ (lo que pide Angular CLI 22)
- El backend arrancado en `http://localhost:8080`

## Arrancar en local

```bash
npm install
npm start
```

Abre http://localhost:4200. En desarrollo, `ng serve` redirige `/api/*` al backend en el puerto
8080 mediante [`proxy.conf.json`](proxy.conf.json), así que no hace falta configurar CORS. Si
el backend no está arrancado, la app lo indica con un mensaje de error.

## Scripts

| Comando           | Qué hace                                  |
| ----------------- | ----------------------------------------- |
| `npm start`       | Servidor de desarrollo con proxy a la API |
| `npm run build`   | Build de producción en `dist/frontend`    |
| `npm test`        | Tests unitarios (Vitest) en modo watch    |
| `npm run test:ci` | Tests unitarios una sola vez              |

## Estructura

```
src/app/
├── core/          # modelos de la API, PadelApi (HTTP), mapeo de errores, toasts, localStorage
├── shared/        # formato de precios/fechas, pipes y alerta de errores
├── booking/       # pantalla "Reservar": parrilla, panel de reserva y reglas de disponibilidad
└── my-bookings/   # pantalla "Mis reservas"
```

La lógica de qué duraciones se pueden reservar vive en funciones puras
(`booking/availability.ts`) con sus tests; el backend vuelve a validar todas las reglas.
