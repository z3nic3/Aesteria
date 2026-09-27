# Aesteria

## Maritime Oil Spill Detection, Drift Analysis and AIS Attribution

Aesteria is a Smart India Hackathon prototype for analysing suspected marine oil-spill events by combining spill detection, drift/origin estimation and AIS vessel attribution.

The prototype pipeline is:

Spill Detection
→ Drift / Origin Estimation
→ AIS Search
→ Distance + Time Correlation
→ Ranked Vessel Candidates
→ Evidence Dashboard

> **Prototype Mode:** This demonstration uses synthetic AIS records and simulated spill/drift metadata. The attribution pipeline is designed to accept real AIS and environmental observations when available.

---

## Project Structure

```text
Aesteria/
├── model/
│   └── notebook.ipynb
│
├── backend/
│   ├── src/
│   │   ├── ais/
│   │   │   ├── AISAttribution.java
│   │   │   ├── SpillEvent.java
│   │   │   ├── VesselRecord.java
│   │   │   └── VesselResult.java
│   │   │
│   │   ├── server/
│   │   │   └── ApiServer.java
│   │   │
│   ├── data/
│   │   ├── drift_summary.csv
│   │   └── results.json
│   │
│   ├── lib/
│   │   └── postgresql-42.7.13.jar
│   │
│   └── bin/
│
├── frontend/
│   └── index.html
│
├── Dockerfile
└── README.md


## How It Works

1. **Spill Detection** — A U-Net segmentation model, trained on labeled Sentinel-1 SAR imagery, identifies oil-spill pixels within a satellite scene and outputs region boundaries, area, and detection confidence (`model/notebook.ipynb`).
2. **Geolocation** — Detected pixel regions are converted into real-world latitude/longitude using the source scene's known corner coordinates.
3. **Drift / Origin Estimation** — A simplified drift model projects the spill backward in time to estimate its point and time of origin, and forward in time to forecast its spread (`backend/data/drift_summary.csv`).
4. **AIS Search & Correlation** — Vessel traffic records near the estimated origin (in space and time) are retrieved and filtered against irrelevant traffic (`backend/src/ais/`).
5. **Distance + Time Correlation** — Each candidate vessel (`VesselRecord.java`) is scored on proximity to the origin, timing alignment, AIS signal gaps, vessel type, and speed anomalies (`AISAttribution.java`, `VesselResult.java`).
6. **Ranked Vessel Candidates** — Vessels are ranked by suspicion score with explainable, human-readable reasoning per vessel, served via `SpillEvent.java` and `ApiServer.java`.
7. **Evidence Dashboard** — All outputs are surfaced in a single visual console (`frontend/index.html`): detection, drift path, and ranked suspects.

## Tech Stack

- **Modeling**: Python, TensorFlow/Keras (U-Net segmentation, MobileNetV2 classifier), XGBoost (tabular classifier), OpenCV, Shapely
- **Backend**: Java, PostgreSQL (attribution logic and API server, see `backend/`)
- **Frontend**: HTML/CSS/JavaScript (evidence dashboard, `frontend/index.html`)
- **Deployment**: Docker
- **Data Sources**: Sentinel-1 SAR imagery (Copernicus Data Space Ecosystem), AIS vessel data (synthetic in prototype mode; designed for real feeds e.g. marinecadastre.gov)

## Getting Started

### Prerequisites
- Docker
- Java (if running the backend outside Docker)
- Python 3.10+ (if re-running or modifying `model/notebook.ipynb`)

### Run with Docker

```bash
docker build -t aesteria .
docker run -p 8080:8080 aesteria
```

### Run the model notebook standalone

```bash
cd model
jupyter notebook notebook.ipynb
```

## Results

| Model | Precision | Recall | F1 |
|---|---|---|---|
| U-Net (segmentation) | ~93–95% | ~94–95% | ~93–95% |

Detection results (`results.json`) are geolocated and passed through the drift and AIS correlation stages to produce ranked vessel candidates, visualized in the evidence dashboard.

## Roadmap

- Replace synthetic AIS records with live/historical feeds (e.g. marinecadastre.gov)
- Integrate real oceanographic and meteorological data (currents, wind) for drift modeling, in place of assumed vectors
- Expand segmentation to explicitly separate look-alike substances from confirmed oil
- End-to-end automated pipeline wiring (detection → drift → attribution → dashboard) without manual handoff

