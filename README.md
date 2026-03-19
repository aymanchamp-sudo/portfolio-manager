# PortfolioX — Investment Portfolio Manager
### Java Backend + REST API + PostgreSQL + Multi-User Auth

> A production-ready full-stack investment portfolio manager.  
> Built with pure Java — no Maven, no Spring, no external JARs (except PostgreSQL JDBC driver).  
> Deployed on Render with Supabase PostgreSQL. Multi-user with session-based auth.

🌐 **Live:** https://portfolio-manager-xtom.onrender.com  
📦 **GitHub:** https://github.com/aymanchamp-sudo/portfolio-manager

---

## 📁 Project Structure

```
portfolio-backend/
├── Dockerfile                        ← Docker build for Render deployment
├── build.sh                          ← Local compile & run script
├── index.html                        ← Frontend (served at /)
└── src/main/java/com/portfolio/
    ├── PortfolioServer.java          ← Main entry point (HTTP server)
    ├── model/
    │   ├── Investment.java           ← Data object + P&L logic
    │   ├── Portfolio.java            ← Collection + aggregates
    │   └── ActivityLog.java          ← Audit log entry
    ├── service/
    │   ├── PortfolioService.java     ← Business logic
    │   └── AuthService.java          ← Register, login, session management
    ├── controller/
    │   └── Router.java               ← HTTP request router + auth middleware
    └── util/
        ├── Database.java             ← PostgreSQL connection + schema init
        ├── InvestmentRepository.java ← DB CRUD for investments
        ├── FileHandler.java          ← CSV import/export
        └── JsonParser.java           ← Lightweight JSON parser/builder
```

---

## 🚀 Quick Start (Local)

### Prerequisites
- Java 17+
- PostgreSQL database (or Supabase free tier)

### 1. Set environment variable
```bash
export DATABASE_URL="postgresql://user:password@host:port/db"
```

### 2. Compile & Run
```bash
chmod +x build.sh
./build.sh
```
Server starts at: **http://localhost:8080**

---

## 🐳 Docker / Render Deployment

```bash
docker build -t portfoliox .
docker run -e DATABASE_URL="your_connection_string" -p 8080:8080 portfoliox
```

Deploy on Render: connect GitHub repo → set DATABASE_URL env variable → auto-deploys on every push.

---

## 🌐 REST API Reference

Base URL: `https://portfolio-manager-xtom.onrender.com`

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Create account |
| POST | /api/auth/login | Login → returns session token |
| POST | /api/auth/logout | Invalidate session |

All other endpoints require: `Authorization: Bearer <token>`

### Investments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/investments | Get all (supports ?filter=profit/loss) |
| GET | /api/investments/{id} | Get single |
| POST | /api/investments | Add |
| PUT | /api/investments/{id} | Update |
| DELETE | /api/investments/{id} | Delete |

### Other Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/summary | Portfolio-wide analytics |
| POST | /api/calculate | Stateless P&L calculator |
| POST | /api/import/csv | Import CSV (raw CSV body) |
| POST | /api/export/csv | Export portfolio as CSV |
| GET | /api/health | Server status |

---

## 💾 P&L Formula

```
Profit/Loss = (Sell Price - Buy Price) × Quantity
Return %    = (P&L / Amount Invested) × 100
Net P&L     = Gross P&L - Brokerage Fees
```

---

## ✅ Features

- [x] Multi-user authentication (register / login / logout)
- [x] Per-user data isolation via PostgreSQL
- [x] Add / Update / Delete investments
- [x] Portfolio summary with aggregates + smart insights
- [x] P&L calculator (gross + net after fees)
- [x] CSV import (browser file upload) and export
- [x] Activity log
- [x] Persistent cloud storage (Supabase PostgreSQL)
- [x] Dockerised for cloud deployment on Render
- [x] Android app via Capacitor (signed AAB for Play Store)
- [x] CORS headers for browser fetch()

---

## 📱 Mobile App

Android app built with Capacitor wrapping the web frontend.  
Loads the live Render URL — UI updates automatically on redeploy.  
Signed AAB generated for Google Play Store submission.
