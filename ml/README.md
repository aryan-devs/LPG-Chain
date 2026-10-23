# LPG-Chain AI Demand Forecasting Service

Lightweight AI/ML demand forecasting microservice for the LPG-Chain distribution management platform.

> **CRITICAL DISCLAIMER: SYNTHETIC TRAINING DATA**
> The current system does not yet possess historical customer booking logs of sufficient depth for production forecasting. Therefore, the baseline model is trained exclusively using reproducible, simulated LPG distribution records.
> **All forecasts and evaluation figures represent synthetic data patterns only.**
> Predictions must be treated as estimates and must NOT be used to automatically adjust official physical stock, financial allocations, or delivery commitments without operator confirmation.

---

## Architecture Overview

1. **Synthetic Data Generator (`generate_synthetic_data.py`)**:
   - Generates 365 days of continuous daily booking history across 3 canonical cylinder categories:
     - `DOMESTIC_14_2` (Domestic 14.2 kg - ~74% volume, repeat interval 28-45 days, weekend & winter heating peaks)
     - `DOMESTIC_5_0` (Domestic 5 kg - ~16% volume, low-consumption households)
     - `COMMERCIAL_19_0` (Commercial 19 kg - ~10% volume, restaurants/hotels, weekday peaks, 3-8 cylinders per refill)
   - Fixed random seed: `42`.

2. **Model Training & Evaluation (`train_forecasting_model.py`)**:
   - Time-series feature engineering:
     - Calendar: Day of week, day of month, month, weekend flag, quarter
     - Lags: Lags 1, 7, 14, 21, 28 (target shifted to avoid future leakage)
     - Rolling statistics: 7-day and 14-day rolling means and standard deviations
     - Category one-hot encodings
   - **Chronological Split**: 80% train, 20% test held-out chronologically to strictly prevent temporal data leakage.
   - **Baseline Comparison**: Evaluated against a 7-day moving average baseline.
   - **Metrics**: Mean Absolute Error (MAE) and Root Mean Squared Error (RMSE).
   - **Model**: `RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)`.

3. **FastAPI Prediction Server (`forecast_api.py`)**:
   - Runs locally on `http://localhost:8000`.
   - Exposes:
     - `GET /health`: Health and model version.
     - `GET /metrics`: Model evaluation metrics, baseline comparison, and synthetic metadata.
     - `GET /forecast?horizon_days=7|14|30&distributor_id={id}`: Multi-step category and daily aggregate forecasts.

---

## Setup & Execution

### 1. Create Virtual Environment & Install Dependencies
```bash
cd ml
python -m venv .venv
.\.venv\Scripts\pip.exe install -r requirements.txt
```

### 2. Generate Synthetic Dataset
```bash
.\.venv\Scripts\python.exe generate_synthetic_data.py
```
Outputs `ml/data/synthetic_bookings.csv`.

### 3. Train the Model & Evaluate
```bash
.\.venv\Scripts\python.exe train_forecasting_model.py
```
Outputs:
- `ml/models/demand_forecast_model.pkl`
- `ml/models/evaluation_metrics.json`
- `ml/models/model_metadata.json`

### 4. Start the Prediction API
```bash
.\.venv\Scripts\python.exe forecast_api.py
```
Runs at `http://localhost:8000`. Interactive documentation at `http://localhost:8000/docs`.

---

## Graceful Fallback in Frontend
The frontend Distributor Portal checks `http://localhost:8000/health`. If the Python microservice is not running, the portal displays a clear, helpful offline state banner with startup commands and does not crash or interrupt distributor operations.
