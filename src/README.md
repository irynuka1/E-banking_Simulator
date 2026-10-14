# Implementation Notes

Technical overview of the J. POO Morgan e-banking simulator, covering both project phases. For a general introduction, see the [root README](../README.md).

## 1. Package Overview

### `entities` — domain model
| Class | Responsibility |
|-------|----------------|
| `User` | Application user; owns accounts, a plan (`standard`, `student`, `silver`, `gold`) and a list of pending split payments |
| `Account` | Base account; subclasses `ClassicAccount` and `SavingsAccount`, created through `AccountFactory` (Factory pattern) |
| `Card` | Regular or one-time card with status |
| `Commerciant` | Merchant data plus per-account transaction counters used for cashback |
| `ToOutput` | Interface for entities that serialize themselves (with sub-entities) to JSON |

Transactions of each user and account are stored as JSON in an `ArrayNode`.

### `e_banking` — application logic
| Class | Responsibility |
|-------|----------------|
| `AppLogic` | Singleton holding users, merchants and exchange rates; initializes the app and runs the command list |
| `helpers/CommandSelector` | Maps a command name to its `Executable` implementation (Command pattern) |
| `helpers/ExchangeRateManager` | Stores input rates and derives intermediate rates when no direct pair exists |
| `helpers/Commission` | Returns the commission rate for a user's plan |
| `commands/*` | One class per command; shared logic lives in abstract bases (`AccountBase`, `CardBase`, `InterestRateBase`, `BaseReport`, `VerifySplitPaymentBase`) |

### `main`, `fileio`, `checker`, `utils`
`Main` runs every test in `input/`, writes to `result/` and compares with `ref/`. `Test` runs a single file into `out.txt`. `fileio` holds the JSON input models; `checker` holds the output comparison and checkstyle tooling.

## 2. Feature Implementation

### Plans and commissions
- Each user gets a `plan` at creation (`student` or `standard`, depending on occupation).
- `UpgradePlan` validates the upgrade rules (current plan, fee, balance) and applies the fee.
- A silver user is upgraded to gold for free after at least five payments of 300 RON or more; `PayOnline` calls `UpgradePlan.freeUpgradeIfPossible` for this.
- `Commission` provides the rate used by payments, transfers and withdrawals.

### Cashback
- Merchants are loaded into `AppLogic` at start-up.
- Each account tracks its transactions per merchant, the total amount spent, and one flag per category (food, clothes, tech) marking an unlocked `nrOfTransactions` discount.
- `PayOnline` handles both cashback strategies:
  - `nrOfTransactions`: increments the merchant counter and applies the discount once the threshold is reached.
  - `spendingThreshold`: adds to the account's total spending and derives the cashback from the plan and the spending tier.

### Split payments
Implemented with the **Strategy** pattern in `commands/splitPayment`:
- `EqualSplitPayment`: the amount is divided equally between accounts.
- `CustomSplitPayment`: each participant has an explicit amount.
- `PendingTransaction`: holds a split awaiting responses; every involved user keeps a list of them.
- `VerifySplitPaymentBase`: checks participant responses; if all accept, the payment is added to the users' pending lists, otherwise it is cancelled.
- `AppLogic.verifiedTransactions` stores timestamps already verified, so a response is never processed twice.
- On `printTransactions`, pending payments up to the current timestamp are executed, and the user's transactions are re-sorted by timestamp before printing.

### Accept / reject
Invalid `acceptSplitPayment` / `rejectSplitPayment` outputs are buffered in `AppLogic.userNotFounds`. `AcceptRejectPayment` checks that buffer: if the output is absent, the command was valid and has been handled by `VerifySplitPaymentBase`.

### Reports
- `report` and `spendingsReport` filter the account's JSON transaction list to the requested time interval.
- `spendingsReport` additionally builds a `TreeMap` of merchant → total spent, so merchants appear alphabetically.

## 3. Design Patterns

| Pattern | Class(es) |
|---------|-----------|
| Singleton | `AppLogic` |
| Command | `CommandSelector`, `Executable` and the command classes |
| Factory | `AccountFactory` |
| Strategy | `SplitPaymentStrategy`, `EqualSplitPayment`, `CustomSplitPayment` |
| Template-style abstract bases | `AccountBase`, `CardBase`, `InterestRateBase`, `BaseReport` |

## 4. Business Accounts

Implemented in `entities/BusinessAccount`, `e_banking/commands/business` and `e_banking/helpers/BusinessRules`.

- **Roles**: the creator is the `owner`; `addNewBusinessAssociate` adds `manager` / `employee` users. An associate gets access to the account through `User.associatedAccounts`, so it can use the account's cards without the account appearing in their `printUsers` output. Unknown users, repeated associates and unknown roles are ignored.
- **Limits**: default spending and deposit limits are 500 RON converted to the account currency (computed lazily, until the owner changes them). Only employees are bound by them: a payment or deposit above the limit is silently ignored.
- **Permissions**: `changeSpendingLimit` / `changeDepositLimit` are owner-only and report an error otherwise. A user that is not associated with the account cannot find its cards (`Card not found`) nor deposit.
- **Statistics**: deposits and payments of managers and employees are stored as `BusinessActivity` entries; `businessReport` aggregates them per time interval, either per associate (`transaction`) or per merchant (`commerciant`). Owner activity is not part of the report.
- **Plans**: commissions and cashback of payments made from a business account use the owner's plan.
