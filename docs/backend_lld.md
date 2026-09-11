# Fairshare Backend — Low-Level Design (LLD)

---

## 1. Maven Project Configuration (`pom.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.3</version>
        <relativePath/>
    </parent>

    <groupId>com.fairshare</groupId>
    <artifactId>fairshare-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>fairshare-backend</name>
    <description>Fairshare Modular Monolith Backend API</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <springdoc.version>2.6.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Database & Migrations -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- JWT (jjwt) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- OpenAPI / Swagger -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 2. PostgreSQL DDL Schema (`V1__initial_schema.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    initials VARCHAR(10),
    avatar_url VARCHAR(512),
    color VARCHAR(32) DEFAULT '#6366F1',
    phone VARCHAR(32) UNIQUE,
    default_currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    onboarding_step VARCHAR(32) NOT NULL DEFAULT 'welcome',
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

-- 2. FEDERATED IDENTITIES (MULTI-OAUTH)
CREATE TABLE user_identities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_user_identities_user ON user_identities(user_id);

-- 3. CONTACTS (MSISDN PHONE-BASED)
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_owner_msisdn UNIQUE (owner_id, msisdn)
);
CREATE INDEX idx_contacts_owner ON contacts(owner_id);
CREATE INDEX idx_contacts_msisdn ON contacts(msisdn);

-- 4. GROUPS
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(20) NOT NULL DEFAULT 'other',
    emoji VARCHAR(16) DEFAULT '💰',
    simplify_debts BOOLEAN NOT NULL DEFAULT true,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

-- 5. GROUP MEMBERS
CREATE TABLE group_members (
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX idx_group_members_user ON group_members(user_id);

-- 6. EXPENSES
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    notes TEXT,
    category VARCHAR(64) NOT NULL DEFAULT 'General',
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_expense_idempotency UNIQUE (group_id, idempotency_key)
);
CREATE INDEX idx_expenses_group ON expenses(group_id, occurred_at DESC) WHERE deleted_at IS NULL;

-- 7. EXPENSE ALLOCATIONS
CREATE TABLE expense_allocations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(10) NOT NULL CHECK (type IN ('PAYER', 'SHARER')),
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    CONSTRAINT uk_allocation UNIQUE (expense_id, user_id, type)
);
CREATE INDEX idx_allocations_user ON expense_allocations(user_id);
CREATE INDEX idx_allocations_expense ON expense_allocations(expense_id);

-- 8. PAYMENTS
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    note VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_different_users CHECK (from_user_id <> to_user_id)
);
CREATE INDEX idx_payments_group ON payments(group_id, occurred_at DESC);

-- 9. UPCOMING BILLS
CREATE TABLE upcoming_bills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    due_date DATE NOT NULL,
    recurrence VARCHAR(20) NOT NULL DEFAULT 'once',
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_bills_due ON upcoming_bills(due_date, status) WHERE status = 'pending';

-- 10. BILL ASSIGNEES
CREATE TABLE bill_assignees (
    bill_id UUID NOT NULL REFERENCES upcoming_bills(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (bill_id, user_id)
);

-- 11. MONEY REQUESTS
CREATE TABLE money_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID REFERENCES groups(id) ON DELETE SET NULL,
    from_user_id UUID NOT NULL REFERENCES users(id),
    to_user_id UUID NOT NULL REFERENCES users(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    description VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_request_users CHECK (from_user_id <> to_user_id)
);
CREATE INDEX idx_requests_to ON money_requests(to_user_id, status);
CREATE INDEX idx_requests_expiry ON money_requests(expires_at) WHERE status = 'open';

-- 12. ACTIVITY EVENTS (AUDIT TRAIL)
CREATE TABLE activity_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID REFERENCES groups(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32),
    target_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_activity_group ON activity_events(group_id, created_at DESC);
CREATE INDEX idx_activity_actor ON activity_events(actor_id, created_at DESC);
```

---

## 3. Financial Invariant & Algorithms

### 3.1 SplitCalculator Engine
Integer-only minor unit arithmetic matching the client `domain/money.ts`:

```java
package com.fairshare.expense.engine;

import com.fairshare.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SplitCalculator {

    public record AllocationResult(UUID userId, long amountMinor) {}
    public record WeightedUser(UUID userId, double weight) {}

    public List<AllocationResult> allocateByWeights(long total, List<WeightedUser> weighted) {
        if (total < 0) throw new BadRequestException("Total must be non-negative");
        if (weighted.isEmpty()) throw new BadRequestException("At least one participant required");

        double weightSum = weighted.stream().mapToDouble(WeightedUser::weight).sum();
        if (weightSum <= 0) throw new BadRequestException("Weights must sum to greater than zero");

        record Entry(UUID userId, long floor, double remainder, int index) {}
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weighted.size(); i++) {
            double exact = (total * weighted.get(i).weight()) / weightSum;
            long floor = (long) Math.floor(exact);
            entries.add(new Entry(weighted.get(i).userId(), floor, exact - floor, i));
        }

        long flooredSum = entries.stream().mapToLong(Entry::floor).sum();
        long remainderUnits = total - flooredSum;

        List<Entry> sorted = entries.stream()
            .sorted(Comparator.comparingDouble(Entry::remainder).reversed()
                .thenComparingInt(Entry::index))
            .toList();

        long[] finalAmounts = new long[entries.size()];
        for (Entry e : entries) finalAmounts[e.index()] = e.floor();
        for (int i = 0; i < remainderUnits; i++) {
            finalAmounts[sorted.get(i % sorted.size()).index()] += 1;
        }

        List<AllocationResult> results = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            results.add(new AllocationResult(weighted.get(i).userId(), finalAmounts[i]));
        }
        return results;
    }

    public List<AllocationResult> splitEqually(long total, List<UUID> userIds) {
        return allocateByWeights(total, userIds.stream().map(id -> new WeightedUser(id, 1.0)).toList());
    }

    public void validateExpenseInvariant(long totalMinor, List<AllocationResult> payers, List<AllocationResult> shares) {
        long payerSum = payers.stream().mapToLong(AllocationResult::amountMinor).sum();
        long sharerSum = shares.stream().mapToLong(AllocationResult::amountMinor).sum();

        if (payerSum != totalMinor || sharerSum != totalMinor) {
            throw new BadRequestException("Financial invariant violated: total=" + totalMinor 
                + ", payers=" + payerSum + ", shares=" + sharerSum);
        }
    }
}
```

### 3.2 DebtSimplifier Engine ($O(N \log N)$ Min-Cash-Flow)

```java
package com.fairshare.expense.engine;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DebtSimplifier {

    public record DebtResult(UUID fromUserId, UUID toUserId, long amountMinor) {}

    public List<DebtResult> simplify(Map<UUID, Long> netBalances) {
        long netSum = netBalances.values().stream().mapToLong(Long::longValue).sum();
        if (netSum != 0) {
            throw new IllegalStateException("Ledger drift detected: net balance sum is " + netSum);
        }

        record Account(UUID userId, long amount) {}

        List<Account> debtors = netBalances.entrySet().stream()
            .filter(e -> e.getValue() < 0)
            .map(e -> new Account(e.getKey(), -e.getValue()))
            .sorted(Comparator.comparingLong(Account::amount).reversed())
            .collect(Collectors.toCollection(ArrayList::new));

        List<Account> creditors = netBalances.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> new Account(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingLong(Account::amount).reversed())
            .collect(Collectors.toCollection(ArrayList::new));

        List<DebtResult> results = new ArrayList<>();
        int di = 0, ci = 0;

        while (di < debtors.size() && ci < creditors.size()) {
            Account debtor = debtors.get(di);
            Account creditor = creditors.get(ci);
            long settle = Math.min(debtor.amount(), creditor.amount());

            if (settle > 0) {
                results.add(new DebtResult(debtor.userId(), creditor.userId(), settle));
            }

            debtors.set(di, new Account(debtor.userId(), debtor.amount() - settle));
            creditors.set(ci, new Account(creditor.userId(), creditor.amount() - settle));

            if (debtors.get(di).amount() == 0) di++;
            if (creditors.get(ci).amount() == 0) ci++;
        }

        return results;
    }
}
```

---

## 4. REST Controller Signatures & Endpoints

| Module | Method | Path | Request Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/v1/auth/register` | `RegisterRequest` | Local user signup with BCrypt hash |
| **Auth** | `POST` | `/api/v1/auth/login` | `LoginRequest` | Returns JWT access token & Redis refresh token |
| **Auth** | `GET` | `/api/v1/auth/oauth/google` | — | Initiates Google OAuth2 authorization code grant |
| **Auth** | `GET` | `/api/v1/auth/oauth/google/callback` | Query params | Exchanges code, links identity, returns JWT |
| **Auth** | `POST` | `/api/v1/auth/refresh` | `RefreshRequest` | Rotates refresh token in Redis |
| **Auth** | `POST` | `/api/v1/auth/logout` | `RefreshRequest` | Revokes refresh token in Redis |
| **Users** | `GET` | `/api/v1/users/me` | — | Current profile & preferences |
| **Users** | `PUT` | `/api/v1/users/me` | `UpdateProfileRequest` | Updates profile name, avatar, phone |
| **Users** | `PUT` | `/api/v1/users/me/onboarding` | `OnboardingRequest` | Steps: welcome → profile → contacts → group → done |
| **Contacts** | `GET` | `/api/v1/users/me/contacts` | — | List phone-number contacts |
| **Contacts** | `POST` | `/api/v1/users/me/contacts` | `AddContactRequest` | Add contact by MSISDN, auto-resolves existing user |
| **Contacts** | `DELETE`| `/api/v1/users/me/contacts/{id}`| — | Removes contact link |
| **Groups** | `GET` | `/api/v1/groups` | — | Current user's groups with summary balances |
| **Groups** | `POST` | `/api/v1/groups` | `CreateGroupRequest` | Create group, adds creator as admin |
| **Groups** | `GET` | `/api/v1/groups/{id}/balances` | — | Raw member balances + simplified debt routes |
| **Expenses** | `GET` | `/api/v1/groups/{id}/expenses` | — | Chronological expense ledger |
| **Expenses** | `POST` | `/api/v1/groups/{id}/expenses` | `CreateExpenseRequest` | Idempotent expense creation with split allocations |
| **Expenses** | `PUT` | `/api/v1/expenses/{id}` | `UpdateExpenseRequest` | Edits expense and updates balance graph |
| **Expenses** | `DELETE`| `/api/v1/expenses/{id}` | — | Soft-delete expense |
| **Payments** | `POST` | `/api/v1/groups/{id}/payments` | `RecordPaymentRequest` | Record settlement payment |
| **Bills** | `GET` | `/api/v1/bills` | — | Upcoming and overdue bills for current user |
| **Bills** | `POST` | `/api/v1/bills` | `CreateBillRequest` | Create upcoming bill with recurrence and assignees |
| **Bills** | `PUT` | `/api/v1/bills/{id}/pay` | — | Mark bill as paid |
| **Requests** | `GET` | `/api/v1/requests` | — | List incoming/outgoing money requests with expiry |
| **Requests** | `POST` | `/api/v1/requests` | `CreateMoneyRequestRequest`| Request money with expiration timestamp |
| **Requests** | `PUT` | `/api/v1/requests/{id}/settle`| — | Mark money request settled |
| **Activity** | `GET` | `/api/v1/groups/{id}/activity` | Pageable | Paginated group audit trail |
| **Admin** | `GET` | `/api/v1/admin/stats` | — | RUM & app metrics (admin role only) |

---

## 5. Docker Deployment with Maven

### Multi-Stage `Dockerfile`

```dockerfile
# Stage 1: Build JAR using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Extract Spring Boot layers
RUN java -Djarmode=layertools -jar target/fairshare-backend-0.0.1-SNAPSHOT.jar extract

# Stage 2: Minimal Distroless JRE Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app

COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```
