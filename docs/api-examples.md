# API examples

These requests use `http://localhost:8080` and require `curl`; response extraction examples also use `jq`. Start the stack as described in the root README.

## Register and authenticate a student

```bash
curl --fail-with-body \
  --request POST http://localhost:8080/api/v1/auth/register \
  --header 'Content-Type: application/json' \
  --data '{"email":"demo.student@example.com","password":"correct-horse-battery-staple"}'
```

```bash
STUDENT_LOGIN=$(curl --fail-with-body --silent \
  --request POST http://localhost:8080/api/v1/auth/login \
  --header 'Content-Type: application/json' \
  --data '{"email":"demo.student@example.com","password":"correct-horse-battery-staple"}')

export STUDENT_TOKEN=$(printf '%s' "$STUDENT_LOGIN" | jq -r '.accessToken')
export REFRESH_TOKEN=$(printf '%s' "$STUDENT_LOGIN" | jq -r '.refreshToken')
```

Self-registration always assigns `STUDENT`. Staff and administrator role provisioning is intentionally an administrative database/identity operation, not a public escalation endpoint. Set a token for an appropriately provisioned staff account before the management examples:

```bash
export STAFF_TOKEN='<staff access token>'
```

## Rotate and revoke credentials

```bash
ROTATED_TOKENS=$(curl --fail-with-body --silent \
  --request POST http://localhost:8080/api/v1/auth/refresh \
  --header 'Content-Type: application/json' \
  --data "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

export REFRESH_TOKEN=$(printf '%s' "$ROTATED_TOKENS" | jq -r '.refreshToken')
```

The refresh response contains a new refresh token; the presented token cannot be used again.

```bash
curl --fail-with-body \
  --request POST http://localhost:8080/api/v1/auth/logout \
  --header 'Content-Type: application/json' \
  --data "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

## Create a catalogue resource and physical item

```bash
RESOURCE=$(curl --fail-with-body --silent \
  --request POST http://localhost:8080/api/v1/resources \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  --header 'Content-Type: application/json' \
  --data '{
    "name":"Dell Latitude 7450",
    "description":"Shared engineering laptop",
    "type":"LAPTOP",
    "category":"IT",
    "identifier":"DELL-7450",
    "loanable":true
  }')

export RESOURCE_ID=$(printf '%s' "$RESOURCE" | jq -r '.id')
```

```bash
ITEM=$(curl --fail-with-body --silent \
  --request POST "http://localhost:8080/api/v1/resources/$RESOURCE_ID/items" \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  --header 'Content-Type: application/json' \
  --data '{"assetTag":"IT-LAPTOP-0042"}')

export ITEM_ID=$(printf '%s' "$ITEM" | jq -r '.id')
```

## Browse and filter inventory

```bash
curl --fail-with-body \
  --get http://localhost:8080/api/v1/resources \
  --header "Authorization: Bearer $STUDENT_TOKEN" \
  --data-urlencode 'type=LAPTOP' \
  --data-urlencode 'status=AVAILABLE' \
  --data-urlencode 'size=10' \
  --data-urlencode 'sort=name,asc'
```

## Execute a loan workflow

```bash
LOAN=$(curl --fail-with-body --silent \
  --request POST http://localhost:8080/api/v1/loans \
  --header "Authorization: Bearer $STUDENT_TOKEN" \
  --header "Idempotency-Key: loan-request-$ITEM_ID" \
  --header 'Content-Type: application/json' \
  --data "{\"resourceItemId\":\"$ITEM_ID\"}")

export LOAN_ID=$(printf '%s' "$LOAN" | jq -r '.id')
```

```bash
curl --fail-with-body --request POST \
  "http://localhost:8080/api/v1/loans/$LOAN_ID/approve" \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  --header "Idempotency-Key: loan-approve-$LOAN_ID"

curl --fail-with-body --request POST \
  "http://localhost:8080/api/v1/loans/$LOAN_ID/collect" \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  --header "Idempotency-Key: loan-collect-$LOAN_ID"

curl --fail-with-body --request POST \
  "http://localhost:8080/api/v1/loans/$LOAN_ID/return" \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  --header "Idempotency-Key: loan-return-$LOAN_ID"
```

## Reserve a resource

```bash
curl --fail-with-body \
  --request POST "http://localhost:8080/api/v1/resources/$RESOURCE_ID/reservations" \
  --header "Authorization: Bearer $STUDENT_TOKEN" \
  --header "Idempotency-Key: reservation-$RESOURCE_ID"
```

```bash
curl --fail-with-body http://localhost:8080/api/v1/reservations \
  --header "Authorization: Bearer $STUDENT_TOKEN"
```

## Query and export operational reports

```bash
curl --fail-with-body \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  http://localhost:8080/api/v1/reports/dashboard

curl --fail-with-body \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  'http://localhost:8080/api/v1/reports/popular-resources?limit=10'

curl --fail-with-body \
  --header "Authorization: Bearer $STAFF_TOKEN" \
  --output active-loans.csv \
  'http://localhost:8080/api/v1/reports/export.csv?report=LOANS&loanStatus=ACTIVE'
```

`currentUtilizationPercent` is the share of physical items currently `ON_LOAN`; unavailable inventory also includes reserved, maintenance, lost and retired items.

## Inspect the error contract

```bash
curl --include \
  --header 'X-Correlation-ID: recruiter-demo-001' \
  --header "Authorization: Bearer $STUDENT_TOKEN" \
  http://localhost:8080/api/v1/resources/00000000-0000-0000-0000-000000000000
```

The response is `application/problem+json`, includes stable code `RESOURCE_NOT_FOUND`, echoes the safe correlation ID in `X-Correlation-ID` and includes it in the body.

## Postman

Postman can import the live OpenAPI document directly:

1. start the application;
2. choose **Import → Link** in Postman;
3. enter `http://localhost:8080/v3/api-docs`;
4. define collection variables `baseUrl`, `studentToken` and `staffToken`;
5. use bearer authentication with the appropriate token variable.

This keeps the Postman collection aligned with the generated contract rather than maintaining a second hand-written API definition.
