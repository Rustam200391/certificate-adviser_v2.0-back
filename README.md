# 🩺 Certificate Adviser — Backend

<p align="center">
  <strong>REST API for managing medical certificates</strong>
</p>

<p align="center">
  Backend service for storing, managing and retrieving medical certificates,
  patient data and doctor information.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-7-green?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-Database-blue?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Liquibase-Migrations-2962FF?style=for-the-badge&logo=liquibase&logoColor=white" alt="Liquibase">
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven">
</p>

---

## ✨ Overview

**Certificate Adviser** is a medical certificate management application.

The backend provides a REST API for:

- 👤 Patient information
- 👨‍⚕️ Doctor information
- 📋 Medical certificates
- 📎 Certificate file uploads
- 🔎 Certificate search
- 🗄️ PostgreSQL persistence
- 🕒 Automatic certificate creation timestamps

The project is designed as a foundation for a larger medical management platform.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| ☕ **Java 21** | Application language |
| 🌱 **Spring Boot** | Backend framework |
| 🌐 **Spring Web MVC** | REST API |
| 🗃️ **Spring Data JPA** | Database access |
| 🧩 **Hibernate** | ORM |
| 🐘 **PostgreSQL** | Database |
| 🔄 **Liquibase** | Database migrations |
| 📦 **Maven** | Build & dependency management |
| 🧪 **JUnit 5** | Testing |
| 🔬 **MockMvc** | API testing |
| 🎭 **Mockito** | Mocking |

---

## 🚀 Features

### 📋 Certificate Management

- ➕ Create certificates
- 📖 Get all certificates
- 🔎 Get certificate by ID
- 🔍 Search certificates
- 🗑️ Certificate management foundation

### 👤 Patient Data

Each certificate contains patient information:

- First name
- Last name

### 👨‍⚕️ Doctor Data

Each certificate contains:

- First name
- Last name
- Specialization

### 📎 File Storage

Certificate files can be uploaded together with certificate data.

Files are stored directly in PostgreSQL using:

```text
BYTEA

🕒 Automatic Timestamps

Every certificate receives an automatic creation timestamp:

created_at

The database uses:

TIMESTAMP WITH TIME ZONE
DEFAULT CURRENT_TIMESTAMP
🌐 API
➕ Create Certificate
POST /api/certificates

Content type:

multipart/form-data

Parameters:

Parameter	Type	Description
dto	JSON	Certificate information
file	File	Certificate document

Example:

{
  "patientFirstName": "Иван",
  "patientLastName": "Иванов",
  "doctorFirstName": "Пётр",
  "doctorLastName": "Петров",
  "doctorSpecialization": "Therapist"
}
📚 Get All Certificates
GET /api/certificates
🔎 Get Certificate
GET /api/certificates/{id}
🔍 Search
GET /api/certificates/search
🗄️ Database

The application uses PostgreSQL.

Main table:

certificates
Certificate structure
certificates
├── id
├── patient_first_name
├── patient_last_name
├── doctor_first_name
├── doctor_last_name
├── doctor_specialization
├── cert_image
└── created_at
🔄 Database Migrations

Database schema changes are managed by Liquibase.

Current changesets:

001-create-certificates-table
002-add-created-at-to-certificates

Master changelog:

src/main/resources/db/changelog/db.changelog-master.yaml
🧪 Testing

The project contains controller and integration tests.

Run all tests:

.\mvnw.cmd test
🔬 Integration Test

The integration test verifies the complete certificate workflow:

HTTP Request
     │
     ▼
🎮 Controller
     │
     ▼
⚙️ Service
     │
     ▼
🗃️ Repository
     │
     ▼
🐘 PostgreSQL
     │
     ▼
📨 HTTP Response

The test verifies:

✅ Certificate creation
✅ Database persistence
✅ Generated ID
✅ createdAt
✅ Patient information
✅ Certificate retrieval
✅ API response
📁 Project Structure
certificate-adviser_v2.0-back/
│
├── 📁 src/
│   ├── 📁 main/
│   │   ├── ☕ java/
│   │   │   └── com/example/certbackend/
│   │   │       ├── 🎮 controller/
│   │   │       ├── ⚙️ service/
│   │   │       ├── 🗃️ repository/
│   │   │       ├── 📦 entity/
│   │   │       ├── 📨 dto/
│   │   │       └── ⚠️ exception/
│   │   │
│   │   └── 📁 resources/
│   │       ├── ⚙️ application.properties
│   │       └── 🔄 db/changelog/
│   │
│   └── 📁 test/
│       └── 🧪 java/
│
├── 📄 pom.xml
├── 📄 mvnw
├── 📄 mvnw.cmd
└── 📄 README.md
⚙️ Running Locally
Requirements

Before running the application:

☕ JDK 21
🐘 PostgreSQL
📦 Maven
Database Configuration

Configure PostgreSQL in:

application.properties

Example:

spring.datasource.url=jdbc:postgresql://localhost:5432/certificate_adviser
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

🔐 Never commit real database passwords or secrets to GitHub.

▶️ Start Backend

Windows:

.\mvnw.cmd spring-boot:run

Backend:

http://localhost:8080
🔗 Application Architecture
                🖥️ Frontend
                     │
                     │ REST API
                     ▼
              🌱 Spring Boot
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
     ⚙️ Service             🧪 Validation
          │
          ▼
     🗃️ Repository
          │
          ▼
      🐘 PostgreSQL
          │
          ├── 📋 Certificate data
          └── 📎 Certificate files
📊 Current Status
Backend Core
Component	Status
🌱 Spring Boot	✅ Working
🌐 REST API	✅ Working
🗃️ PostgreSQL	✅ Working
🔄 Liquibase	✅ Working
📋 Certificate creation	✅ Working
📎 File upload	✅ Implemented
🕒 Created timestamp	✅ Implemented
🧪 Integration tests	✅ Implemented
🔍 Search	🟡 In development
🔐 Authentication	⏳ Planned
👥 Roles & permissions	⏳ Planned
📚 API documentation	⏳ Planned
🚀 Deployment	⏳ Planned
🗺️ Roadmap
 🔍 Advanced certificate search
 🔐 Authentication
 👥 Role-based access
 👨‍⚕️ Doctor management
 👤 Patient management
 📊 Statistics & reports
 📚 OpenAPI / Swagger documentation
 🐳 Docker deployment
 ☁️ Temporary cloud deployment
 🔗 Complete frontend integration
👨‍💻 Development

Certificate Adviser is actively developed as a modern medical management platform.

The backend is designed with a clear separation between:

Controller
    ↓
Service
    ↓
Repository
    ↓
Database

This structure allows the application to grow while keeping business logic, API endpoints and persistence separated.

📄 License

This project is currently developed as a private/demo application.

<p align="center"> 🩺 <strong>Certificate Adviser</strong> <br> Medical certificate management platform <br><br> <sub>Built with Java & Spring Boot</sub> </p> ```
