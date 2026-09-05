# Parking Lot — UML Class Diagram

```mermaid
classDiagram

    %% ─────────────────────────────────────────
    %% CORE — Parking Area
    %% ─────────────────────────────────────────

    class ParkingLot {
        <<Singleton>>
        -instance : ParkingLot
        -spots : Map~Integer, ParkingSpot~
        -tickets : Map~Integer, ParkingTicket~
        -boards : List~DisplayBoard~
        +rate : ParkingRate
        +getInstance() ParkingLot
        +addSpot(ParkingSpot)
        +parkVehicle(Vehicle) ParkingTicket
        +freeSlot(int)
        +getAllSpots() Collection~ParkingSpot~
        -canFit(Vehicle, ParkingSpot) boolean
    }

    class Entrance {
        -id : int
        +getTicket(Vehicle) ParkingTicket
    }

    class Exit {
        -id : int
        +validateTicket(ParkingTicket)
    }

    class DisplayBoard {
        -id : int
        -freeCount : Map~String, Integer~
        +update(Collection~ParkingSpot~)
        +showFreeSlot()
    }

    class ParkingTicket {
        -ticketNo : int
        -slotNo : int
        -entryTime : Date
        -exitTime : Date
        -amount : double
        -status : TicketStatus
        +getTicketNo() int
        +getSlotNo() int
        +setExitTime(Date)
        +setAmount(double)
        +setStatus(TicketStatus)
        +getAmount() double
    }

    %% ─────────────────────────────────────────
    %% PARKING SPOTS
    %% ─────────────────────────────────────────

    class ParkingSpot {
        <<abstract>>
        #id : int
        #isFree : boolean
        #vehicle : Vehicle
        +isFree() boolean
        +assignVehicle(Vehicle) boolean*
        +removeVehicle() boolean
        +getId() int
    }

    class Compact { +assignVehicle(Vehicle) boolean }
    class Large { +assignVehicle(Vehicle) boolean }
    class Accessible { +assignVehicle(Vehicle) boolean }
    class MotorcycleSpot { +assignVehicle(Vehicle) boolean }

    ParkingSpot <|-- Compact
    ParkingSpot <|-- Large
    ParkingSpot <|-- Accessible
    ParkingSpot <|-- MotorcycleSpot

    %% ─────────────────────────────────────────
    %% VEHICLES
    %% ─────────────────────────────────────────

    class Vehicle {
        <<abstract>>
        -licenseNo : String
        -ticket : ParkingTicket
        +getLicenseNo() String
        +assignTicket(ParkingTicket)
        +getTicket() ParkingTicket
    }

    class Car
    class Truck
    class Van
    class Motorcycle

    Vehicle <|-- Car
    Vehicle <|-- Truck
    Vehicle <|-- Van
    Vehicle <|-- Motorcycle

    %% ─────────────────────────────────────────
    %% PAYMENTS
    %% ─────────────────────────────────────────

    class Payment {
        <<abstract>>
        #amount : double
        #status : PaymentStatus
        #timestamp : Date
        +initiateTransaction() boolean*
    }

    class Cash { +initiateTransaction() boolean }
    class CreditCard { +initiateTransaction() boolean }

    class ParkingRate {
        +calculate(double, Vehicle, ParkingSpot) double
    }

    Payment <|-- Cash
    Payment <|-- CreditCard

    %% ─────────────────────────────────────────
    %% USERS
    %% ─────────────────────────────────────────

    class Account {
        <<abstract>>
        -userName : String
        -password : String
        -status : AccountStatus
        +resetPassword() boolean*
    }

    class Admin {
        +addParkingSpot(ParkingSpot) boolean
        +addDisplayBoard(DisplayBoard) boolean
        +addEntrance(Entrance) boolean
        +addExit(Exit) boolean
        +resetPassword() boolean
    }

    class Person {
        -name : String
        -phone : String
        -email : String
        -address : String
    }

    Account <|-- Admin
    Account "1" --> "1" Person : has

    %% ─────────────────────────────────────────
    %% ENUMS
    %% ─────────────────────────────────────────

    class TicketStatus {
        <<enumeration>>
        ISSUED
        IN_USE
        PAID
        VALIDATED
        CANCELED
        REFUNDED
    }

    class PaymentStatus {
        <<enumeration>>
        COMPLETED
        FAILED
        PENDING
        UNPAID
        REFUNDED
    }

    class AccountStatus {
        <<enumeration>>
        ACTIVE
        CLOSED
        CANCELED
        BLOCKLISTED
        NONE
    }

    %% ─────────────────────────────────────────
    %% RELATIONSHIPS
    %% ─────────────────────────────────────────

    ParkingLot "1" o-- "many" ParkingSpot   : manages
    ParkingLot "1" o-- "many" ParkingTicket : tracks
    ParkingLot "1" o-- "many" DisplayBoard  : updates
    ParkingLot "1" --> "1"    ParkingRate   : uses

    Entrance --> ParkingLot    : delegates to
    Entrance ..> ParkingTicket : creates

    Exit --> ParkingLot    : delegates to
    Exit ..> Payment       : creates
    Exit --> ParkingTicket : validates

    ParkingTicket "1" --> "1" Vehicle      : issued to
    ParkingTicket --> TicketStatus         : has status

    Vehicle "1" --> "0..1" ParkingTicket   : holds

    ParkingSpot "1" --> "0..1" Vehicle     : holds

    Payment --> PaymentStatus : has status
    Account --> AccountStatus : has status
```
