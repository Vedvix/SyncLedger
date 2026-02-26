# Scope of Work (SOW)

## COGS-Related Accounts Payable Automation

---

| | |
|---|---|
| **Client** | Long Home Products |
| **Industry** | Home Improvement & Construction |
| **Prepared by** | Evolotek Solutions Pvt Ltd |
| **Date** | February 2026 |
| **Document Version** | 2.0 |
| **Classification** | Confidential |

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Platform Capabilities](#2-platform-capabilities)
3. [Workstreams / Skills Included](#3-workstreams--skills-included)
4. [Solution Architecture](#4-solution-architecture)
5. [Project Timeline](#5-project-timeline)
6. [Responsibilities](#6-responsibilities)
7. [Definition of Success](#7-definition-of-success)
8. [Systems Involved](#8-systems-involved)
9. [Assumptions & Constraints](#9-assumptions--constraints)
10. [Phase 2 — Future Scope & Expansion](#10-phase-2--future-scope--expansion)
11. [Commercials & Pricing](#11-commercials--pricing)
12. [Confidentiality & Legal](#12-confidentiality--legal)
13. [Appendix](#13-appendix)

---

## 1. Project Overview

Evolotek Solutions Private Limited ("Evolotek") will configure and deploy its **production-ready, AI-powered Accounts Payable Automation platform** to automate the processing of material and labor invoices directly tied to Long Home Products' ("Client") **Cost of Goods Sold (COGS)**.

The platform leverages a **3-tier GPT-4o AI extraction engine** combined with OCR, configurable business rules, and an approval workflow to:

- **Eliminate manual data entry** for COGS-related vendor invoices
- **Improve posting accuracy** through automated GL coding, project coding, and PO matching
- **Accelerate invoice cycle times** from receipt to ERP posting within the Client's Sage Intacct environment

This engagement focuses on **high-ROI AP automation for COGS invoices** — the highest-volume, most repetitive segment of the Client's AP workflow — to establish immediate operational gains and measurable cost savings before expanding to broader accounts payable and financial workflows.

> **Key Differentiator:** Evolotek's platform is not a proof-of-concept. The core automation engine — including AI extraction, approval workflows, real-time dashboards, vendor management, and Excel reporting — is **already built, tested, and production-deployed**. This engagement configures and connects the platform to Long Home's specific vendors, GL structure, and Sage Intacct ERP, dramatically reducing time-to-value versus building from scratch.

### 1.1 Business Context

Long Home Products processes a significant volume of vendor invoices for materials and subcontractor labor across multiple job sites. Today, these invoices are manually keyed into Sage Intacct, requiring AP staff to:

- Read and interpret invoice PDFs from multiple vendor formats
- Look up and apply GL account codes and job cost allocations
- Cross-reference purchase orders for three-way matching
- Handle exceptions, duplicates, and data discrepancies

This manual process is **error-prone, time-consuming, and does not scale** with business growth. Evolotek's platform directly addresses these pain points.

### 1.2 Objectives

| # | Objective | Target |
|---|-----------|--------|
| 1 | Reduce manual data entry for COGS AP invoices | 90%+ reduction |
| 2 | Achieve high first-pass posting accuracy | 85%+ without human intervention |
| 3 | Accelerate invoice-to-posting cycle time | 50%+ reduction |
| 4 | Provide visibility into AP pipeline status | Real-time dashboard |
| 5 | Establish foundation for broader AP automation | Phase 2 readiness |

---

## 2. Platform Capabilities

Evolotek's automation platform is a **multi-tenant SaaS solution** with the following production-ready capabilities that Long Home will benefit from on day one:

### 2.1 What's Already Built (Production-Ready)

| Capability | Description | Status |
|-----------|-------------|--------|
| **3-Tier AI Extraction Engine** | GPT-4o Vision → GPT-4o Text → Regex fallback cascade with cross-validation and confidence scoring. Handles any vendor invoice format without templates. | **Live** |
| **Email Ingestion** | Automated Microsoft 365 mailbox polling (configurable interval), PDF attachment detection, duplicate filtering, processed-folder management. | **Live** |
| **Manual Upload Portal** | Direct PDF upload with real-time extraction for invoices received via mail, fax, or hand-delivery. | **Live** |
| **Approval Workflow** | Multi-status lifecycle (Pending → Under Review → Approved/Rejected → Synced), role-based access, rejection with comments, full audit trail. | **Live** |
| **GL Coding & Mapping Engine** | Configurable field mapping profiles per vendor, GL account/project/location/cost center assignment, date transforms, multi-line support. | **Live** |
| **Vendor Management** | Auto-linking invoices to vendors, vendor search, spend tracking and analytics per vendor. | **Live** |
| **Real-Time Dashboard** | Invoice volume/status cards, area/pie/bar charts, recent activity feed, "Needs Attention" alerts. | **Live** |
| **Excel Export** | Multi-sheet export (Invoices + Line Items + Summary), column selection, date/status filtering. | **Live** |
| **User Management & RBAC** | JWT authentication, refresh tokens, four roles (Super Admin, Admin, Approver, Viewer) with granular permissions. | **Live** |
| **Duplicate Detection** | Flags invoices with matching invoice numbers or similar header details to prevent double-posting. | **Live** |
| **Encryption at Rest** | AES-256-GCM encryption for all API credentials and sensitive configuration. | **Live** |
| **Multi-Tenant Isolation** | Organization-scoped data access, per-org storage paths, per-org email config, per-org ERP credentials. | **Live** |
| **Self-Service Onboarding** | Organization provisioning wizard with Microsoft config, ERP config, and trial subscription setup. | **Live** |

### 2.2 What Will Be Built for This Engagement

| Capability | Description | Delivery |
|-----------|-------------|----------|
| **Sage Intacct ERP Sync** | AP bill creation via Sage Intacct Web Services API, dimensional posting (location, department, project), sync status tracking with retry logic. | Weeks 2–3 |
| **PO Matching Engine** | Two-way and three-way matching with configurable tolerance thresholds for price/quantity variance. | Weeks 2–3 |
| **Long Home GL Rulebook** | Custom mapping profiles configured for Long Home's chart of accounts, job cost structure, and vendor-specific coding rules. | Week 2 |

---

## 3. Workstreams / Skills Included

### 3.1 Invoice Ingestion

Automated capture of vendor invoices for materials and labor through multiple channels:

- **Email Ingestion** — Automated polling of designated AP mailbox(es) via Microsoft Graph API; PDF attachments are detected, extracted, and queued for processing. Non-invoice attachments are filtered out. Configurable polling interval (default: 5 minutes).
- **Direct Upload** — Manual upload portal for invoices received via mail, fax, or hand-delivery.
- **Duplicate Detection** — Invoices with matching invoice numbers or similar header details are automatically flagged to prevent double-posting.

### 3.2 AI-Powered Data Extraction (GPT-4o)

The platform uses a proprietary **3-tier AI extraction cascade** — a significant advantage over traditional OCR-only or template-based solutions:

| Tier | Method | When Used | Accuracy |
|------|--------|-----------|----------|
| **Tier 2 (Primary)** | **GPT-4o Vision** — PDF pages rendered to images, analyzed by OpenAI's multimodal model for structured JSON extraction | All invoices (first pass) | 95%+ |
| **Tier 1 (Fallback)** | **GPT-4o Text** — Raw extracted text analyzed by LLM when Vision is unavailable or inconclusive | Vision fallback | 90%+ |
| **Tier 0 (Baseline)** | **Regex + Heuristics** — Pattern-based extraction for standard fields | Cross-validation reference | 70–85% |

**Key advantages of this approach:**

- **No templates required** — Handles any vendor invoice format on first encounter, unlike template-based systems that require manual configuration per vendor.
- **Cross-validation** — AI results are compared against regex extraction for confidence scoring. Discrepancies trigger manual review, ensuring nothing slips through unverified.
- **Continuous improvement** — Extraction accuracy improves as the system processes more of Long Home's vendor invoices.
- **Cost tracking** — Per-invoice token usage and cost are tracked and reported, providing full transparency on AI processing costs.
- **Fields extracted** — Vendor name, invoice number, invoice date, due date, PO number, line items (description, quantity, unit price, amount), subtotal, tax, total, currency, payment terms, and custom fields per mapping profile.

### 3.3 GL Coding & Project Coding

Automated application of predefined accounting and job cost rules:

- Configurable **field mapping profiles** tailored to Long Home's chart of accounts.
- **Vendor-pattern-based auto-profile selection** — The mapping engine automatically selects the correct GL coding profile based on vendor name patterns, eliminating manual profile assignment.
- Automatic assignment of **GL account codes**, **department codes**, and **job/project cost allocations** based on vendor, material type, and PO reference.
- Rules engine supports **multi-line invoices** with different GL coding per line item.
- **Default and fallback values** — Missing fields are populated with configurable defaults to minimize exceptions.

### 3.4 Purchase Order (PO) Matching

PO matching will be configured as part of this engagement to support automated matching of invoices to purchase orders:

- **Two-way match** — Invoice-to-PO header comparison (vendor, PO number, total amount).
- **Three-way match** — Invoice-to-PO-to-receipt for materials with receiving documentation.
- **Tolerance thresholds** — Configurable variance limits for price and quantity discrepancies.
- **PO number extraction** — The AI engine already extracts PO numbers from invoices; this workstream adds the matching logic against PO data from Sage Intacct.

### 3.5 ERP Entry (Sage Intacct Integration)

Evolotek will build and configure the **Sage Intacct sync service** as a core deliverable of this engagement:

- **Sage Intacct Web Services API integration** — Connect to Long Home's Sage Intacct instance using API credentials provided by the Client.
- Create **AP bills / purchase invoices** with complete line item detail.
- Apply correct **tax rates, ledger accounts, and dimensional coding** (location, department, project, class).
- **Sync status tracking** with automatic retry on transient failures (exponential backoff for 5xx errors, token refresh for 401s).
- Full **audit trail** of every sync attempt — request/response payloads, error codes, duration, and retry count stored for troubleshooting.
- **Manual retry** — Admin users can trigger re-sync for failed invoices from the dashboard.

> **Note:** The platform infrastructure for ERP sync (data model, status lifecycle, frontend controls, retry tracking) is already built. This engagement completes the **Sage Intacct API connector** — the service that transforms approved invoice data into Sage Intacct API calls and handles responses.

### 3.6 Exception Handling & Human-in-the-Loop

Intelligent routing of exceptions for AP team review:

- **Low-confidence extractions** (AI confidence below configurable threshold) flagged for manual verification before posting.
- **PO mismatches** (price, quantity, vendor) routed to exception queue with highlighted discrepancies.
- **Missing data** (no PO reference, unknown vendor, incomplete line items) flagged with clear indicators.
- **Approval workflow** with role-based access — Reviewers validate extracted data, Approvers authorize ERP posting.
- **Rejection with comments** — Declined invoices include reason codes for vendor follow-up.
- **Inline PDF viewer** — Side-by-side PDF preview alongside extracted fields for efficient review.

---

## 4. Solution Architecture

### 4.1 High-Level Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     LONG HOME AP TEAM                        │
│              (Review Dashboard & Approvals)                  │
└──────────────┬──────────────────────────┬────────────────────┘
               │                          │
               ▼                          ▼
┌──────────────────────┐    ┌──────────────────────────────────┐
│   Web Dashboard      │    │   Email Ingestion Service        │
│   (React / Tailwind) │    │   (MS Graph API Polling)         │
└──────────┬───────────┘    └──────────────┬───────────────────┘
           │                               │
           ▼                               ▼
┌──────────────────────────────────────────────────────────────┐
│              BACKEND API (Spring Boot / Java 21)             │
│   • Authentication & RBAC   • Invoice Lifecycle Management   │
│   • Approval Workflows      • Vendor Management             │
│   • Sage Intacct Sync  ★    • Dashboard Analytics            │
│   • Audit Logging           • Excel Export                   │
└──────────────┬───────────────────────────┬───────────────────┘
               │                           │
               ▼                           ▼
┌──────────────────────────┐  ┌────────────────────────────────┐
│  AI Extraction Service   │  │   Sage Intacct ERP  ★          │
│  (Python / FastAPI)      │  │   (Web Services API)           │
│  • GPT-4o Vision (Tier 2)│  │   • AP Bill Creation           │
│  • GPT-4o Text (Tier 1)  │  │   • Dimensional Posting        │
│  • Regex Fallback (Tier 0)│ │   • PO Data Retrieval          │
│  • Cross-Validation      │  │   • Sync Status Tracking       │
│  • Mapping Engine        │  └────────────────────────────────┘
└──────────────┬───────────┘
               │                  ★ = Built during this engagement
               ▼
┌──────────────────────────┐
│  PostgreSQL Database     │
│  • Invoice Records       │
│  • Mapping Rules         │
│  • Vendor Profiles       │
│  • Sync & Audit Trail    │
└──────────────────────────┘
```

### 4.2 Technology Stack

| Component | Technology |
|-----------|-----------|
| Frontend Dashboard | React 18, TypeScript, Tailwind CSS, Recharts |
| Backend API | Java 21, Spring Boot 3, Spring Security, Flyway |
| AI / Extraction Engine | Python 3.12, FastAPI, GPT-4o (Vision + Text), PyMuPDF, Tesseract OCR |
| Database | PostgreSQL 16 |
| File Storage | AWS S3 (AES-256 encrypted at rest) |
| Message Queue | AWS SQS (with dead-letter queue for failed messages) |
| ERP Integration | Sage Intacct Web Services API (to be configured) |
| Credential Encryption | AES-256-GCM (custom encryption service) |
| Infrastructure | AWS (ECS / EC2, RDS, S3, SQS, CloudWatch) |

### 4.3 Security & Compliance

- **Role-Based Access Control (RBAC)** — Super Admin, Admin, Approver, Viewer roles with granular permissions.
- **JWT-based authentication** with refresh token rotation and configurable session limits.
- **Data encryption** in transit (TLS 1.2+) and at rest (AES-256-GCM for credentials, AES-256 for storage).
- **Comprehensive audit logging** — Every action (view, edit, approve, reject, sync) is recorded with timestamp, user, IP address, and organization context.
- **Data isolation** — Client data is logically separated with organization-scoped access controls; per-org S3 paths, per-org email credentials, per-org ERP configuration.
- **Pre-signed URLs** — PDF files are never publicly accessible; time-limited signed URLs are generated for each view/download.

---

## 5. Project Timeline

All dates are relative to the **Kickoff Date** (to be mutually agreed). If kickoff occurs the week of March 3, 2026, the projected dates are as shown.

| Week | Projected Dates | Milestone | Key Activities |
|------|----------------|-----------|----------------|
| **Week 1** | Kickoff Week | **Kickoff & Discovery** | Kickoff meeting with stakeholders; Sage Intacct test environment access provisioning; recurring meeting schedule established; collect 15 sample COGS invoices and AP process documentation; confirm AP process owner |
| **Week 2** | Kickoff + 1 week | **Configuration & Rulebook** | Data sampling and analysis of COGS invoice formats; AP rulebook drafting (GL coding rules, PO matching logic, exception criteria); Sage Intacct API connector development; mapping profile configuration for Long Home's chart of accounts; PO matching engine build |
| **Week 3** | Kickoff + 2 weeks | **Boardroom Pilot** | End-to-end pilot with real COGS invoices in Sage Intacct test environment; live demonstration of extraction → coding → matching → posting workflow; Sage Intacct sync validation; refinement based on client feedback; formal pilot approval |
| **Week 4** | Kickoff + 3 weeks | **Go-Live & Monitoring** | Production deployment for COGS-related AP automation; Sage Intacct production sync activation; hypercare support; optimization and performance tuning; weekly performance reporting initiation |

### 5.1 Post Go-Live Support

- **Weeks 5–8**: Performance monitoring, accuracy tuning, and ongoing optimization.
- **Weekly performance reports** delivered to AP process owner covering volume, accuracy, exception rates, and cycle time metrics.
- **Issue resolution SLA**: Critical issues addressed within 4 business hours; standard issues within 1 business day.
- **AI model tuning**: Extraction accuracy improvements based on production invoice data patterns.

---

## 6. Responsibilities

### 6.1 Client Checklist — Long Home Products

| # | Responsibility | Due By |
|---|---------------|--------|
| 1 | Provide **15 sample COGS-related AP invoices** (covering top vendors, varied formats) | Week 1 |
| 2 | Grant **Sage Intacct test environment access** (Web Services API credentials, company ID, sender ID) | Week 1 |
| 3 | Confirm **AP process owner** and designated point of contact | Week 1 |
| 4 | Provide **chart of accounts extract** and **job cost coding rules** from Sage Intacct | Week 1 |
| 5 | Complete **business logic signoff** — PO formats, GL coding rules, job cost allocation rules, exception handling preferences | Week 2 |
| 6 | Participate in **Boardroom Pilot** with key stakeholders | Week 3 |
| 7 | Approve **go-live readiness** and authorize production deployment | Week 3 |
| 8 | Provide **Sage Intacct production API credentials** for go-live | Week 4 |

### 6.2 Evolotek Solutions Checklist

| # | Responsibility | Due By |
|---|---------------|--------|
| 1 | **Configure platform** for Long Home — organization setup, email ingestion, mapping profiles, vendor patterns | Week 1 |
| 2 | **Build Sage Intacct API connector** — AP bill creation, dimensional posting, sync status tracking, retry logic | Weeks 2–3 |
| 3 | **Build PO matching engine** — Two-way/three-way matching with configurable tolerance thresholds | Weeks 2–3 |
| 4 | Draft **AP automation blueprint** documenting extraction rules, GL coding logic, and exception handling flows | Week 2 |
| 5 | **Train AI extraction** on Client's invoice formats; run internal validation against sample invoices | Week 2 |
| 6 | Facilitate **Boardroom Pilot** — prepare test cases, run live demonstration, document results | Week 3 |
| 7 | Complete **go-live configuration** — production deployment, Sage Intacct sync activation, monitoring setup | Week 4 |
| 8 | Provide **weekly performance updates** post go-live (accuracy, volume, exceptions, cycle time) | Ongoing |
| 9 | Deliver **hypercare support** during first two weeks post go-live | Weeks 4–5 |

---

## 7. Definition of Success

The following metrics define successful delivery of this engagement. All metrics will be measured during the Boardroom Pilot and validated during the first 30 days of production operation.

| # | Success Metric | Target | Measurement Method |
|---|---------------|--------|-------------------|
| 1 | **First-pass posting accuracy** — Invoices posted to Sage Intacct without human correction | **85%+** | (Auto-posted invoices ÷ Total invoices) × 100 |
| 2 | **Manual entry reduction** — Decrease in AP staff touchpoints for COGS invoices | **90%+** | Comparison of pre- vs. post-automation keystroke/field entry counts |
| 3 | **Cycle time reduction** — Time from invoice receipt to ERP posting | **50%+** | Average processing time pre- vs. post-automation |
| 4 | **Stakeholder sign-off** — Boardroom Pilot approved by AP process owner | **Approved** | Formal written approval |
| 5 | **Exception rate** — Percentage of invoices requiring manual intervention | **< 15%** | (Exception invoices ÷ Total invoices) × 100 |
| 6 | **Sage Intacct sync success rate** — Approved invoices successfully posted to ERP | **95%+** | (Synced invoices ÷ Approved invoices) × 100 |

### 7.1 Acceptance Criteria

- All six success metrics met or exceeded during Boardroom Pilot with representative invoice sample.
- Client AP process owner provides written go-live approval.
- Solution successfully posts invoices to Sage Intacct production environment with correct GL coding, dimensional allocation, and job cost distribution.
- Sync failures are handled gracefully with retry logic and admin visibility.

---

## 8. Systems Involved

| System | Role | Access Required |
|--------|------|----------------|
| **Sage Intacct** | Primary ERP — AP bill creation, GL posting, dimensional coding, PO data | Web Services API credentials (company ID, sender ID, user/password or API keys) for test and production |
| **Microsoft 365 / Outlook** | Invoice receipt channel — automated email polling | Designated AP mailbox access + Microsoft Graph API app registration (client ID, client secret, tenant ID) |
| **Vendor Invoices** | Source documents | PDF, email attachments, EDI (where applicable) |
| **Purchase Orders** | PO matching reference | PO data via Sage Intacct API or manual export |
| **Job Cost Coding Rules** | GL and project allocation logic | Chart of accounts export + business rules document from Client AP team |
| **Evolotek Cloud Platform** | Hosting, AI processing, and dashboard | AWS infrastructure managed by Evolotek |

### 8.1 Integration Points

```
                                 ┌─────────────────────────┐
Vendor Invoices                  │   Evolotek Platform     │
  │                              │                         │
  ├── Email ──→ MS Graph API ──→ │  Email Ingestion        │
  │                              │       │                 │
  └── Upload ──→ Web Portal ──→  │  AI Extraction (GPT-4o) │
                                 │       │                 │
                                 │  GL Coding & PO Match   │
                                 │       │                 │
                                 │  Approval Workflow      │
                                 │       │                 │
                                 └───────┼─────────────────┘
                                         │
                                         ▼
                                 ┌─────────────────────────┐
                                 │    Sage Intacct          │
                                 │    (Web Services API)    │
                                 │                         │
                                 │  • AP Bills              │
                                 │  • GL Journal Entries    │
                                 │  • Job Cost Allocations  │
                                 │  • Dimensional Coding    │
                                 └─────────────────────────┘
```

---

## 9. Assumptions & Constraints

### 9.1 Assumptions

1. Client will provide timely access to Sage Intacct test and production environments as outlined in the timeline.
2. Sage Intacct Web Services API is enabled on the Client's subscription and supports AP bill creation endpoints.
3. Sample invoices provided are representative of the full range of COGS vendor invoice formats.
4. Client's existing GL chart of accounts and job cost structure will remain stable during the engagement.
5. Client will designate a single AP process owner with decision-making authority for business logic sign-off.
6. Invoice volume during pilot will be representative of typical monthly volume.
7. Client will provide Microsoft 365 mailbox credentials (or app registration) for email ingestion if automated email intake is desired at go-live.

### 9.2 Constraints

1. Scope is limited to **COGS-related AP invoices** (materials and labor). Non-COGS AP, AR, and other financial workflows are excluded from this phase.
2. Custom Sage Intacct module development or modifications to the Client's ERP configuration are **out of scope**.
3. Historical invoice migration or backlog processing is **not included** in this engagement.
4. The solution supports **English-language invoices** only in this phase.
5. PO matching depends on PO data availability — either via Sage Intacct API export or Client-provided PO data.

### 9.3 Change Management

Any changes to scope, timeline, or deliverables will be documented via a **Change Request (CR)** process. CRs will be jointly evaluated for impact on timeline and cost before approval.

---

## 10. Phase 2 — Future Scope & Expansion

Following successful go-live and stabilization of COGS AP automation, the platform can be expanded to deliver additional value. The following roadmap items are **not included in this SOW** but represent natural next steps:

### 10.1 Near-Term Expansion (Months 2–3)

| Initiative | Description | Business Value |
|-----------|-------------|----------------|
| **Non-COGS AP Automation** | Extend automation to overhead, SG&A, and operational invoices (utilities, services, SaaS subscriptions) | Full AP automation across all invoice types |
| **Multi-Level Approval Chains** | Configurable approval hierarchies based on invoice amount thresholds, department, or vendor | Stronger internal controls and compliance |
| **Advanced Reporting & Analytics** | Spend analytics by vendor/category/project, aging reports, processing time trends, accuracy dashboards | Data-driven AP insights for leadership |
| **Automated Vendor Onboarding** | New vendor detection, auto-creation in Sage Intacct, W-9 / insurance tracking | Reduced vendor setup time |

### 10.2 Medium-Term Expansion (Months 4–6)

| Initiative | Description | Business Value |
|-----------|-------------|----------------|
| **Accounts Receivable Automation** | Customer invoice generation, payment tracking, cash application | Full order-to-cash visibility |
| **Multi-ERP Support** | Extend connectors beyond Sage Intacct (QuickBooks, NetSuite, Xero) | Future-proof for ERP migration or multi-entity |
| **Email/SMS Notifications** | Configurable alerts for pending approvals, sync failures, thresholds exceeded | Faster response times, fewer bottlenecks |
| **Mobile Approval App** | Approve/reject invoices from mobile devices | AP processing without desktop dependency |
| **EDI Integration** | Electronic Data Interchange for high-volume vendors | Eliminate PDF dependency for top vendors |

### 10.3 Long-Term Vision

| Initiative | Description | Business Value |
|-----------|-------------|----------------|
| **Predictive Cash Flow** | ML-based cash flow forecasting from AP/AR data | Improved treasury management |
| **Contract Compliance** | Auto-verify invoice pricing against vendor contracts | Prevent overcharges |
| **Audit-Ready Compliance Packages** | Pre-packaged audit trail exports for SOX, financial audits | Reduced audit preparation time |

> Phase 2 scope and pricing will be proposed separately based on Phase 1 outcomes and Client priorities.

---

## 11. Commercials & Pricing

*[To be discussed and finalized based on engagement model — fixed fee, monthly subscription, or per-invoice pricing. Evolotek recommends a phased pricing approach with a fixed implementation fee for Weeks 1–4 and a monthly subscription for ongoing processing and support.]*

### 11.1 Suggested Pricing Models

| Model | Structure | Best For |
|-------|----------|----------|
| **Fixed Implementation + Monthly SaaS** | One-time setup fee + per-month platform fee | Predictable budgeting |
| **Per-Invoice Processing** | Fee per successfully processed invoice | Volume-based cost alignment |
| **Hybrid** | Fixed setup + per-invoice over baseline volume | Balanced risk sharing |

### 11.2 What's Included

- Platform configuration, Sage Intacct connector build, and PO matching engine
- AI extraction tuning for Long Home's vendor invoice formats
- Boardroom Pilot facilitation
- Go-live support and hypercare (2 weeks)
- Ongoing platform hosting, maintenance, and AI processing
- Weekly performance reporting
- Standard support (business hours)

---

## 12. Confidentiality & Legal

- All Client data (invoices, vendor information, financial records) will be treated as **strictly confidential**.
- Data is encrypted in transit (TLS 1.2+) and at rest (AES-256-GCM) using industry-standard encryption protocols.
- Evolotek will execute a **Non-Disclosure Agreement (NDA)** and **Data Processing Agreement (DPA)** prior to receiving any Client data.
- Client retains full ownership of all data processed through the platform.
- AI processing uses OpenAI's enterprise API — **no Client data is used for model training** per OpenAI's data usage policies.
- Evolotek will comply with applicable data protection regulations.

---

## 13. Appendix

### Appendix A — Glossary

| Term | Definition |
|------|-----------|
| **COGS** | Cost of Goods Sold — direct costs attributable to production of goods/services sold |
| **AP** | Accounts Payable — money owed by a company to its suppliers |
| **GL** | General Ledger — master accounting record |
| **PO** | Purchase Order — document authorizing a purchase |
| **IDP** | Intelligent Document Processing — AI-powered document data extraction |
| **OCR** | Optical Character Recognition — technology to extract text from images/scans |
| **GPT-4o** | OpenAI's multimodal large language model used for invoice data extraction |
| **ERP** | Enterprise Resource Planning — business management software (e.g., Sage Intacct) |
| **Boardroom Pilot** | Controlled live demonstration of the solution with real data for stakeholder validation |
| **RBAC** | Role-Based Access Control — permission system based on user roles |

### Appendix B — Sample Invoice Processing Flow

```
1. Invoice Received (Email / Upload)
         │
2. AI Extraction — 3-Tier Cascade
   ├── Tier 2: GPT-4o Vision (PDF → Image → Structured JSON)
   ├── Tier 1: GPT-4o Text (Raw Text → JSON, fallback)
   └── Tier 0: Regex (Cross-validation baseline)
         │
3. Confidence Scoring & Cross-Validation
   ├── High Confidence (>90%) → Auto-proceed
   └── Low Confidence (<90%) → Flag for Review
         │
4. GL Coding & Job Cost Allocation (Rules Engine)
   └── Vendor-pattern-based auto-profile selection
         │
5. PO Matching (2-way / 3-way)
         │
    ┌────┴────┐
    │         │
 Match ✓   Mismatch ✗
    │         │
    ▼         ▼
6a. Auto-    6b. Exception
    Route to      Queue
    Approval      │
    │         AP Review
    │         & Correction
    │              │
    └──────┬───────┘
           │
7. Approval (Admin / Approver role)
   ├── Approve → Queue for ERP Sync
   └── Reject → Comments + Vendor Follow-up
           │
8. Post to Sage Intacct (Web Services API)
   ├── Success → Status: SYNCED
   └── Failure → Retry (exponential backoff) or Alert
           │
9. Confirmation & Audit Log
```

### Appendix C — AI Extraction Accuracy by Method

| Method | Accuracy Range | Cost per Invoice | Best For |
|--------|---------------|-----------------|----------|
| GPT-4o Vision | 95%+ | ~$0.02–0.05 | All formats, complex layouts, handwritten |
| GPT-4o Text | 90%+ | ~$0.01–0.03 | Digital PDFs with clean text |
| Regex / Heuristics | 70–85% | $0 | Cross-validation, simple structured invoices |
| Cross-Validated (Combined) | **95%+** | ~$0.03–0.06 | **Production default** |

### Appendix D — Platform Screenshots

*[Dashboard, Invoice Detail, Mapping Configuration, and Approval Workflow screenshots to be included in presentation materials.]*

### Appendix E — Contact Information

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
