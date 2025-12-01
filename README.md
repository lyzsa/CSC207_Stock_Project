# Stock Project

A comprehensive stock market application built with Java Swing, following Clean Architecture principles. This application provides real-time stock data, news, earnings history, and market status information.

## Features

### User Management
- **User Registration**: Create a new account with username and password
- **User Login**: Secure authentication to access the application
- **Password Management**: Change password functionality
- **User Logout**: Secure session termination

### Stock Information
- **Stock Search**: Search for stocks by symbol with detailed information
- **Filter Search**: Advanced filtering capabilities for stock discovery
- **Real-Time Trade Feed**: Live WebSocket connection to receive real-time trade data
  - Connect to any trading symbol (e.g., `BINANCE:BTCUSDT`, `AAPL`, `TSLA`)
  - View real-time price, volume, and timestamp updates
  - Rate limiting protection to prevent API throttling
  - Automatic error handling for invalid symbols or connection issues

### Market Data
- **Market News**: View latest market and company-specific news articles
- **Earnings History**: Access historical earnings data for companies
- **Market Status**: Real-time market open/closed status indicator

## Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
src/main/java/
├── app/                    # Application entry point and dependency injection
├── data_access/            # Infrastructure layer (API clients, file I/O)
├── entity/                 # Domain models
├── interface_adapter/      # Adapters (ViewModels, Controllers, Presenters)
├── use_case/              # Business logic (Interactors)
└── view/                  # Presentation layer (Swing UI)
```

### Key Components

- **Entity Layer**: Domain models (`User`, `Stock`, `Trade`, `NewsArticle`, etc.)
- **Use Case Layer**: Business logic and application rules
- **Interface Adapter Layer**: Controllers, Presenters, and ViewModels
- **Framework Layer**: Data access objects and UI components

## Getting Started

### Prerequisites
- Java 15 or higher
- Maven 3.6+
- Internet connection (for API access)

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Build the project:
   ```bash
   mvn clean compile
   ```
4. Run the application:
   ```bash
   mvn exec:java -Dexec.mainClass="app.Main"
   ```

Or run directly from your IDE by executing `app.Main`.

### First Time Setup

1. **Create an Account**: Click "Sign Up" to create a new user account
2. **Login**: Use your credentials to log in
3. **Explore Features**: Navigate using the top toolbar buttons

## Usage Guide

### Real-Time Trade Feed

1. Click the **"Realtime Trade"** button in the top toolbar
2. Enter a trading symbol in the input field (default: `BINANCE:BTCUSDT`)
3. Click **"Connect"** to start receiving real-time trade data
4. View live updates for:
   - Symbol
   - Last Price
   - Volume
   - Timestamp
5. Click **"Disconnect"** to stop the feed before connecting to a new symbol

**Important Notes:**
- You must disconnect before connecting to a new symbol
- Rate limiting is enforced (5-second cooldown between connections)
- Invalid symbols or connection errors will display appropriate error messages
- The application automatically handles API rate limits (429 errors)

### Other Features

- **News**: View market and company news with filtering options
- **History**: Access earnings history for companies
- **Filter Search**: Search and filter stocks by various criteria
- **Market Status**: View current market status (open/closed)

## Technical Details

### Dependencies
- **OkHttp 4.12.0**: For HTTP and WebSocket connections
- **JSON 20240303**: For JSON parsing
- **JUnit 5.8.1**: For testing (test scope)

### API Integration
- **Finnhub API**: Used for stock data, news, earnings, and real-time trade feeds
- WebSocket connections for real-time data streaming
- REST API for historical and static data

### Data Storage
- User data stored in `users.csv` (file-based storage)
- Alternative database storage option available (`DBUserDataAccessObject`)

## Project Structure

```
CSC207_Stock_Project/
├── src/
│   ├── main/java/
│   │   ├── app/              # Application setup
│   │   ├── data_access/      # API clients and data sources
│   │   ├── entity/           # Domain models
│   │   ├── interface_adapter/# Adapters
│   │   ├── use_case/         # Business logic
│   │   └── view/             # UI components
│   └── test/java/           # Unit tests
├── pom.xml                   # Maven configuration
└── README.md                 # This file
```

## Error Handling

The application includes comprehensive error handling:
- **Rate Limiting**: Automatic cooldown periods to prevent API throttling
- **Connection Errors**: User-friendly error messages for connection issues
- **Invalid Symbols**: Clear feedback when symbols are not found
- **Network Issues**: Graceful handling of network failures

## Contributing

This is a course project following Clean Architecture principles. When contributing:
- Maintain separation of concerns
- Follow the existing architecture patterns
- Add appropriate error handling
- Include unit tests for new features


