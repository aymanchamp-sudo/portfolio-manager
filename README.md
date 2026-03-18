# PortfolioX — Portfolio Manager System
### Java Backend + REST API

> File-based, zero-dependency Java backend for the Portfolio Manager System.  
> Built with plain Java 21 — no Maven, no Spring, no external JARs.

---

## 📁 Project Structure

```
portfolio-backend/
├── build.sh                          ← Compile & run script
├── data/                             ← Auto-created at runtime
│   ├── portfolio.dat                 ← Binary save file
│   ├── portfolio.bak                 ← Backup (when enabled)
│   ├── portfolio_export.csv          ← CSV export output
│   ├── portfolio_export.json         ← JSON export output
│   ├── portfolio_report.txt          ← TXT export output
│   └── activity.log                  ← Activity audit log
└── src/main/java/com/portfolio/
    ├── PortfolioServer.java          ← Main entry point (HTTP server)
    ├── model/
    │   ├── Investment.java           ← Data object + P&L logic
    │   ├── Portfolio.java            ← Collection + aggregates
    │   └── ActivityLog.java          ← Audit log entry
    ├── service/
    │   └── PortfolioService.java     ← Business logic (singleton)
    ├── controller/
    │   └── Router.java               ← HTTP request router
    └── util/
        ├── FileHandler.java          ← All file I/O (.dat, CSV, JSON, TXT)
        └── JsonParser.java           ← Lightweight JSON parser/builder
```

---

## 🚀 Quick Start

### 1. Compile & Run
```bash
chmod +x build.sh
./build.sh
```
Server starts at: **http://localhost:8080**

### 2. Custom Port
```bash
./build.sh run 9090
```

### 3. Compile Only
```bash
./build.sh compile
```

### 4. Place Frontend Alongside
```bash
# Copy your index.html next to build.sh, then visit:
# http://localhost:8080/
```

---

## 🌐 REST API Reference

Base URL: `http://localhost:8080`

### Health
| Method | Endpoint       | Description      |
|--------|----------------|------------------|
| GET    | /api/health    | Server status    |

**Response:**
```json
{"status":"ok","server":"PortfolioX Backend","version":"1.0.0"}
```

---

### Investments

| Method | Endpoint                        | Description              |
|--------|---------------------------------|--------------------------|
| GET    | /api/investments                | Get all investments      |
| GET    | /api/investments?filter=profit  | Filter: profit/loss/all  |
| GET    | /api/investments/{id}           | Get single investment    |
| POST   | /api/investments                | Add new investment       |
| PUT    | /api/investments/{id}           | Update investment        |
| DELETE | /api/investments/{id}           | Delete investment        |

**Add / Update Request Body:**
```json
{
  "name":         "Reliance Industries",
  "type":         "Stock",
  "sector":       "Energy",
  "amount":       72000,
  "buyPrice":     2400,
  "sellPrice":    2800,
  "quantity":     30,
  "purchaseDate": "2024-01-15",
  "notes":        "Long term hold"
}
```

**Investment Response:**
```json
{
  "status": "ok",
  "data": {
    "id":             1,
    "name":           "Reliance Industries",
    "type":           "Stock",
    "sector":         "Energy",
    "amount":         72000.00,
    "buyPrice":       2400.00,
    "sellPrice":      2800.00,
    "quantity":       30,
    "purchaseDate":   "2024-01-15",
    "notes":          "Long term hold",
    "profitLoss":     12000.00,
    "returnPercent":  16.67,
    "currentValue":   84000.00,
    "isProfit":       true
  }
}
```

---

### Summary
| Method | Endpoint      | Description              |
|--------|---------------|--------------------------|
| GET    | /api/summary  | Portfolio-wide analytics |

**Response:**
```json
{
  "status": "ok",
  "data": {
    "totalInvested":       245000.00,
    "totalCurrentValue":   312450.00,
    "totalProfitLoss":     67450.00,
    "totalProfit":         75650.00,
    "totalLoss":           -8200.00,
    "overallReturnPercent": 27.53,
    "count":               5,
    "bestPerformer":       { ...investment... },
    "worstPerformer":      { ...investment... }
  }
}
```

---

### P&L Calculator
| Method | Endpoint        | Description              |
|--------|-----------------|--------------------------|
| POST   | /api/calculate  | Stateless P&L calculator |

**Request:**
```json
{ "buyPrice": 2400, "sellPrice": 2800, "quantity": 30, "fees": 200 }
```

**Response:**
```json
{
  "status": "ok",
  "data": {
    "grossPL":        12000.00,
    "fees":           200.00,
    "netPL":          11800.00,
    "returnPercent":  16.39,
    "isProfit":       true
  }
}
```

---

### Save & Load
| Method | Endpoint    | Body                    | Description              |
|--------|-------------|-------------------------|--------------------------|
| POST   | /api/save   | `{"backup": true}`      | Save portfolio to .dat   |
| POST   | /api/load   | _(empty)_               | Load portfolio from .dat |
| GET    | /api/fileinfo | —                     | File metadata            |

---

### Export & Import
| Method | Endpoint          | Body                          | Description     |
|--------|-------------------|-------------------------------|-----------------|
| POST   | /api/export/csv   | _(empty)_                     | Export to CSV   |
| POST   | /api/export/json  | _(empty)_                     | Export to JSON  |
| POST   | /api/export/txt   | _(empty)_                     | Export to TXT   |
| POST   | /api/import/csv   | `{"filePath":"path/to.csv"}`  | Import from CSV |

---

### Activity Logs
| Method | Endpoint    | Description          |
|--------|-------------|----------------------|
| GET    | /api/logs   | Get all log entries  |
| DELETE | /api/logs   | Clear all logs       |

---

### Settings
| Method | Endpoint               | Body                      | Description         |
|--------|------------------------|---------------------------|---------------------|
| POST   | /api/settings/autosave | `{"autoSave": "true"}`    | Toggle auto-save    |

---

## 🏗️ Architecture

```
Browser / Frontend
       │  HTTP JSON
       ▼
 PortfolioServer        ← Java HttpServer (port 8080)
       │
     Router             ← Routes method + path → handler
       │
 PortfolioService       ← Singleton, business logic
       │
  ┌────┴────┐
Portfolio  FileHandler  ← In-memory store / disk I/O
  │
Investment[]            ← Core data objects
```

---

## 💾 P&L Formula

```
Profit/Loss = (Sell Price - Buy Price) × Quantity
Return %    = (P&L / Total Amount) × 100
Net P&L     = Gross P&L - Brokerage Fees
```

---

## ✅ Features Implemented

- [x] Add / Update / Delete investments
- [x] View all investments (with filter: profit / loss / all)
- [x] Portfolio summary with aggregates
- [x] P&L calculation (gross + net after fees)
- [x] Best / worst performer detection
- [x] Binary file persistence (`.dat` via Java Serialization)
- [x] Auto-save on every write operation
- [x] Manual save with `.bak` backup
- [x] Load saved portfolio
- [x] Export: CSV, JSON, TXT report
- [x] Import from CSV
- [x] Activity log (persisted to `activity.log`)
- [x] CORS headers for frontend integration
- [x] Graceful shutdown hook
- [x] Zero external dependencies

---

## 🔌 Connecting the Frontend

In `index.html`, the JavaScript API layer calls:
```
http://localhost:8080/api/...
```

Start the backend first, then open `index.html` in your browser (or serve it via the Java server at `http://localhost:8080/`).
