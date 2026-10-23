"""
train_forecasting_model.py
--------------------------
Trains a machine learning demand forecasting model on synthetic LPG booking history.

DISCLAIMER:
All training data is strictly SYNTHETIC (generated via generate_synthetic_data.py).
Evaluation metrics reflect simulated data patterns only and must not be interpreted
as production accuracy benchmarks on real consumers.

Features & Safeguards:
- Chronological train/test split (80% train, 20% test held-out) to prevent temporal data leakage.
- Baseline comparison: 7-day moving average.
- Metrics: Mean Absolute Error (MAE) and Root Mean Squared Error (RMSE).
- Artifacts saved:
    * ml/models/demand_forecast_model.pkl
    * ml/models/evaluation_metrics.json
    * ml/models/model_metadata.json
"""

import os
os.environ["OPENBLAS_NUM_THREADS"] = "1"
os.environ["MKL_NUM_THREADS"] = "1"
os.environ["OMP_NUM_THREADS"] = "1"
os.environ["NUMEXPR_NUM_THREADS"] = "1"

import json
import pickle
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_FILE = os.path.join(BASE_DIR, "data", "synthetic_bookings.csv")
MODELS_DIR = os.path.join(BASE_DIR, "models")
MODEL_FILE = os.path.join(MODELS_DIR, "demand_forecast_model.pkl")
METRICS_FILE = os.path.join(MODELS_DIR, "evaluation_metrics.json")
METADATA_FILE = os.path.join(MODELS_DIR, "model_metadata.json")

CATEGORIES = ["DOMESTIC_14_2", "DOMESTIC_5_0", "COMMERCIAL_19_0"]

def ensure_data():
    if not os.path.exists(DATA_FILE):
        print("Synthetic dataset not found. Generating now...")
        from generate_synthetic_data import generate_dataset
        generate_dataset()

def build_daily_timeseries(df: pd.DataFrame) -> pd.DataFrame:
    df["booking_date"] = pd.to_datetime(df["booking_date"])
    
    # Aggregate daily quantity requested per category
    daily = df.groupby(["booking_date", "cylinder_category"])["quantity"].sum().reset_index()
    
    # Create complete date x category grid
    min_date = df["booking_date"].min()
    max_date = df["booking_date"].max()
    all_dates = pd.date_range(min_date, max_date, freq="D")
    
    idx = pd.MultiIndex.from_product([all_dates, CATEGORIES], names=["booking_date", "cylinder_category"])
    grid = pd.DataFrame(index=idx).reset_index()
    
    merged = pd.merge(grid, daily, on=["booking_date", "cylinder_category"], how="left")
    merged["quantity"] = merged["quantity"].fillna(0).astype(int)
    merged = merged.sort_values(["cylinder_category", "booking_date"]).reset_index(drop=True)
    return merged

def create_features(df: pd.DataFrame) -> pd.DataFrame:
    dfs = []
    for cat in CATEGORIES:
        cdf = df[df["cylinder_category"] == cat].copy().sort_values("booking_date")
        
        # Calendar features
        cdf["day_of_week"] = cdf["booking_date"].dt.dayofweek
        cdf["day_of_month"] = cdf["booking_date"].dt.day
        cdf["month"] = cdf["booking_date"].dt.month
        cdf["is_weekend"] = (cdf["day_of_week"] >= 5).astype(int)
        cdf["quarter"] = cdf["booking_date"].dt.quarter
        
        # Lag features
        cdf["lag_1"] = cdf["quantity"].shift(1)
        cdf["lag_7"] = cdf["quantity"].shift(7)
        cdf["lag_14"] = cdf["quantity"].shift(14)
        cdf["lag_21"] = cdf["quantity"].shift(21)
        cdf["lag_28"] = cdf["quantity"].shift(28)
        
        # Rolling statistics (using shift(1) to strictly avoid target leakage)
        cdf["rolling_mean_7"] = cdf["quantity"].shift(1).rolling(7, min_periods=1).mean()
        cdf["rolling_std_7"] = cdf["quantity"].shift(1).rolling(7, min_periods=1).std().fillna(0)
        cdf["rolling_mean_14"] = cdf["quantity"].shift(1).rolling(14, min_periods=1).mean()
        
        # Baseline prediction: 7-day rolling average
        cdf["baseline_pred"] = cdf["rolling_mean_7"]
        
        dfs.append(cdf)
        
    full = pd.concat(dfs, ignore_index=True)
    
    # Category one-hot encoding
    for cat in CATEGORIES:
        full[f"is_cat_{cat}"] = (full["cylinder_category"] == cat).astype(int)
        
    # Drop warm-up rows where 28-day lag is NaN
    full = full.dropna().reset_index(drop=True)
    return full

def train():
    ensure_data()
    os.makedirs(MODELS_DIR, exist_ok=True)
    
    print("[1/5] Loading synthetic dataset...")
    df_raw = pd.read_csv(DATA_FILE)
    print(f"Loaded {len(df_raw)} records from {DATA_FILE}")
    
    print("[2/5] Building continuous daily series & feature engineering...")
    daily_ts = build_daily_timeseries(df_raw)
    feat_df = create_features(daily_ts)
    
    # Feature columns
    feature_cols = [
        "day_of_week", "day_of_month", "month", "is_weekend", "quarter",
        "lag_1", "lag_7", "lag_14", "lag_21", "lag_28",
        "rolling_mean_7", "rolling_std_7", "rolling_mean_14",
        "is_cat_DOMESTIC_14_2", "is_cat_DOMESTIC_5_0", "is_cat_COMMERCIAL_19_0"
    ]
    
    print(f"[3/5] Chronological train/test split (80/20)...")
    unique_dates = feat_df["booking_date"].drop_duplicates().sort_values().reset_index(drop=True)
    split_idx = int(len(unique_dates) * 0.80)
    split_date = unique_dates.iloc[split_idx]
    
    train_mask = feat_df["booking_date"] < split_date
    test_mask = feat_df["booking_date"] >= split_date
    
    X_train = feat_df.loc[train_mask, feature_cols]
    y_train = feat_df.loc[train_mask, "quantity"]
    
    X_test = feat_df.loc[test_mask, feature_cols]
    y_test = feat_df.loc[test_mask, "quantity"]
    y_test_baseline = feat_df.loc[test_mask, "baseline_pred"]
    
    print(f"Training window: {feat_df.loc[train_mask, 'booking_date'].min().strftime('%Y-%m-%d')} to {feat_df.loc[train_mask, 'booking_date'].max().strftime('%Y-%m-%d')} ({len(X_train)} rows)")
    print(f"Testing window:  {feat_df.loc[test_mask, 'booking_date'].min().strftime('%Y-%m-%d')} to {feat_df.loc[test_mask, 'booking_date'].max().strftime('%Y-%m-%d')} ({len(X_test)} rows)")
    
    print("[4/5] Training Random Forest Regressor & evaluating baseline...")
    # Baseline evaluation
    baseline_mae = float(mean_absolute_error(y_test, y_test_baseline))
    baseline_rmse = float(np.sqrt(mean_squared_error(y_test, y_test_baseline)))
    
    # Train ML Model
    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=10,
        min_samples_split=4,
        random_state=42,
        n_jobs=1
    )
    model.fit(X_train, y_train)
    
    # Predictions
    y_pred = np.maximum(0, model.predict(X_test))
    model_mae = float(mean_absolute_error(y_test, y_pred))
    model_rmse = float(np.sqrt(mean_squared_error(y_test, y_pred)))
    
    # Per-category test metrics
    cat_metrics = {}
    for cat in CATEGORIES:
        cat_mask = feat_df.loc[test_mask, "cylinder_category"] == cat
        y_t_cat = y_test[cat_mask]
        y_p_cat = y_pred[cat_mask]
        cat_metrics[cat] = {
            "mae": round(float(mean_absolute_error(y_t_cat, y_p_cat)), 3),
            "rmse": round(float(np.sqrt(mean_squared_error(y_t_cat, y_p_cat))), 3),
            "mean_actual": round(float(y_t_cat.mean()), 2),
            "mean_predicted": round(float(y_p_cat.mean()), 2)
        }
    
    print(f"\n================ MODEL EVALUATION RESULTS ================")
    print(f"Data source:              SYNTHETIC_TRAINING_DATA")
    print(f"Baseline (7-day MA) MAE:  {baseline_mae:.3f} | RMSE: {baseline_rmse:.3f}")
    print(f"RandomForest Model  MAE:  {model_mae:.3f} | RMSE: {model_rmse:.3f}")
    mae_improvement = ((baseline_mae - model_mae) / baseline_mae) * 100
    print(f"MAE Improvement vs Base:  {mae_improvement:.1f}%")
    print("========================================================\n")
    
    print("[5/5] Saving model artifacts and metadata...")
    with open(MODEL_FILE, "wb") as f:
        pickle.dump(model, f)
        
    metrics = {
        "model_version": "v1.0.0-synthetic-rf",
        "algorithm": "RandomForestRegressor",
        "data_source": "SYNTHETIC_TRAINING_DATA",
        "training_timestamp": datetime.now().isoformat(),
        "disclaimer": "Metrics calculated on held-out synthetic test records. Cold-start demonstration only.",
        "dataset_summary": {
            "total_bookings": len(df_raw),
            "date_range": f"{df_raw['booking_date'].min()} to {df_raw['booking_date'].max()}",
            "train_samples": len(X_train),
            "test_samples": len(X_test)
        },
        "overall_metrics": {
            "model_mae": round(model_mae, 3),
            "model_rmse": round(model_rmse, 3),
            "baseline_mae": round(baseline_mae, 3),
            "baseline_rmse": round(baseline_rmse, 3),
            "mae_improvement_pct": round(mae_improvement, 1)
        },
        "category_metrics": cat_metrics,
        "feature_importance": {
            col: round(float(imp), 4)
            for col, imp in zip(feature_cols, model.feature_importances_)
        }
    }
    
    with open(METRICS_FILE, "w", encoding="utf-8") as f:
        json.dump(metrics, f, indent=2)
        
    metadata = {
        "feature_cols": feature_cols,
        "categories": CATEGORIES,
        "last_trained_date": unique_dates.max().strftime("%Y-%m-%d"),
        "model_file": os.path.basename(MODEL_FILE)
    }
    with open(METADATA_FILE, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
        
    print(f"Saved model to:   {MODEL_FILE}")
    print(f"Saved metrics to: {METRICS_FILE}")
    print(f"Training completed successfully!")

if __name__ == "__main__":
    train()
