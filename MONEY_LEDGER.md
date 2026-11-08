# Farm operations, money, sourcing and direct sales

Deploy the API and frontend together. The new API uses the existing PostgreSQL / Hibernate schema-update configuration to add `money_accounts`, `money_entries`, customer type, sales channel, and purchase location. If schema updates are disabled in an environment, provision these additive schema changes before deployment. No production database was accessed during implementation.

## Starting balances

The first ledger read or sales mutation takes a PostgreSQL transaction advisory lock and imports existing collected sale amounts and cash handovers once, atomically. The Samaksh bank account is the initialization marker. Existing records are not deleted. Historical sale receipts use the collector currently stored on the sale; the old system did not preserve the collector of every partial payment. Historical handover recipients are separate accounts because free-text names cannot safely identify users.

Before treating balances as reconciled, compare them with Excel and actual cash/bank balances. Transfer verified historical-recipient balances into the corresponding user's account. Opening balances should include only funds absent from imported records. Never enter the same imported collection again as an opening balance. The UI retains a reconciliation notice when historical records exist; it does not claim the imported ownership is verified.

## Behavior

- `GET /api/money` returns balances and the journal. Managers, super admins and sales admins can manage money. Sales staff can continue to collect payments and record direct sales; those receipts credit their own farm-held account.
- Farm-held cash, bank funds and savings are farm assets. Personal-pocket accounts record payments made personally, not a person's bank balance or an amount owed to them.
- Expenses and salary payments debit the selected funding source. Salary payments retain the employee and salary month. This is a payment register, not payroll accrual or salary-due calculation.
- Transfers debit and credit two distinct farm accounts in one transaction. Insufficient funds are rejected. Amounts use decimal arithmetic in the journal.
- New collections are distinct journal events. Updating an older invoice does not move earlier receipts between people. Delivery entry edits preserve receipts; receipt reductions are rejected rather than silently rewriting cash history. Refunds/receipt corrections require a future explicit reconciliation workflow.
- Money transactions and direct-sale creation have retry keys with payload fingerprints. Retrying the same payload does not post twice. A changed payload with an already-used key is rejected.
- Expenses, salaries, transfers, contributions and opening balances can be reversed with a reason, preserving both entries. Collections and imported entries cannot be reversed through this general-purpose action. Spent destination funds prevent transfer reversal.
- Old cash-handover mutation endpoints reject writes with a message directing callers to Money & Expenses; deploy the frontend at the same time. The old GET remains historical compatibility data and is no longer used by the frontend.
- Direct sales reuse the sales/product model and contribute to existing sales totals. Customers have `customerType=DIRECT` and sales have `salesChannel=DIRECT`. The dedicated screen supports contact details, location, purchase history and later collections. Old direct sales represented as shops are not automatically reclassified.

## Checks

Run `mvn -Dtest=MoneyServiceTest,DirectSaleControllerTest,SourcingServiceTest,SourcingPaymentTest,OperationsServiceTest,SaleCollectionTest test` for ledger scenarios. The focused suite covers ledger edits/settlements, direct-sale customer reuse/collector permissions, zero-box collections, inventory/production edits, sourcing costs and vendor payments. Mockito's Java-agent attachment required running the tests outside the Windows sandbox. The existing context-load test needs the configured application database and was not run against production.

The journal and direct-sales endpoints currently return full history. Add server pagination/aggregation before significantly larger datasets. Writes share a database lock to keep balance checks consistent across app instances; this prioritizes integrity for the current farm scale.

## Monthly money and edits

The monthly tables carry balances forward, separate transfers from expenditure, and show full current holder balances. Settlement transfers the holder's entire current balance to a bank account atomically. Admin edits update the original eligible journal entry, check its revision, validate resulting balances and retain before/after audit details. Sourcing-linked entries are edited through their receipt/payment screen; internal reversals preserve the old linked expense. Personal accounts are provisioned for active users.

## Sourcing

Vendor and own-farm sources have configurable boxes/kg and cost/kg. Receipts retain conversion and selling-price snapshots. Vendor-borne charges paid by Samaksh reduce payable; farm-borne charges paid by the vendor increase payable. Damaged boxes reduce sellable quantity and estimated margin without silently granting a vendor credit. Optional sold quantity is manual reporting and is not automatically allocated from Sales. Full/partial vendor payments use the selected personal, held-sales or bank account and update the journal and due amount together. Admin receipt/payment edits are audited.

## Inventory and farm operations

Inventory uses stock, transactions and type-management tables. Inventory and movement types are stored in inventory_options. First catalog access seeds built-in types and removes legacy PostgreSQL enum check constraints on inventory_transaction so custom type codes can be stored. Stock direction is saved on each new movement; changing a custom movement type affects future entries, not past stock. Built-in directions are protected. Admins can edit transactions; production-linked movements are changed through batch edits.

Production edits reconcile material consumption (including damage) in the same transaction and cannot remove bags already moved out of the dark room. Production, Room Transfer and Harvest appear under Farm Operations with separate tabs. Older routes redirect to the matching tab. Inventory and production writes share a PostgreSQL advisory lock.

## Local verification and rollout

Frontend: node scripts/test-finance-flows.mjs; npx tsc --noEmit; npx vite build. API tests mock repositories and do not alter production records. A real database/browser end-to-end test has not been performed. Restart/deploy the API alongside the frontend to activate the new endpoints and additive entity fields/tables. Existing installations must allow the configured Hibernate schema update and the inventory constraint migration; otherwise apply equivalent schema changes before use. No deployment or production-data mutation was performed by this implementation.

## Pooled daily profit dashboard and recoverable vendor payments

The dashboard report at /api/sourcing/report uses arbitrary historical date ranges and compares the preceding month (full-month to full-month when a full month is selected). Actual billed sales revenue excludes collection-only rows. Each day's receipt goods costs, including own-farm costs, are pooled without assigning individual sales to sources. Gross profit is billed revenue minus those goods costs; the daily-cost net result also subtracts farm-borne receipt charges and other recorded expenses/salary. Sourcing payment/transport journal entries and their reversals are excluded from overhead to prevent double-counting. Advances and recovered advances never count as turnover or expenses. This is a same-day cost method, not inventory-valued accrual profit; days with different received/sold quantities are flagged. Own-farm costs must not include overhead separately entered as expenses. Underlying receipts, sales and expense entry IDs are returned for traceability.

Vendor payments now separate invoice amount and optional additionalAmount, with independent expense and VENDOR_ADVANCE journal entries in one transaction. Additional money is recoverable rather than spent on goods. Sourcing recoveries (new sourcing_recoveries table) credit the selected account as VENDOR_RECOVERY. Admin edits reverse/replace linked journal entries and retain audit history. Cannot recover more than the remaining advance or reduce an advance below recoveries already recorded. No automatic offset against another invoice is assumed.

## Historical records and imports

SUPER_ADMIN can use Historical Data or the shortcuts inside Money, Farm Operations, Orders and Sourcing. Manual entry and .xlsx/UTF-8 CSV imports cover money accounts/entries, inventory types/movements, production, room transfers, harvest, orders, sources, receipts, payments and recoveries. Templates include reference IDs. Imports support at most 200 rows, validate before committing, and use module + externalReference for retry protection. A changed imported reference is rejected; edit the original record in its normal screen. The complete batch rolls back on failure. Import audit timestamps remain current while business dates are historical. Production imports consume stock; import inventory first. Historical fulfilled order imports do not create a sale. No real records were imported during implementation.

New business-date fields: inventory_transaction.transaction_date, production request startDate / room transfer movementDate, and customer_order.order_date. Existing rows display their creation date as fallback. New retro_import_log records reference, fingerprint, target ID and importer. Old .xls files need conversion to .xlsx; formulas must be pasted as values.

## Personal reimbursements and role access

REIMBURSEMENT debits a farm account and credits a PERSONAL account. Full or partial repayments are recorded in Money & Expenses > Personal reimbursements, and editable there or in Transactions. Repayment cannot exceed the current debt or the debt recorded as of its date. Edits check revision and resulting balances and audit before/after values. Reimbursement is not another expense and does not inflate profit costs. Correct repayments before editing a personal expense below the amount already repaid. Vendor refunds/reversals can create a credit owed to the farm, which is displayed separately.

Navigation and routes use one frontend role matrix: SUPER_ADMIN retains all sections; SALES_EMPLOYEE/SALES_USER have Sales with Delivery Entry and Direct Sales; SALES_ADMIN additionally has Box Allocation and Shop Setup; FARM_MANAGER/LABOUR have Farm Operations only. Multiple roles combine their workspaces. API restrictions mirror these workspaces; customer setup writes require SUPER_ADMIN/SALES_ADMIN, while direct-sale customer creation remains available through the direct-sales endpoint. Logout is never hidden. Deploy backend and frontend together so route restrictions match.

Sourcing now contains receipts, source accounts and payments/recoveries only. Dashboard profit has expandable comparisons and daily traces. Funding dropdowns receive aggregate balances in the same sourcing response and distinguish available farm money from personal amounts owed. Existing account provisioning uses a preloaded account-key set, avoiding one lookup per existing user account on every read. Shared GET caching expires quickly and invalidates after writes; final server balance checks remain transactional.
