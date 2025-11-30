# 🚀 Быстрый старт

## 📋 Что нужно установить

### 1. **Docker Desktop**
```bash
# Windows: скачать с https://www.docker.com/products/docker-desktop
# Linux: установить docker и docker-compose
```

### 2. **Minikube**
```bash
# Windows (PowerShell от администратора)
choco install minikube

# Linux
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

### 3. **kubectl**
```bash
# Windows
choco install kubernetes-cli

# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

### 4. **Helm**
```bash
# Windows
choco install kubernetes-helm

# Linux
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

---

## 🎬 Первоначальная настройка

### Шаг 1: Запустить Minikube

```bash
minikube start --cpus=4 --memory=8192 --disk-size=20g --driver=docker

# Включить metrics-server для HPA
minikube addons enable metrics-server

# Проверить статус
minikube status
```

Должно показать:
```
host: Running
kubelet: Running
apiserver: Running
```

### Шаг 2: Настроить Docker для Minikube

**⚠️ ВАЖНО!** Перед КАЖДОЙ сборкой образов:

```bash
eval $(minikube docker-env --shell bash)
```

Это переключает Docker CLI на Docker daemon внутри Minikube.

**Проверка:** `docker images` покажет образы Minikube, а не локальные.

---

## 📦 Установка системы

### Автоматическая установка (рекомендуется)

```bash
cd helm
./install.sh
```

Скрипт автоматически:
- ✅ Создаст namespace `shop-system`
- ✅ Установит PostgreSQL (4 отдельные БД)
- ✅ Установит Redis
- ✅ Установит Kafka (KRaft mode)
- ✅ Соберёт Docker образы всех 4 микросервисов
- ✅ Установит все микросервисы через Helm
- ✅ Установит Observability Stack (Prometheus, Grafana, Jaeger, Graylog)
- ✅ Установит KrakenD API Gateway

**Время установки:** ~5-10 минут

---

## 🔍 Проверка установки

```bash
kubectl get pods -n shop-system
```

**Ожидаемый результат:**
```
NAME                                READY   STATUS    RESTARTS   AGE
auth-db-postgresql-0                2/2     Running   0          5m
product-db-postgresql-0             2/2     Running   0          5m
customer-db-postgresql-0            2/2     Running   0          5m
order-db-postgresql-0               2/2     Running   0          5m
redis-redis-master-0                1/1     Running   0          4m
kafka-kafka-0                       1/1     Running   0          4m
zookeeper-zookeeper-0               1/1     Running   0          4m
auth-service-xxxxx                  1/1     Running   0          3m
product-service-xxxxx               1/1     Running   0          3m
customer-service-xxxxx              1/1     Running   0          3m
order-service-xxxxx                 1/1     Running   0          3m
krakend-xxxxx                       1/1     Running   0          2m
prometheus-0                        1/1     Running   0          2m
grafana-xxxxx                       1/1     Running   0          2m
jaeger-xxxxx                        1/1     Running   0          2m
```

Все поды должны быть **Running** и **READY**.

---

## 🌐 Доступ к сервисам

### Автоматический Port-Forwarding

```bash
./port-forward.sh
```

Скрипт пробросит порты для всех сервисов:
- ✅ KrakenD API Gateway: `http://localhost:8080`
- ✅ Auth Service (OAuth2): `http://localhost:9000`
- ✅ Grafana: `http://localhost:3000`
- ✅ Jaeger UI: `http://localhost:16686`
- ✅ Prometheus: `http://localhost:9090`

**Оставь этот терминал открытым!**

### Ручной Port-Forwarding

Если нужен контроль, открой отдельные терминалы:

```bash
# KrakenD API Gateway
kubectl port-forward -n shop-system svc/krakend 8080:8080

# Auth Service (OAuth2)
kubectl port-forward -n shop-system svc/auth-service 9000:9000

# Grafana
kubectl port-forward -n shop-system svc/grafana 3000:3000

# Jaeger
kubectl port-forward -n shop-system svc/jaeger-ui 16686:16686

# Prometheus
kubectl port-forward -n shop-system svc/prometheus 9090:9090
```

---

## 🧪 Тестирование API

### Автоматический тест (рекомендуется)

```bash
./test-api.sh
```

Скрипт выполняет полный цикл:
1. ✅ Создание пользователя
2. ✅ Получение OAuth2 access token
3. ✅ Создание продукта
4. ✅ Получение списка продуктов
5. ✅ Создание клиента
6. ✅ Создание заказа (с Kafka валидацией)

### Ручное тестирование

#### 1. Создать пользователя
```bash
curl -X POST http://localhost:8080/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!",
    "email": "test@example.com"
  }'
```

#### 2. Получить OAuth2 токен
```bash
curl -X POST http://localhost:9000/oauth2/token \
  -u "shop-client:shop-secret" \
  -d "grant_type=client_credentials&scope=read write"
```

**Ответ:**
```json
{
  "access_token": "eyJra...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

Сохрани токен:
```bash
export TOKEN="eyJra..."
```

#### 3. Создать продукт
```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop Dell XPS 15",
    "price": 1499.99,
    "quantity": 10
  }'
```

#### 4. Получить список продуктов
```bash
curl http://localhost:8080/products \
  -H "Authorization: Bearer $TOKEN"
```

#### 5. Создать клиента
```bash
curl -X POST http://localhost:8080/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "name": "John Doe",
    "phone": "+1234567890",
    "address": "123 Main St, City"
  }'
```

#### 6. Создать заказ
```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "productId": 1,
    "quantity": 2
  }'
```

---

## 📊 Observability

### Grafana (метрики и дашборды)

```
URL: http://localhost:3000
Логин: admin
Пароль: admin
```

**Что доступно:**
- 📈 PostgreSQL Database Overview (автоматический дашборд)
- 📉 Метрики из Prometheus
- 🔍 Connections, Database Size, TPS, Commits/Rollbacks

### Jaeger (distributed tracing)

```
URL: http://localhost:16686
```

**Что видно:**
- 🔗 Полная трассировка запросов через KrakenD → микросервисы
- ⏱️ Время выполнения каждого span
- 📨 Kafka события (validate-product, validate-customer)
- ❌ Ошибки и stack traces

**Как посмотреть трейс:**
1. Открой Jaeger UI
2. Выбери Service: `krakend` или `order-service`
3. Нажми "Find Traces"
4. Кликни на любой трейс → увидишь полный путь запроса

### Prometheus (метрики)

```
URL: http://localhost:9090
```

**Источники метрик:**
- 🔵 Spring Boot Actuator (`/actuator/prometheus`)
- 🐘 PostgreSQL Exporter
- 🌐 KrakenD (`:8090/metrics`)

**Примеры запросов:**
```promql
# CPU usage микросервисов
process_cpu_usage{job="kubernetes-pods"}

# HTTP requests per second
rate(http_server_requests_seconds_count[1m])

# PostgreSQL connections
pg_stat_database_numbackends
```

### Graylog (централизованные логи)

**⚠️ Требует внешний Graylog:**
```bash
docker run -d --name graylog \
  -p 9000:9000 -p 12201:12201/udp \
  -e GRAYLOG_ROOT_PASSWORD_SHA2=... \
  graylog/graylog:5.0
```

Все микросервисы отправляют логи в Graylog через GELF UDP (порт 12201).

---

## 🔧 Управление системой

### Посмотреть логи сервиса

```bash
# Логи конкретного сервиса
kubectl logs -n shop-system -l app=product-service --tail=100 -f

# Логи PostgreSQL
kubectl logs -n shop-system auth-db-postgresql-0 -c postgresql --tail=50

# Логи Kafka
kubectl logs -n shop-system kafka-kafka-0 --tail=50
```

### Проверить HPA (автомасштабирование)

```bash
kubectl get hpa -n shop-system
```

### Перезапустить сервис

```bash
kubectl rollout restart deployment/product-service -n shop-system
kubectl rollout status deployment/product-service -n shop-system
```

### Пересборка после изменений в коде

```bash
# 1. Переключиться на Docker Minikube
eval $(minikube docker-env --shell bash)

# 2. Пересобрать образ
docker build -t shop-product-service:latest \
  --build-arg SERVICE_NAME=service/product-service \
  -f Dockerfile .

# 3. Перезапустить deployment
kubectl rollout restart deployment/product-service -n shop-system

# 4. Дождаться готовности
kubectl rollout status deployment/product-service -n shop-system
```

---

## 🗑️ Удаление системы

### Полное удаление

```bash
cd helm
./uninstall.sh
```

Или вручную:
```bash
# Удалить все Helm релизы
helm uninstall -n shop-system \
  krakend grafana prometheus jaeger graylog \
  order-service customer-service product-service auth-service \
  kafka redis postgresql

# Удалить PersistentVolumeClaims
kubectl delete pvc --all -n shop-system

# Удалить namespace
kubectl delete namespace shop-system
```

---

## 🐛 Частые проблемы и решения

### 1. "ImagePullBackOff" или "ErrImageNeverPull"

**Причина:** Docker образ не найден в Minikube.

**Решение:**
```bash
# Убедись что Docker CLI указывает на Minikube
eval $(minikube docker-env --shell bash)

# Проверь что образ существует
docker images | grep shop-

# Если нет - пересобери
docker build -t shop-product-service:latest \
  --build-arg SERVICE_NAME=service/product-service \
  -f Dockerfile .
```

### 2. Pod в статусе "Pending"

**Причина:** Не хватает ресурсов CPU/Memory.

**Решение:**
```bash
# Удалить кластер и пересоздать с большими ресурсами
minikube delete
minikube start --cpus=6 --memory=10240 --disk-size=30g
```

### 3. "Connection refused" к PostgreSQL/Kafka/Redis

**Причина:** Сервисы ещё не готовы.

**Решение:** Подожди 2-3 минуты и проверь:
```bash
kubectl get pods -n shop-system -w
```

### 4. HPA показывает "<unknown>/60%"

**Причина:** metrics-server не установлен.

**Решение:**
```bash
minikube addons enable metrics-server
```

### 5. OAuth2 token request fails

**Причина:** Auth service недоступен или Redis не работает.

**Решение:**
```bash
# Проверь логи
kubectl logs -n shop-system -l app=auth-service --tail=100

# Проверь Redis
kubectl logs -n shop-system redis-redis-master-0 --tail=50

# Проверь port-forwarding
curl -v http://localhost:9000/actuator/health
```

### 6. Port-forwarding обрывается

**Причина:** Kubernetes может прерывать соединение.

**Решение:** Перезапусти `./port-forward.sh`

---

## 🔄 Работа с Minikube (WSL)

### Проблема: NodePort недоступен из WSL

**Причина:** Minikube работает в Docker Desktop на Windows.

**Решение:** Используй `./port-forward.sh` - он проксирует всё на localhost.

### Проблема: kubeconfig не работает после перезапуска

**Причина:** Minikube меняет порт API server.

**Решение:** Обнови kubeconfig:
```bash
# WSL
cp /mnt/c/Users/$USER/.kube/config ~/.kube/config
sed -i 's|C:\\Users\\|/mnt/c/Users/|g' ~/.kube/config
sed -i 's|\\|/|g' ~/.kube/config
```

Или создай alias в `~/.bashrc`:
```bash
alias minikube-fix='cp /mnt/c/Users/$USER/.kube/config ~/.kube/config && sed -i "s|C:\\\\Users\\\\|/mnt/c/Users/|g" ~/.kube/config && sed -i "s|\\\\|/|g" ~/.kube/config'
```

---

## 📝 Сохранение данных

### После `minikube stop`

**Данные СОХРАНЯЮТСЯ:**
- ✅ PostgreSQL данные (PersistentVolumes)
- ✅ Redis данные
- ✅ Grafana дашборды
- ✅ Prometheus метрики (за период retention)

**Как запустить снова:**
```bash
minikube start --cpus=4 --memory=8192

# WSL: обнови kubeconfig
minikube-fix

# Проверь что всё запустилось
kubectl get pods -n shop-system

# Запусти port-forwarding
./port-forward.sh
```

### После `minikube delete`

**Данные УДАЛЯЮТСЯ:**
- ❌ Все PersistentVolumes
- ❌ Все базы данных
- ❌ Grafana конфигурация
- ❌ Prometheus метрики

**Восстановление:**
```bash
minikube start --cpus=4 --memory=8192
minikube addons enable metrics-server

eval $(minikube docker-env --shell bash)

cd helm
./install.sh
```

---

## ✅ Чеклист для первого запуска

- [ ] Docker Desktop запущен
- [ ] Minikube установлен
- [ ] kubectl установлен
- [ ] Helm установлен
- [ ] `minikube start --cpus=4 --memory=8192`
- [ ] `minikube addons enable metrics-server`
- [ ] `eval $(minikube docker-env --shell bash)` (перед сборкой)
- [ ] `cd helm && ./install.sh`
- [ ] Все поды Running: `kubectl get pods -n shop-system`
- [ ] `./port-forward.sh` (оставить открытым)
- [ ] `./test-api.sh` (протестировать API)
- [ ] Открыть Grafana: http://localhost:3000
- [ ] Открыть Jaeger: http://localhost:16686

**🎉 Готово! Система работает!**

---

## 📚 Полезные команды

```bash
# Статус Minikube
minikube status

# IP адрес Minikube (если нужен)
minikube ip

# Kubernetes Dashboard
minikube dashboard

# SSH в Minikube node
minikube ssh

# Логи Minikube
minikube logs

# Остановить Minikube
minikube stop

# Удалить Minikube
minikube delete

# Версии
minikube version
kubectl version --client
helm version
```

---

## 🏗️ Архитектура системы

```
┌─────────────────────────────────────────────────────────────┐
│                     KrakenD API Gateway                     │
│                    http://localhost:8080                    │
└────┬─────────────┬─────────────┬─────────────┬──────────────┘
     │             │             │             │
     ▼             ▼             ▼             ▼
┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐
│  Auth   │  │ Product │  │ Customer │  │  Order   │
│ Service │  │ Service │  │  Service │  │  Service │
│  :9000  │  │  :8081  │  │   :8082  │  │   :8083  │
└────┬────┘  └────┬────┘  └─────┬────┘  └─────┬────┘
     │            │              │              │
     ▼            ▼              ▼              ▼
┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐
│ auth_db │  │product  │  │customer  │  │ order_db │
│PostgreSQL  │  _db    │  │   _db    │  │PostgreSQL
└─────────┘  └─────────┘  └──────────┘  └──────────┘

     │
     ▼
┌─────────┐       ┌──────────────────────────────┐
│  Redis  │       │      Apache Kafka            │
│ Session │       │  • order-created             │
└─────────┘       │  • validate-product-request  │
                  │  • validate-customer-request │
                  └──────────────────────────────┘

         Observability Stack
┌──────────────────────────────────────────┐
│ Prometheus → Grafana → Jaeger → Graylog │
└──────────────────────────────────────────┘
```

---

## 🔐 Credentials

**Grafana:**
- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

**PostgreSQL (все БД):**
- Username: `postgres`
- Password: `postgres`

**OAuth2 Client:**
- Client ID: `shop-client`
- Client Secret: `shop-secret`

**Redis:**
- No password

---

## 🎯 Endpoints

**Auth Service:**
- `POST /users` - Регистрация пользователя (public)
- `GET /users` - Список пользователей (protected)
- `POST /oauth2/token` - Получение JWT токена

**Product Service:**
- `GET /products` - Список товаров
- `POST /products` - Создать товар
- `GET /products/{id}` - Получить товар
- `PUT /products/{id}` - Обновить товар
- `DELETE /products/{id}` - Удалить товар

**Customer Service:**
- `GET /customers` - Список клиентов
- `POST /customers` - Создать клиента
- `GET /customers/{id}` - Получить клиента

**Order Service:**
- `GET /orders` - Список заказов
- `POST /orders` - Создать заказ (с Kafka валидацией)
- `GET /orders/{id}` - Получить заказ

Все endpoints (кроме `POST /users`) требуют `Authorization: Bearer <token>`.

---

**Вопросы? Проблемы?** Проверь логи и статус подов:
```bash
kubectl get pods -n shop-system
kubectl describe pod <pod-name> -n shop-system
kubectl logs <pod-name> -n shop-system --tail=100
```
