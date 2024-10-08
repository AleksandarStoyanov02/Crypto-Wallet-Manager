# Cryptocurrency Wallet Manager 💰 💸
## Overview
The Cryptocurrency Wallet Manager is a console-based client-server application designed to simulate a personal cryptocurrency wallet. It allows users to manage their cryptocurrency investments with functionalities such as registration, login, depositing money, buying and selling cryptocurrencies, and obtaining wallet summaries.

The application works with the free public REST API - [CoinAPI](https://www.coinapi.io).
## Features
### Register: Create a new user account with a username and password. Passwords are securely stored.
### Login: Access the wallet by logging in with a registered username and password.
### Deposit Money: Add funds to your wallet in USD.
### List Offerings: View available cryptocurrencies that can be bought or sold.
### Buy Crypto: Purchase a specified amount of cryptocurrency using available funds.
### Sell Crypto: Sell a specified cryptocurrency and keep the proceeds in your wallet.
### Get Wallet Summary: Obtain detailed information about current investments and wallet balance.
### Get Wallet Overall Summary: View the overall profit or loss from your investments.
## API Integration
The application integrates with CoinAPI to fetch real-time cryptocurrency data. Key details are cached for 30 minutes to reduce API requests and improve performance. The following endpoints are used:

GET /v1/assets: Provides information about all available cryptocurrencies.
GET /v1/assets/{asset_id}: Provides details about a specific cryptocurrency.

## Usage
Client Commands
### Register

register <username> <password>

### Login

login <username> <password>

### Deposit Money

deposit-money <amount>

### List Offerings

list-offerings

### Buy Crypto

buy --offering=<offering_code> --money=<amount>

### Sell Crypto

sell --offering=<offering_code>

### Get Wallet Summary

get-wallet-summary

### Get Wallet Overall Summary

get-wallet-overall-summary
