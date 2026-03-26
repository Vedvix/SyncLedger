# SyncLedger
## Complete Project Documentation
### by vedvix

**Version:** 2.0  
**Date:** February 6, 2026  
**Status:** Draft  
**Document Type:** Single Source of Truth (Business & Technical)

> **💡 Architecture Decision:** This project uses a **Hybrid Model** with **Java Spring Boot** (main backend + custom authentication) and **Python FastAPI** (PDF processing microservice) to optimize development efficiency and reduce code complexity. We use **AWS cloud services** as our primary cloud provider for cost-effectiveness as a startup.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [**Multi-Tenant Architecture (SaaS Model)**](#2-multi-tenant-architecture-saas-model)
   - 2.1 - 2.10 Multi-Tenant Fundamentals
   - [2.11 Super Admin Console](#211-super-admin-console)
3. [Business Overview](#3-business-overview)
4. [Project Scope](#4-project-scope)
5. [Functional Requirements](#5-functional-requirements)
6. [System Architecture](#6-system-architecture)
7. [Data Flow](#7-data-flow)
8. [Technology Stack](#8-technology-stack)
9. [Database Design](#9-database-design)
10. [API Specifications](#10-api-specifications)
11. [User Interface Design](#11-user-interface-design)
12. [Security & Compliance](#12-security--compliance)
13. [Implementation Phases](#13-implementation-phases)
14. [Testing Strategy](#14-testing-strategy)
15. [Deployment & Infrastructure](#15-deployment--infrastructure)
16. [Glossary](#16-glossary)
17. [**DEVELOPMENT GUIDE: Complete Step-by-Step Implementation**](#17-development-guide-complete-step-by-step-implementation)
18. [**INFRASTRUCTURE COST ESTIMATION**](#18-infrastructure-cost-estimation)

---

## 1. Executive Summary

### 1.1 Purpose
This document outlines the complete requirements and technical specifications for building **SyncLedger** — an automated invoice processing system by vedvix that reads invoices from emails, extracts key data, stores them in a database, and routes them for approval before sending to the accounting system (Sage).

### 1.2 Problem Statement
Currently, invoice processing is manual, time-consuming, and error-prone. Staff must:
- Manually check emails for invoices
- Open each PDF and extract data
- Enter data into spreadsheets or systems
- Send for approvals via email chains
- Manually enter approved invoices into Sage

### 1.3 Solution Overview
An automated portal that:
- **Automatically reads** invoices from a dedicated Outlook mailbox
- **Extracts data** from PDF invoices using OCR/AI
- **Stores** invoice data in a structured database
- **Displays** invoices on a web portal for review
- **Routes** invoices through an approval workflow
- **Integrates** with Sage accounting system upon approval
- **Custom Authentication** via Spring Boot auth service (cost-effective alternative to third-party auth providers)
- **Hybrid Architecture** using Java Spring Boot + Python microservice for optimal performance
- **AWS Cloud Infrastructure** for startup-friendly, scalable, and cost-effective hosting

### 1.4 Key Benefits

| Benefit | Business Impact |
|---------|-----------------|
| **Time Savings** | 70-80% reduction in manual data entry |
| **Accuracy** | Eliminates human transcription errors |
| **Visibility** | Real-time dashboard of all invoices |
| **Audit Trail** | Complete history of approvals and changes |
| **Faster Processing** | Reduces invoice cycle time from days to hours |

---

## 2. Multi-Tenant Architecture (SaaS Model)

> **🏢 SyncLedger operates as a SaaS platform where multiple client organizations use the system, each with their own dedicated email inbox, users, data isolation, and processing queues.**

### 2.1 Multi-Tenant Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SYNCLEDGER MULTI-TENANT ARCHITECTURE                          │
│                         (SaaS Model by Nevorix)                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

                           ┌─────────────────────────────┐
                           │     SYNCLEDGER PLATFORM     │
                           │      (Central System)       │
                           └─────────────┬───────────────┘
                                         │
           ┌─────────────────────────────┼─────────────────────────────┐
           │                             │                             │
           ▼                             ▼                             ▼
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│     LONGHOME        │     │      EVOLOTEK       │     │    ORGANIZATION N   │
│  ─────────────────  │     │  ─────────────────  │     │  ─────────────────  │
│                     │     │                     │     │                     │
│  📧 Email Inbox:    │     │  📧 Email Inbox:    │     │  📧 Email Inbox:    │
│  longhome@nevorix.co│     │  evolotek@nevorix.co│     │  {org}@nevorix.co   │
│                     │     │                     │     │                     │
│  👥 Users:          │     │  👥 Users:          │     │  👥 Users:          │
│  • John (Admin)     │     │  • Sarah (Admin)    │     │  • Tagged to Org    │
│  • Mary (Approver)  │     │  • Tom (Approver)   │     │  • Role-based       │
│  • Bob (Viewer)     │     │  • Lisa (Viewer)    │     │  • Permissions      │
│                     │     │                     │     │                     │
│  📄 Documents:      │     │  📄 Documents:      │     │  📄 Documents:      │
│  • Isolated Storage │     │  • Isolated Storage │     │  • Isolated Storage │
│  • S3: /longhome/*  │     │  • S3: /evolotek/*  │     │  • S3: /{org}/*     │
│                     │     │                     │     │                     │
│  📊 Audit Trail:    │     │  📊 Audit Trail:    │     │  📊 Audit Trail:    │
│  • Org-specific logs│     │  • Org-specific logs│     │  • Org-specific logs│
│                     │     │                     │     │                     │
│  🔄 Queue:          │     │  🔄 Queue:          │     │  🔄 Queue:          │
│  • longhome-queue   │     │  • evolotek-queue   │     │  • {org}-queue      │
│                     │     │                     │     │                     │
└─────────────────────┘     └─────────────────────┘     └─────────────────────┘
```

### 2.2 Client Organizations (Tenants)

| Organization | Email Address | Description | Industry |
|--------------|---------------|-------------|----------|
| **Longhome** | `longhome@nevorix.co` | Construction company | Real Estate |
| **Evolotek** | `evolotek@nevorix.co` | Technology solutions | IT Services |
| **{New Client}** | `{clientname}@nevorix.co` | Auto-provisioned | Any |

### 2.3 Email Routing Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    EMAIL ROUTING PER ORGANIZATION                                │
└─────────────────────────────────────────────────────────────────────────────────┘

  VENDOR SENDS INVOICE                    NEVORIX DOMAIN EMAIL                    
  ═══════════════════                     ═══════════════════                    

  ┌──────────────────┐                    ┌──────────────────────────────────┐
  │  Acme Supplies   │                    │        MICROSOFT 365             │
  │  (Longhome's     │────Invoice PDF────►│        NEVORIX.CO                │
  │   Vendor)        │                    │                                  │
  └──────────────────┘                    │  ┌────────────────────────────┐  │
                                          │  │  longhome@nevorix.co       │  │
  ┌──────────────────┐                    │  │  ────────────────────────  │  │
  │  Tech Parts Inc  │                    │  │  • Receives Longhome       │  │
  │  (Evolotek's     │────Invoice PDF────►│  │    vendor invoices         │  │
  │   Vendor)        │                    │  └────────────────────────────┘  │
  └──────────────────┘                    │                                  │
                                          │  ┌────────────────────────────┐  │
  ┌──────────────────┐                    │  │  evolotek@nevorix.co       │  │
  │  Office Supplies │                    │  │  ────────────────────────  │  │
  │  Co. (Evolotek's │────Invoice PDF────►│  │  • Receives Evolotek       │  │
  │   Vendor)        │                    │  │    vendor invoices         │  │
  └──────────────────┘                    │  └────────────────────────────┘  │
                                          │                                  │
                                          └──────────────┬───────────────────┘
                                                         │
                                                         │ MS Graph API
                                                         ▼
                                          ┌──────────────────────────────────┐
                                          │      SYNCLEDGER PLATFORM         │
                                          │  ┌────────────────────────────┐  │
                                          │  │   Email Polling Service    │  │
                                          │  │   ──────────────────────   │  │
                                          │  │   • Polls all org inboxes  │  │
                                          │  │   • Routes by email domain │  │
                                          │  │   • Tags org_id to invoice │  │
                                          │  └────────────────────────────┘  │
                                          └──────────────────────────────────┘
```

### 2.4 Data Isolation Model

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         DATA ISOLATION PER TENANT                                │
└─────────────────────────────────────────────────────────────────────────────────┘

  DATABASE (Single Database, Org-ID Isolation)
  ═══════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  PostgreSQL Database: syncledger_db                                         │
  ├─────────────────────────────────────────────────────────────────────────────┤
  │                                                                             │
  │  organizations                                                              │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ org_id │ name      │ email_address          │ s3_bucket │ is_active │   │
  │  │────────│───────────│────────────────────────│───────────│───────────│   │
  │  │ 1      │ Longhome  │ longhome@nevorix.co    │ longhome  │ true      │   │
  │  │ 2      │ Evolotek  │ evolotek@nevorix.co    │ evolotek  │ true      │   │
  │  │ 3      │ Acme Corp │ acme@nevorix.co        │ acme      │ true      │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                             │
  │  users (tagged to organization)                                            │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ user_id │ org_id │ email              │ name    │ role     │        │   │
  │  │─────────│────────│────────────────────│─────────│──────────│        │   │
  │  │ 101     │ 1      │ john@longhome.com  │ John D. │ ADMIN    │ ◄──┐   │   │
  │  │ 102     │ 1      │ mary@longhome.com  │ Mary S. │ APPROVER │ ◄──┤   │   │
  │  │ 103     │ 1      │ bob@longhome.com   │ Bob W.  │ VIEWER   │ ◄──┤   │   │
  │  │─────────│────────│────────────────────│─────────│──────────│    │   │   │
  │  │ 201     │ 2      │ sarah@evolotek.com │ Sarah T.│ ADMIN    │    │   │   │
  │  │ 202     │ 2      │ tom@evolotek.com   │ Tom R.  │ APPROVER │    │   │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                      │      │
  │  invoices (isolated by org_id)                                       │      │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ invoice_id │ org_id │ invoice_no │ vendor_name │ amount  │ status  │   │
  │  │────────────│────────│────────────│─────────────│─────────│─────────│   │
  │  │ 1001       │ 1      │ INV-001    │ Acme Supp.  │ 5000.00 │ PENDING │   │
  │  │ 1002       │ 1      │ INV-002    │ Steel Co.   │ 8500.00 │ APPROVED│   │
  │  │────────────│────────│────────────│─────────────│─────────│─────────│   │
  │  │ 2001       │ 2      │ EVO-001    │ Tech Parts  │ 2500.00 │ PENDING │   │
  │  │ 2002       │ 2      │ EVO-002    │ Office Sup. │ 750.00  │ SYNCED  │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                             │
  │  ⚠️ ALL QUERIES FILTERED BY org_id - Users can ONLY see their org's data   │
  └─────────────────────────────────────────────────────────────────────────────┘


  FILE STORAGE (AWS S3 - Isolated Prefixes per Org)
  ═══════════════════════════════════════════════════════════════════════════════
  
  s3://syncledger-documents/
  │
  ├── longhome/                          ← Longhome's documents
  │   ├── invoices/
  │   │   ├── 2026/02/
  │   │   │   ├── INV-001.pdf
  │   │   │   └── INV-002.pdf
  │   │   └── ...
  │   └── processed/
  │
  ├── evolotek/                          ← Evolotek's documents
  │   ├── invoices/
  │   │   ├── 2026/02/
  │   │   │   ├── EVO-001.pdf
  │   │   │   └── EVO-002.pdf
  │   │   └── ...
  │   └── processed/
  │
  └── {org-slug}/                        ← New organizations auto-created
      └── ...
```

### 2.5 Queue Architecture (Per Organization)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PROCESSING QUEUES PER ORGANIZATION                            │
└─────────────────────────────────────────────────────────────────────────────────┘

                           ┌─────────────────────────┐
                           │   AWS SQS / RabbitMQ    │
                           │    (Message Broker)     │
                           └───────────┬─────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         │                             │                             │
         ▼                             ▼                             ▼
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│  longhome-invoice-  │     │  evolotek-invoice-  │     │  {org}-invoice-     │
│  processing-queue   │     │  processing-queue   │     │  processing-queue   │
├─────────────────────┤     ├─────────────────────┤     ├─────────────────────┤
│                     │     │                     │     │                     │
│  Messages:          │     │  Messages:          │     │  Messages:          │
│  • invoice_id: 1001 │     │  • invoice_id: 2001 │     │  • Isolated by org  │
│  • org_id: 1        │     │  • org_id: 2        │     │  • Priority routing │
│  • action: EXTRACT  │     │  • action: EXTRACT  │     │  • Rate limiting    │
│                     │     │                     │     │                     │
└─────────┬───────────┘     └─────────┬───────────┘     └─────────┬───────────┘
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│  Python PDF Worker  │     │  Python PDF Worker  │     │  Python PDF Worker  │
│  (Longhome context) │     │  (Evolotek context) │     │  (Org N context)    │
│                     │     │                     │     │                     │
│  • Extracts data    │     │  • Extracts data    │     │  • Processes for    │
│  • Saves to S3:     │     │  • Saves to S3:     │     │    specific org     │
│    /longhome/...    │     │    /evolotek/...    │     │                     │
│  • Updates DB with  │     │  • Updates DB with  │     │                     │
│    org_id = 1       │     │    org_id = 2       │     │                     │
└─────────────────────┘     └─────────────────────┘     └─────────────────────┘


  BENEFITS OF PER-ORG QUEUES:
  ═══════════════════════════════════════════════════════════════════════════════
  
  ✅ Isolation      - One org's backlog doesn't affect others
  ✅ Priority       - Can prioritize premium clients
  ✅ Rate Limiting  - Control processing rate per organization
  ✅ Debugging      - Easy to trace issues per organization
  ✅ Scaling        - Scale workers per org demand
  ✅ Billing        - Track processing volume per tenant
```

### 2.6 Audit Trail (Per Organization)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    AUDIT LOGGING PER ORGANIZATION                                │
└─────────────────────────────────────────────────────────────────────────────────┘

  audit_logs table (filtered by org_id)
  ┌──────────────────────────────────────────────────────────────────────────────┐
  │ log_id │ org_id │ user_id │ action        │ entity    │ entity_id │ timestamp│
  │────────│────────│─────────│───────────────│───────────│───────────│──────────│
  │ 5001   │ 1      │ 101     │ INVOICE_VIEW  │ INVOICE   │ 1001      │ 2026-02..│
  │ 5002   │ 1      │ 102     │ APPROVE       │ INVOICE   │ 1001      │ 2026-02..│
  │ 5003   │ 1      │ 101     │ SYNC_TO_SAGE  │ INVOICE   │ 1001      │ 2026-02..│
  │────────│────────│─────────│───────────────│───────────│───────────│──────────│
  │ 6001   │ 2      │ 201     │ INVOICE_VIEW  │ INVOICE   │ 2001      │ 2026-02..│
  │ 6002   │ 2      │ 202     │ REJECT        │ INVOICE   │ 2001      │ 2026-02..│
  └──────────────────────────────────────────────────────────────────────────────┘
  
  AUDIT EVENTS CAPTURED:
  ═══════════════════════════════════════════════════════════════════════════════
  
  📧 Email Events              📄 Invoice Events           👥 User Events
  • EMAIL_RECEIVED             • INVOICE_CREATED           • USER_LOGIN
  • EMAIL_PROCESSED            • INVOICE_VIEWED            • USER_LOGOUT
  • EMAIL_FAILED               • INVOICE_EDITED            • USER_CREATED
                               • INVOICE_APPROVED          • USER_UPDATED
  🔄 Sync Events               • INVOICE_REJECTED          • ROLE_CHANGED
  • SAGE_SYNC_STARTED          • INVOICE_DELETED           • PASSWORD_CHANGED
  • SAGE_SYNC_SUCCESS
  • SAGE_SYNC_FAILED
```

### 2.7 User-Organization Tagging

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         USER ORGANIZATION ASSIGNMENT                             │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                           LONGHOME (org_id: 1)                               │
  │                                                                              │
  │   👤 John Davis                 👤 Mary Smith              👤 Bob Wilson    │
  │   ┌────────────────────┐       ┌────────────────────┐     ┌──────────────┐ │
  │   │ Role: ADMIN        │       │ Role: APPROVER     │     │ Role: VIEWER │ │
  │   │ Email: john@...    │       │ Email: mary@...    │     │ Email: bob@..│ │
  │   │                    │       │                    │     │              │ │
  │   │ Permissions:       │       │ Permissions:       │     │ Permissions: │ │
  │   │ ✅ View Invoices   │       │ ✅ View Invoices   │     │ ✅ View Only │ │
  │   │ ✅ Approve/Reject  │       │ ✅ Approve/Reject  │     │ ❌ No Actions│ │
  │   │ ✅ Manage Users    │       │ ❌ No User Mgmt    │     │              │ │
  │   │ ✅ View Audit Logs │       │ ✅ View Own Logs   │     │              │ │
  │   │ ✅ Configure Sage  │       │                    │     │              │ │
  │   └────────────────────┘       └────────────────────┘     └──────────────┘ │
  │                                                                              │
  │   ⚠️ These users can ONLY see Longhome data (enforced by org_id filter)     │
  └─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                           EVOLOTEK (org_id: 2)                               │
  │                                                                              │
  │   👤 Sarah Thompson            👤 Tom Roberts              👤 Lisa Chen     │
  │   ┌────────────────────┐       ┌────────────────────┐     ┌──────────────┐ │
  │   │ Role: ADMIN        │       │ Role: APPROVER     │     │ Role: VIEWER │ │
  │   │ Email: sarah@...   │       │ Email: tom@...     │     │ Email: lisa@ │ │
  │   │                    │       │                    │     │              │ │
  │   │ Permissions:       │       │ Permissions:       │     │ Permissions: │ │
  │   │ ✅ Full Admin      │       │ ✅ Approve/Reject  │     │ ✅ View Only │ │
  │   └────────────────────┘       └────────────────────┘     └──────────────┘ │
  │                                                                              │
  │   ⚠️ These users can ONLY see Evolotek data (complete isolation)            │
  └─────────────────────────────────────────────────────────────────────────────┘


  SUPER ADMIN (SyncLedger/Nevorix Platform Admin)
  ═══════════════════════════════════════════════════════════════════════════════
  
  👤 SyncLedger Platform Administrator (org_id: NULL - Platform Level)
  ┌────────────────────────────────────────────────────────────────────────────┐
  │ Role: SUPER_ADMIN (Platform-level, Full Access)                             │
  │                                                                             │
  │ ORGANIZATION MANAGEMENT:                                                    │
  │ ✅ Create new organizations and configure their settings                    │
  │ ✅ Edit/Disable existing organizations                                      │
  │ ✅ Configure organization email addresses (e.g., longhome@nevorix.co)       │
  │ ✅ Set up Sage integration credentials per organization                     │
  │ ✅ Manage organization billing and subscription plans                       │
  │                                                                             │
  │ USER MANAGEMENT:                                                            │
  │ ✅ Create Organization ADMIN users for each organization                    │
  │ ✅ View all users across ALL organizations                                  │
  │ ✅ Reset passwords for any user                                             │
  │ ✅ Enable/Disable any user account                                          │
  │                                                                             │
  │ DATA ACCESS (FULL VISIBILITY):                                              │
  │ ✅ View ALL invoices from ALL organizations                                 │
  │ ✅ View ALL users from ALL organizations                                    │
  │ ✅ View ALL audit logs from ALL organizations                               │
  │ ✅ View ALL reports and analytics (platform-wide + per organization)        │
  │ ✅ Export data from any organization                                        │
  │                                                                             │
  │ PLATFORM CONFIGURATION:                                                     │
  │ ✅ Manage email domain routing rules                                        │
  │ ✅ Configure AWS infrastructure settings                                    │
  │ ✅ Manage system-wide settings and feature flags                            │
  │ ✅ View platform health and monitoring dashboards                           │
  └────────────────────────────────────────────────────────────────────────────┘

  ORGANIZATION ADMIN (Created by Super Admin)
  ═══════════════════════════════════════════════════════════════════════════════
  
  👤 Organization Administrator (org_id: specific org, e.g., 1 for Longhome)
  ┌────────────────────────────────────────────────────────────────────────────┐
  │ Role: ADMIN (Organization-level)                                            │
  │                                                                             │
  │ USER MANAGEMENT (within their organization):                                │
  │ ✅ Create APPROVER and VIEWER users for their organization                  │
  │ ✅ Edit/Disable users within their organization                             │
  │ ✅ Reset passwords for organization users                                   │
  │ ❌ Cannot create other ADMIN users (only Super Admin can)                   │
  │                                                                             │
  │ DATA ACCESS (organization-filtered):                                        │
  │ ✅ View ALL invoices for their organization                                 │
  │ ✅ Approve/Reject invoices                                                  │
  │ ✅ View audit logs for their organization                                   │
  │ ✅ View reports for their organization                                      │
  │ ✅ Configure Sage sync settings                                             │
  │ ❌ Cannot see other organizations' data                                     │
  └────────────────────────────────────────────────────────────────────────────┘
```

### 2.8 Multi-Tenant Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE MULTI-TENANT DATA FLOW                               │
└─────────────────────────────────────────────────────────────────────────────────┘

   STEP 1: Email Arrives at Organization-Specific Inbox
   ═══════════════════════════════════════════════════════════════════════════════

   Vendor (Acme Supplies) → sends invoice → longhome@nevorix.co
                                                    │
                                                    ▼
   ┌─────────────────────────────────────────────────────────────────────────────┐
   │  Microsoft 365 (Nevorix Domain)                                             │
   │  ┌─────────────────────────────────────────────────────────────────────┐   │
   │  │ Inbox: longhome@nevorix.co                                          │   │
   │  │ Subject: Invoice INV-2026-001                                       │   │
   │  │ Attachment: invoice.pdf                                             │   │
   │  └─────────────────────────────────────────────────────────────────────┘   │
   └─────────────────────────────────────────────────────────────────────────────┘

   STEP 2: Email Polling & Organization Detection
   ═══════════════════════════════════════════════════════════════════════════════

   ┌─────────────────────────────────────────────────────────────────────────────┐
   │  Spring Boot - Email Polling Service                                        │
   │  ────────────────────────────────────────────────────────────────────────   │
   │                                                                             │
   │  // Poll all organization inboxes                                          │
   │  for (Organization org : organizations) {                                  │
   │      List<Email> emails = graphApi.getEmails(org.getEmailAddress());       │
   │      for (Email email : emails) {                                          │
   │          // Tag with organization ID                                       │
   │          processInvoice(email, org.getId());  // org_id = 1 (Longhome)    │
   │      }                                                                     │
   │  }                                                                         │
   └─────────────────────────────────────────────────────────────────────────────┘

   STEP 3: Queue Message with Organization Context
   ═══════════════════════════════════════════════════════════════════════════════

   ┌─────────────────────────────────────────────────────────────────────────────┐
   │  SQS Queue: longhome-invoice-processing                                     │
   │  ────────────────────────────────────────────────────────────────────────   │
   │  {                                                                          │
   │    "message_id": "msg-12345",                                              │
   │    "org_id": 1,                                                            │
   │    "org_slug": "longhome",                                                 │
   │    "email_id": "email-abc123",                                             │
   │    "pdf_s3_path": "s3://syncledger-documents/longhome/inbox/invoice.pdf", │
   │    "action": "EXTRACT_AND_PROCESS"                                         │
   │  }                                                                          │
   └─────────────────────────────────────────────────────────────────────────────┘

   STEP 4: PDF Processing with Organization Isolation
   ═══════════════════════════════════════════════════════════════════════════════

   ┌─────────────────────────────────────────────────────────────────────────────┐
   │  Python PDF Microservice                                                    │
   │  ────────────────────────────────────────────────────────────────────────   │
   │                                                                             │
   │  # Always include org context in processing                                │
   │  def process_invoice(message):                                             │
   │      org_id = message['org_id']                                            │
   │      org_slug = message['org_slug']                                        │
   │                                                                             │
   │      # Download from org-specific S3 path                                  │
   │      pdf = s3.download(f"{org_slug}/inbox/{filename}")                     │
   │                                                                             │
   │      # Extract data                                                        │
   │      data = extract_invoice_data(pdf)                                      │
   │                                                                             │
   │      # Save to org-specific location                                       │
   │      s3.upload(f"{org_slug}/processed/{filename}", pdf)                    │
   │                                                                             │
   │      # Return with org context                                             │
   │      return {"org_id": org_id, "data": data}                               │
   └─────────────────────────────────────────────────────────────────────────────┘

   STEP 5: Database Record with Organization Tag
   ═══════════════════════════════════════════════════════════════════════════════

   ┌─────────────────────────────────────────────────────────────────────────────┐
   │  INSERT INTO invoices (                                                     │
   │      id, org_id, invoice_number, vendor_name, amount, status, pdf_path     │
   │  ) VALUES (                                                                 │
   │      1001,                                                                  │
   │      1,                        -- ← LONGHOME org_id                        │
   │      'INV-2026-001',                                                       │
   │      'Acme Supplies',                                                      │
   │      5000.00,                                                              │
   │      'PENDING',                                                            │
   │      's3://syncledger-documents/longhome/processed/INV-2026-001.pdf'       │
   │  );                                                                         │
   └─────────────────────────────────────────────────────────────────────────────┘

   STEP 6: User Access (Organization-Filtered)
   ═══════════════════════════════════════════════════════════════════════════════

   ┌─────────────────────────────────────────────────────────────────────────────┐
   │  User: Mary Smith (Longhome Approver) logs into SyncLedger                 │
   │  ────────────────────────────────────────────────────────────────────────   │
   │                                                                             │
   │  // API automatically filters by user's org_id                             │
   │  GET /api/invoices                                                         │
   │                                                                             │
   │  // Backend query (always filtered)                                        │
   │  SELECT * FROM invoices                                                    │
   │  WHERE org_id = 1    -- ← User's organization (from JWT token)             │
   │  AND status = 'PENDING';                                                   │
   │                                                                             │
   │  // Mary sees ONLY Longhome invoices ✅                                    │
   │  // She CANNOT see Evolotek invoices ❌                                    │
   └─────────────────────────────────────────────────────────────────────────────┘
```

### 2.9 Organization Onboarding Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    NEW ORGANIZATION ONBOARDING PROCESS                           │
└─────────────────────────────────────────────────────────────────────────────────┘

  STEP 1: Super Admin Creates Organization
  ═══════════════════════════════════════════════════════════════════════════════
  
  POST /api/admin/organizations
  {
    "name": "Acme Corporation",
    "slug": "acme",
    "email_address": "acme@nevorix.co",
    "primary_contact": {
      "name": "Jane Doe",
      "email": "jane@acme.com",
      "phone": "+1-555-0123"
    },
    "sage_config": {
      "api_endpoint": "https://acme.sage.com/api",
      "credentials_vault_key": "acme-sage-creds"
    }
  }

  STEP 2: System Auto-Provisions Resources
  ═══════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  Auto-Provisioning Checklist                                                │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │                                                                             │
  │  ✅ Create database record in `organizations` table                         │
  │  ✅ Create S3 folder: s3://syncledger-documents/acme/                       │
  │  ✅ Create SQS queue: acme-invoice-processing                               │
  │  ✅ Configure MS Graph to poll acme@nevorix.co                              │
  │  ✅ Create org admin user account                                           │
  │  ✅ Send welcome email with login credentials                               │
  │  ✅ Initialize audit log with ORGANIZATION_CREATED event                    │
  └─────────────────────────────────────────────────────────────────────────────┘

  STEP 3: Organization Admin Sets Up Users
  ═══════════════════════════════════════════════════════════════════════════════
  
  Jane (Acme Admin) logs in and creates her team:
  
  POST /api/users
  {
    "firstName": "Mike",
    "lastName": "Johnson",
    "email": "mike@acme.com",
    "role": "APPROVER",
    "department": "Finance"
  }
  // org_id is automatically set from Jane's JWT token
```

### 2.10 Security Considerations

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    MULTI-TENANT SECURITY MEASURES                                │
└─────────────────────────────────────────────────────────────────────────────────┘

  1. DATA ISOLATION (Mandatory)
  ═══════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  // EVERY database query MUST include org_id filter                         │
  │                                                                             │
  │  @Repository                                                                │
  │  public interface InvoiceRepository extends JpaRepository<Invoice, Long> {  │
  │                                                                             │
  │      // ✅ CORRECT - Always filter by org                                   │
  │      @Query("SELECT i FROM Invoice i WHERE i.organization.id = :orgId")    │
  │      List<Invoice> findByOrganization(@Param("orgId") Long orgId);         │
  │                                                                             │
  │      // ❌ FORBIDDEN - Never expose cross-org data                          │
  │      // List<Invoice> findAll();  // DANGEROUS - disabled                   │
  │  }                                                                          │
  └─────────────────────────────────────────────────────────────────────────────┘

  2. JWT TOKEN INCLUDES ORG_ID
  ═══════════════════════════════════════════════════════════════════════════════
  
  {
    "sub": "user@longhome.com",
    "user_id": 101,
    "org_id": 1,              ← Organization ID in every token
    "org_slug": "longhome",
    "role": "APPROVER",
    "exp": 1738886400
  }

  3. API MIDDLEWARE ENFORCES ORG CONTEXT
  ═══════════════════════════════════════════════════════════════════════════════
  
  @Component
  public class OrganizationContextFilter {
      @Override
      public void doFilter(request, response, chain) {
          Long orgId = extractOrgIdFromJwt(request);
          OrganizationContext.setCurrentOrgId(orgId);  // ThreadLocal
          chain.doFilter(request, response);
      }
  }

  4. S3 BUCKET POLICIES (Org-Specific Prefixes)
  ═══════════════════════════════════════════════════════════════════════════════
  
  // IAM policy ensures service can only access org's folder
  {
    "Effect": "Allow",
    "Action": ["s3:GetObject", "s3:PutObject"],
    "Resource": "arn:aws:s3:::syncledger-documents/${org_slug}/*"
  }
```

### 2.11 Super Admin Console

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SUPER ADMIN CONSOLE (PLATFORM MANAGEMENT)                     │
└─────────────────────────────────────────────────────────────────────────────────┘

  The Super Admin Console is available ONLY to SyncLedger/Nevorix platform 
  administrators. It provides complete visibility and control over all 
  organizations using the platform.

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  SUPER ADMIN LOGIN FLOW                                                      │
  │  ═══════════════════════════════════════════════════════════════════════════│
  │                                                                             │
  │   1. Super Admin navigates to: https://admin.syncledger.com                 │
  │   2. Logs in with platform credentials (separate from org users)            │
  │   3. JWT issued with role: SUPER_ADMIN, org_id: NULL                        │
  │   4. Redirected to Super Admin Dashboard                                    │
  │                                                                             │
  │   ┌────────────────────────────────────────────────────────────────────┐   │
  │   │  JWT Token Structure (Super Admin)                                 │   │
  │   │  ──────────────────────────────────────────────────────────────── │   │
  │   │  {                                                                 │   │
  │   │    "sub": "admin@syncledger.com",                                  │   │
  │   │    "user_id": 1,                                                   │   │
  │   │    "org_id": null,        ← NULL = Platform-level access          │   │
  │   │    "role": "SUPER_ADMIN", ← Full platform access                  │   │
  │   │    "permissions": [                                                │   │
  │   │      "MANAGE_ORGANIZATIONS",                                       │   │
  │   │      "MANAGE_ALL_USERS",                                           │   │
  │   │      "VIEW_ALL_DATA",                                              │   │
  │   │      "VIEW_ALL_REPORTS",                                           │   │
  │   │      "PLATFORM_CONFIG"                                             │   │
  │   │    ],                                                              │   │
  │   │    "exp": 1738886400                                               │   │
  │   │  }                                                                 │   │
  │   └────────────────────────────────────────────────────────────────────┘   │
  └─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.11.1 Super Admin Dashboard

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SUPER ADMIN DASHBOARD                                    │
│  SyncLedger Platform Administration                                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      PLATFORM OVERVIEW                                   │   │
│  │  ─────────────────────────────────────────────────────────────────────  │   │
│  │                                                                         │   │
│  │   🏢 Total Organizations: 12        👥 Total Users: 156                 │   │
│  │   📄 Total Invoices: 8,432          💰 Total Value: $4.2M              │   │
│  │   ✅ Approved Today: 47             ⏳ Pending Review: 89               │   │
│  │   ❌ Failed Processing: 3           🔄 Sage Sync Queue: 12              │   │
│  │                                                                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                    ORGANIZATION STATUS                                   │   │
│  │  ─────────────────────────────────────────────────────────────────────  │   │
│  │                                                                         │   │
│  │  │ Organization  │ Status │ Users │ Invoices │ Pending │ Last Active │ │   │
│  │  │───────────────│────────│───────│──────────│─────────│─────────────│ │   │
│  │  │ Longhome      │ ✅     │ 8     │ 1,245    │ 12      │ 5 min ago   │ │   │
│  │  │ Evolotek      │ ✅     │ 5     │ 892      │ 8       │ 12 min ago  │ │   │
│  │  │ Acme Corp     │ ✅     │ 12    │ 2,341    │ 23      │ 2 min ago   │ │   │
│  │  │ TechStart     │ ⚠️     │ 3     │ 156      │ 45      │ 2 hours ago │ │   │
│  │  │ BuildRight    │ ✅     │ 6     │ 1,102    │ 5       │ 8 min ago   │ │   │
│  │  │───────────────│────────│───────│──────────│─────────│─────────────│ │   │
│  │                                                                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  [➕ Add Organization]  [📊 View Reports]  [⚙️ Platform Settings]               │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### 2.11.2 Organization Management (Super Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    ORGANIZATION MANAGEMENT                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

  CREATE NEW ORGANIZATION
  ═══════════════════════════════════════════════════════════════════════════════

  Super Admin fills out organization creation form:

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  ADD NEW ORGANIZATION                                              [Close X]│
  │  ─────────────────────────────────────────────────────────────────────────  │
  │                                                                             │
  │  Organization Details                                                       │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ Name:            [Sunrise Builders Ltd                           ] │   │
  │  │ Slug:            [sunrise-builders        ] (auto-generated)       │   │
  │  │ Email Address:   [sunrise@nevorix.co      ] @nevorix.co domain    │   │
  │  │ Status:          [Active ▼]                                        │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                             │
  │  Primary Contact                                                            │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ Contact Name:    [David Miller                                   ] │   │
  │  │ Contact Email:   [david@sunrisebuilders.com                      ] │   │
  │  │ Contact Phone:   [+1-555-0199                                    ] │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                             │
  │  Sage Integration Settings                                                  │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ Sage API Endpoint:  [https://sunrise.sage.com/api                ] │   │
  │  │ API Key:            [************************                    ] │   │
  │  │ Auto-Sync Enabled:  [✅ Yes]                                       │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                             │
  │  Create Admin User                                                          │
  │  ┌─────────────────────────────────────────────────────────────────────┐   │
  │  │ Admin First Name: [David                                         ] │   │
  │  │ Admin Last Name:  [Miller                                        ] │   │
  │  │ Admin Email:      [david@sunrisebuilders.com                     ] │   │
  │  │ Temp Password:    [Auto-generate and email ✅]                     │   │
  │  └─────────────────────────────────────────────────────────────────────┘   │
  │                                                                             │
  │         [Cancel]                              [Create Organization]         │
  └─────────────────────────────────────────────────────────────────────────────┘

  BACKEND PROCESS (When "Create Organization" is clicked):
  ═══════════════════════════════════════════════════════════════════════════════

  POST /api/super-admin/organizations
  Authorization: Bearer <super_admin_jwt>

  {
    "name": "Sunrise Builders Ltd",
    "slug": "sunrise-builders",
    "emailAddress": "sunrise@nevorix.co",
    "status": "ACTIVE",
    "primaryContact": {
      "name": "David Miller",
      "email": "david@sunrisebuilders.com",
      "phone": "+1-555-0199"
    },
    "sageConfig": {
      "apiEndpoint": "https://sunrise.sage.com/api",
      "apiKey": "encrypted-key-here",
      "autoSyncEnabled": true
    },
    "initialAdmin": {
      "firstName": "David",
      "lastName": "Miller",
      "email": "david@sunrisebuilders.com",
      "sendWelcomeEmail": true
    }
  }

  RESPONSE (201 Created):
  {
    "id": 6,
    "name": "Sunrise Builders Ltd",
    "slug": "sunrise-builders",
    "emailAddress": "sunrise@nevorix.co",
    "status": "ACTIVE",
    "createdAt": "2026-02-06T10:30:00Z",
    "resourcesProvisioned": {
      "s3Folder": "s3://syncledger-documents/sunrise-builders/",
      "sqsQueue": "sunrise-builders-invoice-processing",
      "adminUser": {
        "id": 157,
        "email": "david@sunrisebuilders.com",
        "role": "ADMIN",
        "welcomeEmailSent": true
      }
    }
  }
```

#### 2.11.3 All Users View (Super Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    ALL USERS (CROSS-ORGANIZATION VIEW)                           │
└─────────────────────────────────────────────────────────────────────────────────┘

  Super Admin can see and manage ALL users across ALL organizations:

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  USER MANAGEMENT                                   [➕ Create Admin User]    │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │                                                                             │
  │  Filter: [All Organizations ▼] [All Roles ▼] [All Status ▼] [🔍 Search... ]│
  │                                                                             │
  │  │ User           │ Email                │ Organization │ Role     │ Status│
  │  │────────────────│──────────────────────│──────────────│──────────│───────│
  │  │ John Davis     │ john@longhome.com    │ Longhome     │ ADMIN    │ ✅    │
  │  │ Mary Smith     │ mary@longhome.com    │ Longhome     │ APPROVER │ ✅    │
  │  │ Sarah Thompson │ sarah@evolotek.com   │ Evolotek     │ ADMIN    │ ✅    │
  │  │ Tom Roberts    │ tom@evolotek.com     │ Evolotek     │ APPROVER │ ✅    │
  │  │ Jane Doe       │ jane@acme.com        │ Acme Corp    │ ADMIN    │ ✅    │
  │  │ Mike Johnson   │ mike@acme.com        │ Acme Corp    │ APPROVER │ ⚠️    │
  │  │ David Miller   │ david@sunrise.com    │ Sunrise      │ ADMIN    │ ✅    │
  │  │────────────────│──────────────────────│──────────────│──────────│───────│
  │                                                                             │
  │  Showing 1-7 of 156 users                         [< Prev] [1] [2]... [Next>]│
  └─────────────────────────────────────────────────────────────────────────────┘

  CREATE ADMIN USER (Super Admin Only)
  ═══════════════════════════════════════════════════════════════════════════════

  Only Super Admin can create ADMIN users for organizations:

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  CREATE ORGANIZATION ADMIN                                         [Close X]│
  │  ─────────────────────────────────────────────────────────────────────────  │
  │                                                                             │
  │  Organization:    [Longhome ▼]  ← Select which org to create admin for     │
  │                                                                             │
  │  First Name:      [_____________________________]                           │
  │  Last Name:       [_____________________________]                           │
  │  Email:           [_____________________________]                           │
  │  Role:            [ADMIN] (Only Super Admin can create admins)              │
  │                                                                             │
  │  □ Send welcome email with temporary password                               │
  │  □ Require password change on first login                                   │
  │                                                                             │
  │         [Cancel]                                      [Create Admin]        │
  └─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.11.4 All Invoices View (Super Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    ALL INVOICES (CROSS-ORGANIZATION VIEW)                        │
└─────────────────────────────────────────────────────────────────────────────────┘

  Super Admin can view ALL invoices from ALL organizations:

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  INVOICES                                                                    │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │                                                                             │
  │  Filter: [All Organizations ▼] [All Status ▼] [Date Range ▼] [🔍 Search...]│
  │                                                                             │
  │  │ Invoice #    │ Organization │ Vendor        │ Amount    │ Status    │   │
  │  │──────────────│──────────────│───────────────│───────────│───────────│   │
  │  │ INV-2026-001 │ Longhome     │ Acme Supplies │ $5,000.00 │ ✅ Approved│   │
  │  │ INV-2026-002 │ Evolotek     │ Tech Parts    │ $2,340.00 │ ⏳ Pending │   │
  │  │ INV-2026-003 │ Acme Corp    │ Office Plus   │ $890.00   │ ✅ Synced  │   │
  │  │ INV-2026-004 │ Longhome     │ BuildMart     │ $12,500   │ ❌ Rejected│   │
  │  │ INV-2026-005 │ Sunrise      │ Steel Co      │ $45,000   │ ⏳ Review  │   │
  │  │──────────────│──────────────│───────────────│───────────│───────────│   │
  │                                                                             │
  │  Total: 8,432 invoices across 12 organizations      [< Prev] [1] [Next >]   │
  └─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.11.5 Platform Reports (Super Admin)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PLATFORM REPORTS & ANALYTICS                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

  Super Admin has access to platform-wide reports:

  REPORT TYPES AVAILABLE:
  ═══════════════════════════════════════════════════════════════════════════════

  📊 PLATFORM-WIDE REPORTS
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │ • Invoice Volume Report (all orgs combined)                                 │
  │ • Revenue by Organization                                                   │
  │ • User Activity Report (all users)                                          │
  │ • System Health & Performance                                               │
  │ • Error & Failed Processing Report                                          │
  │ • Sage Sync Status (all orgs)                                               │
  └─────────────────────────────────────────────────────────────────────────────┘

  📈 PER-ORGANIZATION REPORTS (Super Admin can view any org's reports)
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │ • Organization-specific Invoice Report                                      │
  │ • Organization User Activity                                                │
  │ • Organization Audit Trail                                                  │
  │ • Organization Sage Sync History                                            │
  └─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  PLATFORM ANALYTICS DASHBOARD                                                │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │                                                                             │
  │   Invoice Volume (Last 30 Days)                                             │
  │   ┌─────────────────────────────────────────────────────────────────────┐  │
  │   │                                                        ⬛⬛         │  │
  │   │                                           ⬛⬛⬛       ⬛⬛⬛        │  │
  │   │                              ⬛⬛⬛        ⬛⬛⬛⬛      ⬛⬛⬛⬛       │  │
  │   │               ⬛⬛⬛⬛        ⬛⬛⬛⬛⬛      ⬛⬛⬛⬛⬛     ⬛⬛⬛⬛⬛      │  │
  │   │   ⬛⬛⬛⬛⬛    ⬛⬛⬛⬛⬛⬛     ⬛⬛⬛⬛⬛⬛     ⬛⬛⬛⬛⬛⬛    ⬛⬛⬛⬛⬛⬛     │  │
  │   │───────────────────────────────────────────────────────────────────  │  │
  │   │   Week 1      Week 2        Week 3        Week 4        Week 5      │  │
  │   └─────────────────────────────────────────────────────────────────────┘  │
  │                                                                             │
  │   Top Organizations by Volume:                                              │
  │   1. Acme Corp     - 2,341 invoices (28%)  ████████████████░░░░░░░░░░░░    │
  │   2. Longhome      - 1,245 invoices (15%)  ████████░░░░░░░░░░░░░░░░░░░░    │
  │   3. BuildRight    - 1,102 invoices (13%)  ███████░░░░░░░░░░░░░░░░░░░░░    │
  │   4. Evolotek      - 892 invoices (11%)    █████░░░░░░░░░░░░░░░░░░░░░░░    │
  │   5. Others        - 2,852 invoices (33%)  ███████████████████░░░░░░░░░    │
  │                                                                             │
  └─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.11.6 Role Hierarchy & Permission Matrix

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    ROLE HIERARCHY & PERMISSION MATRIX                            │
└─────────────────────────────────────────────────────────────────────────────────┘

  ROLE HIERARCHY
  ═══════════════════════════════════════════════════════════════════════════════

                    ┌─────────────────────────────┐
                    │       SUPER_ADMIN           │
                    │    (SyncLedger Platform)    │
                    │    org_id: NULL             │
                    └──────────────┬──────────────┘
                                   │
                    Creates & Manages ↓
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
  ┌───────────────┐         ┌───────────────┐         ┌───────────────┐
  │    ADMIN      │         │    ADMIN      │         │    ADMIN      │
  │  (Longhome)   │         │  (Evolotek)   │         │  (Acme Corp)  │
  │  org_id: 1    │         │  org_id: 2    │         │  org_id: 3    │
  └───────┬───────┘         └───────┬───────┘         └───────┬───────┘
          │                         │                         │
  Creates & Manages ↓       Creates & Manages ↓       Creates & Manages ↓
          │                         │                         │
     ┌────┴────┐               ┌────┴────┐               ┌────┴────┐
     ▼         ▼               ▼         ▼               ▼         ▼
  ┌──────┐  ┌──────┐       ┌──────┐  ┌──────┐       ┌──────┐  ┌──────┐
  │APPRVR│  │VIEWER│       │APPRVR│  │VIEWER│       │APPRVR│  │VIEWER│
  │org:1 │  │org:1 │       │org:2 │  │org:2 │       │org:3 │  │org:3 │
  └──────┘  └──────┘       └──────┘  └──────┘       └──────┘  └──────┘


  PERMISSION MATRIX
  ═══════════════════════════════════════════════════════════════════════════════

  │ Permission                    │ SUPER_ADMIN │ ADMIN │ APPROVER │ VIEWER │
  │───────────────────────────────│─────────────│───────│──────────│────────│
  │                               │             │       │          │        │
  │ ORGANIZATION MANAGEMENT       │             │       │          │        │
  │ Create Organization           │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Edit Organization             │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Delete/Disable Organization   │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View All Organizations        │     ✅      │  ❌   │    ❌    │   ❌   │
  │                               │             │       │          │        │
  │ USER MANAGEMENT               │             │       │          │        │
  │ Create ADMIN users            │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Create APPROVER/VIEWER users  │     ✅      │  ✅*  │    ❌    │   ❌   │
  │ Edit users (any org)          │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Edit users (own org)          │     ✅      │  ✅   │    ❌    │   ❌   │
  │ View all users (any org)      │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View users (own org)          │     ✅      │  ✅   │    ❌    │   ❌   │
  │ Reset any user's password     │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Reset org user's password     │     ✅      │  ✅   │    ❌    │   ❌   │
  │                               │             │       │          │        │
  │ INVOICE MANAGEMENT            │             │       │          │        │
  │ View invoices (all orgs)      │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View invoices (own org)       │     ✅      │  ✅   │    ✅    │   ✅   │
  │ Edit invoice data             │     ✅      │  ✅   │    ✅    │   ❌   │
  │ Approve/Reject invoices       │     ✅      │  ✅   │    ✅    │   ❌   │
  │ Delete invoices               │     ✅      │  ✅   │    ❌    │   ❌   │
  │                               │             │       │          │        │
  │ REPORTS & AUDIT               │             │       │          │        │
  │ View platform-wide reports    │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View org reports (any org)    │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View org reports (own org)    │     ✅      │  ✅   │    ✅    │   ✅   │
  │ View audit logs (all orgs)    │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View audit logs (own org)     │     ✅      │  ✅   │    ❌    │   ❌   │
  │ Export data (any org)         │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Export data (own org)         │     ✅      │  ✅   │    ❌    │   ❌   │
  │                               │             │       │          │        │
  │ PLATFORM CONFIGURATION        │             │       │          │        │
  │ Manage email routing          │     ✅      │  ❌   │    ❌    │   ❌   │
  │ Configure Sage settings       │     ✅      │  ✅   │    ❌    │   ❌   │
  │ Platform settings             │     ✅      │  ❌   │    ❌    │   ❌   │
  │ View system health            │     ✅      │  ❌   │    ❌    │   ❌   │
  │───────────────────────────────│─────────────│───────│──────────│────────│
  
  * ADMIN can only create APPROVER/VIEWER users within their own organization
```

#### 2.11.7 API Endpoints for Super Admin

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SUPER ADMIN API ENDPOINTS                                     │
└─────────────────────────────────────────────────────────────────────────────────┘

  All endpoints require: Authorization: Bearer <super_admin_jwt>
  All endpoints check: role == SUPER_ADMIN

  ORGANIZATION ENDPOINTS
  ═══════════════════════════════════════════════════════════════════════════════

  GET    /api/super-admin/organizations           # List all organizations
  POST   /api/super-admin/organizations           # Create new organization
  GET    /api/super-admin/organizations/{id}      # Get organization details
  PUT    /api/super-admin/organizations/{id}      # Update organization
  DELETE /api/super-admin/organizations/{id}      # Disable organization

  USER ENDPOINTS (Cross-Organization)
  ═══════════════════════════════════════════════════════════════════════════════

  GET    /api/super-admin/users                   # List all users (all orgs)
  POST   /api/super-admin/users                   # Create admin user for org
  GET    /api/super-admin/users/{id}              # Get user details
  PUT    /api/super-admin/users/{id}              # Update user
  DELETE /api/super-admin/users/{id}              # Disable user
  POST   /api/super-admin/users/{id}/reset-password  # Reset password

  INVOICE ENDPOINTS (Cross-Organization)
  ═══════════════════════════════════════════════════════════════════════════════

  GET    /api/super-admin/invoices                # List all invoices (all orgs)
  GET    /api/super-admin/invoices?org_id=1       # Filter by organization
  GET    /api/super-admin/invoices/{id}           # Get invoice details

  REPORTS & ANALYTICS ENDPOINTS
  ═══════════════════════════════════════════════════════════════════════════════

  GET    /api/super-admin/reports/platform-overview    # Platform-wide stats
  GET    /api/super-admin/reports/organizations        # Per-org breakdown
  GET    /api/super-admin/reports/users                # User activity report
  GET    /api/super-admin/reports/invoices             # Invoice volume report
  GET    /api/super-admin/reports/audit-logs           # All audit logs
  GET    /api/super-admin/reports/audit-logs?org_id=1  # Filter by org

  PLATFORM CONFIGURATION ENDPOINTS
  ═══════════════════════════════════════════════════════════════════════════════

  GET    /api/super-admin/settings                # Get platform settings
  PUT    /api/super-admin/settings                # Update platform settings
  GET    /api/super-admin/health                  # System health status
  GET    /api/super-admin/email-routing           # Email routing config
  PUT    /api/super-admin/email-routing           # Update email routing
```

#### 2.11.8 Database Schema for Super Admin

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DATABASE SCHEMA UPDATES                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

  USERS TABLE (Updated)
  ═══════════════════════════════════════════════════════════════════════════════

  CREATE TABLE users (
      id                BIGSERIAL PRIMARY KEY,
      org_id            BIGINT REFERENCES organizations(id) NULL,  -- NULL for SUPER_ADMIN
      email             VARCHAR(255) UNIQUE NOT NULL,
      password_hash     VARCHAR(255) NOT NULL,
      first_name        VARCHAR(100) NOT NULL,
      last_name         VARCHAR(100) NOT NULL,
      role              VARCHAR(50) NOT NULL,  -- SUPER_ADMIN, ADMIN, APPROVER, VIEWER
      status            VARCHAR(50) DEFAULT 'ACTIVE',
      created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      created_by        BIGINT REFERENCES users(id),
      last_login_at     TIMESTAMP,
      
      -- For SUPER_ADMIN: org_id is NULL
      -- For ADMIN: can only be created by SUPER_ADMIN
      -- For APPROVER/VIEWER: can be created by ADMIN or SUPER_ADMIN
      
      CONSTRAINT valid_role CHECK (
          role IN ('SUPER_ADMIN', 'ADMIN', 'APPROVER', 'VIEWER')
      ),
      CONSTRAINT super_admin_no_org CHECK (
          (role = 'SUPER_ADMIN' AND org_id IS NULL) OR
          (role != 'SUPER_ADMIN' AND org_id IS NOT NULL)
      )
  );

  ORGANIZATIONS TABLE
  ═══════════════════════════════════════════════════════════════════════════════

  CREATE TABLE organizations (
      id                BIGSERIAL PRIMARY KEY,
      name              VARCHAR(255) NOT NULL,
      slug              VARCHAR(100) UNIQUE NOT NULL,
      email_address     VARCHAR(255) UNIQUE NOT NULL,
      status            VARCHAR(50) DEFAULT 'ACTIVE',
      
      -- Primary contact
      contact_name      VARCHAR(255),
      contact_email     VARCHAR(255),
      contact_phone     VARCHAR(50),
      
      -- Sage configuration (encrypted)
      sage_api_endpoint VARCHAR(500),
      sage_api_key_enc  VARCHAR(500),
      sage_auto_sync    BOOLEAN DEFAULT true,
      
      -- Timestamps
      created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      created_by        BIGINT REFERENCES users(id),  -- SUPER_ADMIN who created
      updated_at        TIMESTAMP,
      
      -- Resource tracking
      s3_folder_path    VARCHAR(500),
      sqs_queue_name    VARCHAR(255)
  );

  SAMPLE DATA
  ═══════════════════════════════════════════════════════════════════════════════

  -- Super Admin (Platform Level)
  INSERT INTO users (id, org_id, email, first_name, last_name, role) VALUES
  (1, NULL, 'admin@syncledger.com', 'Platform', 'Admin', 'SUPER_ADMIN');

  -- Organizations
  INSERT INTO organizations (id, name, slug, email_address) VALUES
  (1, 'Longhome', 'longhome', 'longhome@nevorix.co'),
  (2, 'Evolotek', 'evolotek', 'evolotek@nevorix.co'),
  (3, 'Acme Corporation', 'acme', 'acme@nevorix.co');

  -- Organization Admins (Created by Super Admin)
  INSERT INTO users (org_id, email, first_name, last_name, role, created_by) VALUES
  (1, 'john@longhome.com', 'John', 'Davis', 'ADMIN', 1),
  (2, 'sarah@evolotek.com', 'Sarah', 'Thompson', 'ADMIN', 1),
  (3, 'jane@acme.com', 'Jane', 'Doe', 'ADMIN', 1);

  -- Organization Users (Created by Org Admin)
  INSERT INTO users (org_id, email, first_name, last_name, role, created_by) VALUES
  (1, 'mary@longhome.com', 'Mary', 'Smith', 'APPROVER', 2),  -- Created by John (ADMIN)
  (1, 'bob@longhome.com', 'Bob', 'Wilson', 'VIEWER', 2),     -- Created by John (ADMIN)
  (2, 'tom@evolotek.com', 'Tom', 'Roberts', 'APPROVER', 3);  -- Created by Sarah (ADMIN)
```

---

## 3. Business Overview

### 3.1 Current Process (As-Is)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CURRENT MANUAL PROCESS                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   📧 Email        📄 Open PDF      ✍️ Manual         📧 Email           │
│   Received   →    & Read      →   Data Entry   →   for Approval        │
│                                                                         │
│                   ⏳ Wait for      ✍️ Manual         ✅ Done            │
│               ←   Approval    ←   Sage Entry    ←                      │
│                                                                         │
│   ⚠️ Problems: Slow, Error-prone, No visibility, Lost emails           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Future Process (To-Be)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      AUTOMATED PORTAL PROCESS                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   📧 Email        🤖 Auto         💾 Database       👀 Portal           │
│   Received   →    Extract    →    Storage     →    Review              │
│                                                                         │
│                   🔗 Auto         ✅ Approved       ✅ Done             │
│               ←   Sage Sync  ←   in Portal    ←                        │
│                                                                         │
│   ✅ Benefits: Fast, Accurate, Full visibility, Audit trail            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Stakeholders

| Role | Responsibility | Portal Access |
|------|---------------|---------------|
| **Finance Team** | Reviews and approves invoices | Full Access |
| **Approvers/Managers** | Approve/Reject invoices | Approval Access |
| **Accounting Team** | Manages Sage integration | Admin Access |
| **IT Team** | System maintenance | Admin Access |
| **Vendors** | Submit invoices via email | No Portal Access |

---

## 4. Project Scope

### 4.1 In Scope ✅

| # | Feature | Description |
|---|---------|-------------|
| 1 | Email Monitoring | Monitor dedicated Outlook mailbox for incoming invoices |
| 2 | PDF Processing | Read and extract data from PDF invoice attachments |
| 3 | Data Extraction | Extract: Customer Name, Customer ID, Opportunity Number, Amount |
| 4 | Database Storage | Store invoice data with PDF file reference |
| 5 | Web Portal | Display invoices in a user-friendly interface |
| 6 | Approval Workflow | Route invoices for approval with Accept/Reject actions |
| 7 | Sage Integration | Send approved invoices to Sage accounting system |
| 8 | Audit Trail | Log all actions for compliance |

### 4.2 Out of Scope ❌

| # | Feature | Reason |
|---|---------|--------|
| 1 | Vendor Portal | Vendors will continue emailing invoices |
| 2 | Payment Processing | Handled by Sage |
| 3 | Purchase Order Matching | Phase 2 consideration |
| 4 | Multi-currency Processing | Phase 2 consideration |
| 5 | Mobile App | Web portal is mobile-responsive |

### 4.3 Assumptions

1. Invoices will be sent **only** to the dedicated email address
2. Invoices will be in **PDF format**
3. Invoice PDFs **may have different/varying layouts** - system must handle multiple formats
4. Sage has available **APIs** for data integration
5. Users have **modern web browsers** (Chrome, Edge, Firefox)
6. Organization uses **Microsoft 365** for email

### 4.4 Constraints

1. Must integrate with existing Microsoft 365 infrastructure
2. Must comply with company data security policies
3. Budget and timeline as defined by project governance

---

## 5. Functional Requirements

### 5.1 Email Processing Module

#### FR-001: Email Monitoring
| Attribute | Value |
|-----------|-------|
| **ID** | FR-001 |
| **Priority** | High |
| **Description** | System shall monitor a dedicated Outlook mailbox continuously |
| **Business Rule** | Check for new emails every 5 minutes (configurable) |
| **Acceptance Criteria** | New emails are detected within 5 minutes of arrival |

#### FR-002: Email Filtering
| Attribute | Value |
|-----------|-------|
| **ID** | FR-002 |
| **Priority** | High |
| **Description** | System shall process only emails with PDF attachments |
| **Business Rule** | Ignore emails without attachments or non-PDF attachments |
| **Acceptance Criteria** | Only PDF attachments are processed |

#### FR-003: Attachment Download
| Attribute | Value |
|-----------|-------|
| **ID** | FR-003 |
| **Priority** | High |
| **Description** | System shall download PDF attachments from emails |
| **Business Rule** | Store PDFs in secure file storage |
| **Acceptance Criteria** | PDFs are stored and linked to email record |

### 5.2 PDF Processing Module

#### FR-004: PDF Text Extraction
| Attribute | Value |
|-----------|-------|
| **ID** | FR-004 |
| **Priority** | High |
| **Description** | System shall extract text from PDF invoices |
| **Business Rule** | Use OCR for scanned documents |
| **Acceptance Criteria** | Text is extracted with 95%+ accuracy |

#### FR-005: Data Field Extraction
| Attribute | Value |
|-----------|-------|
| **ID** | FR-005 |
| **Priority** | High |
| **Description** | System shall extract specific fields from invoice |
| **Fields** | Customer Name, Customer ID, Opportunity Number, Invoice Amount |
| **Acceptance Criteria** | All required fields extracted or flagged for manual review |

#### FR-005.1: Variable PDF Format Handling
| Attribute | Value |
|-----------|-------|
| **ID** | FR-005.1 |
| **Priority** | High |
| **Description** | System shall handle invoices with different/varying PDF layouts |
| **Business Rule** | Use AI/ML-based extraction that adapts to different formats |
| **Acceptance Criteria** | System processes invoices regardless of layout without breaking |

> **⚠️ CRITICAL REQUIREMENT: Variable PDF Format Support**
> 
> Invoice PDFs may come from different vendors with completely different layouts. The system **MUST NOT break** when encountering new or unknown formats. Instead, it should:
> - Use intelligent AI-based extraction (not template-based)
> - Extract fields based on semantic understanding, not fixed positions
> - Flag low-confidence extractions for manual review
> - Learn and improve over time

#### FR-006: Invoice Validation
| Attribute | Value |
|-----------|-------|
| **ID** | FR-006 |
| **Priority** | Medium |
| **Description** | System shall validate extracted data |
| **Business Rule** | Check for required fields, valid formats, reasonable amounts |
| **Acceptance Criteria** | Invalid invoices flagged for review |

### 5.3 Database Module

#### FR-007: Invoice Record Creation
| Attribute | Value |
|-----------|-------|
| **ID** | FR-007 |
| **Priority** | High |
| **Description** | System shall create database record for each invoice |
| **Data Stored** | Invoice ID, Customer Name, Customer ID, Opportunity Number, Amount, PDF Path, Status, Timestamps |
| **Acceptance Criteria** | Unique record created with all extracted data |

#### FR-008: Duplicate Detection
| Attribute | Value |
|-----------|-------|
| **ID** | FR-008 |
| **Priority** | Medium |
| **Description** | System shall detect potential duplicate invoices |
| **Business Rule** | Flag if same Invoice ID or similar details exist |
| **Acceptance Criteria** | Duplicates are flagged, not auto-processed |

### 5.4 Portal Module

#### FR-009: Invoice Dashboard
| Attribute | Value |
|-----------|-------|
| **ID** | FR-009 |
| **Priority** | High |
| **Description** | Portal shall display dashboard with invoice summary |
| **Features** | Total invoices, Pending approvals, Approved, Rejected counts |
| **Acceptance Criteria** | Real-time statistics displayed |

#### FR-010: Invoice List View
| Attribute | Value |
|-----------|-------|
| **ID** | FR-010 |
| **Priority** | High |
| **Description** | Portal shall display list of all invoices |
| **Features** | Search, Filter, Sort, Pagination |
| **Acceptance Criteria** | Users can find any invoice within 3 clicks |

#### FR-011: Invoice Detail View
| Attribute | Value |
|-----------|-------|
| **ID** | FR-011 |
| **Priority** | High |
| **Description** | Portal shall display invoice details with PDF preview |
| **Features** | All extracted fields, PDF viewer, Edit capability, Action buttons |
| **Acceptance Criteria** | Complete invoice information visible on one page |

#### FR-012: Manual Data Entry
| Attribute | Value |
|-----------|-------|
| **ID** | FR-012 |
| **Priority** | Medium |
| **Description** | Portal shall allow manual correction of extracted data |
| **Business Rule** | Track all changes with user and timestamp |
| **Acceptance Criteria** | Users can edit any field and save changes |

### 5.5 Approval Workflow Module

#### FR-013: Approval Request
| Attribute | Value |
|-----------|-------|
| **ID** | FR-013 |
| **Priority** | High |
| **Description** | Portal shall allow submitting invoice for approval |
| **Business Rule** | Notify approvers via email |
| **Acceptance Criteria** | Approvers receive notification and can act |

#### FR-014: Approve/Reject Actions
| Attribute | Value |
|-----------|-------|
| **ID** | FR-014 |
| **Priority** | High |
| **Description** | Approvers can approve or reject invoices |
| **Business Rule** | Rejection requires comments |
| **Acceptance Criteria** | Status updated, audit trail created |

#### FR-015: Approval Notifications
| Attribute | Value |
|-----------|-------|
| **ID** | FR-015 |
| **Priority** | Medium |
| **Description** | System shall send email notifications for approval actions |
| **Triggers** | New approval request, Approved, Rejected |
| **Acceptance Criteria** | Emails sent within 1 minute of action |

### 5.6 Sage Integration Module

#### FR-016: Sage Data Export
| Attribute | Value |
|-----------|-------|
| **ID** | FR-016 |
| **Priority** | High |
| **Description** | System shall send approved invoices to Sage |
| **Data Sent** | All invoice fields as per Sage API requirements |
| **Acceptance Criteria** | Invoice created in Sage successfully |

#### FR-017: Sage Sync Status
| Attribute | Value |
|-----------|-------|
| **ID** | FR-017 |
| **Priority** | Medium |
| **Description** | System shall track Sage sync status |
| **Statuses** | Pending, Synced, Failed |
| **Acceptance Criteria** | Status visible in portal, failures alerted |

---

## 6. System Architecture

### 6.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SYNCLEDGER by VEDVIX                                   │
│                    System Architecture (Hybrid Model on AWS)                     │
└─────────────────────────────────────────────────────────────────────────────────┘

    ┌──────────────┐         ┌──────────────────────────────────────────────────┐
    │              │         │                    AWS CLOUD                      │
    │   VENDORS    │         │  ┌────────────────────────────────────────────┐  │
    │              │         │  │         SPRING BOOT MAIN APPLICATION        │  │
    └──────┬───────┘         │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
           │                 │  │  │  Email   │  │  Auth    │  │ Approval │  │  │
           │ Send Invoice    │  │  │ Service  │  │ Service  │  │ Service  │  │  │
           ▼                 │  │  │ (Graph)  │  │  (JWT)   │  │          │  │  │
    ┌──────────────┐         │  │  └────┬─────┘  └──────────┘  └────┬─────┘  │  │
    │  Microsoft   │◄────────┼──┼───────┘                          │        │  │
    │   Outlook    │         │  │                                  │        │  │
    │   (M365)     │         │  │  ┌────────────────────────────────────┐   │  │
    └──────────────┘         │  │  │           WEB PORTAL                │   │  │
                             │  │  │  ┌──────────────────────────────┐  │   │  │
                             │  │  │  │     React + TypeScript        │  │   │  │
                             │  │  │  │     Frontend (CloudFront)     │  │   │  │
                             │  │  │  └──────────────────────────────┘  │   │  │
    ┌──────────────┐         │  │  │  ┌──────────────────────────────┐  │   │  │
    │              │         │  │  │  │     Spring Boot (Java 21)     │  │   │  │
    │   APPROVERS  │◄────────┼──┼──┤  │     Backend API (ECS)         │  │   │  │
    │              │         │  │  │  └──────────────────────────────┘  │   │  │
    └──────────────┘         │  │  └────────────────────────────────────┘   │  │
                             │  └────────────────────────────────────────────┘  │
                             │                      │ SQS                       │
                             │  ┌───────────────────▼────────────────────────┐  │
                             │  │         PYTHON PDF MICROSERVICE             │  │
                             │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
                             │  │  │ PyMuPDF  │  │Tesseract │  │  AWS     │  │  │
                             │  │  │ Extract  │  │   OCR    │  │ Textract │  │  │
                             │  │  └──────────┘  └──────────┘  └──────────┘  │  │
                             │  │             (AWS Lambda - Serverless)       │  │
                             │  └────────────────────────────────────────────┘  │
                             │                                                   │
                             │  ┌────────────────────────────────────────────┐  │
                             │  │                DATA LAYER                   │  │
                             │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  │  │
    ┌──────────────┐         │  │  │ AWS RDS  │  │  AWS S3  │  │ AWS SQS  │  │  │
    │              │         │  │  │PostgreSQL│  │  (PDFs)  │  │ (Queue)  │  │  │
    │     SAGE     │◄────────┼──┼──│          │  │          │  │          │  │  │
    │  Accounting  │         │  │  └──────────┘  └──────────┘  └──────────┘  │  │
    │              │         │  └────────────────────────────────────────────┘  │
    └──────────────┘         └──────────────────────────────────────────────────┘
```

### 6.2 Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            COMPONENT ARCHITECTURE                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                               FRONTEND (SPA)                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  Dashboard  │  │  Invoice    │  │  Invoice    │  │  Settings   │            │
│  │  Component  │  │  List       │  │  Detail     │  │  Component  │            │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  Approval   │  │  Reports    │  │  User       │  │  PDF        │            │
│  │  Component  │  │  Component  │  │  Management │  │  Viewer     │            │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼ REST API
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT BACKEND API (Java 21)                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                    SPRING SECURITY + JWT AUTH                             │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                       │                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  Invoice    │  │  Approval   │  │  User/Auth  │  │  Report     │            │
│  │  Controller │  │  Controller │  │  Controller │  │  Controller │            │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                       │                                          │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                           SERVICE LAYER                                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │  Invoice    │  │  Approval   │  │  Email      │  │  Sage       │            │
│  │  Service    │  │  Service    │  │  Service    │  │  Service    │            │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────────────────────┘
                                       │ AWS SQS
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PYTHON PDF MICROSERVICE (FastAPI on Lambda)                   │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐     │
│  │   PDF Extractor     │  │   OCR Processor     │  │   AI Field Parser   │     │
│  │   (PyMuPDF)         │  │   (pytesseract)     │  │   (spaCy/AWS Textract)   │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Integration Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    INTEGRATION ARCHITECTURE (AWS)                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│   Microsoft    │      │  Spring Boot   │      │      Sage      │
│   Graph API    │◄────►│  Main Backend  │◄────►│   REST API     │
│   (Outlook)    │      │  (AWS ECS)     │      │                │
└────────────────┘      └────────┬───────┘      └────────────────┘
        │                        │ AWS SQS               │
        │                        ▼                       │
        │               ┌────────────────┐               │
        │               │  Python PDF    │               │
        │               │  Microservice  │               │
        │               │  (AWS Lambda)  │               │
        │               └────────────────┘               │
        │                        │                       │
        ▼                        ▼                       ▼
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│  - Read Emails │      │  - PyMuPDF     │      │  - Create      │
│  - Download    │      │  - OCR/AI      │      │    Invoices    │
│    Attachments │      │  - AWS Textract│      │  - Update      │
│  - Mark Read   │      │  - Field Parse │      │    Records     │
└────────────────┘      └────────────────┘      └────────────────┘
```

---

## 7. Data Flow

### 7.1 End-to-End Process Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           END-TO-END DATA FLOW                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

   STEP 1              STEP 2              STEP 3              STEP 4
┌──────────┐       ┌──────────┐       ┌──────────┐       ┌──────────┐
│  Vendor  │       │  Email   │       │   PDF    │       │   Data   │
│  Sends   │──────►│ Received │──────►│ Download │──────►│ Extract  │
│  Invoice │       │ Outlook  │       │ & Store  │       │  Fields  │
└──────────┘       └──────────┘       └──────────┘       └──────────┘
                                                                │
                                                                ▼
   STEP 8              STEP 7              STEP 6              STEP 5
┌──────────┐       ┌──────────┐       ┌──────────┐       ┌──────────┐
│  Sync    │       │ Approved │       │  Review  │       │  Create  │
│  to Sage │◄──────│    by    │◄──────│    on    │◄──────│    DB    │
│          │       │ Approver │       │  Portal  │       │  Record  │
└──────────┘       └──────────┘       └──────────┘       └──────────┘
```

### 7.2 Detailed Process Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         DETAILED PROCESS FLOW                                    │
└─────────────────────────────────────────────────────────────────────────────────┘

        START
          │
          ▼
    ┌───────────┐
    │   Email   │
    │  Arrives  │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐     NO      ┌───────────┐
    │  Has PDF  │────────────►│   Skip    │
    │Attachment?│             │   Email   │
    └─────┬─────┘             └───────────┘
          │ YES
          ▼
    ┌───────────┐
    │ Download  │
    │    PDF    │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐
    │  Extract  │
    │   Text    │
    │  (OCR)    │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐
    │  Parse    │
    │  Fields   │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐     NO      ┌───────────┐
    │   All     │────────────►│  Flag for │
    │  Fields   │             │  Manual   │
    │  Found?   │             │  Review   │
    └─────┬─────┘             └─────┬─────┘
          │ YES                     │
          ▼                         │
    ┌───────────┐                   │
    │  Create   │◄──────────────────┘
    │  Invoice  │
    │  Record   │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐
    │  Display  │
    │    on     │
    │  Portal   │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐
    │  Submit   │
    │    for    │
    │  Approval │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐
    │  Notify   │
    │ Approvers │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐     REJECT    ┌───────────┐
    │  Approver │──────────────►│  Return   │
    │  Decision │               │    for    │
    │           │               │  Review   │
    └─────┬─────┘               └───────────┘
          │ APPROVE
          ▼
    ┌───────────┐
    │   Send    │
    │    to     │
    │   Sage    │
    └─────┬─────┘
          │
          ▼
    ┌───────────┐     FAIL      ┌───────────┐
    │   Sage    │──────────────►│   Retry   │
    │   Sync    │               │    or     │
    │  Status   │               │   Alert   │
    └─────┬─────┘               └───────────┘
          │ SUCCESS
          ▼
        END
```

### 7.3 Invoice Status State Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        INVOICE STATUS LIFECYCLE                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

                              ┌───────────────┐
                              │    RECEIVED   │
                              │  (Auto-set)   │
                              └───────┬───────┘
                                      │
                         ┌────────────┴────────────┐
                         │                         │
                         ▼                         ▼
               ┌───────────────┐         ┌───────────────┐
               │   PROCESSED   │         │    FAILED     │
               │ (Data OK)     │         │  (OCR Error)  │
               └───────┬───────┘         └───────┬───────┘
                       │                         │
                       │                         │ (Manual Fix)
                       │                         │
                       ▼                         │
               ┌───────────────┐                 │
               │    PENDING    │◄────────────────┘
               │   REVIEW      │
               └───────┬───────┘
                       │
                       │ (Submit for Approval)
                       ▼
               ┌───────────────┐
               │    PENDING    │
               │   APPROVAL    │
               └───────┬───────┘
                       │
          ┌────────────┴────────────┐
          │                         │
          ▼                         ▼
┌───────────────┐         ┌───────────────┐
│   APPROVED    │         │   REJECTED    │
│               │         │               │
└───────┬───────┘         └───────┬───────┘
        │                         │
        │                         │ (Re-submit)
        ▼                         │
┌───────────────┐                 │
│    SYNCING    │                 │
│   (To Sage)   │                 │
└───────┬───────┘                 │
        │                         │
   ┌────┴────┐                    │
   │         │                    │
   ▼         ▼                    │
┌──────┐  ┌──────┐                │
│SYNCED│  │SYNC  │                │
│      │  │FAILED│────────────────┘
└──────┘  └──────┘
```

---

## 8. Technology Stack

### 8.1 Selected Technology Stack (Hybrid Model - Recommended for Startups)

> **🎯 VEDVIX DECISION:** We use a **Hybrid Architecture** combining Java Spring Boot (main backend with custom auth) and Python FastAPI (PDF processing microservice) on **AWS cloud services**. This approach reduces code complexity, leverages the best tools for each task, and keeps costs low for a startup.

#### Primary Stack: Java Spring Boot + Python Microservice on AWS

| Layer | Technology | Reason |
|-------|------------|--------|
| **Frontend** | React + TypeScript | Modern, performant, large ecosystem |
| **Backend (Main)** | Java 21 + Spring Boot 3.x | Enterprise standard, handles auth, API, workflow |
| **Backend (PDF)** | Python 3.12 + FastAPI | Best-in-class PDF/OCR/AI libraries |
| **Database** | PostgreSQL (AWS RDS) | Open source, reliable, cost-effective |
| **File Storage** | AWS S3 | Highly scalable, pay-per-use pricing |
| **Authentication** | **Custom Spring Security + JWT** | Cost savings vs third-party auth (no Auth0/Cognito fees) |
| **Email** | Microsoft Graph API | Required for Outlook integration |
| **PDF Processing** | AWS Textract + PyMuPDF + pytesseract | Cloud AI + local fallback |
| **Queue** | AWS SQS / RabbitMQ | Reliable, serverless option available |
| **Hosting** | AWS ECS Fargate / EC2 | Container-based, auto-scaling |
| **CI/CD** | GitHub Actions | Free for public repos, affordable for private |

### 7.1.1 Alternative PDF Processing Methods (Budget-Friendly Options)

| Method | Cost | Accuracy | Best For |
|--------|------|----------|----------|
| **AWS Textract** | ~$1.50 per 1000 pages | 95%+ | High-volume, variable formats |
| **Google Cloud Vision** | ~$1.50 per 1000 pages | 95%+ | Alternative to AWS |
| **PyMuPDF + pytesseract** | Free (self-hosted) | 85-90% | Digital PDFs, budget-conscious |
| **Apache Tika** | Free (self-hosted) | 80-85% | Basic extraction needs |
| **pdf.js + Custom Parser** | Free | 75-85% | Simple, consistent formats |
| **LLM-based (GPT-4 Vision)** | ~$0.01-0.03 per page | 90%+ | Complex formats, smart extraction |

> **💡 STARTUP RECOMMENDATION:** Start with **PyMuPDF + pytesseract** (free) for digital PDFs, use **AWS Textract** for scanned documents. This hybrid approach keeps costs near zero while maintaining high accuracy.

#### Alternative Stacks (For Reference)

##### Option B: Full Python Stack (Fastest Development)

| Layer | Technology | Reason |
|-------|------------|--------|
| **Frontend** | React + TypeScript / Angular | Modern SPA frameworks |
| **Backend** | Java 21 + Spring Boot 3.x | Enterprise standard, robust ecosystem |
| **Database** | PostgreSQL / MySQL | Open source, reliable |
| **File Storage** | AWS S3 / MinIO | Flexible cloud storage |
| **Authentication** | Spring Security + JWT | Custom auth service (cost savings) |
| **Email** | Microsoft Graph SDK for Java | Outlook integration |
| **PDF Processing** | Apache PDFBox + Tesseract OCR / AWS Textract | Open source + cloud AI |
| **Queue** | AWS SQS / RabbitMQ | High-throughput messaging |
| **Hosting** | AWS ECS / EC2 | Container orchestration |

##### Option C: Full Python Stack (Fastest Development)

| Layer | Technology | Reason |
|-------|------------|--------|
| **Frontend** | React + TypeScript / Vue.js | Modern SPA frameworks |
| **Backend** | Python 3.12 + FastAPI / Django | Rapid development, excellent AI/ML libraries |
| **Database** | PostgreSQL | Robust, Python-friendly |
| **File Storage** | AWS S3 | Cloud storage |
| **Authentication** | OAuth2 + JWT (Authlib) | Flexible auth |
| **Email** | Microsoft Graph SDK for Python | Outlook integration |
| **PDF Processing** | PyMuPDF + pytesseract / AWS Textract | Native Python libraries |
| **Queue** | Celery + Redis / AWS SQS | Async task processing |
| **Hosting** | AWS Lambda / ECS | Serverless or container deployment |

##### Option D: Node.js Stack

| Layer | Technology | Reason |
|-------|------------|--------|
| **Frontend** | React + TypeScript | Modern, performant |
| **Backend** | Node.js + Express / NestJS | JavaScript full-stack |
| **Database** | PostgreSQL | Robust, free |
| **File Storage** | AWS S3 / MinIO | Cost-effective |
| **Authentication** | Custom JWT / Keycloak | Flexible identity |
| **Email** | Microsoft Graph API | Required for Outlook |
| **PDF Processing** | Tesseract + Custom ML | Open source OCR |
| **Queue** | RabbitMQ / AWS SQS | Message processing |
| **Hosting** | AWS ECS / Lambda | Portable, scalable |

### 8.2 Technology Stack Comparison Matrix

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              TECHNOLOGY STACK COMPARISON MATRIX                                      │
└─────────────────────────────────────────────────────────────────────────────────────────────────────┘

                         │ Hybrid (Spring+Python) │  Spring Boot   │    Python     │   Node.js    │
─────────────────────────┼────────────────────────┼────────────────┼───────────────┼──────────────│
 Development Speed       │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐       │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐    │
 Runtime Performance     │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐⭐    │   ⭐⭐⭐       │   ⭐⭐⭐⭐    │
 Enterprise Readiness    │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐     │   ⭐⭐⭐     │
 M365 Integration        │    ⭐⭐⭐⭐             │    ⭐⭐⭐⭐     │   ⭐⭐⭐⭐     │   ⭐⭐⭐⭐    │
 AI/ML Libraries         │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐       │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐     │
 PDF Processing          │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐     │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐     │
 Developer Availability  │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐⭐   │
 Learning Curve          │    Medium              │    Medium-High │   Low-Medium  │   Low        │
 Community Support       │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐⭐   │
 Long-term Maintenance   │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐     │   ⭐⭐⭐⭐    │
 **Startup Friendly**    │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐       │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐    │
 **Cost Effectiveness**  │    ⭐⭐⭐⭐⭐            │    ⭐⭐⭐⭐     │   ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐    │
─────────────────────────┼────────────────────────┼────────────────┼───────────────┼──────────────│
```
 Long-term Maintenance   │    ⭐⭐⭐⭐⭐    │    ⭐⭐⭐⭐⭐    │   ⭐⭐⭐⭐     │   ⭐⭐⭐⭐    │
─────────────────────────┼────────────────┼────────────────┼───────────────┼──────────────│
```

### 8.3 Detailed Stack Analysis

#### 8.3.1 Java Spring Boot - Detailed Analysis

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        JAVA SPRING BOOT STACK ANALYSIS                           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Architecture:**
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                   │
│   │   React/     │     │  Spring Boot │     │  PostgreSQL  │                   │
│   │   Angular    │────►│  REST API    │────►│  Database    │                   │
│   │   Frontend   │     │  (Java 21)   │     │              │                   │
│   └──────────────┘     └──────┬───────┘     └──────────────┘                   │
│                               │                                                  │
│                               ▼                                                  │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                   │
│   │   Apache     │     │   Spring     │     │  Tesseract/  │                   │
│   │   Kafka      │◄────│   Scheduler  │────►│  AWS Textract│                   │
│   │   (Queue)    │     │  (Email Job) │     │  (OCR)       │                   │
│   └──────────────┘     └──────────────┘     └──────────────┘                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Pros:**
| Advantage | Description |
|-----------|-------------|
| ✅ **Performance** | Excellent runtime performance, JVM optimization |
| ✅ **Enterprise Standard** | Widely adopted in large enterprises, banks, financial institutions |
| ✅ **Strong Typing** | Compile-time error detection, IDE support |
| ✅ **Mature Ecosystem** | Spring Data, Spring Security, Spring Cloud |
| ✅ **Scalability** | Proven for high-load systems |
| ✅ **Talent Pool** | Large pool of experienced Java developers |
| ✅ **Long-term Support** | LTS versions, stable API changes |

**Cons:**
| Disadvantage | Description |
|--------------|-------------|
| ❌ **Verbose Code** | More boilerplate compared to Python |
| ❌ **Slower Development** | Takes longer to implement features |
| ❌ **Memory Footprint** | JVM requires more memory |
| ❌ **Startup Time** | Slower cold start (mitigated by GraalVM native) |
| ❌ **AI/ML Integration** | Fewer native ML libraries than Python |

**Key Dependencies (pom.xml):**
```xml
<dependencies>
    <!-- Core -->
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-boot-starter-data-jpa</dependency>
    <dependency>spring-boot-starter-security</dependency>
    <dependency>spring-boot-starter-oauth2-client</dependency>
    
    <!-- Email -->
    <dependency>microsoft-graph</dependency>
    <dependency>azure-identity</dependency>
    
    <!-- PDF Processing -->
    <dependency>pdfbox</dependency>
    <dependency>tess4j</dependency> <!-- Tesseract wrapper -->
    
    <!-- Queue -->
    <dependency>spring-kafka</dependency>
    
    <!-- Database -->
    <dependency>postgresql</dependency>
</dependencies>
```

**Implementation Complexity: MEDIUM-HIGH**

| Component | Complexity | Effort (Hours) | Notes |
|-----------|------------|----------------|-------|
| Project Setup | Medium | 12 | Spring Initializr, dependencies |
| REST API | Low | 16 | Spring MVC, annotations |
| Database Layer | Low | 12 | Spring Data JPA |
| Email Integration | Medium | 24 | Microsoft Graph SDK |
| PDF Processing | High | 48 | PDFBox + Tesseract setup |
| Authentication | Medium | 16 | Spring Security OAuth2 |
| Background Jobs | Medium | 16 | Spring Scheduler + Kafka |
| **Total Additional** | | **+40 hours** | vs baseline estimate |

---

#### 8.3.2 Python Stack - Detailed Analysis

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          PYTHON STACK ANALYSIS                                   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Architecture:**
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                                                                  │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                   │
│   │   React/     │     │   FastAPI    │     │  PostgreSQL  │                   │
│   │   Vue.js     │────►│   (Python    │────►│  Database    │                   │
│   │   Frontend   │     │    3.12)     │     │              │                   │
│   └──────────────┘     └──────┬───────┘     └──────────────┘                   │
│                               │                                                  │
│                               ▼                                                  │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                   │
│   │   Celery     │     │   Celery     │     │  PyMuPDF +   │                   │
│   │   + Redis    │◄────│   Beat       │────►│  pytesseract │                   │
│   │   (Queue)    │     │  (Scheduler) │     │  (OCR)       │                   │
│   └──────────────┘     └──────────────┘     └──────────────┘                   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Pros:**
| Advantage | Description |
|-----------|-------------|
| ✅ **Rapid Development** | 30-40% faster development than Java |
| ✅ **AI/ML Best-in-Class** | NumPy, pandas, scikit-learn, transformers |
| ✅ **PDF Processing** | Excellent libraries: PyMuPDF, pdfplumber, pdf2image |
| ✅ **OCR Integration** | Native pytesseract, easy AI model integration |
| ✅ **Concise Code** | Less boilerplate, readable syntax |
| ✅ **FastAPI Performance** | Async support, comparable to Node.js |
| ✅ **Data Processing** | Best for invoice data extraction and transformation |

**Cons:**
| Disadvantage | Description |
|--------------|-------------|
| ❌ **Runtime Performance** | Slower than Java/.NET for CPU-intensive tasks |
| ❌ **Concurrency Model** | GIL limitation (mitigated by async/multiprocessing) |
| ❌ **Type Safety** | Dynamic typing can lead to runtime errors |
| ❌ **Enterprise Perception** | Some enterprises prefer Java/.NET |
| ❌ **Dependency Management** | Virtual environments can be complex |

**Key Dependencies (requirements.txt):**
```txt
# Core Framework
fastapi==0.109.0
uvicorn[standard]==0.27.0
pydantic==2.5.0
sqlalchemy==2.0.25
alembic==1.13.0

# Email Integration
msgraph-sdk==1.0.0
azure-identity==1.15.0

# PDF Processing (EXCELLENT for this project)
PyMuPDF==1.23.0          # Fast PDF text extraction
pdfplumber==0.10.0       # Table extraction
pytesseract==0.3.10      # OCR
pdf2image==1.16.0        # PDF to image conversion
Pillow==10.2.0           # Image processing

# AI/ML for Smart Extraction
transformers==4.36.0     # For NLP-based field extraction
spacy==3.7.0             # Named entity recognition
scikit-learn==1.4.0      # ML models

# Async Task Queue
celery==5.3.0
redis==5.0.0

# Database
psycopg2-binary==2.9.9
asyncpg==0.29.0
```

**Implementation Complexity: LOW-MEDIUM**

| Component | Complexity | Effort (Hours) | Notes |
|-----------|------------|----------------|-------|
| Project Setup | Low | 8 | FastAPI scaffold |
| REST API | Low | 12 | FastAPI auto-docs |
| Database Layer | Low | 10 | SQLAlchemy ORM |
| Email Integration | Medium | 20 | Microsoft Graph SDK |
| PDF Processing | **Low** | **32** | **Excellent Python libraries** |
| Authentication | Medium | 14 | OAuth2 + JWT |
| Background Jobs | Low | 12 | Celery + Redis |
| **Total Savings** | | **-60 hours** | vs baseline estimate |

---

### 8.4 Performance Comparison

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PERFORMANCE BENCHMARKS                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

  API Response Time (GET /invoices - 1000 records)
  ════════════════════════════════════════════════════════════════════════════════
  
  Spring Boot 3    ▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  52ms  ⭐ Selected
  FastAPI (Python) ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  62ms
  Node.js Express  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░  70ms
  Django (Python)  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░  95ms

  
  PDF Processing Time (Single Invoice - Text Extraction + OCR)
  ════════════════════════════════════════════════════════════════════════════════
  
  Python PyMuPDF   ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  0.8s  ⭐ Selected
  Java PDFBox      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  1.2s
  Node.js pdf-lib  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░  1.8s


  Memory Usage (Idle Application)
  ════════════════════════════════════════════════════════════════════════════════
  
  Node.js          ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  80MB  ⭐ Lowest
  Python FastAPI   ▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  120MB  ⭐ Selected
  Spring Boot      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░  350MB  ⭐ Selected


  Concurrent Request Handling (Requests/Second)
  ════════════════════════════════════════════════════════════════════════════════
  
  Spring Boot 3    ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░  11,800 rps ⭐
  FastAPI          ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░   9,200 rps
  Node.js          ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░   8,500 rps
```

> **Hybrid Benefit:** Spring Boot handles high-throughput API requests while Python handles CPU-intensive PDF processing. Best of both worlds!

### 8.5 Implementation Complexity by Stack

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    IMPLEMENTATION COMPLEXITY COMPARISON                          │
└─────────────────────────────────────────────────────────────────────────────────┘

  Overall Project Complexity
  ════════════════════════════════════════════════════════════════════════════════
  
  Python + FastAPI  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  LOW        ⭐ Easiest
  Hybrid (Selected) ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  MEDIUM    ⭐ Best Balance
  Node.js           ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  MEDIUM
  Spring Boot Full  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  MEDIUM-HIGH
```

| Component | Hybrid (Selected) | Spring Boot | Python FastAPI | Node.js |
|-----------|-------------------|-------------|----------------|---------|
| **Project Setup** | Medium | Medium-High | Low | Low |
| **REST API** | Low | Low | Very Low | Low |
| **Database ORM** | Low | Medium | Low | Medium |
| **Authentication** | Medium | Medium-High | Medium | Medium |
| **Email (Graph API)** | Medium | Medium | Medium | Medium |
| **PDF Processing** | **Very Low** | Medium-High | **Very Low** | High |
| **OCR Integration** | **Very Low** | High | **Very Low** | High |
| **AI/ML Fields Extraction** | High | High | **Low** | High |
| **Background Jobs** | Medium | Medium | Low | Medium |
| **Sage Integration** | Medium | Medium | Medium | Medium |

### 8.6 Effort Estimation by Stack

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     ESTIMATED HOURS BY TECHNOLOGY STACK                          │
└─────────────────────────────────────────────────────────────────────────────────┘

                            Base      PDF Module    Total      vs Baseline
  ═══════════════════════════════════════════════════════════════════════════════
  
  Hybrid (Spring+Python)    580 hrs   120 hrs       700 hrs    ⭐ RECOMMENDED
  Python + FastAPI          624 hrs   120 hrs       740 hrs    Best for all-Python
  Node.js + Express         624 hrs   200 hrs       824 hrs    JavaScript teams
  Java Spring Boot (Full)   624 hrs   216 hrs       840 hrs    Enterprise only
```

| Stack | Total Hours | Duration | Est. Cost (Outsourced) | Best For |
|-------|-------------|----------|------------------------|----------|
| **Hybrid (Spring+Python)** | **700 hrs** | **12-14 weeks** | **$21,000 - $35,000** | **Startups, vedvix** |
| Python + FastAPI | 740 hrs | 14-16 weeks | $22,000 - $37,000 | Rapid prototyping |
| Node.js | 824 hrs | 16-18 weeks | $25,000 - $41,000 | JavaScript teams |
| Spring Boot (Full) | 840 hrs | 17-19 weeks | $25,000 - $42,000 | Enterprise/Banking |

> **💰 STARTUP COST NOTE:** Using offshore development (Eastern Europe/India: $30-50/hr), the Hybrid stack can be built for **$21,000-$35,000**. This is significantly lower than third-party auth services like Auth0 which would add $1,000-5,000/year in recurring costs.

### 8.7 Recommendation for SyncLedger (vedvix)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    VEDVIX STACK RECOMMENDATION FOR SYNCLEDGER                    │
└─────────────────────────────────────────────────────────────────────────────────┘

  🥇 SELECTED: Hybrid (Java Spring Boot + Python FastAPI) on AWS
     ─────────────────────────────────────────────────────────────────────────────
     • Spring Boot handles: Auth, API, Workflow, Database, Sage Integration
     • Python FastAPI handles: PDF extraction, OCR, AI field recognition
     • Custom JWT auth saves $1,000-5,000/year vs Auth0/Cognito
     • AWS provides startup-friendly pricing (free tier + pay-as-you-go)
     • Reduces total lines of code by using best tool for each job
     • 12-14 weeks to production
     • Estimated cost: $21,000 - $35,000
     
  WHY NOT FULL PYTHON?
     ─────────────────────────────────────────────────────────────────────────────
     • Spring Boot offers better enterprise patterns for auth/workflow
     • Java's type safety reduces bugs in critical business logic
     • Better tooling for complex API development
     
  WHY NOT FULL JAVA?
     ─────────────────────────────────────────────────────────────────────────────
     • Python's PDF/OCR libraries are significantly better
     • AI/ML integration is native in Python
     • Would add 100+ hours to PDF processing module
```

### 8.8 Hybrid Architecture (SELECTED for SyncLedger)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│              SYNCLEDGER HYBRID ARCHITECTURE (VEDVIX APPROACH)                    │
└─────────────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────────┐
│                               AWS CLOUD                                            │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │                 SPRING BOOT MAIN APPLICATION (Java 21)                      │  │
│  │                                                                             │  │
│  │   📦 Modules:                                                               │  │
│  │   • Custom Auth Service (Spring Security + JWT) - NO EXTERNAL AUTH COST    │  │
│  │   • Invoice API (REST endpoints)                                           │  │
│  │   • Approval Workflow Engine                                               │  │
│  │   • User Management                                                        │  │
│  │   • Email Integration (MS Graph)                                           │  │
│  │   • Sage Sync Service                                                      │  │
│  │   • Notification Service                                                   │  │
│  │                                                                             │  │
│  │   Hosting: AWS ECS Fargate / EC2 (t3.small ~$15/month)                     │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                        │                                          │
│                              AWS SQS (Message Queue)                              │
│                                        │                                          │
│  ┌────────────────────────────────────────────────────────────────────────────┐  │
│  │              PYTHON PDF MICROSERVICE (FastAPI)                              │  │
│  │                                                                             │  │
│  │   📦 Capabilities:                                                          │  │
│  │   • PDF Text Extraction (PyMuPDF - FREE)                                   │  │
│  │   • OCR for Scanned PDFs (pytesseract - FREE)                              │  │
│  │   • AI Field Recognition (spaCy, transformers)                             │  │
│  │   • AWS Textract integration (pay-per-use for complex docs)                │  │
│  │   • Confidence Scoring                                                      │  │
│  │                                                                             │  │
│  │   Hosting: AWS Lambda (serverless, pay only when used)                     │  │
│  └────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  AWS RDS     │  │   AWS S3     │  │  AWS SQS     │  │ AWS Textract │         │
│  │  PostgreSQL  │  │  (PDF files) │  │  (Queues)    │  │  (OCR - opt) │         │
│  │  ~$15/month  │  │  ~$5/month   │  │  ~$1/month   │  │  Pay-per-use │         │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘         │
│                                                                                    │
└───────────────────────────────────────────────────────────────────────────────────┘

  Benefits of This Architecture:
  ✅ Custom Auth = $0/year (vs Auth0 at $1,000-5,000/year)
  ✅ Python PDF processing = Best-in-class extraction
  ✅ Spring Boot = Robust, maintainable business logic
  ✅ AWS = Startup credits available, pay-as-you-go
  ✅ Serverless Python = Zero cost when idle
  ✅ Total monthly infrastructure: ~$50-100/month (startup scale)
```

### 8.9 Alternative PDF Processing Methods

For budget-conscious startups, here are various ways to extract data from PDFs:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│              PDF EXTRACTION OPTIONS (From Outlook to System)                     │
└─────────────────────────────────────────────────────────────────────────────────┘

  METHOD 1: Direct Email API Polling (SELECTED) ⭐
  ═══════════════════════════════════════════════════════════════════════════════
  Outlook → MS Graph API → Spring Boot → SQS → Python Service → S3
  
  💰 Cost: FREE (MS Graph API included with M365 license)
  Pros: Simple, reliable, real-time, no additional cost
  Cons: Requires Graph API setup
  
  METHOD 2: Power Automate / Logic Apps Flow
  ═══════════════════════════════════════════════════════════════════════════════
  Outlook → Power Automate → Webhook → Spring Boot → Process
  
  💰 Cost: $15-40/user/month (Power Automate Premium required for HTTP connectors)
  Pros: No-code setup, Microsoft native
  Cons: Additional Microsoft licensing cost, per-user pricing adds up
  
  METHOD 3: Email Forwarding + AWS SES
  ═══════════════════════════════════════════════════════════════════════════════
  Outlook Rule → Forward to AWS SES → Lambda → S3 → Process
  
  💰 Cost: ~$0.10 per 1,000 emails (AWS SES) + Lambda costs (~$0.20/million requests)
  Pros: Serverless, scalable, very cheap at scale
  Cons: Email forwarding may be blocked by IT policy
  
  METHOD 4: IMAP Direct Access
  ═══════════════════════════════════════════════════════════════════════════════
  Outlook → IMAP → Python Script → Process
  
  💰 Cost: FREE (IMAP is included with M365)
  Pros: Works with any email, simple, no cost
  Cons: Less secure, polling delays, Microsoft may deprecate basic auth
```

#### PDF Text Extraction & OCR Pricing Comparison

| Method | Setup Cost | Per-Page Cost | Monthly Cost (500 invoices) | Best For |
|--------|------------|---------------|----------------------------|----------|
| **PyMuPDF + pytesseract** | Free (self-hosted) | $0 | **$0** ⭐ | Digital PDFs, budget startups |
| **AWS Textract** | Free | $1.50/1,000 pages | **$0.75** | Variable formats, scanned docs |
| **Google Cloud Vision** | Free | $1.50/1,000 pages | **$0.75** | Alternative to AWS |
| **Azure Document Intelligence** | Free | $1.50/1,000 pages | **$0.75** | Azure-native shops |
| **GPT-4 Vision (OpenAI)** | Free | $0.01-0.03/page | **$5-15** | Complex extraction, smart parsing |
| **Claude Vision (Anthropic)** | Free | $0.01-0.02/page | **$5-10** | Alternative to GPT-4 |
| **ABBYY Cloud OCR** | Free | $0.02-0.05/page | **$10-25** | Enterprise, high accuracy |
| **Apache Tika** | Free (self-hosted) | $0 | **$0** | Basic extraction only |

> **💡 VEDVIX RECOMMENDATION:** 
> - **Primary:** PyMuPDF + pytesseract (FREE) - handles 80% of digital PDFs
> - **Fallback:** AWS Textract ($1.50/1,000 pages) - for scanned/complex documents
> - **Estimated monthly cost for 500 invoices: $0 - $2**

---

### 8.9.1 DETAILED SETUP GUIDE: Microsoft Graph API for Email Integration

> **📧 This guide explains how to set up Microsoft Graph API from scratch to read invoices from an Outlook mailbox - completely FREE with your M365 license.**

#### Step 1: Register an Application in Azure Active Directory (Entra ID)

**Prerequisites:**
- Microsoft 365 Business/Enterprise subscription
- Azure Active Directory admin access (or ask your IT admin)

**1.1 Go to Azure Portal**
```
1. Open browser and go to: https://portal.azure.com
2. Sign in with your Microsoft 365 admin account
3. Search for "App registrations" in the search bar
4. Click "App registrations" under Services
```

**1.2 Create New App Registration**
```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  Azure Portal > App registrations > + New registration                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Name:                    [SyncLedger]                            │
│                                                                                  │
│  Supported account types:                                                        │
│  ○ Accounts in this organizational directory only (Single tenant) ← SELECT THIS │
│  ○ Accounts in any organizational directory (Multitenant)                       │
│  ○ Personal Microsoft accounts                                                  │
│                                                                                  │
│  Redirect URI (optional):                                                        │
│  Platform: [Web ▼]  URI: [http://localhost:8080/login/oauth2/code/azure]       │
│                                                                                  │
│  [Register]                                                                      │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**1.3 Note Down Important Values (You'll need these later)**
```
After registration, you'll see:
┌─────────────────────────────────────────────────────────────────────────────────┐
│  SyncLedger | Overview                                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Application (client) ID:    a1b2c3d4-e5f6-7890-abcd-ef1234567890  ← COPY THIS │
│  Directory (tenant) ID:      11111111-2222-3333-4444-555555555555  ← COPY THIS │
│  Object ID:                  xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Step 2: Create Client Secret

```
1. In your app registration, click "Certificates & secrets" in left menu
2. Click "+ New client secret"
3. Fill in:
   - Description: "SyncLedger Production Secret"
   - Expires: 24 months (recommended)
4. Click "Add"
5. IMMEDIATELY COPY the "Value" (shown only once!)
```

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  Client secrets                                                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│  Description                  │ Expires      │ Value                            │
│  ─────────────────────────────│──────────────│────────────────────────────────  │
│  SyncLedger Production Secret │ 02/06/2028   │ abc123...xyz789 ← COPY THIS NOW! │
└─────────────────────────────────────────────────────────────────────────────────┘

⚠️ WARNING: The secret value is shown ONLY ONCE. Copy it immediately and store securely!
```

#### Step 3: Configure API Permissions

```
1. Click "API permissions" in left menu
2. Click "+ Add a permission"
3. Select "Microsoft Graph"
4. Select "Application permissions" (for background service)
5. Search and add these permissions:
```

| Permission | Type | Description |
|------------|------|-------------|
| `Mail.Read` | Application | Read mail in all mailboxes |
| `Mail.ReadBasic` | Application | Read basic mail properties |
| `Mail.ReadWrite` | Application | Read and write mail (to mark as read) |
| `User.Read.All` | Application | Read all users' profiles |

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  API permissions                                                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│  API / Permissions name          │ Type        │ Status                         │
│  ────────────────────────────────│─────────────│────────────────────────────────│
│  Microsoft Graph                 │             │                                │
│    Mail.Read                     │ Application │ ⚠️ Not granted (needs admin)   │
│    Mail.ReadWrite                │ Application │ ⚠️ Not granted (needs admin)   │
│    User.Read.All                 │ Application │ ⚠️ Not granted (needs admin)   │
│                                                                                  │
│  [🔘 Grant admin consent for {Your Organization}]  ← CLICK THIS BUTTON          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Click "Grant admin consent" and confirm. Status should change to ✅ Granted.**

#### Step 4: (Optional) Restrict Mailbox Access for Security

By default, the app can read ALL mailboxes. To restrict to only the invoice mailbox:

```powershell
# Run in PowerShell as Admin (requires Exchange Online PowerShell module)

# Install Exchange Online module if not installed
Install-Module -Name ExchangeOnlineManagement

# Connect to Exchange Online
Connect-ExchangeOnline -UserPrincipalName admin@vedvix.com

# Create an application access policy to restrict to specific mailbox
New-ApplicationAccessPolicy `
    -AppId "a1b2c3d4-e5f6-7890-abcd-ef1234567890" `  # Your App ID
    -PolicyScopeGroupId "invoices@vedvix.com" `   # Invoice mailbox
    -AccessRight RestrictAccess `
    -Description "Restrict SyncLedger to invoice mailbox only"

# Verify the policy
Get-ApplicationAccessPolicy | Format-List
```

#### Step 5: Spring Boot Configuration

**Add to `application.yml`:**

```yaml
# =============================================================================
# MICROSOFT GRAPH API CONFIGURATION
# =============================================================================
microsoft:
  graph:
    # Azure AD App Registration values
    client-id: ${AZURE_CLIENT_ID:a1b2c3d4-e5f6-7890-abcd-ef1234567890}
    client-secret: ${AZURE_CLIENT_SECRET:your-secret-here}
    tenant-id: ${AZURE_TENANT_ID:11111111-2222-3333-4444-555555555555}
    
    # The mailbox to monitor for invoices
    mailbox: ${INVOICE_MAILBOX:invoices@vedvix.com}
    
    # How often to check for new emails (in milliseconds)
    poll-interval: 300000  # 5 minutes
    
    # Microsoft Graph API endpoints
    scope: https://graph.microsoft.com/.default
    authority: https://login.microsoftonline.com/${microsoft.graph.tenant-id}
```

**Add dependencies to `pom.xml`:**

```xml
<!-- Microsoft Graph SDK -->
<dependency>
    <groupId>com.microsoft.graph</groupId>
    <artifactId>microsoft-graph</artifactId>
    <version>5.77.0</version>
</dependency>

<!-- Azure Identity for authentication -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.11.0</version>
</dependency>
```

#### Step 6: Java Service Implementation

**Create `GraphApiConfig.java`:**

```java
package com.vedvix.syncledger.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphApiConfig {

    @Value("${microsoft.graph.client-id}")
    private String clientId;

    @Value("${microsoft.graph.client-secret}")
    private String clientSecret;

    @Value("${microsoft.graph.tenant-id}")
    private String tenantId;

    @Bean
    public GraphServiceClient graphServiceClient() {
        // Create credential using client secret
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        // Create Graph client
        return new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
    }
}
```

**Create `EmailService.java`:**

```java
package com.vedvix.syncledger.service;

import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final GraphServiceClient graphClient;
    private final InvoiceProcessingService invoiceProcessingService;

    @Value("${microsoft.graph.mailbox}")
    private String mailboxAddress;

    /**
     * Polls the invoice mailbox every 5 minutes for new emails with PDF attachments.
     * This runs automatically via Spring Scheduler.
     */
    @Scheduled(fixedRateString = "${microsoft.graph.poll-interval:300000}")
    public void pollForNewInvoices() {
        log.info("Checking mailbox {} for new invoices...", mailboxAddress);

        try {
            // Get unread emails with attachments
            var messages = graphClient.users()
                    .byUserId(mailboxAddress)
                    .messages()
                    .get(requestConfig -> {
                        requestConfig.queryParameters.filter = 
                            "isRead eq false and hasAttachments eq true";
                        requestConfig.queryParameters.select = new String[]{
                            "id", "subject", "from", "receivedDateTime", "hasAttachments"
                        };
                        requestConfig.queryParameters.top = 50;
                        requestConfig.queryParameters.orderby = new String[]{"receivedDateTime desc"};
                    });

            if (messages == null || messages.getValue() == null) {
                log.info("No new emails found.");
                return;
            }

            log.info("Found {} unread emails with attachments", messages.getValue().size());

            for (Message message : messages.getValue()) {
                processEmailMessage(message);
            }

        } catch (Exception e) {
            log.error("Error polling mailbox: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single email message - download PDF attachments and queue for processing.
     */
    private void processEmailMessage(Message message) {
        try {
            log.info("Processing email: {} from {}", 
                    message.getSubject(), 
                    message.getFrom().getEmailAddress().getAddress());

            // Get attachments for this message
            var attachments = graphClient.users()
                    .byUserId(mailboxAddress)
                    .messages()
                    .byMessageId(message.getId())
                    .attachments()
                    .get();

            if (attachments == null || attachments.getValue() == null) {
                return;
            }

            for (Attachment attachment : attachments.getValue()) {
                // Only process PDF files
                if (attachment instanceof FileAttachment fileAttachment) {
                    String filename = fileAttachment.getName();
                    
                    if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                        log.info("Found PDF attachment: {}", filename);
                        
                        // Get the file content (base64 encoded)
                        byte[] fileContent = fileAttachment.getContentBytes();
                        
                        // Send to invoice processing service
                        invoiceProcessingService.processInvoicePdf(
                                fileContent,
                                filename,
                                message.getId(),
                                message.getFrom().getEmailAddress().getAddress(),
                                message.getSubject(),
                                message.getReceivedDateTime()
                        );
                    }
                }
            }

            // Mark email as read after processing
            markEmailAsRead(message.getId());

        } catch (Exception e) {
            log.error("Error processing email {}: {}", message.getId(), e.getMessage(), e);
        }
    }

    /**
     * Mark an email as read after processing.
     */
    private void markEmailAsRead(String messageId) {
        try {
            Message updateMessage = new Message();
            updateMessage.setIsRead(true);

            graphClient.users()
                    .byUserId(mailboxAddress)
                    .messages()
                    .byMessageId(messageId)
                    .patch(updateMessage);

            log.info("Marked email {} as read", messageId);
        } catch (Exception e) {
            log.warn("Could not mark email as read: {}", e.getMessage());
        }
    }
}
```

#### Step 7: Testing the Setup

```java
// Add this test endpoint to verify Graph API connection
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class GraphApiTestController {

    private final GraphServiceClient graphClient;

    @Value("${microsoft.graph.mailbox}")
    private String mailboxAddress;

    @GetMapping("/graph-connection")
    public ResponseEntity<Map<String, Object>> testGraphConnection() {
        try {
            // Try to get mailbox info
            var user = graphClient.users()
                    .byUserId(mailboxAddress)
                    .get();

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "mailbox", mailboxAddress,
                "displayName", user.getDisplayName(),
                "message", "Graph API connection successful!"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }
}
```

**Test with curl:**
```bash
curl http://localhost:8080/api/test/graph-connection
```

**Expected Response:**
```json
{
  "status": "SUCCESS",
  "mailbox": "invoices@vedvix.com",
  "displayName": "Invoice Inbox",
  "message": "Graph API connection successful!"
}
```

---

### 8.9.2 DETAILED SETUP GUIDE: PyMuPDF + pytesseract for PDF Extraction

> **📄 This guide explains how to set up FREE PDF text extraction using PyMuPDF and pytesseract OCR in the Python microservice.**

#### Overview: How It Works

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    PDF EXTRACTION PIPELINE (FREE)                                │
└─────────────────────────────────────────────────────────────────────────────────┘

   PDF File                                                          Extracted
   (Input)                                                           Data (JSON)
      │                                                                   ▲
      ▼                                                                   │
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   PyMuPDF    │────►│  Is text     │────►│   Parse &    │────►│   Return     │
│   Extract    │     │  extractable?│ YES │   Extract    │     │   JSON       │
│   Text       │     │              │     │   Fields     │     │   Response   │
└──────────────┘     └──────┬───────┘     └──────────────┘     └──────────────┘
                           │ NO (scanned PDF)
                           ▼
                    ┌──────────────┐     ┌──────────────┐
                    │  Convert to  │────►│  pytesseract │
                    │  Images      │     │  OCR         │
                    └──────────────┘     └──────────────┘
                                                │
                                                └────► (continues to Parse & Extract)
```

#### Step 1: Install System Dependencies

**On Ubuntu/Debian (AWS EC2, Lambda):**
```bash
# Update package manager
sudo apt-get update

# Install Tesseract OCR engine
sudo apt-get install -y tesseract-ocr

# Install language packs (English + additional if needed)
sudo apt-get install -y tesseract-ocr-eng

# Install poppler-utils for PDF to image conversion
sudo apt-get install -y poppler-utils

# Verify installation
tesseract --version
# Output: tesseract 4.1.1 or higher
```

**On macOS:**
```bash
# Using Homebrew
brew install tesseract
brew install poppler

# Verify
tesseract --version
```

**On Windows:**
```powershell
# Download Tesseract installer from:
# https://github.com/UB-Mannheim/tesseract/wiki

# Install to C:\Program Files\Tesseract-OCR

# Add to PATH environment variable:
# C:\Program Files\Tesseract-OCR

# Verify in PowerShell:
tesseract --version
```

#### Step 2: Create Python Project Structure

```
pdf-microservice/
│
├── requirements.txt          # Python dependencies
├── main.py                   # FastAPI application entry point
├── config.py                 # Configuration settings
│
├── services/
│   ├── __init__.py
│   ├── pdf_extractor.py      # PyMuPDF text extraction
│   ├── ocr_service.py        # Tesseract OCR for scanned PDFs
│   └── field_parser.py       # Invoice field extraction logic
│
├── models/
│   ├── __init__.py
│   └── invoice.py            # Pydantic models
│
├── tests/
│   ├── __init__.py
│   ├── test_pdf_extractor.py
│   └── sample_invoices/      # Test PDF files
│
└── Dockerfile                # For containerization
```

#### Step 3: Install Python Dependencies

**Create `requirements.txt`:**
```txt
# =============================================================================
# PDF MICROSERVICE DEPENDENCIES
# =============================================================================

# Web Framework
fastapi==0.109.0
uvicorn[standard]==0.27.0
python-multipart==0.0.6    # For file uploads

# PDF Processing (FREE - No API costs!)
PyMuPDF==1.23.8           # Fast PDF text extraction (also known as fitz)
pytesseract==0.3.10       # Python wrapper for Tesseract OCR
pdf2image==1.16.3         # Convert PDF pages to images for OCR
Pillow==10.2.0            # Image processing

# Data Validation
pydantic==2.5.3

# Text Processing & Field Extraction
regex==2023.12.25         # Advanced regex for field parsing
python-dateutil==2.8.2    # Date parsing

# Optional: For improved field extraction
spacy==3.7.2              # NLP for entity recognition (optional)

# AWS Integration (for S3 file storage)
boto3==1.34.0

# Logging
structlog==24.1.0

# Testing
pytest==7.4.4
pytest-asyncio==0.23.3
httpx==0.26.0             # For testing FastAPI
```

**Install dependencies:**
```bash
# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Linux/macOS:
source venv/bin/activate
# On Windows:
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Download spaCy model (optional, for improved extraction)
python -m spacy download en_core_web_sm
```

#### Step 4: Implement PDF Extraction Service

**Create `services/pdf_extractor.py`:**

```python
"""
PDF Text Extraction Service using PyMuPDF (fitz)
This is the PRIMARY extraction method - FREE and fast!
"""

import fitz  # PyMuPDF
import logging
from pathlib import Path
from typing import Optional, Tuple
import io

logger = logging.getLogger(__name__)


class PDFExtractor:
    """
    Extracts text from PDF files using PyMuPDF.
    
    PyMuPDF is extremely fast and handles most digital PDFs perfectly.
    For scanned PDFs (image-based), it will return minimal text,
    and we fall back to OCR.
    """
    
    # Minimum characters to consider extraction successful
    MIN_TEXT_LENGTH = 50
    
    def extract_text(self, pdf_content: bytes) -> Tuple[str, float, bool]:
        """
        Extract text from a PDF file.
        
        Args:
            pdf_content: PDF file as bytes
            
        Returns:
            Tuple of (extracted_text, confidence_score, needs_ocr)
        """
        try:
            # Open PDF from bytes
            doc = fitz.open(stream=pdf_content, filetype="pdf")
            
            full_text = ""
            total_pages = len(doc)
            
            logger.info(f"Processing PDF with {total_pages} pages")
            
            for page_num, page in enumerate(doc):
                # Extract text from page
                page_text = page.get_text("text")
                full_text += f"\n--- Page {page_num + 1} ---\n"
                full_text += page_text
                
            doc.close()
            
            # Calculate confidence based on text extraction success
            text_length = len(full_text.strip())
            
            if text_length < self.MIN_TEXT_LENGTH:
                # Very little text extracted - likely a scanned PDF
                logger.info("Minimal text extracted, PDF may be scanned/image-based")
                return full_text, 0.1, True  # needs_ocr = True
            
            # Estimate confidence based on text density
            confidence = min(1.0, text_length / (total_pages * 500))
            
            logger.info(f"Extracted {text_length} characters with {confidence:.2f} confidence")
            
            return full_text, confidence, False  # needs_ocr = False
            
        except Exception as e:
            logger.error(f"Error extracting PDF text: {e}")
            return "", 0.0, True
    
    def extract_text_from_file(self, file_path: str) -> Tuple[str, float, bool]:
        """Extract text from a PDF file path."""
        with open(file_path, "rb") as f:
            return self.extract_text(f.read())
    
    def get_pdf_metadata(self, pdf_content: bytes) -> dict:
        """Extract PDF metadata (author, creation date, etc.)."""
        try:
            doc = fitz.open(stream=pdf_content, filetype="pdf")
            metadata = doc.metadata
            doc.close()
            return metadata or {}
        except Exception as e:
            logger.error(f"Error extracting metadata: {e}")
            return {}
    
    def get_page_count(self, pdf_content: bytes) -> int:
        """Get the number of pages in the PDF."""
        try:
            doc = fitz.open(stream=pdf_content, filetype="pdf")
            count = len(doc)
            doc.close()
            return count
        except Exception:
            return 0
```

#### Step 5: Implement OCR Service for Scanned PDFs

**Create `services/ocr_service.py`:**

```python
"""
OCR Service using pytesseract for scanned/image-based PDFs.
This is the FALLBACK method when PyMuPDF can't extract text.
"""

import pytesseract
from pdf2image import convert_from_bytes
from PIL import Image
import logging
import io
from typing import List, Tuple
import os

logger = logging.getLogger(__name__)


class OCRService:
    """
    Performs OCR on scanned PDFs using Tesseract.
    
    Workflow:
    1. Convert PDF pages to images
    2. Run Tesseract OCR on each image
    3. Combine text from all pages
    """
    
    def __init__(self):
        # Configure Tesseract path if needed (Windows)
        if os.name == 'nt':  # Windows
            tesseract_path = r'C:\Program Files\Tesseract-OCR\tesseract.exe'
            if os.path.exists(tesseract_path):
                pytesseract.pytesseract.tesseract_cmd = tesseract_path
        
        # Verify Tesseract is available
        try:
            pytesseract.get_tesseract_version()
            logger.info("Tesseract OCR initialized successfully")
        except Exception as e:
            logger.error(f"Tesseract not found: {e}")
            raise RuntimeError("Tesseract OCR is not installed or not in PATH")
    
    def extract_text_from_pdf(
        self, 
        pdf_content: bytes,
        dpi: int = 300,
        language: str = "eng"
    ) -> Tuple[str, float]:
        """
        Extract text from a scanned PDF using OCR.
        
        Args:
            pdf_content: PDF file as bytes
            dpi: Resolution for PDF to image conversion (higher = better quality but slower)
            language: Tesseract language code
            
        Returns:
            Tuple of (extracted_text, confidence_score)
        """
        try:
            logger.info("Converting PDF to images for OCR...")
            
            # Convert PDF pages to images
            images = convert_from_bytes(
                pdf_content,
                dpi=dpi,
                fmt='PNG'
            )
            
            logger.info(f"Converted {len(images)} pages to images")
            
            full_text = ""
            total_confidence = 0.0
            
            for i, image in enumerate(images):
                logger.info(f"Running OCR on page {i + 1}/{len(images)}...")
                
                # Preprocess image for better OCR
                processed_image = self._preprocess_image(image)
                
                # Run OCR with detailed output
                ocr_data = pytesseract.image_to_data(
                    processed_image,
                    lang=language,
                    output_type=pytesseract.Output.DICT
                )
                
                # Extract text and calculate confidence
                page_text, page_confidence = self._process_ocr_output(ocr_data)
                
                full_text += f"\n--- Page {i + 1} ---\n"
                full_text += page_text
                total_confidence += page_confidence
            
            # Average confidence across all pages
            avg_confidence = total_confidence / len(images) if images else 0.0
            
            logger.info(f"OCR completed with {avg_confidence:.2f} confidence")
            
            return full_text, avg_confidence
            
        except Exception as e:
            logger.error(f"OCR error: {e}")
            return "", 0.0
    
    def _preprocess_image(self, image: Image.Image) -> Image.Image:
        """
        Preprocess image to improve OCR accuracy.
        
        Techniques:
        - Convert to grayscale
        - Increase contrast
        - Remove noise (optional)
        """
        # Convert to grayscale
        if image.mode != 'L':
            image = image.convert('L')
        
        # You can add more preprocessing here:
        # - Thresholding
        # - Noise removal
        # - Deskewing
        
        return image
    
    def _process_ocr_output(self, ocr_data: dict) -> Tuple[str, float]:
        """
        Process Tesseract output and calculate confidence.
        
        Args:
            ocr_data: Dictionary output from pytesseract.image_to_data
            
        Returns:
            Tuple of (text, confidence)
        """
        words = []
        confidences = []
        
        for i, text in enumerate(ocr_data['text']):
            # Skip empty strings
            if text.strip():
                words.append(text)
                conf = ocr_data['conf'][i]
                if conf > 0:  # -1 means no confidence data
                    confidences.append(conf)
        
        full_text = ' '.join(words)
        avg_confidence = sum(confidences) / len(confidences) if confidences else 0.0
        
        # Normalize confidence to 0-1 scale
        avg_confidence = avg_confidence / 100.0
        
        return full_text, avg_confidence
    
    def extract_text_from_image(
        self, 
        image_content: bytes,
        language: str = "eng"
    ) -> Tuple[str, float]:
        """
        Extract text from a single image file.
        
        Args:
            image_content: Image file as bytes
            language: Tesseract language code
            
        Returns:
            Tuple of (extracted_text, confidence_score)
        """
        try:
            image = Image.open(io.BytesIO(image_content))
            processed_image = self._preprocess_image(image)
            
            ocr_data = pytesseract.image_to_data(
                processed_image,
                lang=language,
                output_type=pytesseract.Output.DICT
            )
            
            return self._process_ocr_output(ocr_data)
            
        except Exception as e:
            logger.error(f"Image OCR error: {e}")
            return "", 0.0
```

#### Step 6: Implement Invoice Field Parser

**Create `services/field_parser.py`:**

```python
"""
Invoice Field Parser - Extracts structured data from raw text.
Uses regex patterns and heuristics to find invoice fields.
"""

import re
import logging
from typing import Optional, Dict, Any
from decimal import Decimal
from datetime import datetime
from dateutil import parser as date_parser

logger = logging.getLogger(__name__)


class InvoiceFieldParser:
    """
    Parses raw invoice text to extract structured fields.
    
    Extracted fields:
    - Invoice Number
    - Customer Name
    - Customer ID
    - Opportunity Number
    - Amount (total)
    - Currency
    - Invoice Date
    - Due Date
    """
    
    # Regex patterns for common invoice fields
    PATTERNS = {
        'invoice_number': [
            r'invoice\s*(?:#|no\.?|number)[:\s]*([A-Z0-9\-]+)',
            r'inv[:\s]*([A-Z0-9\-]+)',
            r'bill\s*(?:#|no\.?|number)[:\s]*([A-Z0-9\-]+)',
            r'(?:^|\s)([A-Z]{2,4}[-\s]?\d{4,10})(?:\s|$)',  # Format: INV-12345
        ],
        'customer_id': [
            r'customer\s*(?:id|#|no\.?|number)[:\s]*([A-Z0-9\-]+)',
            r'client\s*(?:id|#|no\.?)[:\s]*([A-Z0-9\-]+)',
            r'account\s*(?:id|#|no\.?)[:\s]*([A-Z0-9\-]+)',
            r'cust[:\s]*([A-Z0-9\-]+)',
        ],
        'opportunity_number': [
            r'opportunity\s*(?:#|no\.?|number)[:\s]*([A-Z0-9\-]+)',
            r'opp[:\s]*([A-Z0-9\-]+)',
            r'deal\s*(?:#|no\.?)[:\s]*([A-Z0-9\-]+)',
            r'quote\s*(?:#|no\.?)[:\s]*([A-Z0-9\-]+)',
        ],
        'amount': [
            r'total[:\s]*[\$£€]?\s*([\d,]+\.?\d*)',
            r'amount\s*(?:due)?[:\s]*[\$£€]?\s*([\d,]+\.?\d*)',
            r'grand\s*total[:\s]*[\$£€]?\s*([\d,]+\.?\d*)',
            r'balance\s*(?:due)?[:\s]*[\$£€]?\s*([\d,]+\.?\d*)',
            r'[\$£€]\s*([\d,]+\.?\d{2})',  # Currency symbol followed by amount
        ],
        'currency': [
            r'currency[:\s]*([A-Z]{3})',
            r'(USD|EUR|GBP|CAD|AUD)',
        ],
        'invoice_date': [
            r'invoice\s*date[:\s]*(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})',
            r'date[:\s]*(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})',
            r'issued[:\s]*(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})',
        ],
        'due_date': [
            r'due\s*date[:\s]*(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})',
            r'payment\s*due[:\s]*(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4})',
        ],
    }
    
    def parse(self, text: str) -> Dict[str, Any]:
        """
        Parse invoice text and extract structured fields.
        
        Args:
            text: Raw text extracted from invoice PDF
            
        Returns:
            Dictionary with extracted fields and confidence scores
        """
        # Normalize text
        normalized_text = self._normalize_text(text)
        
        result = {
            'invoice_number': self._extract_field('invoice_number', normalized_text),
            'customer_name': self._extract_customer_name(normalized_text),
            'customer_id': self._extract_field('customer_id', normalized_text),
            'opportunity_number': self._extract_field('opportunity_number', normalized_text),
            'amount': self._extract_amount(normalized_text),
            'currency': self._extract_field('currency', normalized_text) or 'USD',
            'invoice_date': self._extract_date('invoice_date', normalized_text),
            'due_date': self._extract_date('due_date', normalized_text),
            'raw_text': text[:5000],  # Store first 5000 chars for reference
        }
        
        # Calculate overall confidence
        result['extraction_confidence'] = self._calculate_confidence(result)
        
        logger.info(f"Parsed invoice: {result['invoice_number']} with confidence {result['extraction_confidence']:.2f}")
        
        return result
    
    def _normalize_text(self, text: str) -> str:
        """Normalize text for better pattern matching."""
        # Convert to lowercase for matching (keep original for customer name)
        text = ' '.join(text.split())  # Normalize whitespace
        return text
    
    def _extract_field(self, field_name: str, text: str) -> Optional[str]:
        """Extract a field using predefined patterns."""
        patterns = self.PATTERNS.get(field_name, [])
        text_lower = text.lower()
        
        for pattern in patterns:
            match = re.search(pattern, text_lower, re.IGNORECASE)
            if match:
                return match.group(1).strip().upper()
        
        return None
    
    def _extract_customer_name(self, text: str) -> Optional[str]:
        """Extract customer/company name using heuristics."""
        patterns = [
            r'(?:bill\s*to|sold\s*to|customer)[:\s]*\n?\s*([A-Z][A-Za-z0-9\s&\.,]+?)(?:\n|$)',
            r'(?:attention|attn)[:\s]*([A-Z][A-Za-z\s]+?)(?:\n|$)',
            r'^([A-Z][A-Z\s&]+(?:LLC|Inc|Corp|Ltd|Company|Co\.)?)(?:\n|$)',
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text, re.MULTILINE | re.IGNORECASE)
            if match:
                name = match.group(1).strip()
                # Clean up the name
                name = re.sub(r'\s+', ' ', name)
                if len(name) > 3 and len(name) < 100:
                    return name
        
        return None
    
    def _extract_amount(self, text: str) -> Optional[Decimal]:
        """Extract the total amount from the invoice."""
        patterns = self.PATTERNS['amount']
        
        amounts = []
        for pattern in patterns:
            matches = re.findall(pattern, text, re.IGNORECASE)
            for match in matches:
                try:
                    # Remove commas and convert to Decimal
                    amount_str = match.replace(',', '')
                    amount = Decimal(amount_str)
                    if amount > 0:
                        amounts.append(amount)
                except:
                    continue
        
        if amounts:
            # Return the largest amount (likely the total)
            return max(amounts)
        
        return None
    
    def _extract_date(self, field_name: str, text: str) -> Optional[str]:
        """Extract and parse a date field."""
        patterns = self.PATTERNS.get(field_name, [])
        
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                try:
                    date_str = match.group(1)
                    parsed_date = date_parser.parse(date_str)
                    return parsed_date.strftime('%Y-%m-%d')
                except:
                    continue
        
        return None
    
    def _calculate_confidence(self, result: Dict[str, Any]) -> float:
        """
        Calculate overall extraction confidence based on fields found.
        
        Weights:
        - invoice_number: 25%
        - customer_name: 20%
        - amount: 30%
        - customer_id: 15%
        - dates: 10%
        """
        score = 0.0
        
        if result.get('invoice_number'):
            score += 0.25
        if result.get('customer_name'):
            score += 0.20
        if result.get('amount'):
            score += 0.30
        if result.get('customer_id'):
            score += 0.15
        if result.get('invoice_date') or result.get('due_date'):
            score += 0.10
        
        return round(score, 2)
```

#### Step 7: Create FastAPI Application

**Create `main.py`:**

```python
"""
PDF Microservice - FastAPI Application
Extracts invoice data from PDF files using PyMuPDF and Tesseract OCR.
"""

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional
import logging
import structlog

from services.pdf_extractor import PDFExtractor
from services.ocr_service import OCRService
from services.field_parser import InvoiceFieldParser

# Configure logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.JSONRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
)

logger = structlog.get_logger()

# Initialize FastAPI app
app = FastAPI(
    title="SyncLedger PDF Microservice",
    description="Extracts invoice data from PDF files using PyMuPDF and OCR",
    version="1.0.0"
)

# Initialize services
pdf_extractor = PDFExtractor()
ocr_service = OCRService()
field_parser = InvoiceFieldParser()


# Response Models
class ExtractionResult(BaseModel):
    success: bool
    invoice_number: Optional[str] = None
    customer_name: Optional[str] = None
    customer_id: Optional[str] = None
    opportunity_number: Optional[str] = None
    amount: Optional[float] = None
    currency: str = "USD"
    invoice_date: Optional[str] = None
    due_date: Optional[str] = None
    extraction_confidence: float
    ocr_used: bool
    page_count: int
    message: str


class HealthResponse(BaseModel):
    status: str
    services: dict


# Endpoints
@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Check service health and dependencies."""
    return HealthResponse(
        status="healthy",
        services={
            "pdf_extractor": "ready",
            "ocr_service": "ready",
            "field_parser": "ready"
        }
    )


@app.post("/extract", response_model=ExtractionResult)
async def extract_invoice_data(file: UploadFile = File(...)):
    """
    Extract invoice data from a PDF file.
    
    Process:
    1. Try PyMuPDF text extraction (fast, free)
    2. If text extraction fails, use OCR (slower but handles scanned PDFs)
    3. Parse extracted text to find invoice fields
    4. Return structured data with confidence score
    """
    logger.info("extract_request", filename=file.filename)
    
    # Validate file type
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=400,
            detail="Only PDF files are supported"
        )
    
    try:
        # Read file content
        pdf_content = await file.read()
        
        # Get page count
        page_count = pdf_extractor.get_page_count(pdf_content)
        
        # Step 1: Try PyMuPDF extraction first (FREE and fast)
        text, confidence, needs_ocr = pdf_extractor.extract_text(pdf_content)
        ocr_used = False
        
        # Step 2: If PyMuPDF didn't get enough text, use OCR
        if needs_ocr:
            logger.info("falling_back_to_ocr", reason="insufficient_text")
            text, confidence = ocr_service.extract_text_from_pdf(pdf_content)
            ocr_used = True
        
        if not text or len(text.strip()) < 10:
            return ExtractionResult(
                success=False,
                extraction_confidence=0.0,
                ocr_used=ocr_used,
                page_count=page_count,
                message="Could not extract text from PDF"
            )
        
        # Step 3: Parse extracted text to find invoice fields
        parsed_data = field_parser.parse(text)
        
        # Step 4: Return structured result
        return ExtractionResult(
            success=True,
            invoice_number=parsed_data.get('invoice_number'),
            customer_name=parsed_data.get('customer_name'),
            customer_id=parsed_data.get('customer_id'),
            opportunity_number=parsed_data.get('opportunity_number'),
            amount=float(parsed_data['amount']) if parsed_data.get('amount') else None,
            currency=parsed_data.get('currency', 'USD'),
            invoice_date=parsed_data.get('invoice_date'),
            due_date=parsed_data.get('due_date'),
            extraction_confidence=parsed_data.get('extraction_confidence', 0.0),
            ocr_used=ocr_used,
            page_count=page_count,
            message="Extraction successful"
        )
        
    except Exception as e:
        logger.error("extraction_error", error=str(e))
        raise HTTPException(
            status_code=500,
            detail=f"Error processing PDF: {str(e)}"
        )


@app.post("/extract/raw")
async def extract_raw_text(file: UploadFile = File(...)):
    """
    Extract raw text from PDF without field parsing.
    Useful for debugging and testing.
    """
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(status_code=400, detail="Only PDF files are supported")
    
    try:
        pdf_content = await file.read()
        text, confidence, needs_ocr = pdf_extractor.extract_text(pdf_content)
        
        if needs_ocr:
            text, confidence = ocr_service.extract_text_from_pdf(pdf_content)
        
        return {
            "success": True,
            "text": text,
            "confidence": confidence,
            "ocr_used": needs_ocr,
            "character_count": len(text)
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Run with: uvicorn main:app --host 0.0.0.0 --port 8000 --reload
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

#### Step 8: Create Dockerfile for Deployment

**Create `Dockerfile`:**

```dockerfile
# =============================================================================
# PDF Microservice Dockerfile
# =============================================================================
FROM python:3.12-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    poppler-utils \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy requirements first for caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

#### Step 9: Testing the Service

**Test with curl:**

```bash
# Start the service
uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# Test health endpoint
curl http://localhost:8000/health

# Test PDF extraction
curl -X POST "http://localhost:8000/extract" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@sample_invoice.pdf"
```

**Expected Response:**
```json
{
  "success": true,
  "invoice_number": "INV-2024-001",
  "customer_name": "Acme Corporation",
  "customer_id": "CUST-12345",
  "opportunity_number": "OPP-2024-100",
  "amount": 15000.00,
  "currency": "USD",
  "invoice_date": "2024-01-15",
  "due_date": "2024-02-15",
  "extraction_confidence": 0.85,
  "ocr_used": false,
  "page_count": 1,
  "message": "Extraction successful"
}
```

#### Cost Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│              TOTAL COST: PyMuPDF + pytesseract SOLUTION                          │
└─────────────────────────────────────────────────────────────────────────────────┘

  Component                              One-Time Cost    Monthly Cost
  ═══════════════════════════════════════════════════════════════════════════════
  
  PyMuPDF (Python library)               $0               $0
  pytesseract (Python wrapper)           $0               $0
  Tesseract OCR (Open source engine)     $0               $0
  pdf2image (PDF conversion)             $0               $0
  FastAPI (Web framework)                $0               $0
  
  AWS Lambda hosting (optional)          $0               ~$1-5 (pay per use)
  EC2 t3.micro (alternative)             $0               ~$8.50
  
  ─────────────────────────────────────────────────────────────────────────────────
  TOTAL                                  $0               $0 - $8.50
  ═══════════════════════════════════════════════════════════════════════════════

  ✅ Compare to AWS Textract: $1.50 per 1,000 pages
  ✅ Compare to Google Vision: $1.50 per 1,000 pages  
  ✅ Compare to ABBYY: $20-50 per 1,000 pages

  💰 SAVINGS for 1,000 invoices/month: $1.50 - $50 saved!
```

---

| Service | AWS (Selected) | Azure | Self-Hosted |
|---------|----------------|-------|-------------|
| **Compute (App)** | ECS Fargate: ~$30/mo | App Service: ~$55/mo | VPS: ~$20/mo |
| **Database** | RDS PostgreSQL: ~$15/mo | Azure SQL: ~$35/mo | Managed: ~$15/mo |
| **File Storage** | S3: ~$5/mo | Blob: ~$5/mo | Local: $0 |
| **OCR Service** | Textract: Pay-per-use | Doc Intel: Pay-per-use | Tesseract: Free |
| **Queue** | SQS: ~$1/mo | Service Bus: ~$10/mo | RabbitMQ: Free |
| **Auth Service** | **Custom: $0** | Azure AD: $0-6/user | Keycloak: Free |
| **Total (Est.)** | **~$50-100/mo** | ~$100-150/mo | ~$35-50/mo |

> **vedvix Choice:** AWS for balance of cost, reliability, and startup credits.

### 7.11 Selected Stack Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SYNCLEDGER TECHNOLOGY STACK (FINAL)                           │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  FRONTEND           │  React 18 + TypeScript + Tailwind CSS                 │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  MAIN BACKEND       │  Java 21 + Spring Boot 3.2 + Spring Security          │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  PDF MICROSERVICE   │  Python 3.12 + FastAPI + PyMuPDF + pytesseract        │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  DATABASE           │  PostgreSQL 16 (AWS RDS)                              │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  FILE STORAGE       │  AWS S3                                               │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  MESSAGE QUEUE      │  AWS SQS                                              │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  AUTHENTICATION     │  Custom Spring Security + JWT (NO THIRD PARTY)        │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  EMAIL INTEGRATION  │  Microsoft Graph API                                  │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  OCR (BACKUP)       │  AWS Textract (for complex scanned docs)              │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  HOSTING            │  AWS ECS Fargate + Lambda                             │
  ├─────────────────────┼───────────────────────────────────────────────────────┤
  │  CI/CD              │  GitHub Actions                                       │
  └─────────────────────┴───────────────────────────────────────────────────────┘
```

---

## 9. Database Design

### 9.1 Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                       ENTITY RELATIONSHIP DIAGRAM                                │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌──────────────────┐         ┌──────────────────┐
│      USERS       │         │     INVOICES     │         │    APPROVALS     │
├──────────────────┤         ├──────────────────┤         ├──────────────────┤
│ PK user_id       │         │ PK invoice_id    │         │ PK approval_id   │
│    email         │◄───┐    │    invoice_no    │    ┌───►│ FK invoice_id    │
│    name          │    │    │    customer_name │    │    │ FK approver_id   │
│    role          │    │    │    customer_id   │    │    │    action        │
│    created_at    │    │    │    opportunity_no│    │    │    comments      │
└──────────────────┘    │    │    amount        │    │    │    created_at    │
                        │    │    pdf_path      │────┘    └──────────────────┘
                        │    │    status        │
┌──────────────────┐    │    │    email_id      │         ┌──────────────────┐
│   EMAIL_LOGS     │    │    │ FK created_by    │────┘    │   AUDIT_LOGS     │
├──────────────────┤    │    │    created_at    │         ├──────────────────┤
│ PK email_id      │◄───┼────│    updated_at    │    ┌───►│ PK log_id        │
│    message_id    │    │    └──────────────────┘    │    │ FK invoice_id    │
│    from_address  │    │                           │    │ FK user_id       │
│    subject       │    │    ┌──────────────────┐   │    │    action        │
│    received_at   │    │    │   SAGE_SYNC      │   │    │    old_value     │
│    processed     │    │    ├──────────────────┤   │    │    new_value     │
│    created_at    │    │    │ PK sync_id       │───┘    │    created_at    │
└──────────────────┘    │    │ FK invoice_id    │         └──────────────────┘
                        │    │    sage_invoice_id│
                        │    │    status        │
                        │    │    error_message │
                        │    │    synced_at     │
                        │    └──────────────────┘
                        │
                        └─── (User who created/modified)
```

### 9.2 Table Definitions

#### 9.2.1 Users Table

```sql
CREATE TABLE Users (
    user_id         UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    email           NVARCHAR(255) NOT NULL UNIQUE,
    name            NVARCHAR(255) NOT NULL,
    role            NVARCHAR(50) NOT NULL,  -- 'Admin', 'Approver', 'Viewer'
    is_active       BIT DEFAULT 1,
    created_at      DATETIME2 DEFAULT GETUTCDATE(),
    updated_at      DATETIME2 DEFAULT GETUTCDATE()
);
```

#### 9.2.2 Invoices Table

```sql
CREATE TABLE Invoices (
    invoice_id      UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    invoice_number  NVARCHAR(100) NULL,          -- Extracted from PDF
    customer_name   NVARCHAR(255) NULL,          -- Extracted from PDF
    customer_id     NVARCHAR(100) NULL,          -- Extracted from PDF
    opportunity_no  NVARCHAR(100) NULL,          -- Extracted from PDF
    amount          DECIMAL(18,2) NULL,          -- Extracted from PDF
    currency        NVARCHAR(10) DEFAULT 'USD',
    pdf_path        NVARCHAR(500) NOT NULL,      -- Path to stored PDF
    pdf_filename    NVARCHAR(255) NOT NULL,
    status          NVARCHAR(50) NOT NULL,       -- See status enum below
    email_id        UNIQUEIDENTIFIER NOT NULL,   -- FK to EmailLogs
    extraction_confidence DECIMAL(5,2) NULL,     -- OCR confidence score
    needs_review    BIT DEFAULT 0,
    created_by      UNIQUEIDENTIFIER NULL,       -- FK to Users (for manual)
    created_at      DATETIME2 DEFAULT GETUTCDATE(),
    updated_at      DATETIME2 DEFAULT GETUTCDATE(),
    
    FOREIGN KEY (email_id) REFERENCES EmailLogs(email_id),
    FOREIGN KEY (created_by) REFERENCES Users(user_id)
);

-- Status Values:
-- 'RECEIVED'         - Email received, not yet processed
-- 'PROCESSING'       - Currently extracting data
-- 'PROCESSED'        - Data extracted successfully
-- 'EXTRACTION_FAILED'- OCR/extraction failed
-- 'PENDING_REVIEW'   - Needs manual review
-- 'PENDING_APPROVAL' - Submitted for approval
-- 'APPROVED'         - Approved by approver
-- 'REJECTED'         - Rejected by approver
-- 'SYNCING'          - Being sent to Sage
-- 'SYNCED'           - Successfully sent to Sage
-- 'SYNC_FAILED'      - Sage sync failed
```

#### 9.2.3 EmailLogs Table

```sql
CREATE TABLE EmailLogs (
    email_id        UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    message_id      NVARCHAR(500) NOT NULL UNIQUE, -- Outlook message ID
    from_address    NVARCHAR(255) NOT NULL,
    subject         NVARCHAR(500) NULL,
    received_at     DATETIME2 NOT NULL,
    processed       BIT DEFAULT 0,
    attachment_count INT DEFAULT 0,
    created_at      DATETIME2 DEFAULT GETUTCDATE()
);
```

#### 9.2.4 Approvals Table

```sql
CREATE TABLE Approvals (
    approval_id     UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    invoice_id      UNIQUEIDENTIFIER NOT NULL,
    approver_id     UNIQUEIDENTIFIER NOT NULL,
    action          NVARCHAR(50) NOT NULL,  -- 'APPROVED', 'REJECTED'
    comments        NVARCHAR(MAX) NULL,
    created_at      DATETIME2 DEFAULT GETUTCDATE(),
    
    FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id),
    FOREIGN KEY (approver_id) REFERENCES Users(user_id)
);
```

#### 9.2.5 SageSync Table

```sql
CREATE TABLE SageSync (
    sync_id         UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    invoice_id      UNIQUEIDENTIFIER NOT NULL,
    sage_invoice_id NVARCHAR(100) NULL,      -- ID returned by Sage
    status          NVARCHAR(50) NOT NULL,   -- 'PENDING','SUCCESS','FAILED'
    request_payload NVARCHAR(MAX) NULL,      -- JSON sent to Sage
    response_payload NVARCHAR(MAX) NULL,     -- JSON response from Sage
    error_message   NVARCHAR(MAX) NULL,
    retry_count     INT DEFAULT 0,
    synced_at       DATETIME2 NULL,
    created_at      DATETIME2 DEFAULT GETUTCDATE(),
    
    FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id)
);
```

#### 9.2.6 AuditLogs Table

```sql
CREATE TABLE AuditLogs (
    log_id          UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    invoice_id      UNIQUEIDENTIFIER NULL,
    user_id         UNIQUEIDENTIFIER NULL,
    action          NVARCHAR(100) NOT NULL,  -- 'CREATE','UPDATE','DELETE','APPROVE','REJECT','SYNC'
    entity_type     NVARCHAR(100) NOT NULL,  -- 'INVOICE','USER','APPROVAL'
    old_value       NVARCHAR(MAX) NULL,      -- JSON of old values
    new_value       NVARCHAR(MAX) NULL,      -- JSON of new values
    ip_address      NVARCHAR(50) NULL,
    user_agent      NVARCHAR(500) NULL,
    created_at      DATETIME2 DEFAULT GETUTCDATE(),
    
    FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id),
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);
```

### 8.3 Database Indexes

```sql
-- Performance indexes
CREATE INDEX IX_Invoices_Status ON Invoices(status);
CREATE INDEX IX_Invoices_CreatedAt ON Invoices(created_at DESC);
CREATE INDEX IX_Invoices_CustomerName ON Invoices(customer_name);
CREATE INDEX IX_EmailLogs_MessageId ON EmailLogs(message_id);
CREATE INDEX IX_EmailLogs_Processed ON EmailLogs(processed);
CREATE INDEX IX_Approvals_InvoiceId ON Approvals(invoice_id);
CREATE INDEX IX_AuditLogs_InvoiceId ON AuditLogs(invoice_id);
CREATE INDEX IX_AuditLogs_CreatedAt ON AuditLogs(created_at DESC);
```

---

## 10. API Specifications

### 9.1 API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/invoices` | List all invoices |
| GET | `/api/invoices/{id}` | Get invoice details |
| PUT | `/api/invoices/{id}` | Update invoice |
| POST | `/api/invoices/{id}/submit` | Submit for approval |
| POST | `/api/invoices/{id}/approve` | Approve invoice |
| POST | `/api/invoices/{id}/reject` | Reject invoice |
| GET | `/api/invoices/{id}/pdf` | Download invoice PDF |
| GET | `/api/dashboard/stats` | Get dashboard statistics |
| POST | `/api/invoices/{id}/sync` | Manual sync to Sage |

### 9.2 API Endpoint Details

#### 9.2.1 List Invoices

```
GET /api/invoices
```

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | int | Page number (default: 1) |
| pageSize | int | Items per page (default: 20, max: 100) |
| status | string | Filter by status |
| search | string | Search in customer name, invoice number |
| sortBy | string | Field to sort by |
| sortOrder | string | 'asc' or 'desc' |

**Response:**
```json
{
  "data": [
    {
      "invoiceId": "guid",
      "invoiceNumber": "INV-2024-001",
      "customerName": "Acme Corp",
      "customerId": "CUST-001",
      "opportunityNumber": "OPP-2024-100",
      "amount": 15000.00,
      "currency": "USD",
      "status": "PENDING_APPROVAL",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "pagination": {
    "currentPage": 1,
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8
  }
}
```

#### 9.2.2 Get Invoice Details

```
GET /api/invoices/{id}
```

**Response:**
```json
{
  "invoiceId": "guid",
  "invoiceNumber": "INV-2024-001",
  "customerName": "Acme Corp",
  "customerId": "CUST-001",
  "opportunityNumber": "OPP-2024-100",
  "amount": 15000.00,
  "currency": "USD",
  "status": "PENDING_APPROVAL",
  "pdfUrl": "/api/invoices/{id}/pdf",
  "needsReview": false,
  "extractionConfidence": 95.5,
  "email": {
    "from": "vendor@example.com",
    "subject": "Invoice INV-2024-001",
    "receivedAt": "2024-01-15T10:00:00Z"
  },
  "approvals": [
    {
      "approver": "John Smith",
      "action": "APPROVED",
      "comments": "Looks good",
      "createdAt": "2024-01-15T14:00:00Z"
    }
  ],
  "sageSync": {
    "status": "SUCCESS",
    "sageInvoiceId": "SAGE-001",
    "syncedAt": "2024-01-15T15:00:00Z"
  },
  "auditTrail": [
    {
      "action": "CREATED",
      "user": "System",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T15:00:00Z"
}
```

#### 9.2.3 Update Invoice

```
PUT /api/invoices/{id}
```

**Request Body:**
```json
{
  "customerName": "Acme Corporation",
  "customerId": "CUST-001",
  "opportunityNumber": "OPP-2024-100",
  "amount": 15500.00
}
```

#### 9.2.4 Approve Invoice

```
POST /api/invoices/{id}/approve
```

**Request Body:**
```json
{
  "comments": "Approved - amount verified"
}
```

#### 9.2.5 Reject Invoice

```
POST /api/invoices/{id}/reject
```

**Request Body:**
```json
{
  "comments": "Amount does not match PO"
}
```

### 9.3 Error Responses

```json
{
  "error": {
    "code": "INVOICE_NOT_FOUND",
    "message": "Invoice with ID {id} not found",
    "details": null
  }
}
```

| HTTP Code | Error Code | Description |
|-----------|------------|-------------|
| 400 | VALIDATION_ERROR | Invalid request data |
| 401 | UNAUTHORIZED | Authentication required |
| 403 | FORBIDDEN | Insufficient permissions |
| 404 | NOT_FOUND | Resource not found |
| 409 | CONFLICT | State conflict (e.g., already approved) |
| 500 | INTERNAL_ERROR | Server error |

---

## 11. User Interface Design

### 10.1 Portal Wireframes

#### 10.1.1 Dashboard

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  🏠 SyncLedger                                          👤 John Doe  ▼     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │     📥      │  │     ⏳      │  │     ✅      │  │     ❌      │        │
│  │     45      │  │     12      │  │     28      │  │      5      │        │
│  │  Received   │  │   Pending   │  │  Approved   │  │  Rejected   │        │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘        │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │  Recent Invoices                                           [View All →]    │ │
│  ├────────────────────────────────────────────────────────────────────────────┤ │
│  │  Invoice #    │ Customer      │ Amount      │ Status          │ Date      │ │
│  │──────────────────────────────────────────────────────────────────────────│ │
│  │  INV-001     │ Acme Corp     │ $15,000.00  │ 🟡 Pending      │ Jan 15    │ │
│  │  INV-002     │ TechStart     │ $8,500.00   │ 🟢 Approved     │ Jan 14    │ │
│  │  INV-003     │ GlobalInc     │ $22,000.00  │ 🔴 Rejected     │ Jan 13    │ │
│  │  INV-004     │ SmallBiz      │ $3,200.00   │ 🟡 Pending      │ Jan 12    │ │
│  │  INV-005     │ BigEnterprise │ $45,000.00  │ 🔵 Synced       │ Jan 11    │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌──────────────────────────────┐  ┌──────────────────────────────────────────┐ │
│  │  📊 This Month               │  │  ⚠️ Needs Attention                      │ │
│  │  ───────────────────────────│  │  ─────────────────────────────────────── │ │
│  │  Total Processed: $245,000  │  │  • 3 invoices need manual review         │ │
│  │  Average: $5,450            │  │  • 2 Sage sync failures                  │ │
│  │  Processing Time: 2.3 hrs   │  │  • 5 approvals pending > 48 hrs          │ │
│  └──────────────────────────────┘  └──────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### 10.1.2 Invoice List View

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  🏠 SyncLedger  >  📋 Invoices                          👤 John Doe  ▼     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │ 🔍 Search invoices...                    │ Status: [All ▼] │ Date: [Any ▼] ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │ □  │ Invoice #    │ Customer        │ Opp #      │ Amount      │ Status   ││
│  │────│──────────────│─────────────────│────────────│─────────────│──────────││
│  │ □  │ INV-2024-001│ Acme Corp       │ OPP-100    │ $15,000.00 │ 🟡 Pending││
│  │ □  │ INV-2024-002│ TechStart Inc   │ OPP-101    │ $8,500.00  │ 🟢 Approved││
│  │ □  │ INV-2024-003│ Global Industries│ OPP-102   │ $22,000.00 │ 🔴 Rejected││
│  │ □  │ INV-2024-004│ SmallBiz LLC    │ OPP-103    │ $3,200.00  │ 🟡 Pending││
│  │ □  │ INV-2024-005│ BigEnterprise   │ OPP-104    │ $45,000.00 │ 🔵 Synced ││
│  │ □  │ INV-2024-006│ StartupXYZ      │ OPP-105    │ $12,750.00 │ ⚠️ Review ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  [◀ Previous]  Page 1 of 8  [Next ▶]                     Showing 1-20 of 150   │
│                                                                                  │
│  Selected: 0  │  [Bulk Approve]  [Bulk Submit]  [Export CSV]                    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### 10.1.3 Invoice Detail View

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  🏠 SyncLedger  >  📋 Invoices  >  INV-2024-001         👤 John Doe  ▼     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌───────────────────────────────────────┐  ┌────────────────────────────────┐ │
│  │  📄 Invoice Details                   │  │  📎 PDF Preview                │ │
│  │  ─────────────────────────────────────│  │  ──────────────────────────────│ │
│  │                                       │  │  ┌────────────────────────────┐│ │
│  │  Invoice Number:  INV-2024-001    ✏️ │  │  │                            ││ │
│  │  Customer Name:   Acme Corporation ✏️ │  │  │     [PDF VIEWER]           ││ │
│  │  Customer ID:     CUST-001        ✏️ │  │  │                            ││ │
│  │  Opportunity #:   OPP-2024-100    ✏️ │  │  │     Invoice                ││ │
│  │  Amount:          $15,000.00      ✏️ │  │  │     ---------              ││ │
│  │  Currency:        USD                │  │  │     Acme Corporation       ││ │
│  │                                       │  │  │     Amount: $15,000.00    ││ │
│  │  Status:          🟡 Pending Approval│  │  │                            ││ │
│  │  Confidence:      95.5%              │  │  │                            ││ │
│  │                                       │  │  └────────────────────────────┘│ │
│  │  ─────────────────────────────────────│  │  [📥 Download PDF] [🔍 Zoom]   │ │
│  │                                       │  └────────────────────────────────┘ │
│  │  📧 Email Info                        │                                      │
│  │  From:    vendor@acme.com            │  ┌────────────────────────────────┐ │
│  │  Subject: Invoice INV-2024-001       │  │  📝 Approval History           │ │
│  │  Received: Jan 15, 2024 10:00 AM     │  │  ──────────────────────────────│ │
│  │                                       │  │  No approvals yet              │ │
│  └───────────────────────────────────────┘  └────────────────────────────────┘ │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────┐│
│  │  💬 Comments                                                                ││
│  │  ┌───────────────────────────────────────────────────────────────────────┐ ││
│  │  │ Add a comment...                                                      │ ││
│  │  └───────────────────────────────────────────────────────────────────────┘ ││
│  └─────────────────────────────────────────────────────────────────────────────┘│
│                                                                                  │
│  [← Back to List]          [Save Changes]  [✅ Approve]  [❌ Reject]           │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 10.2 User Roles and Permissions

> **📌 No Self-Service Sign-Up:** Users cannot register themselves. The **Super Admin** creates all user accounts and assigns one of the following roles.

| Feature | Admin | Approver | Viewer |
|---------|-------|----------|--------|
| View Dashboard | ✅ | ✅ | ✅ |
| View Invoice List | ✅ | ✅ | ✅ |
| View Invoice Details | ✅ | ✅ | ✅ |
| Edit Invoice Data | ✅ | ✅ | ❌ |
| Submit for Approval | ✅ | ✅ | ❌ |
| Approve/Reject | ✅ | ✅ | ❌ |
| Manual Sage Sync | ✅ | ❌ | ❌ |
| User Management (Create/Edit/Deactivate Users) | ✅ | ❌ | ❌ |
| System Settings | ✅ | ❌ | ❌ |

---

## 12. Security & Compliance

### 11.1 Authentication & Authorization (Custom Spring Security)

> **💰 COST SAVING:** Using custom Spring Security + JWT instead of Auth0/Cognito saves $1,000-5,000/year

**User Provisioning (No Sign-Up):** There will be **no self-service sign-up**. A **Super Admin** (Admin with user management permissions) will create user accounts and assign roles from the existing role set (Admin, Approver, Viewer).

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                  SECURITY ARCHITECTURE (Custom JWT Auth)                         │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│    User      │      │   Spring     │      │   Portal     │
│   Browser    │      │   Security   │      │   Backend    │
└──────┬───────┘      └──────┬───────┘      └──────┬───────┘
       │                     │                     │
       │  1. Login Request   │                     │
       │  (email/password)   │                     │
       │─────────────────────────────────────────►│
       │                     │                     │
       │                     │  2. Validate        │
       │                     │◄────────────────────│
       │                     │     Credentials     │
       │                     │                     │
       │  3. Return JWT      │                     │
       │◄────────────────────────────────────────│
       │     Access Token    │                     │
       │     + Refresh Token │                     │
       │                     │                     │
       │  4. API Request     │                     │
       │  (Bearer Token)     │                     │
       │─────────────────────────────────────────►│
       │                     │                     │
       │                     │  5. Validate JWT    │
       │                     │◄────────────────────│
       │                     │                     │
       │  6. Return Data                           │
       │◄────────────────────────────────────────│
```

### 11.2 Security Measures

| Area | Measure | Implementation |
|------|---------|----------------|
| **Authentication** | Custom JWT Auth | Spring Security + JWT (Cost: $0) |
| **Authorization** | Role-based access | Spring Security roles |
| **Data in Transit** | Encryption | TLS 1.3 |
| **Data at Rest** | Encryption | AES-256 (AWS RDS encryption) |
| **API Security** | Rate limiting | Spring Boot rate limiter |
| **File Security** | Secure storage | AWS S3 with pre-signed URLs |
| **Audit Logging** | Complete trail | All actions logged |
| **Session Management** | Secure tokens | JWT with 1-hour expiry, refresh tokens |
| **Password Storage** | BCrypt hashing | Industry standard |

### 11.3 Compliance Considerations

| Requirement | How Addressed |
|-------------|---------------|
| **Data Privacy** | Only authorized users can access invoices |
| **Audit Trail** | Complete history of all actions |
| **Data Retention** | Configurable retention policies |
| **Access Control** | Role-based permissions |
| **Encryption** | All data encrypted in transit and at rest |

---

## 13. Implementation Phases

### 12.1 Phase Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         IMPLEMENTATION ROADMAP                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

  Phase 1                Phase 2                Phase 3                Phase 4
  Foundation             Core Features          Integration            Enhancement
  (4 weeks)              (6 weeks)              (4 weeks)              (2 weeks)
     │                      │                      │                      │
     ▼                      ▼                      ▼                      ▼
┌──────────┐          ┌──────────┐          ┌──────────┐          ┌──────────┐
│ • Setup  │          │ • Email  │          │ • Sage   │          │ • Reports│
│ • Auth   │          │ • PDF    │          │   API    │          │ • Notif. │
│ • Database│         │ • Portal │          │ • Sync   │          │ • Polish │
│ • Basic UI│         │ • Workflow│         │ • Testing│          │ • UAT    │
└──────────┘          └──────────┘          └──────────┘          └──────────┘
     │                      │                      │                      │
     └──────────────────────┴──────────────────────┴──────────────────────┘
                                    │
                                    ▼
                              TOTAL: 16 WEEKS
```

### 12.2 Phase 1: Foundation (Weeks 1-4)

| Week | Tasks | Deliverables |
|------|-------|--------------|
| **Week 1** | Project setup, environment configuration | Dev environment ready |
| | Database design and creation | Database deployed |
| | Spring Security JWT setup | Authentication working |
| **Week 2** | API scaffolding | Basic API structure |
| | User management | User CRUD operations |
| | Basic frontend setup | React app skeleton |
| **Week 3** | Dashboard UI | Dashboard page |
| | Invoice list UI | List view with mock data |
| | Navigation and layout | Complete UI shell |
| **Week 4** | Integration testing | All components connected |
| | Bug fixes | Stable foundation |
| | Documentation | Technical docs updated |

**Phase 1 Exit Criteria:**
- ✅ Users can log in with JWT authentication
- ✅ Basic portal UI is navigable
- ✅ Database is deployed and accessible
- ✅ API endpoints respond correctly

### 12.3 Phase 2: Core Features (Weeks 5-10)

| Week | Tasks | Deliverables |
|------|-------|--------------|
| **Week 5** | Microsoft Graph API integration | Email reading capability |
| | Email monitoring service | Background job running |
| | Email logging | Emails tracked in database |
| **Week 6** | PDF download and storage | PDFs stored in blob storage |
| | Azure Document Intelligence setup | OCR service configured |
| | Text extraction pipeline | Raw text extracted |
| **Week 7** | Field extraction logic | Structured data extracted |
| | Confidence scoring | Extraction quality measured |
| | Manual review flagging | Problem invoices identified |
| **Week 8** | Invoice detail view | Complete detail UI |
| | PDF viewer integration | View PDF in browser |
| | Edit functionality | Users can modify data |
| **Week 9** | Approval workflow | Submit/Approve/Reject flows |
| | Email notifications | Notification service |
| | Status management | Full status lifecycle |
| **Week 10** | Integration testing | End-to-end flows working |
| | Performance optimization | Acceptable response times |
| | Bug fixes | Stable core features |

**Phase 2 Exit Criteria:**
- ✅ Emails are automatically processed
- ✅ PDFs are parsed and data extracted
- ✅ Users can review and edit invoices
- ✅ Approval workflow is functional

### 12.4 Phase 3: Integration (Weeks 11-14)

| Week | Tasks | Deliverables |
|------|-------|--------------|
| **Week 11** | Sage API analysis | Integration spec finalized |
| | Sage data mapping | Field mapping documented |
| | Sage test environment | Test account ready |
| **Week 12** | Sage sync service | Auto-sync on approval |
| | Error handling | Retry logic, alerts |
| | Sync status tracking | Status visible in portal |
| **Week 13** | Full integration testing | End-to-end with Sage |
| | Error scenarios | All edge cases handled |
| | Performance testing | Load testing completed |
| **Week 14** | Security review | Vulnerabilities addressed |
| | Bug fixes | All critical bugs fixed |
| | Documentation | Integration docs complete |

**Phase 3 Exit Criteria:**
- ✅ Approved invoices sync to Sage
- ✅ Sync failures are handled gracefully
- ✅ Full audit trail is available

### 12.5 Phase 4: Enhancement (Weeks 15-16)

| Week | Tasks | Deliverables |
|------|-------|--------------|
| **Week 15** | Reporting dashboard | Reports and analytics |
| | Advanced notifications | Configurable alerts |
| | UI polish | Final design refinements |
| **Week 16** | User acceptance testing | UAT completed |
| | Training materials | User guide, videos |
| | Production deployment | Go-live |

**Phase 4 Exit Criteria:**
- ✅ All UAT test cases pass
- ✅ Users are trained
- ✅ Production deployment successful

### 12.6 Resource Requirements

| Role | Count | Phase 1-2 | Phase 3-4 |
|------|-------|-----------|-----------|
| Project Manager | 1 | Part-time | Part-time |
| Backend Developer | 2 | Full-time | Full-time |
| Frontend Developer | 1 | Full-time | Full-time |
| QA Engineer | 1 | Part-time | Full-time |
| DevOps Engineer | 1 | Part-time | Part-time |
| Business Analyst | 1 | Full-time | Part-time |

---

## 12A. Development Effort Estimation

### 12A.1 Effort Estimation Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      TOTAL PROJECT EFFORT ESTIMATION                             │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────────────┐
  │                                                                            │
  │   TOTAL EFFORT:        640 - 800 Person-Hours                              │
  │   TOTAL DURATION:      16 Weeks (4 Months)                                 │
  │   TEAM SIZE:           4-5 Developers                                      │
  │                                                                            │
  │   ESTIMATED COST:      $64,000 - $120,000 USD                              │
  │   (Based on $100-150/hour blended rate)                                    │
  │                                                                            │
  └────────────────────────────────────────────────────────────────────────────┘
```

### 12A.2 Detailed Module-wise Effort Breakdown

#### Module 1: Project Setup & Infrastructure

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Development environment setup | 8 | Low | Docker, local dev |
| Azure infrastructure provisioning | 16 | Medium | Terraform/ARM templates |
| CI/CD pipeline setup | 16 | Medium | GitHub Actions/Azure DevOps |
| Database setup & migrations | 8 | Low | SQL Server/PostgreSQL |
| **Module Total** | **48** | | |

#### Module 2: Authentication & User Management

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Spring Security JWT setup | 16 | Medium | Custom auth, JWT tokens |
| User roles & permissions | 12 | Medium | RBAC implementation |
| User management UI | 8 | Low | Admin screens |
| Session management | 4 | Low | Token refresh |
| **Module Total** | **40** | | |

#### Module 3: Email Processing Service

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Microsoft Graph API integration | 24 | Medium | Email reading, OAuth |
| Email monitoring background service | 16 | Medium | Scheduled job |
| Attachment download & storage | 12 | Low | Blob storage integration |
| Email logging & tracking | 8 | Low | Database operations |
| Duplicate email detection | 8 | Low | Message ID tracking |
| Error handling & retries | 8 | Medium | Resilience patterns |
| **Module Total** | **76** | | |

#### Module 4: PDF Processing & Data Extraction (CRITICAL)

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Azure Document Intelligence setup | 8 | Low | Service configuration |
| PDF text extraction (digital) | 16 | Medium | Native PDF parsing |
| OCR for scanned PDFs | 16 | Medium | Image-based extraction |
| **AI-based field extraction** | **40** | **High** | **Handle variable formats** |
| Field label recognition | 24 | High | Multiple label variations |
| Value extraction & parsing | 24 | High | Amount, dates, IDs |
| Confidence scoring algorithm | 16 | Medium | Quality assessment |
| Low-confidence flagging | 8 | Low | Review queue |
| Testing with diverse formats | 24 | High | Multiple vendor formats |
| **Module Total** | **176** | | **Most complex module** |

#### Module 5: Web Portal - Frontend

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Project setup (React/Angular) | 8 | Low | Boilerplate, routing |
| Dashboard component | 16 | Medium | Stats, charts |
| Invoice list view | 16 | Medium | Table, filters, search |
| Invoice detail view | 24 | Medium | Form, PDF viewer |
| PDF viewer integration | 12 | Medium | In-browser viewing |
| Manual data editing | 12 | Low | Form validation |
| Approval workflow UI | 16 | Medium | Actions, comments |
| Notifications UI | 8 | Low | Toast, alerts |
| Responsive design | 12 | Medium | Mobile-friendly |
| **Module Total** | **124** | | |

#### Module 6: Web Portal - Backend API

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| API project setup | 8 | Low | Spring Boot / FastAPI |
| Invoice CRUD endpoints | 16 | Low | Standard REST |
| Search & filtering | 12 | Medium | Query optimization |
| Approval workflow logic | 16 | Medium | State machine |
| File download endpoints | 8 | Low | Secure PDF access |
| Dashboard statistics | 12 | Medium | Aggregation queries |
| Audit logging | 12 | Medium | All actions tracked |
| Error handling & validation | 8 | Low | Global handlers |
| **Module Total** | **92** | | |

#### Module 7: Sage Integration

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Sage API research & analysis | 16 | Medium | API documentation |
| Data mapping implementation | 12 | Medium | Field transformations |
| Sync service development | 24 | Medium | Queue-based processing |
| Error handling & retry logic | 12 | Medium | Resilience |
| Sync status tracking | 8 | Low | Status updates |
| Manual retry functionality | 8 | Low | Admin feature |
| **Module Total** | **80** | | |

---

## 12B. Sage Integration - Detailed Data Flow

### 12B.1 Sage Integration Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      SAGE INTEGRATION ARCHITECTURE                               │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────────┐
│                            SyncLedger                                         │
│                                                                                   │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐          │
│  │   Approved      │      │   Sage Sync     │      │   Message       │          │
│  │   Invoice       │─────►│   Service       │─────►│   Queue         │          │
│  │   (Database)    │      │   (Prepares)    │      │   (Azure SB)    │          │
│  └─────────────────┘      └─────────────────┘      └────────┬────────┘          │
│                                                              │                   │
└──────────────────────────────────────────────────────────────┼───────────────────┘
                                                               │
                                                               ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         SAGE SYNC WORKER (Background Job)                         │
│                                                                                   │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐          │
│  │   Pick from     │      │   Transform     │      │   Call Sage     │          │
│  │   Queue         │─────►│   Data          │─────►│   API           │          │
│  └─────────────────┘      └─────────────────┘      └────────┬────────┘          │
│                                                              │                   │
│                           ┌──────────────────────────────────┼───────────────┐   │
│                           │                                  │               │   │
│                           ▼                                  ▼               │   │
│                    ┌─────────────┐                    ┌─────────────┐        │   │
│                    │   SUCCESS   │                    │   FAILURE   │        │   │
│                    │   Handler   │                    │   Handler   │        │   │
│                    └──────┬──────┘                    └──────┬──────┘        │   │
│                           │                                  │               │   │
│                           ▼                                  ▼               │   │
│                    ┌─────────────┐                    ┌─────────────┐        │   │
│                    │ Update DB   │                    │ Retry or    │        │   │
│                    │ Status:     │                    │ Alert Admin │        │   │
│                    │ SYNCED      │                    │             │        │   │
│                    └─────────────┘                    └─────────────┘        │   │
│                                                                              │   │
└──────────────────────────────────────────────────────────────────────────────────┘
                                                               │
                                                               ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                              SAGE ACCOUNTING PORTAL                               │
│                                                                                   │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐          │
│  │   REST API      │      │   Invoice       │      │   Accounting    │          │
│  │   Gateway       │─────►│   Created       │─────►│   Processing    │          │
│  │                 │      │   in Sage       │      │                 │          │
│  └─────────────────┘      └─────────────────┘      └─────────────────┘          │
│                                                                                   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 12B.2 Data Flow - Step by Step

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    SAGE SYNC - DETAILED DATA FLOW                                │
└─────────────────────────────────────────────────────────────────────────────────┘

  STEP 1: APPROVAL TRIGGERS SYNC
  ════════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
  │   Approver      │         │   Invoice       │         │   Sync Queue    │
  │   Clicks        │────────►│   Status =      │────────►│   Message       │
  │   "APPROVE"     │         │   "APPROVED"    │         │   Created       │
  └─────────────────┘         └─────────────────┘         └─────────────────┘
  
  Queue Message Payload:
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │ {                                                                           │
  │   "messageId": "msg-uuid-12345",                                           │
  │   "invoiceId": "inv-uuid-67890",                                           │
  │   "action": "SYNC_TO_SAGE",                                                │
  │   "priority": "normal",                                                    │
  │   "createdAt": "2026-02-05T10:30:00Z",                                     │
  │   "retryCount": 0                                                          │
  │ }                                                                           │
  └─────────────────────────────────────────────────────────────────────────────┘

  STEP 2: WORKER PICKS UP MESSAGE
  ════════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
  │   Sage Sync     │         │   Load Invoice  │         │   Validate      │
  │   Worker        │────────►│   from          │────────►│   Required      │
  │   (Listener)    │         │   Database      │         │   Fields        │
  └─────────────────┘         └─────────────────┘         └─────────────────┘

  STEP 3: DATA TRANSFORMATION
  ════════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────────────────────┐         ┌─────────────────────────────────┐
  │      PORTAL DATA (Source)       │         │       SAGE DATA (Target)        │
  ├─────────────────────────────────┤         ├─────────────────────────────────┤
  │ invoice_number: "INV-2024-001"  │────────►│ InvoiceReference: "INV-2024-001"│
  │ customer_name: "Acme Corp"      │────────►│ CustomerName: "Acme Corp"       │
  │ customer_id: "CUST-001"         │────────►│ CustomerCode: "CUST-001"        │
  │ opportunity_no: "OPP-100"       │────────►│ Reference: "OPP-100"            │
  │ amount: 15000.00                │────────►│ TotalAmount: 15000.00           │
  │ currency: "USD"                 │────────►│ CurrencyCode: "USD"             │
  │ created_at: "2026-02-05"        │────────►│ InvoiceDate: "2026-02-05"       │
  └─────────────────────────────────┘         └─────────────────────────────────┘

  STEP 4: API CALL TO SAGE
  ════════════════════════════════════════════════════════════════════════════════
  
  ┌─────────────────┐                          ┌─────────────────┐
  │   Sync Worker   │   ──── HTTPS POST ────►  │   Sage API      │
  │                 │                          │   /invoices     │
  └─────────────────┘                          └─────────────────┘

  STEP 5: HANDLE RESPONSE
  ════════════════════════════════════════════════════════════════════════════════
  
                              ┌─────────────────┐
                              │  Sage Response  │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
             │ 200/201 OK  │    │ 4xx Error   │    │ 5xx Error   │
             │ SUCCESS     │    │ BAD REQUEST │    │ SERVER ERR  │
             └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
                    │                  │                  │
                    ▼                  ▼                  ▼
             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
             │ Status:     │    │ Status:     │    │ Retry up to │
             │ SYNCED      │    │ SYNC_FAILED │    │ 3 times     │
             │ Store Sage  │    │ Log Error   │    │ Then FAIL   │
             │ Invoice ID  │    │ Alert Admin │    │ Alert Admin │
             └─────────────┘    └─────────────┘    └─────────────┘
```

### 12B.3 Sage API Request/Response Specification

#### Authentication

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SAGE API AUTHENTICATION                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

  1. OAuth 2.0 Client Credentials Flow (Recommended for Server-to-Server)
  
  ┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
  │   Invoice       │  POST   │   Sage OAuth    │  Token  │   Use Token     │
  │   Portal        │────────►│   /oauth/token  │────────►│   for API       │
  │                 │         │                 │         │   Calls         │
  └─────────────────┘         └─────────────────┘         └─────────────────┘
```

**Token Request:**
```http
POST https://api.sage.com/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id={CLIENT_ID}
&client_secret={CLIENT_SECRET}
&scope=invoices.write
```

**Token Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "invoices.write"
}
```

#### Create Invoice Request

```http
POST https://api.sage.com/v3.1/purchase_invoices
Authorization: Bearer {access_token}
Content-Type: application/json
X-Request-Id: {unique-request-id}
```

**Request Payload:**
```json
{
  "purchase_invoice": {
    "contact_id": "CUST-001",
    "contact_name": "Acme Corporation",
    "date": "2026-02-05",
    "due_date": "2026-03-05",
    "reference": "INV-2024-001",
    "vendor_reference": "OPP-2024-100",
    "notes": "Synced from SyncLedger",
    "currency_id": "USD",
    "invoice_lines": [
      {
        "description": "Invoice INV-2024-001 from Acme Corporation",
        "quantity": 1,
        "unit_price": 15000.00,
        "tax_rate_id": "TAX-STANDARD",
        "ledger_account_id": "PURCHASES"
      }
    ],
    "total_amount": 15000.00,
    "external_reference": "PORTAL-inv-uuid-67890"
  }
}
```

#### Success Response (201 Created)

```json
{
  "purchase_invoice": {
    "id": "SAGE-PI-12345",
    "displayed_as": "PI-00123",
    "reference": "INV-2024-001",
    "contact_id": "CUST-001",
    "contact_name": "Acme Corporation",
    "date": "2026-02-05",
    "due_date": "2026-03-05",
    "total_amount": 15000.00,
    "outstanding_amount": 15000.00,
    "currency": {
      "id": "USD",
      "symbol": "$"
    },
    "status": {
      "id": "UNPAID",
      "displayed_as": "Unpaid"
    },
    "created_at": "2026-02-05T10:35:00Z",
    "updated_at": "2026-02-05T10:35:00Z"
  }
}
```

#### Error Response Examples

**400 Bad Request - Validation Error:**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "contact_id",
        "message": "Contact not found with ID: CUST-001"
      },
      {
        "field": "total_amount",
        "message": "Total amount must be greater than 0"
      }
    ]
  }
}
```

**401 Unauthorized:**
```json
{
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid or expired access token"
  }
}
```

**429 Rate Limited:**
```json
{
  "error": {
    "code": "RATE_LIMITED",
    "message": "Too many requests. Please retry after 60 seconds",
    "retry_after": 60
  }
}
```

**500 Server Error:**
```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "An unexpected error occurred. Please try again later",
    "request_id": "req-12345"
  }
}
```

### 12B.4 Complete Data Mapping Table

| Portal Field | Portal DB Column | Transformation | Sage API Field | Sage Data Type |
|--------------|------------------|----------------|----------------|----------------|
| Invoice Number | `invoice_number` | Direct map | `reference` | String (50) |
| Customer Name | `customer_name` | Direct map | `contact_name` | String (200) |
| Customer ID | `customer_id` | Direct map | `contact_id` | String (50) |
| Opportunity No | `opportunity_no` | Direct map | `vendor_reference` | String (50) |
| Amount | `amount` | Direct map | `total_amount` | Decimal (18,2) |
| Currency | `currency` | Map to Sage currency | `currency_id` | String (3) |
| Invoice Date | `created_at` | Format: YYYY-MM-DD | `date` | Date |
| Due Date | Calculated | +30 days from date | `due_date` | Date |
| Portal Invoice ID | `invoice_id` | Direct map | `external_reference` | String (100) |
| Line Description | Computed | Invoice # + Customer | `invoice_lines[0].description` | String (500) |

### 12B.5 Retry & Error Handling Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      RETRY & ERROR HANDLING FLOW                                 │
└─────────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   API Call      │
                              │   to Sage       │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
             │    2xx      │    │    4xx      │    │    5xx      │
             │   SUCCESS   │    │   CLIENT    │    │   SERVER    │
             │             │    │   ERROR     │    │   ERROR     │
             └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
                    │                  │                  │
                    ▼                  ▼                  ▼
             ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
             │ ✅ COMPLETE │    │ ❌ NO RETRY │    │ 🔄 RETRY    │
             │ Update DB   │    │ Log Error   │    │ Exponential │
             │ Status:     │    │ Status:     │    │ Backoff     │
             │ SYNCED      │    │ SYNC_FAILED │    │             │
             └─────────────┘    └─────────────┘    └──────┬──────┘
                                                          │
                                      ┌───────────────────┼───────────────────┐
                                      │                   │                   │
                                      ▼                   ▼                   ▼
                               ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
                               │  Retry 1    │     │  Retry 2    │     │  Retry 3    │
                               │  Wait: 5s   │     │  Wait: 30s  │     │  Wait: 2min │
                               └─────────────┘     └─────────────┘     └──────┬──────┘
                                                                              │
                                                                              ▼
                                                                       ┌─────────────┐
                                                                       │ ❌ GIVE UP  │
                                                                       │ Status:     │
                                                                       │ SYNC_FAILED │
                                                                       │ Alert Admin │
                                                                       └─────────────┘
```

**Retry Policy Configuration:**

| Error Type | Retry? | Max Retries | Backoff Strategy |
|------------|--------|-------------|------------------|
| 400 Bad Request | ❌ No | 0 | N/A - Fix data and re-submit |
| 401 Unauthorized | 🔄 Yes | 1 | Refresh token, then retry |
| 403 Forbidden | ❌ No | 0 | N/A - Permission issue |
| 404 Not Found | ❌ No | 0 | N/A - Invalid endpoint |
| 429 Rate Limited | 🔄 Yes | 3 | Use `retry_after` header |
| 500 Server Error | 🔄 Yes | 3 | Exponential: 5s, 30s, 2min |
| 502 Bad Gateway | 🔄 Yes | 3 | Exponential backoff |
| 503 Unavailable | 🔄 Yes | 3 | Exponential backoff |
| Network Timeout | 🔄 Yes | 3 | Exponential backoff |

### 12B.6 Sync Status Tracking

**Database Record (SageSync Table):**

```json
{
  "sync_id": "sync-uuid-001",
  "invoice_id": "inv-uuid-67890",
  "sage_invoice_id": "SAGE-PI-12345",
  "status": "SUCCESS",
  "request_payload": "{...full request JSON...}",
  "response_payload": "{...full response JSON...}",
  "error_message": null,
  "retry_count": 0,
  "synced_at": "2026-02-05T10:35:00Z",
  "created_at": "2026-02-05T10:30:00Z"
}
```

**Failed Sync Record:**

```json
{
  "sync_id": "sync-uuid-002",
  "invoice_id": "inv-uuid-99999",
  "sage_invoice_id": null,
  "status": "FAILED",
  "request_payload": "{...full request JSON...}",
  "response_payload": "{\"error\": {\"code\": \"VALIDATION_ERROR\"...}}",
  "error_message": "Contact not found with ID: CUST-INVALID",
  "retry_count": 3,
  "synced_at": null,
  "created_at": "2026-02-05T11:00:00Z"
}
```

### 12B.7 Monitoring & Alerts

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SAGE SYNC MONITORING                                     │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                           ADMIN DASHBOARD                                    │
  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
  │  │   Pending   │  │   Synced    │  │   Failed    │  │   Avg Time  │        │
  │  │     12      │  │    145      │  │      3      │  │    2.3s     │        │
  │  │   in queue  │  │  today      │  │   today     │  │  response   │        │
  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
  └─────────────────────────────────────────────────────────────────────────────┘
  
  ALERTS TRIGGERED:
  ═══════════════════════════════════════════════════════════════════════════════
  
  ⚠️  ALERT: Sync failure after 3 retries
      Invoice: INV-2024-099 | Error: Contact not found
      Action: Manual review required
  
  ⚠️  ALERT: High failure rate detected
      Failure rate: 15% (threshold: 5%)
      Action: Check Sage API status
  
  ⚠️  ALERT: Sync queue backlog
      Pending items: 50 (threshold: 20)
      Action: Scale up workers
```

**Alert Configuration:**

| Alert Type | Threshold | Notification |
|------------|-----------|--------------|
| Sync Failure | Any failure after max retries | Email to Admin |
| High Failure Rate | > 5% failures in 1 hour | Email + Slack |
| Queue Backlog | > 20 pending items | Email |
| API Downtime | > 5 consecutive failures | Email + SMS |
| Response Time | > 10 seconds average | Email |

#### Module 8: Testing & Quality Assurance

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Unit test development | 24 | Medium | 70%+ coverage target |
| Integration testing | 24 | Medium | API, database |
| End-to-end testing | 16 | Medium | Playwright/Selenium |
| PDF extraction testing | 24 | High | Multiple formats |
| Performance testing | 12 | Medium | Load testing |
| Security testing | 12 | Medium | OWASP checks |
| UAT support | 16 | Low | User testing |
| **Module Total** | **128** | | |

#### Module 9: Documentation & Training

| Task | Effort (Hours) | Complexity | Notes |
|------|----------------|------------|-------|
| Technical documentation | 16 | Low | API docs, architecture |
| User guide | 12 | Low | How-to guides |
| Training materials | 8 | Low | Videos, walkthroughs |
| **Module Total** | **36** | | |

### 12A.3 Effort Summary by Module

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        EFFORT BY MODULE (HOURS)                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

  Module 4: PDF Processing     ████████████████████████████████████  176 hrs (22%)
  Module 8: Testing            ██████████████████████████            128 hrs (16%)
  Module 5: Frontend           █████████████████████████             124 hrs (16%)
  Module 6: Backend API        ███████████████████                    92 hrs (12%)
  Module 7: Sage Integration   ████████████████                       80 hrs (10%)
  Module 3: Email Processing   ███████████████                        76 hrs (10%)
  Module 1: Infrastructure     ██████████                             48 hrs (6%)
  Module 2: Auth & Users       ████████                               40 hrs (5%)
  Module 9: Documentation      ███████                                36 hrs (4%)
                               ─────────────────────────────────────────────────
                               TOTAL:                                800 hrs
```

### 12A.4 Effort by Phase

| Phase | Duration | Effort (Hours) | Percentage |
|-------|----------|----------------|------------|
| Phase 1: Foundation | 4 weeks | 136 | 17% |
| Phase 2: Core Features | 6 weeks | 400 | 50% |
| Phase 3: Integration | 4 weeks | 172 | 22% |
| Phase 4: Enhancement | 2 weeks | 92 | 11% |
| **Total** | **16 weeks** | **800** | **100%** |

### 12A.5 Cost Estimation

#### Option A: In-House Development

| Role | Hours | Rate ($/hr) | Cost |
|------|-------|-------------|------|
| Senior Backend Developer | 280 | $80 | $22,400 |
| Senior Frontend Developer | 180 | $75 | $13,500 |
| Full Stack Developer | 160 | $70 | $11,200 |
| QA Engineer | 128 | $60 | $7,680 |
| DevOps Engineer | 52 | $85 | $4,420 |
| **Total Labor** | **800** | | **$59,200** |
| Azure Infrastructure (4 months) | | | $1,660 |
| Tools & Licenses | | | $1,000 |
| **Grand Total** | | | **$61,860** |

#### Option B: Outsourced Development

| Region | Rate Range ($/hr) | Estimated Cost |
|--------|-------------------|----------------|
| US/UK/Australia | $120-180 | $96,000 - $144,000 |
| Western Europe | $80-120 | $64,000 - $96,000 |
| Eastern Europe | $50-80 | $40,000 - $64,000 |
| India/Southeast Asia | $30-50 | $24,000 - $40,000 |

#### Option C: Hybrid (Recommended)

| Component | Approach | Cost |
|-----------|----------|------|
| Architecture & Design | In-house/Senior | $8,000 |
| Core Development | Outsourced (Eastern Europe/India) | $25,000 |
| QA & Testing | Outsourced | $6,000 |
| Project Management | In-house | $4,000 |
| Infrastructure | AWS (12 months) | $720 |
| **Total** | | **$43,720** |

> **💡 vedvix Approach:** Using offshore development at $30-50/hr and AWS startup credits, total cost can be reduced to **$25,000-$40,000**.

### 12A.6 Risk-Based Estimation Adjustment

| Risk Factor | Impact | Adjustment |
|-------------|--------|------------|
| PDF format variations more complex than expected | High | +20% |
| Sage API integration challenges | Medium | +10% |
| Scope creep | Medium | +15% |
| Resource availability | Low | +5% |

**Recommended Buffer: 20-30%**

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                  FINAL ESTIMATION WITH BUFFER (STARTUP BUDGET)                   │
└─────────────────────────────────────────────────────────────────────────────────┘

  Base Estimate:          700 hours        │  $21,000 - $35,000
  With 25% Buffer:        875 hours        │  $26,000 - $44,000
  
  Timeline:               12-16 weeks      │  3-4 months
  
  Monthly Infrastructure: ~$50-70/month (AWS)
```

### 12A.7 Estimation Assumptions

1. Team has experience with Java Spring Boot and Python
2. AWS infrastructure with startup credits available
3. Sage API documentation and test environment are accessible
4. Business stakeholders are available for clarifications
5. No major scope changes during development
6. PDF samples from various vendors are available for testing

---

## 14. Testing Strategy

### 13.1 Testing Levels

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            TESTING PYRAMID                                       │
└─────────────────────────────────────────────────────────────────────────────────┘

                              /\
                             /  \
                            / E2E\        End-to-End Tests
                           /Tests \       (Selenium/Playwright)
                          /────────\
                         /Integration\    Integration Tests
                        /    Tests    \   (API, Database)
                       /────────────────\
                      /    Unit Tests    \    Unit Tests
                     /                    \   (xUnit, Jest)
                    ────────────────────────
```

### 13.2 Test Cases Summary

| Category | Test Cases | Priority |
|----------|------------|----------|
| **Email Processing** | Email detection, attachment download, duplicate handling | High |
| **PDF Extraction** | Text extraction, field parsing, confidence scoring | High |
| **Invoice CRUD** | Create, read, update, status changes | High |
| **Approval Workflow** | Submit, approve, reject, notifications | High |
| **Sage Integration** | Sync success, sync failure, retry logic | High |
| **Authentication** | Login, logout, session management | High |
| **Authorization** | Role permissions, access control | Medium |
| **UI/UX** | Responsive design, accessibility | Medium |
| **Performance** | Load testing, response times | Medium |

### 13.3 Sample Test Scenarios

#### Scenario 1: Happy Path - Invoice Processing
```
Given: A vendor sends an email with PDF invoice to invoices@company.com
When: The system processes the email
Then: Invoice data is extracted and stored
And: Invoice appears on the portal with status "Pending Review"
```

#### Scenario 2: Approval Flow
```
Given: An invoice is in "Pending Approval" status
When: An approver clicks "Approve" with comments
Then: Invoice status changes to "Approved"
And: Approver and submitter receive email notification
And: Invoice is queued for Sage sync
```

#### Scenario 3: Sage Sync Failure
```
Given: An approved invoice fails to sync to Sage
When: The sync fails 3 times
Then: Invoice status shows "Sync Failed"
And: Admin receives alert notification
And: Manual retry option is available
```

---

## 15. Deployment & Infrastructure

### 14.1 Deployment Architecture (AWS - Startup Optimized)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    AWS DEPLOYMENT ARCHITECTURE (SYNCLEDGER)                      │
└─────────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────────┐
                    │              AWS ACCOUNT                 │
                    │    (Apply for AWS Activate Startup Credits)
                    └─────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────────────┐
│                              VPC (Virtual Private Cloud)                           │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                    AWS ECS FARGATE (Container Service)                       │ │
│  │  ┌─────────────────────┐     ┌─────────────────────┐                        │ │
│  │  │   Frontend          │     │   Spring Boot API   │                        │ │
│  │  │   (React SPA)       │     │   (Java 21)         │                        │ │
│  │  │   via CloudFront    │     │   0.5 vCPU, 1GB     │                        │ │
│  │  └─────────────────────┘     └─────────────────────┘                        │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐ │
│  │                        AWS LAMBDA (Serverless)                               │ │
│  │  ┌─────────────────────┐     ┌─────────────────────┐                        │ │
│  │  │  Email Processor    │     │  Python PDF Service │                        │ │
│  │  │  (Scheduled/Cron)   │     │  (SQS Trigger)      │                        │ │
│  │  │  Java/Python        │     │  PyMuPDF+Tesseract  │                        │ │
│  │  └─────────────────────┘     └─────────────────────┘                        │ │
│  └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│  │  AWS RDS        │  │   AWS S3        │  │   AWS SQS       │                  │
│  │  PostgreSQL     │  │  (PDF Storage)  │  │  (Message Queue)│                  │
│  │  db.t3.micro    │  │  Standard tier  │  │  Standard       │                  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                  │
│                                                                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│  │  AWS Secrets    │  │  CloudWatch     │  │  AWS Textract   │                  │
│  │  Manager        │  │  (Monitoring)   │  │  (OCR - Optional│                  │
│  │  (Credentials)  │  │  (Logs/Alerts)  │  │  pay-per-use)   │                  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                  │
│                                                                                    │
└───────────────────────────────────────────────────────────────────────────────────┘
```

### 14.2 Estimated AWS Costs (Startup Optimized)

| Resource | SKU/Tier | Monthly Cost (Est.) |
|----------|----------|---------------------|
| ECS Fargate (Spring Boot) | 0.5 vCPU, 1GB | ~$15 |
| Lambda (PDF Service) | Pay-per-invocation | ~$5 |
| RDS PostgreSQL | db.t3.micro (Free Tier eligible) | ~$15 |
| S3 Storage | Standard (10GB) | ~$3 |
| SQS | Standard | ~$1 |
| CloudFront (CDN) | 50GB transfer | ~$5 |
| Secrets Manager | 2 secrets | ~$1 |
| CloudWatch | Basic monitoring | ~$5 |
| **Total (Startup Scale)** | | **~$50-70/month** |

> **💡 AWS Free Tier:** Many services have 12-month free tier. With AWS Activate (startup program), you can get $1,000-$100,000 in credits!

**Cost Comparison:**

| Provider | Monthly Est. | Notes |
|----------|--------------|-------|
| **AWS (Selected)** | **$50-70** | Startup credits available |
| Azure | $150-200 | Higher baseline costs |
| GCP | $60-80 | Good free tier |
| Self-hosted VPS | $40-60 | More maintenance |

### 14.3 CI/CD Pipeline (GitHub Actions - Free)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      CI/CD PIPELINE (GITHUB ACTIONS)                             │
└─────────────────────────────────────────────────────────────────────────────────┘

   Developer          GitHub Actions           AWS
       │                      │                    │
       │  1. Push Code        │                    │
       │─────────────────────►│                    │
       │                      │                    │
       │                      │  2. Build          │
       │                      │  ──────────────    │
       │                      │  - Maven/Gradle    │
       │                      │  - npm build       │
       │                      │  - Unit Tests      │
       │                      │                    │
       │                      │  3. Test           │
       │                      │  ──────────────    │
       │                      │  - Integration     │
       │                      │  - Code Coverage   │
       │                      │                    │
       │                      │  4. Build Docker   │
       │                      │  ──────────────    │
       │                      │  - Push to ECR     │
       │                      │                    │
       │                      │  5. Deploy to      │
       │                      │─────────────────►  │
       │                      │     Staging        │
       │                      │     (ECS)          │
       │                      │                    │
       │                      │  6. Deploy to      │
       │                      │─────────────────►  │
       │                      │    Production      │
       │                      │   (Manual Approval)│
       │                      │                    │
```

> **Cost:** GitHub Actions is FREE for public repos, 2000 minutes/month free for private repos.

---

## 15A. AI / LLM Integration Setup Guide

> **🤖 SyncLedger uses OpenAI GPT-4o for intelligent invoice data extraction.** This section walks you through creating an OpenAI account, generating an API key, and configuring SyncLedger to use it — both via environment variables (local dev) and via the Super Admin UI (production).

### 15A.1 How AI Extraction Works in SyncLedger

SyncLedger uses a **3-tier AI extraction pipeline** to maximize accuracy across any invoice format:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     AI EXTRACTION PIPELINE (3-TIER CASCADE)                      │
└─────────────────────────────────────────────────────────────────────────────────┘

   PDF Invoice
       │
       ▼
  ┌──────────────────────┐
  │  Tier 1: OCR + Text  │  ◄── PyMuPDF + pytesseract (FREE, local)
  │  Extraction          │      Extracts raw text from PDF
  └──────────┬───────────┘
             │
             ▼
  ┌──────────────────────┐
  │  Tier 2: GPT-4o      │  ◄── Vision: Sends PDF page images to GPT-4o
  │  Vision Extraction   │      Understands layout, tables, handwriting
  │  (if enabled)        │      Best accuracy for complex invoices
  └──────────┬───────────┘
             │
             ▼
  ┌──────────────────────┐
  │  Tier 3: GPT-4o      │  ◄── Text+LLM: Sends OCR text to GPT-4o
  │  Text Extraction     │      Extracts structured fields from raw text
  │  (if enabled)        │      Good accuracy, lower cost than Vision
  └──────────┬───────────┘
             │
             ▼
  ┌──────────────────────┐
  │  Cross-Validation    │  ◄── Compares Vision vs Text vs Regex results
  │  & Confidence Score  │      Picks highest-confidence values
  └──────────┬───────────┘
             │
             ▼
  ┌──────────────────────┐
  │  Fallback: Regex     │  ◄── Rule-based pattern matching (FREE)
  │  Field Parser        │      Works without API key, limited accuracy
  └──────────────────────┘
```

| Tier | Method | Requires API Key | Cost per Page | Accuracy |
|------|--------|------------------|---------------|----------|
| **Tier 1** | OCR (PyMuPDF + pytesseract) | No | Free | Text extraction only |
| **Tier 2** | GPT-4o Vision | **Yes** | ~$0.01–0.03 | 95%+ (best) |
| **Tier 3** | GPT-4o Text+LLM | **Yes** | ~$0.005–0.01 | 90%+ |
| **Fallback** | Regex parser | No | Free | 70–85% |

> **💡 Without an API key**, SyncLedger still works — it falls back to regex-based extraction. AI tiers are skipped. You'll get lower accuracy for complex or variable invoice layouts.

---

### 15A.2 Step 1 — Create an OpenAI Account

1. Go to **[https://platform.openai.com/signup](https://platform.openai.com/signup)**
2. Click **"Sign up"** — you can use Google, Microsoft, or Apple SSO, or create with email
3. Verify your email address if prompted
4. Complete phone verification (required by OpenAI)
5. You now have an OpenAI Platform account

---

### 15A.3 Step 2 — Add Billing & Set a Spending Limit

> **⚠️ IMPORTANT:** OpenAI API access requires a paid account with billing enabled. Free-tier accounts do NOT have API access to GPT-4o models.

1. Go to **[https://platform.openai.com/settings/organization/billing/overview](https://platform.openai.com/settings/organization/billing/overview)**
2. Click **"Add payment method"** → enter a credit/debit card
3. Add an initial credit balance (minimum $5 recommended to start)
4. Set a **monthly spending limit** to avoid surprise charges:
   - Go to **Settings → Organization → Limits**
   - Set a **hard limit** (API stops working when reached) — e.g., `$20`
   - Set a **soft limit** (email notification when reached) — e.g., `$10`

**Estimated costs for SyncLedger:**

| Monthly Volume | Vision Only | Text+LLM Only | Both (Recommended) |
|----------------|-------------|----------------|---------------------|
| 100 invoices | ~$1–3 | ~$0.50–1 | ~$1.50–4 |
| 500 invoices | ~$5–15 | ~$2.50–5 | ~$7.50–20 |
| 1,000 invoices | ~$10–30 | ~$5–10 | ~$15–40 |
| 5,000 invoices | ~$50–150 | ~$25–50 | ~$75–200 |

---

### 15A.4 Step 3 — Generate an API Key

1. Go to **[https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)**
2. Click **"+ Create new secret key"**
3. Give it a name: e.g., `SyncLedger Production`
4. **Permissions:** Select **"All"** (or at minimum, **"Chat completions: Write"**)
5. Click **"Create secret key"**
6. **Copy the key immediately** — it starts with `sk-` and will only be shown once

```
Example key format:  sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> **🔒 SECURITY:** Treat this key like a password. Never commit it to source code or share it publicly. SyncLedger masks this value in the Super Admin UI.

---

### 15A.5 Step 4 — Configure SyncLedger

There are **two ways** to configure the API key and AI settings:

#### Option A: Environment Variables (Local Development / Docker)

Set these in your `.env` file or `docker-compose.yml`:

```bash
# Required — your OpenAI API key
OPENAI_API_KEY=sk-proj-your-actual-key-here

# Optional — model selection (defaults shown)
OPENAI_VISION_MODEL=gpt-4o          # Model for vision-based extraction
OPENAI_TEXT_MODEL=gpt-4o            # Model for text-based extraction

# Optional — feature toggles (defaults shown)
AI_ENABLE_VISION=true               # Enable/disable vision extraction
AI_ENABLE_TEXT_LLM=true             # Enable/disable text+LLM extraction
AI_ENABLE_VALIDATION=true           # Enable/disable cross-validation
```

For **Docker Compose**, these are already wired in `docker-compose.yml`:

```yaml
pdf-service:
  environment:
    OPENAI_API_KEY: ${OPENAI_API_KEY:-}
    OPENAI_VISION_MODEL: ${OPENAI_VISION_MODEL:-gpt-4o}
    OPENAI_TEXT_MODEL: ${OPENAI_TEXT_MODEL:-gpt-4o}
```

Create a `.env` file in the project root with your key:

```bash
OPENAI_API_KEY=sk-proj-your-actual-key-here
```

Then start with `docker compose up`.

#### Option B: Super Admin Runtime Config UI (Production — Recommended)

In production, manage AI settings through the **Super Admin Console** without redeploying:

1. Log in as a **Super Admin** user
2. Navigate to **Admin → Configuration**
3. Expand the **"AI / LLM Integration"** category (indigo icon)
4. Configure these settings:

| Config Key | Description | Default | Example |
|------------|-------------|---------|---------|
| `ai.provider` | AI provider name | `openai` | `openai` |
| `ai.openai.api-key` | Your OpenAI API key (masked in UI) | *(empty)* | `sk-proj-xxx...` |
| `ai.openai.vision-model` | Model for vision extraction | `gpt-4o` | `gpt-4o`, `gpt-4o-mini` |
| `ai.openai.text-model` | Model for text extraction | `gpt-4o` | `gpt-4o`, `gpt-4o-mini` |
| `ai.enable-vision` | Enable vision-based extraction | `true` | `true` / `false` |
| `ai.enable-text-llm` | Enable text+LLM extraction | `true` | `true` / `false` |
| `ai.enable-cross-validation` | Enable cross-validation | `true` | `true` / `false` |

5. Click **"Save"** next to each value you change — changes take effect immediately on the next invoice processed

> **💡 Runtime config overrides environment variables.** If both are set, the Super Admin UI value wins. If the UI value is empty, the system falls back to the environment variable.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    CONFIG PRIORITY (HIGHEST TO LOWEST)                           │
└─────────────────────────────────────────────────────────────────────────────────┘

  1. Super Admin Runtime Config (database)     ◄── Highest priority
  2. Environment Variable (docker / OS)        ◄── Fallback
  3. Built-in Default (code)                   ◄── Last resort
```

---

### 15A.6 Step 5 — Verify AI Extraction Is Working

After configuring the API key, verify the pipeline:

1. **Upload a test invoice** through the portal
2. Check the invoice detail page — open the **"Extracted Data"** tab
3. Toggle to **"Raw Extracted"** to see the full AI response
4. If fields are extracted correctly with high confidence → AI is working

**Troubleshooting:**

| Symptom | Cause | Fix |
|---------|-------|-----|
| Fields extracted but low accuracy | Regex fallback is being used (no AI) | Check API key is set and not empty |
| "OPENAI_API_KEY not set" in logs | Key not configured | Set via env var or Super Admin UI |
| 401 / Authentication error | Invalid or expired API key | Generate a new key at platform.openai.com |
| 429 / Rate limit error | Too many requests or billing limit reached | Check spending limits, add credits |
| "model not found" error | Incorrect model name | Use `gpt-4o` or `gpt-4o-mini` |
| Vision extraction skipped | `ai.enable-vision` is `false` | Enable in runtime config |

---

### 15A.7 Model Selection Guide

| Model | Speed | Cost | Best For |
|-------|-------|------|----------|
| **gpt-4o** | Medium | Higher | Best accuracy, complex invoices, handwritten notes |
| **gpt-4o-mini** | Fast | Lower | Good accuracy, high volume, cost-sensitive |

> **Recommendation:** Start with `gpt-4o` for both vision and text models. Switch to `gpt-4o-mini` if costs are a concern and accuracy is acceptable.

You can set different models for vision vs text extraction — e.g., use `gpt-4o` for vision (where accuracy matters most) and `gpt-4o-mini` for text (where it's a secondary pass).

---

### 15A.8 Cost Optimization Tips

1. **Disable vision if invoices are digital PDFs** — text+LLM is cheaper and nearly as accurate for machine-generated PDFs
2. **Use `gpt-4o-mini`** for text model — saves ~60–80% on text extraction costs
3. **Set spending limits** on your OpenAI account to prevent runaway costs
4. **Monitor usage** at [https://platform.openai.com/usage](https://platform.openai.com/usage)
5. **Disable cross-validation** if you trust a single extraction tier — saves one API call per invoice

---

## 16. Glossary

### 15.1 Business Terms

| Term | Definition |
|------|------------|
| **Invoice** | A commercial document issued by a seller to a buyer |
| **Approval Workflow** | Process where invoices are reviewed and approved/rejected |
| **Sage** | Accounting software used for financial management |
| **Opportunity Number** | Reference number linking invoice to a sales opportunity |
| **Customer ID** | Unique identifier for a customer in the system |

### 15.2 Technical Terms

| Term | Definition |
|------|------------|
| **API** | Application Programming Interface - how systems communicate |
| **OCR** | Optical Character Recognition - converts images to text |
| **JWT** | JSON Web Token - secure token for authentication |
| **S3** | AWS Simple Storage Service - cloud file storage |
| **ECS** | AWS Elastic Container Service - container orchestration |
| **Lambda** | AWS serverless compute service |
| **REST** | Representational State Transfer - API architecture style |
| **SPA** | Single Page Application - modern web app architecture |
| **SSO** | Single Sign-On - one login for multiple applications |
| **Microservice** | Small, independent service focused on one capability |
| **FastAPI** | Modern Python web framework for building APIs |
| **Spring Boot** | Java framework for building production applications |

### 15.3 Status Definitions

| Status | Meaning | Next Actions |
|--------|---------|--------------|
| **RECEIVED** | Email received, not yet processed | Automatic processing |
| **PROCESSING** | Currently extracting data | Wait for completion |
| **PROCESSED** | Data extracted successfully | Review and submit |
| **EXTRACTION_FAILED** | OCR/extraction failed | Manual data entry |
| **PENDING_REVIEW** | Needs manual review | Edit and verify data |
| **PENDING_APPROVAL** | Submitted for approval | Approve or reject |
| **APPROVED** | Approved by approver | Auto-sync to Sage |
| **REJECTED** | Rejected by approver | Review and resubmit |
| **SYNCING** | Being sent to Sage | Wait for completion |
| **SYNCED** | Successfully sent to Sage | Complete |
| **SYNC_FAILED** | Sage sync failed | Retry or investigate |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Feb 5, 2026 | vedvix | Initial draft |
| 2.0 | Feb 6, 2026 | vedvix | Updated to hybrid architecture (Spring Boot + Python), AWS infrastructure, custom auth service, removed Azure/.NET references, startup budget optimization |

---

## Appendices

### Appendix A: Sample Invoice Format

*[To be added: Example invoice PDFs that the system will process]*

### Appendix B: Sage API Documentation

*[To be added: Sage API endpoint specifications]*

### Appendix C: Field Mapping Reference

#### C.1 Field Terminology Mapping

Different invoices may use different terms for the same data. The system must recognize all variations:

| Our System Field | Common Invoice Labels (Any of these) | Database Column | Sage Field |
|------------------|--------------------------------------|-----------------|------------|
| **Invoice Number** | Invoice No, Invoice #, Invoice Number, Inv No, Inv #, **Billing Number**, Bill No, Bill #, Document Number, Reference No | `invoice_number` | InvoiceRef |
| **Customer Name** | Customer Name, Client Name, Bill To, Sold To, Company Name, Buyer, Account Name | `customer_name` | CustomerName |
| **Customer ID** | Customer ID, Customer Code, Client ID, Account No, Account Number, Client Code, Cust ID | `customer_id` | CustomerCode |
| **Opportunity Number** | Opportunity No, Opp #, Opportunity ID, Deal ID, Sales Order, SO Number, PO Reference, Project ID, Quote Reference | `opportunity_no` | Reference |
| **Amount** | Amount, Total Amount, Total, Grand Total, Invoice Total, Net Amount, Amount Due, Balance Due, Total Due, Invoice Amount | `amount` | TotalAmount |

#### C.2 Field Extraction Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    INTELLIGENT FIELD EXTRACTION STRATEGY                         │
└─────────────────────────────────────────────────────────────────────────────────┘

   PDF Invoice (Any Format)
          │
          ▼
   ┌─────────────────┐
   │  OCR / Text     │  ◄── Azure Document Intelligence / AWS Textract
   │  Extraction     │
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐
   │  AI/ML Field    │  ◄── Uses semantic understanding, not fixed positions
   │  Recognition    │      Recognizes field labels and their values
   └────────┬────────┘
            │
            ▼
   ┌─────────────────┐      ┌─────────────────┐
   │  Confidence     │──────│  High Conf.     │──► Auto-process
   │  Scoring        │      │  (>85%)         │
   └────────┬────────┘      └─────────────────┘
            │
            │               ┌─────────────────┐
            └───────────────│  Low Conf.      │──► Flag for Manual Review
                            │  (<85%)         │
                            └─────────────────┘
```

#### C.3 Handling Unknown Formats

| Scenario | System Behavior |
|----------|----------------|
| New vendor format | AI attempts extraction, flags for review if confidence < 85% |
| Missing field | Marks field as "Not Found", flags invoice for manual entry |
| Multiple amounts | Identifies "Total" or "Grand Total" as primary amount |
| Ambiguous labels | Uses context and position to determine meaning |
| Scanned/Image PDF | Uses OCR before extraction |
| Digital/Native PDF | Extracts text directly (faster, more accurate) |

---

## 17. DEVELOPMENT GUIDE: Complete Step-by-Step Implementation

> **🎯 PURPOSE OF THIS SECTION**
> 
> This section provides **complete, copy-paste ready code** and step-by-step instructions that even someone with **zero programming experience** can follow to build the entire Invoice Processing Portal.
> 
> **Architecture:** Java Spring Boot (Backend) + Python FastAPI (PDF Microservice) + React (Frontend)

---

### 16.1 Prerequisites & Development Environment Setup

Before writing any code, you need to set up your computer with the required tools.

#### 16.1.1 Required Software Installation

**What you need to install (in order):**

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         REQUIRED SOFTWARE                                        │
└─────────────────────────────────────────────────────────────────────────────────┘

  1. Java Development Kit (JDK) 21    - For running Spring Boot
  2. Maven 3.9+                       - For building Java projects
  3. Python 3.12+                     - For PDF microservice
  4. Node.js 20+                      - For React frontend
  5. PostgreSQL 16+                   - Database
  6. Docker Desktop                   - For containerization
  7. Git                              - Version control
  8. Visual Studio Code               - Code editor (recommended)
  9. IntelliJ IDEA Community          - Java IDE (recommended)
```

---

#### Step 1: Install Java JDK 21

**Windows:**
```powershell
# Option A: Using winget (Windows Package Manager)
winget install Oracle.JDK.21

# Option B: Manual download
# Go to: https://www.oracle.com/java/technologies/downloads/#java21
# Download Windows x64 Installer
# Run the installer, click Next through all steps
```

**Verify installation:**
```powershell
# Open a NEW PowerShell window and type:
java -version

# Expected output:
# java version "21.0.x" 2024-xx-xx LTS
# Java(TM) SE Runtime Environment (build 21.0.x+xx-xx)
```

---

#### Step 2: Install Maven

**Windows:**
```powershell
# Using winget
winget install Apache.Maven

# OR Manual installation:
# 1. Download from: https://maven.apache.org/download.cgi
# 2. Extract to C:\Program Files\Apache\maven
# 3. Add to PATH environment variable:
#    - Open System Properties > Environment Variables
#    - Edit PATH, add: C:\Program Files\Apache\maven\bin
```

**Verify installation:**
```powershell
mvn -version

# Expected output:
# Apache Maven 3.9.x
# Maven home: C:\Program Files\Apache\maven
# Java version: 21.0.x
```

---

#### Step 3: Install Python 3.12

**Windows:**
```powershell
# Using winget
winget install Python.Python.3.12

# OR download from: https://www.python.org/downloads/
# IMPORTANT: Check "Add Python to PATH" during installation!
```

**Verify installation:**
```powershell
python --version
# Expected: Python 3.12.x

pip --version
# Expected: pip 24.x.x from ...
```

---

#### Step 4: Install Node.js 20

**Windows:**
```powershell
# Using winget
winget install OpenJS.NodeJS.LTS

# OR download from: https://nodejs.org/
# Choose LTS version
```

**Verify installation:**
```powershell
node --version
# Expected: v20.x.x

npm --version
# Expected: 10.x.x
```

---

#### Step 5: Install PostgreSQL 16

**Windows:**
```powershell
# Using winget
winget install PostgreSQL.PostgreSQL

# OR download from: https://www.postgresql.org/download/windows/
# Remember the password you set for 'postgres' user!
```

**After installation, create the database:**
```powershell
# Open PowerShell and connect to PostgreSQL
psql -U postgres

# Enter your password when prompted
# Then run these SQL commands:

CREATE DATABASE invoice_portal;
CREATE USER invoice_user WITH PASSWORD 'YourSecurePassword123!';
GRANT ALL PRIVILEGES ON DATABASE invoice_portal TO invoice_user;
\q
```

---

#### Step 6: Install Docker Desktop

**Windows:**
```powershell
# Download from: https://www.docker.com/products/docker-desktop/
# Run installer
# Restart computer when prompted
# Start Docker Desktop from Start menu
```

**Verify installation:**
```powershell
docker --version
# Expected: Docker version 24.x.x

docker-compose --version
# Expected: Docker Compose version v2.x.x
```

---

#### Step 7: Install Git

**Windows:**
```powershell
winget install Git.Git

# OR download from: https://git-scm.com/download/win
```

**Configure Git (one-time setup):**
```powershell
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

---

#### Step 8: Install VS Code

**Windows:**
```powershell
winget install Microsoft.VisualStudioCode

# After installation, install these extensions:
# - Extension Pack for Java
# - Python
# - Docker
# - GitLens
# - Thunder Client (API testing)
```

---

### 16.2 Project Structure Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           PROJECT FOLDER STRUCTURE                               │
└─────────────────────────────────────────────────────────────────────────────────┘

syncledger/
│
├── backend/                          # Java Spring Boot Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vedvix/syncledger/
│   │   │   │   ├── SyncLedgerApplication.java    # Main entry point
│   │   │   │   ├── config/                          # Configuration classes
│   │   │   │   ├── controller/                      # REST API endpoints
│   │   │   │   ├── service/                         # Business logic
│   │   │   │   ├── repository/                      # Database access
│   │   │   │   ├── model/                           # Data models/entities
│   │   │   │   ├── dto/                             # Data transfer objects
│   │   │   │   └── exception/                       # Error handling
│   │   │   └── resources/
│   │   │       ├── application.yml                  # App configuration
│   │   │       └── application-dev.yml              # Dev environment config
│   │   └── test/                                    # Unit tests
│   └── pom.xml                                      # Maven dependencies
│
├── pdf-service/                      # Python PDF Processing Microservice
│   ├── app/
│   │   ├── main.py                                  # FastAPI entry point
│   │   ├── services/
│   │   │   ├── pdf_extractor.py                     # PDF text extraction
│   │   │   ├── ocr_service.py                       # OCR processing
│   │   │   └── field_extractor.py                   # AI field extraction
│   │   ├── models/
│   │   │   └── schemas.py                           # Pydantic models
│   │   └── utils/
│   │       └── field_patterns.py                    # Field recognition patterns
│   ├── requirements.txt                             # Python dependencies
│   ├── Dockerfile                                   # Container config
│   └── tests/                                       # Unit tests
│
├── frontend/                         # React Frontend Application
│   ├── src/
│   │   ├── components/                              # React components
│   │   ├── pages/                                   # Page components
│   │   ├── services/                                # API calls
│   │   └── App.tsx                                  # Main React app
│   ├── package.json                                 # npm dependencies
│   └── Dockerfile                                   # Container config
│
├── docker-compose.yml                # Run all services together
├── .env                              # Environment variables
└── README.md                         # Project documentation
```

---

### 16.3 PART 1: Java Spring Boot Backend - Complete Implementation

#### 16.3.1 Create the Spring Boot Project

**Step 1: Create project folder**
```powershell
# Open PowerShell and navigate to where you want to create the project
cd C:\Projects

# Create main folder
mkdir syncledger
cd syncledger

# Create backend folder
mkdir backend
cd backend
```

**Step 2: Create pom.xml (Maven configuration)**

Create a file named `pom.xml` in the `backend` folder with this content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <!-- Project Information -->
    <groupId>com.vedvix</groupId>
    <artifactId>syncledger</artifactId>
    <version>1.0.0</version>
    <name>SyncLedger</name>
    <description>Invoice Processing Portal Backend</description>
    
    <!-- Spring Boot Parent - provides default configurations -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
    </parent>
    
    <!-- Java Version -->
    <properties>
        <java.version>21</java.version>
    </properties>
    
    <!-- Dependencies (Libraries we need) -->
    <dependencies>
        
        <!-- Spring Boot Web - For building REST APIs -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Boot Data JPA - For database operations -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- Spring Boot Security - For authentication -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- Spring Boot OAuth2 Client - For Azure AD login -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-client</artifactId>
        </dependency>
        
        <!-- Spring Boot OAuth2 Resource Server - For JWT validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        
        <!-- Spring Boot Validation - For input validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Spring Boot Mail - For sending emails -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        
        <!-- PostgreSQL Driver - Database connection -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Microsoft Graph SDK - For reading Outlook emails -->
        <dependency>
            <groupId>com.microsoft.graph</groupId>
            <artifactId>microsoft-graph</artifactId>
            <version>5.77.0</version>
        </dependency>
        
        <!-- Azure Identity - For Azure AD authentication -->
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-identity</artifactId>
            <version>1.11.1</version>
        </dependency>
        
        <!-- Azure Storage Blob - For storing PDF files -->
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-storage-blob</artifactId>
            <version>12.25.1</version>
        </dependency>
        
        <!-- Lombok - Reduces boilerplate code -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- MapStruct - For object mapping -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>
        
        <!-- OpenAPI/Swagger - For API documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>
        
        <!-- WebClient - For calling PDF microservice -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
    </dependencies>
    
    <!-- Build Configuration -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
    
</project>
```

**Step 3: Create folder structure**
```powershell
# Run these commands in the backend folder

# Create main source folders
mkdir -p src/main/java/com/vedvix/syncledger
mkdir -p src/main/java/com/vedvix/syncledger/config
mkdir -p src/main/java/com/vedvix/syncledger/controller
mkdir -p src/main/java/com/vedvix/syncledger/service
mkdir -p src/main/java/com/vedvix/syncledger/repository
mkdir -p src/main/java/com/vedvix/syncledger/model
mkdir -p src/main/java/com/vedvix/syncledger/dto
mkdir -p src/main/java/com/vedvix/syncledger/exception
mkdir -p src/main/resources
mkdir -p src/test/java/com/vedvix/syncledger
```

---

#### 16.3.2 Main Application Entry Point

Create file: `src/main/java/com/vedvix/syncledger/SyncLedgerApplication.java`

```java
package com.vedvix.syncledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the SyncLedger application.
 * 
 * @SpringBootApplication - Tells Spring this is the main class
 * @EnableScheduling - Allows us to run scheduled tasks (like checking emails)
 */
@SpringBootApplication
@EnableScheduling
public class SyncLedgerApplication {

    public static void main(String[] args) {
        // This starts the entire application
        SpringApplication.run(SyncLedgerApplication.class, args);
        System.out.println("✅ SyncLedger Backend Started Successfully!");
        System.out.println("📖 API Documentation: http://localhost:8080/swagger-ui.html");
    }
}
```

---

#### 16.3.3 Application Configuration

Create file: `src/main/resources/application.yml`

```yaml
# ============================================================================
# SyncLedger - APPLICATION CONFIGURATION
# ============================================================================
# This file contains all settings for the application.
# Values in ${...} come from environment variables for security.
# ============================================================================

# Server Configuration
server:
  port: 8080                          # Port the app runs on
  servlet:
    context-path: /api                # All URLs start with /api

# Spring Configuration
spring:
  application:
    name: syncledger

  # Database Configuration
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:invoice_portal}
    username: ${DB_USERNAME:invoice_user}
    password: ${DB_PASSWORD:YourSecurePassword123!}
    driver-class-name: org.postgresql.Driver
    
    # Connection pool settings
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

  # JPA/Hibernate Configuration
  jpa:
    hibernate:
      ddl-auto: update              # Automatically create/update tables
    show-sql: true                  # Show SQL queries in console (for debugging)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  # File Upload Configuration
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# ============================================================================
# AZURE CONFIGURATION
# ============================================================================

azure:
  # Azure AD (Entra ID) - For user authentication
  active-directory:
    tenant-id: ${AZURE_TENANT_ID}
    client-id: ${AZURE_CLIENT_ID}
    client-secret: ${AZURE_CLIENT_SECRET}
  
  # Azure Blob Storage - For storing PDF files
  storage:
    account-name: ${AZURE_STORAGE_ACCOUNT}
    account-key: ${AZURE_STORAGE_KEY}
    container-name: invoices
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING}

  # Microsoft Graph API - For reading Outlook emails
  graph:
    client-id: ${AZURE_CLIENT_ID}
    client-secret: ${AZURE_CLIENT_SECRET}
    tenant-id: ${AZURE_TENANT_ID}
    # The email address to monitor for invoices
    mailbox: ${INVOICE_MAILBOX:invoices@vedvix.com}

# ============================================================================
# PDF MICROSERVICE CONFIGURATION
# ============================================================================

pdf-service:
  url: ${PDF_SERVICE_URL:http://localhost:8000}
  timeout: 60000                      # 60 seconds timeout for PDF processing

# ============================================================================
# SAGE INTEGRATION CONFIGURATION
# ============================================================================

sage:
  api:
    base-url: ${SAGE_API_URL:https://api.sage.com/v3.1}
    client-id: ${SAGE_CLIENT_ID}
    client-secret: ${SAGE_CLIENT_SECRET}
  retry:
    max-attempts: 3
    delay-ms: 5000

# ============================================================================
# EMAIL PROCESSING CONFIGURATION
# ============================================================================

email:
  processing:
    enabled: true
    # How often to check for new emails (in milliseconds)
    # 300000 = 5 minutes
    interval-ms: ${EMAIL_CHECK_INTERVAL:300000}
    # Only process emails with these attachment types
    allowed-extensions:
      - .pdf

# ============================================================================
# LOGGING CONFIGURATION
# ============================================================================

logging:
  level:
    root: INFO
    com.vedvix.syncledger: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# ============================================================================
# API DOCUMENTATION (SWAGGER)
# ============================================================================

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

---

#### 16.3.4 Database Entity Models

**What are Entities?**
Entities are Java classes that represent database tables. Each field in the class becomes a column in the table.

---

**Create file: `src/main/java/com/vedvix/syncledger/model/Invoice.java`**

```java
package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice Entity - Represents the 'invoices' table in the database.
 * 
 * Each invoice received via email becomes one row in this table.
 */
@Entity                                     // Marks this as a database table
@Table(name = "invoices")                   // Table name in database
@Data                                       // Lombok: generates getters, setters, toString
@NoArgsConstructor                          // Lombok: generates empty constructor
@AllArgsConstructor                         // Lombok: generates constructor with all fields
@Builder                                    // Lombok: allows Invoice.builder().field().build()
public class Invoice {

    /**
     * Primary Key - Unique identifier for each invoice
     * UUID is used instead of auto-increment for better security
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id", updatable = false, nullable = false)
    private UUID invoiceId;

    /**
     * Invoice number extracted from the PDF
     * Example: "INV-2024-001" or "BILL-12345"
     */
    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    /**
     * Customer/Vendor name from the invoice
     */
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Customer ID/Code from the invoice
     */
    @Column(name = "customer_id", length = 100)
    private String customerId;

    /**
     * Opportunity/Reference number
     */
    @Column(name = "opportunity_no", length = 100)
    private String opportunityNumber;

    /**
     * Invoice amount
     * BigDecimal is used for money to avoid floating-point errors
     */
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code (e.g., "USD", "EUR", "GBP")
     */
    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "USD";

    /**
     * Path to the PDF file in Azure Blob Storage
     */
    @Column(name = "pdf_path", length = 500, nullable = false)
    private String pdfPath;

    /**
     * Original filename of the PDF
     */
    @Column(name = "pdf_filename", length = 255, nullable = false)
    private String pdfFilename;

    /**
     * Current status of the invoice
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.RECEIVED;

    /**
     * Confidence score from AI extraction (0-100)
     * Higher = more confident in extracted data
     */
    @Column(name = "extraction_confidence", precision = 5, scale = 2)
    private BigDecimal extractionConfidence;

    /**
     * Flag indicating if manual review is needed
     */
    @Column(name = "needs_review")
    @Builder.Default
    private Boolean needsReview = false;

    /**
     * Raw text extracted from PDF (stored for reference)
     */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    /**
     * Link to the email this invoice came from
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id")
    private EmailLog email;

    /**
     * User who last modified this invoice
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    /**
     * List of approval actions on this invoice
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Approval> approvals = new ArrayList<>();

    /**
     * Sage sync records
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SageSync> sageSyncs = new ArrayList<>();

    /**
     * Automatically set when record is created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated when record changes
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/InvoiceStatus.java`**

```java
package com.vedvix.syncledger.model;

/**
 * All possible statuses an invoice can have.
 * 
 * This is an enum (enumeration) - a fixed set of possible values.
 */
public enum InvoiceStatus {
    
    // Initial statuses
    RECEIVED("Received", "Email received, not yet processed"),
    PROCESSING("Processing", "Currently extracting data from PDF"),
    
    // After processing
    PROCESSED("Processed", "Data extracted successfully"),
    EXTRACTION_FAILED("Extraction Failed", "Failed to extract data from PDF"),
    
    // Review statuses
    PENDING_REVIEW("Pending Review", "Needs manual review/correction"),
    
    // Approval workflow
    PENDING_APPROVAL("Pending Approval", "Waiting for approver action"),
    APPROVED("Approved", "Approved by approver"),
    REJECTED("Rejected", "Rejected by approver"),
    
    // Sage sync statuses
    SYNCING("Syncing", "Being sent to Sage"),
    SYNCED("Synced", "Successfully sent to Sage"),
    SYNC_FAILED("Sync Failed", "Failed to sync to Sage");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/User.java`**

```java
package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Entity - Represents users who can access the portal.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    /**
     * Email address (used for login via Azure AD)
     */
    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    /**
     * User's full name
     */
    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /**
     * User's role in the system
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, nullable = false)
    @Builder.Default
    private UserRole role = UserRole.VIEWER;

    /**
     * Whether the user account is active
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Azure AD Object ID (for linking to Azure AD account)
     */
    @Column(name = "azure_ad_id", length = 100)
    private String azureAdId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/UserRole.java`**

```java
package com.vedvix.syncledger.model;

/**
 * User roles with their permissions.
 */
public enum UserRole {
    
    ADMIN("Administrator", "Full access to all features"),
    APPROVER("Approver", "Can approve/reject invoices"),
    VIEWER("Viewer", "Read-only access");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/EmailLog.java`**

```java
package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EmailLog Entity - Tracks all emails received in the invoice mailbox.
 */
@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "email_id", updatable = false, nullable = false)
    private UUID emailId;

    /**
     * Microsoft Graph message ID (unique identifier from Outlook)
     */
    @Column(name = "message_id", length = 500, nullable = false, unique = true)
    private String messageId;

    /**
     * Sender's email address
     */
    @Column(name = "from_address", length = 255, nullable = false)
    private String fromAddress;

    /**
     * Email subject line
     */
    @Column(name = "subject", length = 500)
    private String subject;

    /**
     * When the email was received in Outlook
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * Whether this email has been processed
     */
    @Column(name = "processed")
    @Builder.Default
    private Boolean processed = false;

    /**
     * Number of attachments in the email
     */
    @Column(name = "attachment_count")
    @Builder.Default
    private Integer attachmentCount = 0;

    /**
     * Any error message if processing failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Invoices created from this email
     */
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/Approval.java`**

```java
package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Approval Entity - Records approval/rejection actions on invoices.
 */
@Entity
@Table(name = "approvals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "approval_id", updatable = false, nullable = false)
    private UUID approvalId;

    /**
     * The invoice being approved/rejected
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * The user who performed the action
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    /**
     * The action taken
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 50, nullable = false)
    private ApprovalAction action;

    /**
     * Comments provided with the action
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/ApprovalAction.java`**

```java
package com.vedvix.syncledger.model;

/**
 * Possible approval actions.
 */
public enum ApprovalAction {
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ApprovalAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/SageSync.java`**

```java
package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SageSync Entity - Tracks sync attempts to Sage accounting system.
 */
@Entity
@Table(name = "sage_syncs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SageSync {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "sync_id", updatable = false, nullable = false)
    private UUID syncId;

    /**
     * The invoice being synced
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * Invoice ID returned by Sage after successful sync
     */
    @Column(name = "sage_invoice_id", length = 100)
    private String sageInvoiceId;

    /**
     * Status of this sync attempt
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private SyncStatus status = SyncStatus.PENDING;

    /**
     * JSON payload sent to Sage
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    /**
     * JSON response from Sage
     */
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    /**
     * Error message if sync failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * When the sync was completed (if successful)
     */
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/SyncStatus.java`**

```java
package com.vedvix.syncledger.model;

/**
 * Possible Sage sync statuses.
 */
public enum SyncStatus {
    PENDING("Pending"),
    SUCCESS("Success"),
    FAILED("Failed");

    private final String displayName;

    SyncStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/model/AuditLog.java`**

```java
package com.vedvix.syncledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog Entity - Records all changes for compliance and debugging.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    /**
     * Related invoice (if applicable)
     */
    @Column(name = "invoice_id")
    private UUID invoiceId;

    /**
     * User who performed the action
     */
    @Column(name = "user_id")
    private UUID userId;

    /**
     * What action was performed
     */
    @Column(name = "action", length = 100, nullable = false)
    private String action;

    /**
     * Type of entity affected
     */
    @Column(name = "entity_type", length = 100, nullable = false)
    private String entityType;

    /**
     * JSON of old values (before change)
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * JSON of new values (after change)
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * IP address of the user
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Browser/client information
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

---

#### 16.3.5 Repository Interfaces (Database Access)

**What are Repositories?**
Repositories are interfaces that Spring automatically implements to give you database operations (find, save, delete, etc.).

---

**Create file: `src/main/java/com/vedvix/syncledger/repository/InvoiceRepository.java`**

```java
package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Invoice database operations.
 * 
 * JpaRepository provides: save(), findById(), findAll(), delete(), count(), etc.
 * We add custom query methods below.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Find invoice by invoice number
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find all invoices with a specific status
     */
    List<Invoice> findByStatus(InvoiceStatus status);

    /**
     * Find all invoices that need review
     */
    List<Invoice> findByNeedsReviewTrue();

    /**
     * Find invoices by status with pagination
     */
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Search invoices by customer name (case-insensitive)
     */
    Page<Invoice> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);

    /**
     * Complex search with multiple optional filters
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:search IS NULL OR LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Invoice> searchInvoices(
            @Param("status") InvoiceStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Count invoices by status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Get total amount of all approved invoices
     */
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.status = 'APPROVED'")
    BigDecimal getTotalApprovedAmount();

    /**
     * Get invoices created within a date range
     */
    List<Invoice> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Check if an invoice with the same invoice number already exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Get dashboard statistics
     */
    @Query("SELECT i.status, COUNT(i) FROM Invoice i GROUP BY i.status")
    List<Object[]> getStatusCounts();
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/repository/UserRepository.java`**

```java
package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    
    Optional<User> findByAzureAdId(String azureAdId);
    
    List<User> findByRole(UserRole role);
    
    List<User> findByIsActiveTrue();
    
    boolean existsByEmail(String email);
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/repository/EmailLogRepository.java`**

```java
package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    Optional<EmailLog> findByMessageId(String messageId);
    
    List<EmailLog> findByProcessedFalse();
    
    boolean existsByMessageId(String messageId);
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/repository/ApprovalRepository.java`**

```java
package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    List<Approval> findByInvoice_InvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/repository/SageSyncRepository.java`**

```java
package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.SageSync;
import com.vedvix.syncledger.model.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SageSyncRepository extends JpaRepository<SageSync, UUID> {

    List<SageSync> findByInvoice_InvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
    
    List<SageSync> findByStatus(SyncStatus status);
    
    Optional<SageSync> findTopByInvoice_InvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/repository/AuditLogRepository.java`**

```java
package com.vedvix.syncledger.repository;

import com.vedvix.syncledger.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId, Pageable pageable);
    
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
```

---

#### 16.3.6 Data Transfer Objects (DTOs)

**What are DTOs?**
DTOs are simple objects used to transfer data between the API and clients. They define what data goes in and out of API endpoints.

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/InvoiceDTO.java`**

```java
package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for returning invoice data to the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    
    private UUID invoiceId;
    private String invoiceNumber;
    private String customerName;
    private String customerId;
    private String opportunityNumber;
    private BigDecimal amount;
    private String currency;
    private String pdfPath;
    private String pdfFilename;
    private InvoiceStatus status;
    private String statusDisplayName;
    private BigDecimal extractionConfidence;
    private Boolean needsReview;
    
    // Email info
    private EmailInfoDTO email;
    
    // Approval history
    private List<ApprovalDTO> approvals;
    
    // Sage sync info
    private SageSyncDTO latestSync;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/InvoiceCreateDTO.java`**

```java
package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating/updating invoice data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCreateDTO {
    
    private String invoiceNumber;
    
    private String customerName;
    
    private String customerId;
    
    private String opportunityNumber;
    
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/InvoiceUpdateDTO.java`**

```java
package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating invoice fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceUpdateDTO {
    
    private String invoiceNumber;
    private String customerName;
    private String customerId;
    private String opportunityNumber;
    
    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    private String currency;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/ApprovalDTO.java`**

```java
package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.ApprovalAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDTO {
    
    private UUID approvalId;
    private String approverName;
    private String approverEmail;
    private ApprovalAction action;
    private String comments;
    private LocalDateTime createdAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/ApprovalRequestDTO.java`**

```java
package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for approval/rejection requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestDTO {
    
    private String comments;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/EmailInfoDTO.java`**

```java
package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailInfoDTO {
    
    private String fromAddress;
    private String subject;
    private LocalDateTime receivedAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/SageSyncDTO.java`**

```java
package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SageSyncDTO {
    
    private SyncStatus status;
    private String sageInvoiceId;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime syncedAt;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/DashboardStatsDTO.java`**

```java
package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for dashboard statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    
    private long totalInvoices;
    private long receivedCount;
    private long pendingReviewCount;
    private long pendingApprovalCount;
    private long approvedCount;
    private long rejectedCount;
    private long syncedCount;
    private long syncFailedCount;
    private BigDecimal totalApprovedAmount;
    private long needsAttentionCount;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/PagedResponseDTO.java`**

```java
package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic DTO for paginated responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponseDTO<T> {
    
    private List<T> data;
    private PaginationDTO pagination;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/PaginationDTO.java`**

```java
package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginationDTO {
    
    private int currentPage;
    private int pageSize;
    private long totalItems;
    private int totalPages;
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/dto/PdfExtractionResultDTO.java`**

```java
package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for receiving extraction results from the Python PDF service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfExtractionResultDTO {
    
    private String invoiceNumber;
    private String customerName;
    private String customerId;
    private String opportunityNumber;
    private BigDecimal amount;
    private String currency;
    private BigDecimal confidence;
    private String rawText;
    private boolean success;
    private String errorMessage;
}
```

---

#### 16.3.7 Service Layer (Business Logic)

**What are Services?**
Services contain the business logic of your application. They coordinate between controllers (API) and repositories (database).

---

**Create file: `src/main/java/com/vedvix/syncledger/service/InvoiceService.java`**

```java
package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.exception.InvoiceNotFoundException;
import com.vedvix.syncledger.exception.InvalidOperationException;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for invoice operations.
 */
@Service
@RequiredArgsConstructor  // Lombok: creates constructor for final fields
@Slf4j                    // Lombok: creates logger
@Transactional            // All methods run in a database transaction
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ApprovalRepository approvalRepository;
    private final SageSyncRepository sageSyncRepository;
    private final AuditLogService auditLogService;
    private final SageSyncService sageSyncService;

    /**
     * Get all invoices with pagination and filtering.
     */
    public PagedResponseDTO<InvoiceDTO> getInvoices(
            InvoiceStatus status,
            String search,
            int page,
            int size,
            String sortBy,
            String sortOrder
    ) {
        log.info("Fetching invoices - status: {}, search: {}, page: {}", status, search, page);

        // Create sort
        Sort sort = sortOrder.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();

        // Create pageable
        Pageable pageable = PageRequest.of(page - 1, size, sort); // page is 0-indexed

        // Fetch from database
        Page<Invoice> invoicePage = invoiceRepository.searchInvoices(status, search, pageable);

        // Convert to DTOs
        List<InvoiceDTO> invoiceDTOs = invoicePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Build response
        return PagedResponseDTO.<InvoiceDTO>builder()
                .data(invoiceDTOs)
                .pagination(PaginationDTO.builder()
                        .currentPage(page)
                        .pageSize(size)
                        .totalItems(invoicePage.getTotalElements())
                        .totalPages(invoicePage.getTotalPages())
                        .build())
                .build();
    }

    /**
     * Get single invoice by ID.
     */
    public InvoiceDTO getInvoiceById(UUID invoiceId) {
        log.info("Fetching invoice: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));

        return convertToDetailDTO(invoice);
    }

    /**
     * Update invoice fields.
     */
    public InvoiceDTO updateInvoice(UUID invoiceId, InvoiceUpdateDTO updateDTO, User currentUser) {
        log.info("Updating invoice: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));

        // Store old values for audit
        String oldValues = auditLogService.toJson(invoice);

        // Update fields if provided
        if (updateDTO.getInvoiceNumber() != null) {
            invoice.setInvoiceNumber(updateDTO.getInvoiceNumber());
        }
        if (updateDTO.getCustomerName() != null) {
            invoice.setCustomerName(updateDTO.getCustomerName());
        }
        if (updateDTO.getCustomerId() != null) {
            invoice.setCustomerId(updateDTO.getCustomerId());
        }
        if (updateDTO.getOpportunityNumber() != null) {
            invoice.setOpportunityNumber(updateDTO.getOpportunityNumber());
        }
        if (updateDTO.getAmount() != null) {
            invoice.setAmount(updateDTO.getAmount());
        }
        if (updateDTO.getCurrency() != null) {
            invoice.setCurrency(updateDTO.getCurrency());
        }

        // Clear needs review flag after manual edit
        invoice.setNeedsReview(false);
        invoice.setModifiedBy(currentUser);

        // Save
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Log audit
        auditLogService.logAction(
                invoiceId,
                currentUser.getUserId(),
                "UPDATE",
                "INVOICE",
                oldValues,
                auditLogService.toJson(savedInvoice)
        );

        return convertToDTO(savedInvoice);
    }

    /**
     * Submit invoice for approval.
     */
    public InvoiceDTO submitForApproval(UUID invoiceId, User currentUser) {
        log.info("Submitting invoice for approval: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));

        // Validate status
        if (invoice.getStatus() != InvoiceStatus.PROCESSED && 
            invoice.getStatus() != InvoiceStatus.PENDING_REVIEW &&
            invoice.getStatus() != InvoiceStatus.REJECTED) {
            throw new InvalidOperationException(
                    "Cannot submit invoice in status: " + invoice.getStatus());
        }

        // Update status
        invoice.setStatus(InvoiceStatus.PENDING_APPROVAL);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Log audit
        auditLogService.logAction(
                invoiceId,
                currentUser.getUserId(),
                "SUBMIT_FOR_APPROVAL",
                "INVOICE",
                null,
                "Status changed to PENDING_APPROVAL"
        );

        // TODO: Send notification to approvers

        return convertToDTO(savedInvoice);
    }

    /**
     * Approve an invoice.
     */
    public InvoiceDTO approveInvoice(UUID invoiceId, ApprovalRequestDTO request, User approver) {
        log.info("Approving invoice: {} by user: {}", invoiceId, approver.getEmail());

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));

        // Validate status
        if (invoice.getStatus() != InvoiceStatus.PENDING_APPROVAL) {
            throw new InvalidOperationException(
                    "Cannot approve invoice in status: " + invoice.getStatus());
        }

        // Create approval record
        Approval approval = Approval.builder()
                .invoice(invoice)
                .approver(approver)
                .action(ApprovalAction.APPROVED)
                .comments(request.getComments())
                .build();
        approvalRepository.save(approval);

        // Update invoice status
        invoice.setStatus(InvoiceStatus.APPROVED);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Log audit
        auditLogService.logAction(
                invoiceId,
                approver.getUserId(),
                "APPROVE",
                "INVOICE",
                null,
                "Approved with comments: " + request.getComments()
        );

        // Trigger Sage sync asynchronously
        sageSyncService.syncToSageAsync(invoice);

        return convertToDTO(savedInvoice);
    }

    /**
     * Reject an invoice.
     */
    public InvoiceDTO rejectInvoice(UUID invoiceId, ApprovalRequestDTO request, User approver) {
        log.info("Rejecting invoice: {} by user: {}", invoiceId, approver.getEmail());

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + invoiceId));

        // Validate status
        if (invoice.getStatus() != InvoiceStatus.PENDING_APPROVAL) {
            throw new InvalidOperationException(
                    "Cannot reject invoice in status: " + invoice.getStatus());
        }

        // Comments required for rejection
        if (request.getComments() == null || request.getComments().trim().isEmpty()) {
            throw new InvalidOperationException("Comments are required when rejecting an invoice");
        }

        // Create approval record
        Approval approval = Approval.builder()
                .invoice(invoice)
                .approver(approver)
                .action(ApprovalAction.REJECTED)
                .comments(request.getComments())
                .build();
        approvalRepository.save(approval);

        // Update invoice status
        invoice.setStatus(InvoiceStatus.REJECTED);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Log audit
        auditLogService.logAction(
                invoiceId,
                approver.getUserId(),
                "REJECT",
                "INVOICE",
                null,
                "Rejected with comments: " + request.getComments()
        );

        // TODO: Send notification to submitter

        return convertToDTO(savedInvoice);
    }

    /**
     * Get dashboard statistics.
     */
    public DashboardStatsDTO getDashboardStats() {
        log.info("Fetching dashboard statistics");

        long total = invoiceRepository.count();
        long received = invoiceRepository.countByStatus(InvoiceStatus.RECEIVED);
        long pendingReview = invoiceRepository.countByStatus(InvoiceStatus.PENDING_REVIEW);
        long pendingApproval = invoiceRepository.countByStatus(InvoiceStatus.PENDING_APPROVAL);
        long approved = invoiceRepository.countByStatus(InvoiceStatus.APPROVED);
        long rejected = invoiceRepository.countByStatus(InvoiceStatus.REJECTED);
        long synced = invoiceRepository.countByStatus(InvoiceStatus.SYNCED);
        long syncFailed = invoiceRepository.countByStatus(InvoiceStatus.SYNC_FAILED);
        BigDecimal totalApproved = invoiceRepository.getTotalApprovedAmount();

        return DashboardStatsDTO.builder()
                .totalInvoices(total)
                .receivedCount(received)
                .pendingReviewCount(pendingReview)
                .pendingApprovalCount(pendingApproval)
                .approvedCount(approved)
                .rejectedCount(rejected)
                .syncedCount(synced)
                .syncFailedCount(syncFailed)
                .totalApprovedAmount(totalApproved)
                .needsAttentionCount(pendingReview + syncFailed)
                .build();
    }

    // ============== Helper Methods ==============

    /**
     * Convert Invoice entity to DTO (basic info).
     */
    private InvoiceDTO convertToDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerName(invoice.getCustomerName())
                .customerId(invoice.getCustomerId())
                .opportunityNumber(invoice.getOpportunityNumber())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .statusDisplayName(invoice.getStatus().getDisplayName())
                .extractionConfidence(invoice.getExtractionConfidence())
                .needsReview(invoice.getNeedsReview())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    /**
     * Convert Invoice entity to DTO (with full details).
     */
    private InvoiceDTO convertToDetailDTO(Invoice invoice) {
        InvoiceDTO dto = convertToDTO(invoice);

        // Add PDF info
        dto.setPdfPath(invoice.getPdfPath());
        dto.setPdfFilename(invoice.getPdfFilename());

        // Add email info
        if (invoice.getEmail() != null) {
            dto.setEmail(EmailInfoDTO.builder()
                    .fromAddress(invoice.getEmail().getFromAddress())
                    .subject(invoice.getEmail().getSubject())
                    .receivedAt(invoice.getEmail().getReceivedAt())
                    .build());
        }

        // Add approval history
        List<ApprovalDTO> approvals = approvalRepository
                .findByInvoice_InvoiceIdOrderByCreatedAtDesc(invoice.getInvoiceId())
                .stream()
                .map(a -> ApprovalDTO.builder()
                        .approvalId(a.getApprovalId())
                        .approverName(a.getApprover().getName())
                        .approverEmail(a.getApprover().getEmail())
                        .action(a.getAction())
                        .comments(a.getComments())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        dto.setApprovals(approvals);

        // Add latest Sage sync info
        sageSyncRepository.findTopByInvoice_InvoiceIdOrderByCreatedAtDesc(invoice.getInvoiceId())
                .ifPresent(sync -> dto.setLatestSync(SageSyncDTO.builder()
                        .status(sync.getStatus())
                        .sageInvoiceId(sync.getSageInvoiceId())
                        .errorMessage(sync.getErrorMessage())
                        .retryCount(sync.getRetryCount())
                        .syncedAt(sync.getSyncedAt())
                        .build()));

        return dto;
    }
}
```

---

**(Document continues with more service classes, controllers, and Python microservice code...)**

Due to the extensive length, I'll continue with the remaining critical components:

---

**Create file: `src/main/java/com/vedvix/syncledger/service/PdfProcessingService.java`**

```java
package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.PdfExtractionResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for communicating with the Python PDF microservice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfProcessingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${pdf-service.url}")
    private String pdfServiceUrl;

    @Value("${pdf-service.timeout}")
    private int timeout;

    /**
     * Send PDF to the Python microservice for extraction.
     */
    public PdfExtractionResultDTO extractDataFromPdf(byte[] pdfBytes, String filename) {
        log.info("Sending PDF to extraction service: {}", filename);

        try {
            WebClient webClient = webClientBuilder.baseUrl(pdfServiceUrl).build();

            // Build multipart request
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            }).contentType(MediaType.APPLICATION_PDF);

            // Call the Python service
            PdfExtractionResultDTO result = webClient.post()
                    .uri("/extract")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(PdfExtractionResultDTO.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Extraction complete for {}: success={}", filename, result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("Error calling PDF service for {}: {}", filename, e.getMessage());
            return PdfExtractionResultDTO.builder()
                    .success(false)
                    .errorMessage("PDF processing failed: " + e.getMessage())
                    .build();
        }
    }
}
```

---

#### 16.3.8 REST Controllers (API Endpoints)

**Create file: `src/main/java/com/vedvix/syncledger/controller/InvoiceController.java`**

```java
package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.*;
import com.vedvix.syncledger.model.InvoiceStatus;
import com.vedvix.syncledger.model.User;
import com.vedvix.syncledger.service.InvoiceService;
import com.vedvix.syncledger.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Invoice operations.
 * 
 * All endpoints start with /api/invoices
 */
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management APIs")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserService userService;

    /**
     * GET /api/invoices
     * 
     * List all invoices with pagination and filtering.
     */
    @GetMapping
    @Operation(summary = "List all invoices", description = "Get paginated list of invoices with optional filters")
    public ResponseEntity<PagedResponseDTO<InvoiceDTO>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        PagedResponseDTO<InvoiceDTO> response = invoiceService.getInvoices(
                status, search, page, size, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/invoices/{id}
     * 
     * Get single invoice with full details.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get invoice details", description = "Get full invoice details including approval history")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable("id") UUID invoiceId) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(invoiceId);
        return ResponseEntity.ok(invoice);
    }

    /**
     * PUT /api/invoices/{id}
     * 
     * Update invoice fields.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update invoice", description = "Update invoice fields (for manual correction)")
    public ResponseEntity<InvoiceDTO> updateInvoice(
            @PathVariable("id") UUID invoiceId,
            @Valid @RequestBody InvoiceUpdateDTO updateDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser = userService.getCurrentUser(jwt);
        InvoiceDTO updatedInvoice = invoiceService.updateInvoice(invoiceId, updateDTO, currentUser);
        return ResponseEntity.ok(updatedInvoice);
    }

    /**
     * POST /api/invoices/{id}/submit
     * 
     * Submit invoice for approval.
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit for approval", description = "Submit invoice to the approval workflow")
    public ResponseEntity<InvoiceDTO> submitForApproval(
            @PathVariable("id") UUID invoiceId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser = userService.getCurrentUser(jwt);
        InvoiceDTO invoice = invoiceService.submitForApproval(invoiceId, currentUser);
        return ResponseEntity.ok(invoice);
    }

    /**
     * POST /api/invoices/{id}/approve
     * 
     * Approve an invoice.
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve invoice", description = "Approve an invoice (triggers Sage sync)")
    public ResponseEntity<InvoiceDTO> approveInvoice(
            @PathVariable("id") UUID invoiceId,
            @Valid @RequestBody ApprovalRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser = userService.getCurrentUser(jwt);
        InvoiceDTO invoice = invoiceService.approveInvoice(invoiceId, request, currentUser);
        return ResponseEntity.ok(invoice);
    }

    /**
     * POST /api/invoices/{id}/reject
     * 
     * Reject an invoice.
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject invoice", description = "Reject an invoice (requires comments)")
    public ResponseEntity<InvoiceDTO> rejectInvoice(
            @PathVariable("id") UUID invoiceId,
            @Valid @RequestBody ApprovalRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User currentUser = userService.getCurrentUser(jwt);
        InvoiceDTO invoice = invoiceService.rejectInvoice(invoiceId, request, currentUser);
        return ResponseEntity.ok(invoice);
    }
}
```

---

**Create file: `src/main/java/com/vedvix/syncledger/controller/DashboardController.java`**

```java
package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.DashboardStatsDTO;
import com.vedvix.syncledger.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics APIs")
public class DashboardController {

    private final InvoiceService invoiceService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard stats", description = "Get invoice counts and totals for dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = invoiceService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
```

---

### 16.4 PART 2: Python PDF Microservice - Complete Implementation

Now let's create the Python microservice that handles PDF processing.

#### 16.4.1 Create Project Structure

```powershell
# Navigate to the main project folder
cd C:\Projects\syncledger

# Create Python service folder
mkdir pdf-service
cd pdf-service

# Create subfolders
mkdir app
mkdir app\services
mkdir app\models
mkdir app\utils
mkdir tests
```

---

#### 16.4.2 Create requirements.txt

**Create file: `pdf-service/requirements.txt`**

```txt
# ============================================================================
# SyncLedger - PDF MICROSERVICE DEPENDENCIES
# ============================================================================
# Install all: pip install -r requirements.txt
# ============================================================================

# Web Framework
fastapi==0.109.0
uvicorn[standard]==0.27.0
python-multipart==0.0.6

# Data Validation
pydantic==2.5.0
pydantic-settings==2.1.0

# PDF Processing
PyMuPDF==1.23.8           # Fast PDF text extraction (also known as fitz)
pdfplumber==0.10.3        # Alternative PDF extraction with table support
pdf2image==1.16.3         # Convert PDF to images (for OCR)
Pillow==10.2.0            # Image processing

# OCR (Optical Character Recognition)
pytesseract==0.3.10       # Python wrapper for Tesseract OCR

# AI/ML for Intelligent Extraction
# These help identify fields even in unknown invoice formats
spacy==3.7.2              # NLP library
transformers==4.36.2      # Hugging Face transformers
torch==2.1.2              # PyTorch (required by transformers)
scikit-learn==1.4.0       # Machine learning utilities

# Regular expressions and text processing
regex==2023.12.25

# Logging and utilities
loguru==0.7.2             # Better logging
python-dotenv==1.0.0      # Environment variables

# Testing
pytest==7.4.4
pytest-asyncio==0.23.3
httpx==0.26.0             # Async HTTP client for testing

# For downloading spaCy model, run after install:
# python -m spacy download en_core_web_sm
```

---

#### 16.4.3 Main FastAPI Application

**Create file: `pdf-service/app/main.py`**

```python
"""
SyncLedger - PDF Processing Microservice

This service receives PDF files and extracts invoice data using:
1. Direct text extraction (for digital PDFs)
2. OCR (for scanned PDFs)
3. AI/ML pattern matching (for identifying fields)

Author: vedvix Team
"""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger
import sys

from app.models.schemas import ExtractionResult, HealthCheck
from app.services.pdf_extractor import PdfExtractor
from app.services.field_extractor import FieldExtractor

# ============================================================================
# APPLICATION SETUP
# ============================================================================

# Configure logging
logger.remove()  # Remove default handler
logger.add(
    sys.stdout,
    format="<green>{time:YYYY-MM-DD HH:mm:ss}</green> | <level>{level: <8}</level> | <cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>",
    level="DEBUG"
)
logger.add(
    "logs/pdf_service.log",
    rotation="10 MB",
    retention="7 days",
    level="INFO"
)

# Create FastAPI app
app = FastAPI(
    title="Invoice PDF Extraction Service",
    description="Microservice for extracting data from invoice PDFs",
    version="1.0.0",
    docs_url="/docs",      # Swagger UI
    redoc_url="/redoc"     # ReDoc
)

# Configure CORS (Cross-Origin Resource Sharing)
# This allows the Java backend to call this service
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize services
pdf_extractor = PdfExtractor()
field_extractor = FieldExtractor()

# ============================================================================
# API ENDPOINTS
# ============================================================================

@app.get("/", response_model=HealthCheck)
async def root():
    """
    Root endpoint - returns service status.
    
    Use this to check if the service is running.
    """
    return HealthCheck(
        status="healthy",
        service="PDF Extraction Service",
        version="1.0.0"
    )


@app.get("/health", response_model=HealthCheck)
async def health_check():
    """
    Health check endpoint for monitoring.
    """
    return HealthCheck(
        status="healthy",
        service="PDF Extraction Service",
        version="1.0.0"
    )


@app.post("/extract", response_model=ExtractionResult)
async def extract_invoice_data(file: UploadFile = File(...)):
    """
    Main extraction endpoint.
    
    Accepts a PDF file and returns extracted invoice fields.
    
    **Process:**
    1. Receive PDF file
    2. Extract text from PDF (using OCR if needed)
    3. Use AI/pattern matching to identify fields
    4. Return structured data with confidence scores
    
    **Parameters:**
    - file: PDF file to process
    
    **Returns:**
    - ExtractionResult with invoice fields and confidence score
    """
    logger.info(f"Received file for extraction: {file.filename}")
    
    # Validate file type
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=400,
            detail="Only PDF files are accepted"
        )
    
    try:
        # Read file content
        pdf_content = await file.read()
        logger.info(f"File size: {len(pdf_content)} bytes")
        
        # Step 1: Extract text from PDF
        logger.info("Step 1: Extracting text from PDF...")
        extracted_text, extraction_method = pdf_extractor.extract_text(pdf_content)
        logger.info(f"Text extracted using: {extraction_method}")
        logger.debug(f"Extracted text preview: {extracted_text[:500] if extracted_text else 'No text'}")
        
        if not extracted_text or len(extracted_text.strip()) < 10:
            logger.warning("No text could be extracted from PDF")
            return ExtractionResult(
                success=False,
                error_message="Could not extract text from PDF. The file may be corrupted or contain only images.",
                raw_text=""
            )
        
        # Step 2: Extract fields using AI/pattern matching
        logger.info("Step 2: Extracting fields from text...")
        fields = field_extractor.extract_fields(extracted_text)
        
        # Step 3: Calculate overall confidence
        confidence = field_extractor.calculate_confidence(fields)
        logger.info(f"Extraction complete. Confidence: {confidence}%")
        
        # Return result
        return ExtractionResult(
            success=True,
            invoice_number=fields.get('invoice_number'),
            customer_name=fields.get('customer_name'),
            customer_id=fields.get('customer_id'),
            opportunity_number=fields.get('opportunity_number'),
            amount=fields.get('amount'),
            currency=fields.get('currency', 'USD'),
            confidence=confidence,
            raw_text=extracted_text,
            extraction_method=extraction_method,
            error_message=None
        )
        
    except Exception as e:
        logger.error(f"Error processing PDF: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Error processing PDF: {str(e)}"
        )


@app.post("/extract/batch")
async def extract_batch(files: list[UploadFile] = File(...)):
    """
    Batch extraction endpoint for multiple PDFs.
    
    **Parameters:**
    - files: List of PDF files to process
    
    **Returns:**
    - List of ExtractionResult objects
    """
    logger.info(f"Received batch extraction request: {len(files)} files")
    
    results = []
    for file in files:
        try:
            result = await extract_invoice_data(file)
            results.append({
                "filename": file.filename,
                "result": result
            })
        except Exception as e:
            results.append({
                "filename": file.filename,
                "result": ExtractionResult(
                    success=False,
                    error_message=str(e)
                )
            })
    
    return {"results": results}


# ============================================================================
# STARTUP & SHUTDOWN EVENTS
# ============================================================================

@app.on_event("startup")
async def startup_event():
    """
    Called when the service starts.
    """
    logger.info("=" * 60)
    logger.info("PDF Extraction Service Starting...")
    logger.info("=" * 60)
    
    # Initialize models/resources
    logger.info("Initializing field extractor...")
    field_extractor.initialize()
    
    logger.info("Service ready!")


@app.on_event("shutdown")
async def shutdown_event():
    """
    Called when the service stops.
    """
    logger.info("PDF Extraction Service shutting down...")


# ============================================================================
# RUN THE APPLICATION
# ============================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True  # Auto-reload on code changes (dev only)
    )
```

---

#### 16.4.4 Data Models (Schemas)

**Create file: `pdf-service/app/models/schemas.py`**

```python
"""
Pydantic models for request/response schemas.

These define the structure of data going in and out of the API.
"""

from pydantic import BaseModel, Field
from typing import Optional
from decimal import Decimal


class HealthCheck(BaseModel):
    """Health check response model."""
    status: str
    service: str
    version: str


class ExtractionResult(BaseModel):
    """
    Result of PDF extraction.
    
    This is what gets sent back to the Java backend.
    """
    success: bool = Field(
        description="Whether extraction was successful"
    )
    
    invoice_number: Optional[str] = Field(
        default=None,
        description="Extracted invoice number"
    )
    
    customer_name: Optional[str] = Field(
        default=None,
        description="Extracted customer/vendor name"
    )
    
    customer_id: Optional[str] = Field(
        default=None,
        description="Extracted customer ID/code"
    )
    
    opportunity_number: Optional[str] = Field(
        default=None,
        description="Extracted opportunity/reference number"
    )
    
    amount: Optional[float] = Field(
        default=None,
        description="Extracted invoice amount"
    )
    
    currency: Optional[str] = Field(
        default="USD",
        description="Currency code (e.g., USD, EUR)"
    )
    
    confidence: Optional[float] = Field(
        default=None,
        description="Confidence score (0-100)"
    )
    
    raw_text: Optional[str] = Field(
        default=None,
        description="Raw text extracted from PDF"
    )
    
    extraction_method: Optional[str] = Field(
        default=None,
        description="Method used: 'digital' or 'ocr'"
    )
    
    error_message: Optional[str] = Field(
        default=None,
        description="Error message if extraction failed"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "invoice_number": "INV-2024-001",
                "customer_name": "Acme Corporation",
                "customer_id": "CUST-001",
                "opportunity_number": "OPP-2024-100",
                "amount": 15000.00,
                "currency": "USD",
                "confidence": 95.5,
                "raw_text": "Invoice #INV-2024-001...",
                "extraction_method": "digital",
                "error_message": None
            }
        }
```

---

#### 16.4.5 PDF Text Extraction Service

**Create file: `pdf-service/app/services/pdf_extractor.py`**

```python
"""
PDF Text Extraction Service

Handles extracting text from PDFs using multiple methods:
1. PyMuPDF (fitz) - For digital PDFs with embedded text
2. OCR (pytesseract) - For scanned PDFs/images

The service automatically detects which method is needed.
"""

import fitz  # PyMuPDF
import pytesseract
from PIL import Image
import io
from loguru import logger
from typing import Tuple, Optional
import tempfile
import os


class PdfExtractor:
    """
    Extracts text from PDF files.
    
    Usage:
        extractor = PdfExtractor()
        text, method = extractor.extract_text(pdf_bytes)
    """
    
    def __init__(self):
        """Initialize the PDF extractor."""
        # Configure Tesseract path (Windows)
        # You may need to adjust this path based on your Tesseract installation
        if os.name == 'nt':  # Windows
            tesseract_path = r'C:\Program Files\Tesseract-OCR\tesseract.exe'
            if os.path.exists(tesseract_path):
                pytesseract.pytesseract.tesseract_cmd = tesseract_path
    
    def extract_text(self, pdf_content: bytes) -> Tuple[str, str]:
        """
        Extract text from a PDF file.
        
        Args:
            pdf_content: Raw bytes of the PDF file
            
        Returns:
            Tuple of (extracted_text, method_used)
            method_used is either 'digital' or 'ocr'
        """
        try:
            # First, try to extract text directly (digital PDF)
            text = self._extract_digital_text(pdf_content)
            
            # If we got meaningful text, return it
            if text and len(text.strip()) > 50:
                logger.info("Successfully extracted text from digital PDF")
                return text, "digital"
            
            # Otherwise, use OCR
            logger.info("Digital extraction yielded little text, trying OCR...")
            text = self._extract_with_ocr(pdf_content)
            return text, "ocr"
            
        except Exception as e:
            logger.error(f"Error extracting text: {str(e)}")
            raise
    
    def _extract_digital_text(self, pdf_content: bytes) -> str:
        """
        Extract text from a digital PDF using PyMuPDF.
        
        Digital PDFs have text embedded that can be directly extracted.
        This is faster and more accurate than OCR.
        """
        text_parts = []
        
        # Open PDF from bytes
        doc = fitz.open(stream=pdf_content, filetype="pdf")
        
        try:
            for page_num in range(len(doc)):
                page = doc[page_num]
                text = page.get_text()
                
                if text:
                    text_parts.append(f"--- Page {page_num + 1} ---\n{text}")
                    
        finally:
            doc.close()
        
        return "\n".join(text_parts)
    
    def _extract_with_ocr(self, pdf_content: bytes) -> str:
        """
        Extract text from a scanned PDF using OCR.
        
        This converts each page to an image and runs OCR on it.
        Used when the PDF doesn't have embedded text.
        """
        text_parts = []
        
        # Open PDF
        doc = fitz.open(stream=pdf_content, filetype="pdf")
        
        try:
            for page_num in range(len(doc)):
                page = doc[page_num]
                
                # Convert page to image
                # zoom = 2 means 2x resolution for better OCR
                mat = fitz.Matrix(2, 2)
                pix = page.get_pixmap(matrix=mat)
                
                # Convert to PIL Image
                img_data = pix.tobytes("png")
                image = Image.open(io.BytesIO(img_data))
                
                # Run OCR
                text = pytesseract.image_to_string(image, lang='eng')
                
                if text:
                    text_parts.append(f"--- Page {page_num + 1} ---\n{text}")
                    
        finally:
            doc.close()
        
        return "\n".join(text_parts)
    
    def get_page_count(self, pdf_content: bytes) -> int:
        """Get the number of pages in a PDF."""
        doc = fitz.open(stream=pdf_content, filetype="pdf")
        try:
            return len(doc)
        finally:
            doc.close()
```

---

#### 16.4.6 Field Extraction Service (AI-Powered)

**Create file: `pdf-service/app/services/field_extractor.py`**

```python
"""
Intelligent Field Extraction Service

Uses pattern matching and NLP to extract invoice fields from text.
Handles various invoice formats by recognizing common field labels.
"""

import re
from typing import Dict, Any, Optional, List
from loguru import logger
from app.utils.field_patterns import FIELD_PATTERNS


class FieldExtractor:
    """
    Extracts structured invoice fields from raw text.
    
    Uses multiple strategies:
    1. Regex pattern matching for common field labels
    2. Positional analysis for amounts
    3. NLP for entity recognition (customer names)
    """
    
    def __init__(self):
        """Initialize the field extractor."""
        self.nlp = None  # Will be loaded on first use
        self._initialized = False
    
    def initialize(self):
        """
        Initialize NLP models.
        
        Called at application startup.
        """
        if self._initialized:
            return
            
        logger.info("Loading spaCy model...")
        try:
            import spacy
            self.nlp = spacy.load("en_core_web_sm")
            logger.info("spaCy model loaded successfully")
        except Exception as e:
            logger.warning(f"Could not load spaCy model: {e}")
            logger.warning("NLP features will be limited")
            self.nlp = None
        
        self._initialized = True
    
    def extract_fields(self, text: str) -> Dict[str, Any]:
        """
        Extract invoice fields from text.
        
        Args:
            text: Raw text extracted from PDF
            
        Returns:
            Dictionary of extracted fields
        """
        logger.info("Extracting fields from text...")
        
        # Normalize text for matching
        text_lower = text.lower()
        
        # Extract each field
        fields = {}
        
        # Invoice Number
        fields['invoice_number'] = self._extract_invoice_number(text)
        logger.debug(f"Invoice Number: {fields['invoice_number']}")
        
        # Customer Name
        fields['customer_name'] = self._extract_customer_name(text)
        logger.debug(f"Customer Name: {fields['customer_name']}")
        
        # Customer ID
        fields['customer_id'] = self._extract_customer_id(text)
        logger.debug(f"Customer ID: {fields['customer_id']}")
        
        # Opportunity Number
        fields['opportunity_number'] = self._extract_opportunity_number(text)
        logger.debug(f"Opportunity Number: {fields['opportunity_number']}")
        
        # Amount
        fields['amount'] = self._extract_amount(text)
        logger.debug(f"Amount: {fields['amount']}")
        
        # Currency
        fields['currency'] = self._extract_currency(text)
        logger.debug(f"Currency: {fields['currency']}")
        
        return fields
    
    def _extract_invoice_number(self, text: str) -> Optional[str]:
        """
        Extract invoice number from text.
        
        Looks for patterns like:
        - Invoice #: INV-2024-001
        - Invoice Number: 12345
        - Bill No: BILL-001
        - Billing Number: 98765
        """
        # Common invoice number labels
        labels = [
            r'invoice\s*(?:#|no\.?|number|num)[\s:]*',
            r'inv\s*(?:#|no\.?|number)[\s:]*',
            r'bill(?:ing)?\s*(?:#|no\.?|number)[\s:]*',
            r'document\s*(?:#|no\.?|number)[\s:]*',
            r'reference\s*(?:#|no\.?)[\s:]*',
        ]
        
        # Invoice number patterns (after the label)
        number_patterns = [
            r'([A-Z]{2,4}[-/]?\d{4,}[-/]?\d*)',  # INV-2024-001, INV/2024/001
            r'([A-Z]+[-]?\d+)',                    # INV-12345
            r'(\d{4,})',                           # 12345678
        ]
        
        for label in labels:
            for num_pattern in number_patterns:
                pattern = label + num_pattern
                match = re.search(pattern, text, re.IGNORECASE)
                if match:
                    return match.group(1).strip()
        
        return None
    
    def _extract_customer_name(self, text: str) -> Optional[str]:
        """
        Extract customer/vendor name from text.
        
        Strategies:
        1. Look for labels like "Bill To:", "Customer:"
        2. Use NLP to find organization names
        """
        # Labels that typically precede customer name
        labels = [
            r'(?:bill\s*to|sold\s*to|customer(?:\s*name)?|client(?:\s*name)?|vendor(?:\s*name)?|company(?:\s*name)?|buyer)[\s:]+',
        ]
        
        for label in labels:
            # Match the label followed by one or more lines
            pattern = label + r'([A-Z][A-Za-z0-9\s\.,&]+?)(?:\n|$)'
            match = re.search(pattern, text, re.IGNORECASE | re.MULTILINE)
            if match:
                name = match.group(1).strip()
                # Clean up the name
                name = re.sub(r'\s+', ' ', name)  # Normalize whitespace
                name = name.strip('.,')
                if len(name) > 2 and len(name) < 100:
                    return name
        
        # Fallback: Use NLP if available
        if self.nlp:
            try:
                doc = self.nlp(text[:2000])  # Analyze first 2000 chars
                for ent in doc.ents:
                    if ent.label_ == "ORG":
                        return ent.text
            except Exception as e:
                logger.warning(f"NLP extraction failed: {e}")
        
        return None
    
    def _extract_customer_id(self, text: str) -> Optional[str]:
        """
        Extract customer ID/code from text.
        """
        labels = [
            r'(?:customer|client|account|cust)\s*(?:id|code|no\.?|number|#)[\s:]+',
        ]
        
        id_patterns = [
            r'([A-Z]{2,4}[-]?\d{3,})',  # CUST-001, ACC123
            r'(\d{5,})',                 # 12345678
            r'([A-Z0-9]{4,})',           # ABC123
        ]
        
        for label in labels:
            for id_pattern in id_patterns:
                pattern = label + id_pattern
                match = re.search(pattern, text, re.IGNORECASE)
                if match:
                    return match.group(1).strip()
        
        return None
    
    def _extract_opportunity_number(self, text: str) -> Optional[str]:
        """
        Extract opportunity/reference number from text.
        """
        labels = [
            r'(?:opportunity|opp|deal|sales\s*order|so|po|purchase\s*order|project|quote)\s*(?:#|no\.?|number|id|ref(?:erence)?)[\s:]+',
        ]
        
        patterns = [
            r'([A-Z]{2,4}[-/]?\d{4,}[-/]?\d*)',  # OPP-2024-001
            r'([A-Z]+[-]?\d+)',                    # OPP-12345
            r'(\d{6,})',                           # 123456
        ]
        
        for label in labels:
            for pattern in patterns:
                full_pattern = label + pattern
                match = re.search(full_pattern, text, re.IGNORECASE)
                if match:
                    return match.group(1).strip()
        
        return None
    
    def _extract_amount(self, text: str) -> Optional[float]:
        """
        Extract the invoice total amount.
        
        Looks for the highest-priority amount label (Total, Grand Total, etc.)
        """
        # Priority order of amount labels (most specific first)
        labels = [
            r'grand\s*total[\s:]*',
            r'total\s*(?:amount|due)[\s:]*',
            r'amount\s*due[\s:]*',
            r'balance\s*due[\s:]*',
            r'invoice\s*total[\s:]*',
            r'net\s*(?:amount|total)[\s:]*',
            r'total[\s:]*',
            r'amount[\s:]*',
        ]
        
        # Amount pattern: handles $1,234.56 or 1234.56 or 1,234
        amount_pattern = r'[$€£]?\s*(\d{1,3}(?:,\d{3})*(?:\.\d{2})?|\d+(?:\.\d{2})?)'
        
        amounts_found = []
        
        for label in labels:
            pattern = label + amount_pattern
            matches = re.findall(pattern, text, re.IGNORECASE)
            for match in matches:
                try:
                    # Clean and convert the amount
                    clean_amount = match.replace(',', '')
                    amount = float(clean_amount)
                    if amount > 0:
                        amounts_found.append((label, amount))
                except ValueError:
                    continue
        
        # Return the first (highest priority) amount found
        if amounts_found:
            return amounts_found[0][1]
        
        # Fallback: find any large number that looks like an amount
        all_amounts = re.findall(r'[$€£]?\s*(\d{1,3}(?:,\d{3})*\.\d{2})', text)
        if all_amounts:
            amounts = [float(a.replace(',', '')) for a in all_amounts]
            # Return the largest amount (likely the total)
            return max(amounts) if amounts else None
        
        return None
    
    def _extract_currency(self, text: str) -> str:
        """
        Extract currency from text.
        
        Returns 'USD' as default if not found.
        """
        # Currency symbol patterns
        if re.search(r'[$]|USD|U\.S\.\s*Dollar', text, re.IGNORECASE):
            return 'USD'
        if re.search(r'[€]|EUR|Euro', text, re.IGNORECASE):
            return 'EUR'
        if re.search(r'[£]|GBP|Pound', text, re.IGNORECASE):
            return 'GBP'
        if re.search(r'CAD|Canadian\s*Dollar', text, re.IGNORECASE):
            return 'CAD'
        if re.search(r'AUD|Australian\s*Dollar', text, re.IGNORECASE):
            return 'AUD'
        
        return 'USD'  # Default
    
    def calculate_confidence(self, fields: Dict[str, Any]) -> float:
        """
        Calculate overall confidence score based on extracted fields.
        
        Returns a score from 0 to 100.
        """
        scores = {
            'invoice_number': 25,   # Most important
            'customer_name': 20,
            'amount': 25,           # Very important
            'customer_id': 15,
            'opportunity_number': 15,
        }
        
        total_possible = sum(scores.values())
        achieved = 0
        
        for field, score in scores.items():
            if fields.get(field) is not None:
                achieved += score
        
        confidence = (achieved / total_possible) * 100
        return round(confidence, 1)
```

---

#### 16.4.7 Field Patterns Utility

**Create file: `pdf-service/app/utils/field_patterns.py`**

```python
"""
Field Patterns for Invoice Data Extraction

This file contains regex patterns and label variations used to identify
invoice fields across different formats.
"""

# ============================================================================
# INVOICE NUMBER PATTERNS
# ============================================================================

INVOICE_NUMBER_LABELS = [
    # Primary patterns
    r'invoice\s*(?:#|no\.?|number)',
    r'inv\s*(?:#|no\.?)',
    
    # Billing variations
    r'bill(?:ing)?\s*(?:#|no\.?|number)',
    
    # Document/Reference
    r'document\s*(?:#|no\.?|number)',
    r'ref(?:erence)?\s*(?:#|no\.?)',
    
    # Tax invoice
    r'tax\s*invoice\s*(?:#|no\.?)',
]

# ============================================================================
# CUSTOMER NAME PATTERNS  
# ============================================================================

CUSTOMER_NAME_LABELS = [
    r'bill\s*to',
    r'sold\s*to',
    r'customer(?:\s*name)?',
    r'client(?:\s*name)?',
    r'company(?:\s*name)?',
    r'vendor(?:\s*name)?',
    r'buyer',
    r'purchaser',
    r'account\s*name',
]

# ============================================================================
# CUSTOMER ID PATTERNS
# ============================================================================

CUSTOMER_ID_LABELS = [
    r'customer\s*(?:id|code|#)',
    r'client\s*(?:id|code|#)',
    r'account\s*(?:no\.?|number|#)',
    r'cust\s*(?:id|#)',
    r'vendor\s*(?:id|code)',
]

# ============================================================================
# OPPORTUNITY/REFERENCE PATTERNS
# ============================================================================

OPPORTUNITY_LABELS = [
    r'opportunity\s*(?:#|no\.?|number|id)',
    r'opp\s*(?:#|no\.?)',
    r'deal\s*(?:#|no\.?|id)',
    r'sales\s*order\s*(?:#|no\.?)',
    r'so\s*(?:#|no\.?)',
    r'purchase\s*order\s*(?:#|no\.?)',
    r'po\s*(?:#|no\.?)',
    r'project\s*(?:#|no\.?|id)',
    r'quote\s*(?:#|no\.?|ref)',
    r'reference\s*(?:#|no\.?)',
    r'job\s*(?:#|no\.?)',
    r'order\s*(?:#|no\.?)',
]

# ============================================================================
# AMOUNT PATTERNS
# ============================================================================

AMOUNT_LABELS = [
    # Highest priority (most specific)
    r'grand\s*total',
    r'total\s*amount',
    r'invoice\s*total',
    r'total\s*due',
    r'amount\s*due',
    r'balance\s*due',
    r'net\s*total',
    r'net\s*amount',
    
    # Medium priority
    r'sub\s*total',
    r'subtotal',
    
    # Lower priority (generic)
    r'total',
    r'amount',
]

# ============================================================================
# CURRENCY PATTERNS
# ============================================================================

CURRENCY_PATTERNS = {
    'USD': [r'[$]', r'USD', r'U\.S\.\s*Dollar', r'US\s*Dollar'],
    'EUR': [r'[€]', r'EUR', r'Euro'],
    'GBP': [r'[£]', r'GBP', r'Pound\s*Sterling', r'British\s*Pound'],
    'CAD': [r'CAD', r'C\$', r'Canadian\s*Dollar'],
    'AUD': [r'AUD', r'A\$', r'Australian\s*Dollar'],
    'INR': [r'[₹]', r'INR', r'Indian\s*Rupee'],
    'JPY': [r'[¥]', r'JPY', r'Yen'],
}

# ============================================================================
# COMBINED FIELD PATTERNS
# ============================================================================

FIELD_PATTERNS = {
    'invoice_number': INVOICE_NUMBER_LABELS,
    'customer_name': CUSTOMER_NAME_LABELS,
    'customer_id': CUSTOMER_ID_LABELS,
    'opportunity_number': OPPORTUNITY_LABELS,
    'amount': AMOUNT_LABELS,
    'currency': CURRENCY_PATTERNS,
}
```

---

#### 16.4.8 Dockerfile for Python Service

**Create file: `pdf-service/Dockerfile`**

```dockerfile
# ============================================================================
# SyncLedger - PDF MICROSERVICE DOCKERFILE
# ============================================================================
# This file defines how to build a Docker container for the PDF service.
# ============================================================================

# Use Python 3.12 slim image
FROM python:3.12-slim

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Set work directory
WORKDIR /app

# Install system dependencies
# Tesseract OCR is required for scanned PDFs
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first (for Docker cache optimization)
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Download spaCy model
RUN python -m spacy download en_core_web_sm

# Copy application code
COPY app/ ./app/

# Create logs directory
RUN mkdir -p logs

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

#### 16.4.9 How to Run the Python Service

**Option A: Run directly (for development)**

```powershell
# Navigate to pdf-service folder
cd C:\Projects\syncledger\pdf-service

# Create a virtual environment
python -m venv venv

# Activate the virtual environment
.\venv\Scripts\Activate

# Install dependencies
pip install -r requirements.txt

# Download spaCy model
python -m spacy download en_core_web_sm

# Install Tesseract OCR (Windows)
# Download from: https://github.com/UB-Mannheim/tesseract/wiki
# Install to: C:\Program Files\Tesseract-OCR

# Create logs folder
mkdir logs

# Run the service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

**Option B: Run with Docker**

```powershell
# Navigate to pdf-service folder
cd C:\Projects\syncledger\pdf-service

# Build the Docker image
docker build -t syncledger-pdf-service .

# Run the container
docker run -d -p 8000:8000 --name pdf-service syncledger-pdf-service

# Check logs
docker logs pdf-service
```

---

### 16.5 Docker Compose - Run Everything Together

**Create file: `C:\Projects\syncledger\docker-compose.yml`**

```yaml
# ============================================================================
# SyncLedger - DOCKER COMPOSE
# ============================================================================
# Run all services with: docker-compose up -d
# ============================================================================

version: '3.8'

services:
  # ========================================================================
  # DATABASE - PostgreSQL
  # ========================================================================
  postgres:
    image: postgres:16-alpine
    container_name: syncledger-db
    environment:
      POSTGRES_DB: invoice_portal
      POSTGRES_USER: invoice_user
      POSTGRES_PASSWORD: YourSecurePassword123!
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U invoice_user -d invoice_portal"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ========================================================================
  # PDF MICROSERVICE - Python FastAPI
  # ========================================================================
  pdf-service:
    build:
      context: ./pdf-service
      dockerfile: Dockerfile
    container_name: syncledger-pdf-service
    ports:
      - "8000:8000"
    environment:
      - LOG_LEVEL=INFO
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8000/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ========================================================================
  # BACKEND API - Java Spring Boot
  # ========================================================================
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: syncledger-backend
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=invoice_portal
      - DB_USERNAME=invoice_user
      - DB_PASSWORD=YourSecurePassword123!
      - PDF_SERVICE_URL=http://pdf-service:8000
      # Add your Azure credentials here
      - AZURE_TENANT_ID=${AZURE_TENANT_ID}
      - AZURE_CLIENT_ID=${AZURE_CLIENT_ID}
      - AZURE_CLIENT_SECRET=${AZURE_CLIENT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      pdf-service:
        condition: service_started
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ========================================================================
  # FRONTEND - React (Optional, add if building frontend)
  # ========================================================================
  # frontend:
  #   build:
  #     context: ./frontend
  #     dockerfile: Dockerfile
  #   container_name: syncledger-frontend
  #   ports:
  #     - "3000:80"
  #   depends_on:
  #     - backend

volumes:
  postgres_data:
```

---

### 16.6 Running the Complete Application

#### Step-by-Step Execution Guide

```powershell
# ============================================================================
# STEP 1: Start everything with Docker Compose
# ============================================================================

cd C:\Projects\syncledger

# Create .env file with your credentials
# (Create this file manually with your actual values)

# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f


# ============================================================================
# STEP 2: Verify services are running
# ============================================================================

# Check PDF Service
curl http://localhost:8000/health
# Expected: {"status":"healthy","service":"PDF Extraction Service","version":"1.0.0"}

# Check Backend API
curl http://localhost:8080/api/dashboard/stats
# Expected: {"totalInvoices":0,...}

# Open Swagger UI in browser
start http://localhost:8080/api/swagger-ui.html


# ============================================================================
# STEP 3: Test PDF extraction
# ============================================================================

# Using PowerShell to upload a test PDF
$uri = "http://localhost:8000/extract"
$filePath = "C:\path\to\test-invoice.pdf"

Invoke-RestMethod -Uri $uri -Method Post -InFile $filePath -ContentType "multipart/form-data"
```

---

### 16.7 Quick Reference Commands

```powershell
# ============================================================================
#                    QUICK REFERENCE - COMMON COMMANDS
# ============================================================================

# ----- Docker Commands -----
docker-compose up -d          # Start all services
docker-compose down           # Stop all services
docker-compose logs -f        # View logs (follow)
docker-compose ps             # Check status
docker-compose restart        # Restart all services

# ----- Individual Service Logs -----
docker logs syncledger-backend
docker logs syncledger-pdf-service
docker logs syncledger-db

# ----- Database Access -----
docker exec -it syncledger-db psql -U invoice_user -d invoice_portal

# ----- Rebuild After Code Changes -----
docker-compose build backend
docker-compose build pdf-service
docker-compose up -d

# ----- Clean Up Everything -----
docker-compose down -v        # Remove containers AND volumes (deletes data!)
docker system prune -a        # Remove all unused Docker resources
```

---

## 18. INFRASTRUCTURE COST ESTIMATION

> **🎯 PURPOSE OF THIS SECTION**
> 
> This section provides detailed monthly and annual infrastructure costs to run the Invoice Processing Portal in a production environment. Costs are estimated for **Azure Cloud** (recommended) with alternatives for AWS and on-premises deployment.

---

### 17.1 Production Infrastructure Cost Summary

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│               TOTAL INFRASTRUCTURE COST - PRODUCTION ENVIRONMENT                 │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                                                                             │
  │   MONTHLY INFRASTRUCTURE COST:     $850 - $1,500 USD                       │
  │                                                                             │
  │   ANNUAL INFRASTRUCTURE COST:      $10,200 - $18,000 USD                   │
  │                                                                             │
  │   ─────────────────────────────────────────────────────────────────────    │
  │                                                                             │
  │   TIER BREAKDOWN:                                                          │
  │   ├── Small (< 500 invoices/month):      $850 - $1,000/month              │
  │   ├── Medium (500-2000 invoices/month):  $1,100 - $1,300/month            │
  │   └── Enterprise (2000+ invoices/month): $1,400 - $2,500/month            │
  │                                                                             │
  └─────────────────────────────────────────────────────────────────────────────┘
```

---

### 17.2 Azure Cloud Infrastructure (Recommended)

#### 17.2.1 Compute Services

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure App Service (Backend)** | P1v3 (2 vCPU, 8GB RAM) | Java Spring Boot API | $150 - $200 |
| **Azure Container Instance (PDF Service)** | 2 vCPU, 4GB RAM | Python FastAPI Microservice | $80 - $120 |
| **Azure App Service (Frontend)** | B2 (2 vCPU, 3.5GB RAM) | React Frontend | $55 - $75 |
| **Azure Functions** | Consumption Plan | Email polling, async tasks | $15 - $40 |
| **Subtotal Compute** | | | **$300 - $435** |

#### 17.2.2 Database Services

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure Database for PostgreSQL** | General Purpose, 2 vCores | Primary database | $130 - $180 |
| **Azure Cache for Redis** | Basic C1 (1GB) | Session cache, rate limiting | $25 - $40 |
| **Subtotal Database** | | | **$155 - $220** |

#### 17.2.3 Storage Services

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure Blob Storage** | Hot tier, 500GB | PDF file storage | $25 - $40 |
| **Azure Blob Storage** | Cool tier, 1TB | Archived invoices | $15 - $25 |
| **Bandwidth (Egress)** | ~100GB/month | Data transfer out | $10 - $20 |
| **Subtotal Storage** | | | **$50 - $85** |

#### 17.2.4 AI & Cognitive Services

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure AI Document Intelligence** | S0 tier (1000 pages/month) | Advanced OCR/extraction | $50 - $100 |
| **Azure OpenAI (Optional)** | GPT-4o mini | Intelligent field extraction | $30 - $80 |
| **Subtotal AI Services** | | | **$50 - $180** |

#### 17.2.5 Identity & Security

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure Active Directory (Entra ID)** | P1 License (per user) | SSO, MFA, user management | $6/user |
| **Azure Key Vault** | Standard | Secrets management | $5 - $10 |
| **Azure DDoS Protection** | Basic (included) | DDoS protection | $0 |
| **SSL Certificates** | App Service Managed | HTTPS | $0 |
| **Subtotal Identity (10 users)** | | | **$65 - $80** |

#### 17.2.6 Monitoring & Logging

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure Monitor / Log Analytics** | 5GB/day ingestion | Centralized logging | $50 - $100 |
| **Application Insights** | Basic tier | APM, performance tracking | $25 - $50 |
| **Azure Alerts** | 10 alert rules | Proactive monitoring | $5 - $15 |
| **Subtotal Monitoring** | | | **$80 - $165** |

#### 17.2.7 Networking

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Azure Load Balancer** | Standard | Traffic distribution | $20 - $30 |
| **Azure DNS** | Zone hosting | Domain management | $1 - $5 |
| **Azure Front Door (Optional)** | Standard | Global CDN, WAF | $35 - $50 |
| **Virtual Network** | Basic | Network isolation | $0 |
| **Subtotal Networking** | | | **$21 - $85** |

#### 17.2.8 Integration Services

| Service | Configuration | Purpose | Monthly Cost |
|---------|--------------|---------|--------------|
| **Microsoft Graph API** | Included with M365 | Outlook email reading | $0* |
| **Azure Service Bus** | Standard tier | Message queuing | $10 - $25 |
| **Subtotal Integration** | | | **$10 - $25** |

*\*Requires Microsoft 365 license (assumed existing)*

---

### 17.3 Azure Total Cost by Tier

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    AZURE MONTHLY COST BREAKDOWN BY TIER                          │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  SMALL DEPLOYMENT (< 500 invoices/month, 5 users)                          │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  Compute (App Service B2, Container Basic)        $180                     │
  │  Database (PostgreSQL Basic, 2 vCores)            $100                     │
  │  Storage (100GB Hot)                              $25                      │
  │  AI Services (Document Intelligence S0)           $50                      │
  │  Identity (5 users × $6)                          $30                      │
  │  Monitoring (Basic)                               $50                      │
  │  Networking (Basic LB, DNS)                       $25                      │
  │  Integration (Service Bus Basic)                  $10                      │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  TOTAL SMALL:                              ~$470 - $600/month              │
  │  ANNUAL:                                   ~$5,640 - $7,200/year           │
  └─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  MEDIUM DEPLOYMENT (500-2000 invoices/month, 15 users)                     │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  Compute (App Service P1v3, Container 2vCPU)      $350                     │
  │  Database (PostgreSQL GP, 2 vCores + Redis)       $180                     │
  │  Storage (500GB Hot, 500GB Cool)                  $55                      │
  │  AI Services (Document Intelligence + GPT)        $130                     │
  │  Identity (15 users × $6)                         $90                      │
  │  Monitoring (Standard)                            $100                     │
  │  Networking (Standard LB, Front Door)             $60                      │
  │  Integration (Service Bus Standard)               $20                      │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  TOTAL MEDIUM:                             ~$985 - $1,200/month            │
  │  ANNUAL:                                   ~$11,820 - $14,400/year         │
  └─────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │  ENTERPRISE DEPLOYMENT (2000+ invoices/month, 50+ users, HA)               │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  Compute (App Service P2v3 × 2, AKS cluster)      $600                     │
  │  Database (PostgreSQL GP 4 vCores + Read Replica) $400                     │
  │  Storage (2TB Hot, 5TB Cool, Archive)             $120                     │
  │  AI Services (Document Intelligence + GPT-4)      $250                     │
  │  Identity (50 users × $6)                         $300                     │
  │  Monitoring (Enterprise + Log Analytics)          $200                     │
  │  Networking (Front Door, WAF, Private Link)       $150                     │
  │  Integration (Service Bus Premium)                $50                      │
  │  DR/Backup (Geo-redundant backup)                 $100                     │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  TOTAL ENTERPRISE:                         ~$2,170 - $2,800/month          │
  │  ANNUAL:                                   ~$26,040 - $33,600/year         │
  └─────────────────────────────────────────────────────────────────────────────┘
```

---

### 17.4 AWS Alternative Pricing

| AWS Service | Azure Equivalent | Configuration | Monthly Cost |
|-------------|------------------|---------------|--------------|
| **EC2 (t3.medium × 2)** | App Service | 2 vCPU, 4GB RAM | $75 × 2 = $150 |
| **ECS Fargate** | Container Instance | Python PDF Service | $80 - $100 |
| **RDS PostgreSQL** | Azure PostgreSQL | db.t3.medium | $120 - $160 |
| **S3** | Blob Storage | 500GB | $25 - $35 |
| **Textract** | Document Intelligence | OCR/Extraction | $50 - $100 |
| **CloudWatch** | Azure Monitor | Logging/Metrics | $50 - $80 |
| **ALB** | Load Balancer | Application LB | $25 - $40 |
| **Route 53** | Azure DNS | DNS Hosting | $5 |
| **Cognito** | Azure AD | User Auth | $30 - $50 |
| **SQS** | Service Bus | Message Queue | $10 - $20 |
| **Secrets Manager** | Key Vault | Secrets | $5 - $10 |
| **Total AWS** | | | **$625 - $950/month** |

---

### 17.5 On-Premises/Self-Hosted Costs

If deploying on-premises or on self-managed infrastructure:

#### 17.5.1 Hardware Requirements (Minimum Production)

| Component | Specification | One-Time Cost |
|-----------|--------------|---------------|
| **Application Server** | 8 vCPU, 32GB RAM, 500GB SSD | $3,000 - $5,000 |
| **Database Server** | 8 vCPU, 64GB RAM, 1TB NVMe | $5,000 - $8,000 |
| **Storage (NAS/SAN)** | 10TB usable, RAID | $3,000 - $6,000 |
| **Network Equipment** | Firewall, Switch, Router | $2,000 - $4,000 |
| **Backup System** | NAS + Cloud backup | $1,500 - $3,000 |
| **Total Hardware** | | **$14,500 - $26,000** |

#### 17.5.2 Recurring On-Premises Costs

| Item | Monthly Cost |
|------|--------------|
| **Electricity** | $100 - $200 |
| **Internet (Business)** | $150 - $300 |
| **Software Licenses** | $200 - $400 |
| **Maintenance/Support** | $300 - $500 |
| **Cloud Backup** | $50 - $100 |
| **Total Monthly** | **$800 - $1,500** |

---

### 17.6 Third-Party Service Costs

| Service | Purpose | Monthly Cost |
|---------|---------|--------------|
| **Sage API** | Accounting Integration | Included with Sage subscription* |
| **SendGrid/Mailgun** | Email Notifications (10K emails) | $15 - $30 |
| **Twilio (Optional)** | SMS Notifications | $20 - $50 |
| **Domain Registration** | .com domain | $1 - $2 |
| **SSL Certificate** | Wildcard (if not using Azure) | $10 - $20 |
| **Total Third-Party** | | **$46 - $102** |

*\*Assumes existing Sage subscription*

---

### 17.7 DevOps & Operational Costs

| Item | Description | Monthly Cost |
|------|-------------|--------------|
| **Azure DevOps / GitHub** | CI/CD pipelines, repos | $0 - $40 |
| **Container Registry** | Docker image storage | $5 - $20 |
| **Artifact Storage** | Build artifacts | $5 - $15 |
| **Testing Tools** | Automated testing | $0 - $50 |
| **Total DevOps** | | **$10 - $125** |

---

### 17.8 Complete Cost Summary Table

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                 COMPLETE INFRASTRUCTURE COST SUMMARY                             │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┬───────────────┬───────────────┬───────────────┐
│ CATEGORY             │ SMALL         │ MEDIUM        │ ENTERPRISE    │
├──────────────────────┼───────────────┼───────────────┼───────────────┤
│ Compute              │ $180          │ $350          │ $600          │
│ Database             │ $100          │ $180          │ $400          │
│ Storage              │ $25           │ $55           │ $120          │
│ AI/ML Services       │ $50           │ $130          │ $250          │
│ Identity/Security    │ $30           │ $90           │ $300          │
│ Monitoring           │ $50           │ $100          │ $200          │
│ Networking           │ $25           │ $60           │ $150          │
│ Integration          │ $10           │ $20           │ $50           │
│ Third-Party          │ $50           │ $75           │ $100          │
│ DevOps               │ $10           │ $40           │ $80           │
│ DR/Backup            │ $0            │ $50           │ $100          │
├──────────────────────┼───────────────┼───────────────┼───────────────┤
│ MONTHLY TOTAL        │ ~$530         │ ~$1,150       │ ~$2,350       │
│ ANNUAL TOTAL         │ ~$6,360       │ ~$13,800      │ ~$28,200      │
└──────────────────────┴───────────────┴───────────────┴───────────────┘
```

---

### 17.9 Cost Optimization Recommendations

#### 17.9.1 Immediate Savings

| Optimization | Potential Savings |
|-------------|-------------------|
| **Reserved Instances (1-year)** | 20-30% on compute |
| **Reserved Instances (3-year)** | 40-50% on compute |
| **Azure Hybrid Benefit** | Up to 40% (if existing Windows licenses) |
| **Spot/Preemptible VMs (Dev/Test)** | 60-80% on non-prod |
| **Auto-scaling** | 20-40% by scaling down during off-hours |

#### 17.9.2 Architecture Optimizations

| Optimization | Description | Savings |
|-------------|-------------|---------|
| **Serverless PDF Service** | Use Azure Functions for PDF processing | $50-100/month |
| **Cool/Archive Storage** | Move old invoices to cold storage | 50-80% storage |
| **Database Right-sizing** | Use burstable DB tier | $30-50/month |
| **CDN for Static Assets** | Offload frontend to CDN | $20-30/month |

---

### 17.10 Total Cost of Ownership (TCO) - First Year

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                 FIRST YEAR TOTAL COST OF OWNERSHIP (TCO)                         │
└─────────────────────────────────────────────────────────────────────────────────┘

  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                                                                             │
  │  ONE-TIME COSTS (Development + Setup)                                      │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  Development (800 hours @ $80/hr)                        $64,000           │
  │  Azure Setup & Configuration                             $5,000            │
  │  Security Audit & Penetration Testing                    $3,000            │
  │  Documentation & Training                                $2,000            │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  SUBTOTAL ONE-TIME:                                      $74,000           │
  │                                                                             │
  │  RECURRING COSTS (12 months - Medium Tier)                                 │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  Infrastructure ($1,150/month × 12)                      $13,800           │
  │  Maintenance & Support (15% of dev)                      $9,600            │
  │  ─────────────────────────────────────────────────────────────────────────  │
  │  SUBTOTAL RECURRING:                                     $23,400           │
  │                                                                             │
  │  ═══════════════════════════════════════════════════════════════════════   │
  │                                                                             │
  │  FIRST YEAR TCO:                                        $97,400            │
  │                                                                             │
  │  SUBSEQUENT YEARS TCO (Recurring only):                 ~$23,400/year      │
  │                                                                             │
  └─────────────────────────────────────────────────────────────────────────────┘
```

---

### 17.11 Cost Comparison: Build vs Buy

| Factor | Build (This Project) | Buy (SaaS Solution) |
|--------|---------------------|---------------------|
| **Year 1 Cost** | ~$97,400 | $30,000 - $60,000 |
| **Year 2+ Cost** | ~$23,400/year | $30,000 - $60,000/year |
| **Break-even** | ~2-3 years | - |
| **Customization** | Full control | Limited |
| **Data Ownership** | Complete | Vendor-dependent |
| **Integration Flexibility** | Unlimited | API-dependent |
| **Long-term Cost (5 years)** | ~$190,000 | ~$180,000 - $300,000 |

**Recommendation:** Building custom is cost-effective if:
- You need deep customization
- You process >1000 invoices/month
- You have specific compliance requirements
- You plan to use the system for 3+ years

---

**END OF DOCUMENT**
