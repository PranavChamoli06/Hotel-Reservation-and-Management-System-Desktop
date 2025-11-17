---

# <div align="center">

```
███████╗ ███████╗ ███████╗██╗     ███████╗██████╗ 
██╔════╝ ██╔════╝ ██╔════╝██║     ██╔════╝██╔══██╗
█████╗   █████╗   █████╗  ██║     █████╗  ██████╔╝
██╔══╝   ██╔══╝   ██╔══╝  ██║     ██╔══╝  ██╔══██╗
██║      ██║      ███████╗███████╗███████╗██║  ██║
╚═╝      ╚═╝      ╚══════╝╚══════╝╚══════╝╚═╝  ╚═╝
```

### **HOTEL RESERVATION & BOOKING MANAGEMENT SYSTEM**

**A complete JavaFX + MySQL hotel booking platform with role-based access, reporting, and full CRUD functionality.**

</div>

---

# <div align="center">

## 🏷️ **Badges**

![Java](https://img.shields.io/badge/Java-17-blue?logo=oracle)
![JavaFX](https://img.shields.io/badge/JavaFX-Desktop%20UI-green)
![MySQL](https://img.shields.io/badge/MySQL-Database-orange?logo=mysql)
![Maven](https://img.shields.io/badge/Maven-Build%20Tool-red?logo=apachemaven)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)
![Status](https://img.shields.io/badge/Status-Phase%204%20Completed-success)

</div>

---

# 📌 **Overview**

This system is designed to automate hotel operations with a clean, modern JavaFX-based interface.
It supports Admin, Manager, Staff, and User roles, complete with reservation handling, reporting, and data export.

---

# 🚀 **Features (Phase 1 → Phase 4 Completed)**

### **🧩 Core Features**

* 🔑 Login & role-based authentication
* 🏨 Interactive dashboard for all roles
* 📝 Create / Edit / Delete reservations
* 🔍 Search by name, room number, and date
* 📅 Date-based filtering
* 🧮 Live price calculation

### **📊 Reporting & Export**

* 📄 CSV Export
* 📊 Excel Export
* 🧾 PDF Export
* 📆 Daily & Monthly booking reports

### **🧱 Tech Architecture**

* MVC architecture
* DAO pattern
* MySQL relational schema
* Secure password hashing
* Centralized session management

---

# 🛠️ **Tech Stack**

| Category        | Technologies                                    |
| --------------- | ----------------------------------------------- |
| **Frontend**    | JavaFX, FXML, CSS3                              |
| **Backend**     | Java 17, JDBC                                   |
| **Database**    | MySQL                                           |
| **Build Tools** | Maven                                           |
| **Security**    | Password hashing, session-based role management |

---

# 📸 **Screenshots**

### 🔐 **Login Screen**

![Login Screen](screenshots/login-screen.png)

---

### 🧑‍💼 **Admin Dashboard**

![Admin Dashboard](screenshots/admin-dashboard.png)

---

### 👔 **Manager Dashboard**

![Manager Dashboard](screenshots/manager-dashboard.png)

---

### 👷‍♂️ **Staff Dashboard**

![Staff Dashboard](screenshots/staff-dashboard.png)

---

### 👤 **User Dashboard**

![User Dashboard](screenshots/user-dashboard.png)

---

### 📝 **Reservation Form**

![Reservation Form](screenshots/reservation-form.png)

---

# ⚙️ **Installation & Setup**

### **1️⃣ Clone the Repository**

```bash
git clone https://github.com/<username>/hotel-reservation-management-system.git
cd hotel-reservation-management-system
```

### **2️⃣ Configure Database**

Update:

```
src/main/resources/db.properties
```

Example:

```
db.url=jdbc:mysql://localhost:3306/hotel_db
db.username=root
db.password=yourpassword
```

### **3️⃣ Build & Run**

```bash
mvn clean install
mvn javafx:run
```

---

# 🧭 **Project Structure**

```
src/main/java/com/hotel/
 ├── controller/
 ├── dao/
 ├── model/
 ├── reports/
 ├── util/
 ├── migration/
 ├── security/
 ├── session/
 └── MainApp.java

src/main/resources/
 ├── fxml/
 ├── css/
 ├── images/
 ├── db.properties
 └── logging.properties

screenshots/
 ├── admin-dashboard.png
 ├── login-screen.png
 ├── manager-dashboard.png
 ├── staff-dashboard.png
 ├── user-dashboard.png
 └── reservation-form.png
```

---

# 📅 **Roadmap (Upcoming Phases)**

### **Phase 5 – Spring Boot Migration**

* Goal: Convert to a web-based app.

### **Phase 6 – Advanced Features**

* Goal: Enterprise-level capabilities.

### **Phase 7 — Deployment**

* Goal: Make it live.

---

# 🧾 **License**

This project is licensed under the **MIT License**.

---

# 🙌 **Contributors**

**Pranav Chamoli**
*Developer & Architect*

---
