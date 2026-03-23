# SyncLedger — Complete Project Flow Documentation

> **Written for everyone** — no technical background needed.  
> This document explains how SyncLedger works from start to finish: how invoices come in, how they get processed, how rules decide where numbers go, and how the final data reaches your accounting system.

---

## Table of Contents

1. [What is SyncLedger?](#1-what-is-syncledger)
2. [The Big Picture — How It All Fits Together](#2-the-big-picture)
3. [Getting Started — First-Time Setup](#3-getting-started)
4. [How Invoices Enter the System](#4-how-invoices-enter-the-system)
5. [How SyncLedger Reads Your Invoices (AI Extraction)](#5-how-syncledger-reads-your-invoices)
6. [How Confidence Scores Work](#6-how-confidence-scores-work)
7. [Mapping Rules — Telling SyncLedger Where Numbers Go](#7-mapping-rules)
8. [Different ERPs, Different Rules — Per-Organization Customization](#8-different-erps-different-rules)
9. [Review and Approval Workflow](#9-review-and-approval-workflow)
10. [Pushing Data to Your Accounting System (ERP Sync)](#10-pushing-data-to-your-accounting-system)
11. [Confirming the Data Reached Your ERP](#11-confirming-the-data-reached-your-erp)
12. [Vendors — Automatic Recognition](#12-vendors)
13. [Notifications — Staying in the Loop](#13-notifications)
14. [User Roles — Who Can Do What](#14-user-roles)
15. [Multi-Company Support](#15-multi-company-support)
16. [The Complete Journey — Start to Finish](#16-the-complete-journey)
17. [Dashboard and Reporting](#17-dashboard-and-reporting)
18. [System Settings — Runtime Controls](#18-system-settings)
19. [Subscription Plans](#19-subscription-plans)

---

## 1. What is SyncLedger?

SyncLedger is an **invoice processing platform** that uses artificial intelligence to:

- **Receive** invoices (via email or manual upload)
- **Read** them automatically (extract vendor name, amounts, dates, line items)
- **Classify** them (assign the right accounting codes based on your rules)
- **Route** them for approval (the right people review and approve)
- **Push** the approved data to your accounting/ERP system (like Sage, NetSuite, QuickBooks)

**Think of it like a smart assistant** who opens every invoice, reads all the numbers, files it under the right accounting category, gets it approved, and enters it into your books — all without you doing data entry.

---

## 2. The Big Picture

Here is the simplified flow of how every invoice travels through SyncLedger:

```
  📧 Email with PDF          📤 Manual Upload
  attachment arrives          from your computer
        │                           │
        └──────────┬────────────────┘
                   ▼
        ┌─────────────────────┐
        │  SyncLedger receives │
        │  the PDF file        │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  AI reads the PDF    │
        │  and extracts all    │
        │  the data            │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  Your mapping rules  │
        │  assign the right    │
        │  GL account, project,│
        │  and cost center     │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  Confidence check:   │
        │  Is the AI sure?     │
        │  ┌─YES──────┐       │
        │  │ Goes to   │       │
        │  │ Pending   │       │
        │  └──────────┘       │
        │  ┌─NOT SURE──┐      │
        │  │ Flagged for│      │
        │  │ Manual     │      │
        │  │ Review     │      │
        │  └───────────┘      │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  Your team reviews   │
        │  and clicks          │
        │  ✅ Approve  or      │
        │  ❌ Reject           │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  Approved invoice    │
        │  gets pushed to your │
        │  accounting system   │
        │  (Sage, NetSuite,    │
        │   QuickBooks, etc.)  │
        └─────────┬───────────┘
                  ▼
        ┌─────────────────────┐
        │  ✅ Confirmation:    │
        │  Data is in your ERP │
        │  with a reference ID │
        └─────────────────────┘
```

---

## 3. Getting Started

### First-Time Setup (Onboarding)

When a new company joins SyncLedger, they go through a simple setup wizard:

1. **Create your organization** — Enter your company name, address, and contact details
2. **Connect your email** (optional) — Provide your Microsoft email credentials so SyncLedger can automatically pick up invoices from your inbox
3. **Connect your ERP** (optional) — Enter your Sage/NetSuite/QuickBooks API details so approved invoices can be automatically pushed
4. **Set up mapping rules** — Define which GL accounts, projects, and cost centers to use for different vendors or invoice types
5. **Invite your team** — Add users with the right roles (Admin, Approver, Viewer)

Once setup is complete, you land on your **Dashboard** and SyncLedger starts working.

---

## 4. How Invoices Enter the System

There are **two ways** invoices get into SyncLedger:

### Way 1: Automatic Email Pickup

> **How it works:** SyncLedger checks your email inbox every 5 minutes for new invoices.

- Your organization connects a **Microsoft mailbox** (e.g., `invoices@yourcompany.com`)
- SyncLedger reads unread emails, looks for **PDF or image attachments**
- For each attachment it finds, it:
  1. Downloads the file
  2. Stores it securely in the cloud
  3. Creates an invoice record in the system
  4. Sends it to the AI for reading
  5. Marks the email as read and moves it to a "Processed" folder
- **Each attachment becomes a separate invoice** — if an email has 3 PDFs, you get 3 invoices

**What gets captured from the email:**
- Who sent it (sender email)
- The email subject line
- When it was received
- Which attachments were found

**Duplicate prevention:** If SyncLedger already processed an email (based on its unique ID), it won't process it again.

### Way 2: Manual Upload

> **How it works:** You drag and drop (or browse for) a PDF file in the SyncLedger web app.

- Go to the **Invoices** page
- Click the **Upload PDF** button
- Select a PDF file from your computer
- SyncLedger immediately stores the file and sends it to the AI for reading
- The invoice appears in your list within seconds

**Only PDF files are accepted** — the system validates the file type before uploading.

---

## 5. How SyncLedger Reads Your Invoices

This is where the magic happens. SyncLedger uses a **3-level AI system** to read invoices, from most accurate to most basic:

### Level 1: Vision AI (Best Quality)

- The PDF is converted into images
- **GPT-4o Vision** (an advanced AI model by OpenAI) literally "looks" at the invoice — just like a human would
- It can understand any layout, handwriting, logos, tables, and even messy scans
- It returns all the data in a structured format
- **Most accurate** but costs a small amount per invoice

### Level 2: Text AI (Good Quality)

- If Vision fails for any reason, SyncLedger falls back to this
- The system extracts the raw text from the PDF
- **GPT-4o** (text model) reads the text and identifies the invoice fields
- Works great for clean, digital PDFs
- **Cheaper** than Vision but slightly less accurate for complex layouts

### Level 3: Pattern Matching (Basic / Free)

- If both AI levels fail, SyncLedger uses **smart pattern matching** (regex)
- It looks for patterns like "Invoice #", "Total:", "Due Date:", etc.
- Works for standard invoice formats
- **No AI cost** — completely free
- About 70% accuracy — good enough to get started, but may need manual review

### Cross-Validation (Quality Check)

After the AI extracts the data, SyncLedger **double-checks its work**:

1. It runs the basic pattern matcher independently
2. It compares the AI's answers with the pattern matcher's answers
3. For each field, it checks: **Did both methods find the same answer?**
   - Invoice number: Do they match?
   - Total amount: Within $0.02 of each other?
   - Vendor name: Same company?
   - Dates: Same dates (regardless of format)?
4. Based on how many fields agree, it calculates a **confidence score**

### What Gets Extracted

From a single invoice PDF, SyncLedger pulls out:

| Category | Fields |
|----------|--------|
| **Invoice ID** | Invoice number, PO number |
| **Vendor** | Company name, address, email, phone, tax ID |
| **Money** | Subtotal, tax amount, discount, shipping, total amount, currency |
| **Dates** | Invoice date, due date |
| **Line Items** | Each product/service line: description, quantity, unit price, line total |
| **Accounting** | GL account, project, cost center (from your rules) |

---

## 6. How Confidence Scores Work

Every extracted invoice gets a **confidence score** from 0% to 100%. This tells you **how sure the AI is** about its reading.

### What Determines the Score

| Factor | Impact |
|--------|--------|
| AI's own certainty | Starting baseline (~70-80%) |
| Invoice number matches between AI and pattern matcher | +20% if match |
| Total amount matches | +25% if match |
| Vendor name matches | +15% if match |
| Date matches | +10% if match |
| Other fields match | +5-8% each |
| Mismatches found | -10% per critical mismatch |

### What Happens Based on the Score

| Score | Label | What Happens |
|-------|-------|--------------|
| **90%+** | High Confidence | Invoice goes directly to **Pending** — ready for approval. Green indicator. |
| **70-89%** | Medium Confidence | Invoice goes to **Pending** but may be flagged. Yellow indicator. |
| **Below 70%** | Low Confidence | Invoice goes to **Under Review** — someone needs to manually check the data. Red indicator with warning banner. |

The **threshold** that decides "needs review" vs. "good to go" is configurable (default: 87%). Your admin can change this in Settings.

---

## 7. Mapping Rules

### What Are Mapping Rules?

Mapping rules tell SyncLedger: *"When you see an invoice from Vendor X, put the costs in GL Account Y and charge it to Project Z."*

Without rules, SyncLedger extracts the invoice data but doesn't know **which accounting bucket** it belongs to. Rules solve this.

### How Rules Are Organized: Mapping Profiles

A **Mapping Profile** is a collection of rules. Think of it as a "recipe card" for a type of invoice.

**Example profiles:**
- **"MGD Construction Invoices"** — For all invoices from MGD Construction, use GL 5100, project = Opportunity Number
- **"Standard Vendor Invoices"** — For generic invoices, use GL 5000, due date = invoice date + 30 days

### How SyncLedger Picks the Right Profile

When an invoice comes in, SyncLedger automatically selects the best profile using this priority:

1. **Vendor name match** — If the vendor name matches a profile's pattern (e.g., profile says "MGD" and invoice is from "MGD Construction Inc."), use that profile
2. **Condition match** — If the invoice's data matches specific conditions on a profile (e.g., "total > $10,000"), use that profile
3. **Organization default** — Use your company's default profile
4. **System default** — Use the built-in fallback profile

### How Individual Rules Work

Each rule in a profile says:

> **"For this target field, get the value from this source, and if that's empty, try this backup, and if both are empty, use this default."**

**Example Rule:**
| Setting | Value |
|---------|-------|
| **Target** | Due Date |
| **Primary Source** | The "Due Date" field on the invoice |
| **Backup Source** | The "Invoice Date" field |
| **Transformation** | Add 30 days to the backup date |
| **Default** | Today + 30 days |

So if the AI finds "Due Date: Feb 14, 2025" on the invoice → that's used directly.  
If no due date is found but invoice date is "Jan 15, 2025" → the system calculates Jan 15 + 30 = Feb 14.

### Date Transformations

Rules can transform dates:

| Transformation | What It Does | Example |
|---------------|--------------|---------|
| **Next Friday** | Finds the next Friday after the date | Jan 15 (Wed) → Jan 17 (Fri) |
| **Add 30 Days** | Adds 30 days (Net 30 terms) | Jan 15 → Feb 14 |
| **Add 60 Days** | Adds 60 days (Net 60 terms) | Jan 15 → Mar 16 |
| **Add 90 Days** | Adds 90 days (Net 90 terms) | Jan 15 → Apr 15 |
| **End of Month** | Moves to last day of that month | Jan 15 → Jan 31 |

### Conditions on Rules

Rules can have conditions — they only apply **when the condition is true**:

| Condition Type | Example |
|---------------|---------|
| **Equals** | "Vendor name equals 'ABC Corp'" |
| **Contains** | "Invoice contains 'RUSH ORDER'" |
| **Starts With** | "PO number starts with 'PO-'" |
| **Regex Match** | Advanced pattern matching |
| **Exists** | "Only apply if tax amount exists" |

### What Gets Mapped

| Target Field | What It Means |
|-------------|---------------|
| **GL Account** | The general ledger account code (e.g., 5100 = Cost of Goods) |
| **Project** | Which project/job to charge (e.g., "OPP-12345") |
| **Cost Center** | Which department/division (e.g., "CC-001") |
| **Item Category** | Type of expense (e.g., "Materials", "Labor") |
| **Location** | Physical location this cost relates to |

### Field Mapping Audit Trail

Every time a mapping rule is applied, the system records **exactly what happened**:
- Which profile was selected and why
- Which rule set each field's value
- What the source value was, what transformation was applied, and what the final value is

This is stored as a JSON record on the invoice, so you can always look back and understand **why** a GL account was assigned.

---

## 8. Different ERPs, Different Rules — Per-Organization Customization

### The Core Problem

Different companies use different accounting systems, and each system expects data in a different format:

- A **Sage** user needs a `contact_id` and a `ledger_account_id`
- A **NetSuite** user needs a `vendor_id` and a `subsidiary`
- A **QuickBooks** user needs a `CustomerRef` and an `AccountRef`
- A **Custom ERP** user might need completely unique fields

SyncLedger solves this by letting **each organization configure its own ERP type, connection, and field mapping rules** — completely independent from other organizations on the platform.

### How Each Organization Configures Its ERP

During setup (or anytime in Organization Settings), an admin configures:

| Setting | What It Does | Example |
|---------|-------------|----------|
| **ERP Type** | Which accounting system you use | Sage, NetSuite, QuickBooks, Oracle, SAP, Xero, or Custom |
| **API Endpoint** | The web address of your ERP's API | `https://api.intacct.com/ia/xml/xmlgw.phtm` |
| **API Key / Credentials** | Your login credentials for the ERP (stored encrypted) | `api_key_abc123...` |
| **Company ID** | Which company/entity within the ERP | `LONGHOME-PROD` |
| **Tenant ID** | User or account identifier | `sage_user@company.com` |
| **Auto-Sync** | Should approved invoices push automatically? | On / Off |
| **Extended Config** | Any extra settings your ERP needs (stored as flexible JSON) | Certificates, subsidiary codes, etc. |

### Each Organization Gets Its Own Mapping Profiles

Mapping profiles are **scoped per organization** — your company's rules are completely separate from any other company's rules.

**What this means in practice:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    SyncLedger Platform                          │
│                                                                 │
│  ┌─────────────────────┐    ┌─────────────────────────────┐    │
│  │  Company A           │    │  Company B                   │    │
│  │  ERP: Sage            │    │  ERP: QuickBooks             │    │
│  │                       │    │                               │    │
│  │  Profiles:            │    │  Profiles:                   │    │
│  │  ├─ "Subcontractors"  │    │  ├─ "Materials Vendors"      │    │
│  │  │  GL → 5100         │    │  │  GL → 6100                │    │
│  │  │  Project → PO #    │    │  │  Class → Department       │    │
│  │  │                    │    │  │                            │    │
│  │  ├─ "Office Supplies" │    │  ├─ "Service Providers"      │    │
│  │  │  GL → 6200         │    │  │  GL → 7200                │    │
│  │  │  Cost Center → HQ  │    │  │  Location → Branch Name   │    │
│  │  │                    │    │  │                            │    │
│  │  └─ Default Profile   │    │  └─ Default Profile          │    │
│  │     GL → 5000         │    │     GL → 5000                │    │
│  └─────────────────────┘    └─────────────────────────────┘    │
│                                                                 │
│  ┌─────────────────────┐                                       │
│  │  Company C           │    (+ any number of companies)       │
│  │  ERP: Custom API      │                                      │
│  │                       │                                      │
│  │  Profiles:            │                                      │
│  │  └─ Custom rules for  │                                      │
│  │     their proprietary │                                      │
│  │     ERP system        │                                      │
│  └─────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
```

### ERP-Specific Profiles

Each mapping profile also has an **ERP type** tag, so a single organization could even have profiles for different ERP systems (for example, during a migration from Sage to NetSuite).

### Real-World Example: Three Companies, Three ERPs, Three Approaches

Here's how the **same invoice** from "ABC Construction" would be handled differently by three companies:

**Company A uses Sage:**

| What SyncLedger Does | Result |
|---------------------|--------|
| Matches vendor "ABC Construction" to profile "Subcontractors" | Profile selected |
| Rule 1: GL Account → Fixed value `5100` | GL = 5100 |
| Rule 2: Project → Read from "Opportunity Number" field on invoice | Project = OPP-12345 |
| Rule 3: Due Date → Next Friday after order date | Due = Jan 17, 2025 |
| Pushes to Sage as: `purchase_invoice` with `ledger_account_id: 5100` | Sage format |

**Company B uses QuickBooks:**

| What SyncLedger Does | Result |
|---------------------|--------|
| No vendor match → uses default profile "Standard" | Default profile selected |
| Rule 1: GL Account → Read from invoice's "Account" field | GL = 4200 |
| Rule 2: Class → Read from "Department" field | Class = "Operations" |
| Rule 3: Due Date → Invoice date + 30 days (Net 30) | Due = Feb 14, 2025 |
| Pushes to QuickBooks as: `Bill` with `AccountRef: 4200` | QuickBooks format |

**Company C uses Custom ERP:**

| What SyncLedger Does | Result |
|---------------------|--------|
| Matches vendor to profile "Construction Jobs" | Profile selected |
| Rule 1: GL Account → Look up from cost center table | GL = CC-CONST-001 |
| Rule 2: Project → Always use fixed project code | Project = MAIN-2025 |
| Rule 3: Due Date → End of month | Due = Jan 31, 2025 |
| Pushes to Custom API with their unique JSON format | Custom format |

### Managing Mapping Profiles (Admin UI)

Admins manage their organization's profiles through the **Mapping Configuration** page:

1. **View all profiles** — See a list of all your mapping profiles
2. **Create a new profile** — Define:
   - Profile name and description
   - Which ERP type it's for
   - Vendor pattern (regex) to auto-match incoming invoices
   - Individual field mapping rules (source → target, with conditions and defaults)
   - Whether this is the default profile for your org
3. **Edit a profile** — Update any rules or conditions
4. **Delete a profile** — Remove profiles you no longer need (built-in profiles can't be deleted)
5. **Set as default** — Choose which profile applies when no vendor match is found

### Built-in vs. Custom Profiles

| Type | Who Creates It | Who Can Edit | Scope |
|------|---------------|-------------|-------|
| **Built-in** | SyncLedger platform | Nobody (read-only) | Available to all organizations as a starting point |
| **Custom** | Your admin | Your admin | Only visible to your organization |

Admins typically start with a built-in profile and then create custom profiles tailored to their specific vendors, GL structure, and ERP requirements.

### How ERP Type Affects the Data That Gets Pushed

When an invoice is approved and synced, SyncLedger **formats the data differently** based on your ERP type:

| ERP | Data Format | Key Differences |
|-----|------------|----------------|
| **Sage Intacct** | XML/JSON via Intacct API | Uses `contact_id`, `ledger_account_id`, purchase invoice format |
| **NetSuite** | REST/SuiteTalk | Uses `vendor_id`, `subsidiary`, vendor bill format |
| **QuickBooks** | REST API | Uses `AccountRef`, `VendorRef`, bill format |
| **Oracle** | REST/SOAP | Uses `supplier_id`, `distribution_lines` |
| **SAP** | RFC/REST | Uses `vendor_no`, `posting_key`, `cost_center` |
| **Xero** | REST API | Uses `ContactID`, `AccountCode`, invoice format |
| **Custom** | Your API format | Fully configurable via `erpConfigJson` |

### Extended ERP Configuration

For ERPs that need extra settings beyond the basics, organizations can store **custom configuration** in a flexible JSON block. Examples:

**Sage Intacct:**
```
{
  "userId": "api_user",
  "objectId": "optional_reference",
  "certificateSecretId": "TLS_cert_id"
}
```

**NetSuite:**
```
{
  "account": "12345_SB1",
  "subsidiary": "2",
  "defaultLocation": "Main Warehouse"
}
```

**SAP:**
```
{
  "client": "300",
  "language": "EN",
  "companyCode": "1000"
}
```

This means **no ERP is too unique** — the system adapts to whatever your accounting system needs.

---

## 9. Review and Approval Workflow

### Invoice Statuses (The Lifecycle)

Every invoice goes through a series of statuses:

```
┌──────────┐     ┌──────────────┐     ┌──────────┐     ┌────────┐
│ PENDING  │────►│ UNDER REVIEW │────►│ APPROVED │────►│ SYNCED │
└──────────┘     └──────────────┘     └──────────┘     └────────┘
                        │                   │
                        ▼                   ▼
                 ┌──────────┐        ┌─────────────┐
                 │ REJECTED │        │ SYNC FAILED │
                 └──────────┘        └─────────────┘
```

| Status | Meaning | What Can Happen Next |
|--------|---------|---------------------|
| **Pending** | AI read the invoice successfully and it's waiting for someone to review | Approve, Reject, Edit, or Reprocess |
| **Under Review** | The AI wasn't sure about some fields — a human needs to check | Edit fields, Approve, Reject, or Reprocess |
| **Approved** | A human reviewed and confirmed the data is correct | Push to accounting system (ERP) |
| **Rejected** | A human reviewed and determined the invoice is wrong or a duplicate | End of the line (can be archived) |
| **Synced** | The invoice data was successfully sent to your accounting system | Done! |
| **Sync Failed** | Tried to send to accounting system but something went wrong | Can retry |
| **Archived** | Invoice moved to archive (no longer active) | Can be viewed but not changed |

### Who Can Approve?

| Role | Can Approve? | Can Reject? | Can Edit? |
|------|-------------|-------------|-----------|
| **Admin** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Approver** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Viewer** | ❌ No | ❌ No | ❌ No |

### Approval Process

1. **Open the invoice** — Click on any invoice in the list to see its details
2. **Review the data** — Check the extracted fields (vendor, amounts, dates, line items) against the PDF preview shown side-by-side
3. **Edit if needed** — Toggle "Edit Mode" to correct any fields the AI got wrong (only for Pending or Under Review invoices)
4. **Approve or Reject**:
   - Click **Approve** → Invoice moves to "Approved" status
   - Click **Reject** → You must enter a reason, then invoice moves to "Rejected" status
5. **Notification sent** — When approved or rejected, an email notification is sent to the relevant team members

### Reprocessing Failed Invoices

If an invoice failed during AI extraction (or you want a fresh read):

1. Open the invoice
2. Click **Reprocess**
3. SyncLedger clears the old data and re-sends the PDF through the AI pipeline
4. New extraction data replaces the old data
5. Invoice returns to **Pending** status

### Audit Trail

Every action taken on an invoice is recorded and visible in the **Activity tab**:

- When the invoice was received (upload or email)
- When AI extraction started and completed
- When someone edited a field
- When a vendor was auto-matched
- When someone approved or rejected it
- When it was synced to the ERP
- Who did each action, and when

This creates a complete, tamper-proof history for compliance and auditing.

---

## 10. Pushing Data to Your Accounting System — Sage Intacct Deep Dive

### What Is ERP Sync?

Once an invoice is **Approved**, the extracted and verified data needs to go into your accounting system. SyncLedger's primary integration is with **Sage Intacct**, and the architecture supports additional ERPs:

| ERP System | Status |
|-----------|--------|
| **Sage Intacct** | Primary integration |
| **NetSuite** | Supported |
| **QuickBooks** | Supported |
| **Oracle** | Supported |
| **SAP** | Supported |
| **Xero** | Supported |
| **Custom** | Configurable via API |

### How Sage Credentials Are Stored

Before any sync can happen, your organization's Sage Intacct credentials must be configured. Here's what gets set up and how it's protected:

| Credential | What It Is | How It's Stored |
|-----------|-----------|----------------|
| **API Endpoint** | Sage Intacct's gateway URL | Stored in plain text (it's a public URL) |
| **User ID** | Your Sage API user account | Stored in the `erpTenantId` field |
| **Password / API Key** | Your Sage API password | **Encrypted** using AES-256-GCM before storage |
| **Company ID** | Which company entity inside Sage | Stored in `erpCompanyId` |
| **Object ID** | Optional reference object (e.g., for multi-entity) | Stored in encrypted JSON config |
| **Certificate Secret ID** | TLS certificate reference (if required) | Stored in encrypted JSON config |

**Security**: The API key is never stored as plain text. SyncLedger uses an `EncryptionService` with AES-256-GCM encryption. The key is only decrypted in-memory at the moment of the API call, then discarded.

The Sage Intacct API endpoint is:
```
https://api.intacct.com/ia/xml/xmlgw.phtm
```

### Step-by-Step: How an Invoice Is Pushed to Sage

Here is the complete flow of what happens when you click "Sync to Sage" or when auto-sync triggers:

```
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 1: TRIGGER                                             │
  │                                                              │
  │  Either:                                                     │
  │  • Admin clicks "Sync to Sage" button (manual)               │
  │  • Invoice just got approved + auto-sync is ON (automatic)   │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 2: VALIDATE                                            │
  │                                                              │
  │  SyncLedger checks:                                          │
  │  ✓ Invoice status is APPROVED                                │
  │  ✓ Invoice hasn't already been synced (syncStatus ≠ SUCCESS) │
  │  ✓ Organization has Sage credentials configured              │
  │  ✓ User has permission (Admin or Super Admin)                │
  │                                                              │
  │  If any check fails → error returned, sync does not proceed  │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 3: AUDIT — "Sync Started"                              │
  │                                                              │
  │  • Create a Sync Log entry (status: IN_PROGRESS)             │
  │  • Record who triggered it (user or system)                  │
  │  • Record trigger type (MANUAL, AUTO, or RETRY)              │
  │  • Start a timer (to measure how long it takes)              │
  │  • Log audit event: SYNC_STARTED                             │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 4: DECRYPT CREDENTIALS                                 │
  │                                                              │
  │  • Look up org's erpApiKeyEncrypted from database            │
  │  • Decrypt it using AES-256-GCM (in-memory only)             │
  │  • Read erpTenantId (user ID) and erpCompanyId               │
  │  • Read any extended config from erpConfigJson               │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 5: BUILD THE SAGE API REQUEST                          │
  │                                                              │
  │  SyncLedger builds an XML payload using YOUR invoice data    │
  │  and YOUR organization's Sage credentials.                   │
  │                                                              │
  │  (see "What the Sage Request Looks Like" below)              │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 6: SEND TO SAGE INTACCT                                │
  │                                                              │
  │  HTTP POST to:                                               │
  │  https://api.intacct.com/ia/xml/xmlgw.phtm                  │
  │                                                              │
  │  • Content-Type: text/xml                                    │
  │  • Body: the XML payload from Step 5                         │
  │  • The ENTIRE request payload is saved in the Sync Log       │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
  ┌──────────────────────────────────────────────────────────────┐
  │  STEP 7: RECEIVE SAGE'S RESPONSE                             │
  │                                                              │
  │  Sage Intacct responds with XML containing:                  │
  │                                                              │
  │  On SUCCESS:                                                 │
  │  • <status>success</status>                                  │
  │  • <recordno>SAGE_INVOICE_ID</recordno>   ← the reference!  │
  │  • <transactionid>TXN-ID</transactionid>  ← the receipt!    │
  │                                                              │
  │  On FAILURE:                                                 │
  │  • <status>failure</status>                                  │
  │  • <errorno>BL01001234</errorno>          ← error code       │
  │  • <description>Invalid GL account</description>             │
  │                                                              │
  │  • The ENTIRE response payload is saved in the Sync Log      │
  └──────────────────────────┬───────────────────────────────────┘
                             ▼
                    ┌────────┴────────┐
                    ▼                 ▼
  ┌─────────────────────┐  ┌─────────────────────────┐
  │  SUCCESS PATH       │  │  FAILURE PATH           │
  │                     │  │                         │
  │  • Save Sage ID     │  │  • Save error code      │
  │  • Save Transaction │  │  • Save error message   │
  │    ID on invoice    │  │  • Increment attempt     │
  │  • Set invoice      │  │    counter              │
  │    status → SYNCED  │  │  • If attempts < 3:     │
  │  • Set sync status  │  │    status → SYNC_FAILED │
  │    → SUCCESS        │  │    (can retry)          │
  │  • Log audit:       │  │  • If attempts = 3:     │
  │    SYNC_COMPLETED   │  │    status → SYNC_FAILED │
  │  • Stop timer       │  │    (final failure)      │
  │  • Save duration    │  │  • Log audit:           │
  │                     │  │    SYNC_FAILED           │
  └─────────────────────┘  └─────────────────────────┘
```

### What the Sage Request Looks Like

When SyncLedger talks to Sage Intacct, it sends an XML message. Here's a simplified version of what gets sent (think of it as a structured letter to Sage):

```
To: Sage Intacct
From: SyncLedger (on behalf of Your Company)

Authentication:
  Company: LONGHOME-PROD
  User: sage_user@company.com
  Password: ******* (decrypted at send time)

Please create this invoice:
  ┌─────────────────────────────────────────────┐
  │  Vendor ID:     VENDOR-ABC-001              │
  │  Invoice Date:  2025-01-15                  │
  │  Due Date:      2025-02-14                  │
  │  Description:   Invoice INV-2847            │
  │  Total Amount:  $5,234.56                   │
  │                                             │
  │  Line Items:                                │
  │  ┌──────────────┬─────────┬──────────────┐  │
  │  │ GL Account   │ Amount  │ Description  │  │
  │  ├──────────────┼─────────┼──────────────┤  │
  │  │ 5100         │ $4,500  │ Windows      │  │
  │  │ 5100         │ $1,600  │ Door frames  │  │
  │  │ 5200         │ $1,100  │ Labor        │  │
  │  └──────────────┴─────────┴──────────────┘  │
  └─────────────────────────────────────────────┘
```

**Important:** The GL account codes, vendor ID, and amounts all come from — your **mapping profile** (set during extraction) and the **AI-extracted data** (verified by your team).

### What Sage Sends Back

When Sage receives the request, it processes it and sends back a response. This is the **handshake** — the confirmation that the data was received and accepted.

**On Success — Sage says "Got it!":**
```
Status:         SUCCESS
Record Number:  12847          ← This is the Sage Invoice ID
Transaction ID: TXN-20250115  ← This is the Sage Transaction Reference
```

**On Failure — Sage says "Something is wrong":**
```
Status:         FAILURE
Error Code:     BL01001234
Error Message:  "Invalid account number '5100' for entity 'LONGHOME-PROD'"
```

### Automatic vs. Manual Sync

| Mode | How It Works |
|------|-------------|
| **Auto Sync** | When invoice is approved, sync is triggered automatically (if your org has `erpAutoSync = ON`) |
| **Manual Sync** | After approval, an admin clicks "Sync to Sage" button on the invoice detail page |

Your admin configures this per organization in the settings. Each organization independently chooses auto or manual.

### What Data Goes to Sage — Field-by-Field

Here's exactly which invoice fields are mapped to which Sage Intacct fields:

| SyncLedger Field | Sage Intacct Field | Where It Comes From |
|-----------------|-------------------|-------------------|
| Vendor ID / Name | `vendorid` | AI-extracted, verified by your team |
| Invoice Date | `invoicedate` | AI-extracted from PDF |
| Due Date | `duedate` | AI-extracted or calculated by mapping rule |
| Total Amount | `amount` | AI-extracted, cross-validated |
| Invoice Description | `description` | "Invoice INV-2847" (auto-generated) |
| Company Entity | `entity` | Your org's `erpCompanyId` setting |
| **Per Line Item:** | | |
| GL Account | `accountno` | Assigned by your mapping profile rule |
| Line Amount | `amount` | AI-extracted |
| Line Description | `memo` | AI-extracted from PDF |
| Project | `projectid` | Assigned by your mapping profile rule |
| Cost Center | `departmentid` | Assigned by your mapping profile rule |
| Location | `locationid` | Assigned by your mapping profile rule |

---

## 11. Confirming the Data Reached Your ERP — The Handshake

### What Is the Handshake?

The "handshake" is the **confirmation loop** between SyncLedger and Sage Intacct. It's how you know for certain that the invoice data made it into your books.

Think of it like sending a registered letter:
1. You send the letter (SyncLedger sends the invoice data to Sage)
2. The recipient signs for it (Sage responds with a confirmation and reference number)
3. You get the signed receipt back (SyncLedger saves the Sage reference number)
4. You can look at the receipt anytime to confirm delivery (visible on the invoice detail page)

### The 5 Levels of Confirmation

#### Level 1: Invoice Status Change (Instant Visual)

The moment Sage responds, the invoice status changes on screen:

| Before Sync | Sage Says "Success" | Sage Says "Error" |
|-------------|---------------------|-------------------|
| 🟢 APPROVED | 🟣 **SYNCED** | 🟠 **SYNC FAILED** |

This is visible immediately in the invoice list and on the invoice detail page.

#### Level 2: Sage Reference IDs (The Receipt)

When Sage accepts the invoice, it returns **two reference numbers** that SyncLedger saves permanently on the invoice:

| Reference | What It Is | How To Use It |
|-----------|-----------|--------------|
| **Sage Invoice ID** | The unique ID Sage assigned to this invoice in its system | Open Sage Intacct, search for this ID → you'll find the exact same invoice |
| **Sage Vendor ID** | The vendor record ID in Sage that this invoice was linked to | Confirms the right vendor was matched |

**Example:**[
- Your SyncLedger invoice `INV-2847` → Sage Invoice ID `12847`
- You can now go into Sage Intacct, search for record `12847`, and see the same amounts, GL codes, and vendor

This is the **cross-referencing proof** — both systems have matching records with linked IDs.

#### Level 3: Sync Status Tracking (Detailed State Machine)

SyncLedger tracks the sync through precise states:

```
             Sync triggered
                  │
                  ▼
           ┌──────────┐
           │ PENDING   │  Waiting in queue
           └─────┬─────┘
                 ▼
          ┌─────────────┐
          │ IN_PROGRESS  │  API call in flight
          └──────┬──────┘
                 │
         ┌───────┴───────┐
         ▼               ▼
   ┌──────────┐    ┌──────────┐
   │ SUCCESS  │    │  FAILED  │
   │          │    │          │
   │ Done!    │    │ Can retry?│
   │ Sage ID  │    │          │
   │ saved    │    │ Yes → RETRYING
   └──────────┘    │ No  → stays FAILED
                   └──────────┘
```

| Status | Meaning | What You See |
|--------|---------|-------------|
| **PENDING** | Sync is queued, waiting to be sent | Clock icon |
| **IN_PROGRESS** | Currently talking to Sage — API call in flight | Spinner |
| **SUCCESS** | Sage confirmed receipt — reference ID saved | Green checkmark + Sage ID |
| **FAILED** | Sage rejected the data or connection error | Red X + error message |
| **RETRYING** | Previous attempt failed, trying again | Spinner + attempt count |

#### Level 4: Complete Sync Log (Full Audit Trail)

Every single sync attempt is recorded in a dedicated **Sage Sync Log** table. This is the most detailed level of confirmation — it captures *everything*:

| What's Recorded | Purpose | Example |
|----------------|---------|---------|
| **Request Payload** | The exact XML data sent to Sage | Full XML document (preserved for debugging) |
| **Response Payload** | The exact XML response from Sage | Full XML response (proves what Sage said) |
| **HTTP Status Code** | The raw network response code | `200` = success, `401` = bad credentials, `500` = Sage error |
| **Sage Invoice ID** | The reference number from Sage's response | `12847` |
| **Sage Transaction ID** | The ledger transaction reference | `TXN-20250115` |
| **Error Code** | If failed, Sage's specific error code | `BL01001234` |
| **Error Message** | If failed, human-readable explanation | `"Invalid account number '5100'"` |
| **Attempt Number** | Which try this was (1, 2, or 3) | `1` (first attempt) |
| **Duration (ms)** | How long the API call took | `1,234` milliseconds |
| **Triggered By** | Which user initiated the sync | `Sarah Admin` |
| **Trigger Type** | How it was started | `MANUAL`, `AUTO`, or `RETRY` |
| **Timestamp** | When the attempt happened | `2025-01-15 14:46:32.456` |

**Why this matters:** If there's ever a question about whether data was sent correctly, you can pull up the sync log and see the **exact data that was sent** and the **exact response that came back**. Nothing is lost.

#### Level 5: Invoice Activity Timeline (Human-Readable History)

The invoice's **Activity tab** shows sync events in plain English alongside all other invoice events:

```
  📥 Jan 15 @ 2:34 PM — Received via email from billing@abc.com
  🤖 Jan 15 @ 2:34 PM — AI extraction started
  ✅ Jan 15 @ 2:34 PM — AI extraction completed (94% confidence)
  🔗 Jan 15 @ 2:34 PM — Vendor linked: ABC Construction Inc.
  ✅ Jan 15 @ 2:45 PM — Approved by Sarah
  🔄 Jan 15 @ 2:46 PM — Sync to Sage started (triggered by Sarah)     ← SYNC
  ✅ Jan 15 @ 2:46 PM — Synced to Sage — ID: 12847                    ← CONFIRMED
```

If the sync fails:
```
  ✅ Jan 15 @ 2:45 PM — Approved by Sarah
  🔄 Jan 15 @ 2:46 PM — Sync to Sage started (attempt 1)
  ❌ Jan 15 @ 2:46 PM — Sync failed: Invalid GL account '5100'        ← FAILED
  🔄 Jan 15 @ 2:51 PM — Sync to Sage started (attempt 2, auto-retry)
  ❌ Jan 15 @ 2:51 PM — Sync failed: Invalid GL account '5100'        ← FAILED AGAIN
  🔄 Jan 15 @ 3:06 PM — Sync to Sage started (attempt 3, auto-retry)
  ❌ Jan 15 @ 3:06 PM — Sync failed: Invalid GL account '5100'        ← FINAL FAILURE
  ⚠️  All 3 attempts failed. Manual intervention required.
```

### The Retry Mechanism — What Happens When Sync Fails

Not every failure is permanent. Sometimes Sage is temporarily unavailable, or there was a network glitch. SyncLedger handles this automatically:

```
  Attempt 1 fails
       │
       ▼
  Wait 5 minutes → Auto-retry (Attempt 2)
       │
       ▼
  Attempt 2 fails
       │
       ▼
  Wait 15 minutes → Auto-retry (Attempt 3)
       │
       ▼
  Attempt 3 fails
       │
       ▼
  FINAL: Invoice stays in SYNC_FAILED status
  Admin must investigate and fix the issue
  Then they can manually trigger a new sync
```

**The retry rules:**
- Maximum **3 attempts** per invoice
- Each retry is logged as a separate Sync Log entry
- The trigger type is marked as `RETRY` (vs. `MANUAL` or `AUTO`)
- After 3 failures, automatic retrying stops — a human must investigate
- Common fixable issues: wrong GL account, expired credentials, Sage maintenance window
- Admin can fix the mapping/config and click "Sync" again to restart the counter

### How To Cross-Reference Between SyncLedger and Sage

Once an invoice is synced, you have matching records in both systems:

```
  ┌─────────────────────────────┐     ┌─────────────────────────────┐
  │       SyncLedger            │     │       Sage Intacct          │
  │                             │     │                             │
  │  Invoice: INV-2847          │ ←──→│  Record #: 12847            │
  │  Sage Invoice ID: 12847     │     │  Reference: INV-2847        │
  │  Status: SYNCED ✅          │     │  Status: Posted             │
  │  Vendor: ABC Construction   │     │  Vendor: ABC Construction   │
  │  Total: $5,234.56           │     │  Total: $5,234.56           │
  │  GL: 5100                   │     │  Account: 5100              │
  │  Synced At: Jan 15 @ 2:46  │     │  Created: Jan 15 @ 2:46     │
  │                             │     │                             │
  │  Sync Log:                  │     │  Audit Log:                 │
  │  • Request XML ✓            │     │  • API received ✓           │
  │  • Response XML ✓           │     │  • Posted to GL ✓           │
  │  • Duration: 1,234ms        │     │                             │
  └─────────────────────────────┘     └─────────────────────────────┘
         Both systems agree. The data is confirmed in your books.
```

**To verify manually:**
1. In SyncLedger: Open the invoice → look at "Sage Invoice ID" field → note the number (e.g., `12847`)
2. In Sage Intacct: Go to Accounts Payable → Search for record `12847`
3. Compare: Same vendor? Same amount? Same GL account? Same date?
4. If they match → confirmed, the sync worked perfectly

### Summary: The Complete Handshake Lifecycle

| Step | What Happens | Confirmation Type |
|------|-------------|------------------|
| 1. **Trigger** | Admin clicks "Sync" or auto-sync fires | Sync Log created (PENDING) |
| 2. **Send** | XML payload sent to Sage Intacct API | Request payload saved in Sync Log |
| 3. **Receive** | Sage processes and responds | Response payload saved in Sync Log |
| 4. **Store IDs** | Sage Invoice ID + Transaction ID saved on invoice | Permanent cross-reference created |
| 5. **Update Status** | Invoice status → SYNCED (or SYNC_FAILED) | Visible in UI immediately |
| 6. **Audit** | Activity timeline updated with sync event | Full history preserved |
| 7. **Verify** | Admin can cross-reference with Sage Intacct directly | Both systems show matching records |

---

## 12. Vendors

### Automatic Vendor Recognition

When SyncLedger extracts an invoice, it automatically:

1. **Reads the vendor name** from the invoice (e.g., "MGD Construction Inc.")
2. **Normalizes it** — removes spaces, punctuation, converts to lowercase (e.g., "mgdconstructioninc")
3. **Checks if this vendor already exists** in your organization's vendor list
4. **If found** — links the invoice to the existing vendor, and updates any missing info (address, email, phone)
5. **If not found** — creates a new vendor record automatically

### Vendor Information

Each vendor record contains:

| Field | Example |
|-------|---------|
| Name | MGD Construction Inc. |
| Code | MGD-001 |
| Address | 123 Builder Way, Austin, TX |
| Email | billing@mgd.com |
| Phone | (512) 555-1234 |
| Tax ID | 87-1234567 |
| Payment Terms | Net 30 |
| Status | Active / Inactive / Blocked |

### Vendor Analytics

For each vendor, SyncLedger tracks:
- Total number of invoices
- Total dollar amount across all invoices
- Average invoice amount
- Last invoice date

---

## 13. Notifications

### When Are Notifications Sent?

| Event | Who Gets Notified | Channel |
|-------|------------------|---------|
| **Invoice Approved** | Organization contacts | Email |
| **Invoice Rejected** | Organization contacts | Email (marked High Priority) |

### What's in the Notification?

**Approval Email:**
- Invoice number and vendor name
- Total amount and currency
- Who approved it
- Any notes from the approver
- Link to view the invoice in SyncLedger

**Rejection Email (High Priority):**
- Invoice number and vendor name
- Total amount and currency
- Who rejected it
- **The reason for rejection**
- Link to view the invoice in SyncLedger

### Notification Tracking

Every notification is tracked with:
- Whether it was sent, failed, or cancelled
- The external message ID from the email provider
- How many delivery attempts were made

---

## 14. User Roles

SyncLedger has **four roles**, each with different permissions:

| Role | What They Can Do |
|------|-----------------|
| **Super Admin** | Everything. Manages all organizations, all users, system settings, runtime configuration. Platform-level access. |
| **Admin** | Full access within their organization: upload invoices, approve/reject, manage users, configure mappings, view billing, manage vendors, sync to ERP. |
| **Approver** | Can view invoices, approve or reject them, edit invoice data. Cannot manage users or system settings. |
| **Viewer** | Read-only access. Can view invoices, dashboard, and reports. Cannot make any changes. |

### Data Isolation

- **Super Admins** see data across all organizations
- **All other roles** only see data from their own organization
- There is no way for a user in Company A to see invoices from Company B

---

## 15. Multi-Company Support

SyncLedger is built for **multi-tenant** operation — multiple companies use the same platform, but each one is completely isolated:

### What's Separate Per Company

| Feature | Per Company? | Details |
|---------|-------------|--------|
| Invoices | ✅ Fully isolated | Each company sees only their own invoices |
| Vendors | ✅ Separate lists | Vendor auto-matching works within each company |
| Users | ✅ Org-scoped | Users belong to one company; roles enforced per org |
| **Mapping Rules** | ✅ **Custom per company** | **Each company creates its own profiles for its own ERP** |
| **ERP Type** | ✅ **Independent choice** | **Company A can use Sage while Company B uses QuickBooks** |
| **ERP Credentials** | ✅ **Separate & encrypted** | **Each company's API keys stored separately** |
| **ERP Field Mapping** | ✅ **Fully customizable** | **Each company defines which fields to push and how** |
| Email Inbox | ✅ Separate mailboxes | Each company connects their own Microsoft mailbox |
| File Storage | ✅ Isolated folders | Files stored in company-specific cloud folders |
| Subscription Plan | ✅ Independent billing | Each company has their own plan and billing |

### Why This Matters

Consider a platform with three companies:

```
┌──────────────────────────────────────────────────────────────────────┐
│                        SyncLedger Platform                          │
│                                                                      │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────┐ │
│  │ Alpha Corp        │ │ Beta LLC          │ │ Gamma Industries     │ │
│  │                   │ │                   │ │                      │ │
│  │ ERP: Sage Intacct │ │ ERP: QuickBooks   │ │ ERP: Custom API      │ │
│  │ GL: 5100, 5200    │ │ GL: 6100, 7200    │ │ GL: CC-001, CC-002   │ │
│  │ Mapping: 4 rules  │ │ Mapping: 2 rules  │ │ Mapping: 8 rules     │ │
│  │ Auto-sync: ON     │ │ Auto-sync: OFF    │ │ Auto-sync: ON        │ │
│  │ Email: invoices@  │ │ Email: ap@        │ │ Email: billing@      │ │
│  │   alpha.com       │ │   betallc.com     │ │   gammaindustries.com│ │
│  │ 3 users           │ │ 12 users          │ │ 5 users              │ │
│  │ 200 invoices/mo   │ │ 2,000 invoices/mo │ │ 500 invoices/mo      │ │
│  └──────────────────┘ └──────────────────┘ └──────────────────────┘ │
│                                                                      │
│  Each company's data, rules, and ERP sync are COMPLETELY SEPARATE.  │
│  They cannot see or affect each other in any way.                    │
└──────────────────────────────────────────────────────────────────────┘
```

When Alpha Corp's invoice gets approved → it syncs to **Sage** using **Alpha's GL codes**.  
When Beta LLC's invoice gets approved → it syncs to **QuickBooks** using **Beta's GL codes**.  
When Gamma's invoice gets approved → it syncs to **Gamma's Custom API** using **Gamma's GL codes**.  

**They never overlap. They never interfere. Each company is its own world.**

### Organization Settings

Each company can configure:
- **ERP type** and connection credentials
- **Email polling** mailbox and credentials
- **Auto-sync** on/off for approved invoices
- **Mapping profiles** for their vendors
- **Team members** with appropriate roles

---

## 16. The Complete Journey — Start to Finish

Here's the full story of one invoice, from arrival to being in your books:

---

**📧 Step 1: Invoice Arrives**

Your vendor, ABC Construction, emails an invoice to `invoices@yourcompany.com`. The email has a PDF attachment: `Invoice-2847.pdf`.

---

**🔍 Step 2: SyncLedger Picks It Up**

Within 5 minutes, SyncLedger's email poller checks your inbox, finds the unread email, downloads the PDF attachment, and stores it securely in the cloud.

It records: *"Received invoice via email from billing@abcconstruction.com, subject: 'Invoice #2847 for January work'"*

---

**🤖 Step 3: AI Reads the Invoice**

SyncLedger sends the PDF to its AI extraction service:

1. **Vision AI** looks at the invoice image and identifies:
   - Invoice Number: 2847
   - Vendor: ABC Construction Inc.
   - Invoice Date: January 15, 2025
   - Due Date: February 14, 2025
   - Subtotal: $5,000.00
   - Tax: $234.56
   - Total: $5,234.56
   - 3 line items (Windows, Doors, Labor)

2. **Cross-validation** runs the pattern matcher independently and compares:
   - Invoice number: ✅ Both say 2847
   - Total: ✅ Both say $5,234.56
   - Vendor: ✅ Both say ABC Construction
   - **Confidence: 94%** (High)

---

**📋 Step 4: Mapping Rules Applied**

SyncLedger checks your mapping profiles:

1. Finds profile "ABC Construction Invoices" (vendor pattern matches "ABC")
2. Applies rules:
   - GL Account → **5100** (Construction Materials)
   - Project → **OPP-12345** (read from PO number field)
   - Cost Center → **CC-001** (Main Office)
   - Due Date → Confirmed from invoice (Feb 14, 2025)
3. Applies GL 5100 to each line item that doesn't have its own GL code

Records the mapping audit trail: *"Profile 'ABC Construction Invoices' selected via vendor pattern match. GL Account 5100 set from rule #1 default value."*

---

**👤 Step 5: Vendor Auto-Matched**

SyncLedger normalizes "ABC Construction Inc." → "abcconstructioninc" and finds it already exists in your vendor list. Links the invoice to the existing vendor record. Updates the vendor's phone number (was missing, now found on this invoice).

---

**📊 Step 6: Invoice Ready for Review**

The invoice appears in the **Invoices** page under the "Pending Review" tab:

| Invoice # | Vendor | Amount | Date | Status | Confidence |
|-----------|--------|--------|------|--------|------------|
| 2847 | ABC Construction Inc. | $5,234.56 | Jan 15, 2025 | 🟡 Pending | 🟢 94% |

---

**✅ Step 7: Admin Reviews and Approves**

Sarah (Admin) clicks on Invoice 2847. She sees:

- **Left side:** The actual PDF preview
- **Right side:** All extracted data, line items, mapping info, and confidence score

Everything checks out. She clicks **Approve**.

The system:
- Creates an approval record (Sarah, Approved, Jan 15 @ 2:45 PM)
- Changes status to **Approved**
- Logs the action in the audit trail
- Sends an email notification to the team

---

**🔄 Step 8: Push to Sage**

Sarah clicks **Sync to Sage**. SyncLedger:

1. Builds a data package with all invoice fields + GL codes + line items
2. Sends it to Sage via secure API
3. Sage responds: *"Created. Invoice ID: SI-20250115-001, Transaction: TXN-98234"*
4. SyncLedger saves these reference IDs
5. Status changes to **Synced** ✅

---

**✅ Step 9: Confirmation**

The invoice now shows:

| Field | Value |
|-------|-------|
| Status | 🟣 Synced |
| Sage Invoice ID | SI-20250115-001 |
| Sage Transaction ID | TXN-98234 |
| Synced At | Jan 15, 2025 @ 2:46 PM |

The Activity tab shows the complete history:
1. 📥 Received via email — Jan 15 @ 2:34 PM
2. 🤖 AI extraction started — Jan 15 @ 2:34 PM
3. ✅ AI extraction completed (94% confidence) — Jan 15 @ 2:34 PM
4. 🔗 Vendor linked: ABC Construction Inc. — Jan 15 @ 2:34 PM
5. ✅ Approved by Sarah — Jan 15 @ 2:45 PM
6. 🔄 Sync started — Jan 15 @ 2:46 PM
7. ✅ Synced to Sage (SI-20250115-001) — Jan 15 @ 2:46 PM

**The invoice is now in your books. Done! 🎉**

---

## 17. Dashboard and Reporting

### Dashboard Overview

When you log in, you see a personalized dashboard with:

- **Welcome message**: "Good morning, Sarah!"
- **Quick stats**: Total invoices, Pending review, Approved today, Total value
- **Your organization context**: "Showing data for ABC Company"

### Customizable Analytics Widgets

You can add and arrange widgets:
- Invoices by status (pie chart)
- Monthly invoice volume (bar chart)
- Vendor breakdown (who sends the most invoices)
- Approval metrics (how fast invoices get approved)
- Total value over time

### Filters

- **Date range**: Today, This Week, This Month, Custom range
- **Status**: Filter by any combination of statuses

### Export to Excel

You can export invoices to Excel spreadsheets:
- Choose which columns to include
- Filter by status, date range, vendor
- Download as `.xlsx` file

---

## 18. System Settings

### Runtime Configuration (Admin)

Admins and Super Admins can adjust system behavior **without restarting** the application:

| Setting | What It Controls | Default |
|---------|-----------------|---------|
| **CORS Allowed Origins** | Which websites can access the API | Your app domain |
| **Email Polling Enabled** | Turn email polling on/off | On |
| **Email Polling Interval** | How often to check email | Every 5 minutes |
| **Max Emails Per Batch** | How many emails to process at once | 50 |
| **Confidence Threshold** | Below this score → manual review required | 87% |
| **PDF Service URL** | Where the AI extraction service lives | (internal URL) |
| **PDF Service Timeout** | How long to wait for AI response | 120 seconds |
| **Logging Levels** | How much detail to log (for debugging) | INFO |

### Mapping Configuration (Admin)

Admins can create and manage mapping profiles:
- Create new profiles for specific vendors
- Define mapping rules with conditions
- Set default profiles for the organization
- Test rules against sample invoices

---

## 19. Subscription Plans

| Plan | Price/Month | Invoice Limit | Key Features |
|------|------------|--------------|--------------|
| **Trial** | Free (15 days) | Full access | Try everything before buying |
| **Starter** | $349 | 1,000/month | Core features, email support |
| **Professional** | $649 | 5,000/month | Advanced mapping, priority support |
| **Business** | $799 | 10,000/month | Custom rules, dedicated onboarding |
| **Enterprise** | $1,499 | Unlimited | Custom SLA, dedicated support, API access |

---

## Glossary

| Term | Meaning |
|------|---------|
| **AI Extraction** | Using artificial intelligence to read a PDF and pull out the data |
| **Confidence Score** | A number (0-100%) showing how sure the AI is about what it read |
| **Cross-Validation** | Double-checking AI results against pattern matching |
| **ERP** | Enterprise Resource Planning — your accounting/business system (Sage, QuickBooks, etc.) |
| **GL Account** | General Ledger Account — an accounting category code (e.g., 5100 = Materials) |
| **Mapping Profile** | A set of rules that tells SyncLedger which accounting codes to assign |
| **Mapping Rule** | A single instruction like "put the value from Field A into GL Account B" |
| **OCR** | Optical Character Recognition — converting scanned images to readable text |
| **PO Number** | Purchase Order number — a reference linking the invoice to an order |
| **Presigned URL** | A temporary, secure link to download a file from cloud storage |
| **Regex** | Regular Expression — a pattern-matching technique for finding data in text |
| **Sync** | Sending approved invoice data to your accounting system |
| **Tenant** | A company/organization using the platform — each tenant's data is separate |
| **Under Review** | Status meaning the AI wasn't confident enough — a human needs to check |

---

*This documentation covers the complete SyncLedger platform flow as of March 2026.*
