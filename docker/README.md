# Docker

Desde la raíz del repositorio, construye y arranca la API:

```bash
docker compose -f docker/compose.yml up --build -d
```

La API quedará disponible en `http://localhost:8080`. El mapeo `8080:8080`
publica el puerto del contenedor en el host.

Para comprobar el contenedor:

```bash
docker compose -f docker/compose.yml ps
docker compose -f docker/compose.yml logs -f
```

Para detenerlo:

```bash
docker compose -f docker/compose.yml down
```

También se puede construir sin Compose:

```bash
docker build -f docker/Dockerfile -t pokebattle-api:latest .
docker run --rm -p 8080:8080 --name pokebattle-api pokebattle-api:latest
```
