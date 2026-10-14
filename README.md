# J. POO Morgan — E-Banking Simulator

A Java 21 command-driven banking engine. It reads a JSON file describing users, exchange rates, merchants and a timeline of commands, executes them against an in-memory bank, and writes the results as JSON.

## Features

| Area | Capabilities |
|------|--------------|
| **Accounts** | Classic and savings accounts, aliases, minimum balance, interest rates, business accounts with owner / manager / employee roles |
| **Cards** | Regular and one-time cards, freeze/status checks, deletion |
| **Payments** | Online payments, money transfers, ATM cash withdrawal, savings withdrawal |
| **Split payments** | Equal and custom splits with accept/reject flow per participant |
| **Plans & fees** | `standard`, `student`, `silver`, `gold` plans with per-plan commissions and upgrades (including automatic silver → gold) |
| **Cashback** | Per-merchant transaction-count and spending-threshold cashback |
| **Reports** | Transaction history, account reports, per-merchant spending reports |
| **Multi-currency** | Exchange rates resolved through a rate graph |

## Supported Commands

`printUsers` · `addAccount` · `deleteAccount` · `createCard` · `createOneTimeCard` · `deleteCard` · `checkCardStatus` · `addFunds` · `setAlias` · `setMinimumBalance` · `payOnline` · `sendMoney` · `splitPayment` · `acceptSplitPayment` · `rejectSplitPayment` · `cashWithdrawal` · `withdrawSavings` · `upgradePlan` · `changeInterestRate` · `addInterest` · `printTransactions` · `report` · `spendingsReport` · `addNewBusinessAssociate` · `changeSpendingLimit` · `changeDepositLimit` · `businessReport`

## Architecture

```
JSON input ──► Main ──► AppLogic (singleton state) ──► CommandSelector ──► Executable command ──► JSON output
```

| Pattern | Where | Purpose |
|---------|-------|---------|
| **Singleton** | `AppLogic` | Single holder of users, merchants and exchange rates |
| **Command** | `CommandSelector`, `Executable` | Each command is its own class, selected by name |
| **Factory** | `AccountFactory` | Creates classic or savings accounts |
| **Strategy** | `SplitPaymentStrategy` | Equal vs. custom split payments |

### Split payment flow

1. A `splitPayment` command creates a `PendingTransaction` shared by all participants.
2. `acceptSplitPayment` / `rejectSplitPayment` responses are checked by `VerifySplitPaymentBase`.
3. If every participant accepts, the transaction is executed and logged for each of them; otherwise it is cancelled.
4. Pending transactions are flushed when `printTransactions` is called, up to the current timestamp.

## Project Structure

```
.
├── pom.xml                         # Maven build (Java 21, Jackson, Gson, Lombok)
├── input/                          # Test scenarios (test01 … test20)
├── ref/                            # Expected outputs
├── result/                         # Generated outputs (written by Main)
└── src/main/java/org/poo/
    ├── main/                       # Entry points (Main runs all tests, Test runs one)
    ├── checker/                    # Output comparison and checkstyle runner
    ├── fileio/                     # JSON input models
    ├── entities/                   # Domain: User, Account, Card, Commerciant, AccountFactory
    ├── e_banking/
    │   ├── AppLogic.java           # Application state and initialization
    │   ├── helpers/                # CommandSelector, Executable, Commission, ExchangeRateManager
    │   └── commands/               # One class (or package) per command
    │       ├── accountAdd_Del/
    │       ├── cardAdd_Del/
    │       ├── interestRate/
    │       ├── payOnlineCommand/
    │       ├── reports/
    │       └── splitPayment/
    └── utils/                      # Shared utilities
```

## Getting Started

**Requirements:** JDK 21 and Maven.

```bash
# Build
mvn clean package

# Run all tests (reads input/, writes result/, compares with ref/)
java -cp target/classes:$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) org.poo.main.Main

# Run a single test (writes out.txt)
# Run org.poo.main.Test with the input file name as argument, e.g. test01_user_updates.json
```

Running `Main` from an IDE works too.

## Tests

| Group | Tests | Focus |
|-------|-------|-------|
| Basic | 01 – 10 | Users, plans, commissions, savings, cashback, split payments, business accounts |
| Functional | 11 – 17 | Error handling, split & spend, savings, merchants |
| Flow | 18 – 20 | Large mixed inputs |

## Code Style

Checkstyle rules are in `src/main/resources/poo_checks.xml`; run `src/main/resources/checkstyle.sh` to verify.
