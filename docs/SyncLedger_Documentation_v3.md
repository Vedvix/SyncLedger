# SyncLedger - Complete Project Documentation
## Single Source of Truth for Business & Development Teams
### by Vedvix (Nevorix)

---

**Document Control**

| Attribute | Value |
|-----------|-------|
| Version | 3.0 |
| Date | February 7, 2026 |
| Status | Active |
| Owner | Vedvix Development Team |
| Classification | Internal |

---

## 📋 Table of Contents

### PART A: BUSINESS DOCUMENTATION
1. [Executive Summary](#1-executive-summary)
2. [Business Requirements](#2-business-requirements)
3. [Use Case Diagrams](#3-use-case-diagrams)
4. [User Roles & Permissions](#4-user-roles--permissions)

### PART B: TECHNICAL ARCHITECTURE
5. [System Architecture](#5-system-architecture)
6. [Sequence Diagrams](#6-sequence-diagrams)
7. [Data Flow Diagrams](#7-data-flow-diagrams)
8. [Database Design](#8-database-design)

### PART C: THIRD-PARTY SETUP GUIDES
9. [AWS Account Setup](#9-aws-account-setup)
10. [Microsoft 365 & Graph API Setup](#10-microsoft-365--graph-api-setup)
11. [Sage Intacct Integration Setup](#11-sage-intacct-integration-setup)

### PART D: DEVELOPMENT GUIDE
12. [Development Environment Setup](#12-development-environment-setup)
13. [Backend Development (Spring Boot)](#13-backend-development-spring-boot)
14. [PDF Microservice (Python)](#14-pdf-microservice-python)
15. [Frontend Development (React)](#15-frontend-development-react)

### PART E: DEPLOYMENT & OPERATIONS
16. [Deployment Guide](#16-deployment-guide)
17. [API Reference](#17-api-reference)
18. [Cost Estimation](#18-cost-estimation)

---

# PART A: BUSINESS DOCUMENTATION

---

## 1. Executive Summary

### 1.1 What is SyncLedger?

**SyncLedger** is a multi-tenant SaaS invoice processing platform that automates the entire invoice lifecycle - from email receipt to accounting system integration.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYNCLEDGER AT A GLANCE                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  BEFORE (Manual Process)              AFTER (With SyncLedger)               │
│  ════════════════════════             ═══════════════════════               │
│                                                                             │
│  📧 Check email manually              📧 Auto-monitored inbox               │
│  📄 Open PDF, read data               🤖 AI extracts data automatically     │
│  ✍️ Type into spreadsheet             💾 Auto-saved to database             │
│  📧 Email for approval                👆 One-click approval in portal       │
│  ⏳ Wait days for response            ⚡ Instant notifications              │
│  ✍️ Manual Sage entry                 🔗 Auto-sync to Sage                  │
│                                                                             │
│  ⏱️ 15-30 min per invoice             ⏱️ 2-5 min per invoice                │
│  ❌ Error-prone                       ✅ 95%+ accuracy                      │
│  ❌ No audit trail                    ✅ Complete audit history             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Key Features

| Feature | Description |
|---------|-------------|
| **Multi-Tenant SaaS** | Multiple organizations, each with isolated data |
| **Email Integration** | Auto-reads invoices from dedicated email inboxes |
| **AI PDF Extraction** | Extracts data from any PDF format using AI/OCR |
| **Approval Workflow** | Route invoices for approval with audit trail |
| **Sage Integration** | Auto-sync approved invoices to Sage accounting |
| **Role-Based Access** | Super Admin, Admin, Approver, Viewer roles |

### 1.3 Technology Stack Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TECHNOLOGY STACK                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  FRONTEND          BACKEND             PDF SERVICE        INFRASTRUCTURE   │
│  ─────────         ───────             ───────────        ──────────────   │
│  React 18          Java 21             Python 3.12        AWS Cloud        │
│  TypeScript        Spring Boot 3       FastAPI            PostgreSQL       │
│  Tailwind CSS      Spring Security     PyMuPDF            S3 Storage       │
│  Vite              JWT Auth            Tesseract OCR      SQS Queues       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Business Requirements

### 2.1 Problem Statement

Organizations currently process invoices manually, which causes:
- ⏱️ **Time waste**: 15-30 minutes per invoice
- ❌ **Errors**: Manual data entry mistakes
- 📭 **Lost invoices**: Emails get buried or missed
- 🔍 **No visibility**: Can't track invoice status
- 📋 **No audit trail**: Who approved what and when?

### 2.2 Solution Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    INVOICE PROCESSING FLOW                                  │
└─────────────────────────────────────────────────────────────────────────────┘

  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
  │  Vendor  │    │  Email   │    │   PDF    │    │  Portal  │    │   Sage   │
  │  Sends   │───▶│  Inbox   │───▶│ Extract  │───▶│ Approval │───▶│   Sync   │
  │  Invoice │    │  (M365)  │    │  (AI)    │    │          │    │          │
  └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
       │               │               │               │               │
       │               │               │               │               │
       ▼               ▼               ▼               ▼               ▼
   PDF Email      Auto-detect     Extract all     Review &       Create in
   to dedicated   new emails      invoice data    Approve/       Accounting
   org inbox      every 5 min     with AI         Reject         System
```

### 2.3 Multi-Tenant Model

SyncLedger operates as a SaaS platform serving multiple client organizations:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    MULTI-TENANT ARCHITECTURE                                │
└─────────────────────────────────────────────────────────────────────────────┘

                        ┌─────────────────────────┐
                        │    SYNCLEDGER PLATFORM   │
                        │    (Managed by Nevorix)  │
                        └────────────┬────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
         ▼                           ▼                           ▼
  ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
  │   LONGHOME      │       │    EVOLOTEK     │       │   ACME CORP     │
  │   ───────────   │       │   ───────────   │       │   ───────────   │
  │                 │       │                 │       │                 │
  │ 📧 longhome@    │       │ 📧 evolotek@    │       │ 📧 acme@        │
  │    nevorix.co   │       │    nevorix.co   │       │    nevorix.co   │
  │                 │       │                 │       │                 │
  │ 👥 Users: 8     │       │ 👥 Users: 5     │       │ 👥 Users: 12    │
  │ 📄 Invoices     │       │ 📄 Invoices     │       │ 📄 Invoices     │
  │ 📊 Reports      │       │ 📊 Reports      │       │ 📊 Reports      │
  │                 │       │                 │       │                 │
  │ 🔒 ISOLATED     │       │ 🔒 ISOLATED     │       │ 🔒 ISOLATED     │
  └─────────────────┘       └─────────────────┘       └─────────────────┘
  
  Each organization has:
  ✅ Dedicated email inbox (org@nevorix.co)
  ✅ Isolated database records (filtered by org_id)
  ✅ Separate S3 storage folder
  ✅ Own processing queue
  ✅ Independent users and permissions
```

### 2.4 Stakeholders

| Stakeholder | Role in System | Access Level |
|-------------|----------------|--------------|
| **Nevorix (Platform Owner)** | Manages the SaaS platform | Super Admin |
| **Client Organization** | Uses the platform for their invoices | Admin + Users |
| **Vendors** | Send invoices via email | No portal access |

---

## 3. Use Case Diagrams

### 3.1 System Use Case Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         USE CASE DIAGRAM                                    │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────────────────────────┐
                              │         SYNCLEDGER SYSTEM           │
                              │                                     │
  ┌──────────┐                │  ┌───────────────────────────────┐  │
  │  SUPER   │────────────────│──│ UC-01: Manage Organizations   │  │
  │  ADMIN   │                │  └───────────────────────────────┘  │
  │(Platform)│────────────────│──┌───────────────────────────────┐  │
  └──────────┘                │  │ UC-02: Create Org Admins      │  │
       │                      │  └───────────────────────────────┘  │
       │                      │  ┌───────────────────────────────┐  │
       └──────────────────────│──│ UC-03: View All Data/Reports  │  │
                              │  └───────────────────────────────┘  │
                              │                                     │
  ┌──────────┐                │  ┌───────────────────────────────┐  │
  │  ORG     │────────────────│──│ UC-04: Manage Org Users       │  │
  │  ADMIN   │                │  └───────────────────────────────┘  │
  │          │────────────────│──┌───────────────────────────────┐  │
  └──────────┘                │  │ UC-05: View Org Invoices      │  │
       │                      │  └───────────────────────────────┘  │
       │                      │  ┌───────────────────────────────┐  │
       └──────────────────────│──│ UC-06: Configure Sage         │  │
                              │  └───────────────────────────────┘  │
                              │                                     │
  ┌──────────┐                │  ┌───────────────────────────────┐  │
  │ APPROVER │────────────────│──│ UC-07: Review Invoices        │  │
  │          │                │  └───────────────────────────────┘  │
  │          │────────────────│──┌───────────────────────────────┐  │
  └──────────┘                │  │ UC-08: Approve/Reject Invoice │  │
                              │  └───────────────────────────────┘  │
                              │                                     │
  ┌──────────┐                │  ┌───────────────────────────────┐  │
  │  VIEWER  │────────────────│──│ UC-09: View Invoices (R/O)    │  │
  └──────────┘                │  └───────────────────────────────┘  │
                              │                                     │
  ┌──────────┐                │  ┌───────────────────────────────┐  │
  │  SYSTEM  │────────────────│──│ UC-10: Poll Email Inbox       │  │
  │  (Auto)  │                │  └───────────────────────────────┘  │
  │          │────────────────│──┌───────────────────────────────┐  │
  │          │                │  │ UC-11: Extract PDF Data       │  │
  │          │────────────────│──└───────────────────────────────┘  │
  │          │                │  ┌───────────────────────────────┐  │
  └──────────┘────────────────│──│ UC-12: Sync to Sage           │  │
                              │  └───────────────────────────────┘  │
                              │                                     │
                              └─────────────────────────────────────┘
```

### 3.2 Use Case Details

#### UC-01: Manage Organizations (Super Admin)
| Field | Description |
|-------|-------------|
| **Actor** | Super Admin |
| **Description** | Create, edit, disable client organizations |
| **Precondition** | Logged in as Super Admin |
| **Main Flow** | 1. View organization list → 2. Add/Edit org → 3. Configure email/Sage → 4. Save |
| **Postcondition** | Organization created with dedicated resources |

#### UC-07: Review Invoices (Approver)
| Field | Description |
|-------|-------------|
| **Actor** | Approver |
| **Description** | Review invoice details and PDF |
| **Precondition** | Invoice in PENDING_APPROVAL status |
| **Main Flow** | 1. View invoice list → 2. Select invoice → 3. Review PDF & data → 4. Edit if needed |
| **Postcondition** | Invoice ready for approval decision |

#### UC-08: Approve/Reject Invoice (Approver)
| Field | Description |
|-------|-------------|
| **Actor** | Approver |
| **Description** | Approve or reject an invoice |
| **Precondition** | Invoice reviewed |
| **Main Flow** | 1. Click Approve/Reject → 2. Add comments (required for reject) → 3. Submit |
| **Postcondition** | Invoice status updated, audit log created |

---

## 4. User Roles & Permissions

### 4.1 Role Hierarchy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ROLE HIERARCHY                                      │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────┐
                    │        SUPER_ADMIN          │
                    │   (SyncLedger Platform)     │
                    │   org_id = NULL             │
                    └──────────────┬──────────────┘
                                   │
                    Creates Org Admins ↓
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
  ┌───────────────┐         ┌───────────────┐         ┌───────────────┐
  │     ADMIN     │         │     ADMIN     │         │     ADMIN     │
  │  (Longhome)   │         │  (Evolotek)   │         │  (Acme Corp)  │
  │  org_id = 1   │         │  org_id = 2   │         │  org_id = 3   │
  └───────┬───────┘         └───────┬───────┘         └───────┬───────┘
          │                         │                         │
  Creates Org Users ↓               │                         │
          │                         │                         │
     ┌────┴────┐               ┌────┴────┐               ┌────┴────┐
     ▼         ▼               ▼         ▼               ▼         ▼
 ┌────────┐ ┌────────┐    ┌────────┐ ┌────────┐    ┌────────┐ ┌────────┐
 │APPROVER│ │ VIEWER │    │APPROVER│ │ VIEWER │    │APPROVER│ │ VIEWER │
 │org_id=1│ │org_id=1│    │org_id=2│ │org_id=2│    │org_id=3│ │org_id=3│
 └────────┘ └────────┘    └────────┘ └────────┘    └────────┘ └────────┘
```

### 4.2 Permission Matrix

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         PERMISSION MATRIX                                   │
└─────────────────────────────────────────────────────────────────────────────┘

│ Permission                      │ SUPER_ADMIN │ ADMIN │ APPROVER │ VIEWER │
│─────────────────────────────────│─────────────│───────│──────────│────────│
│ ORGANIZATIONS                   │             │       │          │        │
│ Create/Edit Organizations       │     ✅      │  ❌   │    ❌    │   ❌   │
│ View All Organizations          │     ✅      │  ❌   │    ❌    │   ❌   │
│─────────────────────────────────│─────────────│───────│──────────│────────│
│ USERS                           │             │       │          │        │
│ Create ADMIN users              │     ✅      │  ❌   │    ❌    │   ❌   │
│ Create APPROVER/VIEWER users    │     ✅      │  ✅*  │    ❌    │   ❌   │
│ View All Users (any org)        │     ✅      │  ❌   │    ❌    │   ❌   │
│ View Org Users (own org)        │     ✅      │  ✅   │    ❌    │   ❌   │
│─────────────────────────────────│─────────────│───────│──────────│────────│
│ INVOICES                        │             │       │          │        │
│ View Invoices (all orgs)        │     ✅      │  ❌   │    ❌    │   ❌   │
│ View Invoices (own org)         │     ✅      │  ✅   │    ✅    │   ✅   │
│ Edit Invoice Data               │     ✅      │  ✅   │    ✅    │   ❌   │
│ Approve/Reject Invoices         │     ✅      │  ✅   │    ✅    │   ❌   │
│─────────────────────────────────│─────────────│───────│──────────│────────│
│ REPORTS                         │             │       │          │        │
│ Platform-wide Reports           │     ✅      │  ❌   │    ❌    │   ❌   │
│ Organization Reports            │     ✅      │  ✅   │    ✅    │   ✅   │
│ Audit Logs (all orgs)           │     ✅      │  ❌   │    ❌    │   ❌   │
│ Audit Logs (own org)            │     ✅      │  ✅   │    ❌    │   ❌   │
│─────────────────────────────────│─────────────│───────│──────────│────────│

* ADMIN can only create users within their own organization
```

---

# PART B: TECHNICAL ARCHITECTURE

---

## 5. System Architecture

### 5.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SYNCLEDGER SYSTEM ARCHITECTURE                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              INTERNET                                            │
└───────────────────────────────────┬─────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│   VENDORS     │          │    USERS      │          │   MICROSOFT   │
│  (Send PDF    │          │  (Web Portal) │          │     365       │
│   via Email)  │          │               │          │   (Outlook)   │
└───────────────┘          └───────┬───────┘          └───────┬───────┘
                                   │                          │
                                   │ HTTPS                    │ Graph API
                                   ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              AWS CLOUD                                           │
│  ┌───────────────────────────────────────────────────────────────────────────┐  │
│  │                         APPLICATION LAYER                                  │  │
│  │                                                                           │  │
│  │  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐       │  │
│  │  │   REACT SPA     │    │  SPRING BOOT    │    │ PYTHON FASTAPI  │       │  │
│  │  │   (Frontend)    │◄──►│  (Main API)     │◄──►│ (PDF Service)   │       │  │
│  │  │                 │    │                 │    │                 │       │  │
│  │  │  • Dashboard    │    │  • REST API     │    │  • OCR          │       │  │
│  │  │  • Invoice List │    │  • Auth (JWT)   │    │  • AI Extract   │       │  │
│  │  │  • Approval     │    │  • Business     │    │  • PDF Parse    │       │  │
│  │  │  • User Mgmt    │    │    Logic        │    │                 │       │  │
│  │  │  • Settings     │    │  • Email Poll   │    │                 │       │  │
│  │  └─────────────────┘    └────────┬────────┘    └─────────────────┘       │  │
│  │         S3                       │                      ▲                 │  │
│  │                                  │                      │                 │  │
│  └──────────────────────────────────┼──────────────────────┼─────────────────┘  │
│                                     │                      │                    │
│  ┌──────────────────────────────────┼──────────────────────┼─────────────────┐  │
│  │                         DATA LAYER│         SQS Queue   │                 │  │
│  │                                  │                      │                 │  │
│  │  ┌─────────────────┐    ┌───────┴───────┐    ┌─────────┴─────┐           │  │
│  │  │   AWS S3        │    │  PostgreSQL   │    │   AWS SQS     │           │  │
│  │  │   (Files)       │    │  (Database)   │    │   (Queues)    │           │  │
│  │  │                 │    │               │    │               │           │  │
│  │  │  /longhome/*    │    │  • users      │    │ • pdf-process │           │  │
│  │  │  /evolotek/*    │    │  • invoices   │    │ • sage-sync   │           │  │
│  │  │  /acme/*        │    │  • orgs       │    │ • email-notif │           │  │
│  │  └─────────────────┘    └───────────────┘    └───────────────┘           │  │
│  │                                                                           │  │
│  └───────────────────────────────────────────────────────────────────────────┘  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                          ┌───────────────┐
                          │   SAGE API    │
                          │  (Accounting) │
                          └───────────────┘
```

### 5.2 Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    COMPONENT ARCHITECTURE                                        │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│ FRONTEND (React + TypeScript)                                                    │
│ ─────────────────────────────                                                    │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│ │   Pages      │ │  Components  │ │   Services   │ │    Store     │            │
│ │ ──────────── │ │ ──────────── │ │ ──────────── │ │ ──────────── │            │
│ │ • Login      │ │ • Modal      │ │ • api.ts     │ │ • authStore  │            │
│ │ • Dashboard  │ │ • Table      │ │ • authSvc    │ │              │            │
│ │ • Invoices   │ │ • Form       │ │ • invoiceSvc │ │              │            │
│ │ • Users      │ │ • Sidebar    │ │ • userSvc    │ │              │            │
│ │ • Settings   │ │ • Header     │ │              │ │              │            │
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘            │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │ REST API (HTTPS)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ BACKEND - Spring Boot (Java 21)                                                  │
│ ───────────────────────────────                                                  │
│ ┌───────────────────────────────────────────────────────────────────────────┐   │
│ │                        SPRING SECURITY + JWT FILTER                        │   │
│ └───────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                           │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│ │ Controllers  │ │  Services    │ │ Repositories │ │   Models     │            │
│ │ ──────────── │ │ ──────────── │ │ ──────────── │ │ ──────────── │            │
│ │ • AuthCtrl   │ │ • AuthSvc    │ │ • UserRepo   │ │ • User       │            │
│ │ • InvoiceCtrl│ │ • InvoiceSvc │ │ • InvoiceRepo│ │ • Invoice    │            │
│ │ • UserCtrl   │ │ • UserSvc    │ │ • OrgRepo    │ │ • Org        │            │
│ │ • OrgCtrl    │ │ • EmailSvc   │ │ • ApprovalRep│ │ • Approval   │            │
│ │ • ApprovalCtr│ │ • SageSvc    │ │ • AuditRepo  │ │ • AuditLog   │            │
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘            │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │ SQS Message
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│ PDF MICROSERVICE - Python FastAPI                                                │
│ ─────────────────────────────────                                                │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                              │
│ │  Endpoints   │ │   Services   │ │    Models    │                              │
│ │ ──────────── │ │ ──────────── │ │ ──────────── │                              │
│ │ POST /extract│ │ • PDFExtract │ │ • InvoiceData│                              │
│ │ GET  /health │ │ • OCRService │ │ • ExtractResp│                              │
│ │              │ │ • FieldParser│ │              │                              │
│ └──────────────┘ └──────────────┘ └──────────────┘                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Data Isolation Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    MULTI-TENANT DATA ISOLATION                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

  Every request is filtered by org_id to ensure complete data isolation:

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │ USER REQUEST FLOW WITH ORG ISOLATION                                        │
  │                                                                             │
  │  1. User logs in                                                            │
  │     ┌────────────────────────────────────────────────────────────────────┐ │
  │     │ POST /api/auth/login { email, password }                           │ │
  │     └────────────────────────────────────────────────────────────────────┘ │
  │                                                                             │
  │  2. JWT Token issued with org_id                                           │
  │     ┌────────────────────────────────────────────────────────────────────┐ │
  │     │ {                                                                  │ │
  │     │   "sub": "mary@longhome.com",                                      │ │
  │     │   "userId": 101,                                                   │ │
  │     │   "orgId": 1,             ← ORGANIZATION ID IN TOKEN               │ │
  │     │   "role": "APPROVER",                                              │ │
  │     │   "exp": 1738886400                                                │ │
  │     │ }                                                                  │ │
  │     └────────────────────────────────────────────────────────────────────┘ │
  │                                                                             │
  │  3. API Request with JWT                                                    │
  │     ┌────────────────────────────────────────────────────────────────────┐ │
  │     │ GET /api/invoices                                                  │ │
  │     │ Authorization: Bearer <jwt_token>                                  │ │
  │     └────────────────────────────────────────────────────────────────────┘ │
  │                                                                             │
  │  4. Backend extracts org_id and filters query                              │
  │     ┌────────────────────────────────────────────────────────────────────┐ │
  │     │ SELECT * FROM invoices WHERE org_id = 1;  ← AUTO-FILTERED          │ │
  │     │                                                                    │ │
  │     │ // User CANNOT see org_id = 2 data                                 │ │
  │     └────────────────────────────────────────────────────────────────────┘ │
  │                                                                             │
  └─────────────────────────────────────────────────────────────────────────────┘

  SUPER ADMIN EXCEPTION:
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │ Super Admin has org_id = NULL in JWT, allowing access to all data          │
  │ SELECT * FROM invoices; -- No filter for super admin                       │
  │ SELECT * FROM invoices WHERE org_id = ?; -- Can filter by specific org     │
  └─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Sequence Diagrams

### 6.1 User Authentication Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE: USER LOGIN                                          │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌──────┐          ┌──────────┐          ┌──────────┐          ┌──────────┐
  │ User │          │ Frontend │          │ Backend  │          │ Database │
  └──┬───┘          └────┬─────┘          └────┬─────┘          └────┬─────┘
     │                   │                     │                     │
     │  1. Enter email   │                     │                     │
     │     & password    │                     │                     │
     │──────────────────>│                     │                     │
     │                   │                     │                     │
     │                   │  2. POST /api/auth/login                  │
     │                   │     {email, password}                     │
     │                   │────────────────────>│                     │
     │                   │                     │                     │
     │                   │                     │  3. Find user       │
     │                   │                     │     by email        │
     │                   │                     │────────────────────>│
     │                   │                     │                     │
     │                   │                     │  4. Return user     │
     │                   │                     │     with org_id     │
     │                   │                     │<────────────────────│
     │                   │                     │                     │
     │                   │                     │  5. Verify password │
     │                   │                     │     (BCrypt)        │
     │                   │                     │                     │
     │                   │                     │  6. Generate JWT    │
     │                   │                     │     with org_id     │
     │                   │                     │                     │
     │                   │  7. Return JWT token                      │
     │                   │     + user info                           │
     │                   │<────────────────────│                     │
     │                   │                     │                     │
     │                   │  8. Store token     │                     │
     │                   │     in localStorage │                     │
     │                   │                     │                     │
     │  9. Redirect to   │                     │                     │
     │     Dashboard     │                     │                     │
     │<──────────────────│                     │                     │
     │                   │                     │                     │
```

### 6.2 Invoice Processing Flow (Email to Database)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE: EMAIL TO DATABASE                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

┌────────┐    ┌─────────┐    ┌─────────┐    ┌────────┐    ┌─────────┐    ┌──────┐
│ Vendor │    │ Outlook │    │ Spring  │    │  SQS   │    │ Python  │    │  DB  │
│        │    │  (M365) │    │  Boot   │    │ Queue  │    │ FastAPI │    │      │
└───┬────┘    └────┬────┘    └────┬────┘    └───┬────┘    └────┬────┘    └──┬───┘
    │              │              │             │              │             │
    │ 1. Send email│              │             │              │             │
    │    with PDF  │              │             │              │             │
    │─────────────>│              │             │              │             │
    │              │              │             │              │             │
    │              │  2. Poll inbox             │              │             │
    │              │     (every 5 min)          │              │             │
    │              │<─────────────│             │              │             │
    │              │              │             │              │             │
    │              │  3. Return   │             │              │             │
    │              │     new emails             │              │             │
    │              │─────────────>│             │              │             │
    │              │              │             │              │             │
    │              │              │ 4. Download │              │             │
    │              │              │    PDF to S3│              │             │
    │              │              │────────────────────────────────────────>│
    │              │              │             │              │             │
    │              │              │ 5. Create invoice record (RECEIVED)     │
    │              │              │────────────────────────────────────────>│
    │              │              │             │              │             │
    │              │              │ 6. Send to  │              │             │
    │              │              │    queue    │              │             │
    │              │              │────────────>│              │             │
    │              │              │             │              │             │
    │              │              │             │ 7. Consume   │             │
    │              │              │             │    message   │             │
    │              │              │             │<─────────────│             │
    │              │              │             │              │             │
    │              │              │             │ 8. Download  │             │
    │              │              │             │    PDF from S3             │
    │              │              │             │              │<────────────│
    │              │              │             │              │             │
    │              │              │             │ 9. Extract   │             │
    │              │              │             │    text/OCR  │             │
    │              │              │             │              │             │
    │              │              │             │ 10. Parse    │             │
    │              │              │             │     fields   │             │
    │              │              │             │              │             │
    │              │              │             │ 11. Return   │             │
    │              │              │             │     extracted│             │
    │              │              │<────────────│     data     │             │
    │              │              │             │              │             │
    │              │              │ 12. Update invoice (PROCESSED)          │
    │              │              │────────────────────────────────────────>│
    │              │              │             │              │             │
```

### 6.3 Invoice Approval Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE: INVOICE APPROVAL                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Approver │    │ Frontend │    │ Backend  │    │ Database │    │   Sage   │
└────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │               │               │
     │ 1. Click      │               │               │               │
     │    Approve    │               │               │               │
     │──────────────>│               │               │               │
     │               │               │               │               │
     │               │ 2. POST /api/invoices/{id}/approve            │
     │               │    {comments}                │               │
     │               │──────────────>│               │               │
     │               │               │               │               │
     │               │               │ 3. Verify     │               │
     │               │               │    user role  │               │
     │               │               │    & org_id   │               │
     │               │               │               │               │
     │               │               │ 4. Update     │               │
     │               │               │    invoice    │               │
     │               │               │    status =   │               │
     │               │               │    APPROVED   │               │
     │               │               │──────────────>│               │
     │               │               │               │               │
     │               │               │ 5. Create     │               │
     │               │               │    approval   │               │
     │               │               │    record     │               │
     │               │               │──────────────>│               │
     │               │               │               │               │
     │               │               │ 6. Create     │               │
     │               │               │    audit log  │               │
     │               │               │──────────────>│               │
     │               │               │               │               │
     │               │               │ 7. Sync to    │               │
     │               │               │    Sage       │               │
     │               │               │──────────────────────────────>│
     │               │               │               │               │
     │               │               │ 8. Update     │               │
     │               │               │    sage_status│               │
     │               │               │    = SYNCED   │               │
     │               │               │──────────────>│               │
     │               │               │               │               │
     │               │ 9. Return success             │               │
     │               │<──────────────│               │               │
     │               │               │               │               │
     │ 10. Show      │               │               │               │
     │     success   │               │               │               │
     │<──────────────│               │               │               │
     │               │               │               │               │
```

### 6.4 Super Admin Creates Organization

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SEQUENCE: CREATE ORGANIZATION                                 │
└─────────────────────────────────────────────────────────────────────────────────┘

┌───────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌─────┐   ┌─────┐
│SuperAdmin │   │ Frontend │   │ Backend  │   │ Database │   │ S3  │   │ SQS │
└─────┬─────┘   └────┬─────┘   └────┬─────┘   └────┬─────┘   └──┬──┘   └──┬──┘
      │              │              │              │             │         │
      │ 1. Fill org  │              │              │             │         │
      │    form      │              │              │             │         │
      │─────────────>│              │              │             │         │
      │              │              │              │             │         │
      │              │ 2. POST /api/super-admin/organizations    │         │
      │              │    {name, slug, email, admin}             │         │
      │              │─────────────>│              │             │         │
      │              │              │              │             │         │
      │              │              │ 3. Verify    │             │         │
      │              │              │    SUPER_ADMIN             │         │
      │              │              │    role      │             │         │
      │              │              │              │             │         │
      │              │              │ 4. Create    │             │         │
      │              │              │    org record│             │         │
      │              │              │─────────────>│             │         │
      │              │              │              │             │         │
      │              │              │ 5. Create S3 folder        │         │
      │              │              │────────────────────────────>│         │
      │              │              │              │             │         │
      │              │              │ 6. Create SQS queue        │         │
      │              │              │──────────────────────────────────────>│
      │              │              │              │             │         │
      │              │              │ 7. Create    │             │         │
      │              │              │    admin user│             │         │
      │              │              │    (org_id)  │             │         │
      │              │              │─────────────>│             │         │
      │              │              │              │             │         │
      │              │              │ 8. Send welcome email      │         │
      │              │              │              │             │         │
      │              │ 9. Return created org       │             │         │
      │              │<─────────────│              │             │         │
      │              │              │              │             │         │
      │ 10. Show     │              │              │             │         │
      │     success  │              │              │             │         │
      │<─────────────│              │              │             │         │
      │              │              │              │             │         │
```

---

## 7. Data Flow Diagrams

### 7.1 Level 0 - Context Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    CONTEXT DIAGRAM (DFD Level 0)                                 │
└─────────────────────────────────────────────────────────────────────────────────┘

                              ┌───────────────┐
                              │    VENDOR     │
                              └───────┬───────┘
                                      │
                            Invoice PDF via Email
                                      │
                                      ▼
     ┌───────────────┐      ┌─────────────────────┐      ┌───────────────┐
     │   MICROSOFT   │─────>│                     │<─────│     USER      │
     │     365       │      │     SYNCLEDGER      │      │   (Portal)    │
     │   (Outlook)   │<─────│      SYSTEM         │─────>│               │
     └───────────────┘      │                     │      └───────────────┘
                            └──────────┬──────────┘
                                       │
                              Approved Invoices
                                       │
                                       ▼
                            ┌───────────────┐
                            │     SAGE      │
                            │  (Accounting) │
                            └───────────────┘
```

### 7.2 Level 1 - System Processes

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DATA FLOW DIAGRAM (DFD Level 1)                               │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐                                              ┌──────────────┐
│   OUTLOOK    │                                              │    SAGE      │
└──────┬───────┘                                              └──────▲───────┘
       │                                                             │
       │ Emails with PDF                              Approved Invoice
       │                                                             │
       ▼                                                             │
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌─────────────┐
│              │      │              │      │              │      │             │
│  1.0 EMAIL   │─────>│  2.0 PDF     │─────>│  3.0 INVOICE │─────>│  4.0 SAGE   │
│  PROCESSOR   │      │  EXTRACTOR   │      │  MANAGER     │      │  SYNC       │
│              │ PDF  │              │ Data │              │      │             │
└──────────────┘      └──────────────┘      └──────┬───────┘      └─────────────┘
       │                     │                     │
       │                     │                     │
       ▼                     ▼                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                              DATA STORES                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  │
│  │  D1: S3      │   │  D2: Users   │   │ D3: Invoices │   │ D4: Audit    │  │
│  │  (PDF Files) │   │              │   │              │   │    Logs      │  │
│  └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
                                   ▲
                                   │
                            ┌──────┴───────┐
                            │     USER     │
                            │   (Portal)   │
                            └──────────────┘

PROCESS DESCRIPTIONS:
═════════════════════

1.0 EMAIL PROCESSOR
  - Polls Outlook inbox via MS Graph API
  - Downloads PDF attachments
  - Stores PDFs in S3
  - Creates initial invoice record

2.0 PDF EXTRACTOR
  - Reads PDF from S3
  - Extracts text (OCR if needed)
  - Parses invoice fields
  - Returns structured data

3.0 INVOICE MANAGER
  - Stores extracted data
  - Manages approval workflow
  - Tracks status changes
  - Logs all actions

4.0 SAGE SYNC
  - Sends approved invoices
  - Updates sync status
  - Handles retries on failure
```

### 7.3 Invoice Status Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    INVOICE STATUS STATE DIAGRAM                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

                            ┌─────────────────┐
                            │    RECEIVED     │
                            │ (Email arrived) │
                            └────────┬────────┘
                                     │
                            PDF extraction started
                                     │
                        ┌────────────┴────────────┐
                        │                         │
                        ▼                         ▼
               ┌─────────────────┐       ┌─────────────────┐
               │   PROCESSING    │       │     FAILED      │
               │ (Extracting...) │       │ (Extraction err)│
               └────────┬────────┘       └────────┬────────┘
                        │                         │
                   Extraction OK                  │
                        │                         │
                        ▼                         │
               ┌─────────────────┐                │
               │    PROCESSED    │                │
               │ (Data extracted)│                │
               └────────┬────────┘                │
                        │                         │
                  Manual review                   │
                        │                         │
                        ▼                         │
               ┌─────────────────┐                │
               │ PENDING_REVIEW  │◄───────────────┘
               │(Needs human fix)│        (Retry after fix)
               └────────┬────────┘
                        │
                  Submit for approval
                        │
                        ▼
               ┌─────────────────┐
               │PENDING_APPROVAL │
               │ (Waiting...)    │
               └────────┬────────┘
                        │
          ┌─────────────┴─────────────┐
          │                           │
          ▼                           ▼
  ┌─────────────────┐        ┌─────────────────┐
  │    APPROVED     │        │    REJECTED     │
  └────────┬────────┘        └─────────────────┘
           │
      Sage sync
           │
      ┌────┴────┐
      │         │
      ▼         ▼
  ┌───────┐  ┌────────┐
  │SYNCED │  │SYNC_   │
  │       │  │FAILED  │
  └───────┘  └────────┘
```

---

## 8. Database Design

### 8.1 Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    ENTITY RELATIONSHIP DIAGRAM                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
  │  organizations  │       │     users       │       │    invoices     │
  ├─────────────────┤       ├─────────────────┤       ├─────────────────┤
  │ PK id           │       │ PK id           │       │ PK id           │
  │    name         │       │ FK org_id    ───┼──────>│ FK org_id    ───┼───┐
  │    slug         │<──────┼─── org_id       │   ┌──>│    invoice_no   │   │
  │    email_addr   │       │    email        │   │   │    vendor_name  │   │
  │    status       │       │    password     │   │   │    amount       │   │
  │    created_at   │       │    first_name   │   │   │    status       │   │
  │    sage_config  │       │    last_name    │   │   │    pdf_path     │   │
  └─────────────────┘       │    role         │   │   │    created_at   │   │
                            │    status       │   │   └─────────────────┘   │
                            │    created_at   │   │                         │
                            └─────────────────┘   │   ┌─────────────────┐   │
                                    │             │   │   approvals     │   │
                                    │             │   ├─────────────────┤   │
                                    │             │   │ PK id           │   │
                                    │             │   │ FK invoice_id ──┼───┘
                                    │             │   │ FK user_id   ───┼───┐
                                    └─────────────┼───┼─── user_id      │   │
                                                  │   │    action       │   │
                                                  │   │    comments     │   │
                                                  │   │    created_at   │   │
                                                  │   └─────────────────┘   │
                                                  │                         │
                                                  │   ┌─────────────────┐   │
                                                  │   │   audit_logs    │   │
                                                  │   ├─────────────────┤   │
                                                  │   │ PK id           │   │
                                                  │   │ FK org_id       │   │
                                                  │   │ FK user_id   ───┼───┘
                                                  └───┼─── org_id       │
                                                      │    action       │
                                                      │    entity_type  │
                                                      │    entity_id    │
                                                      │    created_at   │
                                                      └─────────────────┘
```

### 8.2 Table Definitions

```sql
-- ============================================================================
-- ORGANIZATIONS TABLE
-- ============================================================================
CREATE TABLE organizations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) UNIQUE NOT NULL,
    email_address   VARCHAR(255) UNIQUE NOT NULL,
    status          VARCHAR(50) DEFAULT 'ACTIVE',
    contact_name    VARCHAR(255),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(50),
    sage_endpoint   VARCHAR(500),
    sage_api_key    VARCHAR(500),  -- Encrypted
    s3_folder       VARCHAR(500),
    sqs_queue       VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_at      TIMESTAMP
);

-- ============================================================================
-- USERS TABLE
-- ============================================================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    org_id          BIGINT REFERENCES organizations(id),  -- NULL for SUPER_ADMIN
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(50) NOT NULL,  -- SUPER_ADMIN, ADMIN, APPROVER, VIEWER
    status          VARCHAR(50) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT REFERENCES users(id),
    last_login_at   TIMESTAMP,
    
    CONSTRAINT chk_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')),
    CONSTRAINT chk_super_admin CHECK (
        (role = 'SUPER_ADMIN' AND org_id IS NULL) OR
        (role != 'SUPER_ADMIN' AND org_id IS NOT NULL)
    )
);

-- ============================================================================
-- INVOICES TABLE
-- ============================================================================
CREATE TABLE invoices (
    id              BIGSERIAL PRIMARY KEY,
    org_id          BIGINT NOT NULL REFERENCES organizations(id),
    invoice_number  VARCHAR(100),
    vendor_name     VARCHAR(255),
    customer_name   VARCHAR(255),
    customer_id     VARCHAR(100),
    opportunity_no  VARCHAR(100),
    amount          DECIMAL(15,2),
    currency        VARCHAR(10) DEFAULT 'GBP',
    invoice_date    DATE,
    due_date        DATE,
    status          VARCHAR(50) DEFAULT 'RECEIVED',
    pdf_path        VARCHAR(500),
    email_subject   VARCHAR(500),
    email_from      VARCHAR(255),
    email_received  TIMESTAMP,
    extraction_conf DECIMAL(5,2),  -- AI confidence score
    sage_sync_id    VARCHAR(100),
    sage_status     VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN (
        'RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED',
        'PENDING_REVIEW', 'PENDING_APPROVAL', 'APPROVED',
        'REJECTED', 'SYNCED', 'SYNC_FAILED'
    ))
);

-- ============================================================================
-- APPROVALS TABLE
-- ============================================================================
CREATE TABLE approvals (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoices(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    org_id          BIGINT NOT NULL REFERENCES organizations(id),
    action          VARCHAR(50) NOT NULL,  -- APPROVED, REJECTED
    comments        TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- AUDIT LOGS TABLE
-- ============================================================================
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    org_id          BIGINT REFERENCES organizations(id),  -- NULL for platform events
    user_id         BIGINT REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(50),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================
CREATE INDEX idx_users_org ON users(org_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_invoices_org ON invoices(org_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_org_status ON invoices(org_id, status);
CREATE INDEX idx_approvals_invoice ON approvals(invoice_id);
CREATE INDEX idx_audit_org ON audit_logs(org_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
```

---

# PART C: THIRD-PARTY SETUP GUIDES

> **📋 This section provides step-by-step instructions to set up all external services from scratch.**

---

## 9. AWS Account Setup

### 9.1 Create AWS Account

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 1: CREATE AWS ACCOUNT                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: https://aws.amazon.com/
2. Click "Create an AWS Account"
3. Enter email and account name (e.g., "SyncLedger Production")
4. Verify email
5. Enter payment information (credit card required)
6. Select Support Plan: "Basic" (Free)
7. Complete sign-up

⚠️ IMPORTANT: Enable MFA (Multi-Factor Authentication) immediately!
   - Go to IAM → Users → Your user → Security credentials → MFA
```

### 9.2 Create IAM User for Development

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 2: CREATE IAM USER                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: AWS Console → IAM → Users → Add Users

2. User details:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ User name: syncledger-dev                                                  │
   │ Access type: ☑ Programmatic access                                         │
   │              ☑ AWS Management Console access                               │
   └────────────────────────────────────────────────────────────────────────────┘

3. Permissions - Attach policies directly:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ ☑ AmazonS3FullAccess                                                       │
   │ ☑ AmazonSQSFullAccess                                                      │
   │ ☑ AmazonRDSFullAccess                                                      │
   │ ☑ AmazonEC2ContainerRegistryFullAccess                                     │
   │ ☑ AmazonECS_FullAccess                                                     │
   └────────────────────────────────────────────────────────────────────────────┘

4. Download credentials CSV (contains Access Key ID and Secret Access Key)
   ⚠️ STORE SECURELY - You won't be able to see the secret key again!
```

### 9.3 Create S3 Bucket for PDF Storage

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 3: CREATE S3 BUCKET                                      │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: AWS Console → S3 → Create Bucket

2. Configuration:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Bucket name:     syncledger-documents                                      │
   │ AWS Region:      eu-west-2 (London)                                        │
   │                                                                            │
   │ Block Public Access settings:                                              │
   │ ☑ Block all public access (KEEP THIS CHECKED!)                            │
   │                                                                            │
   │ Bucket Versioning: Enable                                                  │
   │ Default encryption: Amazon S3-managed keys (SSE-S3)                        │
   └────────────────────────────────────────────────────────────────────────────┘

3. Create folder structure:
   syncledger-documents/
   ├── longhome/           # Organization 1
   │   ├── inbox/          # Raw PDFs from email
   │   └── processed/      # Processed PDFs
   ├── evolotek/           # Organization 2
   │   ├── inbox/
   │   └── processed/
   └── acme/               # Organization 3
       ├── inbox/
       └── processed/
```

### 9.4 Create SQS Queues

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 4: CREATE SQS QUEUES                                     │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: AWS Console → SQS → Create Queue

2. Create queue: pdf-processing-queue
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Type: Standard                                                             │
   │ Name: syncledger-pdf-processing                                            │
   │                                                                            │
   │ Configuration:                                                             │
   │   Visibility timeout: 300 seconds (5 min)                                 │
   │   Message retention: 4 days                                               │
   │   Delivery delay: 0 seconds                                               │
   │   Maximum message size: 256 KB                                            │
   │   Receive message wait time: 20 seconds (long polling)                    │
   │                                                                            │
   │ Dead-letter queue:                                                         │
   │   Enable: Yes                                                             │
   │   Queue: syncledger-pdf-processing-dlq (create this first)                │
   │   Max receives: 3                                                         │
   └────────────────────────────────────────────────────────────────────────────┘

3. Note the Queue URL:
   https://sqs.eu-west-2.amazonaws.com/123456789012/syncledger-pdf-processing
```

### 9.5 Create RDS PostgreSQL Database

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 5: CREATE RDS DATABASE                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: AWS Console → RDS → Create Database

2. Configuration:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Engine: PostgreSQL                                                         │
   │ Version: 16.x                                                              │
   │ Template: Free tier (for dev) / Production (for prod)                     │
   │                                                                            │
   │ Settings:                                                                  │
   │   DB instance identifier: syncledger-db                                   │
   │   Master username: syncledger_admin                                       │
   │   Master password: [GENERATE STRONG PASSWORD]                             │
   │                                                                            │
   │ Instance:                                                                  │
   │   DB instance class: db.t3.micro (dev) / db.t3.small (prod)              │
   │   Storage: 20 GB (auto-scaling enabled)                                   │
   │                                                                            │
   │ Connectivity:                                                              │
   │   VPC: Default VPC                                                        │
   │   Public access: No (for production) / Yes (for dev)                      │
   │   Security group: Create new                                              │
   │                                                                            │
   │ Database name: syncledger                                                 │
   └────────────────────────────────────────────────────────────────────────────┘

3. Note the Endpoint:
   syncledger-db.xxxxx.eu-west-2.rds.amazonaws.com:5432
```

### 9.6 AWS Credentials Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    AWS CREDENTIALS TO SAVE                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

Save these in a secure password manager or AWS Secrets Manager:

┌────────────────────────────────────────────────────────────────────────────────┐
│ AWS_ACCESS_KEY_ID=AKIA...                                                     │
│ AWS_SECRET_ACCESS_KEY=...                                                     │
│ AWS_REGION=eu-west-2                                                          │
│                                                                               │
│ S3_BUCKET=syncledger-documents                                               │
│                                                                               │
│ SQS_QUEUE_URL=https://sqs.eu-west-2.amazonaws.com/123.../syncledger-pdf...   │
│                                                                               │
│ RDS_HOST=syncledger-db.xxxxx.eu-west-2.rds.amazonaws.com                     │
│ RDS_PORT=5432                                                                │
│ RDS_DATABASE=syncledger                                                      │
│ RDS_USERNAME=syncledger_admin                                                │
│ RDS_PASSWORD=[YOUR_PASSWORD]                                                 │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Microsoft 365 & Graph API Setup

### 10.1 Prerequisites

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PREREQUISITES                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘

Required:
✅ Microsoft 365 Business subscription (or higher)
✅ Azure AD admin access (or request from IT admin)
✅ Dedicated email addresses for each organization:
   - longhome@nevorix.co
   - evolotek@nevorix.co
   - acme@nevorix.co
```

### 10.2 Register App in Azure Active Directory

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 1: REGISTER APP IN AZURE AD                              │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: https://portal.azure.com
2. Search for "App registrations" → Click "New registration"

3. Fill in:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Name: SyncLedger Email Integration                                         │
   │                                                                            │
   │ Supported account types:                                                   │
   │ ○ Accounts in this organizational directory only (Single tenant) ← SELECT │
   │                                                                            │
   │ Redirect URI: (leave blank for now)                                        │
   └────────────────────────────────────────────────────────────────────────────┘

4. Click "Register"

5. COPY THESE VALUES (you'll need them):
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Application (client) ID:  a1b2c3d4-e5f6-7890-abcd-ef1234567890           │
   │ Directory (tenant) ID:    11111111-2222-3333-4444-555555555555           │
   └────────────────────────────────────────────────────────────────────────────┘
```

### 10.3 Create Client Secret

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 2: CREATE CLIENT SECRET                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

1. In your app registration → "Certificates & secrets"
2. Click "+ New client secret"

3. Fill in:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Description: SyncLedger Production                                         │
   │ Expires: 24 months                                                         │
   └────────────────────────────────────────────────────────────────────────────┘

4. Click "Add"

5. ⚠️ IMMEDIATELY COPY THE VALUE:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Secret Value: abc123xyz789... (COPY NOW - only shown once!)               │
   └────────────────────────────────────────────────────────────────────────────┘
```

### 10.4 Configure API Permissions

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 3: CONFIGURE API PERMISSIONS                             │
└─────────────────────────────────────────────────────────────────────────────────┘

1. In your app registration → "API permissions"
2. Click "Add a permission" → "Microsoft Graph" → "Application permissions"

3. Add these permissions:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ ☑ Mail.Read          - Read mail in all mailboxes                         │
   │ ☑ Mail.ReadWrite     - Read and write mail (to mark as read)              │
   │ ☑ User.Read.All      - Read all users' profiles                           │
   └────────────────────────────────────────────────────────────────────────────┘

4. Click "Grant admin consent for [Your Organization]"
   
5. Verify all permissions show ✅ Granted
```

### 10.5 Create Dedicated Mailboxes

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    STEP 4: CREATE MAILBOXES                                      │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Go to: Microsoft 365 Admin Center → Users → Active Users → Add User

2. Create mailbox for each organization:

   Organization 1 - Longhome:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Display name: Longhome Invoices                                            │
   │ Username: longhome@nevorix.co                                              │
   │ License: Exchange Online (or M365 Business Basic)                          │
   └────────────────────────────────────────────────────────────────────────────┘

   Organization 2 - Evolotek:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Display name: Evolotek Invoices                                            │
   │ Username: evolotek@nevorix.co                                              │
   │ License: Exchange Online                                                   │
   └────────────────────────────────────────────────────────────────────────────┘

   Organization 3 - Acme:
   ┌────────────────────────────────────────────────────────────────────────────┐
   │ Display name: Acme Invoices                                                │
   │ Username: acme@nevorix.co                                                  │
   │ License: Exchange Online                                                   │
   └────────────────────────────────────────────────────────────────────────────┘
```

### 10.6 Microsoft Graph Credentials Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    MICROSOFT GRAPH CREDENTIALS TO SAVE                           │
└─────────────────────────────────────────────────────────────────────────────────┘

Save these in your configuration:

┌────────────────────────────────────────────────────────────────────────────────┐
│ MICROSOFT_TENANT_ID=11111111-2222-3333-4444-555555555555                      │
│ MICROSOFT_CLIENT_ID=a1b2c3d4-e5f6-7890-abcd-ef1234567890                     │
│ MICROSOFT_CLIENT_SECRET=abc123xyz789...                                       │
│                                                                               │
│ # Organization email addresses:                                               │
│ # These are stored in the organizations table in the database                 │
│ # longhome@nevorix.co                                                         │
│ # evolotek@nevorix.co                                                         │
│ # acme@nevorix.co                                                             │
└────────────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Generic ERP Integration System

SyncLedger supports **any ERP system** through a generic, plugin-based connector architecture.
Each ERP type defines its own required properties (credentials and settings), and the frontend
renders a dynamic configuration form based on the property schema returned by the API.

### 11.1 Supported ERP Types

| Type        | Display Name       | Status        | Auth Model                     |
|-------------|--------------------|---------------|--------------------------------|
| SAGE        | Sage Intacct       | Implemented   | XML Web Services (Sender+Login)|
| QUICKBOOKS  | QuickBooks         | Schema ready  | OAuth2 REST                    |
| NETSUITE    | Oracle NetSuite    | Schema ready  | Token-Based Auth REST          |
| ORACLE      | Oracle Fusion Cloud| Schema ready  | Basic Auth REST                |
| SAP         | SAP S/4HANA / B1   | Schema ready  | OAuth2 / Service Layer         |
| XERO        | Xero               | Schema ready  | OAuth2 REST                    |
| CUSTOM      | Custom API         | Schema ready  | Configurable                   |

### 11.2 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    GENERIC ERP CONNECTOR ARCHITECTURE                             │
└─────────────────────────────────────────────────────────────────────────────────┘

  Frontend                          Backend
  ─────────                         ─────────
  1. GET /erp-types            →  Returns all ERP types + implemented flag
  2. GET /erp-types/SAGE/      →  Returns property schema (labels, types,
     properties                   required flags, help text)
  3. UI renders dynamic form
  4. PUT /erp-config           →  { erpType: "SAGE", properties: { ... } }
     (user fills in fields)       Saves to erp_properties table (secrets encrypted)
  5. POST /erp-config/verify   →  ErpConnectorFactory resolves SageIntacctService
                                  → testConnection(decryptedProps) → result

  ┌──────────────┐    ┌────────────────────┐    ┌──────────────────┐
  │ ErpConnector  │◄───│ ErpConnectorFactory │───►│ ErpPropertyService│
  │  (interface)  │    │ (auto-discovers     │    │ (CRUD + encrypt/ │
  │               │    │  all @Service beans) │    │  decrypt + mask)  │
  └──────┬────────┘    └────────────────────┘    └──────────────────┘
         │
    ┌────┴────────────────────────────────┐
    │     │           │          │         │
  Sage  QuickBooks  NetSuite  Oracle  Custom
  (impl) (stub)     (stub)   (stub)  (stub)
```

**Key classes:**
- `ErpConnector` — Interface: `getErpType()`, `testConnection(Map)`, `createBill(Invoice, Map)`
- `ErpConnectorFactory` — Spring DI auto-discovers all `ErpConnector` beans
- `ErpPropertyDefinitions` — Static schema: what properties each ERP type requires
- `ErpPropertyService` — CRUD for `erp_properties` table with AES-256-GCM encryption
- `ErpSyncResult` — Generic result record (replaces old SageResponse)

### 11.3 Property Storage

ERP properties are stored in the `erp_properties` table as generic key-value pairs:

```sql
CREATE TABLE erp_properties (
    id                 BIGSERIAL PRIMARY KEY,
    organization_id    BIGINT NOT NULL REFERENCES organizations(id),
    erp_type           VARCHAR(50) NOT NULL,
    property_key       VARCHAR(100) NOT NULL,
    property_value     TEXT,
    is_encrypted       BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP DEFAULT NOW(),
    updated_at         TIMESTAMP DEFAULT NOW(),
    UNIQUE (organization_id, erp_type, property_key)
);
```

Secret properties (passwords, tokens) are encrypted with AES-256-GCM at rest and masked in API responses.

### 11.4 API Endpoints

#### Discover available ERP types
```
GET /api/v1/organization-settings/erp-types

Response:
[
  {
    "type": "SAGE",
    "displayName": "Sage Intacct",
    "description": "Sage Intacct cloud ERP",
    "implemented": true,
    "propertyCount": 8
  },
  {
    "type": "QUICKBOOKS",
    "displayName": "QuickBooks",
    "description": "Intuit QuickBooks",
    "implemented": false,
    "propertyCount": 6
  },
  ...
]
```

#### Get property schema for a type (for dynamic form rendering)
```
GET /api/v1/organization-settings/erp-types/SAGE/properties

Response:
[
  {
    "key": "sender_id",
    "label": "Sender ID",
    "helpText": "Web Services developer Sender ID...",
    "type": "text",
    "required": true,
    "secret": false,
    "defaultValue": null,
    "displayOrder": 1
  },
  {
    "key": "sender_password",
    "label": "Sender Password",
    "type": "password",
    "required": true,
    "secret": true,
    ...
  },
  ...
]
```

#### Get current ERP config (secrets masked)
```
GET /api/v1/organization-settings/erp-config

Response:
{
  "erpType": "SAGE",
  "erpTypeDisplayName": "Sage Intacct",
  "erpConfigured": true,
  "properties": {
    "sender_id": "MyApp",
    "sender_password": "My***",
    "company_id": "ACME_CORP",
    "user_id": "ws_user",
    "user_password": "pa***",
    "gateway_url": "https://api.intacct.com/ia/xml/xmlgw.phtml",
    "auto_sync": "true"
  },
  "propertyDefinitions": [ ... ]
}
```

#### Update ERP config (generic properties)
```
PUT /api/v1/organization-settings/erp-config
{
  "erpType": "SAGE",
  "properties": {
    "sender_id": "MyApp",
    "sender_password": "MySecretPass",
    "company_id": "ACME_CORP",
    "user_id": "ws_user",
    "user_password": "User1234!",
    "gateway_url": "https://api.intacct.com/ia/xml/xmlgw.phtml",
    "auto_sync": "true"
  }
}
```

#### Verify ERP connection
```
POST /api/v1/organization-settings/erp-config/verify
```

### 11.5 Sage Intacct — Detailed Setup

Sage Intacct uses XML Web Services with two-layer authentication:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  LAYER 1 — SENDER (Application Level)                                        │
│  ─────────────────────────────────────                                        │
│  sender_id / sender_password — Identifies your application to the gateway.   │
│  Register at https://developer.intacct.com                                   │
│  Can be set globally via SAGE_SENDER_ID / SAGE_SENDER_PASSWORD env vars,     │
│  or per-org in properties.                                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│  LAYER 2 — LOGIN (Per-Organization)                                           │
│  ──────────────────────────────────                                           │
│  user_id / company_id / user_password — Identifies the specific user +       │
│  company. Customer creates WS user in Sage Intacct and authorizes your       │
│  Sender ID in Company → Admin → Web Services Authorizations.                 │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 11.6 Adding a New ERP Connector

To add support for a new ERP system:

1. **Define properties** in `ErpPropertyDefinitions.java`:
   ```java
   DEFINITIONS.put(ErpType.MY_ERP, List.of(
       new PropertyDef("api_key", "API Key", "Your API key", "password", true, true, null, 1),
       new PropertyDef("base_url", "Base URL", "API base URL", "url", true, false, null, 2),
       new PropertyDef("auto_sync", "Auto-Sync", "...", "select", false, false, "true", 3)
   ));
   ```

2. **Add enum value** to `ErpType.java`:
   ```java
   MY_ERP("My ERP", "My ERP cloud platform")
   ```

3. **Implement the connector**:
   ```java
   @Service
   public class MyErpService implements ErpConnector {
       @Override public ErpType getErpType() { return ErpType.MY_ERP; }
       @Override public ErpSyncResult testConnection(Map<String, String> props) { ... }
       @Override public ErpSyncResult createBill(Invoice invoice, Map<String, String> props) { ... }
   }
   ```

4. That's it — `ErpConnectorFactory` auto-discovers the new `@Service` bean via Spring DI.

### 11.7 Environment Variables Reference

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│              ERP — ENVIRONMENT VARIABLES                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Global (Sage Intacct application-level):                                        │
│  ──────────────────────────────────────                                           │
│  SAGE_SENDER_ID              App sender ID (shared across all orgs)             │
│  SAGE_SENDER_PASSWORD        App sender password                                 │
│                                                                                  │
│  Per-org seed (LongHome example):                                                │
│  ────────────────────────────────                                                │
│  SEED_LONGHOME_ERP_COMPANY_ID       Sage Intacct company ID                     │
│  SEED_LONGHOME_ERP_USER_ID          Web Services user ID                         │
│  SEED_LONGHOME_ERP_PASSWORD          Web Services user password                  │
│  SEED_LONGHOME_ERP_SENDER_ID         Sender ID (overrides global for this org)  │
│  SEED_LONGHOME_ERP_SENDER_PASSWORD   Sender password (overrides global)         │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

# PART D: DEVELOPMENT GUIDE

---

## 12. Development Environment Setup

### 12.1 Required Software

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    INSTALL THESE IN ORDER                                        │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Java JDK 21
   Download: https://adoptium.net/
   Verify: java --version
   
2. Maven 3.9+
   Download: https://maven.apache.org/download.cgi
   Verify: mvn --version

3. Python 3.12+
   Download: https://python.org/downloads/
   Verify: python --version

4. Node.js 20+
   Download: https://nodejs.org/
   Verify: node --version

5. PostgreSQL 16+
   Download: https://postgresql.org/download/
   OR use Docker: docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16

6. Docker Desktop
   Download: https://docker.com/products/docker-desktop
   Verify: docker --version

7. Git
   Download: https://git-scm.com/downloads
   Verify: git --version

8. VS Code (Recommended)
   Download: https://code.visualstudio.com/
   Extensions:
   - Java Extension Pack
   - Python
   - ES7+ React/Redux/React-Native snippets
   - Tailwind CSS IntelliSense
```

### 12.2 Project Structure

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PROJECT STRUCTURE                                             │
└─────────────────────────────────────────────────────────────────────────────────┘

SyncLedger/
│
├── syncledger-backend/          # Java Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vedvix/syncledger/
│   │   │   │   ├── config/          # Security, AWS configs
│   │   │   │   ├── controller/      # REST endpoints
│   │   │   │   ├── dto/             # Data transfer objects
│   │   │   │   ├── exception/       # Custom exceptions
│   │   │   │   ├── model/           # JPA entities
│   │   │   │   ├── repository/      # Database repos
│   │   │   │   └── service/         # Business logic
│   │   │   └── resources/
│   │   │       └── application.yml  # Configuration
│   │   └── test/
│   └── pom.xml
│
├── pdf-microservice/            # Python FastAPI
│   ├── main.py                  # Entry point
│   ├── requirements.txt
│   ├── models/
│   │   └── invoice_data.py
│   ├── services/
│   │   ├── pdf_extractor.py
│   │   ├── ocr_service.py
│   │   └── field_parser.py
│   └── tests/
│
├── frontend/                    # React + TypeScript
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── store/
│   │   ├── types/
│   │   └── App.tsx
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml           # Local development
└── docs/
    └── SyncLedger_Documentation_v3.md
```

### 12.3 Clone and Initial Setup

```bash
# Clone repository
git clone https://github.com/vedvix/syncledger.git
cd syncledger

# Create environment file
cp .env.example .env
# Edit .env with your credentials from previous sections
```

### 12.4 Environment Variables (.env)

```bash
# =============================================================================
# DATABASE
# =============================================================================
DB_HOST=localhost
DB_PORT=5432
DB_NAME=syncledger
DB_USERNAME=postgres
DB_PASSWORD=postgres

# =============================================================================
# AWS
# =============================================================================
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=eu-west-2
S3_BUCKET=syncledger-documents
SQS_QUEUE_URL=https://sqs.eu-west-2.amazonaws.com/xxx/syncledger-pdf-processing

# =============================================================================
# MICROSOFT GRAPH
# =============================================================================
MICROSOFT_TENANT_ID=your_tenant_id
MICROSOFT_CLIENT_ID=your_client_id
MICROSOFT_CLIENT_SECRET=your_client_secret

# =============================================================================
# JWT
# =============================================================================
JWT_SECRET=your-256-bit-secret-key-here-make-it-long-and-random
JWT_EXPIRATION=86400000

# =============================================================================
# SAGE (Optional - can be configured per organization)
# =============================================================================
SAGE_CLIENT_ID=your_sage_client_id
SAGE_CLIENT_SECRET=your_sage_client_secret
```

---

## 13. Backend Development (Spring Boot)

### 13.1 Start Backend

```bash
cd syncledger-backend

# Install dependencies and run
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run

# Backend runs on: http://localhost:8080
```

### 13.2 Key Configuration (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION}

# AWS Configuration
aws:
  region: ${AWS_REGION}
  s3:
    bucket: ${S3_BUCKET}
  sqs:
    queue-url: ${SQS_QUEUE_URL}

# Microsoft Graph Configuration
microsoft:
  graph:
    tenant-id: ${MICROSOFT_TENANT_ID}
    client-id: ${MICROSOFT_CLIENT_ID}
    client-secret: ${MICROSOFT_CLIENT_SECRET}
    poll-interval: 300000  # 5 minutes
```

### 13.3 Key API Endpoints

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    BACKEND API ENDPOINTS                                         │
└─────────────────────────────────────────────────────────────────────────────────┘

AUTHENTICATION
──────────────
POST   /api/auth/login              # Login, returns JWT
POST   /api/auth/refresh            # Refresh token
POST   /api/auth/logout             # Logout

INVOICES (Org-filtered)
──────────────
GET    /api/invoices                # List invoices
GET    /api/invoices/{id}           # Get invoice details
PUT    /api/invoices/{id}           # Update invoice
POST   /api/invoices/{id}/approve   # Approve invoice
POST   /api/invoices/{id}/reject    # Reject invoice

USERS (Org-filtered)
──────────────
GET    /api/users                   # List users
POST   /api/users                   # Create user
PUT    /api/users/{id}              # Update user
DELETE /api/users/{id}              # Disable user

DASHBOARD
──────────────
GET    /api/dashboard/stats         # Get dashboard statistics

SUPER ADMIN ONLY
──────────────
GET    /api/super-admin/organizations       # List all orgs
POST   /api/super-admin/organizations       # Create org
PUT    /api/super-admin/organizations/{id}  # Update org
GET    /api/super-admin/users               # List all users
GET    /api/super-admin/invoices            # List all invoices
GET    /api/super-admin/reports             # Platform reports
```

---

## 14. PDF Microservice (Python)

### 14.1 Start PDF Service

```bash
cd pdf-microservice

# Create virtual environment
python -m venv venv

# Activate (Windows)
venv\Scripts\activate

# Activate (Mac/Linux)
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Install Tesseract OCR (required)
# Windows: Download from https://github.com/UB-Mannheim/tesseract/wiki
# Mac: brew install tesseract
# Linux: sudo apt-get install tesseract-ocr

# Run service
uvicorn main:app --reload --port 8000

# Service runs on: http://localhost:8000
```

### 14.2 PDF Service Endpoints

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PDF SERVICE ENDPOINTS                                         │
└─────────────────────────────────────────────────────────────────────────────────┘

POST /extract
  Request:
  {
    "pdf_url": "s3://bucket/path/to/invoice.pdf",
    "org_id": 1
  }
  
  Response:
  {
    "success": true,
    "confidence": 0.92,
    "data": {
      "invoice_number": "INV-2026-001",
      "vendor_name": "Acme Supplies",
      "amount": 5000.00,
      "invoice_date": "2026-02-07",
      "customer_name": "Longhome Ltd"
    }
  }

GET /health
  Response: { "status": "healthy" }
```

---

## 15. Frontend Development (React)

### 15.1 Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Frontend runs on: http://localhost:5173
```

### 15.2 Key Pages

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    FRONTEND PAGES                                                │
└─────────────────────────────────────────────────────────────────────────────────┘

/login                    # Login page
/dashboard                # Dashboard with stats
/invoices                 # Invoice list
/invoices/:id             # Invoice detail
/users                    # User management (Admin)
/settings                 # Settings page

SUPER ADMIN ONLY:
/admin/organizations      # Organization management
/admin/users              # All users view
/admin/reports            # Platform reports
```

---

# PART E: DEPLOYMENT & OPERATIONS

---

## 16. Deployment Guide

### 16.1 Docker Compose (Local Development)

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: syncledger
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: ./syncledger-backend
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=syncledger
      - DB_USERNAME=postgres
      - DB_PASSWORD=postgres
    depends_on:
      - postgres

  pdf-service:
    build: ./pdf-microservice
    ports:
      - "8000:8000"

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

### 16.2 Run Locally with Docker

```bash
# Build and start all services
docker-compose up --build

# Access:
# Frontend:    http://localhost:3000
# Backend:     http://localhost:8080
# PDF Service: http://localhost:8000
```

---

## 17. API Reference

### 17.1 Authentication

```
POST /api/auth/login
────────────────────

Request:
{
  "email": "user@longhome.com",
  "password": "password123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 101,
    "email": "user@longhome.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "APPROVER",
    "orgId": 1,
    "orgName": "Longhome"
  }
}

Error (401 Unauthorized):
{
  "error": "Invalid credentials"
}
```

### 17.2 Invoices

```
GET /api/invoices
─────────────────
Authorization: Bearer <token>

Query Parameters:
  page (default: 0)
  size (default: 20)
  status (optional): PENDING, APPROVED, REJECTED
  search (optional): text search

Response (200 OK):
{
  "content": [
    {
      "id": 1001,
      "invoiceNumber": "INV-2026-001",
      "vendorName": "Acme Supplies",
      "amount": 5000.00,
      "status": "PENDING_APPROVAL",
      "createdAt": "2026-02-07T10:30:00Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8,
  "number": 0
}


POST /api/invoices/{id}/approve
───────────────────────────────
Authorization: Bearer <token>

Request:
{
  "comments": "Approved for payment"
}

Response (200 OK):
{
  "id": 1001,
  "status": "APPROVED",
  "approvedBy": "John Doe",
  "approvedAt": "2026-02-07T14:30:00Z"
}
```

---

## 18. Cost Estimation

### 18.1 Monthly Infrastructure Costs

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    MONTHLY COST ESTIMATION                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────┬───────────┬───────────┬───────────┐
│ Service                 │ Startup   │ Growth    │ Enterprise│
│                         │ (<500 inv)│ (500-5K)  │ (5K+)     │
├─────────────────────────┼───────────┼───────────┼───────────┤
│ AWS EC2 (Backend)       │ $15       │ $50       │ $150      │
│ AWS RDS (PostgreSQL)    │ $15       │ $50       │ $200      │
│ AWS S3 (Storage)        │ $5        │ $20       │ $50       │
│ AWS SQS (Queues)        │ $1        │ $5        │ $20       │
│ AWS Lambda (PDF)        │ $5        │ $20       │ $50       │
├─────────────────────────┼───────────┼───────────┼───────────┤
│ Microsoft 365 Licenses  │ $36*      │ $72*      │ $180*     │
│ (per mailbox)           │ (3 orgs)  │ (6 orgs)  │ (15 orgs) │
├─────────────────────────┼───────────┼───────────┼───────────┤
│ TOTAL MONTHLY           │ ~$77      │ ~$217     │ ~$650     │
│ TOTAL ANNUAL            │ ~$924     │ ~$2,604   │ ~$7,800   │
└─────────────────────────┴───────────┴───────────┴───────────┘

* Microsoft 365 Business Basic: ~$6/user/month per mailbox
```

### 18.2 Development Cost Estimate

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DEVELOPMENT COST ESTIMATE                                     │
└─────────────────────────────────────────────────────────────────────────────────┘

│ Component               │ Hours     │ Rate      │ Cost      │
├─────────────────────────┼───────────┼───────────┼───────────┤
│ Backend (Spring Boot)   │ 200       │ $50/hr    │ $10,000   │
│ PDF Microservice        │ 80        │ $50/hr    │ $4,000    │
│ Frontend (React)        │ 160       │ $50/hr    │ $8,000    │
│ DevOps & Deployment     │ 40        │ $50/hr    │ $2,000    │
│ Testing & QA            │ 60        │ $50/hr    │ $3,000    │
│ Documentation           │ 20        │ $50/hr    │ $1,000    │
├─────────────────────────┼───────────┼───────────┼───────────┤
│ TOTAL                   │ 560 hrs   │           │ $28,000   │
└─────────────────────────┴───────────┴───────────┴───────────┘

Timeline: 10-14 weeks with 1-2 developers
```

---

## 📎 Appendix

### A. Glossary

| Term | Definition |
|------|------------|
| **Multi-Tenant** | Single application serving multiple organizations with isolated data |
| **JWT** | JSON Web Token - used for authentication |
| **OCR** | Optical Character Recognition - extracts text from images |
| **SQS** | Amazon Simple Queue Service - message queue |
| **S3** | Amazon Simple Storage Service - file storage |

### B. Contact

For questions about this documentation:
- **Technical Lead**: dev@vedvix.com
- **Project Repository**: https://github.com/vedvix/syncledger

---

**END OF DOCUMENT**

*Document Version: 3.0 | Last Updated: February 7, 2026*
