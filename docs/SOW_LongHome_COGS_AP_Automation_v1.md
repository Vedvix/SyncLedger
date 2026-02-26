# Scope of Work (SOW)

## COGS-Related Accounts Payable Automation

---

| | |
|---|---|
| **Client** | Long Home Products |
| **Industry** | Home Improvement & Construction |
| **Prepared by** | Evolotek Solutions Pvt Ltd |
| **Date** | February 2026 |
| **Document Version** | 1.0 |
| **Classification** | Confidential |

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Workstreams / Skills Included](#2-workstreams--skills-included)
3. [Solution Architecture](#3-solution-architecture)
4. [Project Timeline](#4-project-timeline)
5. [Responsibilities](#5-responsibilities)
6. [Definition of Success](#6-definition-of-success)
7. [Systems Involved](#7-systems-involved)
8. [Assumptions & Constraints](#8-assumptions--constraints)
9. [Commercials & Pricing](#9-commercials--pricing)
10. [Confidentiality & Legal](#10-confidentiality--legal)
11. [Appendix](#11-appendix)

---

## 1. Project Overview

Evolotek Solutions Private Limited ("Evolotek") will develop and deploy an **AI-powered Accounts Payable Automation solution** to automate the processing of material and labor invoices directly tied to Long Home Products' ("Client") **Cost of Goods Sold (COGS)**.

The solution leverages **intelligent document processing (IDP)** combining OCR, AI-based field extraction, and configurable business rules to:

- **Eliminate manual data entry** for COGS-related vendor invoices
- **Improve posting accuracy** through automated GL coding, project coding, and PO matching
- **Accelerate invoice cycle times** from receipt to ERP posting within the Client's Sage Intacct environment

This engagement focuses on **high-ROI AP automation for COGS invoices** — the highest-volume, most repetitive segment of the Client's AP workflow — to establish immediate operational gains and measurable cost savings before expanding to broader accounts payable and financial workflows.

### 1.1 Business Context

Long Home Products processes a significant volume of vendor invoices for materials and subcontractor labor across multiple job sites. Today, these invoices are manually keyed into Sage Intacct, requiring AP staff to:

- Read and interpret invoice PDFs from multiple vendor formats
- Look up and apply GL account codes and job cost allocations
- Cross-reference purchase orders for three-way matching
- Handle exceptions, duplicates, and data discrepancies

This manual process is **error-prone, time-consuming, and does not scale** with business growth. Evolotek's solution directly addresses these pain points.

### 1.2 Objectives

| # | Objective | Target |
|---|-----------|--------|
| 1 | Reduce manual data entry for COGS AP invoices | 90%+ reduction |
| 2 | Achieve high first-pass posting accuracy | 85%+ without human intervention |
| 3 | Accelerate invoice-to-posting cycle time | 50%+ reduction |
| 4 | Provide visibility into AP pipeline status | Real-time dashboard |
| 5 | Establish foundation for broader AP automation | Phase 2 readiness |

---

## 2. Workstreams / Skills Included

### 2.1 Invoice Ingestion

Automated capture of vendor invoices for materials and labor through multiple channels:

- **Email Ingestion** — Automated polling of designated AP mailbox(es); PDF attachments are detected, extracted, and queued for processing. Non-invoice attachments are filtered out.
- **Direct Upload** — Manual upload portal for invoices received via mail, fax, or hand-delivery.
- **Duplicate Detection** — Invoices with matching invoice numbers or similar header details are automatically flagged to prevent double-posting.

### 2.2 AI-Powered Data Extraction

Hybrid extraction pipeline that handles **variable vendor formats without fixed templates**:

- **Digital PDFs** — Direct text extraction for electronically generated invoices.
- **Scanned / Image PDFs** — OCR-based extraction using Tesseract and/or AWS Textract for photographed or scanned documents.
- **Semantic Field Mapping** — AI-driven identification of key invoice fields (vendor name, invoice number, date, line items, amounts, tax, totals) regardless of document layout.

### 2.3 GL Coding & Project Coding

Automated application of predefined accounting and job cost rules:

- Configurable **field mapping profiles** tailored to Long Home's chart of accounts.
- Automatic assignment of **GL account codes**, **department codes**, and **job/project cost allocations** based on vendor, material type, and PO reference.
- Rules engine supports **multi-line invoices** with different GL coding per line item.

### 2.4 Purchase Order (PO) Matching

Automated matching of invoices to purchase orders where applicable:

- **Two-way match** — Invoice-to-PO header comparison (vendor, PO number, total amount).
- **Three-way match** — Invoice-to-PO-to-receipt for materials with receiving documentation.
- **Tolerance thresholds** — Configurable variance limits for price and quantity discrepancies.

### 2.5 ERP Entry (Sage Intacct Integration)

Post processed and approved invoices directly into Sage Intacct:

- Create **AP bills / purchase invoices** with complete line item detail.
- Apply correct **tax rates, ledger accounts, and dimensional coding** (location, department, project).
- Track **sync status** with automatic retry on transient failures.
- Full **audit trail** of every invoice synced to ERP.

### 2.6 Exception Handling & Human-in-the-Loop

Intelligent routing of exceptions for AP team review:

- **Low-confidence extractions** flagged for manual verification before posting.
- **PO mismatches** (price, quantity, vendor) routed to exception queue with highlighted discrepancies.
- **Missing data** (no PO reference, unknown vendor, incomplete line items) flagged with clear indicators.
- **Approval workflow** with role-based access — Reviewers validate, Approvers authorize posting.
- **Rejection with comments** — Declined invoices include reason codes for vendor follow-up.

---

## 3. Solution Architecture

### 3.1 High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     LONG HOME AP TEAM                        │
│              (Review Dashboard & Approvals)                  │
└──────────────┬──────────────────────────┬────────────────────┘
               │                          │
               ▼                          ▼
┌──────────────────────┐    ┌──────────────────────────────────┐
│   Web Dashboard      │    │   Email Ingestion Service        │
│   (React / Tailwind) │    │   (Mailbox Polling)              │
└──────────┬───────────┘    └──────────────┬───────────────────┘
           │                               │
           ▼                               ▼
┌──────────────────────────────────────────────────────────────┐
│              BACKEND API (Spring Boot / Java)                │
│   • Authentication & RBAC   • Invoice Lifecycle Management   │
│   • Approval Workflows      • Audit Logging                  │
│   • Sage Intacct Sync       • Dashboard Analytics            │
└──────────────┬───────────────────────────┬───────────────────┘
               │                           │
               ▼                           ▼
┌──────────────────────────┐  ┌────────────────────────────────┐
│  AI Extraction Service   │  │   Sage Intacct ERP             │
│  (Python / FastAPI)      │  │   (REST API Integration)       │
│  • OCR (Tesseract)       │  │   • AP Bill Creation           │
│  • AI Field Extraction   │  │   • GL & Dimensional Posting   │
│  • Mapping Engine        │  │   • Sync Status Tracking       │
└──────────────┬───────────┘  └────────────────────────────────┘
               │
               ▼
┌──────────────────────────┐
│  PostgreSQL Database     │
│  • Invoice Records       │
│  • Mapping Rules         │
│  • Audit Trail           │
└──────────────────────────┘
```

### 3.2 Technology Stack

| Component | Technology |
|-----------|-----------|
| Frontend Dashboard | React 18, TypeScript, Tailwind CSS |
| Backend API | Java 21, Spring Boot 3, Spring Security |
| AI / Extraction Engine | Python 3.12, FastAPI, PyMuPDF, Tesseract OCR |
| Database | PostgreSQL 16 |
| File Storage | AWS S3 (encrypted at rest) |
| Message Queue | AWS SQS (with dead-letter queue for failed messages) |
| ERP Integration | Sage Intacct REST API |
| Infrastructure | AWS (ECS / EC2, RDS, S3, SQS) |

### 3.3 Security & Compliance

- **Role-Based Access Control (RBAC)** — Admin, Approver, Reviewer, Viewer roles with granular permissions.
- **JWT-based authentication** with secure session management.
- **Data encryption** in transit (TLS 1.2+) and at rest (AES-256).
- **Comprehensive audit logging** — Every action (view, edit, approve, reject, sync) is recorded with timestamp, user, and organization context.
- **Data isolation** — Client data is logically separated with organization-scoped access controls.

---

## 4. Project Timeline

| Week | Date Range | Milestone | Key Activities |
|------|-----------|-----------|----------------|
| **Week 1** | Feb 09 – Feb 15 | **Kickoff & Discovery** | Kickoff meeting with stakeholders; data access provisioning; recurring meeting schedule established; collect sample invoices and AP process documentation |
| **Week 2** | Feb 16 – Feb 22 | **Configuration & Rulebook** | Data sampling and analysis of COGS invoice formats; AP rulebook drafting (GL coding rules, PO matching logic, exception criteria); Sage Intacct test environment setup; mapping profile configuration |
| **Week 3** | Feb 23 – Feb 28 | **Boardroom Pilot** | End-to-end pilot with real COGS invoices in test environment; live demonstration of extraction → coding → matching → posting workflow; refinement based on client feedback; formal pilot approval |
| **Week 4** | Mar 02 – Mar 08 | **Go-Live & Monitoring** | Production deployment for COGS-related AP automation; hypercare support; optimization and performance tuning; weekly performance reporting initiation |

### 4.1 Post Go-Live Support

- **Weeks 5–8**: Performance monitoring, accuracy tuning, and ongoing optimization.
- **Weekly performance reports** delivered to AP process owner covering volume, accuracy, exception rates, and cycle time metrics.
- **Issue resolution SLA**: Critical issues addressed within 4 business hours; standard issues within 1 business day.

---

## 5. Responsibilities

### 5.1 Client Checklist — Long Home Products

| # | Responsibility | Due By |
|---|---------------|--------|
| 1 | Provide **15 sample COGS-related AP invoices** (covering top vendors, varied formats) | Week 1 |
| 2 | Grant **Sage Intacct test environment access** (read/write API credentials) | Week 1 |
| 3 | Confirm **AP process owner** and designated point of contact | Week 1 |
| 4 | Complete **business logic signoff** — PO formats, GL coding rules, job cost allocation rules, exception handling preferences | Week 2 |
| 5 | Participate in **Boardroom Pilot** with key stakeholders | Week 3 |
| 6 | Approve **go-live readiness** and authorize production deployment | Week 3 |
| 7 | Provide **Sage Intacct production credentials** for go-live | Week 4 |

### 5.2 Evolotek Solutions Checklist

| # | Responsibility | Due By |
|---|---------------|--------|
| 1 | **Configure & develop** automation solution tailored to Client's COGS AP workflow | Weeks 1–3 |
| 2 | Draft **AP automation blueprint** documenting extraction rules, GL coding logic, and exception handling flows | Week 2 |
| 3 | **Train AI extraction models** on Client's invoice formats; run internal validation against sample invoices | Week 2 |
| 4 | Facilitate **Boardroom Pilot** — prepare test cases, run live demonstration, document results | Week 3 |
| 5 | Complete **go-live configuration** — production deployment, Sage Intacct sync activation, monitoring setup | Week 4 |
| 6 | Provide **weekly performance updates** post go-live (accuracy, volume, exceptions, cycle time) | Ongoing |
| 7 | Deliver **hypercare support** during first two weeks post go-live | Weeks 4–5 |

---

## 6. Definition of Success

The following metrics define successful delivery of this engagement. All metrics will be measured during the Boardroom Pilot and validated during the first 30 days of production operation.

| # | Success Metric | Target | Measurement Method |
|---|---------------|--------|-------------------|
| 1 | **First-pass posting accuracy** — Invoices posted to Sage Intacct without human correction | **85%+** | (Auto-posted invoices ÷ Total invoices) × 100 |
| 2 | **Manual entry reduction** — Decrease in AP staff touchpoints for COGS invoices | **90%+** | Comparison of pre- vs. post-automation keystroke/field entry counts |
| 3 | **Cycle time reduction** — Time from invoice receipt to ERP posting | **50%+** | Average processing time pre- vs. post-automation |
| 4 | **Stakeholder sign-off** — Boardroom Pilot approved by AP process owner | **Approved** | Formal written approval |
| 5 | **Exception rate** — Percentage of invoices requiring manual intervention | **< 15%** | (Exception invoices ÷ Total invoices) × 100 |

### 6.1 Acceptance Criteria

- All five success metrics met or exceeded during Boardroom Pilot with representative invoice sample.
- Client AP process owner provides written go-live approval.
- Solution successfully posts invoices to Sage Intacct production environment with correct GL coding and dimensional allocation.

---

## 7. Systems Involved

| System | Role | Access Required |
|--------|------|----------------|
| **Sage Intacct** | Primary ERP — AP bill creation, GL posting, dimensional coding | Read/write API credentials for test and production environments |
| **Email / Mailbox** | Invoice receipt channel | Designated AP mailbox access for automated polling |
| **Vendor Invoices** | Source documents | PDF, email attachments, EDI (where applicable) |
| **Purchase Orders** | PO matching reference | PO data export or API access from Sage Intacct |
| **Job Cost Coding Rules** | GL and project allocation logic | Business rules document from Client AP team |
| **Evolotek Cloud Platform** | Hosting, processing, and dashboard | AWS infrastructure managed by Evolotek |

### 7.1 Integration Points

```
Vendor Invoices ──→ [ Email / Upload ] ──→ Evolotek Platform ──→ Sage Intacct
       │                                         │
       └── PDF, Email, EDI                       ├── AP Bills
                                                  ├── GL Entries
                                                  ├── Job Cost Allocations
                                                  └── Dimensional Coding
```

---

## 8. Assumptions & Constraints

### 8.1 Assumptions

1. Client will provide timely access to Sage Intacct test and production environments as outlined in the timeline.
2. Sample invoices provided are representative of the full range of COGS vendor invoice formats.
3. Client's existing GL chart of accounts and job cost structure will remain stable during the engagement.
4. Sage Intacct API is available and supports the required bill creation and posting endpoints.
5. Client will designate a single AP process owner with decision-making authority for business logic sign-off.
6. Invoice volume during pilot will be representative of typical monthly volume.

### 8.2 Constraints

1. Scope is limited to **COGS-related AP invoices** (materials and labor). Non-COGS AP, AR, and other financial workflows are excluded from this phase.
2. Custom Sage Intacct module development or modifications to the Client's ERP configuration are **out of scope**.
3. Historical invoice migration or backlog processing is **not included** in this engagement.
4. The solution supports **English-language invoices** only in this phase.

### 8.3 Change Management

Any changes to scope, timeline, or deliverables will be documented via a **Change Request (CR)** process. CRs will be jointly evaluated for impact on timeline and cost before approval.

---

## 9. Commercials & Pricing

*[To be discussed and finalized based on engagement model — fixed fee, monthly subscription, or per-invoice pricing. Evolotek recommends a phased pricing approach with a fixed implementation fee for Weeks 1–4 and a monthly subscription for ongoing processing and support.]*

### 9.1 Suggested Pricing Models

| Model | Structure | Best For |
|-------|----------|----------|
| **Fixed Implementation + Monthly SaaS** | One-time setup fee + per-month platform fee | Predictable budgeting |
| **Per-Invoice Processing** | Fee per successfully processed invoice | Volume-based cost alignment |
| **Hybrid** | Fixed setup + per-invoice over baseline volume | Balanced risk sharing |

### 9.2 What's Included

- Solution configuration, deployment, and testing
- Boardroom Pilot facilitation
- Go-live support and hypercare (2 weeks)
- Ongoing platform hosting and maintenance
- Weekly performance reporting
- Standard support (business hours)

---

## 10. Confidentiality & Legal

- All Client data (invoices, vendor information, financial records) will be treated as **strictly confidential**.
- Data is encrypted in transit and at rest using industry-standard encryption protocols.
- Evolotek will execute a **Non-Disclosure Agreement (NDA)** and **Data Processing Agreement (DPA)** prior to receiving any Client data.
- Client retains full ownership of all data processed through the platform.
- Evolotek will comply with applicable data protection regulations.

---

## 11. Appendix

### Appendix A — Glossary

| Term | Definition |
|------|-----------|
| **COGS** | Cost of Goods Sold — direct costs attributable to production of goods/services sold |
| **AP** | Accounts Payable — money owed by a company to its suppliers |
| **GL** | General Ledger — master accounting record |
| **PO** | Purchase Order — document authorizing a purchase |
| **IDP** | Intelligent Document Processing — AI-powered document data extraction |
| **OCR** | Optical Character Recognition — technology to extract text from images/scans |
| **ERP** | Enterprise Resource Planning — business management software (e.g., Sage Intacct) |
| **Boardroom Pilot** | Controlled live demonstration of the solution with real data for stakeholder validation |

### Appendix B — Sample Invoice Processing Flow

```
1. Invoice Received (Email / Upload)
         │
2. AI Extraction (Vendor, Amount, Line Items, PO#)
         │
3. GL Coding & Job Cost Allocation (Rules Engine)
         │
4. PO Matching (2-way / 3-way)
         │
    ┌────┴────┐
    │         │
 Match ✓   Mismatch ✗
    │         │
    ▼         ▼
5a. Auto-    5b. Exception
    Approve       Queue
    │              │
    │         AP Review
    │         & Correction
    │              │
    └──────┬───────┘
           │
6. Post to Sage Intacct
           │
7. Confirmation & Audit Log
```

### Appendix C — Contact Information

| Role | Name | Email |
|------|------|-------|
| **Evolotek Project Lead** | *[To be confirmed]* | *[To be confirmed]* |
| **Evolotek Technical Lead** | *[To be confirmed]* | *[To be confirmed]* |
| **Long Home AP Process Owner** | *[To be confirmed]* | *[To be confirmed]* |
| **Long Home IT Contact** | *[To be confirmed]* | *[To be confirmed]* |

---

*This Scope of Work is intended for discussion purposes and is subject to mutual agreement. Final terms will be confirmed upon execution of a Master Services Agreement (MSA) between Evolotek Solutions Pvt Ltd and Long Home Products.*

---

**Evolotek Solutions Pvt Ltd**
*Empowering Financial Operations Through Intelligent Automation*
